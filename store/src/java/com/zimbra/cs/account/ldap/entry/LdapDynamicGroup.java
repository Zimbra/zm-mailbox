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
package com.zimbra.cs.account.ldap.entry;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.Provisioning.MemberOf;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.ldap.BySearchResultEntrySearcher;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchResultEntry;

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

    private static final Set<ObjectType> DYNAMIC_GROUPS_TYPE = Sets.newHashSet(ObjectType.dynamicgroups);
    private static final String [] BASIC_ATTRS = { Provisioning.A_zimbraId, Provisioning.A_zimbraIsAdminGroup,
        Provisioning.A_memberURL };

    public static GroupMembership updateGroupMembershipForCustomDynamicGroups(LdapProvisioning prov,
            GroupMembership membership, Account acct, Domain domain, boolean adminGroupsOnly)
    throws ServiceException {
        String acctDN = prov.getDNforAccount(acct, null, false);
        if (acctDN == null) {
            return membership;
        }
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().allDynamicGroups();
        ZLdapContext zlcCompare = null;
        try {
            zlcCompare = LdapClient.getContext(LdapServerType.get(false /* useMaster */), LdapUsage.COMPARE);
            BySearchResultEntrySearcher searcher = new BySearchResultEntrySearcher(
                    prov, (ZLdapContext) null, domain, BASIC_ATTRS, new GroupMembershipUpdator(prov, zlcCompare,
                            acctDN, membership, adminGroupsOnly, true, false));
            searcher.doSearch(filter, DYNAMIC_GROUPS_TYPE);
        } finally {
            LdapClient.closeContext(zlcCompare);
        }
        return membership;
    }

    public static GroupMembership updateGroupMembershipForDynamicGroups(LdapProvisioning prov,
            GroupMembership membership, Account acct, Collection<String> ids,
            boolean adminGroupsOnly, boolean customGroupsOnly, boolean nonCustomGroupsOnly)
    throws ServiceException {
        if (ids.size() == 0) {
            return membership;
        }
        String acctDN = prov.getDNforAccount(acct, null, false);
        if (acctDN == null) {
            return membership;
        }
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().dynamicGroupByIds(ids.toArray(new String[0]));
        ZLdapContext zlcCompare = null;
        try {
            zlcCompare = LdapClient.getContext(LdapServerType.get(false /* useMaster */), LdapUsage.COMPARE);
            BySearchResultEntrySearcher searcher = new BySearchResultEntrySearcher(
                    prov, (ZLdapContext) null, (Domain) null, BASIC_ATTRS, new GroupMembershipUpdator(prov, zlcCompare,
                            acctDN, membership, adminGroupsOnly, customGroupsOnly, nonCustomGroupsOnly));
            searcher.doSearch(filter, DYNAMIC_GROUPS_TYPE);
        } finally {
            LdapClient.closeContext(zlcCompare);
        }
        return membership;
    }

    public static class GroupMembershipUpdator implements BySearchResultEntrySearcher.SearchEntryProcessor {
        private final LdapProvisioning prov;
        private final ZLdapContext zlcCompare;
        private final GroupMembership membership;
        private final boolean adminGroupsOnly;
        private final boolean customGroupsOnly;
        private final boolean nonCustomGroupsOnly;
        private final String acctDN;

        public GroupMembershipUpdator(LdapProvisioning prov, ZLdapContext zlcCompare, String acctDN,
                GroupMembership membership,
                boolean adminGroupsOnly, boolean customGroupsOnly, boolean nonCustomGroupsOnly) {
            this.prov = prov;
            this.zlcCompare = zlcCompare;
            this.acctDN = acctDN;
            this.membership = membership;
            this.adminGroupsOnly = adminGroupsOnly;
            this.customGroupsOnly = customGroupsOnly;
            this.nonCustomGroupsOnly = nonCustomGroupsOnly;
        }

        @Override
        public void processSearchEntry(ZSearchResultEntry sr) {
            ZAttributes attrs = sr.getAttributes();
            String id = null;
            String isAdminStr = null;
            String memberURL = null;
            boolean isAdmin = false;
            try {
                id = attrs.getAttrString(Provisioning.A_zimbraId);
                isAdminStr = attrs.getAttrString(Provisioning.A_zimbraIsAdminGroup);
                memberURL = attrs.getAttrString(Provisioning.A_memberURL);
                isAdmin = (isAdminStr == null ? false : ProvisioningConstants.TRUE.equals(isAdminStr));
            } catch (LdapException e) {
                ZimbraLog.search.debug("Problem processing search result entry - ignoring", e);
                return;
            }
            if (adminGroupsOnly && !isAdmin) {
                return;
            }
            if (nonCustomGroupsOnly && DynamicGroup.isMembershipDefinedByCustomURL(memberURL)) {
                return;
            }
            if (customGroupsOnly && !DynamicGroup.isMembershipDefinedByCustomURL(memberURL)) {
                return;
            }
            try {
                if (prov.getHelper().compare(sr.getDN(), Provisioning.A_member, acctDN, zlcCompare, false)) {
                    membership.append(new MemberOf(id, isAdmin, true));
                }
            } catch (ServiceException e) {
                ZimbraLog.search.debug("Problem doing compare on group %s for member %s - ignoring",
                        sr.getDN(), acctDN, e);
            }
        }
    }
}
