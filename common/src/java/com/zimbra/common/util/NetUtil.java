/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import com.zimbra.common.service.ServiceException;

public class NetUtil {

    public static ServerSocket getTcpServerSocket(String address, int port) throws ServiceException {
        return getServerSocket(address, port, false, false, null, null, null);
    }

    public static ServerSocket getSslTcpServerSocket(String address, int port, String[] excludeCiphers, String[] includeCiphers) throws ServiceException {
        return getServerSocket(address, port, true, /* doesn't matter, but keep it false always */ false, excludeCiphers, includeCiphers, null);
    }

    public static ServerSocket getSslTcpServerSocket(String address, int port, String[] excludeCiphers, String[] includeCiphers, String[] sslProtocols) throws ServiceException {
        return getServerSocket(address, port, true, /* doesn't matter, but keep it false always */ false, excludeCiphers, includeCiphers, sslProtocols);
    }

    public static ServerSocket getNioServerSocket(String address, int port) throws ServiceException {
        return getServerSocket(address, port, false, true, null, null, null);
    }

    public static void bindTcpServerSocket(String address, int port) throws IOException {
        bindServerSocket(address, port, false, false, null, null, null);
    }

    public static void bindSslTcpServerSocket(String address, int port, String[] excludeCiphers, String[] includeCiphers) throws IOException {
        bindServerSocket(address, port, true, /* doesn't matter, but it false always */ false, excludeCiphers, includeCiphers, null);
    }

    public static void bindSslTcpServerSocket(String address, int port, String[] excludeCiphers, String[] includeCiphers, String[] sslProtocols) throws IOException {
        bindServerSocket(address, port, true, /* doesn't matter, but it false always */ false, excludeCiphers, includeCiphers, sslProtocols);
    }

    public static void bindNioServerSocket(String address, int port) throws IOException {
        bindServerSocket(address, port, false, true, null, null, null);
    }

    public static synchronized ServerSocket getServerSocket(String address, int port, boolean ssl, boolean useChannels, String[] excludeCiphers, String[] includeCiphers) throws ServiceException {
        return getServerSocket(address, port, true, /* doesn't matter, but keep it false always */ false, excludeCiphers, includeCiphers, null);
    }

    public static synchronized ServerSocket getServerSocket(String address, int port, boolean ssl, boolean useChannels, String[] excludeCiphers, String[] includeCiphers, String[] sslProtocols) throws ServiceException {
        ServerSocket serverSocket = getAlreadyBoundServerSocket(address, port, ssl, useChannels);
        if (serverSocket != null) {
            return serverSocket;
        }
        try {
            serverSocket = newBoundServerSocket(address, port, ssl, useChannels, excludeCiphers, includeCiphers, sslProtocols);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Could not bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl + " useChannels=" + useChannels, ioe);
        }
        if (serverSocket == null) {
            throw ServiceException.FAILURE("Server socket null after bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl + " useChannels=" + useChannels, null);
        }
        return serverSocket;
    }

    private static ServerSocket newBoundServerSocket(String address, int port, boolean ssl, boolean useChannels,
            String[] excludeCiphers, String[] includeCiphers, String[] sslProtocols) throws IOException {
        ServerSocket serverSocket = null;
        InetAddress bindAddress = null;
        if (address != null && address.length() > 0) {
            bindAddress = InetAddress.getByName(address);
        }

        if (useChannels) {
            //for SSL channels, it's up to the selector to configure SSL stuff
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false); //I believe the only time we use channels is in NIO
            serverSocket = serverSocketChannel.socket();
        } else {
            if (ssl) {
                SSLServerSocketFactory fact = (SSLServerSocketFactory)
                SSLServerSocketFactory.getDefault();
                serverSocket = fact.createServerSocket();
                setSSLProtocols((SSLServerSocket)serverSocket, sslProtocols);
                setSSLEnabledCipherSuites((SSLServerSocket)serverSocket, excludeCiphers, includeCiphers);
            } else {
                serverSocket = new ServerSocket();
            }
        }

