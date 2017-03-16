/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.service.mail.CreateWaitSet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;

public class AdminCreateWaitSetRequest extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        com.zimbra.soap.admin.message.AdminCreateWaitSetRequest req = zsc.elementToJaxb(request);
        AdminCreateWaitSetResponse resp = new AdminCreateWaitSetResponse();
        CreateWaitSet.staticHandle(this, req, context, resp);
        return zsc.jaxbToElement(resp);  /* MUST use zsc variant NOT JaxbUtil */
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("If allAccounts is specified, " + AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
        notes.add("Otherwise, for each requested account, " + AdminRightCheckPoint.Notes.ADMIN_LOGIN_AS);
    }

}
