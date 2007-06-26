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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.naming.directory.DirContext;
import javax.servlet.http.HttpServlet;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.common.util.EasySSLProtocolSocketFactory;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.znative.IO;
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

            System.setProperty("javax.net.ssl.keyStore", LC.mailboxd_keystore.value());
            System.setProperty("javax.net.ssl.keyStorePassword", LC.mailboxd_keystore_password.value());
            System.setProperty("javax.net.ssl.trustStorePassword", LC.mailboxd_truststore_password.value());

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

            /* This should be done after privileges are dropped... */
            setupOutputRotation();
        } catch (Throwable t) {
            Util.halt("PrivilegedServlet init failed", t);
        }
    }

    private static Timer sOutputRotationTimer = new Timer();
    
    private static void doOutputRotation() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        String suffix = sdf.format(now);
        String current = LC.mailboxd_output_file.value();
        String rotateTo = current + "." + suffix;
        try {
            new File(current).renameTo(new File(rotateTo));
            IO.setStdoutStderrTo(current);
        } catch (IOException ioe) {
            System.err.println("WARN: rotate stdout stderr failed: " + ioe);
            ioe.printStackTrace();
        }
    }
    
    private static void setupOutputRotation() throws FileNotFoundException, SecurityException, IOException {
        long configMillis = LC.mailboxd_output_rotate_interval.intValue() * 1000;
        if (configMillis <= 0) {
            return;
        }
        GregorianCalendar now = new GregorianCalendar();
        long millisSinceEpoch = now.getTimeInMillis(); 
        long dstOffset = now.get(Calendar.DST_OFFSET);
        long zoneOffset = now.get(Calendar.ZONE_OFFSET);
        long millisSinceEpochLocal = millisSinceEpoch + dstOffset + zoneOffset;
        long firstRotateInMillis = configMillis - (millisSinceEpochLocal % configMillis);
        TimerTask tt = new TimerTask() { public void run() { doOutputRotation(); } };
        sOutputRotationTimer.scheduleAtFixedRate(tt, firstRotateInMillis, configMillis);
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