        serverSocket.setReuseAddress(true);
        InetSocketAddress isa = new InetSocketAddress(bindAddress, port);
        serverSocket.bind(isa, 1024);
        return serverSocket;
    }

    private static void setSSLProtocols(SSLServerSocket socket, String[] sslProtocols) {
        if (sslProtocols != null && sslProtocols.length > 0) {
            socket.setEnabledProtocols(sslProtocols);
        }
    }

    public static void setSSLProtocols(SSLSocket socket, String[] sslProtocols) {
        if (sslProtocols != null && sslProtocols.length > 0) {
            socket.setEnabledProtocols(sslProtocols);
        }
    }

    /**
     *
     * @param enabledCiphers
     * @param excludeCiphers
     * @return Array of enabled cipher after excluding the unwanted ones
     *         null if either enabledCiphers or excludeCiphers are null or empty.  Callers should not
     *         alter the default ciphers on the SSL socket/engine if computeEnabledCipherSuites returns null.
     */
    public static String[] computeEnabledCipherSuites(String[] enabledCiphers, String[] excludeCiphers) {
        if (enabledCiphers == null || enabledCiphers.length == 0 ||
            excludeCiphers == null || excludeCiphers.length == 0)
            return null;

        List<String> excludedCSList = new ArrayList<String>(Arrays.asList(excludeCiphers));
        List<String> enabledCSList = new ArrayList<String>(Arrays.asList(enabledCiphers));

        for (String cipher : excludedCSList) {
            if (enabledCSList.contains(cipher))
                enabledCSList.remove(cipher);
        }

        return enabledCSList.toArray(new String[enabledCSList.size()]);
    }

    private static void setSSLEnabledCipherSuites(SSLServerSocket socket, String[] excludeCiphers, String[] includeCiphers) {
        String[] enabledCiphers = computeEnabledCipherSuites(includeCiphers != null && includeCiphers.length > 0 ? includeCiphers : socket.getEnabledCipherSuites(), excludeCiphers);
        if (enabledCiphers != null)
            socket.setEnabledCipherSuites(enabledCiphers);
    }

    public static void setSSLEnabledCipherSuites(SSLSocket socket, String[] excludeCiphers, String[] includeCiphers) {
        String[] enabledCiphers = computeEnabledCipherSuites(includeCiphers != null && includeCiphers.length > 0 ? includeCiphers : socket.getEnabledCipherSuites(), excludeCiphers);
        if (enabledCiphers != null)
            socket.setEnabledCipherSuites(enabledCiphers);
    }

    private static Map<String, ServerSocket> mBoundSockets = new HashMap<String, ServerSocket>();

    private static String makeKey(String address, int port, boolean ssl, boolean useChannels) {
        return "[ssl=" + ssl + ";addr=" + address + ";port=" + port + ";useChannels=" + useChannels + "]";
    }

    public static void dumpMap() {
        for (Iterator<Map.Entry<String, ServerSocket>> iter = mBoundSockets.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, ServerSocket> entry = iter.next();
            System.err.println(entry.getKey() + " => " + entry.getValue());
        }
    }

    public static synchronized void bindServerSocket(String address, int port, boolean ssl, boolean useChannels, String[] excludeCiphers, String[] includeCiphers) throws IOException {
        bindServerSocket(address, port, ssl, useChannels, excludeCiphers, includeCiphers, null );
    }

    public static synchronized void bindServerSocket(String address, int port, boolean ssl, boolean useChannels, String[] excludeCiphers, String[] includeCiphers, String[] sslProtocols) throws IOException {
        // Don't use log4j - when this code is called, log4j might not have been initialized
        // and we do not want to initialize log4j at this time because we are likely still
        // running as root.
        System.err.println("Zimbra server reserving server socket port=" + port + " bindaddr=" + address + " ssl=" + ssl);
        String key = makeKey(address, port, ssl, useChannels);
        ServerSocket serverSocket = NetUtil.newBoundServerSocket(address, port, ssl, useChannels, excludeCiphers, includeCiphers, sslProtocols);
        //System.err.println("put table=" + mBoundSockets.hashCode() + " key=" + key + " sock=" + serverSocket);
        mBoundSockets.put(key, serverSocket);
        //dumpMap();
    }

    private static ServerSocket getAlreadyBoundServerSocket(String address, int port, boolean ssl, boolean useChannels) {
        //dumpMap();
        String key = makeKey(address, port, ssl, useChannels);
        ServerSocket serverSocket = mBoundSockets.get(key);
        //System.err.println("get table=" + mBoundSockets.hashCode() + " key=" + key + " sock=" + serverSocket);
        return serverSocket;
    }

    public static void main(String[] args) {
        SSLServerSocketFactory sf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        String[] supportedCipherSuites = sf.getSupportedCipherSuites();
        System.out.println("\nsupported cipher suites:\n");
        for (String c : supportedCipherSuites)
            System.out.println(c);
    }

    /**
     * Determines if a target address falls within the specified subnet.<br>
     * If the prefix has no bit-length, determines direct match with target address.
     * @param targetAddress The address in question
     * @param prefix CIDR notation (first ip and number of relevant bits), or single ip - no wildcards
     * @return True if the address matches or is within subnet range
     */
    public static boolean isAddressInRange(InetAddress targetAddress, String prefix) {
        ZimbraLog.misc.debug("checking if ip: %s is in range of: %s", targetAddress, prefix);
        // split first ip from bit length
        String [] firstIpAndLength = prefix.split("/");
        // the first ip in the subnet
        InetAddress firstIp;
        // the number of relevant bits in the entire address
        int bitLength;
        try {
            firstIp = InetAddress.getByName(firstIpAndLength[0]);
            // compare direct if no bit length
            if (firstIpAndLength.length < 2) {
                return targetAddress.getHostAddress().equals(firstIp.getHostAddress());
            }
            bitLength = Integer.parseInt(firstIpAndLength[1]);
        } catch (UnknownHostException | NumberFormatException e) {
            ZimbraLog.misc.error("ignoring unparsable ip address prefix: %s", prefix);
            ZimbraLog.misc.debug(e);
            return false;
        }

        // don't compare across ipv4 vs ipv6
        if (!targetAddress.getClass().equals(firstIp.getClass())) {
            ZimbraLog.misc.debug("cannot compare across ipv4 and ipv6 address. target: %s, first ip: %s",
                targetAddress, firstIp);
            return false;
        }

        // determine number of relevant bytes to compare
        // e.g. /116 -> 116/8=14.5 -> 14 -> remaining bits handled below
        // e.g. /30 -> 30/8=3.75 -> 4 -> remaining bits handled below
        // e.g. /24 -> 24/8=3 -> 3
        int maskLength = bitLength / Byte.SIZE;

        // mask on and compare #maskLength bytes we care about
        byte mask = (byte) 0xFF;
        byte [] targetBytes = targetAddress.getAddress();
        byte [] subBytes = firstIp.getAddress();
        for (int i = 0; i < maskLength; i++) {
            if ((targetBytes[i] & mask) != (subBytes[i] & mask)) {
                return false;
            }
        }

        // the number of relevant bits in the last byte of the address
        int doCareLength = bitLength % Byte.SIZE;

        // last byte is only relevant for non-multiples of 8
        // last byte has all bits on except the don't cares specified by bit length
        // e.g. /30 -> 30%8=6 -> 8-6=2 -> last 2 bits are off
        // e.g. /29 -> 29%8=5 -> 8-5=3 -> last 3 bits are off
        if (doCareLength != 0) {
            // set on all bits
            byte lastByteMask = (byte) 0xFF;
            // set off the lowest bits remaining from a full byte
            int dontCareLength = Byte.SIZE - doCareLength;
            lastByteMask <<= dontCareLength;
            return (targetBytes[maskLength] & lastByteMask) == (subBytes[maskLength] & lastByteMask);
        }

        return true;
    }

}
