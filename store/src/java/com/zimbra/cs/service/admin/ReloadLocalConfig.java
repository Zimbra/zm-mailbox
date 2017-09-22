/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ReloadLocalConfigResponse;

/**
 * Reload the local config file on the fly.
 * <p>
 * After successfully reloading a new local config file, subsequent
 * {@link LC#get(String)} calls should receive new value. However, if you store/
 * cache those values (e.g. keep them as class member or instance member), new
 * values are of course not reflected.
 *
 * @author ysasaki
 */
public final class ReloadLocalConfig extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        try {
            LC.reload();
        } catch (DocumentException e) {
            ZimbraLog.misc.error("Failed to reload LocalConfig", e);
            throw AdminServiceException.FAILURE("Failed to reload LocalConfig", e);
        } catch (ConfigException e) {
            ZimbraLog.misc.error("Failed to reload LocalConfig", e);
            throw AdminServiceException.FAILURE("Failed to reload LocalConfig", e);
        }
        ZimbraLog.misc.info("LocalConfig reloaded");
        reloadLCOnAllImapDaemons();
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        return zsc.jaxbToElement(new ReloadLocalConfigResponse());
    }

    private void reloadLCOnAllImapDaemons() {
        List<Server> imapServers;
        try {
            imapServers = Provisioning.getIMAPDaemonServersForLocalServer();
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("unable to fetch list of imapd servers", e);
            return;
        }
        for (Server server: imapServers) {
            reloadImapDaemonLC(server);
        }
    }
    private void reloadImapDaemonLC(Server server) {
        if (server == null) {
            return;
        }
        ImapConnection connection = null;
        try {
            connection = ImapConnection.getZimbraConnection(server, LC.zimbra_ldap_user.value(), AuthProvider.getAdminAuthToken());
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("unable to connect to IMAP server '%s'", server.getServiceHostname(), e);
            return;
        }
        try {
            ZimbraLog.imap.debug("issuing RELOADLC request to imapd server '%s'", server.getServiceHostname());
            connection.reloadLocalConfig();
        } catch (IOException e) {
            ZimbraLog.imap.warn("unable to issue RELOADLC request to IMAP server '%s'", server.getServiceHostname(), e);
        } finally {
            connection.close();
        }
    }

}
