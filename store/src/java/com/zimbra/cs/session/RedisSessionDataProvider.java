package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.session.PendingLocalModifications.PendingModificationSnapshot;
import com.zimbra.cs.session.SoapSession.QueuedNotifications;

public class RedisSessionDataProvider extends SessionDataProvider {

    private static final String SOAP_SESSION_SEQ_MAP_NAME = "SOAP_SESSION_SEQUENCES";
    private static final String SESSION_ID_SEQ_NAME = "SESSION_ID_SEQUENCE";
    private RMap<String, Integer> soapSequenceMap;
    private RAtomicLong idCounter;
    private Map<String, RedisNotificationQueue> notificationMap;

    public RedisSessionDataProvider() {
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        soapSequenceMap = client.getMap(SOAP_SESSION_SEQ_MAP_NAME);
        idCounter = client.getAtomicLong(SESSION_ID_SEQ_NAME);
        notificationMap = new HashMap<>();
    }

    @Override
    protected long getNextIdSequence() {
        return idCounter.addAndGet(1);
    }

    @Override
    public int getNextSoapSessionSequence(String soapSessionId) {
        return soapSequenceMap.addAndGet(soapSessionId, 1);
    }


    @Override
    public int getCurrentSoapSessionSequence(String soapSessionId) {
        Integer val = soapSequenceMap.putIfAbsent(soapSessionId, 1);
        return val == null ? 1 : val;
    }

    @Override
    public void cleanup(String sessionId) {
        soapSequenceMap.remove(sessionId);
        if (notificationMap.containsKey(sessionId)) {
            notificationMap.get(sessionId).clear();
        }
    }

    @Override
    public boolean soapSessionExists(String sessionId) {
        return soapSequenceMap.containsKey(sessionId);
    }

    @Override
    public NotificationQueue getSoapNotifications(SoapSession session) {
        RedisNotificationQueue queue = notificationMap.get(session.getSessionId());
        if (queue == null) {
            queue = new RedisNotificationQueue(session);
            notificationMap.put(session.getSessionId(), queue);
        }
        return queue;
    }

    public static class RedisNotificationQueue extends SessionDataProvider.NotificationQueue {

        private RList<SerializableNotification> list;
        private Mailbox mbox;
        private String authAcctId;

        public RedisNotificationQueue(SoapSession session) {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            list = client.getList("session_" + session.getSessionId());
            mbox = session.getMailboxOrNull();
            authAcctId = session.getAuthenticatedAccountId();
        }

        @Override
        public List<QueuedNotifications> getNotifications() {
            List<QueuedNotifications> l = new ArrayList<>();
            try {
                for (SerializableNotification s: list.readAll()){
                    try {
                        l.add(s.toNotifications(mbox));
                    } catch (ServiceException e) {
                        ZimbraLog.session.warn("unable to deserialize notification from redis");
                    }
                }
            } catch (RedisException e) {
                ZimbraLog.session.warnQuietly("unable to read notification list from redis, returning empty list", e);
            }
            return l;
        }

        private static class SerializableNotification {
            private String authAcctId;
            private PendingModificationSnapshot snapshot;
            private int sequence;

            public SerializableNotification() {}

            public SerializableNotification(String acctId, PendingModificationSnapshot snapshot, int sequence) {
                this.authAcctId = acctId;
                this.snapshot = snapshot;
                this.sequence = sequence;
            }

            private QueuedNotifications toNotifications(Mailbox mbox) throws ServiceException {
                QueuedNotifications q = new QueuedNotifications(authAcctId, sequence);
                if (snapshot != null) {
                    q.addNotification(PendingLocalModifications.fromSnapshot(mbox, snapshot));
                }
                return q;
            }
        }

        @Override
        public void add(QueuedNotifications notifications) {
            try {
                PendingModificationSnapshot snapshot = notifications.mMailboxChanges == null ? null : notifications.mMailboxChanges.toSnapshot();
                //TODO: handle remote notifications
                int seq = notifications.getSequence();
                list.add(new SerializableNotification(authAcctId, snapshot, seq));
            } catch (ServiceException e) {
                ZimbraLog.session.error("unable to push QueuedNotifications to redis", e);
            }
        }

        /**
         * overrides to act directly on the underlying RList, to avoid unnecessary deserializing
         */
        @Override
        public boolean isEmpty() {
            try {
                return list.isEmpty();
            } catch (RedisException e) {
                ZimbraLog.session.warnQuietly("unable to determine if notification queue is empty, returning true", e);
                return true;
            }
        }

        @Override
        public void clear() {
            list.clear();
        }

        @Override
        public int size() {
            try {
                return list.size();
            } catch (RedisException e) {
                ZimbraLog.session.warnQuietly("unable to determine size of notification queue, returning 0", e);
                return 0;
            }
        }

        @Override
        public void purge(int sequence) {
            //TODO: should this be made fully atomic with a Lua script?
            int idx = 0;
            for (QueuedNotifications qn : getNotifications()) {
                if (qn.getSequence() <= sequence) {
                    idx++;
                } else {
                    break;
                }
            }
            list.trim(idx, -1);
        }
    }

    public static class Factory implements SessionDataProvider.Factory {

        @Override
        public SessionDataProvider getIdProvider() {
            return new RedisSessionDataProvider();
        }
    }
}
