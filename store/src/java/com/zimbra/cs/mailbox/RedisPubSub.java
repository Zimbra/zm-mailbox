package com.zimbra.cs.mailbox;

import java.io.IOException;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.PendingLocalModifications;
import com.zimbra.cs.session.PendingLocalModifications.PendingModificationSnapshot;
import com.zimbra.cs.session.Session.SourceSessionInfo;
import com.zimbra.cs.session.Session.Type;

public class RedisPubSub extends NotificationPubSub {

    private RedissonClient client;
    RTopic<NotificationMsg> channel;

    public RedisPubSub(Mailbox mbox, RedissonClient client) {
        super(mbox);
        this.client = client;
        this.channel = client.getTopic(getChannelName());
    }

    private String getChannelName() {
        return mbox.getAccountId();
    }
    @Override
    protected Publisher initPubisher() {
        return new RedisPublisher();
    }

    @Override
    protected Subscriber initSubscriber() {
        return new RedisSubscriber();
    }

    public class RedisPublisher extends Publisher {

        @Override
        public void publish(PendingLocalModifications pns, int changeId, SourceSessionInfo source, int sourceMailboxHash) {
            super.publish(pns, changeId, source, sourceMailboxHash); //notify local first
            try {
                //don't publish mods with no changes remotely to minimize chatter
                if (!pns.hasNotifications()) {
                    return;
                }
                PendingModificationSnapshot snapshot = pns.toSnapshot();
                long received = channel.publish(new NotificationMsg(snapshot, changeId, source, sourceMailboxHash));
                ZimbraLog.mailbox.info("published notifications for changeId=%d, received by %d", changeId, received);
            } catch (IOException | ServiceException e) {
                ZimbraLog.ml.error("unable to serialize notifications for changeId=%d, accountId=%s", changeId, mbox.getAccountId(), e);
            }
        }

        @Override
        public int getNumListeners(Type type) {
            //TODO: this only returns the number of local listeners!
            return RedisPubSub.this.getSubscriber().getListeners(type).size();
        }
    }

    public class RedisSubscriber extends Subscriber implements MessageListener<NotificationMsg> {

        private int listenerId;

        public RedisSubscriber() {
            listenerId = channel.addListener(this);
            ZimbraLog.mailbox.info("began listening on Redis channel %s", channel.getChannelNames().get(0));
        }

        @Override
        public void onMessage(String channelName, NotificationMsg msg) {
            ZimbraLog.mailbox.info("got notification for changeId=%d from channel %s", msg.changeId, channelName);
            PendingLocalModifications mods;
            try {
                mods = PendingLocalModifications.fromSnapshot(mbox, msg.modification);
                notifyListeners(mods, msg.changeId, msg.source, msg.sourceMailboxHash, true);
            } catch (ServiceException e) {
                ZimbraLog.ml.error("unable to deserialize notifications for changeId=%d, accountId=%s", msg.changeId, mbox.getAccountId(), e);
            }
        }
    }

    private static class NotificationMsg {
        private PendingModificationSnapshot modification;
        private int changeId;
        private SourceSessionInfo source;
        private int sourceMailboxHash;

        public NotificationMsg() {}

        public NotificationMsg(PendingModificationSnapshot modification, int changeId,
                SourceSessionInfo source, int sourceMailboxHash) throws IOException, ServiceException {
            this.modification = modification;
            this.changeId = changeId;
            this.source = source;
            this.sourceMailboxHash = sourceMailboxHash;
        }
    }

    public static class Factory extends NotificationPubSub.Factory {

        @Override
        protected NotificationPubSub initPubSub(Mailbox mbox) {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            return new RedisPubSub(mbox, client);
        }
    }
}
