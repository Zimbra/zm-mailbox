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
package com.zimbra.cs.prov.ldap.entry;

import com.zimbra.cs.account.XMPPComponent;
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
class LdapXMPPComponent extends XMPPComponent implements LdapEntry {
    
    private String mDn;

    LdapXMPPComponent(ZSearchResultEntry entry, Provisioning prov) throws LdapException {
        super(LdapUtil.getAttrString(entry, Provisioning.A_cn),
                LdapUtil.getAttrString(entry, Provisioning.A_zimbraId),
                LdapUtil.getAttrs(entry),
                prov);
        mDn = entry.getDN();
    }

    public String getDN() {
        return mDn;
    }

}
