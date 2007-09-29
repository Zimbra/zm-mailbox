/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.servlet;

import java.util.Date;

import javax.naming.directory.DirContext;
import javax.servlet.http.HttpServlet;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.httpclient.EasySSLProtocolSocketFactory;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.znative.Process;
import com.zimbra.znative.Util;

/**
 * Bind to all necessary privileged ports and then drop privilege.
 */
public class PrivilegedServlet extends HttpServlet {

    private static final long serialVersionUID = -1660545976482412029L;

    private static final int CHECK_LDAP_SLEEP_MILLIS = 10000;

    private static void checkLDAP() {
        while (true) {
            DirContext ctxt = null;
            try {
                ctxt = LdapUtil.getDirContext();
                return;
            } catch (ServiceException e) {
                System.err.println(new Date() + ": error communicating with LDAP (will retry)");
                e.printStackTrace();
                try {
                    Thread.sleep(CHECK_LDAP_SLEEP_MILLIS);
                } catch (InterruptedException ie) {
                }
            } finally {
                LdapUtil.closeContext(ctxt);
            }
        }
    }
    
    public void init() {
        Server server;
        int port;
        String address;
        try {
            if (LC.ssl_allow_untrusted_certs.booleanValue())
                EasySSLProtocolSocketFactory.init();

            System.setProperty("javax.net.ssl.keyStore", LC.tomcat_keystore.value());
            System.setProperty("javax.net.ssl.keyStorePassword", LC.tomcat_keystore_password.value());
            System.setProperty("javax.net.ssl.trustStorePassword", LC.tomcat_truststore_password.value());

            if (Provisioning.getInstance() instanceof LdapProvisioning)
                checkLDAP();

            server = Provisioning.getInstance().getLocalServer();

            if (server.getBooleanAttr(Provisioning.A_zimbraPop3ServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraPop3BindPort, Config.D_POP3_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraPop3BindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraPop3BindOnStartup, port < 1024)) {
            		NetUtil.bindTcpServerSocket(address, port);
            	}
            }

            if (server.getBooleanAttr(Provisioning.A_zimbraPop3SSLServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraPop3SSLBindPort, Config.D_POP3_SSL_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraPop3SSLBindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraPop3SSLBindOnStartup, port < 1024)) {
            		NetUtil.bindSslTcpServerSocket(address, port);
            	}
            }

            boolean ozImap = LC.get("nio_imap_enable").equalsIgnoreCase("true");

            if (server.getBooleanAttr(Provisioning.A_zimbraImapServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraImapBindPort, Config.D_IMAP_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraImapBindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraImapBindOnStartup, port < 1024)) {
                    if (ozImap) {
                        NetUtil.bindOzServerSocket(address, port);
                    } else {
                        NetUtil.bindTcpServerSocket(address, port);
                    }
            	}
            }

            if (server.getBooleanAttr(Provisioning.A_zimbraImapSSLServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraImapSSLBindPort, Config.D_IMAP_SSL_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraImapSSLBindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraImapSSLBindOnStartup, port < 1024)) {
                    if (ozImap) {
                        NetUtil.bindOzServerSocket(address, port);
                    } else {
                        NetUtil.bindSslTcpServerSocket(address, port);
                    }
            	}
            }

            port = server.getIntAttr(Provisioning.A_zimbraLmtpBindPort, Config.D_LMTP_BIND_PORT);
            address = server.getAttr(Provisioning.A_zimbraLmtpBindAddress, null);
            if (server.getBooleanAttr(Provisioning.A_zimbraLmtpBindOnStartup, port < 1024)) {
                NetUtil.bindTcpServerSocket(address, port);
            }
            
            if (Process.geteuid() == 0) {
                String user = LC.zimbra_user.value();
                int uid = LC.zimbra_uid.intValue();
                int gid = LC.zimbra_gid.intValue();
                System.err.println("Zimbra server process is running as root, changing to user=" + user + " uid=" + uid + " gid=" + gid);
                Process.setPrivileges(user, uid, gid);
                System.err.println("Zimbra server process, after change, is running with uid=" + Process.getuid() + " euid=" + Process.geteuid() + " gid=" + Process.getgid() + " egid=" + Process.getegid());
            } else {
                System.err.println("Zimbra server process is not running as root");
            }
            
            if (Process.getuid() == 0) {
                Util.halt("can not start server with uid of 0");
            }
            if (Process.geteuid() == 0) {
                Util.halt("can not start server with effective uid of 0");
            }
            if (Process.getgid() == 0) {
                Util.halt("can not start server with gid of 0");
            }
            if (Process.getegid() == 0) {
                Util.halt("can not start server with effective gid of 0");
            }
            
            synchronized (mInitializedCondition) {
                mInitialized = true;
                mInitializedCondition.notifyAll();
            }
        } catch (Throwable t) {
            Util.halt("PrivilegedServlet init failed", t);
        }
    }
    
    private static boolean mInitialized = false;

    private static Object mInitializedCondition = new Object(); 
        
    public static void waitForInitialization() {
        synchronized (mInitializedCondition) {
            while (!mInitialized) {
                try {
                    mInitializedCondition.wait();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}
