/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package org.apache.mina.transport.socket.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.core.polling.AbstractPollingIoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioProcessor;

/**
 * Zimbra patched version of {@code NioSocketAcceptor}.
 * <p>
 * The original {@code NioSocketAcceptor} does not accept a pre-bound server socket when starting a new listener. This
 * is necessary in order for us to pre-bind server socket channel to privileged port as {@code root} using Jetty's
 * {@code setuid} extension before starting the server. To address this, this implementation accepts a pre-bound
 * {@link ServerSocketChannel}.
 * <p>
 * Since this implementation needs to access package private classes of {@code org.apache.mina.transport.socket.nio},
 * this class is located in the same package. To avoid security check by JRE, this class needs to be loaded by the same
 * class loader as {@code mina-*.jar}, i.e. do not put {@code mina-*.jar} in {@code jetty/common/}.
 *
 * @author ysasaki
 */
public final class ZimbraSocketAcceptor extends AbstractPollingIoAcceptor<NioSession, ServerSocketChannel>
        implements SocketAcceptor {

    private volatile Selector selector;
    private final ServerSocketChannel channel;

    public ZimbraSocketAcceptor(ServerSocketChannel channel) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
        this.channel = channel;
    }

    public ZimbraSocketAcceptor(ServerSocketChannel channel, int processorCount) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
        this.channel = channel;
    }

    public ZimbraSocketAcceptor(ServerSocketChannel channel, IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
        this.channel = channel;
    }

    public ZimbraSocketAcceptor(ServerSocketChannel channel, Executor executor, IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), executor, processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
        this.channel = channel;
    }

    @Override
    protected void init() throws Exception {
        selector = Selector.open();
    }

    @Override
    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }

    @Override
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) super.getSessionConfig();
    }


    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }

    @Override
    public InetSocketAddress getDefaultLocalAddress() {
        return (InetSocketAddress) super.getDefaultLocalAddress();
    }

    @Override
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        setDefaultLocalAddress((SocketAddress) localAddress);
    }

    @Override
    public boolean isReuseAddress() {
        try {
            return channel.socket().getReuseAddress();
        } catch (SocketException e) {
            return false;
        }
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBacklog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBacklog(int backlog) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected NioSession accept(IoProcessor<NioSession> processor, ServerSocketChannel handle) throws Exception {

        SelectionKey key = handle.keyFor(selector);

        if ((key == null) || (!key.isValid()) || (!key.isAcceptable()) ) {
            return null;
        }

        // accept the connection from the client
        SocketChannel ch = handle.accept();

        if (ch == null) {
            return null;
        }

        return new NioSocketSession(this, processor, ch);
    }

    @Override
    protected ServerSocketChannel open(SocketAddress localAddress) throws Exception {
        boolean success = false;
        try {
            if (channel.isBlocking()) {
                throw new IllegalStateException("ServerSocketChannel is blocking mode");
            }
            // Register the channel within the selector for ACCEPT event
            channel.register(selector, SelectionKey.OP_ACCEPT);
            success = true;
        } finally {
            if (!success) {
                close(channel);
            }
        }
        return channel;
    }

    @Override
    protected SocketAddress localAddress(ServerSocketChannel handle) throws Exception {
        return handle.socket().getLocalSocketAddress();
    }

    @Override
    protected int select() throws Exception {
        return selector.select();
    }

    @Override
    protected Iterator<ServerSocketChannel> selectedHandles() {
        return new ServerSocketChannelIterator(selector.selectedKeys());
    }

    @Override
    protected void close(ServerSocketChannel handle) throws Exception {
        SelectionKey key = handle.keyFor(selector);

        if (key != null) {
            key.cancel();
        }

        handle.close();
    }

    @Override
    protected void wakeup() {
        selector.wakeup();
    }

    private static class ServerSocketChannelIterator implements Iterator<ServerSocketChannel> {
        private final Iterator<SelectionKey> iterator;

        private ServerSocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            iterator = selectedKeys.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public ServerSocketChannel next() {
            SelectionKey key = iterator.next();

            if ( key.isValid() && key.isAcceptable() ) {
                return (ServerSocketChannel) key.channel();
            }

            return null;
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    @Override
    protected void init(SelectorProvider arg0) throws Exception {
        selector = Selector.open();
    }

}
