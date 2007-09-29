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
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.Element;

class SoapDataSource extends DataSource implements SoapEntry {
        
    SoapDataSource(DataSource.Type type, String name, String id, Map<String, Object> attrs) {
        super(type, name, id, attrs);
    }

    SoapDataSource(Element e) throws ServiceException {
        super(DataSource.Type.fromString(e.getAttribute(AccountService.A_TYPE)),
                e.getAttribute(AccountService.A_NAME), e.getAttribute(AccountService.A_ID), SoapProvisioning.getAttrs(e));
    }

    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        // not needed?        
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        // not needed?
    }

}
