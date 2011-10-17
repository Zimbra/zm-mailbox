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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

/**
 * 
 * @author pshao
 *
 */
public class LdapDomain extends Domain implements LdapEntry {

    private String mDn;
    
    public LdapDomain(String dn, ZAttributes attrs, Map<String, Object> defaults, Provisioning prov) 
    throws LdapException {
        super(attrs.getAttrString(Provisioning.A_zimbraDomainName), 
                attrs.getAttrString(Provisioning.A_zimbraId), 
                attrs.getAttrs(), defaults, prov);
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
