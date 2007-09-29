/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author jhahm
 */
public class GetCalendarResource extends AdminDocumentHandler {

    /**
     * must be careful and only return calendar resources
     * a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        boolean applyCos =
            request.getAttributeBool(AdminService.A_APPLY_COS, true);        
        Element cr = request.getElement(AdminService.E_CALENDAR_RESOURCE);
        String key = cr.getAttribute(AdminService.A_BY);
        String value = cr.getText();

        CalendarResource resource = prov.get(CalendarResourceBy.fromString(key), value);

        if (resource == null)
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(value);

        if (!canAccessAccount(lc, resource))
            throw ServiceException.PERM_DENIED(
                    "can not access calendar resource account");

        Element response = lc.createElement(
                AdminService.GET_CALENDAR_RESOURCE_RESPONSE);
        ToXML.encodeCalendarResource(response, resource, applyCos);

        return response;
    }
}
