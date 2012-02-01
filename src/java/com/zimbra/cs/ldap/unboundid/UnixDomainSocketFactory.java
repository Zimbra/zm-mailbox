/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.ldap.unboundid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import javax.net.SocketFactory;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.zimbra.common.localconfig.LC;

public class UnixDomainSocketFactory extends SocketFactory {

    /*
     * address and port are not used.
     */
    @Override
    public Socket createSocket(String address, int port) throws IOException,
            UnknownHostException {
        return new UnixDomainSocket();
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(String address, int port, InetAddress localAddress, int localPort)
            throws IOException, UnknownHostException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    
    private static class UnixDomainSocket extends Socket {
        // "/opt/zimbra/openldap-2.4.28.5z/var/run/ldapi";
        private static final String LDAPI_SOCKET_FILE = LC.ldap_ldapi_socket_file.value(); 
        
        // wrap, instead of extends AFUNIXSocket because AFUNIXSocket does not have
        // an accessible constructor
        private AFUNIXSocket socket;
        
        private UnixDomainSocket() throws IOException {
            socket = AFUNIXSocket.newInstance();
            AFUNIXSocketAddress endpoint = new AFUNIXSocketAddress(new File(LDAPI_SOCKET_FILE));
            socket.connect(endpoint);
        }
        
        @Override
        public void bind(SocketAddress bindpoint) throws IOException {
            socket.bind(bindpoint);
        }
        
        @Override
        public void close() throws IOException {
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
            return socket.getInputStream();
        }
        
        @Override
        public  boolean getKeepAlive() throws SocketException {
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
            return socket.getOutputStream();
        }
        
        @Override
        public int getPort() {
            return socket.getPort();
        }
        
        @Override
        public int getReceiveBufferSize() throws SocketException {
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
        public int getSendBufferSize() throws SocketException {
            return socket.getSendBufferSize();
        }
        
        @Override
        public int getSoLinger() throws SocketException {
            return socket.getSoLinger();
        }
        
        @Override
        public int getSoTimeout() throws SocketException {
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
        public void setReceiveBufferSize(int size) throws SocketException {
            socket.setReceiveBufferSize(size);
        }
        
        @Override
        public void setReuseAddress(boolean on) throws SocketException {
            /*
             * unboundid (LDAPConnectionInternals.java:159) always invoke this method, 
             * this option is not supported by AFUNIXSocketImpl. 
             * (org.newsclub.net.unix.AFUNIXSocketException: Unsupported option: 4) 
             * 
             * Override to just make this method a noop.
             */
            // socket.setReuseAddress(on);
        }
        
        @Override
        public void setSendBufferSize(int size) throws SocketException {
            socket.setSendBufferSize(size);
        }
        
        @Override
        public void setSoLinger(boolean on, int linger) throws SocketException {
            socket.setSoLinger(on, linger);
        }
        
        @Override
        public void setSoTimeout(int timeout) throws SocketException {
            socket.setSoTimeout(timeout);
        }
        
        @Override
        public void setTcpNoDelay(boolean on) throws SocketException {
            /*
             * unboundid (LDAPConnectionInternals.java:162) always invoke this method, 
             * this option is not supported by AFUNIXSocketImpl. 
             * (org.newsclub.net.unix.AFUNIXSocketException: Unsupported socket option) 
             * 
             * Override to just make this method a noop.
             */
            // socket.setTcpNoDelay(on);
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

}
