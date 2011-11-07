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
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;


public class ForeignPrincipal extends AttributeCallback {
    
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {
        
        if (entry == null || context.isCreate()) {
            return;
        }
        
        if (!(entry instanceof Account))
            return;
            
        Provisioning prov = Provisioning.getInstance();
        if (!(prov instanceof LdapProv))
            return;
        
        Account acct = (Account)entry;
        ((LdapProv) prov).removeFromCache(acct);
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
    
}
