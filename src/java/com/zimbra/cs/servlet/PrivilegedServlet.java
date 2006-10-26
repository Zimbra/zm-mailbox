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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
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

    private void setupProviders() {
        // Setup provisioning provider
        Provisioning prov = null;
        String provClassName = getInitParameter("provisioning.provider");
        if (provClassName != null) {
            try {
                Class provClass = Class.forName(provClassName);
                prov = (Provisioning)provClass.newInstance();
            } catch (Exception e) {
                Util.halt("error instantiating provisioning provider " + provClassName + " " + e);
            }
        } else {
            prov = new LdapProvisioning();
        }
        Provisioning.setInstance(prov);
        System.out.println("INFO: using provisioning provider of type: " + prov.getClass().getName());

        // Setup mailbox provider
        MailboxManager mbxmgr = null;
        String mbxmgrClassName = getInitParameter("mailbox.provider");
        if (mbxmgrClassName != null) {
             try {
                 Class mbxmgrClass = Class.forName(mbxmgrClassName);
                 mbxmgr = (MailboxManager)mbxmgrClass.newInstance();
             } catch (Exception e) {
                 Util.halt("error instantiating mailbox provider " + mbxmgrClassName + " " + e);
             }
        } else {
            try {
                mbxmgr = new MailboxManager();
            } catch (ServiceException e) {
                Util.halt("error instantiating default mailbox manager: " + e);
                e.printStackTrace();
            } 
        }
        MailboxManager.setInstance(mbxmgr);
        System.out.println("INFO: using mailbox provider of type: " + mbxmgr.getClass().getName());
    }
    
    public void init() {
        Server server;
        int port;
        String address;
        try {
            setupProviders();
            
            if (LC.ssl_allow_untrusted_certs.booleanValue())
                EasySSLProtocolSocketFactory.init();

            System.setProperty("javax.net.ssl.keyStore", LC.tomcat_keystore.value());
            System.setProperty("javax.net.ssl.keyStorePassword", "zimbra");
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

            if (Provisioning.getInstance() instanceof LdapProvisioning)
                checkLDAP();

            server = Provisioning.getInstance().getLocalServer();

            if (server.getBooleanAttr(Provisioning.A_zimbraPop3ServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraPop3BindPort, Config.D_POP3_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraPop3BindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraPop3BindOnStartup, port < 1024)) {
            		NetUtil.reserveServerSocket(address, port, false);
            	}
            }

            if (server.getBooleanAttr(Provisioning.A_zimbraPop3SSLServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraPop3SSLBindPort, Config.D_POP3_SSL_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraPop3SSLBindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraPop3SSLBindOnStartup, port < 1024)) {
            		NetUtil.reserveServerSocket(address, port, true);
            	}
            }

            if (server.getBooleanAttr(Provisioning.A_zimbraImapServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraImapBindPort, Config.D_IMAP_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraImapBindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraImapBindOnStartup, port < 1024)) {
            		NetUtil.reserveServerSocket(address, port, false);
            	}
            }

            if (server.getBooleanAttr(Provisioning.A_zimbraImapSSLServerEnabled, false)) {
            	port = server.getIntAttr(Provisioning.A_zimbraImapSSLBindPort, Config.D_IMAP_SSL_BIND_PORT);
            	address = server.getAttr(Provisioning.A_zimbraImapSSLBindAddress, null);
            	if (server.getBooleanAttr(Provisioning.A_zimbraImapSSLBindOnStartup, port < 1024)) {
                    boolean nioImap = LC.get("nio_imap_enable").equalsIgnoreCase("true");
                    /* In the case of NIO IMAPS, make sure to keep the socket clear! */
            		NetUtil.reserveServerSocket(address, port, nioImap ? false : true);
            	}
            }

            port = server.getIntAttr(Provisioning.A_zimbraLmtpBindPort, Config.D_LMTP_BIND_PORT);
            address = server.getAttr(Provisioning.A_zimbraLmtpBindAddress, null);
            if (server.getBooleanAttr(Provisioning.A_zimbraLmtpBindOnStartup, port < 1024)) {
                NetUtil.reserveServerSocket(address, port, false);
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
