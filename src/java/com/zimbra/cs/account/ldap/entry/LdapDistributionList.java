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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

/**
 * 
 * @author pshao
 *
 */
public class LdapDistributionList extends DistributionList implements LdapEntry {
    private String mDn;
    private boolean mIsBasic; // contains only basic attrs in Ldap
    
    public LdapDistributionList(String dn, String email, ZAttributes attrs, 
            boolean isBasic, Provisioning prov) throws LdapException {
        super(email, attrs.getAttrString(Provisioning.A_zimbraId), 
                attrs.getAttrs(), prov);
        mDn = dn;
        mIsBasic = isBasic;
    }
    
    public String getDN() {
        return mDn;
    }

    @Override
    public String[] getAllMembers() throws ServiceException {
        // need to re-get the DistributionList in full if this object was 
        // created from getDLBasic, which does not bring in members
        if (mIsBasic) {
            DistributionList dl = getProvisioning().get(DistributionListBy.id, getId());
            return dl.getMultiAttr(MEMBER_ATTR);
        } else {
            return super.getAllMembers();
        }
    }
    
    @Override
    public Set<String> getAllMembersSet() throws ServiceException {
        // need to re-get the DistributionList if this object was 
        // created from getDLBasic, which does not bring in members
        if (mIsBasic) {
            DistributionList dl = getProvisioning().get(DistributionListBy.id, getId());
            return getMultiAttrSet(MEMBER_ATTR);
        } else {
            return super.getAllMembersSet();
        }
    }
}
