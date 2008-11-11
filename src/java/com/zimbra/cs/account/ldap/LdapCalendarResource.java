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

package com.zimbra.cs.account.ldap;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;

/**
 * @author jhahm
 */
class LdapCalendarResource extends CalendarResource implements LdapEntry {

    private String mDn;

    LdapCalendarResource(String dn, String email, Attributes attrs, Map<String, Object> defaults, Provisioning prov) throws NamingException {
        super(email,
              LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId), 
              LdapUtil.getAttrs(attrs), defaults, prov);
        mDn = dn;
    }

    public String getDN() { return mDn; }
}
