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
        String className = config.getAttr(Provisioning.A_liquidRedoLogProvider);
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
