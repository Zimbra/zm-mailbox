package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.session.SoapSession.QueuedNotifications;

public class LocalSessionDataProvider extends SessionDataProvider {

    private long idSequence = 0;
    private Map<String, Integer> soapSequenceMap;
    private Map<String, NotificationQueue> notificationMap;

    public LocalSessionDataProvider() {
        soapSequenceMap = new HashMap<>();
        notificationMap = new HashMap<>();
    }

    @Override
    public long getNextIdSequence() {
        return idSequence++;
    }

    @Override
    public int getNextSoapSessionSequence(String soapSessionId) {
        return soapSequenceMap.merge(soapSessionId, 1, Integer::sum);
    }

    @Override
    public void cleanup(String sessionId) {
        soapSequenceMap.remove(sessionId);
    }

    @Override
    public boolean soapSessionExists(String sessionId) {
        return soapSequenceMap.containsKey(sessionId);
    }

    @Override
    public NotificationQueue getSoapNotifications(SoapSession session) {
        String id = session.getSessionId();
        NotificationQueue queue = notificationMap.get(id);
        if (queue == null) {
            queue = new LocalNotificationQueue();
            notificationMap.put(id, queue);
        }
        return queue;
    }

    @Override
    public synchronized String getNextSessionIdSequence(Session.Type type) {
        //synchonizing in the local case
        return super.getNextSessionIdSequence(type);
    }

    @Override
    public int getCurrentSoapSessionSequence(String soapSessionId) {
        Integer curId = soapSequenceMap.get(soapSessionId);
        return curId == null ? 0 : curId;
    }

    public static class LocalNotificationQueue extends SessionDataProvider.NotificationQueue {


        private List<QueuedNotifications> list;

        public LocalNotificationQueue() {
            list = new ArrayList<>();
        }

        @Override
        public List<QueuedNotifications> getNotifications() {
            return list;
        }

        @Override
        public void add(QueuedNotifications notifications) {
            list.add(notifications);
        }

        @Override
        public void purge(int sequence) {
            synchronized(list){
                for (Iterator<QueuedNotifications> it = list.iterator(); it.hasNext(); ) {
                    if (it.next().getSequence() <= sequence) {
                        it.remove();
                    } else {
                        break;
                    }
                }
            }
        }

    }

    public static class Factory implements SessionDataProvider.Factory {

        @Override
        public SessionDataProvider getIdProvider() {
            return new LocalSessionDataProvider();
        }
    }
}
