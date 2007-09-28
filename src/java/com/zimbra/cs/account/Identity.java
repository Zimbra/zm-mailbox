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

package com.zimbra.cs.account;

import java.util.Map;
import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 */
public class Identity extends NamedEntry implements Comparable {

    private final String mAcctId;
    
    public Identity(Account acct, String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
        mAcctId = acct.getId();
    }
    
    /**
     * this should only be used internally by the server. it doesn't modify the real id, just
     * the cached one.
     * @param id
     */
    public void setId(String id) {
        mId = id;
        getRawAttrs().put(Provisioning.A_zimbraPrefIdentityId, id);
    }
    
    /*
     * get account of the identity
     */
    public Account getAccount() throws ServiceException {
        return Provisioning.getInstance().get(Provisioning.AccountBy.id, mAcctId);
    }
}


