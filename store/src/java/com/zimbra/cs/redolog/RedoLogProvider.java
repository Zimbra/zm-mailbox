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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author jhahm
 */
public abstract class RedoLogProvider {

    private static RedoLogProvider theInstance = null;

    static {
    	try {
			theInstance = loadProvider();
		} catch (ServiceException e) {
            Zimbra.halt("Unable to initialize redolog provider", e);
		}
    }

    public static RedoLogProvider getInstance() {
    	return theInstance;
    }

    private static RedoLogProvider loadProvider()
    throws ServiceException {
        RedoLogProvider provider = null;
        Class providerClass = null;
        Server config = Provisioning.getInstance().getLocalServer();
        String className = config.getAttr(Provisioning.A_zimbraRedoLogProvider);
        try {
            if (className != null) {
                providerClass = Class.forName(className);
            } else {
                providerClass = DefaultRedoLogProvider.class;
                ZimbraLog.misc.debug("Redolog provider name not specified.  Using default " +
                                     providerClass.getName());
            }
            provider = (RedoLogProvider) providerClass.newInstance();
        } catch (OutOfMemoryError e) {
            Zimbra.halt("out of memory", e);
        } catch (Throwable e) {
        	throw ServiceException.FAILURE("Unable to load redolog provider " + className, e);
        }
        return provider;
    }

    protected RedoLogManager mRedoLogManager;
    
    public abstract boolean isMaster();
    public abstract boolean isSlave();
    public abstract void startup() throws ServiceException;
    public abstract void shutdown() throws ServiceException;

    public abstract void initRedoLogManager() throws ServiceException;
    
    public RedoLogManager getRedoLogManager() {
        return mRedoLogManager;
    }
}
