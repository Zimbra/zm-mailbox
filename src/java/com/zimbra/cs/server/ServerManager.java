/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.zimbra.common.consul.CatalogRegistration;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.imap.ImapConfig;
import com.zimbra.cs.imap.ImapServer;
import com.zimbra.cs.imap.NioImapServer;
import com.zimbra.cs.imap.TcpImapServer;
import com.zimbra.cs.lmtpserver.LmtpConfig;
import com.zimbra.cs.lmtpserver.LmtpServer;
import com.zimbra.cs.lmtpserver.TcpLmtpServer;
import com.zimbra.cs.milter.MilterConfig;
import com.zimbra.cs.milter.MilterServer;
import com.zimbra.cs.pop3.NioPop3Server;
import com.zimbra.cs.pop3.Pop3Config;
import com.zimbra.cs.pop3.Pop3Server;
import com.zimbra.cs.pop3.TcpPop3Server;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraApplication;

public final class ServerManager {
    private LmtpServer lmtpServer;
    private Pop3Server pop3Server;
    private Pop3Server pop3SSLServer;
    private ImapServer imapServer;
    private ImapServer imapSSLServer;
    private MilterServer milterServer;

    private static final ServerManager INSTANCE = new ServerManager();

    // For debugging...
    private static final boolean NIO_ENABLED = Boolean.getBoolean("ZimbraNioEnabled");

    public static ServerManager getInstance() {
        return INSTANCE;
    }

    public void startServers() throws ServiceException {
        ZimbraApplication app = ZimbraApplication.getInstance();
        if (app.supports(LmtpServer.class)) {
            lmtpServer = startLmtpServer();
            registerWithServiceLocator(lmtpServer, "zmhealthcheck-lmtp");
        }
        if (app.supports(Pop3Server.class)) {
            if (isEnabled(Provisioning.A_zimbraPop3ServerEnabled)) {
                boolean ssl = false;
                pop3Server = startPop3Server(ssl);
                registerWithServiceLocator(pop3Server, "zmhealthcheck-pop");
            }
            if (isEnabled(Provisioning.A_zimbraPop3SSLServerEnabled)) {
                boolean ssl = true;
                pop3SSLServer = startPop3Server(ssl);
                registerWithServiceLocator(pop3SSLServer, "zmhealthcheck-pop");
            }
        }
        if (app.supports(ImapServer.class)) {
            if (isEnabled(Provisioning.A_zimbraImapServerEnabled)) {
                boolean ssl = false;
                imapServer = startImapServer(ssl);
                registerWithServiceLocator(imapServer, "zmhealthcheck-imap");
            }
            if (isEnabled(Provisioning.A_zimbraImapSSLServerEnabled)) {
                boolean ssl = true;
                imapSSLServer = startImapServer(ssl);
                registerWithServiceLocator(imapSSLServer, "zmhealthcheck-imap");
            }
        }

        // run milter service in the same process as mailboxd. should be used only in dev environment
        if (app.supports(MilterServer.class)) {
            if (LC.milter_in_process_mode.booleanValue()) {
                milterServer = startMilterServer();
            }
        }
    }

    private static boolean isEnabled(String key) throws ServiceException {
        return Provisioning.getInstance().getLocalServer().getBooleanAttr(key, false);
    }

    private LmtpServer startLmtpServer() throws ServiceException {
        LmtpConfig config = LmtpConfig.getInstance();
        LmtpServer server = new TcpLmtpServer(config);
        server.start();
        return server;
    }

    private Pop3Server startPop3Server(boolean ssl) throws ServiceException {
        Pop3Config config = new Pop3Config(ssl);
        Pop3Server server = NIO_ENABLED || ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraPop3NioEnabled, true)?
            new NioPop3Server(config) : new TcpPop3Server(config);
        server.start();
        return server;
    }

    private ImapServer startImapServer(boolean ssl) throws ServiceException {
        ImapConfig config = new ImapConfig(ssl);
        ImapServer server = NIO_ENABLED || isEnabled(Provisioning.A_zimbraImapNioEnabled) ?
            new NioImapServer(config) : new TcpImapServer(config);
        server.start();
        return server;
    }

    private MilterServer startMilterServer() throws ServiceException {
        MilterServer server = new MilterServer(new MilterConfig());
        server.start();
        return server;
    }

    public void stopServers() throws ServiceException {
        if (lmtpServer != null) {
            deregisterWithServiceLocator(lmtpServer);
            lmtpServer.stop();
        }
        if (pop3Server != null) {
            deregisterWithServiceLocator(pop3Server);
            pop3Server.stop();
        }
        if (pop3SSLServer != null) {
            deregisterWithServiceLocator(pop3SSLServer);
            pop3SSLServer.stop();
        }
        if (imapServer != null) {
            deregisterWithServiceLocator(imapServer);
            imapServer.stop();
        }
        if (imapSSLServer != null) {
            deregisterWithServiceLocator(imapSSLServer);
            imapSSLServer.stop();
        }
        if (milterServer != null) {
            deregisterWithServiceLocator(milterServer);
            milterServer.stop();
        }
    }

    public LmtpServer getLmtpServer() {
        return lmtpServer;
    }

    protected String getServiceLocatorServiceName(Server server) {
        return "zimbra-" + server.getName().replace("Server", "").replace("SSL", "").toLowerCase();
    }

    /**
     * Register with service locator.
     */
    public CatalogRegistration.Service registerWithServiceLocator(Server server, String script) throws ServiceException {
        String name = getServiceLocatorServiceName(server);
        String id = name + ":" + server.getConfig().getBindPort();
        CatalogRegistration.Service service = new CatalogRegistration.Service(id, name, server.getConfig().getBindPort());
        service.tags.add(BuildInfo.MAJORVERSION + "." + BuildInfo.MINORVERSION + ".x");
        service.tags.add(BuildInfo.MAJORVERSION + "." + BuildInfo.MINORVERSION + "." + BuildInfo.MICROVERSION);
        if (server.getConfig().isSslEnabled()) {
            service.tags.add("ssl");
        }

        if (script != null) {
            CatalogRegistration.Check check = new CatalogRegistration.Check(id + ":health", name);
            String bindAddress = server.getConfig().getBindAddress();
            if (bindAddress == null) {
                try {
                    bindAddress = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    throw ServiceException.FAILURE("Failed determining local host address", e);
                }
            }
            String url = server.getConfig().getUrl();
            check.script = "/opt/zimbra/libexec/" + script + " -url " + url;
            check.interval = "1m";
            service.check = check;
        }

        Zimbra.getAppContext().getBean(ServiceLocator.class).registerSilent(service);
        return service;
    }

    /**
     * De-register with service locator.
     */
    public void deregisterWithServiceLocator(Server server) {
        String name = getServiceLocatorServiceName(server);
        String id = name + ":" + server.getConfig().getBindPort();
        Zimbra.getAppContext().getBean(ServiceLocator.class).deregisterSilent(id);
    }
}
