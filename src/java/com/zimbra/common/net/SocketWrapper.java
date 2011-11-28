/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOptions;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.localconfig.LC;

class SocketWrapper extends Socket {
    private Socket sock;
    private SocketAddress bindpoint;
    private Map<Integer, Object> options;

    SocketWrapper() {}

    public void setDelegate(Socket sock) {
        this.sock = sock;
    }

    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, LC.socket_connect_timeout.intValue());
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (sock == null) {
            throw new IllegalStateException("No delegate socket");
        }
        if (sock.isConnected()) {
            return;
        }
        if (options != null) {
            for (Map.Entry<Integer, Object> e : options.entrySet()) {
                setOption(sock, e.getKey(), e.getValue());
            }
            options = null;
        }
        if (bindpoint != null) {
            sock.bind(bindpoint);
        }
        // Temporarily set SO_TIMEOUT to connect timeout if specified and
        // restore after connection is complete. Otherwise, we may hang during
        // SOCKS or SSL negotiation.
        if (timeout > 0) {
            int soTimeout = sock.getSoTimeout();
            sock.setSoTimeout(timeout);
            sock.connect(endpoint, timeout);
            sock.setSoTimeout(soTimeout);
        } else {
            sock.connect(endpoint, timeout);
        }
        if (sock.getSoTimeout() == 0) {
            sock.setSoTimeout(LC.socket_so_timeout.intValue());
        }
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        if (sock == null) {
            this.bindpoint = bindpoint;
        } else {
            sock.bind(bindpoint);
        }
    }

    public InetAddress getInetAddress() {
        return sock != null ? sock.getInetAddress() : null;
    }

    public InetAddress getLocalAddress() {
        return sock != null ? sock.getLocalAddress() : super.getLocalAddress();
    }

    public int getPort() {
        return sock != null ? sock.getPort() : 0;
    }

    public int getLocalPort() {
        return sock != null ? sock.getLocalPort() : -1;
    }

    public SocketAddress getRemoteSocketAddress() {
        return sock != null ? sock.getRemoteSocketAddress() : null;
    }

    public SocketAddress getLocalSocketAddress() {
        return sock != null ? sock.getLocalSocketAddress() : null;
    }

    public SocketChannel getChannel() {
        return sock != null ? sock.getChannel() : null;
    }

    public InputStream getInputStream() throws IOException {
        checkConnected();
        return sock.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        checkConnected();
        return sock.getOutputStream();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        if (sock != null) {
            sock.setTcpNoDelay(on);
        } else {
            setOption(SocketOptions.TCP_NODELAY, on);
        }
    }

    public boolean getTcpNoDelay() throws SocketException {
        return sock != null ?
            sock.getTcpNoDelay() : getBoolean(SocketOptions.TCP_NODELAY);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        if (sock != null) {
            sock.setSoLinger(on, linger);
        }
        if (!on) {
            linger = -1;
        } else if (linger < 0) {
            throw new IllegalArgumentException("invalid value for SO_LINGER");
        } else if (linger > 65535) {
            linger = 65535;
        }
        setOption(SocketOptions.SO_LINGER, linger);
    }

    public int getSoLinger() throws SocketException {
        return sock != null ?
            sock.getSoLinger() : getInt(SocketOptions.SO_LINGER);
    }

    public void sendUrgentData(int data) throws IOException {
        checkConnected();
        sock.sendUrgentData(data);
    }

    public void setOOBInline(boolean on) throws SocketException {
        if (sock != null) {
            sock.setOOBInline(on);
        } else {
            setOption(SocketOptions.SO_OOBINLINE, on);
        }
    }

    public boolean getOOBInline() throws SocketException {
        return sock != null ?
            sock.getOOBInline() : getBoolean(SocketOptions.SO_OOBINLINE);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        if (sock != null) {
            sock.setSoTimeout(timeout);
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout");
        } else {
            setOption(SocketOptions.SO_TIMEOUT, timeout);
        }
    }

    public int getSoTimeout() throws SocketException {
        return sock != null ?
            sock.getSoTimeout() : getInt(SocketOptions.SO_TIMEOUT);
    }

    public void setSendBufferSize(int size) throws SocketException {
        if (sock != null) {
            sock.setSendBufferSize(size);
        } else if (size <= 0) {
            throw new IllegalArgumentException("Invalid buffer size");
        } else {
            setOption(SocketOptions.SO_SNDBUF, size);
        }
    }

    public int getSendBufferSize() throws SocketException {
        return sock != null ?
            sock.getSendBufferSize() : getInt(SocketOptions.SO_SNDBUF);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        if (sock != null) {
            sock.setReceiveBufferSize(size);
        } else if (size <= 0) {
            throw new IllegalArgumentException("Invalid buffer size");
        } else {
            setOption(SocketOptions.SO_RCVBUF, size);
        }
    }

    public int getReceiveBufferSize() throws SocketException {
        return sock != null ?
            sock.getReceiveBufferSize() : getInt(SocketOptions.SO_RCVBUF);
    }

    public void setKeepAlive(boolean on) throws SocketException {
        if (sock != null) {
            sock.setKeepAlive(on);
        } else {
            setOption(SocketOptions.SO_KEEPALIVE, on);
        }
    }

    public boolean getKeepAlive() throws SocketException {
        return sock != null ?
            sock.getKeepAlive() : getBoolean(SocketOptions.SO_KEEPALIVE);
    }

    public void setTrafficClass(int tc) throws SocketException {
        if (sock != null) {
            sock.setTrafficClass(tc);
        } else if (tc < 0 || tc > 255) {
            throw new IllegalArgumentException("Invalid traffic class");
        } else {
            setOption(SocketOptions.IP_TOS, tc);
        }
    }

    public int getTrafficClass() throws SocketException {
        return sock != null ?
            sock.getTrafficClass() : getInt(SocketOptions.IP_TOS);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        if (sock != null) {
            sock.setReuseAddress(on);
        } else {
            setOption(SocketOptions.SO_REUSEADDR, on);
        }
    }

    public boolean getReuseAddress() throws SocketException {
        return sock != null ?
            sock.getReuseAddress() : getBoolean(SocketOptions.SO_REUSEADDR);
    }

    public void close() throws IOException {
        if (sock != null) {
            sock.close();
        }
    }

    public void shutdownInput() throws IOException {
        checkConnected();
        sock.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        checkConnected();
        sock.shutdownOutput();
    }

    public String toString() {
        return sock != null ? sock.toString() : "Socket[unconnected]";
    }

    public boolean isConnected() {
        return sock != null && sock.isConnected();
    }

    public boolean isBound() {
        return sock != null && sock.isBound();
    }

    public boolean isClosed() {
        return sock == null || sock.isClosed();
    }

    public boolean isInputShutdown() {
        return sock == null || sock.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return sock == null || sock.isOutputShutdown();
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        throw new UnsupportedOperationException();
    }

    private void checkConnected() throws SocketException {
        if (sock == null) {
            throw new SocketException("Socket is not connected");
        }
    }

    private void setOption(int id, Object val) {
        if (options == null) {
            options = new HashMap<Integer, Object>();
        }
        options.put(id, val);
    }

    private boolean getBoolean(int id) throws SocketException {
        Object val = getOption(id);
        return val instanceof Boolean ? ((Boolean) val) : false;
    }
    
    private Integer getInt(int id) throws SocketException {
        Object val = getOption(id);
        return val instanceof Integer ? ((Integer) val) : 0;
    }
    
    private Object getOption(int id) throws SocketException {
        if (options != null) {
            Object val = options.get(id);
            if (val != null) {
                return val;
            }
        }
        return getDefault(id);
    }

    private Object getDefault(int id) throws SocketException {
        switch (id) {
        case SocketOptions.TCP_NODELAY:
            return super.getTcpNoDelay();
        case SocketOptions.SO_LINGER:
            return super.getSoLinger();
        case SocketOptions.SO_OOBINLINE:
            return super.getOOBInline();
        case SocketOptions.SO_TIMEOUT:
            return super.getSoTimeout();
        case SocketOptions.SO_SNDBUF:
            return super.getSendBufferSize();
        case SocketOptions.SO_RCVBUF:
            return super.getReceiveBufferSize();
        case SocketOptions.SO_KEEPALIVE:
            return super.getKeepAlive();
        case SocketOptions.IP_TOS:
            return super.getTrafficClass();
        case SocketOptions.SO_REUSEADDR:
            return super.getReuseAddress();
        default:
            throw new AssertionError();
        }
    }

    private static void setOption(Socket sock, int id, Object value)
        throws SocketException {

        switch (id) {
        case SocketOptions.TCP_NODELAY:
            sock.setTcpNoDelay((Boolean) value);
            break;
        case SocketOptions.SO_LINGER:
            int linger = (Integer) value;
            sock.setSoLinger(linger > 0, linger);
            break;
        case SocketOptions.SO_OOBINLINE:
            sock.setOOBInline((Boolean) value);
            break;
        case SocketOptions.SO_TIMEOUT:
            sock.setSoTimeout((Integer) value);
            break;
        case SocketOptions.SO_SNDBUF:
            sock.setSendBufferSize((Integer) value);
            break;
        case SocketOptions.SO_RCVBUF:
            sock.setReceiveBufferSize((Integer) value);
            break;
        case SocketOptions.IP_TOS:
            sock.setTrafficClass((Integer) value);
            break;
        case SocketOptions.SO_REUSEADDR:
            sock.setReuseAddress((Boolean) value);
        default:
            throw new AssertionError();
        }
    }
}

