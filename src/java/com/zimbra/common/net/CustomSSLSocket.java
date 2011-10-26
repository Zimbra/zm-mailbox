/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import com.zimbra.common.localconfig.LC;

class CustomSSLSocket extends SSLSocket {
    private CustomSSLSocketFactory factory;
    private SSLSocket sslSocket;
    private Socket socket;
    private String host;
    private boolean isHandshakeStarted;

    // SSLSocket settings which may be deferred if using wrapped socket
    private List<HandshakeCompletedListener> listeners;
    private Boolean enableSessionCreation;
    private String[] enabledCipherSuites;
    private String[] enabledProtocols;
    private Boolean useClientMode;
    private Boolean needClientAuth;
    private Boolean wantClientAuth;

    private static final ThreadLocal<String> threadLocal = new ThreadLocal<String>();

    static String getCertificateHostname() {
        return threadLocal.get();
    }

    CustomSSLSocket(CustomSSLSocketFactory factory, SSLSocket sslSocket, String host) {
        this.factory = factory;
        this.sslSocket = sslSocket;
        this.host = host;
    }

    CustomSSLSocket(CustomSSLSocketFactory factory, Socket socket) {
        this.factory = factory;
        this.socket = socket;
    }

    private String getHostname() {
        if (host == null)
            host = ((InetSocketAddress) sslSocket.getRemoteSocketAddress()).getHostName();
        return host;
    }

    //Overriding SSLSocket

    @Override
    public void startHandshake() throws IOException {
        SSLSocket sock = sslSocket();
        
        if (isHandshakeStarted)
            return;
        else
            isHandshakeStarted = true;

        if (sock.getSoTimeout() == 0)
            sock.setSoTimeout(LC.socket_so_timeout.intValue());

        threadLocal.set(getHostname());
        try {
            sock.startHandshake();
        } catch (IOException x) {
            try {
                sock.close();
            } catch (Exception e) {}
            throw x;
        } finally {
            threadLocal.remove();
        }

        if (factory.isVerifyHostname()) {
            CustomHostnameVerifier.verifyHostname(getHostname(), sock.getSession());
        }
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (sslSocket != null) {
            sslSocket.addHandshakeCompletedListener(listener);
        } else {
            if (listeners == null) {
                listeners = new ArrayList<HandshakeCompletedListener>();
            }
            listeners.add(listener);
        }
    }

    @Override
    public boolean getEnableSessionCreation() {
        if (sslSocket != null) {
            return sslSocket.getEnableSessionCreation();
        }
        if (enableSessionCreation == null) {
            enableSessionCreation = true;
        }
        return enableSessionCreation;
    }

    @Override
    public String[] getEnabledCipherSuites() {
        if (sslSocket != null) {
            return sslSocket.getEnabledCipherSuites();
        }
        if (enabledCipherSuites == null) {
            enabledCipherSuites = sampleSSLSocket().getEnabledCipherSuites();
        }
        return enabledCipherSuites;
    }

    @Override
    public String[] getEnabledProtocols() {
        if (sslSocket != null) {
            return sslSocket.getEnabledProtocols();
        }
        if (enabledProtocols == null) {
            enabledProtocols = sampleSSLSocket().getEnabledProtocols();
        }
        return enabledProtocols;
    }

    @Override
    public boolean getNeedClientAuth() {
        if (sslSocket != null) {
            return sslSocket.getNeedClientAuth();
        }
        if (needClientAuth == null) {
            needClientAuth = sampleSSLSocket().getNeedClientAuth();
        }
        return needClientAuth;
    }

    @Override
    public SSLSession getSession() {
        if (sslSocket != null) {
            return sslSocket.getSession();
        }
        return sampleSSLSocket().getSession();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        if (sslSocket != null) {
            return sslSocket.getSupportedCipherSuites();
        }
        return sampleSSLSocket().getSupportedProtocols();
    }

    @Override
    public String[] getSupportedProtocols() {
        if (sslSocket != null) {
            return sslSocket.getSupportedProtocols();
        }
        return sampleSSLSocket().getSupportedProtocols();
    }

    @Override
    public boolean getUseClientMode() {
        if (sslSocket != null) {
            return sslSocket.getUseClientMode();
        }
        if (useClientMode == null) {
            useClientMode = sampleSSLSocket().getUseClientMode();
        }
        return useClientMode;
    }

