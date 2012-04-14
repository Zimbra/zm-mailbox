/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.UCService;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.UCServiceInfo;

class SoapUCService extends UCService implements SoapEntry {

    SoapUCService(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    SoapUCService(UCServiceInfo ucServiceInfo, Provisioning prov) throws ServiceException {
        super(ucServiceInfo.getName(), ucServiceInfo.getId(),
                Attr.collectionToMap(ucServiceInfo.getAttrList()), prov);
    }

    SoapUCService(Element e, Provisioning prov) throws ServiceException {
        super(e.getAttribute(AdminConstants.A_NAME), e.getAttribute(AdminConstants.A_ID), 
                SoapProvisioning.getAttrs(e), prov);
    }

    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) 
    throws ServiceException {
        XMLElement req = new XMLElement(AdminConstants.MODIFY_UC_SERVICE_REQUEST);
        req.addElement(AdminConstants.E_ID).setText(getId());
        SoapProvisioning.addAttrElements(req, attrs);
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminConstants.E_UC_SERVICE)));
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        XMLElement req = new XMLElement(AdminConstants.GET_UC_SERVICE_REQUEST);
        Element a = req.addElement(AdminConstants.E_UC_SERVICE);
        a.setText(getId());
        a.addAttribute(AdminConstants.A_BY, Key.UCServiceBy.id.name());
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminConstants.E_UC_SERVICE)));
    }
}
