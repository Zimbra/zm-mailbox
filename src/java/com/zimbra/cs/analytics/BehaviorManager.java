package com.zimbra.cs.analytics;

import java.util.Collection;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ExtensionManager;

/**
 * @author Greg Solovyev
 * abstraction for recording and accessing behaviors associated with a message
 */
public abstract class BehaviorManager {
	public abstract void init();

	/**
	 * @param b
	 * @throws ServiceException
	 */
	public abstract void storeBehavior(MessageBehavior b) throws ServiceException;
	private static Factory factory;

	public abstract void deleteBehaviorStore(String accountId) throws ServiceException;
	public abstract MessageBehavior getFirstBehavior(String accountId) throws ServiceException;

    public interface Factory {
        /**
         * Get an instance of BehaviorManager
         */
    	BehaviorManager getBehaviorManager();

    }

    public static Factory getFactory() {
        if (factory == null) {
            try {
                setFactory(Provisioning.getInstance().getLocalServer().getBehaviorProviderFactory());
            } catch (ServiceException e) {
                ZimbraLog.analytics.error("Unable to retrieve Behavior Manager factory from LDAP", e);
            }
        }
        return factory;
    }

    public static final void setFactory(String factoryClassName) {
        Class<? extends Factory> factoryClass = null;
        try {
            try {
                if(factoryClassName != null && !factoryClassName.isEmpty()) {
                    factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
                } else {
                    factoryClass = DefaultBehaviorManager.Factory.class; //default stubbed implementation
                }
            } catch (ClassNotFoundException e) {
                try {
                    factoryClass = ExtensionManager.getInstance().findClass(factoryClassName).asSubclass(Factory.class);
                } catch (ClassNotFoundException cnfe) {
                    ZimbraLog.analytics.error("Unable to initialize Behavior Manager for class " + factoryClassName, cnfe);
                }
            }
        } catch (ClassCastException cce) {
            ZimbraLog.analytics.error("Unable to initialize Behavior Manager for class " + factoryClassName, cce);
        }

        setFactory(factoryClass != null ? factoryClass : DefaultBehaviorManager.Factory.class.asSubclass(Factory.class));
        ZimbraLog.analytics.info("Using Behavior Manager %s", factory.getClass().getDeclaringClass().getSimpleName());
    }

    private static synchronized final void setFactory(Class<? extends Factory> factoryClass) {
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException ie) {
            ZimbraLog.analytics.error("Unable to initialize Behavior Manager for " + factoryClass.getDeclaringClass().getSimpleName(), ie);
        } catch (IllegalAccessException iae) {
            ZimbraLog.analytics.error("Unable to initialize Behavior Manager for " + factoryClass.getDeclaringClass().getSimpleName(), iae);
        }
    }

    /**
     *
     * @param accountId
     * @param from
     * @param to
     * @return list of MessageBehavior sorted by time, oldest first
     * @throws ServiceException
     */
	public abstract Collection<MessageBehaviorSet> findBehaviorsByTime(String accountId,
			 long from, long to) throws ServiceException;

	/**
     *
     * @param accountId
     * @return list of MessageBehavior sorted by time, oldest first
     * @throws ServiceException
     */
	public abstract Collection<MessageBehaviorSet> findBehaviors(String accountId)
			throws ServiceException;

	/**
	 * @param accountId
	 * @param itemId
	 * @param type
	 * @return true if this behavior has already been logged for this message, false otherwise
	 * @throws ServiceException
	 */
	public abstract boolean hasBehavior(String accountId, int itemId, MessageBehavior.BehaviorType type) throws ServiceException;
}
