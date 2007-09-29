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
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.XMLElement;

class SoapCos extends Cos implements SoapEntry {
    
    SoapCos(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs);
    }

    SoapCos(Element e) throws ServiceException {
        super(e.getAttribute(AdminService.A_NAME), e.getAttribute(AdminService.A_ID), SoapProvisioning.getAttrs(e));
    }
    
    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.MODIFY_COS_REQUEST);
        req.addElement(AdminService.E_ID).setText(getId());
        SoapProvisioning.addAttrElements(req, attrs);
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminService.E_COS)));        
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_COS_REQUEST);
        Element a = req.addElement(AdminService.E_COS);
        a.setText(getId());
        a.addAttribute(AdminService.A_BY, CosBy.id.name());
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminService.E_COS)));        
    }
}
