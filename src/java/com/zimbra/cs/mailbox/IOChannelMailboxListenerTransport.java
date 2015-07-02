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
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.zimbra.common.iochannel.Client;
import com.zimbra.common.iochannel.Config;
import com.zimbra.common.iochannel.Config.ServerConfig;
import com.zimbra.common.iochannel.Server;
import com.zimbra.common.iochannel.Server.NotifyCallback;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.iochannel.ServiceLocatorConfig;
import com.zimbra.cs.mailbox.MailboxListener.ChangeNotification;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModificationsJsonSerializer;
import com.zimbra.cs.session.PendingModificationsSerializer;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.Zimbra;

/**
 * iochannel-based MailboxListenerTransport. Since iochannel is a peer-to-peer protocol, use of a
 * service locator is required to lookup the peer list before each notify.
 */
public class IOChannelMailboxListenerTransport implements MailboxListenerTransport {
    protected Config config;
    protected Client client;
    protected Server server;
    protected ServiceLocator serviceLocator;
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected Map<String, Set<Session>> subscribedSessionsByAccountId = new ConcurrentHashMap<>();

    public IOChannelMailboxListenerTransport(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @PostConstruct
    public void init() throws Exception {
        config = new ServiceLocatorConfig(serviceLocator);
        client = Client.start(config);
        server = Server.start(config, serviceLocator);
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
            for (ServerConfig peerServerConfig: config.getPeerServers()) {
                client.getPeer(peerServerConfig.id).sendMessage(message);
            }

        } catch (Exception e) {
            ZimbraLog.session.warn("failed sending ChangeNotification via iochannel", e);
        }
    }


    /** Subscribe to remote mailbox change notifications */
    @Override
    public void subscribe(Session session) throws ServiceException {
        // Add to a collection, where received notifies can find & delegate to
        String key = session.getMailbox().getAccountId();
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
            PendingModificationsSerializer serializer = Zimbra.getAppContext().getBean(PendingModificationsSerializer.class);
            contentType = serializer.getContentType();
            contentEncoding = serializer.getContentEncoding();
            content = new String(serializer.serialize(notification.mods));
        }

        public static Holder parseJSON(byte[] json) throws IOException {
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


    class MyNotifyCallback implements NotifyCallback {
        public void dataReceived(String sender, ByteBuffer buffer) {
            ZimbraLog.mailbox.debug("dataReceived sender=%s", sender);
            try {
                Holder holder = Holder.parseJSON(buffer.array());

                // Ignore pending modifications messages that originated in this process (so is already handled)
                if (ObjectUtils.equals(holder.senderServerId, Provisioning.getInstance().getLocalServer().getId())) {
                    return;
                }

                for (Session session: getSubscribedSessions(holder.accountId)) {
                    byte[] data = holder.content.getBytes();
                    PendingModifications pm = new PendingModificationsJsonSerializer().deserialize(session.getMailbox(), data);
                    Session source = null; // Foreign session is the publisher
                    session.notifyPendingChanges(pm, holder.changeId, source);
                }
            } catch (Exception e) {
                ZimbraLog.mailbox.error("failed decoding PendingModifications notification", e);
            }
        }
    }
}
