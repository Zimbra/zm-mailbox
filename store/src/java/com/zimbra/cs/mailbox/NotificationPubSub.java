package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.PendingLocalModifications;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.Session.SourceSessionInfo;
import com.zimbra.cs.session.Session.Type;
import com.zimbra.cs.session.SessionCache;

public abstract class NotificationPubSub {

    private static Factory factory;
    private Publisher publisher = null;
    private Subscriber subscriber = null;
    protected Mailbox mbox;

    static {
        setFactory(new RedisPubSub.Factory());
    }

    public NotificationPubSub(Mailbox mbox) {
        this.mbox = mbox;
    }

    public static void setFactory(Factory factory) {
        NotificationPubSub.factory = factory;
    }

    public static Factory getFactory() {
        return factory;
    }

    public synchronized Publisher getPublisher() {
        if (publisher == null) {
            publisher = initPubisher();
        }
        return publisher;
    }

    public synchronized Subscriber getSubscriber() {
        if (subscriber == null) {
            subscriber = initSubscriber();
        }
        return subscriber;
    }

    protected abstract Publisher initPubisher();

    protected abstract Subscriber initSubscriber();

    public abstract class Publisher {

        /**
         * Publish notifications to listeners. By default, only notifies local listeners.
         */
        public void publish(PendingLocalModifications pns, int changeId, SourceSessionInfo source, int sourceMailboxHash) {
            notifyLocal(pns, changeId, source, sourceMailboxHash);
        }

        /**
         * Returns the number of listeners that are listening on this publisher
         */
        public abstract int getNumListeners(Type type);

        public void notifyLocal(PendingLocalModifications pns, int changeId, SourceSessionInfo source, int sourceMailboxHash) {
            Subscriber subscriber = NotificationPubSub.this.getSubscriber();
            subscriber.notifyListeners(pns, changeId, source, sourceMailboxHash, false);
        }
    }

    public abstract class Subscriber {

        private final List<Session> listeners = new CopyOnWriteArrayList<Session>();

        /** Returns the list of all <code>Mailbox</code> listeners of a given type.
         *  Returns all listeners when the passed-in type is <tt>null</tt>. */
        public List<Session> getListeners(Session.Type stype) {
            if (listeners.isEmpty()) {
                return Collections.emptyList();
            } else if (stype == null) {
                return new ArrayList<Session>(listeners);
            }

            List<Session> sessions = new ArrayList<Session>(listeners.size());
            for (Session s : listeners) {
                if (s.getType() == stype) {
                    sessions.add(s);
                }
            }
            return sessions;
        }

        boolean hasListeners(Session.Type stype) {
            if (listeners.isEmpty()) {
                return false;
            } else if (stype == null) {
                return true;
            }

            for (Session s : listeners) {
                if (s.getType() == stype) {
                    return true;
                }
            }
            return false;
        }

        /** Loookup a {@link Session} in the set of listeners on this mailbox. */
        public Session getListener(String sessionId) {
            if (sessionId != null) {
                for (Session session : listeners) {
                    if (sessionId.equals(session.getSessionId())) {
                        return session;
                    }
                }
            }
            return null;
        }

        /** Adds a {@link Session} to the set of listeners notified on Mailbox
         *  changes.
         *
         * @param session  The Session registering for notifications.
         * @throws ServiceException  If the mailbox is in maintenance mode. */
        public void addListener(Session session) throws ServiceException {
            if (session == null) {
                return;
            }
            assert(session.getSessionId() != null);
            if (mbox.getMaintenance() != null) {
                throw MailServiceException.MAINTENANCE(mbox.getId());
            }
            ZimbraLog.mailbox.debug("adding listener: %s", session);
            if (!listeners.contains(session)) {
                listeners.add(session);
            }
            /* check whether beginMaintenance happened whilst adding a listener.  If it did then
             * undo it.  This avoids getting a write lock */
            if (mbox.getMaintenance() != null) {
                purgeListeners();
                throw MailServiceException.MAINTENANCE(mbox.getId());
            }
        }

        /** Removes a {@link Session} from the set of listeners notified on
         *  Mailbox changes.
         *
         * @param session  The listener to deregister for notifications. */
        public void removeListener(Session session) {
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                ZimbraLog.mailbox.debug("clearing listener: %s", session);
            }
            listeners.remove(session);
        }

        /** Cleans up and disconnects all {@link Session}s listening for
         *  notifications on this Mailbox.
         *
         * @see SessionCache#clearSession(Session) */
        public void purgeListeners() {
            ZimbraLog.mailbox.debug("purging listeners");

            for (Session session : listeners) {
                SessionCache.clearSession(session);
            }
            listeners.clear();
        }

        protected void notifyListeners(PendingModifications pns, int changeId, SourceSessionInfo source, int sourceMailboxHash, boolean skipLocal) {
            for (Session session : listeners) {
                MailboxStore mbox = session.getMailbox();
                if (skipLocal && mbox != null && sourceMailboxHash == mbox.hashCode()) {
                    //ignore all notifications received via pub/sub from the same mailbox node - sessions were notified locally
                    continue;
                }
                if (skipLocal && source != null && source.equals(session)) {
                    //ignore notifications received via pub/sub from the the same session on a different node
                    continue;
                }
                session.notifyPendingChanges(pns, changeId, source);
            }
        }
    }

    public static abstract class Factory {

        private Map<String, NotificationPubSub> cache = new HashMap<>();

        protected abstract NotificationPubSub initPubSub(Mailbox mbox);

        public synchronized NotificationPubSub getNotificationPubSub(Mailbox mbox) {
            NotificationPubSub pubsub = cache.get(mbox.getAccountId());
            if (pubsub == null) {
                pubsub = initPubSub(mbox);
                cache.put(mbox.getAccountId(), pubsub);
            }
            return pubsub;
        }
    }
}
