/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.Provisioning;

class LdapAlias extends Alias implements LdapEntry {
    private String mDn;

    LdapAlias(String dn, String email, Attributes attrs, Provisioning prov) throws NamingException
    {
        super(email,
              LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId), 
              LdapUtil.getAttrs(attrs), prov);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
}
