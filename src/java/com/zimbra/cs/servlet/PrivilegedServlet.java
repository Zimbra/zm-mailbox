/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.servlet;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.httpclient.EasySSLProtocolSocketFactory;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.znative.Process;

/**
 * Bind to all necessary privileged ports and then drop privilege.
 */
public class PrivilegedServlet extends HttpServlet {

    private static void checkLDAP() {
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
        } catch (ServiceException e) {
            throw new RuntimeException("Error communicating with LDAP", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public void init() throws ServletException {
        Server server;
        int port;
        String address;
        try {
            checkLDAP();

            if (LC.ssl_allow_untrusted_certs.booleanValue())
                EasySSLProtocolSocketFactory.init();
            
            System.setProperty("javax.net.ssl.keyStore", LC.tomcat_keystore.value());
            System.setProperty("javax.net.ssl.keyStorePassword", "zimbra");

            server = Provisioning.getInstance().getLocalServer();

            port = server.getIntAttr(Provisioning.A_zimbraPop3BindPort, Config.D_POP3_BIND_PORT);
            address = server.getAttr(Provisioning.A_zimbraPop3BindAddress, null);
            if (server.getBooleanAttr(Provisioning.A_zimbraPop3BindOnStartup, port < 1024)) {
                NetUtil.reserveServerSocket(address, port, false);
            }

            port = server.getIntAttr(Provisioning.A_zimbraPop3SSLBindPort, Config.D_POP3_SSL_BIND_PORT);
            address = server.getAttr(Provisioning.A_zimbraPop3SSLBindAddress, null);
            if (server.getBooleanAttr(Provisioning.A_zimbraPop3SSLBindOnStartup, port < 1024)) {
                NetUtil.reserveServerSocket(address, port, true);
            }

            port = server.getIntAttr(Provisioning.A_zimbraImapBindPort, Config.D_IMAP_BIND_PORT);
            address = server.getAttr(Provisioning.A_zimbraImapBindAddress, null);
            if (server.getBooleanAttr(Provisioning.A_zimbraImapBindOnStartup, port < 1024)) {
                NetUtil.reserveServerSocket(address, port, false);
            }

            port = server.getIntAttr(Provisioning.A_zimbraImapSSLBindPort, Config.D_IMAP_SSL_BIND_PORT);
            address = server.getAttr(Provisioning.A_zimbraImapSSLBindAddress, null);
            if (server.getBooleanAttr(Provisioning.A_zimbraImapSSLBindOnStartup, port < 1024)) {
                NetUtil.reserveServerSocket(address, port, true);
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
                System.err.println("Zimbra server process is running as root, changing to user=" + user + " uid=" + uid + " gid" + gid);
                Process.setPrivileges(user, uid, gid);
                System.err.println("Zimbra server process, after change, is running with uid=" + Process.getuid() + " euid=" + Process.geteuid() + " gid=" + Process.getgid() + " egid=" + Process.getegid());
            } else {
                System.err.println("Zimbra server process is not running as root");
            }
            
            if (Process.getuid() == 0) {
                halt("can not start server with uid of 0");
            }
            if (Process.geteuid() == 0) {
                halt("can not start server with effective uid of 0");
            }
            if (Process.getgid() == 0) {
                halt("can not start server with gid of 0");
            }
            if (Process.getegid() == 0) {
                halt("can not start server with effective gid of 0");
            }
        } catch (Throwable t) {
            halt("PrivilegedServlet init failed", t);
        }
    }
    
    /**
     * Logs the given message and shuts down the server.
     * log4j has not been initialized yet, so we can't use Zimbra.halt.
     * 
     * @param message the message to log before shutting down
     */
    public static void halt(String message) {
        try {
            System.err.println(message);
        } finally {
            Runtime.getRuntime().halt(1);
        }
    }

    /**
     * Logs the given message and shuts down the server.
     * log4j has not been initialized yet, so we can't use Zimbra.halt.
     * 
     * @param message the message to log before shutting down
     * @param t the exception that was thrown
     */
    public static void halt(String message, Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println(message);
            t.printStackTrace(pw);
            System.err.println(sw.toString());
        } finally {
            Runtime.getRuntime().halt(1);
        }
    }

}
