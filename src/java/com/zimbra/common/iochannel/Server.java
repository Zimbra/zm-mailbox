/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.iochannel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * iochannel is a framework to send and receive data in a cluster
 * of machines.  All the machines in the clusters are fully connected
 * with each other using TCP connection.  Each peer machines are
 * addressable using unique identifier by implementation of Config.
 *
 * The connections are managed using non blocking {@link Channel}
 * and {@link Selector}.  Each machine in the cluster runs an
 * instance of Server and an instance of Client.  Server class
 * provides a channel of unidirectional inbound packets, and Client
 * class is used to send packets to another machine.  Typically
 * Client maintains open sockets to all the peer servers for
 * outbound packets, and Server maintains a server socket bound
 * to a well known port, and open accepted sockets from all the
 * peer machines.  The connections are persistent, and there is
 * no TCP setup and teardown overhead for sending packets.
 * When a connection breaks due to network or hardware issue,
 * then Client attempts to restore the connection in regular
 * interval until it succeeds.
 *
 * The socket io is done asynchronously.  When the application
 * requests a packet to be sent to a peer, the request is queued
 * for the peer in memory, and will be sent as soon as all the
 * preceding packets to the peer have been sent.  The queues for
 * each peers are managed independently, so when one of the peer
 * goes down, the packets to other peers can be sent unaffected.
 * When the Server receives a packet from a peer, it will send
 * the packet to the registered callback class to be consumed.
 *
 * @author jylee
 *
 */
public class Server implements Runnable {

    /**
     * NotifyCallback is a callback class to be used to alert
     * the application that a packet has been received from
     * peer.
     */
    public interface NotifyCallback {
        public void dataReceived(String sender, ByteBuffer buffer);
    }

    /**
     * Starts a new server instance with supplied configuration.
     */
    public static Server start(Config c) throws IOException {
        return new Server(c).start();
    }

    private Server start() {
        shutdown = false;
        serverThread = new Thread(this);
        serverThread.setDaemon(true);
        serverThread.start();
        return this;
    }

    /**
     * Cleans up and shuts down the server thread.
     */
    public synchronized void shutdown() {
        shutdown = true;
        for (SelectionKey key : selector.keys()) {
            try {
                key.cancel();
                Channel channel = key.channel();
                log.debug("server:closing channel %s", channel);
                channel.close();
            } catch (IOException e) {
                log.debug("shutdown", e);
            }
        }
        try {
            selector.close();
        } catch (IOException e) {
            log.debug("shutdown", e);
        }
        try {
            sschannel.close();
        } catch (IOException e) {
            log.debug("shutdown", e);
        }
    }

    /**
     * Registers the callback class for this Server instance.
     */
    public void registerCallback(NotifyCallback cb) {
        callback = cb;
    }

    private Server(Config c) throws IOException {
        config = c;
        selector = Selector.open();
        sschannel = ServerSocketChannel.open();
        sschannel.configureBlocking(false);
        ServerSocket ss = sschannel.socket();
        ss.setReuseAddress(true);
        InetAddress address = null;
        String host = config.getLocalConfig().host;
        if (host != null) {
            address = InetAddress.getByName(config.getLocalConfig().host);
        }
        ss.bind(new InetSocketAddress(address, config.getLocalConfig().port), 1024);
        sschannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        while (!shutdown) {
            SelectionKey key = null;
            try {
                selector.select();
                for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); iter.hasNext();) {
                    key = iter.next();
                    iter.remove();
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            SocketChannel channel = ((ServerSocketChannel)key.channel()).accept();
                            if (channel != null) {
                                log.debug("server:accepting port %d", channel.socket().getPort());
                                channel.configureBlocking(false);       // non blocking socket
                                channel.socket().setSoLinger(true, 0);  // socket close to be synchronous
                                channel.socket().setTcpNoDelay(true);   // Nagling disabled on high performance server
                                SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ);
                                ByteBuffer buf = ByteBuffer.allocate(Packet.maxMessageSize);
                                buf.clear();
                                newKey.attach(buf);
                            }
                        } else if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel)key.channel();
                            ByteBuffer buffer = (ByteBuffer)key.attachment();
                            log.debug("buffer remaining %d", buffer.remaining());
                            int bytesRead = channel.read(buffer);
                            log.debug("server:reading %d bytes from %d", bytesRead, channel.socket().getPort());
                            if (bytesRead == -1) {
                                log.debug("server:EOF 0");
                                throw IOChannelException.ChannelClosed(channel.toString());
                            } else {
                                int pos = buffer.position();
                                while (pos >= 20) {
                                    int magic = buffer.getInt(0);
                                    long headerLen = buffer.getLong(4);
                                    long bodyLen = buffer.getLong(12);
                                    if (magic != Packet.magic) {
                                        log.debug("server: magic mismatch %d, ignoring the data", magic);
                                        buffer.clear();
                                        break;
                                    }
                                    long packetLen = headerLen + bodyLen + 20;
                                    log.debug("server:pos=%d packetLen=%d", pos, packetLen);
                                    if (pos >= packetLen) {
                                        buffer.position((int)packetLen);
                                        ByteBuffer newBuffer = buffer.slice();
                                        newBuffer.position((int) (pos - packetLen));
                                        // attach the new buffer
                                        key.attach(newBuffer);
                                        // process current packet
                                        buffer.flip();
                                        buffer.position((int)packetLen);
                                        checkBuffer(buffer);
                                        buffer = newBuffer;
                                        pos = buffer.position();
                                        continue;
                                    } else {
                                        break;
                                    }
                                }
                                if (buffer.limit() == buffer.position()) {
                                    //increase the buffer size
                                    ByteBuffer buf = ByteBuffer.allocate(Math.max(2 * buffer.limit(), Packet.maxMessageSize));
                                    key.attach(buf);
                                    if (buffer.position() > 0) {
                                        byte[] data = new byte[buffer.position()];
                                        buffer.flip();
                                        buffer.get(data);
                                        buf.put(data);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOChannelException e) {
                log.warn("exception while retrieving the message", e);
                switch (e.getCode()) {
                case ChannelClosed:
                    try {
                        key.channel().close();
                    } catch (IOException ignore) {
                    }
                    key.cancel();
                    break;
                }
                ByteBuffer buffer = (ByteBuffer)key.attachment();
                if (buffer != null) {
                    buffer.clear();
                }
            } catch (ClosedSelectorException ignore) {
                // ignore since we are shutting down
            } catch (Throwable e) {
                log.warn("exception while retrieving the message", e);
                if (e instanceof OutOfMemoryError) {
                    break;
                }
            }
        }
        log.info("server:shutting down");
    }

    private void checkBuffer(ByteBuffer buffer) throws IOChannelException {
        log.debug("server:checking buffer" + buffer.toString());
        Packet p = Packet.fromBuffer(buffer);
        while (p != null) {
            try {
                callback.dataReceived(p.getHeader(), p.getContent());
            } catch (IOException e) {
                log.warn(e);
                buffer.clear();
                return;
            }
            p = Packet.fromBuffer(buffer);
        }
    }

    private static Log log = LogFactory.getLog("iochannel");

    private Thread serverThread;
    private boolean shutdown;
    private NotifyCallback callback;

    private final Selector selector;
    private final ServerSocketChannel sschannel;
    private final Config config;
}
