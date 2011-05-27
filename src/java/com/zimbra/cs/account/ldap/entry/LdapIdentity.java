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
package com.zimbra.cs.account.ldap.entry;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

/**
 * 
 * @author pshao
 *
 */
public class LdapIdentity extends Identity implements LdapEntry {

    private String mDn;

    public LdapIdentity(Account acct, String dn, ZAttributes attrs, Provisioning prov) throws LdapException {
        super(acct, attrs.getAttrString(Provisioning.A_zimbraPrefIdentityName),
                attrs.getAttrString(Provisioning.A_zimbraPrefIdentityId),
                attrs.getAttrs(), prov);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }

}
