/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.common.soap.Element;

class SoapDataSource extends DataSource implements SoapEntry {
        
    SoapDataSource(Account acct, DataSource.Type type, String name, String id, Map<String, Object> attrs) {
        super(acct, type, name, id, attrs);
    }

    SoapDataSource(Account acct, Element e) throws ServiceException {
        super(acct,
              DataSource.Type.fromString(e.getAttribute(AccountConstants.A_TYPE)),
              e.getAttribute(AccountConstants.A_NAME), e.getAttribute(AccountConstants.A_ID), SoapProvisioning.getAttrs(e));
    }

    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        // not needed?        
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        // not needed?
    }
    
    public Account getAccount() throws ServiceException {
        throw ServiceException.INVALID_REQUEST("unsupported, use getAccount(Provisioning)", null);
    }

}
