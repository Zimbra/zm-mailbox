/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.prov.ldap;

import java.util.Map;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapEntry;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZSearchResultEntry;

/**
 * 
 * @author pshao
 *
 */
class LdapDomain extends Domain implements LdapEntry {

    private String mDn;

    LdapDomain(ZSearchResultEntry entry, Map<String, Object> defaults, Provisioning prov) throws LdapException {
        super(LdapUtil.getAttrString(entry, Provisioning.A_zimbraDomainName), 
                LdapUtil.getAttrString(entry, Provisioning.A_zimbraId), 
                LdapUtil.getAttrs(entry), 
                defaults, prov);
        mDn = entry.getDN();
    }

    public String getDN() {
        return mDn;
    }
}
