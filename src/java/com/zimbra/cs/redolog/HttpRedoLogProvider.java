/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog;

import com.zimbra.common.service.ServiceException;

/**
 * Redolog provider which writes operations to http redolog service
 *
 */
public class HttpRedoLogProvider extends RedoLogProvider {

	public HttpRedoLogProvider() {
        super();
    }

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

    @Override
    public void initRedoLogManager() throws ServiceException {
        super.mRedoLogManager = new HttpRedoLogManager();
    }
}
