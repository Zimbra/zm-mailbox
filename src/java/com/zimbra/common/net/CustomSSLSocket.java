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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import sun.security.util.HostnameChecker;

import com.zimbra.common.localconfig.LC;

public class CustomSSLSocket extends SSLSocket {
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<String>();

    static String getCertificateHostname() {
        return threadLocal.get();
    }

    private static java.security.cert.X509Certificate certJavax2Java(javax.security.cert.X509Certificate cert) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(cert.getEncoded());
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate) cf.generateCertificate(bis);
        } catch (java.security.cert.CertificateEncodingException e) {
        } catch (javax.security.cert.CertificateEncodingException e) {
        } catch (java.security.cert.CertificateException e) {
        }
        return null;
    }

    private static void verifyHostname(String hostname, SSLSession session) throws IOException {
        
        if (LC.ssl_allow_mismatched_certs.booleanValue())
            return;

        try {
            InetAddress.getByName(hostname);
        } catch (UnknownHostException uhe) {
            throw new UnknownHostException("Could not resolve SSL sessions server hostname: " + hostname);
        }

        javax.security.cert.X509Certificate[] certs = session.getPeerCertificateChain();
        if (certs == null || certs.length == 0)
            throw new SSLPeerUnverifiedException("No server certificates found: " + hostname);

        X509Certificate cert = certJavax2Java(certs[0]);

        CustomTrustManager ctm;
        try {
            ctm = CustomTrustManager.getInstance();
        } catch (GeneralSecurityException e) {
            throw new IOException("CustomTrustManager not found", e);
        }

        if (ctm.isCertificateAcceptedForHostname(hostname, cert))
            return;

        HostnameChecker hc = HostnameChecker.getInstance(HostnameChecker.TYPE_TLS);
        try {
            hc.match(hostname, cert);
        } catch (CertificateException x) {
            String certInfo = ctm.handleCertificateCheckFailure(hostname, cert, true);
            throw new SSLPeerUnverifiedException(certInfo);
        }
    }

    private final SSLSocket socket;
    private String host;
    private final boolean verifyHostname;
    private boolean isHandshakeStarted;

    public CustomSSLSocket(SSLSocket socket, String host, boolean verifyHostname) {
        this.socket = socket;
        this.host = host;
        this.verifyHostname = verifyHostname;
    }

    private String getHostname() {
        if (host == null)
            host = ((InetSocketAddress) socket.getRemoteSocketAddress()).getHostName();
        return host;
    }

    //Overriding SSLSocket

    @Override
    public void startHandshake() throws IOException {
        if (isHandshakeStarted)
            return;
        else
            isHandshakeStarted = true;

        if (socket.getSoTimeout() <= 0)
            socket.setSoTimeout(60000);

        threadLocal.set(getHostname());
        try {
            socket.startHandshake();
        } catch (IOException x) {
            try {
                socket.close();
            } catch (Exception e) {}
            throw x;
        } finally {
            threadLocal.remove();
        }

        if (verifyHostname)
            verifyHostname(getHostname(), socket.getSession());
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        socket.addHandshakeCompletedListener(listener);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return socket.getEnableSessionCreation();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return socket.getEnabledCipherSuites();
    }

    @Override
    public String[] getEnabledProtocols() {
        return socket.getEnabledProtocols();
    }

    @Override
    public boolean getNeedClientAuth() {
        return socket.getNeedClientAuth();
    }

    @Override
    public SSLSession getSession() {
        return socket.getSession();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return socket.getSupportedCipherSuites();
    }

    @Override
    public String[] getSupportedProtocols() {
        return socket.getSupportedProtocols();
    }

    @Override
    public boolean getUseClientMode() {
        return socket.getUseClientMode();
    }

    @Override
    public boolean getWantClientAuth() {
        return socket.getWantClientAuth();
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        socket.removeHandshakeCompletedListener(listener);
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        socket.setEnableSessionCreation(flag);
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        socket.setEnabledCipherSuites(suites);
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        socket.setEnabledProtocols(protocols);
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        socket.setNeedClientAuth(need);
    }

    @Override
    public void setUseClientMode(boolean mode) {
        socket.setUseClientMode(mode);
    }

    @Override
    public void setWantClientAuth(boolean want) {
        socket.setWantClientAuth(want);
    }

    //Overriding Socket

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        socket.bind(bindpoint);
    }

    @Override
    public synchronized void close() throws IOException {
        socket.close();
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        socket.connect(endpoint);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        socket.connect(endpoint, timeout);
    }

    @Override
    public SocketChannel getChannel() {
        return socket.getChannel();
    }

    @Override
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream in = socket.getInputStream();
        startHandshake();
        return in;
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return socket.getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return socket.getOOBInline();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        OutputStream out = socket.getOutputStream();
        startHandshake();
        return out;
    }

    @Override
    public int getPort() {
        return socket.getPort();
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return socket.getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return socket.getSoLinger();
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return socket.getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return socket.getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return socket.getTrafficClass();
    }

    @Override
    public boolean isBound() {
        return socket.isBound();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        socket.sendUrgentData(data);
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        socket.setKeepAlive(on);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        socket.setOOBInline(on);
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        socket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        socket.setSoLinger(on, linger);
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        socket.setTcpNoDelay(on);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        socket.setTrafficClass(tc);
    }

    @Override
    public void shutdownInput() throws IOException {
        socket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }

    @Override
    public String toString() {
        return socket.toString();
    }
}
