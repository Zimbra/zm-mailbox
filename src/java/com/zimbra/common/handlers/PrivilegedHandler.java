/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.handlers;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.NetUtil;

import java.util.Map;

/**
 * This is the class jetty will call back before setuid.
 * We will open sockets on privileged ports if configured to do so.
 * 
 * @author jjzhuang
 *
 */
public class PrivilegedHandler {
	
    private static final String A_zimbraLmtpBindPort = "zimbraLmtpBindPort";
    private static final String A_zimbraLmtpBindAddress = "zimbraLmtpBindAddress";
    private static final String A_zimbraImapBindPort = "zimbraImapBindPort";
    private static final String A_zimbraImapBindAddress = "zimbraImapBindAddress";
    private static final String A_zimbraImapServerEnabled = "zimbraImapServerEnabled";
    private static final String A_zimbraImapSSLBindPort = "zimbraImapSSLBindPort";
    private static final String A_zimbraImapSSLBindAddress = "zimbraImapSSLBindAddress";
    private static final String A_zimbraImapSSLServerEnabled = "zimbraImapSSLServerEnabled";
    private static final String A_zimbraLmtpServerEnabled = "zimbraLmtpServerEnabled";
    
    private static final String A_zimbraPop3BindPort = "zimbraPop3BindPort";
    private static final String A_zimbraPop3BindAddress = "zimbraPop3BindAddress";
    private static final String A_zimbraPop3ServerEnabled = "zimbraPop3ServerEnabled";
    private static final String A_zimbraPop3SSLBindPort = "zimbraPop3SSLBindPort";
    private static final String A_zimbraPop3SSLBindAddress = "zimbraPop3SSLBindAddress";
    private static final String A_zimbraPop3SSLServerEnabled = "zimbraPop3SSLServerEnabled";
    
    private static final String mailboxd_keystore = "mailboxd_keystore";
    private static final String mailboxd_keystore_password = "mailboxd_keystore_password";
    private static final String mailboxd_truststore_password = "mailboxd_truststore_password";
    
    private static final int D_LMTP_BIND_PORT = 7025;
    private static final int D_IMAP_BIND_PORT = 143;
    private static final int D_IMAP_SSL_BIND_PORT = 993;
    private static final int D_POP3_BIND_PORT = 110;
    private static final int D_POP3_SSL_BIND_PORT = 995;

    public static void openPorts(Map<String, Object> attributes) {
        int port;
        String address;
        boolean nio_enabled = LC.nio_enabled.booleanValue(); // TODO move this to ldap
        
        try {
        	
        	if (LC.zimbra_ssl_enabled.booleanValue()) { //default is true
        		System.setProperty("javax.net.ssl.keyStore", getAttr(attributes, mailboxd_keystore));
        		System.setProperty("javax.net.ssl.keyStorePassword", getAttr(attributes, mailboxd_keystore_password));
        		System.setProperty("javax.net.ssl.trustStorePassword", getAttr(attributes, mailboxd_truststore_password));
        	}
            
            if (getBooleanAttr(attributes, A_zimbraPop3ServerEnabled, false)) {
                port = getIntAttr(attributes, A_zimbraPop3BindPort, D_POP3_BIND_PORT);
                address = getAttr(attributes, A_zimbraPop3BindAddress, null);
                if (nio_enabled || LC.nio_pop3_enabled.booleanValue()) {
                    NetUtil.bindNioServerSocket(address, port);
                } else {
                    NetUtil.bindTcpServerSocket(address, port);
                }
            }

            if (getBooleanAttr(attributes, A_zimbraPop3SSLServerEnabled, false)) {
                port = getIntAttr(attributes, A_zimbraPop3SSLBindPort, D_POP3_SSL_BIND_PORT);
                address = getAttr(attributes, A_zimbraPop3SSLBindAddress, null);
                if (nio_enabled || LC.nio_pop3_enabled.booleanValue()) {
                    NetUtil.bindNioServerSocket(address, port);
                } else {
                    NetUtil.bindSslTcpServerSocket(address, port);
                }
            }

            if (getBooleanAttr(attributes, A_zimbraImapServerEnabled, false)) {
            	port = getIntAttr(attributes, A_zimbraImapBindPort, D_IMAP_BIND_PORT);
            	address = getAttr(attributes, A_zimbraImapBindAddress, null);
                if (nio_enabled || LC.nio_imap_enabled.booleanValue()) {
                    NetUtil.bindNioServerSocket(address, port);
                } else {
                    NetUtil.bindTcpServerSocket(address, port);
                }
            }

            if (getBooleanAttr(attributes, A_zimbraImapSSLServerEnabled, false)) {
            	port = getIntAttr(attributes, A_zimbraImapSSLBindPort, D_IMAP_SSL_BIND_PORT);
            	address = getAttr(attributes, A_zimbraImapSSLBindAddress, null);
                if (nio_enabled || LC.nio_imap_enabled.booleanValue()) {
                    NetUtil.bindNioServerSocket(address, port);
                } else {
                    NetUtil.bindSslTcpServerSocket(address, port);
                }
            }

            if (getBooleanAttr(attributes, A_zimbraLmtpServerEnabled, false)) {
            	port = getIntAttr(attributes, A_zimbraLmtpBindPort, D_LMTP_BIND_PORT);
            	address = getAttr(attributes, A_zimbraLmtpBindAddress, null);
            	if (nio_enabled || LC.nio_lmtp_enabled.booleanValue()) {
            		NetUtil.bindNioServerSocket(address, port);
            	} else {
            		NetUtil.bindTcpServerSocket(address, port);
            	}
            }
        } catch (Throwable t) {        	
            try {
            	System.err.println("Fatal error: exception while binding to ports");
            	t.printStackTrace(System.err);
            } finally {
                Runtime.getRuntime().halt(1);
            }
        }
	}
	
	private static String getAttr(Map<String, Object> attributes, String name) {
		String val = getAttr(attributes, name, null);
		if (val == null) {
			throw new RuntimeException("missing parameter " + name);
		}
		return val;
	}
	
	private static String getAttr(Map<String, Object> attributes, String name, String defaultValue) {
        Object v = attributes.get(name);
        String s = (String)v;
        if (s != null && s.equals("")) s = null; //null out empty string because jetty-setuid.xml may pass in ""
        return s == null ? defaultValue : s;
	}
	
	private static boolean getBooleanAttr(Map<String, Object> attributes, String name, boolean defaultValue) {
        Object v = attributes.get(name);
        return v == null ? defaultValue : ((Boolean)v).booleanValue();
	}

	private static int getIntAttr(Map<String, Object> attributes, String name, int defaultValue) {
        Object v = attributes.get(name);
        return v == null ? defaultValue : ((Integer)v).intValue();
	}
}
