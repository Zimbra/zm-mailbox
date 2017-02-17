/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

/**
 * @author pshao
 */
public abstract class DynamicGroup extends ZAttrDynamicGroup {

    private Boolean hasCustomMemberURL = null;

    public DynamicGroup(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.DYNAMICGROUP;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public Domain getDomain() throws ServiceException {
        return getProvisioning().getDomain(this);
    }

    @Override  // Override in LdapDynamicGroup and SoapDynamicGroup
    public String[] getAllMembers() throws ServiceException {
        return getMultiAttr(Provisioning.A_member);
    }

    @Override  // overriden on LdapDynamicGroup
    public Set<String> getAllMembersSet() throws ServiceException {
        return getMultiAttrSet(Provisioning.A_member);
    }

    @Override
    public String[] getAliases() throws ServiceException {
        return getMailAlias();
    }

    /*
     * Override in LdapDynamicGroup
     *
     * Default implementation is calling getAllMembers() regardless
     * of supportNonDefaultMemberURL.
     *
     * Should only be called from the edge: ProvUtil or adminNamespace
     * GetDistributuionList.  If supportNonDefaultMemberURL is true,
     * this call can be very expensive.
     */
    public String[] getAllMembers(boolean supportNonDefaultMemberURL)
    throws ServiceException {
        return getAllMembers();
    }

    public boolean isMembershipDefinedByCustomURL() {
        if (hasCustomMemberURL == null) {
            hasCustomMemberURL = isMembershipDefinedByCustomURL(getMemberURL());
        }
        return hasCustomMemberURL;
    }

    public static boolean isMembershipDefinedByCustomURL(String memberURL) {
        return ((memberURL != null) && (!memberURL.startsWith("ldap:///??sub?(|(zimbraMemberOf=")));
    }
}
