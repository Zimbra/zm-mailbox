/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.ldap.custom;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class CustomLdapProvisioning extends LdapProvisioning {

    @Override
    protected void setDIT() {
        mDIT = new CustomLdapDIT(this);
    }

    public CustomLdapProvisioning() {
        super();
    }

    public CustomLdapProvisioning(CacheMode cacheMode) {
        super(cacheMode);
    }

    @Override
    public List<?> getAllDistributionLists(Domain domain) throws ServiceException {
        /* Don't specify domain in constructor - custom DIT doesn't necessarily store groups under the domain */
        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions();
        searchOpts.setFilter(mDIT.filterDistributionListsByDomain(domain));
        searchOpts.setTypes(ObjectType.distributionlists);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING);
        return searchDirectoryInternal(searchOpts);
    }

    /**
     * Note: Only returns distributionlists.  Dynamic groups are not supported with customDIT
     */
    @Override
    public List getAllGroups(Domain domain) throws ServiceException {
        /* Note: If going to support dynamicgroups in the future, when specifying both groups and
         *       DLs in searchDirectoryInternal, it add a sub-tree match filter for the domain, which
         *       won't work because groups/DLs aren't necessarily stored under the domain sub-tree.
         *       Suggest doing 2 searches and combining the results
         */
        return getAllDistributionLists(domain);
    }

    /**
     * Always returns an empty GroupMembership as Dynamic groups are not supported with customDIT
     */
    @Override
    @VisibleForTesting
    public GroupMembership getCustomDynamicGroupMembership(Account acct, boolean adminGroupsOnly)
            throws ServiceException {
        return new GroupMembership();
    }


}
