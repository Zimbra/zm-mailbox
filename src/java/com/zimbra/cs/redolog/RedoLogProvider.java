/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 6. 29.
 */
package com.zimbra.cs.redolog;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

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
                ZimbraLog.misc.info("Redolog provider name not specified.  Using default " +
                                    providerClass.getName());
            }
            provider = (RedoLogProvider) providerClass.newInstance();
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

    public abstract void initRedoLogManager();
    
    public RedoLogManager getRedoLogManager() {
        return mRedoLogManager;
    }
}
