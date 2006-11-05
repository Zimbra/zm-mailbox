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

import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.Element;

class SoapDataSource extends DataSource implements SoapEntry {
        
    SoapDataSource(DataSource.Type type, String name, String id, Map<String, Object> attrs) {
        super(type, name, id, attrs);
    }

    SoapDataSource(Element e) throws ServiceException {
        super(DataSource.Type.fromString(e.getAttribute(AccountService.A_TYPE)),
                e.getAttribute(AccountService.A_NAME), e.getAttribute(AccountService.A_ID), SoapProvisioning.getAttrs(e,AccountService.A_NAME));
    }

    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        // not needed?        
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        // not needed?
    }

}
