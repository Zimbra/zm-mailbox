/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.util;

import java.io.File;
import java.util.Timer;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.Versions;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.im.IMRouter;
import com.zimbra.cs.im.ZimbraIM;
import com.zimbra.cs.imap.ImapServer;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.lmtpserver.LmtpServer;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.PurgeThread;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.mailbox.calendar.WellKnownTimeZones;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.pop3.Pop3Server;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.servlet.FirstServlet;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.StoreManager;

/**
 * @author jhahm
 *
 * Class that encapsulates the initialization and shutdown of services needed
 * by any process that adds mail items.  Services under control include redo
 * logging and indexing.
 */
public class Zimbra {
    private static boolean sInited = false;
    
    private static void checkForClass(String clzName, String jarName) {
        try {
            String s = Class.forName(clzName).getName();
            ZimbraLog.misc.debug("checked for class " + s + " and found it");
        } catch (ClassNotFoundException cnfe) {
            ZimbraLog.misc.error(jarName + " not in your common/lib?", cnfe);
        } catch (UnsatisfiedLinkError ule) {
            ZimbraLog.misc.error("error in shared library used by " + jarName + "?", ule);
        }
    }

    private static void checkForClasses() {
        checkForClass("javax.activation.DataSource", "activation.jar");
        checkForClass("javax.mail.internet.MimeMessage", "mail.jar");
        checkForClass("com.zimbra.znative.IO", "zimbra-native.jar");
    }

    public static synchronized void startup() throws ServiceException {
        if (sInited)
            return;
        
        FirstServlet.waitForInitialization();

        ZimbraLog.misc.info("version=" + BuildInfo.VERSION +
                            " release=" + BuildInfo.RELEASE +
                            " builddate=" + BuildInfo.DATE +
                            " buildhost=" + BuildInfo.HOST);
        ZimbraLog.misc.info("LANG environment is set to: " + System.getenv("LANG"));

        checkForClasses();

    	if (!Versions.checkVersions())
            throw new RuntimeException("Data version mismatch.  Reinitialize or upgrade the backend data store.");

        String tzFilePath = LC.timezone_file.value();
        try {
            File tzFile = new File(tzFilePath);
            WellKnownTimeZones.loadFromFile(tzFile);
        } catch (Throwable t) {
            Zimbra.halt("Unable to load timezones from " + tzFilePath, t);
        }

        MailboxManager.getInstance();

    	ExtensionUtil.loadAll();
    	ExtensionUtil.initAll();

    	// ZimletUtil.loadZimlets();

        MailboxIndex.startup();

        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        redoLog.startup();

        System.setProperty("ical4j.unfolding.relaxed", "true");

        MailboxManager.getInstance().startup();
        
        SessionCache.startup();

        if (!redoLog.isSlave()) {
            Server server = Provisioning.getInstance().getLocalServer();

            if (server.getBooleanAttr(Provisioning.A_zimbraXMPPEnabled, false)) {
                ZimbraIM.startup();
            }
            
            LmtpServer.startupLmtpServer();
            if (server.getBooleanAttr(Provisioning.A_zimbraPop3ServerEnabled, false))
                Pop3Server.startupPop3Server();
            if (server.getBooleanAttr(Provisioning.A_zimbraPop3SSLServerEnabled, false))
                Pop3Server.startupPop3SSLServer();
            if (server.getBooleanAttr(Provisioning.A_zimbraImapServerEnabled, false))
                ImapServer.startupImapServer();
            if (server.getBooleanAttr(Provisioning.A_zimbraImapSSLServerEnabled, false))
                ImapServer.startupImapSSLServer();
        }
        
        WaitSetMgr.startup();
        
        MemoryStats.startup();
        
        ScheduledTaskManager.startup();
        
        PurgeThread.startup();

        // should be last, so that other subsystems can add dynamic stats counters 
        ZimbraPerf.initialize();
        
        ExtensionUtil.postInitAll();
        
        sInited = true;
    }

    public static synchronized void shutdown() throws ServiceException {
        if (!sInited)
            return;

        sInited = false;

        MemoryStats.shutdown();
        
        WaitSetMgr.shutdown();
        
        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        if (!redoLog.isSlave()) {
            LmtpServer.shutdownLmtpServer();
            Pop3Server.shutdownPop3Servers();
            ImapServer.shutdownImapServers();
        }
        
        ZimbraIM.shutdown();

        SessionCache.shutdown();
        
        MailboxIndex.shutdown();
        IMRouter.getInstance().shutdown();

        redoLog.shutdown();

        StoreManager.getInstance().shutdown();

        ExtensionUtil.destroyAll();

        MailboxManager.getInstance().shutdown();

        sTimer.cancel();
    }

    public static Timer sTimer = new Timer(true);

    /**
     * Logs the given message and shuts down the server.
     * 
     * @param message the message to log before shutting down
     */
    public static void halt(String message) {
        try {
            ZimbraLog.system.fatal(message);
        } finally {
            Runtime.getRuntime().halt(1);
        }
    }

    /**
     * Logs the given message and shuts down the server.
     * 
     * @param message the message to log before shutting down
     * @param t the exception that was thrown
     */
    public static void halt(String message, Throwable t) {
        try {
            ZimbraLog.system.fatal(message, t);
        } finally {
            Runtime.getRuntime().halt(1);
        }
    }
}
