/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author jhahm
 */
public class CreateCalendarResource extends AdminDocumentHandler {

    /**
     * must be careful and only create calendar recources
     * for the domain admin!
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String name = request.getAttribute(AdminConstants.E_NAME).toLowerCase();
        String password = request.getAttribute(AdminConstants.E_PASSWORD, null);
        Map<String, Object> attrs = AdminService.getAttrs(request, true);

        checkDomainRightByEmail(zsc, name, Admin.R_createCalendarResource);
        checkSetAttrsOnCreate(zsc, TargetType.calresource, name, attrs);
        
        CalendarResource resource = prov.createCalendarResource(name,password, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateCalendarResource","name", name},
                attrs));

        Element response = zsc.createElement(
                AdminConstants.CREATE_CALENDAR_RESOURCE_RESPONSE);

        ToXML.encodeCalendarResource(response, resource, true);

        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_createCalendarResource);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyCalendarResource.getName(), "calendar resource"));
    }
}
