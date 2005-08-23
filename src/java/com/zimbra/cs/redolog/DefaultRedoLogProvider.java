/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

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
