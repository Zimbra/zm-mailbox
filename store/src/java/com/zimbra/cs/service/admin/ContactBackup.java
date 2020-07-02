/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.callback.CallbackUtil;
import com.zimbra.cs.mailbox.ContactBackupThread;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ContactBackupRequest;
import com.zimbra.soap.admin.message.ContactBackupRequest.Operation;
import com.zimbra.soap.admin.message.ContactBackupResponse;
import com.zimbra.soap.admin.type.ContactBackupServer;
import com.zimbra.soap.admin.type.ContactBackupServer.ContactBackupStatus;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.admin.type.ServerSelector.ServerBy;

public class ContactBackup extends AdminDocumentHandler {
    protected ZimbraSoapContext zsc = null;
    protected List<Integer> doneIds = null;
    protected List<Integer> skippedIds = null;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        zsc = getZimbraSoapContext(context);
        ContactBackupRequest req = JaxbUtil.elementToJaxb(request);
        Operation op = req.getOp();
        if (op == null) {
            throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        List<ServerSelector> selectors = req.getServers();
        if (selectors != null && selectors.isEmpty()) {
            selectors = null;
        }
        ContactBackupResponse resp = new ContactBackupResponse();
        List<ContactBackupServer> servers = null;

        switch (op) {
            case start:
                servers = startContactBackup(selectors, context, zsc);
                break;
            case stop:
                servers = stopContactBackup(selectors, context, zsc);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        resp.setServers(servers);
        return zsc.jaxbToElement(resp);
    }

    protected List<ContactBackupServer> stopContactBackup(List<ServerSelector> selectors, Map<String, Object> context, ZimbraSoapContext zsc) throws ServiceException {
        List<ServerSelector> selectorsToIterate = setEffectiveSelectors(selectors);
        List<ContactBackupServer> servers = new ArrayList<ContactBackupServer>();
        for (ServerSelector serverSelector : selectorsToIterate) {
            Server server = null;
            try {
                server = verifyServerPerms(serverSelector, zsc);
            } catch (ServiceException se) {
                ZimbraLog.contactbackup.debug("Could not find server or no permission on %s", serverSelector.getKey());
                servers.add(new ContactBackupServer(serverSelector.getKey(), ContactBackupStatus.error));
                continue;
            }
            boolean local = CallbackUtil.isLocalServer(server);
            if (local) {
                if (!ContactBackupThread.isRunning()) {
                    ZimbraLog.contactbackup.debug("ContactBackup is not running on %s", server.getServiceHostname());
                    servers.add(new ContactBackupServer(server.getName(), ContactBackupStatus.error));
                } else {
                    ContactBackupThread.shutdown();
                    servers.add(new ContactBackupServer(server.getName(), ContactBackupStatus.stopped));
                }
            } else {
                List<ServerSelector> list = new ArrayList<ServerSelector>();
                list.add(serverSelector);
                ContactBackupRequest req = new ContactBackupRequest(Operation.stop, list);
                Element request = JaxbUtil.jaxbToElement(req);
                Element response = proxyRequest(request, context, Provisioning.myIpAddress(), zsc);
                ContactBackupResponse resp = JaxbUtil.elementToJaxb(response);
                servers.addAll(resp.getServers());
            }
        }
        return servers;
    }

    protected List<ContactBackupServer> startContactBackup(List<ServerSelector> selectors, Map<String, Object> context, ZimbraSoapContext zsc) throws ServiceException {
        List<ServerSelector> selectorsToIterate = setEffectiveSelectors(selectors);
        List<ContactBackupServer> servers = new ArrayList<ContactBackupServer>();
        for (ServerSelector serverSelector : selectorsToIterate) {
            Server server = null;
            try {
                server = verifyServerPerms(serverSelector, zsc);
            } catch (ServiceException se) {
                ZimbraLog.contactbackup.debug("Could not find server or no permission on %s", serverSelector.getKey());
                servers.add(new ContactBackupServer(serverSelector.getKey(), ContactBackupStatus.error));
                continue;
            }
            boolean local = CallbackUtil.isLocalServer(server);
            if (local) {
                if (!ContactBackupThread.isRunning()) {
                    ContactBackupThread.startup();
                    servers.add(new ContactBackupServer(server.getName(), ContactBackupStatus.started));
                } else {
                    servers.add(new ContactBackupServer(server.getName(), ContactBackupStatus.error));
                }
            } else {
                List<ServerSelector> list = new ArrayList<ServerSelector>();
                list.add(serverSelector);
                ContactBackupRequest req = new ContactBackupRequest(Operation.start, list);
                Element request = JaxbUtil.jaxbToElement(req);
                Element response = proxyRequest(request, context, Provisioning.myIpAddress(), zsc);
                ContactBackupResponse resp = JaxbUtil.elementToJaxb(response);
                servers.addAll(resp.getServers());
            }
        }
        return servers;
    }

    private List<ServerSelector> setEffectiveSelectors(List<ServerSelector> selectors) throws ServiceException {
        if (selectors == null || selectors.isEmpty()) {
            List<ServerSelector> retSelectors = new ArrayList<ServerSelector>();
            List<Server> servers = Provisioning.getInstance().getAllServers(Provisioning.SERVICE_MAILBOX);
            for (Server server : servers) {
                retSelectors.add(new ServerSelector(ServerBy.id, server.getId()));
            }
            return retSelectors;
        }
        return selectors;
    }
}
