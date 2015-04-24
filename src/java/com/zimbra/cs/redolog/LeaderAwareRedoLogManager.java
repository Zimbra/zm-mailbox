package com.zimbra.cs.redolog;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;

import com.zimbra.common.consul.ConsulClient;
import com.zimbra.common.consul.ServiceHealthResponse;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.redolog.logger.FileHeader;
import com.zimbra.cs.redolog.logger.LeaderAwareFileLogWriter;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.util.Zimbra;

/**
 * Redolog manager which implements specific behavior depending on service leader status
 */
public class LeaderAwareRedoLogManager extends FileRedoLogManager {

    private RedologLeaderListener leaderListener;

    LeaderAwareRedoLogManager(File redolog, File archdir,
            boolean supportsCrashRecovery, RedologLeaderListener leaderListener) throws ServiceException {
        super(redolog, archdir, supportsCrashRecovery);
        this.leaderListener = leaderListener;
    }

    @Override
    public LogWriter createLogWriter(long fsyncIntervalMS) throws ServiceException {
        return new LeaderAwareFileLogWriter(this, mLogFile, fsyncIntervalMS, leaderListener);
    }

    @Override
    public void log(RedoableOp op, boolean synchronous) throws ServiceException {
        super.log(op, synchronous);
    }

    @Override
    public void logOnly(RedoableOp op, boolean synchronous)
            throws ServiceException {
        super.logOnly(op, synchronous);
    }

    @Override
    protected boolean isRolloverNeeded(boolean immediate)
            throws ServiceException {
        return immediate || super.isRolloverNeeded(immediate);
    }

    @Override
    protected void signalLogError(Throwable e) throws ServiceException {
        if (isCausedBy(e, LeaderUnavailableException.class)) {
            throw ServiceException.TEMPORARILY_UNAVAILABLE(e);
        } else {
            throw ServiceException.FAILURE("redolog exception", e);
        }
    }

    private boolean isCausedBy(Throwable throwable, Class<? extends Throwable> targetCause) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause.getClass().isAssignableFrom(targetCause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public void forceRolloverPeers(String serverId) throws IOException, ServiceException {
        File[] files;
        if (serverId != null) {
            files = new File[]{new File(LeaderAwareRedoLogProvider.logPathForServerId(serverId))};
        } else {
            //all of them
            File logDir = new File(RedoConfig.redoLogPath()).getParentFile();
            files = logDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return (name != null && name.endsWith(LeaderAwareRedoLogProvider.fileBaseName()));
                }
            });
        }
        Map<String, File> serverFiles = new HashMap<String, File>();

        for (File serverFile : files) {
            if (serverFile.equals(this.getLogFile())) {
                this.forceRollover();
            } else {
                serverFiles.put(serverFile.getAbsolutePath(), serverFile);
            }
        }

        if (!serverFiles.isEmpty()) {
            List<ServiceHealthResponse> healthResp = Zimbra.getAppContext().getBean(ConsulClient.class).health("zimbra-redolog", true);
            if (healthResp != null) {
                for (ServiceHealthResponse healthyService : healthResp) {
                    String nodeName = healthyService.node.name;
                    Server nodeServer = nodeName == null ? null : Provisioning.getInstance().getServerByName(nodeName);
                    String logPath = nodeServer == null ? null : LeaderAwareRedoLogProvider.logPathForServerId(nodeServer.getId());
                    if (nodeServer != null && serverFiles.containsKey(logPath)) {
                        //we have a healthy matching server, try to roll via http API
                        String url = healthyService.service.tags.contains("ssl") ? "https" : "http"
                                + "://" + healthyService.node.address + ":" + healthyService.service.port
                                + "/redolog/data";
                        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
                        PostMethod post = new PostMethod(url);
                        try {
                            post.setParameter("cmd", "rollover");
                            post.setParameter("force", "true");
                            int code = client.executeMethod(post);
                            if (code != HttpStatus.SC_OK) {
                                ZimbraLog.redolog.warn("unexpected response from redolog servlet [" + code + "] message:[" + post.getResponseBodyAsString() + "]");
                            } else {
                                ZimbraLog.redolog.debug("rolled over server file %s", logPath);
                                serverFiles.remove(logPath);
                            }
                        } finally {
                            post.releaseConnection();
                        }
                    }
                }
            }

            //force the rest via shared file access
            for (String remainingPath : serverFiles.keySet()) {
                ZimbraLog.redolog.debug("rolling over file %s", remainingPath);
                File serverFile = new File(remainingPath);
                if (RedoConfig.redoLogDeleteOnRollover()) {
                    serverFile.delete();
                } else {
                    FileRolloverManager fileRollMgr = getRolloverManager();
                    RandomAccessFile raf = new RandomAccessFile(serverFile, "r");
                    FileHeader header = new FileHeader();
                    header.read(raf);
                    raf.close();
                    File rolloverFile = fileRollMgr.getRolloverFile(header.getSequence());
                    ZimbraLog.redolog.debug("renaming %s to %s during rollover", serverFile.getAbsolutePath(), rolloverFile.getAbsolutePath());
                    File destDir = rolloverFile.getParentFile();
                    if (destDir != null && !destDir.exists()) {
                        destDir.mkdirs();
                    }
                    if (!serverFile.renameTo(rolloverFile)) {
                        throw new IOException("Unable to rename current redo log to " + rolloverFile.getAbsolutePath());
                    }
                }
            }
        }
    }

}
