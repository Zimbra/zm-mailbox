/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.ObjectUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxListener.ChangeNotification;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;

/**
 * Redis-based MailboxListenerTransport.
 */
public class RedisMailboxListenerTransport implements MailboxListenerTransport {
    static final String KEY_PREFIX = MemcachedKeyPrefix.MBOX_PENDING_MODS;
    @Autowired protected Pool<Jedis> jedisPool;
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected Map<String, Set<Session>> subscribedSessionsByAccountId = new ConcurrentHashMap<>();

    public RedisMailboxListenerTransport() {
    }

    public RedisMailboxListenerTransport(Pool<Jedis> jedisPool) {
        this.jedisPool = jedisPool;
    }

    @PostConstruct
    public void init() throws Exception {
        final Jedis jedis = jedisPool.getResource();
        Thread thread = new Thread() {
            public void run() {
                try {
                    String channelPattern = KEY_PREFIX + "*";
                    ZimbraLog.mailbox.info("Subscribing to Redis channel pattern %s for mailbox notifies", channelPattern);
                    jedis.psubscribe(new MyListener(), channelPattern);
                    ZimbraLog.mailbox.info("Unsubscribed from Redis channel pattern %s for mailbox notifies", channelPattern);
                } finally {
                    jedisPool.returnResource(jedis);
                }
            }
        };
        thread.start();
    }

    protected String channel(Account mailboxAccount) {
        return KEY_PREFIX + mailboxAccount.getId();
    }

    protected Set<Session> getSubscribedSessions(String accountId) {
        Set<Session> sessions = subscribedSessionsByAccountId.get(accountId);
        return sessions != null ? sessions : Collections.emptySet();
    }

    @Override
    public void publish(ChangeNotification notification) throws ServiceException {
        try {
            // Generate a message to send
            String message = new Holder(notification).toJSON();

            // Send
            try (Jedis jedis = jedisPool.getResource()) {
                String channel = channel(notification.mailboxAccount);
                jedis.publish(channel, message);
            }

        } catch (Exception e) {
            ZimbraLog.session.warn("failed sending ChangeNotification to Redis", e);
        }
    }


    /** Subscribe to remote mailbox change notifications */
    @Override
    public void subscribe(Session session) throws ServiceException {
        // Add to a collection, where received notifies can find & delegate to
        String  key = session.getMailbox().getAccountId();
        Set<Session> sessions = subscribedSessionsByAccountId.get(key);
        if (sessions == null) {
            sessions = new HashSet<Session>();
            subscribedSessionsByAccountId.put(key, sessions);
        }
        sessions.add(session);
    }


    /** Unsubscribe from remote mailbox change notifications */
    @Override
    public void unsubscribe(Session session) {
        Mailbox mbox = session.getMailbox();
        if (mbox == null) {
            return;
        }
        Set<Session> sessions = subscribedSessionsByAccountId.get(mbox.getAccountId());
        if (sessions != null) {
            sessions.remove(session);
        }
    }


    public static class Holder {
        public String accountId;
        public int changeId;
        public String mailboxOp;
        public String senderServerId;
        public String timestamp;
        public String contentType;
        public String contentEncoding;
        public String content;

        public Holder() {}

        public Holder(ChangeNotification notification) throws ServiceException, IOException {
            accountId = notification.mailboxAccount.getId();
            changeId = notification.lastChangeId;
            mailboxOp = notification.op == null ? null : notification.op.name();
            senderServerId = Provisioning.getInstance().getLocalServer().getId();
            timestamp = DateUtil.toISO8601(new Date(notification.timestamp));
            // TODO BZ98708: send JSON, not serialized Java, so that other languages can work with these messages
            contentType = "application/x-java-serialized-object; class=" + PendingModifications.class.getName();
            contentEncoding = "base64";
            content = Base64.encode(PendingModifications.JavaObjectSerializer.serialize(notification.mods));
        }

        public static Holder parseJSON(String json) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            Holder holder = objectMapper.readValue(json, Holder.class);
            return holder;
        }

        public String toJSON() throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(this);
            return json;
        }
    }


    class MyListener extends JedisPubSub {
        public void onPMessage(String pattern, String channel, String message) {
            ZimbraLog.mailbox.debug("onPMessage pattern=%s channel=%s", pattern, channel);
            try {
                Holder holder = Holder.parseJSON(message);

                // Ignore pending modifications messages that originated in this process (so is already handled)
                if (ObjectUtils.equals(holder.senderServerId, Provisioning.getInstance().getLocalServer().getId())) {
                    return;
                }

                for (Session session: getSubscribedSessions(holder.accountId)) {
                    byte[] data = Base64.decode(holder.content);
                    PendingModifications pm = PendingModifications.JavaObjectSerializer.deserialize(session.getMailbox(), data);
                    Session source = null; // Foreign session is the publisher
                    session.notifyPendingChanges(pm, holder.changeId, source);
                }
            } catch (Exception e) {
                ZimbraLog.mailbox.error("failed decoding PendingModifications notification", e);
            }
        }

        public void onMessage(String channel, String message) {}
        public void onSubscribe(String channel, int subscribedChannels) {};
        public void onUnsubscribe(String channel, int subscribedChannels) {};
        public void onPUnsubscribe(String pattern, int subscribedChannels) {};
        public void onPSubscribe(String pattern, int subscribedChannels) {};
    }
}
