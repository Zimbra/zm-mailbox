/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetServerRequest;
import com.zimbra.soap.admin.type.ServerSelector.ServerBy;

/**
 * @author schemers
 */
public class GetServer extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        GetServerRequest req = JaxbUtil.elementToJaxb(request);

        boolean applyConfig = ! Boolean.FALSE.equals(req.isApplyConfig());
        req.getAttrs();
        Set<String> reqAttrs = getReqAttrs(req.getAttrs(), AttributeClass.server);

        ServerBy method = req.getServer().getBy();
        String name = req.getServer().getKey();

        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("must specify a value for a server", null);
        }

        Server server = prov.get(Key.ServerBy.fromString(method.toString()), name);

        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(name);

        AdminAccessControl aac = checkRight(zsc, context, server, AdminRight.PR_ALWAYS_ALLOW);

        // reload the server
        prov.reload(server);

        Element response = zsc.createElement(AdminConstants.GET_SERVER_RESPONSE);
        encodeServer(response, server, applyConfig, reqAttrs, aac.getAttrRightChecker(server));

        return response;
    }

    public static void encodeServer(Element e, Server s) throws ServiceException {
        encodeServer(e, s, true, null, null);
    }

    public static void encodeServer(Element e, Server s, boolean applyConfig, Set<String> reqAttrs,
            AttrRightChecker attrRightChecker) throws ServiceException {
        Element server = e.addNonUniqueElement(AdminConstants.E_SERVER);
        server.addAttribute(AdminConstants.A_NAME, s.getName());
        server.addAttribute(AdminConstants.A_ID, s.getId());
        Map<String, Object> attrs = s.getUnicodeAttrs(applyConfig);

        ToXML.encodeAttrs(server, attrs, reqAttrs, attrRightChecker);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getServer);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getServer.getName()));
    }
}