    @Override
    public boolean getWantClientAuth() {
        if (sslSocket != null) {
            return sslSocket.getWantClientAuth();
        }
        if (wantClientAuth == null) {
            wantClientAuth = sampleSSLSocket().getWantClientAuth();
        }
        return wantClientAuth;
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (sslSocket != null) {
            sslSocket.removeHandshakeCompletedListener(listener);
        } else if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        if (sslSocket != null) {
            sslSocket.setEnableSessionCreation(flag);
        } else {
            enableSessionCreation = flag;
        }
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        if (sslSocket != null) {
            sslSocket.setEnabledCipherSuites(suites);
        } else {
            enabledCipherSuites = suites;
        }
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        if (sslSocket != null) {
            sslSocket.setEnabledProtocols(protocols);
        } else {
            enabledProtocols = protocols;
        }
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        if (sslSocket != null) {
            sslSocket.setNeedClientAuth(need);
        } else {
            needClientAuth = need;
        }
    }

    @Override
    public void setUseClientMode(boolean mode) {
        if (sslSocket != null) {
            sslSocket.setUseClientMode(mode);
        } else {
            useClientMode = mode;
        }
    }

    @Override
    public void setWantClientAuth(boolean want) {
        if (sslSocket != null) {
            sslSocket.setWantClientAuth(want);
        } else {
            wantClientAuth = want;
        }
    }

    //Overriding Socket

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        socket().bind(bindpoint);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            sslSocket().close();
        }
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, LC.socket_connect_timeout.intValue());
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (sslSocket != null) {
            sslSocket.connect(endpoint, timeout);
        } else {
            socket.connect(endpoint, timeout);
            host = ((InetSocketAddress) endpoint).getHostName();
            sslSocket = wrap(socket);
        }
    }

    private SSLSocket wrap(Socket sock) throws IOException {
        sslSocket = factory.wrap(sock);
        if (listeners != null) {
            for (HandshakeCompletedListener listener : listeners) {
                sslSocket.addHandshakeCompletedListener(listener);
            }
        }
        if (enableSessionCreation != null) {
            sslSocket.setEnableSessionCreation(enableSessionCreation);
        }
        if (enabledCipherSuites != null) {
            sslSocket.setEnabledCipherSuites(enabledCipherSuites);
        }
        if (enabledProtocols != null) {
            sslSocket.setEnabledProtocols(enabledProtocols);
        }
        if (useClientMode != null) {
            sslSocket.setUseClientMode(useClientMode);
        }
        if (needClientAuth != null) {
            sslSocket.setNeedClientAuth(needClientAuth);
        }
        if (wantClientAuth != null) {
            sslSocket.setWantClientAuth(wantClientAuth);
        }
        return sslSocket;
    }

    @Override
    public SocketChannel getChannel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetAddress getInetAddress() {
        return socket().getInetAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream in = sslSocket().getInputStream();
        startHandshake();
        return in;
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return socket().getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
        return socket().getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return socket().getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return socket().getLocalSocketAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return socket().getOOBInline();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        OutputStream out = sslSocket().getOutputStream();
        startHandshake();
        return out;
    }

    @Override
    public int getPort() {
        return socket().getPort();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return socket().getReceiveBufferSize();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return socket().getRemoteSocketAddress();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return socket().getReuseAddress();
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return socket().getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return socket().getSoLinger();
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return socket().getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return socket().getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return socket().getTrafficClass();
    }

    @Override
    public boolean isBound() {
        return socket().isBound();
    }

    @Override
    public boolean isClosed() {
        return socket().isClosed();
    }

    @Override
    public boolean isConnected() {
        return socket().isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return socket().isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return socket().isOutputShutdown();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        socket().sendUrgentData(data);
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        socket().setKeepAlive(on);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        socket().setOOBInline(on);
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        socket().setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        socket().setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        socket().setReuseAddress(on);
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        socket().setSendBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        socket().setSoLinger(on, linger);
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        socket().setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        socket().setTcpNoDelay(on);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        socket().setTrafficClass(tc);
    }

    // Not supported in SSLSocket
    
    @Override
    public void shutdownInput() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdownOutput() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return socket().toString();
    }

    private SSLSocket sslSocket() throws IOException {
        if (sslSocket == null) {
            throw new IOException("Not connected");
        }
        return sslSocket;
    }
    
    private SSLSocket sampleSSLSocket() {
        return factory.getSampleSSLSocket();
    }
    
    private Socket socket() {
        return sslSocket != null ? sslSocket : socket;
    }
}
