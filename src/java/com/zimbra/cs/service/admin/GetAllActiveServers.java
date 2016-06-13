/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.zookeeper.CuratorManager;
import com.zimbra.soap.ZimbraSoapContext;

public final class GetAllActiveServers extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        Element response = zsc.createElement(AdminConstants.GET_ALL_ACTIVE_SERVERS_RESPONSE);
        CuratorManager curator = CuratorManager.getInstance();
        if (curator == null) {
            return response;
        }
        Set<String> serverIds;
        try {
            serverIds = curator.getActiveServers();
        } catch (Exception e) {
            throw ServiceException.FAILURE("error while getting active servers", e);
        }
        Provisioning prov = Provisioning.getInstance();
        List<Server> servers = new ArrayList<Server>();
        for (String serverId : serverIds) {
            Server server = prov.getServerById(serverId);
            servers.add(server);
        }

        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);

        for (Iterator<Server> it = servers.iterator(); it.hasNext(); ) {
            Server server = it.next();
            if (aac.hasRightsToList(server, Admin.R_listServer, null))
                GetServer.encodeServer(response, server, true, null, aac.getAttrRightChecker(server));
        }

        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}