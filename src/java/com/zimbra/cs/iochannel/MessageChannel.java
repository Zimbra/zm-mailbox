/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.iochannel;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.zimbra.common.iochannel.Client;
import com.zimbra.common.iochannel.Client.PeerServer;
import com.zimbra.common.iochannel.IOChannelException;
import com.zimbra.common.iochannel.Server;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

/**
 * MessageChannel is a service in ZCS that allows a message to be sent
 * from one mailboxd to another.  There can be multiple apps registered
 * to share the single underlying iochannel without creating its own
 * server socket.  Each application can register its own type of Message
 * it wants to send and receive, and MessageChannel will alert the app
 * when a message for its type is received.
 *
 * @author jylee
 *
 */
public class MessageChannel {

    public static MessageChannel getInstance() {
        synchronized (MessageChannel.class) {
            if (instance == null) {
                instance = new MessageChannel();
            }
        }
        return instance;
    }

    public synchronized void startup() throws ServiceException, IOException {
        if (!running) {
            ZcsConfig config = new ZcsConfig();
            server = Server.start(config);
            client = Client.start(config);
            server.registerCallback(new MessageChannelCallback());
            running = true;
        }
    }

    public synchronized void shutdown() {
        server.shutdown();
        client.shutdown();
        running = false;
    }

    /**
     * Sends the message to peer server that hosts Account identified
     * in Message.getRecipientAccountId().
     */
    public void sendMessage(Message message) {
        String accountId = message.getRecipientAccountId();
        Provisioning prov = Provisioning.getInstance();
        try {
            Account targetAccount = prov.getAccountById(accountId);
            if (targetAccount == null) {
                log.error("account %s doesn't exist", accountId);
                return;
            }
            com.zimbra.cs.account.Server targetServer = targetAccount.getServer();
            String peerHostname;
            PeerServer peer;
            if (targetServer == null ||
                    (peerHostname = targetServer.getServiceHostname()) == null ||
                    (peer = client.getPeer(peerHostname)) == null) {
                log.error("can't find server for account %s", accountId);
                return;
            }
            peer.sendMessage(message.serialize());
        } catch (ServiceException e) {
            log.error("can't send notification", e);
        } catch (IOChannelException e) {
            log.error("can't find the client", e);
        } catch (IOException e) {
            log.error("can't send notification", e);
        }
    }

    private static class MessageChannelCallback implements Server.NotifyCallback {

        @Override
        public void dataReceived(String header, ByteBuffer buffer) {
            try {
                Message m = Message.create(buffer);
                m.getHandler().handle(m, header);
            } catch (IOException e) {
                log.warn("can't create message", e);
            }
        }
    }

    private Server server;
    private Client client;
    private boolean running;

    private static Log log = LogFactory.getLog("iochannel");
    private static MessageChannel instance;
}
