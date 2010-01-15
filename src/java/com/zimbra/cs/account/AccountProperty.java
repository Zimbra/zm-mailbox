/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;

public abstract class AccountProperty extends NamedEntry {
    private final String mAcctId;
    
    AccountProperty(Account acct, String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, null);
        mAcctId = acct.getId();
    }
    
    public String getAccountId() {
        return mAcctId;
    }
    
    public Account getAccount() throws ServiceException{
        return Provisioning.getInstance().get(Provisioning.AccountBy.id, mAcctId);
    }
    
    public Account getAccount(Provisioning prov) throws ServiceException{
        return prov.get(Provisioning.AccountBy.id, mAcctId);
    }
}
