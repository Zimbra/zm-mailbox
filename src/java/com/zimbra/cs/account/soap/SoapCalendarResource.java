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

package com.zimbra.cs.account.soap;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.XMLElement;

class SoapCalendarResource extends CalendarResource implements SoapEntry {

    SoapCalendarResource(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
    }

    SoapCalendarResource(Element e) throws ServiceException {
        super(e.getAttribute(AdminService.A_NAME), e.getAttribute(AdminService.A_ID), SoapProvisioning.getAttrs(e), null);        
    }
    
    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.MODIFY_CALENDAR_RESOURCE_REQUEST);
        req.addElement(AdminService.E_ID).setText(getId());
        SoapProvisioning.addAttrElements(req, attrs);
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminService.E_CALENDAR_RESOURCE)));
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_CALENDAR_RESOURCE_REQUEST);
        Element a = req.addElement(AdminService.E_CALENDAR_RESOURCE);
        a.setText(getId());
        a.addAttribute(AdminService.A_BY, CalendarResourceBy.id.name());
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminService.E_CALENDAR_RESOURCE)));
    }
}
