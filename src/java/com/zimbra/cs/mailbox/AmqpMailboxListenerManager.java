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

import java.util.Date;

import org.apache.commons.lang.ObjectUtils;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.amqp.AmqpConstants;
import com.zimbra.cs.mailbox.MailboxListener.ChangeNotification;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.Zimbra;

/**
 * AMQP-based MailboxListener management.
 */
public class AmqpMailboxListenerManager implements MailboxListenerManager {
    protected AmqpAdmin amqpAdmin;
    protected AmqpTemplate amqpTemplate;
    protected SimpleMessageListenerContainer messageListenerContainer;


    public AmqpMailboxListenerManager(AmqpAdmin amqpAdmin, AmqpTemplate amqpTemplate) {
        this.amqpAdmin = amqpAdmin;
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public void publish(ChangeNotification notification) throws ServiceException {

        try {
            // Generate a message to send
            // TODO BZ98708: send JSON, not serialized Java, so that other languages can work with these messages
            org.springframework.amqp.core.Message message = MessageBuilder
                    .withBody(PendingModifications.JavaObjectSerializer.serialize(notification.mods))
                    .setContentType(MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT)
                    .setType(PendingModifications.class.getSimpleName())
                    .setHeader(AmqpConstants.HEADER_CHANGE_ID, notification.lastChangeId)
                    .setHeader(AmqpConstants.HEADER_MAILBOX_OP, notification.op.name())
                    .setHeader(AmqpConstants.HEADER_SENDER_SERVER_ID, Provisioning.getInstance().getLocalServer().getId())
                    .setHeader(AmqpConstants.HEADER_TIMESTAMP, DateUtil.toISO8601(new Date(notification.timestamp)))
                    .build();

            // Send
            String exchange = AmqpConstants.EXCHANGE_MBOX.getName();
            String routingKey = "" + notification.mailboxAccount.getId();
            amqpTemplate.send(exchange, routingKey, message);

        } catch (Exception e) {
            ZimbraLog.session.warn("failed sending ChangeNotification to AMQP exchange", e);
        }
    }


    // Create a private queue for this Session object, and bind it to the mailbox exchange using
    // the mailbox id routing key (filter)
    @Override
    public void subscribe(Session session) throws ServiceException {
        Queue queue = amqpAdmin.declareQueue();
        String routingKey = "" + session.getAuthenticatedAccountId();
        Binding binding = BindingBuilder.bind(queue).to(AmqpConstants.EXCHANGE_MBOX).with(routingKey);
        amqpAdmin.declareBinding(binding);

        // Register an async listener to handle messages received by the queue
        messageListenerContainer = new SimpleMessageListenerContainer();
        messageListenerContainer.setApplicationContext(Zimbra.getAppContext());
        messageListenerContainer.setAutoStartup(false);
        messageListenerContainer.setConnectionFactory(Zimbra.getAppContext().getBean(ConnectionFactory.class));
        messageListenerContainer.setExclusive(true);
        messageListenerContainer.setMessageListener(new AmqpMessageListener(session));
        messageListenerContainer.setQueues(queue);
        messageListenerContainer.setReceiveTimeout(60 * 1000);
        messageListenerContainer.start();
    }


    @Override
    public void unsubscribe(Session session) {
        messageListenerContainer.stop();
        messageListenerContainer = null;
    }


    class AmqpMessageListener implements MessageListener {
        final Session session;

        AmqpMessageListener(Session session) {
            this.session = session;
        }

        /** Receive an incoming AMQP message, and delegate it to the notifyPendingChanges handler */
        @Override
        public void onMessage(Message message) {
            MessageProperties props = message.getMessageProperties();

            if (PendingModifications.class.getSimpleName().equals(props.getType())) {
                try {
                    // Ignore pending modifications messages that originated in this process (so is already handled)
                    if (ObjectUtils.equals(Provisioning.getInstance().getLocalServer().getId(), props.getHeaders().get(AmqpConstants.HEADER_SENDER_SERVER_ID))) {
                        return;
                    }

                    if (MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT.equals(props.getContentType())) {
                        PendingModifications pns = PendingModifications.JavaObjectSerializer.deserialize(session.getMailbox(), message.getBody());
                        Integer changeId = new Integer(props.getHeaders().get(AmqpConstants.HEADER_CHANGE_ID).toString());
                        session.notifyPendingChanges(pns, changeId, null);
                        return;
                    }
                } catch (Exception e) {
                    ZimbraLog.mailbox.error("failed decoding PendingModifications notification", e);
                }
            }
        }
    }
}
