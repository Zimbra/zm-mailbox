package com.zimbra.cs.analytics;

import java.util.Collection;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.analytics.MessageBehavior.BehaviorType;

/**
 *
 * @author Greg Solovyev
 * Default stubbed implementation of BehaviorManager. Does not actually do anything.
 */
public class DefaultBehaviorManager extends BehaviorManager {

    public DefaultBehaviorManager() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public void storeBehavior(MessageBehavior b) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteBehaviorStore(String accountId) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public MessageBehavior getFirstBehavior(String accountId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<MessageBehaviorSet> findBehaviorsByTime(String accountId,
            long from, long to) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<MessageBehaviorSet> findBehaviors(String accountId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasBehavior(String accountId, int itemId, BehaviorType type)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    public static final class Factory implements BehaviorManager.Factory {
        private static DefaultBehaviorManager accessor = null;
        /**
         * Get an BehaviorWriter instance for a particular account
         */
        @Override
        public DefaultBehaviorManager getBehaviorManager()  {
            if(accessor == null) {
                accessor = new DefaultBehaviorManager();
                accessor.init();
            }
            return accessor;
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        public void destroy() {

        }
    }

}
