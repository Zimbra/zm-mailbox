/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
