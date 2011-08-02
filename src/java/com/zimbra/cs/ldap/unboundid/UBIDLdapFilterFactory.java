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
package com.zimbra.cs.ldap.unboundid;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

public class UBIDLdapFilterFactory extends ZLdapFilterFactory {
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public ZLdapFilter fromStringFilter(String stringFilter) throws LdapException {
        return new UBIDLdapFilter(stringFilter);
    }
    
    /*
     * canned filters
     */
    private static UBIDLdapFilter FILTER_ALL_ACCOUNTS;       // including calendar resources
    private static UBIDLdapFilter FILTER_ALL_ACCOUNTS_ONLY;  // excluding calendar resources
    private static UBIDLdapFilter FILTER_ALL_ADMIN_ACCOUNTS;
    private static UBIDLdapFilter FILTER_ALL_CALENDAR_RESOURCES;
    private static UBIDLdapFilter FILTER_ALL_COSES;
    private static UBIDLdapFilter FILTER_ALL_DATASOURCES;
    private static UBIDLdapFilter FILTER_ALL_DISTRIBUTION_LISTS;
    private static UBIDLdapFilter FILTER_ALL_DOMAINS;
    private static UBIDLdapFilter FILTER_ALL_DYNAMIC_GROUPS;
    private static UBIDLdapFilter FILTER_ALL_GROUPS;
    private static UBIDLdapFilter FILTER_ALL_IDENTITIES;
    private static UBIDLdapFilter FILTER_ALL_MIME_ENTRIES;
    private static UBIDLdapFilter FILTER_ALL_NON_SYSTEM_ACCOUNTS;
    private static UBIDLdapFilter FILTER_ALL_SERVERS;
    private static UBIDLdapFilter FILTER_ALL_SIGNATURES;
    private static UBIDLdapFilter FILTER_ALL_XMPP_COMPONENTS;
    private static UBIDLdapFilter FILTER_ALL_ZIMLETS;
    private static UBIDLdapFilter FILTER_ANY_ENTRY;
    private static UBIDLdapFilter FILTER_DOMAIN_LABEL;
    private static UBIDLdapFilter FILTER_HAS_SUBORDINATES;
    private static UBIDLdapFilter FILTER_IS_SYSTEM_RESOURCE;
    
    private static boolean initialized = false;
    
