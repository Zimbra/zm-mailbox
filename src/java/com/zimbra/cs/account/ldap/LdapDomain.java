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

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

/**
 * @author schemers
 */
public class LdapDomain extends Domain implements LdapEntry {

    private String mDn;

    LdapDomain(String dn, Attributes attrs, Map<String, Object> defaults) throws NamingException {
        super(LdapUtil.getAttrString(attrs, Provisioning.A_zimbraDomainName), 
                LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId), 
                LdapUtil.getAttrs(attrs), defaults);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
}
