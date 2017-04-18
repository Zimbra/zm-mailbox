/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.imap;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.util.ZimbraLog;


public class ImapDaemon {

    public static final String IMAP_LOG4J_CONFIG = "/opt/zimbra/conf/imap.log4j.properties";

    private ImapServer imapServer, imapSSLServer;

    private ImapServer startImapServer(boolean ssl) throws ServiceException {
        RemoteImapConfig config = new RemoteImapConfig(ssl);
        ZimbraLog.imap.info("Starting IMAP server, port=%d", config.getBindPort());
        ImapServer server = LC.nio_imap_enabled.booleanValue() ?
            new NioImapServer(config) : new TcpImapServer(config);
        server.start();
        return server;
    }

    private int startServers() throws ServiceException {
        int cnt = 0;
        if (isEnabled(Provisioning.A_zimbraRemoteImapServerEnabled)) {
            imapServer = startImapServer(false);
            cnt += 1;
        } else {
            System.err.println(Provisioning.A_zimbraRemoteImapServerEnabled + " is FALSE");
        }
        if (isEnabled(Provisioning.A_zimbraRemoteImapSSLServerEnabled)) {
            imapSSLServer = startImapServer(true);
            cnt += 1;
        } else {
            System.err.println(Provisioning.A_zimbraRemoteImapSSLServerEnabled + " is FALSE");
        }

        return cnt;
    }

    private void stopServer(ImapServer server) {
        try {
            if(server != null) {
                ZimbraLog.imap.info("Stopping IMAP server, port=%d", server.getConfig().getBindPort());
                server.stop(10); // TODO configure wait time
            }
        } catch(ServiceException e) {
            ZimbraLog.imap.error("stopServer", e);
        }
    }

    private void stopServers() {
        stopServer(imapServer);
        stopServer(imapSSLServer);
    }

    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(IMAP_LOG4J_CONFIG));
            PropertyConfigurator.configure(props);

            if(isZimbraImapEnabled()) {
                ImapDaemon daemon = new ImapDaemon();
                int numStarted = daemon.startServers();
                if(numStarted > 0) {
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                              ZimbraLog.imap.info("Shutting down servers");
                              daemon.stopServers();
                          }
                        });
                } else {
                    System.err.println("ImapDaemon: No servers started. Check zimbraRemoteImapServerEnabled and zimbraRemoteImapSSLServerEnabled");
                    System.exit(1);
                }
                if(!isMemberOfPool()) {
                    System.err.println("ImapDaemon: This server not member of pool. Check zimbraReverseProxyUpstreamImapServers.");
                }
            } else {
                System.err.println("ImapDaemon: imap service is not enabled on this server. Check zimbraServiceEnabled.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("ImapDaemon: " + e);
        }
    }

    private static boolean isEnabled(String key) throws ServiceException {
        return Provisioning.getInstance().getLocalServer().getBooleanAttr(key, false);
    }

    private static boolean isMemberOfPool() throws ServiceException {
        String localServer = Provisioning.getInstance().getLocalServer().getName();
        String[] imapServers = Provisioning.getInstance().getLocalServer().getReverseProxyUpstreamImapServers();
        for(String server: imapServers) {
            if(localServer.equals(server)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isZimbraImapEnabled() throws ServiceException {
        String[] enabledServices = Provisioning.getInstance().getLocalServer().getMultiAttr(Provisioning.A_zimbraServiceEnabled);
        for(String service: enabledServices) {
            if(service.equals("imap")) {
                return true;
            }
        }
        return false;
   }
}
