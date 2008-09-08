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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Provisioning;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * 
 */
public class LdapXMPPComponent extends XMPPComponent implements LdapEntry {
    
    private String mDn;

    LdapXMPPComponent(String dn, Attributes attrs) throws NamingException, ServiceException {
        super(LdapUtil.getAttrString(attrs, Provisioning.A_cn),
              LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId),
              LdapUtil.getAttrs(attrs)
        );
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }

}
