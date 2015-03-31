package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.FileRedoLogManager;
import com.zimbra.cs.redolog.HttpRedoLogManager;
import com.zimbra.cs.redolog.LeaderChangeListener;
import com.zimbra.cs.redolog.LeaderUnavailableException;
import com.zimbra.cs.redolog.RedologLeaderListener;
import com.zimbra.cs.redolog.op.RedoableOp;

/**
 * Log writer implementation which is aware of service leadership
 * When the current session is the leader; this class logs directly to a (shared) file e.g. /opt/zimbra/redolog/redo.log
 * When the another session is the leader; this class forwards the request on
 * When there is no leader; this class stages operations locally and forwards them once a new leader has been elected
 */
public class LeaderAwareFileLogWriter extends FileLogWriter {

    private FileLogWriter alternateWriter;
    private HttpLogWriter remoteWriter;
    private boolean hasAlternateWrites = false;
    private RedologLeaderListener leaderListener;

    private File stagedredofile = new File(LC.zimbra_tmp_directory.value() + "/redolog/stage/stageredo.log");

    public LeaderAwareFileLogWriter(FileRedoLogManager redoLogMgr, File logfile,
            long fsyncIntervalMS, RedologLeaderListener leaderListener) throws ServiceException {
        super(redoLogMgr, logfile, fsyncIntervalMS);
        this.leaderListener = leaderListener;
        leaderListener.addListener(new LeaderChangeListener() {
            @Override
            public void onLeaderChange(String newLeaderSessionId) {
                if (newLeaderSessionId != null) {
                    try {
                        drainLogOps();
                    } catch (IOException e) {
                        ZimbraLog.redolog.error("unable to drain queued ops due to ioexception", e);
                    }
                }
            }
        });
        stagedredofile.getParentFile().mkdirs();
        this.alternateWriter = new FileLogWriter(redoLogMgr, stagedredofile, fsyncIntervalMS) {
            @Override
            protected boolean deleteOnRollover() {
                return true;
            }
        };
        try {
            alternateWriter.open();
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("unable to start redolog", ioe);
        }

        this.remoteWriter = new HttpLogWriter(redoLogMgr) {
            @Override
            protected void notifyCallback(RedoableOp op) throws IOException {
            }

            @Override
            protected String getUrl() throws IOException {
                return HttpRedoLogManager.getUrl(false);
            }
        };
        try {
            remoteWriter.open();
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("unable to start redolog", ioe);
        }
    }

    private void drainLogOps() throws IOException {
        synchronized (alternateWriter) {
            if (hasAlternateWrites) {
                //drain the alternatewriter
                alternateWriter.close();
                FileLogReader logReader = new FileLogReader(stagedredofile);
                logReader.open();

                RedoableOp stagedOp = null;
                while((stagedOp = logReader.getNextOp()) != null) {
                    ZimbraLog.redolog.debug("staged op %s", stagedOp);
                    if (leaderListener.isLeader()) {
                        super.log(stagedOp, stagedOp.getInputStream(), true);
                        ZimbraLog.redolog.debug("logged local master");
                    } else {
                        remoteWriter.log(stagedOp, stagedOp.getInputStream(), true);
                        ZimbraLog.redolog.debug("logged remote master");
                    }
                }
                logReader.close();

                @SuppressWarnings("rawtypes")
                LinkedHashMap activeOps = new LinkedHashMap();
                alternateWriter.rollover(activeOps);
                alternateWriter.open();
                hasAlternateWrites = false;
            }
        }
    }

    @Override
    public void log(RedoableOp op, InputStream data, boolean synchronous)
            throws IOException {
        if (leaderListener.isLeader()) {
            ZimbraLog.redolog.debug("the leader - logging directly");
            drainLogOps();
            super.log(op, data, synchronous);
        } else if (leaderListener.getLeaderSessionId() != null) {
            ZimbraLog.redolog.debug("not the leader - sending to existing leader");
            try {
                remoteWriter.log(op, data, synchronous);
            } catch (IOException ioe) {
                throw new LeaderUnavailableException(ioe);
            }
        } else {
            ZimbraLog.redolog.debug("no leader - queuing locally");
            synchronized (alternateWriter) {
                alternateWriter.log(op, data, true);
                hasAlternateWrites = true;
            }
        }
    }
}
