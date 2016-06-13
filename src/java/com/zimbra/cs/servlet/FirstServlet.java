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

import javax.servlet.http.HttpServlet;

import com.zimbra.common.net.SocketFactories;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.localconfig.LC;
import com.zimbra.znative.IO;
import com.zimbra.znative.Process;
import com.zimbra.znative.Util;

/**
 * Bind to all necessary privileged ports and then drop privilege.
 */
public class FirstServlet extends HttpServlet {

    private static final long serialVersionUID = -1660545976482412029L;

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

            SocketFactories.registerProtocolsServer();

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

    private static Timer sOutputRotationTimer;
    
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
        
        if (configMillis <= 0)
            return;
        sOutputRotationTimer = new Timer("Timer-OutputRotation");
        GregorianCalendar now = new GregorianCalendar();
        long millisSinceEpoch = now.getTimeInMillis(); 
        long dstOffset = now.get(Calendar.DST_OFFSET);
        long zoneOffset = now.get(Calendar.ZONE_OFFSET);
        long millisSinceEpochLocal = millisSinceEpoch + dstOffset + zoneOffset;
        long firstRotateInMillis = configMillis - (millisSinceEpochLocal % configMillis);
        TimerTask tt = new TimerTask() { 
            public void run() { 
                try {
                    doOutputRotation(); 
                } catch (Throwable e) {
                    if (e instanceof OutOfMemoryError)
                        Zimbra.halt("Caught out of memory error", e);
                    System.err.println("WARN: Caught exception in FirstServlet timer " + e);
                    e.printStackTrace();
                }
            }
        };
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
