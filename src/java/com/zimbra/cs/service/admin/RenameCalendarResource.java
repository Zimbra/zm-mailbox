/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author jhahm
 */
public class RenameCalendarResource extends AdminDocumentHandler {

    private static final String[] TARGET_RESOURCE_PATH = new String[] { AdminConstants.E_ID };
    protected String[] getProxiedResourcePath()  { return TARGET_RESOURCE_PATH; }

    /**
     * must be careful and only allow renames from/to
     * domains a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.E_ID);
        String newName = request.getAttribute(AdminConstants.E_NEW_NAME);

        CalendarResource resource = prov.get(Key.CalendarResourceBy.id, id);
        if (resource == null)
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(id);
        
        String oldName = resource.getName();

        // check if the admin can rename the calendar resource
        checkAccountRight(zsc, resource, Admin.R_renameCalendarResource);

        // check if the admin can "create calendar resource" in the domain (can be same or diff)
        checkDomainRightByEmail(zsc, newName, Admin.R_createCalendarResource);

        prov.renameCalendarResource(id, newName);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "RenameCalendarResource", "name",
                              oldName, "newName", newName}));

        // get again with new name...

        resource = prov.get(Key.CalendarResourceBy.id, id);
        if (resource == null)
            throw ServiceException.FAILURE("unable to get calendar resource after rename: " + id, null);
        Element response = zsc.createElement(AdminConstants.RENAME_CALENDAR_RESOURCE_RESPONSE);
        ToXML.encodeCalendarResource(response, resource, true);
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_renameCalendarResource);
        relatedRights.add(Admin.R_createCalendarResource);
    }
}