    static synchronized void initialize() throws LdapException {
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
        FILTER_ALL_ACCOUNTS = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraAccount"));

        FILTER_ALL_ADMIN_ACCOUNTS = new UBIDLdapFilter(
                Filter.createORFilter(
                        Filter.createEqualityFilter("zimbraIsAdminAccount", LdapConstants.LDAP_TRUE),
                        Filter.createEqualityFilter("zimbraIsDelegatedAdminAccount", LdapConstants.LDAP_TRUE),
                        Filter.createEqualityFilter("zimbraIsDomainAdminAccount", LdapConstants.LDAP_TRUE)));

        FILTER_ALL_CALENDAR_RESOURCES = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraCalendarResource"));
        
        FILTER_ALL_COSES = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraCOS"));
        
        FILTER_ALL_DATASOURCES = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraDataSource"));
        
        FILTER_ALL_DISTRIBUTION_LISTS = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraDistributionList"));
        
        FILTER_ALL_DOMAINS = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraDomain"));
        
        FILTER_ALL_DYNAMIC_GROUPS = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraGroup"));
        
        FILTER_ALL_IDENTITIES = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraIdentity"));
        
        FILTER_ALL_MIME_ENTRIES = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraMimeEntry"));
        
        FILTER_ALL_SERVERS = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraServer"));
        
        FILTER_ALL_SIGNATURES = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraSignature"));
        
        FILTER_ALL_XMPP_COMPONENTS = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraXMPPComponent"));
        
        FILTER_ALL_ZIMLETS = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "zimbraZimletEntry"));
        
        FILTER_ANY_ENTRY = new UBIDLdapFilter(
                Filter.createPresenceFilter("objectclass"));
        
        FILTER_DOMAIN_LABEL = new UBIDLdapFilter(
                Filter.createEqualityFilter("objectclass", "dcObject"));
        
        FILTER_HAS_SUBORDINATES = new UBIDLdapFilter(
                Filter.createEqualityFilter("hasSubordinates", "TRUE"));
        
        FILTER_IS_SYSTEM_RESOURCE = new UBIDLdapFilter(
                Filter.createEqualityFilter("zimbraIsSystemResource", "TRUE"));
        
        /*
         * filters built on top of other filters
         */
        FILTER_ALL_ACCOUNTS_ONLY = new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS.getNative(),
                        Filter.createNOTFilter(FILTER_ALL_CALENDAR_RESOURCES.getNative())));

        FILTER_ALL_NON_SYSTEM_ACCOUNTS = new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS_ONLY.getNative(),
                        Filter.createNOTFilter(FILTER_IS_SYSTEM_RESOURCE.getNative())));
        
        FILTER_ALL_GROUPS = new UBIDLdapFilter(
                Filter.createORFilter(
                        FILTER_ALL_DISTRIBUTION_LISTS.getNative(),
                        FILTER_ALL_DYNAMIC_GROUPS.getNative()));
    }


    
    /*
     * operational
     */
    @Override
    public ZLdapFilter hasSubordinates() {
        return FILTER_HAS_SUBORDINATES;
    }
    
    /*
     * general
     */
    @Override
    public ZLdapFilter anyEntry() {
        return FILTER_ANY_ENTRY;
    }
    
    @Override
    public ZLdapFilter fromFilterString(String filterString) throws LdapException {
        try {
            return new UBIDLdapFilter(Filter.create(filterString));
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
        }
    }
    
    
    /*
     * account
     */
    @Override
    public ZLdapFilter allAccounts() {
        return FILTER_ALL_ACCOUNTS;
    }
    
    @Override
    public ZLdapFilter allNonSystemAccounts() {
        return FILTER_ALL_NON_SYSTEM_ACCOUNTS;
    }

    @Override
    public ZLdapFilter accountByForeignPrincipal(String foreignPrincipal) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraForeignPrincipal, foreignPrincipal),
                        FILTER_ALL_ACCOUNTS.getNative()));
    }

    @Override
    public ZLdapFilter accountById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_ACCOUNTS.getNative()));
    }

    @Override
    public ZLdapFilter accountByName(String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createORFilter(
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailDeliveryAddress, name),
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name)),
                        FILTER_ALL_ACCOUNTS.getNative()));
    }

    @Override
    public ZLdapFilter accountByMemberOf(String dynGroupId) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMemberOf, dynGroupId),
                        FILTER_ALL_ACCOUNTS.getNative()));
    }
    
    @Override
    public ZLdapFilter adminAccountByRDN(String namingRdnAttr, String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(namingRdnAttr, name),
                        FILTER_ALL_ACCOUNTS.getNative()));
    }

    @Override
    public ZLdapFilter adminAccountByAdminFlag() {
        return FILTER_ALL_ADMIN_ACCOUNTS;
    }

    @Override
    public ZLdapFilter accountsHomedOnServer(Server server) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_ACCOUNTS.getNative(),
                        ((UBIDLdapFilter) homedOnServer(server)).getNative()));
    }

    @Override
    public ZLdapFilter homedOnServer(String serverName) {
        return new UBIDLdapFilter(
                Filter.createEqualityFilter(Provisioning.A_zimbraMailHost, serverName));
    }

    @Override
    public ZLdapFilter accountsOnServerOnCosHasSubordinates(Server server, String cosId) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        ((UBIDLdapFilter) allAccounts()).getNative(),
                        ((UBIDLdapFilter) homedOnServer(server)).getNative(),
                        ((UBIDLdapFilter) hasSubordinates()).getNative(),
                        Filter.createORFilter(
                                Filter.createNOTFilter(Filter.createPresenceFilter(Provisioning.A_zimbraCOSId)),
                                Filter.createEqualityFilter(Provisioning.A_zimbraCOSId, cosId))));
    }

    
    /*
     * calendar resource
     */
    @Override
    public ZLdapFilter allCalendarResources() {
        return FILTER_ALL_CALENDAR_RESOURCES;
    }

    @Override
    public ZLdapFilter calendarResourceByForeignPrincipal(String foreignPrincipal) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraForeignPrincipal, foreignPrincipal),
                        FILTER_ALL_CALENDAR_RESOURCES.getNative()));
    }

    @Override
    public ZLdapFilter calendarResourceById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_CALENDAR_RESOURCES.getNative()));
    }

    @Override
    public ZLdapFilter calendarResourceByName(String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createORFilter(
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailDeliveryAddress, name),
                                Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name)),
                        FILTER_ALL_CALENDAR_RESOURCES.getNative()));        
    }

    
    /*
     * cos
     */
    @Override
    public ZLdapFilter allCoses() {
        return FILTER_ALL_COSES;
    }

    @Override
    public ZLdapFilter cosById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_COSES.getNative()));
    }

    @Override
    public ZLdapFilter cosesByMailHostPool(String server) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_COSES.getNative(),
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailHostPool, server)));
    }

    
    /*
     * data source
     */
    @Override
    public ZLdapFilter allDataSources() {
        return FILTER_ALL_DATASOURCES;
    }

    @Override
    public ZLdapFilter dataSourceById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_DATASOURCES.getNative(),
                        Filter.createEqualityFilter(Provisioning.A_zimbraDataSourceId, id)));
    }

    @Override
    public ZLdapFilter dataSourceByName(String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_DATASOURCES.getNative(),
                        Filter.createEqualityFilter(Provisioning.A_zimbraDataSourceName, name)));
    }

    
    /*
     * distribution list
     */
    @Override
    public ZLdapFilter allDistributionLists() {
        return FILTER_ALL_DISTRIBUTION_LISTS;
    }

    @Override
    public ZLdapFilter distributionListById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_DISTRIBUTION_LISTS.getNative()));
    }

    @Override
    public ZLdapFilter distributionListByName(String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name),
                        FILTER_ALL_DISTRIBUTION_LISTS.getNative()));
    }
    

    /*
     * dynamic group
     */
    @Override
    public ZLdapFilter dynamicGroupById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_DYNAMIC_GROUPS.getNative()));
    }
    
    @Override
    public ZLdapFilter dynamicGroupByName(String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name),
                        FILTER_ALL_DYNAMIC_GROUPS.getNative()));
    }
    
    
    /*
     * group (distribution list or dynamic group)
     */
    @Override
    public ZLdapFilter allGroups() {
        return FILTER_ALL_GROUPS;
    }
    
    @Override
    public ZLdapFilter groupById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_GROUPS.getNative()));
    }
    
    @Override
    public ZLdapFilter groupByName(String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraMailAlias, name),
                        FILTER_ALL_GROUPS.getNative())); 
    }
    
    
    /*
     * domain
     */
    @Override
    public ZLdapFilter allDomains() {
        return FILTER_ALL_DOMAINS;
    }

    @Override
    public ZLdapFilter domainById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_DOMAINS.getNative()));
    }

    @Override
    public ZLdapFilter domainByName(String name) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraDomainName, name),
                        FILTER_ALL_DOMAINS.getNative()));
    }

    @Override
    public ZLdapFilter domainByKrb5Realm(String krb5Realm) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraAuthKerberos5Realm, krb5Realm),
                        FILTER_ALL_DOMAINS.getNative()));
    }
    
    @Override
    public ZLdapFilter domainByVirtualHostame(String virtualHostname) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraVirtualHostname, virtualHostname),
                        FILTER_ALL_DOMAINS.getNative()));
    }
    
    @Override
    public ZLdapFilter domainByForeignName(String foreignName) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraForeignName, foreignName),
                        FILTER_ALL_DOMAINS.getNative()));
    }

    @Override
    public ZLdapFilter domainLabel() {
        return FILTER_DOMAIN_LABEL;
    }
    
    @Override
    public ZLdapFilter domainLockedForEagerAutoProvision() {
        return new UBIDLdapFilter(Filter.createNOTFilter(
                Filter.createPresenceFilter(Provisioning.A_zimbraAutoProvLock)));
    }
    
    
    /*
     * global config
     */
    public ZLdapFilter globalConfig() {
        return new UBIDLdapFilter(Filter.createEqualityFilter(Provisioning.A_cn, "config"));
    }
    
    
    /*
     * identity
     */
    @Override
    public ZLdapFilter allIdentities() {
        return FILTER_ALL_IDENTITIES;
    }

    @Override
    public ZLdapFilter identityByName(String name) {
        // name = Filter.encodeValue(name);
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_IDENTITIES.getNative(),
                        Filter.createEqualityFilter(Provisioning.A_zimbraPrefIdentityName, name)));
    }

    
    /*
     * mime enrty
     */
    @Override
    public ZLdapFilter allMimeEntries() {
        return FILTER_ALL_MIME_ENTRIES;
    }

    @Override
    public ZLdapFilter mimeEntryByMimeType(String mimeType) {
        return new UBIDLdapFilter(
                Filter.createEqualityFilter(Provisioning.A_zimbraMimeType, mimeType));
    }
    

    /*
     * server
     */
    @Override
    public ZLdapFilter allServers() {
        return FILTER_ALL_SERVERS;
    }

    @Override
    public ZLdapFilter serverById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_SERVERS.getNative()));
    }

    @Override
    public ZLdapFilter serverByService(String service) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_SERVERS.getNative(),
                        Filter.createEqualityFilter(Provisioning.A_zimbraServiceEnabled, service)));
    }

    
    /*
     * signature
     */
    @Override
    public ZLdapFilter allSignatures() {
        return FILTER_ALL_SIGNATURES;
    }

    @Override
    public ZLdapFilter signatureById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_SIGNATURES.getNative(),
                        Filter.createEqualityFilter(Provisioning.A_zimbraSignatureId, id)));
    }

    
    /* 
     * XMPPComponent
     */
    @Override
    public ZLdapFilter allXMPPComponents() {
        return FILTER_ALL_XMPP_COMPONENTS;
    }
    
    @Override
    public ZLdapFilter imComponentById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        FILTER_ALL_XMPP_COMPONENTS.getNative(),
                        Filter.createEqualityFilter("zimbraXMPPComponentId", id)));
    }

    @Override
    public ZLdapFilter xmppComponentById(String id) {
        return new UBIDLdapFilter(
                Filter.createANDFilter(
                        Filter.createEqualityFilter(Provisioning.A_zimbraId, id),
                        FILTER_ALL_XMPP_COMPONENTS.getNative()));
    }


    /*
     * zimlet
     */
    @Override
    public ZLdapFilter allZimlets() {
        return FILTER_ALL_ZIMLETS;
    }
}
