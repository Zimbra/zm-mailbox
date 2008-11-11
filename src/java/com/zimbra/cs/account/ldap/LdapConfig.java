/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

class LdapConfig extends Config implements LdapEntry {
    
    private String mDn;
    
    LdapConfig(String dn, Attributes attrs, Provisioning provisioning) throws NamingException {
        super(LdapUtil.getAttrs(attrs), provisioning);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
}
