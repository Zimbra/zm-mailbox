/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap.legacy.entry;

import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.ldap.LdapConstants;

/**
 * @author schemers
 */
public class LdapDomain extends Domain implements LdapEntry {

    private String mDn;

    public LdapDomain(String dn, Attributes attrs, Map<String, Object> defaults, Provisioning prov) throws NamingException {
        super(LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraDomainName), 
                LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraId), 
                LegacyLdapUtil.getAttrs(attrs), defaults, prov);
        mDn = dn;
    }

    public String getDN() {
        return mDn;
    }
    
    public String getGalSearchBase(String searchBaseSpec) throws ServiceException {
        LdapProv ldapProv = (LdapProv)getProvisioning();
        
        if (searchBaseSpec.equalsIgnoreCase("DOMAIN")) {
            return ldapProv.getDIT().domainDNToAccountSearchDN(getDN());
        } else if (searchBaseSpec.equalsIgnoreCase("SUBDOMAINS")) {
            return getDN();
        } else if (searchBaseSpec.equalsIgnoreCase("ROOT")) {
            return LdapConstants.DN_ROOT_DSE;
        }
        return LdapConstants.DN_ROOT_DSE;
    }
}
