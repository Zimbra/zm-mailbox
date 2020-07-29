package com.zimbra.cs.pubsub;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;
import com.zimbra.cs.pubsub.message.BackupHostMsg;
import com.zimbra.cs.pubsub.message.FlushCacheMsg;
import com.zimbra.cs.pubsub.message.PubSubMsg;
import com.zimbra.cs.redolog.BackupHostManager;
import com.zimbra.cs.service.admin.FlushCache;

public class PubSubService {
    public static String BROADCAST = "ZMC-BROADCAST";

    private static PubSubService sInstance;
    private static PubSub.Factory factory;

    private PubSub pubsub;

    protected PubSubService(PubSub pSub) {
        pubsub = pSub;
    }

    public static void setInstance(PubSubService instance) {
        PubSubService.sInstance = instance;
    }

    public static void setFactory(PubSub.Factory factory) {
        PubSubService.factory = factory;
    }

    public void setPubSub(PubSub pubSub) {
        pubsub = pubSub;
    }

    /**
     * Called by Zimbra.startup to start up the PubSub service.
     */
    public void startup() {
        pubsub.startup();
        PubSubService.getInstance().addListener(BROADCAST, new BroadcastCallback());
    }

    /**
     * Adds a new ListenerCallback to the channel
     * @param channel channel name
     * @param callback ListenerCallback to be notified of message arrival
     */
    public void addListener(String channel, ListenerCallback callback) {
        pubsub.addListener(channel, callback);
    }

    public void removeListener(String channel, ListenerCallback callback) {
        pubsub.removeListener(channel, callback);
    }

    /**
     * Publishes a PubSubMsg to all listeners of the BROADCAST topic
     * @param msg message to be broadcast
     */
    public void broadcast(PubSubMsg msg) {
        publish(BROADCAST, msg);
    }

    /**
     * Publishes a PubSubMsg to all listeners of the channel specified
     * @param channel channel to publish
     * @param msg message to be sent on the channel
     */
    public void publish(String channel, PubSubMsg msg) {
        pubsub.publish(channel, msg);
    }

    public static PubSubService getInstance() {
        if (sInstance == null) {
            sInstance = new PubSubService(PubSubService.factory.initPubSub());
        }
        return sInstance;
    }

    public class BroadcastCallback extends ListenerCallback {
        @Override
        public void onMessage(CharSequence channel, PubSubMsg msg) {
            ZimbraLog.pubsub.debug("PubSubService.BroadcastCallback.onMessage(%s) = %s", channel, msg);
            if (msg instanceof FlushCacheMsg) {
                ZimbraLog.pubsub.debug("FlushCacheMsg received: calling FlushCache.doFlush(selector)");
                try {
                    FlushCache.doFlush(((FlushCacheMsg)msg).getSelector());
                } catch (ServiceException e) {
                    ZimbraLog.pubsub.warn("Exception encountered processing '%s' message. \n%s", msg, e);
                }
            } else if (msg instanceof BackupHostMsg) {
                String origin = ((BackupHostMsg) msg).getOriginatingHost();
                ZimbraLog.backup.debug("received pubsub message that backup hosts have changed (origin=%s)", origin);
                if (!origin.equals(MailboxClusterUtil.getPodInfo().getName())) {
                    BackupHostManager.getInstance().getStreamSelector().reload();
                }
            }
        }
    }
}
