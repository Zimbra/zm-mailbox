/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.io.File;
import java.io.IOException;
import java.util.Timer;

import com.zimbra.cs.account.ExternalAccountManagerTask;
import org.apache.mina.core.buffer.IoBuffer;

import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.lmtp.SmtpToLmtp;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AutoProvisionThread;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.Versions;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.PurgeThread;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.mailbox.acl.AclPushTask;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.server.ServerManager;
import com.zimbra.cs.servlet.FirstServlet;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.znative.Util;

/**
 * Class that encapsulates the initialization and shutdown of services needed
 * by any process that adds mail items.  Services under control include redo
 * logging and indexing.
 */
public final class Zimbra {
    private static boolean sInited = false;
    private static boolean sIsMailboxd = false;

    /** Sets system properties before the server fully starts up.  Note that
     *  there's a potential race condition if {@link FirstServlet} or another
     *  servlet faults in classes or references properties before they're set
     *  here. */
    private static void setSystemProperties() {
        System.setProperty("mail.mime.decodetext.strict",       "false");
        System.setProperty("mail.mime.encodefilename",          "true");
        System.setProperty("mail.mime.charset",                 "utf-8");
        System.setProperty("mail.mime.base64.ignoreerrors",     "true");
        System.setProperty("mail.mime.ignoremultipartencoding", "false");
    }

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

    private static String getSysProperty(String prop) {
        try {
            return System.getProperty(prop);
        } catch (SecurityException e) {
            return "(accessing " + prop + " is not allowed by security manager)";
        }
    }

    private static void logVersionAndSysInfo() {
        ZimbraLog.misc.info("version=" + BuildInfo.VERSION +
                " release=" + BuildInfo.RELEASE +
                " builddate=" + BuildInfo.DATE +
                " buildhost=" + BuildInfo.HOST);

        ZimbraLog.misc.info("LANG environment is set to: " + System.getenv("LANG"));

        ZimbraLog.misc.info("System property java.home="            + getSysProperty("java.home"));
        ZimbraLog.misc.info("System property java.runtime.version=" + getSysProperty("java.runtime.version"));
        ZimbraLog.misc.info("System property java.version="         + getSysProperty("java.version"));
        ZimbraLog.misc.info("System property java.vm.info="         + getSysProperty("java.vm.info"));
        ZimbraLog.misc.info("System property java.vm.name="         + getSysProperty("java.vm.name"));
        ZimbraLog.misc.info("System property java.vm.version="      + getSysProperty("java.vm.version"));
        ZimbraLog.misc.info("System property os.arch="              + getSysProperty("os.arch"));
        ZimbraLog.misc.info("System property os.name="              + getSysProperty("os.name"));
        ZimbraLog.misc.info("System property os.version="           + getSysProperty("os.version"));
        ZimbraLog.misc.info("System property sun.arch.data.model="  + getSysProperty("sun.arch.data.model"));
        ZimbraLog.misc.info("System property sun.cpu.endian="       + getSysProperty("sun.cpu.endian"));
        ZimbraLog.misc.info("System property sun.cpu.isalist="      + getSysProperty("sun.cpu.isalist"));
        ZimbraLog.misc.info("System property sun.os.patch.level="   + getSysProperty("sun.os.patch.level"));
    }

    private static void checkForClasses() {
        checkForClass("javax.activation.DataSource", "activation.jar");
        checkForClass("javax.mail.internet.MimeMessage", "mail.jar");
        checkForClass("com.zimbra.znative.IO", "zimbra-native.jar");
    }

    public static void startup() {
        try {
            startup(true);
        } catch (ServiceException se) {
            Zimbra.halt("Exception during startup, aborting server, please check your config", se);
        }
    }

    public static void startupCLI() throws ServiceException {
        startup(false);
    }

