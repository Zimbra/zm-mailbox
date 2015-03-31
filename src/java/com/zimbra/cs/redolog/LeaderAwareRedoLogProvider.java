package com.zimbra.cs.redolog;

import java.io.File;

import com.zimbra.common.service.ServiceException;

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
    public void startup() throws ServiceException {
        initRedoLogManager();
        if (RedoConfig.redoLogEnabled()) {
            mRedoLogManager.start();
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
        File redoLog = new File(RedoConfig.redoLogPath());
        File archDir = new File(RedoConfig.redoLogArchiveDir());
        super.mRedoLogManager = new LeaderAwareRedoLogManager(redoLog, archDir, true, leaderListener);
    }

}
