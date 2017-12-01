package com.zimbra.cs.event;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.contacts.RelatedContactsParams;
import com.zimbra.cs.contacts.RelatedContactsResults;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics;
import com.zimbra.cs.event.logger.EventLogger;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraphDataPoint;

import java.util.List;


/**
 * Interface to an event storage backend that allows for querying and deleting of
 * event data. This is distinct from {@link EventLogger}, which only handles
 * writing events.
 */
public abstract class EventStore {

    private static Map<String, String> REGISTERED_FACTORIES = new HashMap<>();
    private static Factory factory;
    protected String accountId;

    public EventStore(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    public static void registerFactory(String prefix, String clazz) {
        if (REGISTERED_FACTORIES.containsKey(prefix)) {
            ZimbraLog.index.warn(
                    "Replacing EventStore factory class '%s' registered for prefix '%s' with another factory class: '%s'",
                    REGISTERED_FACTORIES.get(prefix), prefix, clazz);
        }
        REGISTERED_FACTORIES.put(prefix, clazz);
    }

    public static Factory getFactory() throws ServiceException {
        if (factory == null) {
            String factoryClassName = null;
            String eventURL = Provisioning.getInstance().getLocalServer().getEventBackendURL();
            if (eventURL != null) {
                String[] tokens = eventURL.split(":");
                if (tokens != null && tokens.length > 0) {
                    String backendFactoryName = tokens[0];
                    factoryClassName = REGISTERED_FACTORIES.get(backendFactoryName);
                }
            } else {
                throw ServiceException.FAILURE("EventStore is not configured", null);
            }
            setFactory(factoryClassName);
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

    public static void setFactory(String factoryClassName) throws ServiceException {
        Class<? extends Factory> factoryClass = null;
        try {
            try {
                factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException e) {
                try {
                    factoryClass = ExtensionUtil.findClass(factoryClassName)
                            .asSubclass(Factory.class);
                } catch (ClassNotFoundException cnfe) {
                    throw ServiceException.FAILURE("unable to find EventStore Factory class " + factoryClassName, cnfe);
                }
            }
        } catch (ClassCastException cce) {
            ZimbraLog.event.error("unable to initialize EventStore Factory class %s", factoryClassName, cce);
        }
        setFactory(factoryClass);
    }

    public static void setFactory(Class<? extends Factory> factoryClass) throws ServiceException {
        if (factoryClass == null) {
            throw ServiceException.FAILURE("EventStore Factory class cannot be null", null);
        }
        String className = factoryClass.getName();
        ZimbraLog.search.info("setting EventStore.Factory class %s", className);
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE(String.format("unable to initialize EventStore Factory %s", className), e);
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
            ZimbraLog.event.debug("no event store specified; skipping deleting events for account %s", accountId);
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

    /**
     * Calculate contact affinity based on SENT and AFFINITY events
     */
    public abstract RelatedContactsResults getContactAffinity(RelatedContactsParams params) throws ServiceException;

    public abstract Long getContactFrequencyCount(String contact, ContactAnalytics.ContactFrequencyEventType combined, ContactAnalytics.ContactFrequencyTimeRange timeRange) throws ServiceException;

    /**
     * Get the frequency of emails sent and received from a contact for
     * 1) Current Month
     *      StartDate - First day of Month
     *      EndDate - Current date (today)
     *      Aggregation Unit - Per day
     *
     * 2) Last 6 Months
     *      StartDate - First day of the week 6 months back from the first day of week for the current week
     *      EndDate - First day of the week of current week
     *      Aggregation Unit - Per week
     *      Optional int FirstDayOfWeek - day of the week set as first day of week for attribute 'zimbraPrefCalendarFirstDayOfWeek'
     *      Default first day of week should be set to Sunday.
     *      As per 'zimbraPrefCalendarFirstDayOfWeek' 0 = Sunday...6 = Saturday
     *
     * 3) Current year
     *      StartDate - First day of the year
     *      EndDate - Current date (today)
     *      Aggregation unit - Per month
     */
    public abstract List<ContactFrequencyGraphDataPoint> getContactFrequencyGraph(String contact, ContactAnalytics.ContactFrequencyGraphTimeRange timeRange) throws ServiceException;

    public interface Factory {

        public EventStore getEventStore(String accountId);

        public void shutdown();
    }
}
