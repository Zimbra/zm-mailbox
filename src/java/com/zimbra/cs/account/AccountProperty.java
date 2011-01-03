/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;

public abstract class AccountProperty extends NamedEntry {
	/*
	 *  We should clean this up in HELIX - only use mAcct, and deprecate mAcctId.  
	 *  Reason for doing this in GNR is to avoid potential regressions
	 *  (it should not cause any regression, in theory) because there are too many 
	 *  callsites of  AccountProperty.getAccount(). 
     */
    private final String mAcctId; // HELIX TODO: remove in HELIX, use only mAcct
    private final Account mAcct; 
    
    AccountProperty(Account acct, String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, null, prov);
        mAcctId = acct.getId();
        mAcct = acct;
    }
    
    // HELIX TODO: return mAcct.getId() in HELIX
    public String getAccountId() {
        return mAcctId;
    }
    
    // HELIX TODO: return mAcct in HELIX
    public Account getAccount() throws ServiceException {
        return Provisioning.getInstance().get(Provisioning.AccountBy.id, mAcctId);
    }
    
    // HELIX TODO: delete in HELIX, use only getAccount()
    public Account getOwnerAccount() {
    	return mAcct;
    }
    
}
