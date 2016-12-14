/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.util.Set;

import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetCalendarResourceRequest;

/**
 * @author jhahm
 */
public class GetCalendarResource extends AdminDocumentHandler {

    private static final String[] TARGET_RESOURCE_PATH = new String[] { AdminConstants.E_CALENDAR_RESOURCE };
    @Override
    protected String[] getProxiedResourceElementPath()  { return TARGET_RESOURCE_PATH; }

    /**
     * must be careful and only return calendar resources
     * a domain admin can see
     */
    @Override
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    /**
     * @return true - which means accept responsibility for measures to prevent account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        GetCalendarResourceRequest req = zsc.elementToJaxb(request);
        boolean applyCos = !Boolean.FALSE.equals(req.getApplyCos());
        CalendarResourceBy calresBy = req.getCalResource().getBy().toKeyCalendarResourceBy();
        String value = req.getCalResource().getKey();

        CalendarResource resource = prov.get(calresBy, value);
        defendAgainstCalResourceHarvesting(resource, calresBy, value, zsc, Admin.R_getCalendarResourceInfo);

        AdminAccessControl aac = checkCalendarResourceRight(zsc, resource, AdminRight.PR_ALWAYS_ALLOW);

        Element response = zsc.createElement(AdminConstants.GET_CALENDAR_RESOURCE_RESPONSE);
        Set<String> reqAttrs = getReqAttrs(req.getAttrs(), AttributeClass.calendarResource);
        ToXML.encodeCalendarResource(response, resource, applyCos, reqAttrs, aac.getAttrRightChecker(resource));

        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getCalendarResource);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getCalendarResource.getName()));
    }
}
