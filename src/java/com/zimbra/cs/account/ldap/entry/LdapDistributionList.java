/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.entry;

import java.util.Set;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

/**
 * @author pshao
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
            return dl.getMultiAttrSet(MEMBER_ATTR);
        } else {
            return super.getAllMembersSet();
        }
    }

}
