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
package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;

class LdapGlobalGrant extends GlobalGrant implements LdapEntry {
    
    private String mDn;
    
    LdapGlobalGrant(String dn, Attributes attrs, Provisioning provisioning) throws NamingException {
        super(LdapUtil.getAttrs(attrs), provisioning);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
}