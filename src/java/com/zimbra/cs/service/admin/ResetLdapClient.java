/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ResetLdapClientRequest;
import com.zimbra.soap.admin.message.ResetLdapClientResponse;

/**
 * @author sankumar
 *
 */
public class ResetLdapClient extends AdminDocumentHandler {

    /*
     * (non-Javadoc)
     *
     * @see
     * com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element,
     * java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ResetLdapClientRequest req = JaxbUtil.elementToJaxb(request);
        boolean allServers = req.isAllServers();
        LdapClient.restart();
        if (allServers) {
            resetLdapClientOnAllServers(zsc, request);
        }
        return zsc.jaxbToElement(new ResetLdapClientResponse());
    }

    private void resetLdapClientOnAllServers(ZimbraSoapContext zsc,
            Element origReq) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String localServerId = prov.getLocalServer().getId();

        for (Server server : prov.getAllMailClientServers()) {
            if (localServerId.equals(server.getId())) {
                continue;
            }
            ZimbraLog.soap.debug("Resetting ldap client on server: "
                    + server.getName());
            Element req = origReq.clone();
            String adminUrl = URLUtil.getAdminURL(server,
                    AdminConstants.ADMIN_SERVICE_URI);
            SoapHttpTransport mTransport = new SoapHttpTransport(adminUrl);
            mTransport.setAuthToken(zsc.getRawAuthToken());
            try {
                mTransport.invoke(req);
            } catch (ServiceException|IOException e) {
                // log and continue
                ZimbraLog.soap
                        .warn("Encountered exception while ResetLdapClient on server: "
                                + server.getName()
                                + ", skip and continue with the next server", e);
            }
        }
    }

}
