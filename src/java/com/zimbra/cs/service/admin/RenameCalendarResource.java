/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author jhahm
 */
public class RenameCalendarResource extends AdminDocumentHandler {

    /**
     * must be careful and only allow renames from/to
     * domains a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map context)
    throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminService.E_ID);
        String newName = request.getAttribute(AdminService.E_NEW_NAME);

        CalendarResource resource = prov.getCalendarResourceById(id);
        if (resource == null)
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(id);

        if (!canAccessAccount(lc, resource))
            throw ServiceException.PERM_DENIED(
                    "cannot access calendar resource account");

        String oldName = resource.getName();

        if (!canAccessEmail(lc, newName))
            throw ServiceException.PERM_DENIED(
                    "cannot access calendar resource account: " + newName);

        prov.renameCalendarResource(id, newName);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "RenameCalendarResource", "name",
                              oldName, "newName", newName}));

        // get again with new name...

        resource = prov.getCalendarResourceById(id);
        if (resource == null)
            throw ServiceException.FAILURE(
                    "unable to get calendar resource after rename: " + id,
                    null);
        Element response =
            lc.createElement(AdminService.RENAME_CALENDAR_RESOURCE_RESPONSE);
        ToXML.encodeCalendarResource(response, resource, true);
        return response;
    }
}
