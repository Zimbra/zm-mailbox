package com.zimbra.cs.redolog;

import java.io.File;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;

/**
 * Redolog provider which implements specific behavior depending on service leader status
 *
 */

public class LeaderAwareRedoLogProvider extends RedoLogProvider {

    private RedologLeaderListener leaderListener;

    @Override
    public boolean isMaster() {
        return true;
    }

    @Override
    public boolean isSlave() {
        return false;
    }

    @Override
    public void startup(boolean runCrashRecovery) throws ServiceException {
        initRedoLogManager();
        if (RedoConfig.redoLogEnabled()) {
            mRedoLogManager.start(runCrashRecovery);
        }
    }

    @Override
    public void shutdown() throws ServiceException {
        if (RedoConfig.redoLogEnabled()) {
            mRedoLogManager.stop();
        }
    }

    public RedologLeaderListener getLeaderListener() {
        return leaderListener;
    }

    public void setLeaderListener(RedologLeaderListener leaderListener) {
        this.leaderListener = leaderListener;
    }

    @Override
    public void initRedoLogManager() throws ServiceException {
        //make current log filename unique per server
        //this avoids consistency issues which can occur if another process changes shared file after opening
        //the name is not material per-say, and becomes irrelevant once a rollover occurs
        File redoLog = new File(logPathForServerId(Provisioning.getInstance().getLocalServer().getId()));

        //archive dir should be safe to share; since each log gets a unique sequence number and thus unique name
        File archDir = new File(RedoConfig.redoLogArchiveDir());
        super.mRedoLogManager = new LeaderAwareRedoLogManager(redoLog, archDir, true, leaderListener);
    }

    static String logPathForServerId(String serverId) throws ServiceException {
        File baseFile = new File(RedoConfig.redoLogPath());
        StringBuilder sb = new StringBuilder();
        String directory = baseFile.getParent();
        sb.append(directory);
        if (!directory.endsWith("/")) {
            sb.append("/");
        }
        sb.append(serverId)
            .append("-").append(baseFile.getName());
        return sb.toString();
    }

    static String fileBaseName() {
        return new File(RedoConfig.redoLogPath()).getName();
    }

}
