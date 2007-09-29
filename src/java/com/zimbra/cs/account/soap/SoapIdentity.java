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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.Element;

import java.util.Map;

class SoapIdentity extends Identity implements SoapEntry {
    
    SoapIdentity(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs);
    }

    SoapIdentity(Element e) throws ServiceException {
        super(e.getAttribute(AccountService.A_NAME), e.getAttribute(AccountService.A_ID), SoapProvisioning.getAttrs(e, AccountService.A_NAME));
    }
    
    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        /*
        XMLElement req = new XMLElement(AccountService.MODIFY_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountService.E_IDENTITY);
        identity.addAttribute(AccountService.A_NAME, getName());
        SoapProvisioning.addAttrElements(identity, attrs);
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req)));
        */
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        //XMLElement req = new XMLElement(AdminService.GET_ALL_CONFIG_REQUEST);
        //setAttrs(SoapProvisioning.getAttrs(prov.invoke(req)));
    }
}
