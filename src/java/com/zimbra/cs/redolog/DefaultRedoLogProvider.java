/*
 * Created on 2005. 6. 29.
 */
package com.zimbra.cs.redolog;

import java.io.File;

import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class DefaultRedoLogProvider extends RedoLogProvider {

	public boolean isMaster() {
		return true;
	}

	public boolean isSlave() {
		return false;
	}

    public void startup() throws ServiceException {
        initRedoLogManager();
        if (RedoConfig.redoLogEnabled())
            mRedoLogManager.start();
    }

    public void shutdown() throws ServiceException {
        if (RedoConfig.redoLogEnabled())
            mRedoLogManager.stop();
    }
    
    public void initRedoLogManager() {
        // RedoLogManager instance is needed even when redo logging
        // is disabled.
        File redoLog = new File(RedoConfig.redoLogPath());
        File archDir = new File(RedoConfig.redoLogArchiveDir());
        super.mRedoLogManager = new RedoLogManager(redoLog, archDir, true);
    }
}
