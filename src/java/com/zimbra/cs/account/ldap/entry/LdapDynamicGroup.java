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

import java.util.Set;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

public class LdapDynamicGroup extends DynamicGroup implements LdapEntry {
    
    private String mDn;
    
    public LdapDynamicGroup(String dn, String email, ZAttributes attrs, Provisioning prov) 
    throws LdapException {
        super(email, attrs.getAttrString(Provisioning.A_zimbraId), attrs.getAttrs(), prov);
        mDn = dn;
    }

    @Override
    public String getDN() {
        return mDn;
    }
    
    @Override
    public String[] getAllMembers() throws ServiceException {
        return ((LdapProvisioning) getProvisioning()).getDynamicGroupMembers(this);
    }
    
    @Override
    public Set<String> getAllMembersSet() throws ServiceException {
        return ((LdapProvisioning) getProvisioning()).getDynamicGroupMembersSet(this);
    }

    @Override
    public String[] getAllMembers(boolean supportNonDefaultMemberURL) 
    throws ServiceException {
        if (isIsACLGroup()) {
            // is a dynamic group with default memberURL
            // expand it by search memberOf
            return getAllMembers();
        } else {
            if (supportNonDefaultMemberURL) {
                return ((LdapProvisioning) getProvisioning()).getNonDefaultDynamicGroupMembersList(this);
            } else {
                return new String[0];
            }
        }
        
    }
}
