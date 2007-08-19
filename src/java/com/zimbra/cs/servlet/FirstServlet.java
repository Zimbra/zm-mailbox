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
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EasySSLProtocolSocketFactory;
import com.zimbra.znative.IO;
import com.zimbra.znative.Process;
import com.zimbra.znative.Util;

/**
 * Bind to all necessary privileged ports and then drop privilege.
 */
public class FirstServlet extends HttpServlet {

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
    	try {
    		System.err.println("Zimbra server process is running as uid=" + Process.getuid() + " euid=" + Process.geteuid() + " gid=" + Process.getgid() + " egid=" + Process.getegid());

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

            System.setProperty("javax.net.ssl.keyStore", LC.mailboxd_keystore.value());
            System.setProperty("javax.net.ssl.keyStorePassword", LC.mailboxd_keystore_password.value());
            System.setProperty("javax.net.ssl.trustStorePassword", LC.mailboxd_truststore_password.value());

            if (LC.ssl_allow_untrusted_certs.booleanValue())
                EasySSLProtocolSocketFactory.init();
            
            if (Provisioning.getInstance() instanceof LdapProvisioning)
                checkLDAP();
    		
            synchronized (mInitializedCondition) {
                mInitialized = true;
                mInitializedCondition.notifyAll();
            }

            setupOutputRotation();
        } catch (Throwable t) {
        	System.err.println("PrivilegedServlet init failed");
        	t.printStackTrace(System.err);
        	Runtime.getRuntime().halt(1);
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
