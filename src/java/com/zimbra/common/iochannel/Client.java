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
package com.zimbra.common.iochannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.zimbra.common.iochannel.Config.ServerConfig;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * Client manages the outbound packets in iochannel framework.
 * It maintains the list of available peer servers in the cluster
 * and allows the application to send a packet to designated peer
 * server.
 *
 * @author jylee
 *
 */
public class Client implements Runnable {

    /**
     * PeerServer is an abstraction of server running on another
     * machine in the cluster.  There is a separate message queue
     * and socket channel for each peer.
     */
    public class PeerServer {
        private PeerServer(ServerConfig config) throws IOException {
            this(config.id, config.host, config.port);
        }

        private PeerServer(String id, String host, int p) throws IOException {
            this.id = id;
            hostname = host;
            port = p;
            backlog = new ConcurrentLinkedQueue<Packet>();
            connect();
        }

        /**
         * Requests iochannel to send a message to this peer server.
         */
        public void sendMessage(String msg) throws IOException {
            sendMessage(msg.getBytes("UTF-8"));
        }

        /**
         * Requests iochannel to send a message to this peer server.
         */
        public void sendMessage(byte[] msg) {
            sendMessage(ByteBuffer.wrap(msg));
        }

        /**
         * Requests iochannel to send a message to this peer server.
         */
        public void sendMessage(ByteBuffer msg) {
            backlog.add(Packet.create(clientId, this, msg));
            setActive();
        }

        /**
         * Returns the backlog, or how many messages are queued
         * for this peer server.
         */
        public synchronized int getBacklog() {
            return backlog.size();
        }

        /*
         * Called by Client when this peer is ready to be connected.
         */
        private void connect() {
            connected = false;
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                key.attach(this);
                channel.connect(new InetSocketAddress(hostname, port));
            } catch (IOException e) {
                log.warn("can't connect to %s:%d", hostname, port, e);
            }
        }

        /*
         * Called by Client when this peer is ready for writing.
         */
        private long write() throws IOException {
            long bytesWritten = 0;
            synchronized (this) {
                if (!connected || isBacklogEmpty()) {
                    return bytesWritten;
                } else {
                    if (current == null) {
                        current = backlog.poll();
                    }
                }
            }
            try {
                log.debug("client:writing to %s", channel);
                bytesWritten = channel.write(current.getPayload());
            } catch (NotYetConnectedException e) {
                log.warn("channel %s:%d is down", current.getDestination().hostname, current.getDestination().port);
            }
            synchronized (this) {
                if (!current.hasRemaining()) {
                    current = null;
                }
            }
            log.debug("client:writing %d bytes to %d", bytesWritten, channel.socket().getLocalPort());
            return bytesWritten;
        }

        private synchronized boolean isBacklogEmpty() {
            return current == null && backlog.isEmpty();
        }

        /*
         * Called by the client during shutdown.
         */
        private void shutdown() {
            try {
                log.debug("client:closing channel %s", channel);
                channel.close();
            } catch (IOException e) {
                log.debug("shutdown", e);
            }
        }

        /*
         * Make sure we are not holding another lock when calling
         * setActive or unsetActive in order to avoid deadlock.
         */
        private void unsetActive() {
            synchronized (clientThread) {
                activeSet.remove(this);
            }
        }

        private void setActive() {
            if (!connected) {
                return;
            }
            synchronized (clientThread) {
                if (!activeSet.contains(this)) {
                    activeSet.add(this);
                }
                clientThread.notifyAll();
            }
        }

        /*
         * The peer is connected to the remote server
         */
        private void finishConnect() throws IOException {
            channel.finishConnect();               // prepare the channel
            channel.socket().setTcpNoDelay(true);  // disable Nagling for better latency
            connected = true;                      // change the status
            setActive();                           // make itself available as active
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(id);
            buf.append(" (").append(hostname).append(":").append(port).append(")");
            return buf.toString();
        }

        private boolean connected;
        private Packet current;
        private final String id;
        private final String hostname;
        private final int port;
        private SocketChannel channel;
        private final ConcurrentLinkedQueue<Packet> backlog;

    }

    /**
     * Creates a Client instance and starts it.
     */
    public static Client start(Config c) throws IOException {
        Client client = new Client(c);
        return client.start();
    }

    private Client(Config c) throws IOException {
        selector = Selector.open();
        peers = new HashMap<String,PeerServer>();
        activeSet = new HashSet<PeerServer>();
        for (ServerConfig peer : c.getPeerServers()) {
            peers.put(peer.id, new PeerServer(peer));
        }
        clientId = c.getLocalConfig().id;
    }

    private Client start() {
        shutdown = false;
        clientThread = new Thread(this);
        clientThread.setDaemon(true);
        clientThread.start();
        return this;
    }

    /**
     * Returns a collection of peer servers available to send a message to
     * from this client.
     */
    public Collection<PeerServer> getPeerServers() {
        return peers.values();
    }

    public PeerServer getPeer(String id) throws IOChannelException {
        PeerServer p = peers.get(id);
        if (p == null) {
            throw IOChannelException.NoSuchPeer(id);
        }
        return p;
    }

    public void shutdown() {
        shutdown = true;
        for (PeerServer peer : peers.values()) {
            peer.shutdown();
        }
        try {
            selector.close();
        } catch (IOException e) {
            log.debug("shutdown", e);
        }
    }

    private static Log log = LogFactory.getLog("iochannel");

    private final String clientId;
    private Thread clientThread;
    private boolean shutdown;
    private final Selector selector;
    private final HashMap<String,PeerServer> peers;
    private final HashSet<PeerServer> activeSet;

    private static final int waitInterval = 10000;  // 10s

    private boolean hasBusyPeer() {
        return !activeSet.isEmpty();
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                // when there is no work sleep for a little.  it will be
                // either waken up when work is brought in by another
                // thread or timeout expires
                synchronized (clientThread) {
                    if (!hasBusyPeer()) {
                        clientThread.wait(waitInterval);
                    }
                }
                selector.select();
                for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); iter.hasNext();) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isValid()) {
                        PeerServer peer = (PeerServer)key.attachment();
                        try {
                            if (key.isConnectable()) {
                                log.debug("client:connecting to %s %s:%d", peer.id, peer.hostname, peer.port);
                                peer.finishConnect();
                            }
                            if (peer.isBacklogEmpty()) {
                                peer.unsetActive();
                                continue;
                            }
                            if (key.isWritable()) {
                                peer.write();
                            }
                        } catch (IOException e) {
                            log.debug("socket operation failed. retry connect %s", peer.id, e);
                            // retry
                            key.cancel();
                            peer.unsetActive();
                            peer.connect();
                            continue;
                        }
                    }
                }
            } catch (Throwable e) {
                if (e instanceof OutOfMemoryError) {
                    break;
                }
                if (!(e instanceof ClosedSelectorException)) {
                    // log the error and continue
                    log.warn("writing to peer server", e);
                    continue;
                }
            }
        }
    }
}
