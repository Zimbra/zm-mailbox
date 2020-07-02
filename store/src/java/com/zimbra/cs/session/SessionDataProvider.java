package com.zimbra.cs.session;

import java.util.List;

import com.zimbra.cs.session.SoapSession.QueuedNotifications;


public abstract class SessionDataProvider {

    private static Factory factory;

    public static Factory getFactory() {
        return factory;
    }

    public static void setFactory(Factory factory) {
        SessionDataProvider.factory = factory;
    }

    public String getNextSessionIdSequence(Session.Type type) {
        return Integer.toString(type.getIndex()) + Long.toString(getNextIdSequence());
    }

    protected abstract long getNextIdSequence();

    public abstract int getNextSoapSessionSequence(String soapSessionId);

    public abstract int getCurrentSoapSessionSequence(String soapSessionId);

    public abstract void cleanup(String sessionId);

    public abstract boolean soapSessionExists(String sessionId);

    public abstract NotificationQueue getSoapNotifications(SoapSession session);

    public static abstract class NotificationQueue {

        public abstract List<QueuedNotifications> getNotifications();

        public abstract void add(QueuedNotifications notifications);

        public boolean isEmpty() {
            return getNotifications().isEmpty();
        }

        public void clear() {
            getNotifications().clear();
        }

        public int size() {
            return getNotifications().size();
        }

        public abstract void purge(int lastSequence);
    }

    public static interface Factory {
        public SessionDataProvider getIdProvider();
    }
}
