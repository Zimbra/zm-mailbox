/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.entry;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;

/**
 * @author pshao
 */
public class LdapDynamicGroup extends DynamicGroup implements LdapEntry {

    private final String dn;
    private DynamicUnit dynamicUnit;
    private StaticUnit staticUnit;

    public LdapDynamicGroup(String dn, String email, ZAttributes attrs, Provisioning prov)
    throws LdapException {
        super(email, attrs.getAttrString(Provisioning.A_zimbraId), attrs.getAttrs(), prov);
        this.dn = dn;
    }

    public void setSubUnits(DynamicUnit dynamicUnit, StaticUnit staticUnit) {
        this.dynamicUnit = dynamicUnit;
        this.staticUnit = staticUnit;
    }

    public DynamicUnit getDynamicUnit() {
        assert(dynamicUnit != null);
        return dynamicUnit;
    }

    public StaticUnit getStaticUnit() {
        assert(staticUnit != null);
        return staticUnit;
    }

    public boolean hasExternalMembers() {
        return staticUnit != null && staticUnit.hasExternalMembers();
    }

    @Override
    public String getDN() {
        return dn;
    }

    @Override
    public String[] getAllMembers() throws ServiceException {
        if (this.isMembershipDefinedByCustomURL()) {
            return ((LdapProvisioning) getProvisioning()).getNonDefaultDynamicGroupMembers(this);
        } else {
            return ((LdapProvisioning) getProvisioning()).getDynamicGroupMembers(this);
        }
    }

    @Override
    public Set<String> getAllMembersSet() throws ServiceException {
        return ((LdapProvisioning) getProvisioning()).getDynamicGroupMembersSet(this);
    }

    @Override
    public String[] getAllMembers(boolean supportNonDefaultMemberURL)
    throws ServiceException {
        if (isMembershipDefinedByCustomURL()) {
            if (supportNonDefaultMemberURL) {
                return ((LdapProvisioning) getProvisioning()).getNonDefaultDynamicGroupMembers(this);
            } else {
                return new String[0];
            }
        } else {
            // is a classic dynamic group with a standard MemberURL - expand it by searching memberOf
            return getAllMembers();
        }
    }

    public static String getDefaultDynamicUnitMemberURL(String zimbraId) {
        return String.format("ldap:///??sub?(zimbraMemberOf=%s)", zimbraId);
    }

    public static String getDefaultMemberURL(String zimbraId, String staticUnitZimbraId) {
        return String.format("ldap:///??sub?(|(zimbraMemberOf=%s)(zimbraId=%s))",
                zimbraId, staticUnitZimbraId);
    }

    public static class DynamicUnit extends NamedEntry implements LdapEntry {
        private final String dn;
        private final String emailAddr;

        public DynamicUnit(String dn, String name, ZAttributes attrs, Provisioning prov)
        throws LdapException {
            super(name, attrs.getAttrString(Provisioning.A_zimbraId), attrs.getAttrs(), null, prov);
            this.dn = dn;
            this.emailAddr = attrs.getAttrString(Provisioning.A_mail);
        }

        @Override
        public EntryType getEntryType() {
            return EntryType.DYNAMICGROUP_DYNAMIC_UNIT;
        }

        @Override
        public String getDN() {
            return dn;
        }

        public String getEmailAddr() {
            return emailAddr;
        }
    }

    public static class StaticUnit extends NamedEntry implements LdapEntry {
        public static final String MEMBER_ATTR = Provisioning.A_zimbraMailForwardingAddress;

        private final String dn;

        public StaticUnit(String dn, String name, ZAttributes attrs, Provisioning prov)
        throws LdapException {
            super(name, attrs.getAttrString(Provisioning.A_zimbraId), attrs.getAttrs(), null, prov);
            this.dn = dn;
        }

        @Override
        public EntryType getEntryType() {
            return EntryType.DYNAMICGROUP_STATIC_UNIT;
        }

        @Override
        public String getDN() {
            return dn;
        }

        private boolean hasExternalMembers() {
            return !getMembersSet().isEmpty();
        }

        public String[] getMembers() {
            return getMultiAttr(MEMBER_ATTR);
        }

        public Set<String> getMembersSet() {
            return getMultiAttrSet(MEMBER_ATTR);
        }
    }

}
