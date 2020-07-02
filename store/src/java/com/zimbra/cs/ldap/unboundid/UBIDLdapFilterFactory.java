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
package com.zimbra.cs.ldap.unboundid;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

/**
 * @author pshao
 */
public class UBIDLdapFilterFactory extends ZLdapFilterFactory {

    @Override
    public void debug() {
    }


    /*
     * canned filters
     */
    private static Filter FILTER_ALL_ACCOUNTS;       // including calendar resources
    private static Filter FILTER_ALL_ACCOUNTS_ONLY;  // excluding calendar resources
    private static Filter FILTER_ALL_ADMIN_ACCOUNTS;
    private static Filter FILTER_ALL_ALIASES;
    private static Filter FILTER_ALL_CALENDAR_RESOURCES;
    private static Filter FILTER_ALL_COSES;
    private static Filter FILTER_ALL_DATASOURCES;
    private static Filter FILTER_ALL_DISTRIBUTION_LISTS;
    private static Filter FILTER_ALL_DOMAINS;
    private static Filter FILTER_ALL_DYNAMIC_GROUPS;
    private static Filter FILTER_ALL_DYNAMIC_GROUP_DYNAMIC_UNITS;
    private static Filter FILTER_ALL_DYNAMIC_GROUP_STATIC_UNITS;
    private static Filter FILTER_ALL_GROUPS;
    private static Filter FILTER_HAB_GROUPS;
    private static Filter FILTER_ALL_HAB_GROUPS;
    private static Filter FILTER_ALL_IDENTITIES;
    private static Filter FILTER_ALL_MIME_ENTRIES;
    private static Filter FILTER_ALL_NON_SYSTEM_ACCOUNTS;
    private static Filter FILTER_ALL_NON_SYSTEM_ARCHIVING_ACCOUNTS;
    private static Filter FILTER_ALL_NON_SYSTEM_INTERNAL_ACCOUNTS;
    private static Filter FILTER_ALL_SERVERS;
    private static Filter FILTER_ALL_ALWAYSONCLUSTERS;
    private static Filter FILTER_ALL_UC_SERVICES;
    private static Filter FILTER_ALL_SHARE_LOCATORS;
    private static Filter FILTER_ALL_SIGNATURES;
    private static Filter FILTER_ALL_XMPP_COMPONENTS;
    private static Filter FILTER_ALL_ZIMLETS;
    private static Filter FILTER_ANY_ENTRY;
    private static Filter FILTER_DOMAIN_LABEL;
    private static Filter FILTER_HAS_SUBORDINATES;
    private static Filter FILTER_IS_ARCHIVING_ACCOUNT;
    private static Filter FILTER_IS_EXTERNAL_ACCOUNT;
    private static Filter FILTER_IS_SYSTEM_RESOURCE;
    private static Filter FILTER_NOT_SYSTEM_RESOURCE;
    private static Filter FILTER_PUBLIC_SHARE;
    private static Filter FILTER_ALLAUTHED_SHARE;
    private static Filter FILTER_NOT_EXCLUDED_FROM_CMB_SEARCH;
    private static Filter FILTER_WITH_ARCHIVE;
    private static Filter FILTER_ALL_INTERNAL_ACCOUNTS;
    private static Filter FILTER_ALL_ADDRESS_LISTS;


    private static boolean initialized = false;

    public static synchronized void initialize() throws LdapException {
        assert(!initialized);
        initialized = true;

        try {
            _initialize();
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
        }
    }

    /**
     * initialize canned filters
     *
     * @throws LDAPException
     */
    private static void _initialize() throws LDAPException {

        /*
         * self-defined filters
         */
        FILTER_ALL_ACCOUNTS = Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraAccount);

