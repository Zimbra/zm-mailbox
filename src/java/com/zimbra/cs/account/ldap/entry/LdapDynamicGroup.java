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
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;

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

    public static final class Searcher {
        public interface SearchEntryProcessor {
            public void processSearchEntry(ZSearchResultEntry sr);
        }

        private final LdapProvisioning prov;
        private final Domain domain;
        private final SearchEntryProcessor visitor;
        private final String[] returnAttrs;
        public Searcher(LdapProvisioning prov, Domain domain, SearchEntryProcessor visitor,
                String [] retAttrs) {
            this.prov = prov;
            this.domain = domain;
            this.visitor = visitor;
            this.returnAttrs = retAttrs;
        }

        public void doSearch(ZLdapFilter filter) throws ServiceException {
            Set<ObjectType> types = Sets.newHashSet();
            types.add(ObjectType.dynamicgroups);
            String[] bases = prov.getSearchBases(domain, types);
            for (String base : bases) {
                try {
                    ZSearchControls ctrl = ZSearchControls.createSearchControls(ZSearchScope.SEARCH_SCOPE_SUBTREE,
                            ZSearchControls.SIZE_UNLIMITED, returnAttrs);
                    ZSearchResultEnumeration results =
                            prov.getHelper().searchDir(base, filter, ctrl, null, LdapServerType.REPLICA);
                    while(results.hasMore()) {
                        ZSearchResultEntry sr = results.next();
                        visitor.processSearchEntry(sr);
                    }
                    results.close();
                } catch (ServiceException e) {
                    ZimbraLog.search.debug("Unexpected exception searching dynamic groups", e);
                }
            }
        }
    }

    private static final String [] BASIC_ATTRS = { Provisioning.A_zimbraId, Provisioning.A_zimbraIsAdminGroup,
        Provisioning.A_memberURL };

    public static GroupMembership updateGroupMembershipForCustomDynamicGroups(LdapProvisioning prov,
            GroupMembership membership, Account acct, Domain domain, boolean adminGroupsOnly)
    throws ServiceException {
        String acctDN = prov.getDNforAccountById(acct.getId(), null, false);
        if (acctDN == null) {
            return membership;
        }
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().allDynamicGroups();
        Searcher searcher = new Searcher(prov, domain,
                new GroupMembershipUpdator(prov, acctDN, membership, adminGroupsOnly, true, false), BASIC_ATTRS);
        searcher.doSearch(filter);
        return membership;
    }

    public static GroupMembership updateGroupMembershipForCustomDynamicGroups(LdapProvisioning prov,
            GroupMembership membership, Account acct, Collection<String> ids, boolean adminGroupsOnly)
    throws ServiceException {
        return updateGroupMembershipForDynamicGroups(prov,
            membership, acct, ids,
            adminGroupsOnly, true /* customGroupsOnly */, false /* nonCustomGroupsOnly */);
    }

    public static GroupMembership updateGroupMembershipForDynamicGroups(LdapProvisioning prov,
            GroupMembership membership, Account acct, Collection<String> ids,
            boolean adminGroupsOnly, boolean customGroupsOnly, boolean nonCustomGroupsOnly)
    throws ServiceException {
        if (ids.size() == 0) {
            return membership;
        }
        String acctDN = prov.getDNforAccountById(acct.getId(), null, false);
        if (acctDN == null) {
            return membership;
        }
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().dynamicGroupByIds(ids.toArray(new String[0]));
        Searcher searcher = new Searcher(prov, (Domain) null,
                new GroupMembershipUpdator(prov, acctDN, membership, adminGroupsOnly,
                        customGroupsOnly, nonCustomGroupsOnly),
                BASIC_ATTRS);
        searcher.doSearch(filter);
        return membership;
    }

    public static class GroupMembershipUpdator implements Searcher.SearchEntryProcessor {
        private final LdapProvisioning prov;
        private final GroupMembership membership;
        private final boolean adminGroupsOnly;
        private final boolean customGroupsOnly;
        private final boolean nonCustomGroupsOnly;
        private final String acctDN;

        public GroupMembershipUpdator(LdapProvisioning prov, String acctDN, GroupMembership membership,
                boolean adminGroupsOnly, boolean customGroupsOnly, boolean nonCustomGroupsOnly) {
            this.prov = prov;
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
                if (prov.getHelper().compare(sr.getDN(), Provisioning.A_member, acctDN, null, false)) {
                    membership.append(new MemberOf(id, isAdmin, true), id);
                }
            } catch (ServiceException e) {
                ZimbraLog.search.debug("Problem doing compare on group %s for member %s - ignoring",
                        sr.getDN(), acctDN, e);
            }
        }
    }
}
