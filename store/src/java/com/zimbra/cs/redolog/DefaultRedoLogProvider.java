/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 6. 29.
 */
package com.zimbra.cs.redolog;

import java.io.File;

import com.zimbra.common.service.ServiceException;

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
    
    public void initRedoLogManager() throws ServiceException {
        // RedoLogManager instance is needed even when redo logging
        // is disabled.
        File redoLog = new File(RedoConfig.redoLogPath());
        File archDir = new File(RedoConfig.redoLogArchiveDir());
        super.mRedoLogManager = new RedoLogManager(redoLog, archDir, false);
    }
}
