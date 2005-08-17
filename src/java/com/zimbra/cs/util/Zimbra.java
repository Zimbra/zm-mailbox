/*
 * Created on 2004. 9. 9.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.util;

import java.util.Timer;

import javax.naming.directory.DirContext;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.convert.TransformationStub;
import com.zimbra.cs.db.Versions;
import com.zimbra.cs.httpclient.EasySSLProtocolSocketFactory;
import com.zimbra.cs.imap.ImapServer;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.IndexEditor;
import com.zimbra.cs.pop3.Pop3Server;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.lmtpserver.LmtpServer;
import com.zimbra.cs.localconfig.LC;

/**
 * @author jhahm
 *
 * Class that encapsulates the initialization and shutdown of services needed
 * by any process that adds mail items.  Services under control include redo
 * logging and indexing.
 */
public class Liquid {
	private static boolean sInited = false;

    private static void checkForClass(String clzName, String jarName) {
        try {
            String s = Class.forName(clzName).getName();
            LiquidLog.misc.debug("checked for class " + s + " and found it");
        } catch (ClassNotFoundException cnfe) {
            LiquidLog.misc.error(jarName + " not in your common/lib?", cnfe);
        } catch (UnsatisfiedLinkError ule) {
            LiquidLog.misc.error("error in shared library used by " + jarName + "?", ule);
        }
    }

    private static void checkForClasses() {
        checkForClass("javax.activation.DataSource", "activation.jar");
        checkForClass("javax.mail.internet.MimeMessage", "mail.jar");
        checkForClass("com.liquidsys.os.IO", "liquidos.jar");
    }

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
    
    public static void toolSetup() {
        toolSetup("INFO");
    }

    public static void toolSetup(String defaultLogLevel) {
        LiquidLog.toolSetupLog4j(defaultLogLevel);
        if (LC.ssl_allow_untrusted_certs.booleanValue())
            EasySSLProtocolSocketFactory.init();
    }

	public static synchronized void startup() throws ServiceException {
		if (sInited)
			return;

        LiquidLog.misc.info(
                "version=" + BuildInfo.VERSION +
                " release=" + BuildInfo.RELEASE +
                " builddate=" + BuildInfo.DATE +
                " buildhost=" + BuildInfo.HOST);
        
        if (LC.ssl_allow_untrusted_certs.booleanValue())
            EasySSLProtocolSocketFactory.init();

        checkForClasses();
        
        checkLDAP();
        
    	if (!Versions.checkVersions())
        	throw new RuntimeException("Data version mismatch.  Reinitialize or upgrade the backend data store.");

        TransformationStub.getInstance().init();
        
        Indexer.GetInstance().startup();

        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        redoLog.startup();

        System.setProperty("javax.net.ssl.keyStore", "/opt/liquid/tomcat/conf/keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "liquid");

        if (!redoLog.isSlave()) {
            Server server = Provisioning.getInstance().getLocalServer();
            LmtpServer.startupLmtpServer();
            if (server.getBooleanAttr(Provisioning.A_liquidPop3ServerEnabled, false))
                Pop3Server.startupPop3Server();
            if (server.getBooleanAttr(Provisioning.A_liquidPop3SSLServerEnabled, false))
                Pop3Server.startupPop3SSLServer();
            if (server.getBooleanAttr(Provisioning.A_liquidImapServerEnabled, false))
                ImapServer.startupImapServer();
            if (server.getBooleanAttr(Provisioning.A_liquidImapSSLServerEnabled, false))
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

		SessionCache.getInstance().shutdown();

        Indexer.GetInstance().shutdown();

        redoLog.shutdown();

		StoreManager.getInstance().shutdown();

        TransformationStub.getInstance().destroy();
        
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
            LiquidLog.system.fatal(message);
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
            LiquidLog.system.fatal(message, t);
        } finally {
            Runtime.getRuntime().halt(1);
        }
    }
}
