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

package com.zimbra.cs.util;

import java.util.Timer;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.convert.TransformationStub;
import com.zimbra.cs.db.Versions;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.httpclient.EasySSLProtocolSocketFactory;
import com.zimbra.cs.imap.ImapServer;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.lmtpserver.LmtpServer;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.pop3.Pop3Server;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.PrivilegedServlet;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.zimlet.ZimletUtil;

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
        
        PrivilegedServlet.waitForInitialization();
        
        ZimbraLog.misc.info(
                            "version=" + BuildInfo.VERSION +
                            " release=" + BuildInfo.RELEASE +
                            " builddate=" + BuildInfo.DATE +
                            " buildhost=" + BuildInfo.HOST);
        
        checkForClasses();
        
    	if (!Versions.checkVersions())
            throw new RuntimeException("Data version mismatch.  Reinitialize or upgrade the backend data store.");
        
    	ExtensionUtil.loadAll();
    	ExtensionUtil.initAll();
    	
    	//ZimletUtil.loadZimlets();
    	
        TransformationStub.getInstance().init();
        
        Indexer.GetInstance().startup();
        
        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        redoLog.startup();
        
        System.setProperty("ical4j.unfolding.relaxed", "true");
        
        if (!redoLog.isSlave()) {
            Server server = Provisioning.getInstance().getLocalServer();
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

        sInited = true;
    }

    public static synchronized void shutdown() throws ServiceException {
        if (!sInited)
            return;

        sInited = false;
        
        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        if (!redoLog.isSlave()) {
            LmtpServer.shutdownLmtpServer();
            Pop3Server.shutdownPop3Servers();
            ImapServer.shutdownImapServers();
        }

        SessionCache.shutdown();

        Indexer.GetInstance().shutdown();

        redoLog.shutdown();

        StoreManager.getInstance().shutdown();

        TransformationStub.getInstance().destroy();
        
        ExtensionUtil.destroyAll();
        
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

    public static void toolSetup() {
        toolSetup("INFO");
    }

    public static void toolSetup(String defaultLogLevel) {
    	toolSetup(defaultLogLevel, false);
    }
    
    public static void toolSetup(String defaultLogLevel, boolean showThreads) {
        ZimbraLog.toolSetupLog4j(defaultLogLevel, showThreads);
        if (LC.ssl_allow_untrusted_certs.booleanValue())
            EasySSLProtocolSocketFactory.init();
    }


}
