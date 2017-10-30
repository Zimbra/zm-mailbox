package com.zimbra.cs.event.logger;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.Event;

/**
 * Interface to an event storage backend that allows for querying and deleting of
 * event data. This is distinct from {@link EventLogger}, which only handles
 * writing events.
 * @author iraykin
 *
 */
public abstract class EventStore {

    private static Factory factory;
    protected String accountId;

    public EventStore(String accountId) {
        this.accountId = accountId;
    }

    public static Factory getFactory() throws ServiceException {
        if (factory == null) {
            setFactory(SolrEventStore.Factory.class);
        }
        return factory;
    }

    public static void clearFactory() {
        if (factory != null) {
            ZimbraLog.event.info("shutting down %s",factory.getClass().getName());
            factory.shutdown();
            factory = null;
        }
    }

    public static final void setFactory(Class<? extends Factory> factoryClass) throws ServiceException {
        String className = factoryClass.getName();
        ZimbraLog.search.info("setting EventStore.Factory class %s", className);
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE(String.format("unable to initialize ContactGraph factory %s", className), e);
        }
    }

    public static boolean isEnabled() {
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            return !Strings.isNullOrEmpty(server.getEventBackendURL());
        } catch (ServiceException e) {
            ZimbraLog.event.error("unable to determine if event store is enabled");
            return false;
        }
    }

    public void deleteEvents() throws ServiceException {
        if (isEnabled()) {
            deleteEventsByAccount();
            EventLogger.getEventLogger().log(Event.generateDeleteAccountEvent(accountId));
        } else {
            ZimbraLog.event.debug("no event store specifed; skipping deleting events for account %s", accountId);
        }
    }

    public void deleteEvents(String dataSourceId) throws ServiceException {
        if (isEnabled()) {
            deleteEventsByDataSource(dataSourceId);
            EventLogger.getEventLogger().log(Event.generateDeleteDataSourceEvent(accountId, dataSourceId));
        } else {
            ZimbraLog.event.debug("no event store specifed; skipping deleting events for account %s, dsId=%s", accountId, dataSourceId);
        }
    }
    /**
     * Delete all event data for this account
     */
    protected abstract void deleteEventsByAccount() throws ServiceException;

    /**
     * Delete all events for the specified datasource ID
     */
    protected abstract void deleteEventsByDataSource(String dataSourceId) throws ServiceException;

    public interface Factory {

        public EventStore getEventStore(String accountId);

        public void shutdown();
    }
}
