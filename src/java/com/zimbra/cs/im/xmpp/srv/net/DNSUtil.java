/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.net;

import com.zimbra.cs.im.xmpp.util.Log;

import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.DirContext;
import java.util.Hashtable;

/**
 * Utilty class to perform DNS lookups for XMPP services.
 *
 * @author Matt Tucker
 */
public class DNSUtil {

    private static DirContext context;

    static {
        try {
            Hashtable<String,String> env = new Hashtable<String,String>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            context = new InitialDirContext(env);
        }
        catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * Returns the host name and port that the specified XMPP server can be
     * reached at for server-to-server communication. A DNS lookup for a SRV
     * record in the form "_xmpp-server._tcp.example.com" is attempted, according
     * to section 14.4 of RFC 3920. If that lookup fails, a lookup in the older form
     * of "_jabber._tcp.example.com" is attempted since servers that implement an
     * older version of the protocol may be listed using that notation. If that
     * lookup fails as well, it's assumed that the XMPP server lives at the
     * host resolved by a DNS lookup at the specified domain on the specified default port.<p>
     *
     * As an example, a lookup for "example.com" may return "im.example.com:5269".
     *
     * @param domain the domain.
     * @param defaultPort default port to return if the DNS look up fails.
     * @return a HostAddress, which encompasses the hostname and port that the XMPP
     *      server can be reached at for the specified domain.
     */
    public static HostAddress resolveXMPPServerDomain(String domain, int defaultPort) {
        if (context == null) {
            return new HostAddress(domain, defaultPort);
        }
        String host = domain;
        int port = defaultPort;
        try {
            Attributes dnsLookup = context.getAttributes("_xmpp-server._tcp." + domain);
            String srvRecord = (String)dnsLookup.get("SRV").get();
            String [] srvRecordEntries = srvRecord.split(" ");
            port = Integer.parseInt(srvRecordEntries[srvRecordEntries.length-2]);
            host = srvRecordEntries[srvRecordEntries.length-1];
        }
        catch (Exception e) {
            // Attempt lookup with older "jabber" name.
            try {
                Attributes dnsLookup = context.getAttributes("_jabber._tcp." + domain);
                String srvRecord = (String)dnsLookup.get("SRV").get();
                String [] srvRecordEntries = srvRecord.split(" ");
                port = Integer.parseInt(srvRecordEntries[srvRecordEntries.length-2]);
                host = srvRecordEntries[srvRecordEntries.length-1];
            }
            catch (Exception e2) { }
        }
        // Host entries in DNS should end with a ".".
        if (host.endsWith(".")) {
            host = host.substring(0, host.length()-1);
        }
        return new HostAddress(host, port);
    }

    /**
     * Encapsulates a hostname and port.
     */
    public static class HostAddress {

        private String host;
        private int port;

        private HostAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }

        /**
         * Returns the hostname.
         *
         * @return the hostname.
         */
        public String getHost() {
            return host;
        }

        /**
         * Returns the port.
         *
         * @return the port.
         */
        public int getPort() {
            return port;
        }

        public String toString() {
            return host + ":" + port;
        }
    }
}