    /**
     * Initialize the various subsystems at server/CLI startup time.
     * @param forMailboxd true if this is the mailboxd process; false for CLI processes
     * @throws ServiceException
     */
    private static synchronized void startup(boolean forMailboxd) throws ServiceException {
        if (sInited)
            return;

        sIsMailboxd = forMailboxd;
        if (sIsMailboxd) {
            FirstServlet.waitForInitialization();
        }

        setSystemProperties();

        logVersionAndSysInfo();

        SoapTransport.setDefaultUserAgent("ZCS", BuildInfo.VERSION);

        checkForClasses();

        ZimbraApplication app = ZimbraApplication.getInstance();

        DbPool.startup();

        app.initializeZimbraDb(forMailboxd);


        if (!Versions.checkVersions()) {
            Zimbra.halt("Data version mismatch.  Reinitialize or upgrade the backend data store.");
        }

        DbPool.loadSettings();

        String tzFilePath = LC.timezone_file.value();
        try {
            File tzFile = new File(tzFilePath);
            WellKnownTimeZones.loadFromFile(tzFile);
        } catch (Throwable t) {
            Zimbra.halt("Unable to load timezones from " + tzFilePath, t);
        }

        Provisioning prov = Provisioning.getInstance();
        if (prov instanceof LdapProv) {
            ((LdapProv) prov).waitForLdapServer();
            if (forMailboxd) {
                AttributeManager.loadLdapSchemaExtensionAttrs((LdapProv) prov);
            }
        }

        try {
            RightManager.getInstance();
        } catch (ServiceException e) {
            Util.halt("cannot initialize RightManager", e);
        }

        ZimbraHttpConnectionManager.startReaperThread();

        try {
            StoreManager.getInstance().startup();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to initialize StoreManager.", e);
        }

        MailboxManager.getInstance();

        app.startup();

        if (app.supports(MemcachedConnector.class.getName())) {
            MemcachedConnector.startup();
        }
        if (app.supports(EhcacheManager.class.getName())) {
            EhcacheManager.getInstance().startup();
        }

        ExtensionUtil.initAll();

        // ZimletUtil.loadZimlets();

        MailboxIndex.startup();

        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        if (sIsMailboxd) {
            redoLog.startup();
        } else {
            redoLog.initRedoLogManager();
        }

        System.setProperty("ical4j.unfolding.relaxed", "true");

        MailboxManager.getInstance().startup();

        app.initialize(sIsMailboxd);
        if (sIsMailboxd) {
            SessionCache.startup();

            Server server = Provisioning.getInstance().getLocalServer();

            if (!redoLog.isSlave()) {
                boolean useDirectBuffers = server.isMailUseDirectBuffers();
                IoBuffer.setUseDirectBuffer(useDirectBuffers);
                ZimbraLog.misc.info("MINA setUseDirectBuffers(" + useDirectBuffers + ")");

                ServerManager.getInstance().startServers();
            }

            if (app.supports(WaitSetMgr.class.getName())) {
                WaitSetMgr.startup();
            }

            if (app.supports(MemoryStats.class.getName())) {
                MemoryStats.startup();
            }

            if (app.supports(ScheduledTaskManager.class.getName())) {
                ScheduledTaskManager.startup();
            }

            if (app.supports(PurgeThread.class.getName())) {
                PurgeThread.startup();
            }

            if (app.supports(AutoProvisionThread.class.getName())) {
                AutoProvisionThread.startup();
            }

            if (LC.smtp_to_lmtp_enabled.booleanValue()) {
                int smtpPort = LC.smtp_to_lmtp_port.intValue();
                int lmtpPort = Provisioning.getInstance().getLocalServer().getLmtpBindPort();
                SmtpToLmtp smtpServer = SmtpToLmtp.startup(smtpPort, "localhost", lmtpPort);
                smtpServer.setRecipientValidator(new SmtpRecipientValidator());
            }

            if (app.supports(AclPushTask.class)) {
                long pushInterval = server.getSharingUpdatePublishInterval();
                sTimer.schedule(new AclPushTask(), pushInterval, pushInterval);
            }

            if (app.supports(ExternalAccountManagerTask.class)) {
                long interval = server.getExternalAccountStatusCheckInterval();
                sTimer.schedule(new ExternalAccountManagerTask(), interval, interval);
            }

            // should be last, so that other subsystems can add dynamic stats counters
            if (app.supports(ZimbraPerf.class.getName())) {
                ZimbraPerf.initialize();
            }
        }

        ExtensionUtil.postInitAll();

        sInited = true;
    }

    public static synchronized void shutdown() throws ServiceException {
        if (!sInited)
            return;

        sInited = false;

        if (sIsMailboxd) {
            PurgeThread.shutdown();
            AutoProvisionThread.shutdown();
        }

        ZimbraApplication app = ZimbraApplication.getInstance();

        app.shutdown();

        if (sIsMailboxd) {
            if (app.supports(MemoryStats.class.getName())) {
                MemoryStats.shutdown();
            }

            if (app.supports(WaitSetMgr.class.getName())) {
                WaitSetMgr.shutdown();
            }
        }

        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        if (sIsMailboxd) {
            if (!redoLog.isSlave()) {
                ServerManager.getInstance().stopServers();
            }

            SessionCache.shutdown();
        }

        MailboxIndex.shutdown();

        if (sIsMailboxd) {
            redoLog.shutdown();
        }

        if (app.supports(ExtensionUtil.class.getName())) {
            ExtensionUtil.destroyAll();
        }

        if (app.supports(MemcachedConnector.class.getName())) {
            MemcachedConnector.shutdown();
        }
        if (app.supports(EhcacheManager.class.getName())) {
            EhcacheManager.getInstance().shutdown();
        }

        MailboxManager.getInstance().shutdown();

        if (sIsMailboxd) {
            StoreManager.getInstance().shutdown();
        }

        ZimbraHttpConnectionManager.shutdownReaperThread();

        sTimer.cancel();

        try {
            DbPool.shutdown();
        } catch (Exception ignored) {
        }
    }

    public static synchronized boolean started() {
        return sInited;
    }

    public static Timer sTimer = new Timer("Timer-Zimbra", true);

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
