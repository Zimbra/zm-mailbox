/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.Provisioning.MemberOf;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.ldap.BySearchResultEntrySearcher;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchResultEntry;

public abstract class DistributionList extends ZAttrDistributionList implements GroupedEntry {

    protected static final String MEMBER_ATTR = Provisioning.A_zimbraMailForwardingAddress;

    public DistributionList(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.DISTRIBUTIONLIST;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public Domain getDomain() throws ServiceException {
        return getProvisioning().getDomain(this);
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public void deleteDistributionList() throws ServiceException {
        getProvisioning().deleteDistributionList(getId());
    }

    public void addAlias(String alias) throws ServiceException {
        getProvisioning().addAlias(this, alias);
    }

    public void removeAlias(String alias) throws ServiceException {
        getProvisioning().removeAlias(this, alias);
    }

    public void renameDistributionList(String newName) throws ServiceException {
        getProvisioning().renameDistributionList(getId(), newName);
    }

    public void addMembers(String[] members) throws ServiceException {
        getProvisioning().addMembers(this, members);
    }

    public void removeMembers(String[] member) throws ServiceException {
        getProvisioning().removeMembers(this, member);
    }

    @Override  // overriden in LdapDistributionList
    public String[] getAllMembers() throws ServiceException {
        return getMultiAttr(MEMBER_ATTR);
    }

    @Override  // overriden in LdapDistributionList
    public Set<String> getAllMembersSet() throws ServiceException {
        return getMultiAttrSet(MEMBER_ATTR);
    }

    @Override
    public String[] getAliases() throws ServiceException {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }

    @Override
    protected void resetData() {
        super.resetData();
    }

    /**
     * Keep in sync with BasicInfo.getAllAddrsAsGroupMember
     */
    @Override
    public String[] getAllAddrsAsGroupMember() throws ServiceException {
        String aliases[] = getAliases();
        List<String> addrs = Lists.newArrayListWithExpectedSize(aliases.length + 1);
        String myName = getName();
        addrs.add(myName);
        for (String alias : aliases) {
            /* the name is usually a zimbraMailAlias too */
            if (!alias.equals(myName)) {
                addrs.add(alias);
            }
        }
        return addrs.toArray(new String[0]);
    }

    private final static class BasicInfo {
        final String id;
        final String name;
        final String[] aliases;
        final boolean isAdmin;
        BasicInfo(String id, String mail, String[] aliases, boolean isAdmin) {
            this.id = id;
            this.isAdmin = isAdmin;
            this.name = mail;
            this.aliases = aliases;
        }

        BasicInfo(DistributionList dl) throws ServiceException {
            this.id = dl.getId();
            this.isAdmin = dl.isIsAdminGroup();
            this.name = dl.getName();
            this.aliases = dl.getAllAddrsAsGroupMember();
        }

        public List<String> getAllAddrsAsGroupMember() throws ServiceException {
            List<String> addrs = Lists.newArrayListWithExpectedSize(aliases.length + 1);
            addrs.add(name);
            for (String alias : aliases) {
                if (!alias.equals(name)) {
                    addrs.add(alias);
                }
            }
            return addrs;
        }
        public MemberOf toMemberOf() {
            return new MemberOf(id, isAdmin, false /* isDynamicGroup */);
        }
        public static GroupMembership mergeIntoGroupMembership(GroupMembership membership, List<BasicInfo> dls) {
            for (BasicInfo dl : dls) {
                membership.append(dl.toMemberOf());
            }
            return membership;
        }
    }

    private static final Set<ObjectType> DISTRIBUTION_LISTS = Sets.newHashSet(ObjectType.distributionlists);
    private static final String [] BASIC_ATTRS = { Provisioning.A_zimbraId, Provisioning.A_zimbraIsAdminGroup,
        Provisioning.A_zimbraMailAlias };

    /**
     * @param via - leave as NULL if not needed as computing via is significantly more expensive.
     * @return Updated membership
     */
    public static GroupMembership updateGroupMembership(LdapProvisioning prov, ZLdapContext zlc,
            GroupMembership membership, Account acct, Map<String, String> via,
            boolean adminGroupsOnly, boolean directOnly)
    throws ServiceException {
        boolean ownContext = false;
        String[] addrs = acct.getAllAddrsAsGroupMember();
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().distributionListsByMemberAddrs(addrs);
        ContainingDLUpdator dlUpdator = new ContainingDLUpdator(prov, adminGroupsOnly);
        try {
            if (zlc == null) {
                ownContext = true;
                zlc = LdapClient.getContext(LdapServerType.get(false /* useMaster */), LdapUsage.SEARCH);
            }
            BySearchResultEntrySearcher searcher = new BySearchResultEntrySearcher(prov, zlc, (Domain) null,
                    BASIC_ATTRS, dlUpdator);
            searcher.doSearch(filter, DISTRIBUTION_LISTS);
            List<BasicInfo> directDLs = dlUpdator.getDistLists();
            BasicInfo.mergeIntoGroupMembership(membership, directDLs);
            if (directOnly) {
                return membership;
            }
            if (via == null) {
                updateGroupMembership(prov, zlc, membership, directDLs, adminGroupsOnly, directOnly);
            } else {
                for (BasicInfo directDL : directDLs) {
                    updateGroupMembership(prov, zlc, membership, directDL, via, adminGroupsOnly, directOnly);
                }
            }
            return membership;
        } finally {
            if (ownContext) {
                LdapClient.closeContext(zlc);
            }
        }
    }

    public static GroupMembership updateGroupMembership(LdapProvisioning prov, ZLdapContext zlc,
            GroupMembership membership, DistributionList dl, Map<String, String> via,
            boolean adminGroupsOnly, boolean directOnly)
    throws ServiceException {
        return updateGroupMembership(prov, zlc, membership, new BasicInfo(dl), via, adminGroupsOnly, directOnly);
    }

    /**
     * @param via - leave as NULL if not needed as computing via is significantly more expensive.
     * @return Updated membership
     */
    private static GroupMembership updateGroupMembership(LdapProvisioning prov, ZLdapContext zlc,
            GroupMembership membership, BasicInfo dl, Map<String, String> via,
            boolean adminGroupsOnly, boolean directOnly)
    throws ServiceException {
        boolean ownContext = false;
        try {
            if (zlc == null) {
                ownContext = true;
                zlc = LdapClient.getContext(LdapServerType.get(false /* useMaster */), LdapUsage.SEARCH);
            }
            List<BasicInfo> directDLs = getContainingDLs(prov, zlc, dl, adminGroupsOnly, true /* directOnly */);
            Iterator<BasicInfo> iter = directDLs.iterator();
            while (iter.hasNext()){
                BasicInfo directDL = iter.next();
                if (membership.groupIds().contains(directDL.id)) {
                    iter.remove();
                } else if ((via != null) && (membership.groupIds().contains(dl.id))) {
                    via.put(directDL.name, dl.name);
                }
            }
            BasicInfo.mergeIntoGroupMembership(membership, directDLs);
            if (directOnly) {
                return membership;
            }
            if (via == null) {
                updateGroupMembership(prov, zlc, membership, directDLs, adminGroupsOnly, directOnly);
            } else {
                for (BasicInfo directDL : directDLs) {
                    updateGroupMembership(prov, zlc, membership, directDL, via, adminGroupsOnly, directOnly);
                }
            }
            return membership;
        } finally {
            if (ownContext) {
                LdapClient.closeContext(zlc);
            }
        }
    }

    private static GroupMembership updateGroupMembership(LdapProvisioning prov, ZLdapContext zlc,
            GroupMembership membership, List<BasicInfo> dls,
            boolean adminGroupsOnly, boolean directOnly)
    throws ServiceException {
        boolean ownContext = false;
        try {
            if (zlc == null) {
                ownContext = true;
                zlc = LdapClient.getContext(LdapServerType.get(false /* useMaster */), LdapUsage.SEARCH);
            }
            List<BasicInfo> directDLs = getContainingDLs(prov, zlc, dls, adminGroupsOnly, true /* directOnly */);
            Iterator<BasicInfo> iter = directDLs.iterator();
            while (iter.hasNext()){
                BasicInfo directDL = iter.next();
                if (membership.groupIds().contains(directDL.id)) {
                    iter.remove();
                }
            }
            BasicInfo.mergeIntoGroupMembership(membership, directDLs);
            if (directOnly || directDLs.isEmpty()) {
                return membership;
            }
            updateGroupMembership(prov, zlc, membership, directDLs, adminGroupsOnly, directOnly);
            return membership;
        } finally {
            if (ownContext) {
                LdapClient.closeContext(zlc);
            }
        }
    }

    public static List<BasicInfo> getContainingDLs(LdapProvisioning prov, ZLdapContext zlc,
            BasicInfo dl, boolean adminGroupsOnly, boolean directOnly)
    throws ServiceException {
        String[] addrs = dl.getAllAddrsAsGroupMember().toArray(new String[0]);
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().distributionListsByMemberAddrs(addrs);
        ContainingDLUpdator dlUpdator = new ContainingDLUpdator(prov, adminGroupsOnly);
        BySearchResultEntrySearcher searcher = new BySearchResultEntrySearcher(prov, zlc, (Domain) null,
                BASIC_ATTRS, dlUpdator);
        searcher.doSearch(filter, DISTRIBUTION_LISTS);
        return dlUpdator.getDistLists();
    }

    public static List<BasicInfo> getContainingDLs(LdapProvisioning prov, ZLdapContext zlc,
            List<BasicInfo> dls, boolean adminGroupsOnly, boolean directOnly)
    throws ServiceException {
        final int chunkSize = 500;
        List<String> addrs = Lists.newArrayList();
        for (BasicInfo dl : dls) {
            addrs.addAll(dl.getAllAddrsAsGroupMember());
        }
        int lastIndex = addrs.size() - 1;
        int start = 0;
        int end = (lastIndex < chunkSize) ? lastIndex : chunkSize - 1;
        List<BasicInfo> containingDLs = Lists.newArrayList();
        while (end <= lastIndex) {
            String[] chunk = addrs.subList(start, end + 1).toArray(new String[0]);
            ZLdapFilter filter = ZLdapFilterFactory.getInstance().distributionListsByMemberAddrs(chunk);
            ContainingDLUpdator dlUpdator = new ContainingDLUpdator(prov, adminGroupsOnly);
            BySearchResultEntrySearcher searcher = new BySearchResultEntrySearcher(prov, zlc, (Domain) null,
                    BASIC_ATTRS, dlUpdator);
            searcher.doSearch(filter, DISTRIBUTION_LISTS);
            containingDLs.addAll(dlUpdator.getDistLists());
            if (end >= lastIndex) {
                break;
            }
            start += chunkSize;
            end += chunkSize;
            if (end > lastIndex) {
                end = lastIndex;
            }
        }
        return containingDLs;
    }

    private static class ContainingDLUpdator implements BySearchResultEntrySearcher.SearchEntryProcessor {
        private final boolean adminGroupsOnly;
        private final LdapProvisioning ldapProv;
        private final List<BasicInfo> distLists = Lists.newArrayList();

        public ContainingDLUpdator(LdapProvisioning ldapProv, boolean adminGroupsOnly) {
            this.ldapProv = ldapProv;
            this.adminGroupsOnly = adminGroupsOnly;
        }

        @Override
        public void processSearchEntry(ZSearchResultEntry sr) {
            ZAttributes attrs = sr.getAttributes();
            try {
                String id = attrs.getAttrString(Provisioning.A_zimbraId);
                String name = ldapProv.getDIT().dnToEmail(sr.getDN(), attrs);
                String isAdminStr = attrs.getAttrString(Provisioning.A_zimbraIsAdminGroup);
                String[] aliases = attrs.getMultiAttrString( Provisioning.A_zimbraMailAlias);
                boolean isAdmin = (isAdminStr == null ? false : ProvisioningConstants.TRUE.equals(isAdminStr));
                if (adminGroupsOnly && !isAdmin) {
                    return;
                }
                distLists.add(new BasicInfo(id, name, aliases, isAdmin));
            } catch (ServiceException e) {
                ZimbraLog.search.debug("Problem processing search result entry - ignoring", e);
                return;
            }
        }

        public List<BasicInfo> getDistLists() {
            return distLists;
        }
    }
}
