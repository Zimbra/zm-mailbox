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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.Map;

import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.service.ServiceException;
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