        FILTER_ALL_ALIASES = Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraAlias);

        FILTER_ALL_CALENDAR_RESOURCES =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraCalendarResource);

        FILTER_ALL_COSES =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraCOS);

        FILTER_ALL_DATASOURCES =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraDataSource);

        FILTER_ALL_DISTRIBUTION_LISTS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraDistributionList);

        FILTER_ALL_DOMAINS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraDomain);

        FILTER_ALL_DYNAMIC_GROUPS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraGroup);
        FILTER_ALL_HAB_GROUPS =
            Filter.createEqualityFilter(
            LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraHabGroup);

        FILTER_ALL_DYNAMIC_GROUP_DYNAMIC_UNITS =
            Filter.createEqualityFilter(
            LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraGroupDynamicUnit);

        FILTER_ALL_DYNAMIC_GROUP_STATIC_UNITS =
            Filter.createEqualityFilter(
            LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraGroupStaticUnit);

        FILTER_ALL_IDENTITIES =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraIdentity);

        FILTER_ALL_MIME_ENTRIES =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraMimeEntry);

        FILTER_ALL_SERVERS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraServer);

        FILTER_ALL_ALWAYSONCLUSTERS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraAlwaysOnCluster);

        FILTER_ALL_UC_SERVICES =
            Filter.createEqualityFilter(
            LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraUCService);

        FILTER_ALL_SHARE_LOCATORS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraShareLocator);

        FILTER_ALL_SIGNATURES =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraSignature);

        FILTER_ALL_XMPP_COMPONENTS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraXMPPComponent);

        FILTER_ALL_ZIMLETS =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraZimletEntry);

        FILTER_ANY_ENTRY =
                Filter.createPresenceFilter(LdapConstants.ATTR_objectClass);

        FILTER_DOMAIN_LABEL =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, LdapConstants.OC_dcObject);

        FILTER_HAS_SUBORDINATES =
                Filter.createEqualityFilter(
                LdapConstants.ATTR_hasSubordinates, LdapConstants.LDAP_TRUE);

        FILTER_IS_ARCHIVING_ACCOUNT =
                Filter.createPresenceFilter(Provisioning.A_amavisArchiveQuarantineTo);

        FILTER_IS_EXTERNAL_ACCOUNT =
                Filter.createEqualityFilter(
                Provisioning.A_zimbraIsExternalVirtualAccount, LdapConstants.LDAP_TRUE);

        FILTER_IS_SYSTEM_RESOURCE =
                Filter.createEqualityFilter(
                Provisioning.A_zimbraIsSystemResource, LdapConstants.LDAP_TRUE);

        FILTER_NOT_SYSTEM_RESOURCE =
                Filter.createNOTFilter(FILTER_IS_SYSTEM_RESOURCE);

        FILTER_PUBLIC_SHARE =
                Filter.createSubstringFilter(
                Provisioning.A_zimbraSharedItem, null, new String[]{"granteeType:pub"}, null);

        FILTER_ALLAUTHED_SHARE =
                Filter.createSubstringFilter(
                Provisioning.A_zimbraSharedItem, null, new String[]{"granteeType:all"}, null);

        FILTER_NOT_EXCLUDED_FROM_CMB_SEARCH =
                Filter.createORFilter(
                Filter.createNOTFilter(Filter.createPresenceFilter(Provisioning.A_zimbraExcludeFromCMBSearch)),
                Filter.createEqualityFilter(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE"));

        FILTER_WITH_ARCHIVE =
                Filter.createPresenceFilter(Provisioning.A_zimbraArchiveAccount);


        /*
         * filters built on top of other filters
         */
        FILTER_ALL_ACCOUNTS_ONLY =
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS,
                        Filter.createNOTFilter(FILTER_ALL_CALENDAR_RESOURCES));

       FILTER_ALL_ADMIN_ACCOUNTS =
               Filter.createANDFilter(
                       FILTER_ALL_ACCOUNTS,
                       Filter.createORFilter(
                            Filter.createEqualityFilter(Provisioning.A_zimbraIsAdminAccount, LdapConstants.LDAP_TRUE),
                            Filter.createEqualityFilter(Provisioning.A_zimbraIsDelegatedAdminAccount, LdapConstants.LDAP_TRUE),
                            Filter.createEqualityFilter(Provisioning.A_zimbraIsDomainAdminAccount, LdapConstants.LDAP_TRUE)));

        FILTER_ALL_NON_SYSTEM_ACCOUNTS =
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        Filter.createNOTFilter(FILTER_IS_SYSTEM_RESOURCE));

        FILTER_ALL_NON_SYSTEM_ARCHIVING_ACCOUNTS =
            Filter.createANDFilter(
                    FILTER_ALL_ACCOUNTS_ONLY,
                    Filter.createNOTFilter(FILTER_IS_SYSTEM_RESOURCE),
                    FILTER_IS_ARCHIVING_ACCOUNT);

        FILTER_ALL_NON_SYSTEM_INTERNAL_ACCOUNTS =
            Filter.createANDFilter(
                    FILTER_ALL_ACCOUNTS,
                    Filter.createNOTFilter(FILTER_IS_SYSTEM_RESOURCE),
                    Filter.createNOTFilter(FILTER_ALL_CALENDAR_RESOURCES),
                    Filter.createNOTFilter(FILTER_IS_EXTERNAL_ACCOUNT));

        FILTER_ALL_GROUPS =
                Filter.createORFilter(
                        FILTER_ALL_DYNAMIC_GROUPS,
                        FILTER_ALL_DISTRIBUTION_LISTS);
        FILTER_HAB_GROUPS =
            Filter.createANDFilter(FILTER_ALL_HAB_GROUPS);

        FILTER_ALL_INTERNAL_ACCOUNTS = Filter.createANDFilter(
            FILTER_ALL_ACCOUNTS,
            Filter.createNOTFilter(FILTER_IS_EXTERNAL_ACCOUNT));

        FILTER_ALL_ADDRESS_LISTS = Filter.createEqualityFilter(
                LdapConstants.ATTR_objectClass, AttributeClass.OC_zimbraAddressList);
    }

    @Override
    public String encodeValue(String value) {
        return Filter.encodeValue(value);
    }

    private Filter homedOnServerFilter(String serverServiceHostname) {
        return Filter.createEqualityFilter(Provisioning.A_zimbraMailHost, serverServiceHostname);
    }

    /*
     * operational
     */
    @Override
    public ZLdapFilter hasSubordinates() {
        return new UBIDLdapFilter(
                FilterId.HAS_SUBORDINATES,
                FILTER_HAS_SUBORDINATES);
    }

    @Override
    public ZLdapFilter createdLaterOrEqual(String generalizedTime) {
        return new UBIDLdapFilter(
                FilterId.CREATED_LATEROREQUAL,
                Filter.createGreaterOrEqualFilter(LdapConstants.ATTR_createTimestamp, generalizedTime));
    }

    /*
     * general
     */
    @Override
    public ZLdapFilter anyEntry() {
        return new UBIDLdapFilter(
                FilterId.ANY_ENTRY,
                FILTER_ANY_ENTRY);
    }

    @Override
    public ZLdapFilter fromFilterString(FilterId filterId, String filterString)
    throws LdapException {
        try {
            return new UBIDLdapFilter(
                    filterId,
                    Filter.create(encloseFilterIfNot(filterString)));
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(filterString, e);
        }
    }

    @Override
    public ZLdapFilter andWith(ZLdapFilter filter, ZLdapFilter otherFilter) {
        /*
        return new UBIDLdapFilter(
                filter.getFilterId(),  // use filter id of the filter to which the other filter is appended.
                Filter.createANDFilter(
                        ((UBIDLdapFilter) filter).getNative(),
                        ((UBIDLdapFilter) otherFilter).getNative()));
                        */
        ZLdapFilter andedFilter = null;
        try {
            andedFilter = new UBIDLdapFilter(
                    filter.getFilterId(),  // use filter id of the filter to which the other filter is appended.
                    Filter.createANDFilter(
                            ((UBIDLdapFilter) filter).getNative(),
                            ((UBIDLdapFilter) fromFilterString(FilterId.DN_SUBTREE_MATCH, otherFilter.toFilterString())).getNative()));
        } catch (LdapException e) {
            ZimbraLog.ldap.warn("filter error", e);
            assert(false);  // should not happen
        }
        return andedFilter;
    }

    @Override
    public ZLdapFilter negate(ZLdapFilter filter) {
        return new UBIDLdapFilter(
                filter.getFilterId(),
                Filter.createNOTFilter(
                        ((UBIDLdapFilter) filter).getNative()));
    }



    /*
     * Mail target (accounts and groups)
     */
    @Override
    public ZLdapFilter addrsExist(String[] addrs) {
        List<Filter> filters = Lists.newArrayList();
        for (int i=0; i < addrs.length; i++) {
            filters.add(Filter.createEqualityFilter(Provisioning.A_zimbraMailDeliveryAddress, addrs[i]));
            filters.add(Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, addrs[i]));
        }

        return new UBIDLdapFilter(
                FilterId.ADDRS_EXIST,
                Filter.createORFilter(filters));
    }


    /*
     * account
     */
    @Override
    public ZLdapFilter allAccounts() {
        return new UBIDLdapFilter(
                FilterId.ALL_ACCOUNTS,
                FILTER_ALL_ACCOUNTS);
    }

    @Override
    public ZLdapFilter allAccountsOnly() {
        return new UBIDLdapFilter(
                FilterId.ALL_ACCOUNTS_ONLY,
                FILTER_ALL_ACCOUNTS_ONLY);
    }

    @Override
    public ZLdapFilter allAccountsOnlyByCos(String cosId) {
        return new UBIDLdapFilter(
                FilterId.ALL_ACCOUNTS_ONLY_BY_COS,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        Filter.createORFilter(
                                // local account in default COS
                                Filter.createANDFilter(
                                        Filter.createNOTFilter(FILTER_IS_EXTERNAL_ACCOUNT),
                                        Filter.createNOTFilter(Filter.createPresenceFilter(Provisioning.A_zimbraCOSId))
                                        ),
                                Filter.createEqualityFilter(Provisioning.A_zimbraCOSId, cosId))));
    }

    @Override
    public ZLdapFilter allAdminAccounts() {
        return new UBIDLdapFilter(
                FilterId.ALL_ADMIN_ACCOUNTS,
                FILTER_ALL_ADMIN_ACCOUNTS);
    }

    @Override
    public ZLdapFilter allNonSystemAccounts() {
        return new UBIDLdapFilter(
                FilterId.ALL_NON_SYSTEM_ACCOUNTS,
                FILTER_ALL_NON_SYSTEM_ACCOUNTS);
    }

    @Override
    public ZLdapFilter allNonSystemArchivingAccounts() {
        return new UBIDLdapFilter(
                FilterId.ALL_NON_SYSTEM_ARCHIVING_ACCOUNTS,
                FILTER_ALL_NON_SYSTEM_ARCHIVING_ACCOUNTS);
    }

    @Override
    public ZLdapFilter allNonSystemInternalAccounts() {
        return new UBIDLdapFilter(
                FilterId.ALL_NON_SYSTEM_INTERNAL_ACCOUNTS,
                FILTER_ALL_NON_SYSTEM_INTERNAL_ACCOUNTS);
    }

    @Override
    public ZLdapFilter accountByForeignPrincipal(String foreignPrincipal) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNT_BY_FOREIGN_PRINCIPAL,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraForeignPrincipal, foreignPrincipal),
                        FILTER_ALL_ACCOUNTS));
    }

    @Override
    public ZLdapFilter accountById(String id) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNT_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_ACCOUNTS));
    }

    @Override
    public ZLdapFilter accountByMemberOf(String dynGroupId) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNT_BY_MEMBEROF,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMemberOf, dynGroupId),
                        FILTER_ALL_INTERNAL_ACCOUNTS));
    }

    @Override
    public ZLdapFilter accountByName(String name) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNT_BY_NAME,
                Filter.createANDFilter(
                        Filter.createORFilter(
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailDeliveryAddress, name),
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name),
                                Filter.createEqualityFilter(Provisioning.A_zimbraOldMailAddress, name)),
                        FILTER_ALL_ACCOUNTS));
    }

    @Override
    public ZLdapFilter adminAccountByRDN(String namingRdnAttr, String name) {
        return new UBIDLdapFilter(
                FilterId.ADMIN_ACCOUNT_BY_RDN,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(namingRdnAttr, name),
                        FILTER_ALL_ACCOUNTS));
    }

    @Override
    public ZLdapFilter accountsHomedOnServer(String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNTS_HOMED_ON_SERVER,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS,
                        homedOnServerFilter(serverServiceHostname)));
    }

    @Override
    public ZLdapFilter accountsHomedOnServerAccountsOnly(String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNTS_HOMED_ON_SERVER_ACCOUNTS_ONLY,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        homedOnServerFilter(serverServiceHostname)));
    }

    @Override
    public ZLdapFilter homedOnServer(String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.HOMED_ON_SERVER,
                homedOnServerFilter(serverServiceHostname));
    }

    @Override
    public ZLdapFilter accountsOnServerAndCosHasSubordinates(String serverServiceHostname, String cosId) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNTS_ON_SERVER_AND_COS_HAS_SUBORDINATES,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS,
                        homedOnServerFilter(serverServiceHostname),
                        FILTER_HAS_SUBORDINATES,
                        Filter.createORFilter(
                                Filter.createNOTFilter(Filter.createPresenceFilter(Provisioning.A_zimbraCOSId)),
                                Filter.createEqualityFilter(Provisioning.A_zimbraCOSId, cosId))));
    }

    @Override
    public ZLdapFilter accountsOnUCService(String usServiceId) {
        return new UBIDLdapFilter(
                FilterId.ACCOUNTS_ON_UCSERVICE,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraUCServiceId, usServiceId),
                        FILTER_ALL_ACCOUNTS));
    }

    @Override
    public ZLdapFilter externalAccountsHomedOnServer(String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.EXTERNAL_ACCOUNTS_HOMED_ON_SERVER,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        FILTER_IS_EXTERNAL_ACCOUNT,
                        homedOnServerFilter(serverServiceHostname)));
    }

    @Override
    public ZLdapFilter accountsByGrants(List<String> granteeIds,
            boolean includePublicShares, boolean includeAllAuthedShares) {

        List<Filter> filters = Lists.newArrayList();
        for (String granteeId : granteeIds) {
            filters.add(Filter.createSubstringFilter(Provisioning.A_zimbraSharedItem, "granteeId:" + granteeId, null, null));
        }

        if (includePublicShares) {
            filters.add(FILTER_PUBLIC_SHARE);
        }

        if (includeAllAuthedShares) {
            filters.add(FILTER_ALLAUTHED_SHARE);
        }

        return new UBIDLdapFilter(
                FilterId.ACCOUNTS_BY_GRANTS,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS,
                        Filter.createORFilter(filters)));
    }

    @Override
    public ZLdapFilter CMBSearchAccountsOnly() {
        return new UBIDLdapFilter(
                FilterId.CMB_SEARCH_ACCOUNTS_ONLY,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        FILTER_NOT_EXCLUDED_FROM_CMB_SEARCH));
    }

    @Override
    public ZLdapFilter CMBSearchAccountsOnlyWithArchive() {
        return new UBIDLdapFilter(
                FilterId.CMB_SEARCH_ACCOUNTS_ONLY_WITH_ARCHIVE,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        FILTER_WITH_ARCHIVE,
                        FILTER_NOT_EXCLUDED_FROM_CMB_SEARCH));
    }

    @Override
    public ZLdapFilter CMBSearchNonSystemResourceAccountsOnly() {
        return new UBIDLdapFilter(
                FilterId.CMB_SEARCH_NON_SYSTEM_RESOURCE_ACCOUNTS_ONLY,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        FILTER_NOT_SYSTEM_RESOURCE,
                        FILTER_NOT_EXCLUDED_FROM_CMB_SEARCH));
    }


    /*
     * alias
     */
    @Override
    public ZLdapFilter allAliases() {
        return new UBIDLdapFilter(
                FilterId.ALL_ALIASES,
                FILTER_ALL_ALIASES);
    }

    /*
     * calendar resource
     */
    @Override
    public ZLdapFilter allCalendarResources() {
        return new UBIDLdapFilter(
                FilterId.ALL_CALENDAR_RESOURCES,
                FILTER_ALL_CALENDAR_RESOURCES);
    }

    @Override
    public ZLdapFilter calendarResourceByForeignPrincipal(String foreignPrincipal) {
        return new UBIDLdapFilter(
                FilterId.CALENDAR_RESOURCE_BY_FOREIGN_PRINCIPAL,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraForeignPrincipal, foreignPrincipal),
                        FILTER_ALL_CALENDAR_RESOURCES));
    }

    @Override
    public ZLdapFilter calendarResourceById(String id) {
        return new UBIDLdapFilter(
                FilterId.CALENDAR_RESOURCE_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_CALENDAR_RESOURCES));
    }

    @Override
    public ZLdapFilter calendarResourceByName(String name) {
        return new UBIDLdapFilter(
                FilterId.CALENDAR_RESOURCE_BY_NAME,
                Filter.createANDFilter(
                        Filter.createORFilter(
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailDeliveryAddress, name),
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name)),
                        FILTER_ALL_CALENDAR_RESOURCES));
    }

    @Override
    public ZLdapFilter calendarResourcesHomedOnServer(String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.CALENDAR_RESOURCES_HOMED_ON_SERVER,
                Filter.createANDFilter(
                        FILTER_ALL_CALENDAR_RESOURCES,
                        homedOnServerFilter(serverServiceHostname)));
    }


    /*
     * cos
     */
    @Override
    public ZLdapFilter allCoses() {
        return new UBIDLdapFilter(
                FilterId.ALL_COSES,
                FILTER_ALL_COSES);
    }

    @Override
    public ZLdapFilter cosById(String id) {
        return new UBIDLdapFilter(
                FilterId.COS_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_COSES));
    }

    @Override
    public ZLdapFilter cosesByMailHostPool(String serverId) {
        return new UBIDLdapFilter(
                FilterId.COSES_BY_MAILHOST_POOL,
                Filter.createANDFilter(
                        FILTER_ALL_COSES,
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailHostPool, serverId)));
    }

    @Override
    public ZLdapFilter cosesOnUCService(String usServiceId) {
        return new UBIDLdapFilter(
                FilterId.COSES_ON_UCSERVICE,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraUCServiceId, usServiceId),
                        FILTER_ALL_COSES));
    }


    /*
     * data source
     */
    @Override
    public ZLdapFilter allDataSources() {
        return new UBIDLdapFilter(
                FilterId.ALL_DATA_SOURCES,
                FILTER_ALL_DATASOURCES);
    }

    @Override
    public ZLdapFilter dataSourceById(String id) {
        return new UBIDLdapFilter(
                FilterId.DATA_SOURCE_BY_ID,
                Filter.createANDFilter(
                        FILTER_ALL_DATASOURCES,
                        Filter.createEqualityFilter(Provisioning.A_zimbraDataSourceId, id)));
    }

    @Override
    public ZLdapFilter dataSourceByName(String name) {
        return new UBIDLdapFilter(
                FilterId.DATA_SOURCE_BY_NAME,
                Filter.createANDFilter(
                        FILTER_ALL_DATASOURCES,
                        Filter.createEqualityFilter(Provisioning.A_zimbraDataSourceName, name)));
    }


    /*
     * distribution list
     */
    @Override
    public ZLdapFilter allDistributionLists() {
        return new UBIDLdapFilter(
                FilterId.ALL_DISTRIBUTION_LISTS,
                FILTER_ALL_DISTRIBUTION_LISTS);
    }

    @Override
    public ZLdapFilter distributionListById(String id) {
        return new UBIDLdapFilter(
                FilterId.DISTRIBUTION_LIST_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_DISTRIBUTION_LISTS));
    }

    @Override
    public ZLdapFilter distributionListByName(String name) {
        return new UBIDLdapFilter(
                FilterId.DISTRIBUTION_LIST_BY_NAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name),
                        FILTER_ALL_DISTRIBUTION_LISTS));
    }

    @Override
    public ZLdapFilter distributionListsByMemberAddrs(String[] memberAddrs) {
        List<Filter> filters = Lists.newArrayList();
        for (int i=0; i < memberAddrs.length; i++) {
            filters.add(Filter.createEqualityFilter(Provisioning.A_zimbraMailForwardingAddress, memberAddrs[i]));
        }

        return new UBIDLdapFilter(
                FilterId.DISTRIBUTION_LISTS_BY_MEMBER_ADDRS,
                Filter.createANDFilter(
                        FILTER_ALL_DISTRIBUTION_LISTS,
                        Filter.createORFilter(filters)));
    }

    /*
     * dynamic group
     */
    @Override
    public ZLdapFilter allDynamicGroups() {
        return new UBIDLdapFilter(FilterId.ALL_DYNAMIC_GROUPS, FILTER_ALL_DYNAMIC_GROUPS);
    }

    @Override
    public ZLdapFilter dynamicGroupById(String id) {
        return new UBIDLdapFilter(
                FilterId.DYNAMIC_GROUP_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_DYNAMIC_GROUPS));
    }

    @Override
    public ZLdapFilter dynamicGroupByIds(String[] ids) {
        List<Filter> filters = Lists.newArrayList();
        for (String id : ids) {
            filters.add(Filter.createEqualityFilter(Provisioning.A_zimbraId, id));
        }
        return new UBIDLdapFilter(FilterId.DYNAMIC_GROUP_BY_IDS,
                Filter.createANDFilter(FILTER_ALL_DYNAMIC_GROUPS,
                        Filter.createORFilter(Filter.createORFilter(filters))));
    }

    @Override
    public ZLdapFilter dynamicGroupByName(String name) {
        return new UBIDLdapFilter(
                FilterId.DYNAMIC_GROUP_BY_NAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name),
                        FILTER_ALL_DYNAMIC_GROUPS));
    }

    @Override
    public ZLdapFilter dynamicGroupDynamicUnitByMailAddr(String mailAddr) {
        return new UBIDLdapFilter(
                FilterId.DYNAMIC_GROUP_DYNAMIC_UNIT_BY_MAIL_ADDR,
                Filter.createANDFilter(
                        FILTER_ALL_DYNAMIC_GROUP_DYNAMIC_UNITS,
                        Filter.createEqualityFilter(Provisioning.A_mail, mailAddr)));
    }

    @Override
    public ZLdapFilter dynamicGroupsStaticUnitByMemberAddr(String memberAddr) {
        return new UBIDLdapFilter(
                FilterId.DYNAMIC_GROUPS_STATIC_UNIT_BY_MEMBER_ADDR,
                Filter.createANDFilter(
                        FILTER_ALL_DYNAMIC_GROUP_STATIC_UNITS,
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailForwardingAddress, memberAddr)));
    }

    /*
     * group (distribution list or dynamic group)
     */
    @Override
    public ZLdapFilter allGroups() {
        return new UBIDLdapFilter(
                FilterId.ALL_GROUPS,
                FILTER_ALL_GROUPS);
    }

    @Override
    public ZLdapFilter allHabGroups() {
        return new UBIDLdapFilter(
                FilterId.ALL_GROUPS,
                FILTER_ALL_HAB_GROUPS);
    }

    @Override
    public ZLdapFilter groupById(String id) {
        return new UBIDLdapFilter(
                FilterId.GROUP_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_GROUPS));
    }

    @Override
    public ZLdapFilter groupByName(String name) {
        return new UBIDLdapFilter(
                FilterId.GROUP_BY_NAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name),
                        FILTER_ALL_GROUPS));
    }


    /*
     * domain
     */
    @Override
    public ZLdapFilter allDomains() {
        return new UBIDLdapFilter(
                FilterId.ALL_DOMAINS,
                FILTER_ALL_DOMAINS);
    }

    @Override
    public ZLdapFilter domainAliases(String id) {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_ALIASES,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraDomainAliasTargetId, id), // zimbraDomainAliasTargetId is indexed, bug 72309
                        FILTER_ALL_DOMAINS,
                        Filter.createEqualityFilter(Provisioning.A_zimbraDomainType, Provisioning.DomainType.alias.name())));
    }

    @Override
    public ZLdapFilter domainById(String id) {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_DOMAINS));
    }

    @Override
    public ZLdapFilter domainsByIds(Collection<String> ids) {
        List<Filter> filters = Lists.newArrayList();
        for (String id : ids) {
            filters.add(Filter.createEqualityFilter(Provisioning.A_zimbraId, id));
        }
        return new UBIDLdapFilter(FilterId.DOMAINS_BY_IDS,
                Filter.createANDFilter(FILTER_ALL_DOMAINS, Filter.createORFilter(Filter.createORFilter(filters))));
    }

    @Override
    public ZLdapFilter domainByName(String name) {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_BY_NAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraDomainName, name),
                        FILTER_ALL_DOMAINS));
    }

    @Override
    public ZLdapFilter domainByKrb5Realm(String krb5Realm) {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_BY_KRB5_REALM,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraAuthKerberos5Realm, krb5Realm),
                        FILTER_ALL_DOMAINS));
    }

    @Override
    public ZLdapFilter domainByVirtualHostame(String virtualHostname) {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_BY_VIRTUAL_HOSTNAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraVirtualHostname, virtualHostname),
                        FILTER_ALL_DOMAINS));
    }

    @Override
    public ZLdapFilter domainByForeignName(String foreignName) {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_BY_FOREIGN_NAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraForeignName, foreignName),
                        FILTER_ALL_DOMAINS));
    }

    @Override
    public ZLdapFilter domainLabel() {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_LABEL,
                FILTER_DOMAIN_LABEL);
    }

    @Override
    public ZLdapFilter domainLockedForEagerAutoProvision() {
        return new UBIDLdapFilter(
                FilterId.DOMAIN_LOCKED_FOR_AUTO_PROVISION,
                Filter.createNOTFilter(
                        Filter.createPresenceFilter(Provisioning.A_zimbraAutoProvLock)));
    }

    @Override
    public ZLdapFilter domainsOnUCService(String usServiceId) {
        return new UBIDLdapFilter(
                FilterId.DOMAINS_ON_UCSERVICE,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraUCServiceId, usServiceId),
                        FILTER_ALL_DOMAINS));
    }


    /*
     * global config
     */
    @Override
    public ZLdapFilter globalConfig() {
        return new UBIDLdapFilter(
                FilterId.GLOBAL_CONFIG,
                Filter.createEqualityFilter(Provisioning.A_cn, "config"));
    }


    /*
     * identity
     */
    @Override
    public ZLdapFilter allIdentities() {
        return new UBIDLdapFilter(
                FilterId.ALL_IDENTITIES,
                FILTER_ALL_IDENTITIES);
    }

    @Override
    public ZLdapFilter identityByName(String name) {
        // name = Filter.encodeValue(name);
        return new UBIDLdapFilter(
                FilterId.IDENTITY_BY_NAME,
                Filter.createANDFilter(
                        FILTER_ALL_IDENTITIES,
                        Filter.createEqualityFilter(Provisioning.A_zimbraPrefIdentityName, name)));
    }


    /*
     * mime enrty
     */
    @Override
    public ZLdapFilter allMimeEntries() {
        return new UBIDLdapFilter(
                FilterId.ALL_MIME_ENTRIES,
                FILTER_ALL_MIME_ENTRIES);
    }

    @Override
    public ZLdapFilter mimeEntryByMimeType(String mimeType) {
        return new UBIDLdapFilter(
                FilterId.MIME_ENTRY_BY_MIME_TYPE,
                Filter.createEqualityFilter(Provisioning.A_zimbraMimeType, mimeType));
    }


    /*
     * server
     */
    @Override
    public ZLdapFilter allServers() {
        return new UBIDLdapFilter(
                FilterId.ALL_SERVERS,
                FILTER_ALL_SERVERS);
    }

    @Override
    public ZLdapFilter serverById(String id) {
        return new UBIDLdapFilter(
                FilterId.SERVER_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_SERVERS));
    }

    @Override
    public ZLdapFilter serverByService(String service) {
        return new UBIDLdapFilter(
                FilterId.SERVER_BY_SERVICE,
                Filter.createANDFilter(
                        FILTER_ALL_SERVERS,
                        Filter.createEqualityFilter(Provisioning.A_zimbraServiceEnabled, service)));
    }

    @Override
    public ZLdapFilter serverByAlwaysOnCluster(String clusterId) {
        return new UBIDLdapFilter(
                FilterId.SERVER_BY_ALWAYSONCLUSTER,
                Filter.createANDFilter(
                        FILTER_ALL_SERVERS,
                        Filter.createEqualityFilter(Provisioning.A_zimbraAlwaysOnClusterId, clusterId)));
    }

    @Override
    public ZLdapFilter serverByServiceAndAlwaysOnCluster(String service, String clusterId) {
        if (clusterId == null) {
            return serverByService(service);
        } else if (service == null) {
            return serverByAlwaysOnCluster(clusterId);
        } else {
            return new UBIDLdapFilter(
                FilterId.SERVERY_BY_SERVICE_AND_ALWAYSONCLUSTER,
                Filter.createANDFilter(
                        FILTER_ALL_SERVERS,
                        Filter.createEqualityFilter(Provisioning.A_zimbraServiceEnabled, service),
                        Filter.createEqualityFilter(Provisioning.A_zimbraAlwaysOnClusterId, clusterId)));
        }
    }

    /*
     * alwaysOnCluster
     */
    @Override
    public ZLdapFilter allAlwaysOnClusters() {
        return new UBIDLdapFilter(
                FilterId.ALL_ALWAYSONCLUSTERS,
                FILTER_ALL_ALWAYSONCLUSTERS);
    }

    @Override
    public ZLdapFilter alwaysOnClusterById(String id) {
        return new UBIDLdapFilter(
                FilterId.ALWAYSONCLUSTER_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_ALWAYSONCLUSTERS));
    }

    /*
     * UC service
     */
    @Override
    public ZLdapFilter allUCServices() {
        return new UBIDLdapFilter(
                FilterId.ALL_UC_SERVICES,
                FILTER_ALL_UC_SERVICES);
    }

    @Override
    public ZLdapFilter ucServiceById(String id) {
        return new UBIDLdapFilter(
                FilterId.UC_SERVICE_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_UC_SERVICES));
    }


    /*
     * share locator
     */
    @Override
    public ZLdapFilter shareLocatorById(String id) {
        return new UBIDLdapFilter(
                FilterId.SHARE_LOCATOR_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_cn, id),
                        FILTER_ALL_SHARE_LOCATORS));
    }


    /*
     * signature
     */
    @Override
    public ZLdapFilter allSignatures() {
        return new UBIDLdapFilter(
                FilterId.ALL_SIGNATURES,
                FILTER_ALL_SIGNATURES);
    }

    @Override
    public ZLdapFilter signatureById(String id) {
        return new UBIDLdapFilter(
                FilterId.SIGNATURE_BY_ID,
                Filter.createANDFilter(
                        FILTER_ALL_SIGNATURES,
                        Filter.createEqualityFilter(Provisioning.A_zimbraSignatureId, id)));
    }


    /*
     * XMPPComponent
     */
    @Override
    public ZLdapFilter allXMPPComponents() {
        return new UBIDLdapFilter(
                FilterId.ALL_XMPP_COMPONENTS,
                FILTER_ALL_XMPP_COMPONENTS);
    }

    @Override
    public ZLdapFilter imComponentById(String id) {
        return new UBIDLdapFilter(
                FilterId.XMPP_COMPONENT_BY_ZIMBRA_XMPP_COMPONENT_ID,
                Filter.createANDFilter(
                        FILTER_ALL_XMPP_COMPONENTS,
                        Filter.createEqualityFilter("zimbraXMPPComponentId", id)));
    }

    @Override
    public ZLdapFilter xmppComponentById(String id) {
        return new UBIDLdapFilter(
                FilterId.XMPP_COMPONENT_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_XMPP_COMPONENTS));
    }


    /*
     * zimlet
     */
    @Override
    public ZLdapFilter allZimlets() {
        return new UBIDLdapFilter(
                FilterId.ALL_ZIMLETS,
                FILTER_ALL_ZIMLETS);
    }


    /*
     * AD
     */
    @Override
    public ZLdapFilter memberOf(String dnOfGroup) {
        return new UBIDLdapFilter(
                FilterId.MEMBER_OF,
                Filter.createEqualityFilter(LdapConstants.ATTR_memberOf, dnOfGroup));
    }

    /*
     * Velodrome
     */
    private Filter velodromePrimaryEmailOnDomainFilter(String domainName) {
        return Filter.createSubstringFilter(
                Provisioning.A_zimbraMailDeliveryAddress, null, null, "@" + domainName);
    }

    private Filter velodromeMailOrZimbraMailAliasOnDomainFilter(String domainName) {
        return Filter.createORFilter(
                Filter.createSubstringFilter( Provisioning.A_mail, null, null, "@" + domainName),
                Filter.createSubstringFilter(Provisioning.A_zimbraMailAlias, null, null, "@" + domainName));
    }

    @Override
    public ZLdapFilter velodromeAllAccountsByDomain(String domainName) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS,
                        velodromePrimaryEmailOnDomainFilter(domainName)));
    }

    @Override
    public ZLdapFilter velodromeAllAccountsOnlyByDomain(String domainName) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        velodromePrimaryEmailOnDomainFilter(domainName)));
    }

    @Override
    public ZLdapFilter velodromeAllCalendarResourcesByDomain(String domainName) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN,
                Filter.createANDFilter(
                        FILTER_ALL_CALENDAR_RESOURCES,
                        velodromePrimaryEmailOnDomainFilter(domainName)));
    }

    @Override
    public ZLdapFilter velodromeAllAccountsByDomainAndServer(
            String domainName, String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN_AND_SERVER,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS,
                        homedOnServerFilter(serverServiceHostname),
                        velodromePrimaryEmailOnDomainFilter(domainName)));
    }

    @Override
    public  ZLdapFilter velodromeAllAccountsOnlyByDomainAndServer(
            String domainName, String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN_AND_SERVER,
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY,
                        homedOnServerFilter(serverServiceHostname),
                        velodromePrimaryEmailOnDomainFilter(domainName)));
    }

    @Override
    public  ZLdapFilter velodromeAllCalendarResourcesByDomainAndServer(
            String domainName, String serverServiceHostname) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN_AND_SERVER,
                Filter.createANDFilter(
                        FILTER_ALL_CALENDAR_RESOURCES,
                        homedOnServerFilter(serverServiceHostname),
                        velodromePrimaryEmailOnDomainFilter(domainName)));

    }

    /**
     * DistributionLists in customDIT do not have the "zimbraMailDeliveryAddress" attribute.
     * From an earlier comment in CustomLdapDIT we can't tell whether the main email is "zimbraMailAlias" or "mail".
     * So, accept either.
     */
    @Override
    public ZLdapFilter velodromeAllDistributionListsByDomain(String domainName) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_DISTRIBUTION_LISTS_BY_DOMAIN,
                Filter.createANDFilter(
                        FILTER_ALL_DISTRIBUTION_LISTS,
                        velodromeMailOrZimbraMailAliasOnDomainFilter (domainName)));
    }

    /**
     * DistributionLists in customDIT do not have the "zimbraMailDeliveryAddress" attribute.
     * From an earlier comment in CustomLdapDIT we can't tell whether the main email is "zimbraMailAlias" or "mail".
     * So, accept either.
     */
    @Override
    public ZLdapFilter velodromeAllGroupsByDomain(String domainName) {
        return new UBIDLdapFilter(
                FilterId.VELODROME_ALL_GROUPS_BY_DOMAIN,
                Filter.createANDFilter(
                        FILTER_ALL_GROUPS,
                        velodromeMailOrZimbraMailAliasOnDomainFilter (domainName)));
    }

    @Override
    public ZLdapFilter dnSubtreeMatch(String... dns) {
        List<Filter> filters = Lists.newArrayList();
        for (String dn : dns) {
            filters.add(Filter.createExtensibleMatchFilter(
                    LdapConstants.DN_SUBTREE_MATCH_ATTR,
                    LdapConstants.DN_SUBTREE_MATCH_MATCHING_RULE,
                    false,
                    dn));
        }

        return new UBIDLdapFilter(
                FilterId.DN_SUBTREE_MATCH,
                Filter.createORFilter(filters));
    }

    @Override
    public ZLdapFilter habOrgUnitByName(String name) {
        return new UBIDLdapFilter(
                FilterId.HAB_ORG_UNIT_BY_NAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_ou, name),
                        Filter.createEqualityFilter(LdapConstants.ATTR_objectClass, "organizationalUnit"))
                );
    }

    /*
     * address lists
     */
    @Override
    public ZLdapFilter allAddressLists() {
        return new UBIDLdapFilter(
                FilterId.ALL_ADDRESS_LISTS,
                FILTER_ALL_ADDRESS_LISTS
                );
    }

    @Override
    public ZLdapFilter addressListById(String id) {
        return new UBIDLdapFilter(
                FilterId.ADDRESS_LIST_BY_ID,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_ADDRESS_LISTS));
    }

    @Override
    public ZLdapFilter addressListByName(String name) {
        return new UBIDLdapFilter(
                FilterId.ADDRESS_LIST_BY_NAME,
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_uid, name),
                        FILTER_ALL_ADDRESS_LISTS));
    }
}
