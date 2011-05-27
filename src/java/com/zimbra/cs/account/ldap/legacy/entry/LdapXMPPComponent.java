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
package com.zimbra.cs.account.ldap.legacy.entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * 
 */
public class LdapXMPPComponent extends XMPPComponent implements LdapEntry {
    
    private String mDn;

    public LdapXMPPComponent(String dn, Attributes attrs, Provisioning prov) throws NamingException, ServiceException {
        super(LegacyLdapUtil.getAttrString(attrs, Provisioning.A_cn),
              LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraId),
              LegacyLdapUtil.getAttrs(attrs),
              prov
        );
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }

}
