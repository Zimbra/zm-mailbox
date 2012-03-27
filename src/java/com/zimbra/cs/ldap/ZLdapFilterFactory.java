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
package com.zimbra.cs.ldap;

import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.cs.account.Provisioning;

public abstract class ZLdapFilterFactory extends ZLdapElement {

    private static ZLdapFilterFactory SINGLETON;

    static synchronized void setInstance(ZLdapFilterFactory factory) {
        assert(SINGLETON == null);
        SINGLETON = factory;
    }

    public static ZLdapFilterFactory getInstance() {
        assert(SINGLETON != null);
        return SINGLETON;
    }

    public static enum FilterId {
        ACCOUNT_BY_ID(SINGLETON.accountById("{ACCOUNT-ID}")),
        ACCOUNT_BY_FOREIGN_PRINCIPAL(SINGLETON.accountByForeignPrincipal("{FOREIGN-PRINCIPAL}")),
        ACCOUNT_BY_MEMBEROF(SINGLETON.accountByMemberOf("{DYNAMIC-GROUP-ID}")),
        ACCOUNT_BY_NAME(SINGLETON.accountByName("{ACCOUNT-NAME}")),
        ACCOUNTS_BY_EXTERNAL_GRANT(SINGLETON.accountsByExternalGrant("{GRANTEE-EMAIL}")),
        ACCOUNTS_BY_GRANTS(SINGLETON.accountsByGrants(Lists.newArrayList("GRANTEE-ID-1", "GRANTEE-ID-2", "..."), true, true)),
        ACCOUNTS_HOMED_ON_SERVER(SINGLETON.accountsHomedOnServer("{SERVER-SERVICE-HOSTNAME}")),
        ACCOUNTS_HOMED_ON_SERVER_ACCOUNTS_ONLY(SINGLETON.accountsHomedOnServerAccountsOnly("{SERVER-SERVICE-HOSTNAME}")),
        ACCOUNTS_ON_SERVER_AND_COS_HAS_SUBORDINATES(SINGLETON.accountsOnServerAndCosHasSubordinates("{SERVER-SERVICE-HOSTNAME}", "{COS-ID}")),

        ADDRS_EXIST(SINGLETON.addrsExist(new String[]{"{ADDR-1}", "ADDR-2", "..."})),
        ADMIN_ACCOUNT_BY_RDN(SINGLETON.adminAccountByRDN("{NAMING-RDN-ATTR}", "{NAME}")),
        ALL_ACCOUNTS(SINGLETON.allAccounts()),
        ALL_ACCOUNTS_ONLY(SINGLETON.allAccountsOnly()),
        ALL_ADMIN_ACCOUNTS(SINGLETON.allAdminAccounts()),
        ALL_ALIASES(SINGLETON.allAliases()),
        ALL_CALENDAR_RESOURCES(SINGLETON.allCalendarResources()),
        ALL_COSES(SINGLETON.allCoses()),
        ALL_DATA_SOURCES(SINGLETON.allDataSources()),
        ALL_DISTRIBUTION_LISTS(SINGLETON.allDistributionLists()),
        ALL_DOMAINS(SINGLETON.allDomains()),
        ALL_GROUPS(SINGLETON.allGroups()),
        ALL_IDENTITIES(SINGLETON.allIdentities()),
        ALL_MIME_ENTRIES(SINGLETON.allMimeEntries()),
        ALL_NON_SYSTEM_ACCOUNTS(SINGLETON.allNonSystemAccounts()),
        ALL_NON_SYSTEM_ARCHIVING_ACCOUNTS(SINGLETON.allNonSystemArchivingAccounts()),
        ALL_NON_SYSTEM_INTERNAL_ACCOUNTS(SINGLETON.allNonSystemInternalAccounts()),
        ALL_SERVERS(SINGLETON.allServers()),
        ALL_SIGNATURES(SINGLETON.allSignatures()),
        ALL_XMPP_COMPONENTS(SINGLETON.allXMPPComponents()),
        ALL_ZIMLETS(SINGLETON.allZimlets()),
        ANY_ENTRY(SINGLETON.anyEntry()),
        CALENDAR_RESOURCE_BY_FOREIGN_PRINCIPAL(SINGLETON.calendarResourceByForeignPrincipal("{FOREIGN-PRINCIPAL}")),
        CALENDAR_RESOURCE_BY_ID(SINGLETON.calendarResourceById("{CALENDAR-RESOURCE-ID}")),
        CALENDAR_RESOURCE_BY_NAME(SINGLETON.calendarResourceByName("{CALENDAR-RESOURCE-NAME}")),
        CALENDAR_RESOURCES_HOMED_ON_SERVER(SINGLETON.calendarResourcesHomedOnServer("{SERVER-SERVICE-HOSTNAME}")),

        CMB_SEARCH_ACCOUNTS_ONLY(SINGLETON.CMBSearchAccountsOnly()),
        CMB_SEARCH_ACCOUNTS_ONLY_WITH_ARCHIVE(SINGLETON.CMBSearchAccountsOnlyWithArchive()),
        CMB_SEARCH_NON_SYSTEM_RESOURCE_ACCOUNTS_ONLY(SINGLETON.CMBSearchNonSystemResourceAccountsOnly()),

        COS_BY_ID(SINGLETON.cosById("{COS-ID}")),
        COSES_BY_MAILHOST_POOL(SINGLETON.cosesByMailHostPool("{SERVER-ID}")),
        CREATED_LATEROREQUAL(SINGLETON.createdLaterOrEqual("{GENERALIZED_TIME}")),
        DATA_SOURCE_BY_ID(SINGLETON.dataSourceById("{DATA-SOURCE-ID}")),
        DATA_SOURCE_BY_NAME(SINGLETON.dataSourceByName("{DATA-SOURCE-NAME}")),
        DISTRIBUTION_LIST_BY_ID(SINGLETON.distributionListById("{DISTRIBUTION-LIST-ID}")),
        DISTRIBUTION_LIST_BY_NAME(SINGLETON.distributionListByName("{DISTRIBUTION-LIST-NAME}")),
        DISTRIBUTION_LISTS_BY_MEMBER_ADDRS(SINGLETON.distributionListsByMemberAddrs(new String[]{"{ADDR-1}", "ADDR-2", "..."})),

        DN_SUBTREE_MATCH(SINGLETON.dnSubtreeMatch("dn1", "dn2")),

        DOMAIN_BY_ID(SINGLETON.domainById("{DOMAIN-ID}")),
        DOMAIN_BY_NAME(SINGLETON.domainByName("{DOMAIN-NAME}")),
        DOMAIN_BY_KRB5_REALM(SINGLETON.domainByKrb5Realm("{DOMAIN-KRB5-REALM}")),
        DOMAIN_BY_VIRTUAL_HOSTNAME(SINGLETON.domainByVirtualHostame("{DOMAIN-VIRTUAL-HOSTNAME}")),
        DOMAIN_BY_FOREIGN_NAME(SINGLETON.domainByForeignName("{DOMAIN-FOREIGN-NAME}")),
        DOMAIN_LABEL(SINGLETON.domainLabel()),
        DOMAIN_LOCKED_FOR_AUTO_PROVISION(SINGLETON.domainLockedForEagerAutoProvision()),
        DYNAMIC_GROUP_BY_ID(SINGLETON.dynamicGroupById("{DYNAMIC-GROUP-ID}")),
        DYNAMIC_GROUP_BY_NAME(SINGLETON.dynamicGroupByName("{DYNAMIC-GROUP-NAME}")),
        EXTERNAL_ACCOUNTS_HOMED_ON_SERVER(SINGLETON.externalAccountsHomedOnServer("{SERVER-SERVICE-HOSTNAME}")),
        GLOBAL_CONFIG(SINGLETON.globalConfig()),
        GROUP_BY_ID(SINGLETON.groupById("{GROUP-ID}")),
        GROUP_BY_NAME(SINGLETON.groupByName("{GROUP-NAME}")),
        HAS_SUBORDINATES(SINGLETON.hasSubordinates()),
        HOMED_ON_SERVER(SINGLETON.homedOnServer("{SERVER-SERVICE-HOSTNAME}")),
        IDENTITY_BY_NAME(SINGLETON.identityByName("{IDENTITY-NAME}")),
        MEMBER_OF(SINGLETON.memberOf("{DN-OF-GROUP}")),
        MIME_ENTRY_BY_MIME_TYPE(SINGLETON.mimeEntryByMimeType("{MIME-TYPE}")),

        SERVER_BY_ID(SINGLETON.serverById("{SERVER-ID}")),
        SERVER_BY_SERVICE(SINGLETON.serverByService("{SERVICE}")),
        SHARE_LOCATOR_BY_ID(SINGLETON.shareLocatorById("{SHARE-LOCATOR-ID}")),
        SIGNATURE_BY_ID(SINGLETON.signatureById("{SIGNATURE-ID}")),
        XMPP_COMPONENT_BY_ID(SINGLETON.xmppComponentById("{XMPP-COMPOMENT-ID}")),
        XMPP_COMPONENT_BY_ZIMBRA_XMPP_COMPONENT_ID(SINGLETON.imComponentById("{ZIMBRA-XMPP-COMPOMENT-ID}")),

        // filters only used in the Velodrome DIT
        VELODROME_ALL_ACCOUNTS_BY_DOMAIN(SINGLETON.velodromeAllAccountsByDomain("{DOMAIN-NAME}")),
        VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN(SINGLETON.velodromeAllAccountsOnlyByDomain("{DOMAIN-NAME}")),
        VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN(SINGLETON.velodromeAllCalendarResourcesByDomain("{DOMAIN-NAME}")),
        VELODROME_ALL_ACCOUNTS_BY_DOMAIN_AND_SERVER(SINGLETON.velodromeAllAccountsByDomainAndServer("{DOMAIN-NAME}", "{SERVER-SERVICE-HOSTNAME}")),
        VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN_AND_SERVER(SINGLETON.velodromeAllAccountsOnlyByDomainAndServer("{DOMAIN-NAME}", "{SERVER-SERVICE-HOSTNAME}")),
        VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN_AND_SERVER(SINGLETON.velodromeAllCalendarResourcesByDomainAndServer("{DOMAIN-NAME}", "{SERVER-SERVICE-HOSTNAME}")),


        //
        // =====================================
        // FilterId for fromFilterString() calls
        // =====================================
        //
        ACCOUNT_BY_SSL_CLENT_CERT_PRINCIPAL_MAP(SINGLETON.allAccounts() + " AND " +
                "filter in " + Provisioning.A_zimbraMailSSLClientCertPrincipalMap),
        ADMIN_SEARCH("Admin search"),
        AUTO_PROVISION_ADMIN_SEARCH("Admin entered filter"),
        AUTO_PROVISION_SEARCH("Filter in " + Provisioning.A_zimbraAutoProvLdapSearchFilter),
        AUTO_PROVISION_SEARCH_CREATED_LATERTHAN("Filter in " +
                Provisioning.A_zimbraAutoProvLdapSearchFilter + " AND " +
                SINGLETON.createdLaterOrEqual("{GENERALIZED_TIME}")),
        EXTERNAL_GROUP("Filter in " + Provisioning.A_zimbraExternalGroupLdapSearchFilter),
        GAL_SEARCH("GAL search"),
        LDAP_AUTHENTICATE("Filter in " + Provisioning.A_zimbraAuthLdapSearchFilter),
        NGINX_GET_DOMAIN_BY_SERVER_IP("Filter in "),
        NGINX_GET_PORT_BY_MAILHOST("Filter in "),
        NGINX_GET_MAILHOST("Filter in " + Provisioning.A_zimbraReverseProxyMailHostQuery),
        RENAME_DOMAIN("Search entries during RenameDomain"),
        SEARCH_ALIAS_TARGET("Search alias target entry"),
        SEARCH_GRANTEE("Search grantee for revoking orphan grants"),
        SMIME_LOOKUP("Filter in " + Provisioning.A_zimbraSMIMELdapFilter),

        UNITTEST("UNITTEST"),
        LDAP_UPGRADE("LDAP_UPGRADE"),

        TODO("TODO");

        private String template;

        private FilterId(ZLdapFilter template) {
            this(template.toFilterString());
        }

        private FilterId(String template) {
            this.template = template;
        }

        public String getStatString() {
            return name() + ": " + template;
        }
    }

    public static void main(String[] args) {
        LdapClient.getInstance();  // init

        for (FilterId filderId : FilterId.values()) {
            System.out.println(filderId.getStatString());
        }

    }

    /**
     * Encodes the provided value into a form suitable for use as the assertion value
     * in the string representation of a search filter.
     *
     * @param value
     * @return
     */
    public abstract String encodeValue(String value);

    protected String encloseFilterIfNot(String filterString) {
        if (filterString.startsWith("(") && filterString.endsWith(")")) {
            return filterString;
        } else {
            return "(" + filterString + ")";
        }
    }

    /*
     * operational
     */
    public abstract ZLdapFilter hasSubordinates();
    public abstract ZLdapFilter createdLaterOrEqual(String generalizedTime);

    /*
     * general
     */
    public abstract ZLdapFilter anyEntry();
    public abstract ZLdapFilter fromFilterString(FilterId filterId, String filterString)
    throws LdapException;
    public abstract ZLdapFilter andWith(ZLdapFilter filter, ZLdapFilter otherFilter);
    public abstract ZLdapFilter negate(ZLdapFilter filter);

    public String presenceFilter(String attr) {
        return String.format("(%s%s%s)", attr, LdapConstants.FILTER_TYPE_EQUAL, LdapConstants.FILTER_VALUE_ANY);
    }

    public String equalityFilter(String attr, String value, boolean valueIsRaw) {
        return String.format("(%s%s%s)", attr, LdapConstants.FILTER_TYPE_EQUAL,
                valueIsRaw ? encodeValue(value) : value);
    }

    public String greaterOrEqualFilter(String attr, String value, boolean valueIsRaw) {
        return String.format("(%s%s%s)",
                attr, LdapConstants.FILTER_TYPE_GREATER_OR_EQUAL,
                valueIsRaw ? encodeValue(value) : value);
    }

    public String lessOrEqualFilter(String attr, String value, boolean valueIsRaw) {
        return String.format("(%s%s%s)",
                attr, LdapConstants.FILTER_TYPE_LESS_OR_EQUAL,
                valueIsRaw ? encodeValue(value) : value);
    }

    public String startsWithFilter(String attr, String value, boolean valueIsRaw) {
        return String.format("(%s%s%s%s)",
                attr, LdapConstants.FILTER_TYPE_EQUAL,
                valueIsRaw ? encodeValue(value) : value,
                LdapConstants.FILTER_VALUE_ANY);
    }

    public String endsWithFilter(String attr, String value, boolean valueIsRaw) {
        return String.format("(%s%s%s%s)",
                attr, LdapConstants.FILTER_TYPE_EQUAL,
                LdapConstants.FILTER_VALUE_ANY,
                valueIsRaw ? encodeValue(value) : value);
    }

    public String substringFilter(String attr, String value, boolean valueIsRaw) {
        return String.format("(%s%s%s%s%s)",
                attr, LdapConstants.FILTER_TYPE_EQUAL,
                LdapConstants.FILTER_VALUE_ANY,
                valueIsRaw ? encodeValue(value) : value,
                LdapConstants.FILTER_VALUE_ANY);
    }

    /*
     * Mail target (accounts and groups)
     */
    public abstract ZLdapFilter addrsExist(String[] addrs);

    /*
     * account
     */
    public abstract ZLdapFilter allAccounts();
    public abstract ZLdapFilter allAccountsOnly();
    public abstract ZLdapFilter allAdminAccounts();
    public abstract ZLdapFilter allNonSystemAccounts();
    public abstract ZLdapFilter allNonSystemArchivingAccounts();
    public abstract ZLdapFilter allNonSystemInternalAccounts();
    public abstract ZLdapFilter accountByForeignPrincipal(String foreignPrincipal);
    public abstract ZLdapFilter accountById(String id);
    public abstract ZLdapFilter accountByMemberOf(String dynGroupId);
    public abstract ZLdapFilter accountByName(String name);
    public abstract ZLdapFilter adminAccountByRDN(String namingRdnAttr, String name);

    public abstract ZLdapFilter accountsHomedOnServer(String serverServiceHostname);
    public abstract ZLdapFilter accountsHomedOnServerAccountsOnly(String serverServiceHostname); // no calendar resources
    public abstract ZLdapFilter homedOnServer(String serverServiceHostname);
    public abstract ZLdapFilter accountsOnServerAndCosHasSubordinates(
            String serverServiceHostname, String cosId);
    public abstract ZLdapFilter externalAccountsHomedOnServer(String serverServiceHostname);
    public abstract ZLdapFilter accountsByExternalGrant(String granteeEmail);
    public abstract ZLdapFilter accountsByGrants(List<String> granteeIds,
            boolean includePublicShares, boolean includeAllAuthedShares);
    public abstract ZLdapFilter CMBSearchAccountsOnly();
    public abstract ZLdapFilter CMBSearchAccountsOnlyWithArchive();
    public abstract ZLdapFilter CMBSearchNonSystemResourceAccountsOnly();

    /*
     * alias
     */
    public abstract ZLdapFilter allAliases();

    /*
     * calendar resource
     */
    public abstract ZLdapFilter allCalendarResources();
    public abstract ZLdapFilter calendarResourceByForeignPrincipal(String foreignPrincipal);
    public abstract ZLdapFilter calendarResourceById(String id);
    public abstract ZLdapFilter calendarResourceByName(String name);
    public abstract ZLdapFilter calendarResourcesHomedOnServer(String serverServiceHostname);

    /*
     * cos
     */
    public abstract ZLdapFilter allCoses();
    public abstract ZLdapFilter cosById(String id);
    public abstract ZLdapFilter cosesByMailHostPool(String serverId);

    /*
     * data source
     */
    public abstract ZLdapFilter allDataSources();
    public abstract ZLdapFilter dataSourceById(String id);
    public abstract ZLdapFilter dataSourceByName(String name);

    /*
     * distribution list
     */
    public abstract ZLdapFilter allDistributionLists();
    public abstract ZLdapFilter distributionListById(String id);
    public abstract ZLdapFilter distributionListByName(String name);
    public abstract ZLdapFilter distributionListsByMemberAddrs(String memberAddrs[]);


    /*
     * dynamic group
     */
    public abstract ZLdapFilter dynamicGroupById(String id);
    public abstract ZLdapFilter dynamicGroupByName(String name);


    /*
     * groups (distribution list or dynamic group)
     */
    public abstract ZLdapFilter allGroups();
    public abstract ZLdapFilter groupById(String id);
    public abstract ZLdapFilter groupByName(String name);


    /*
     * domain
     */
    public abstract ZLdapFilter allDomains() ;
    public abstract ZLdapFilter domainById(String id);
    public abstract ZLdapFilter domainByName(String name);
    public abstract ZLdapFilter domainByKrb5Realm(String krb5Realm);
    public abstract ZLdapFilter domainByVirtualHostame(String virtualHostname);
    public abstract ZLdapFilter domainByForeignName(String foreignName);
    public abstract ZLdapFilter domainLabel();
    public abstract ZLdapFilter domainLockedForEagerAutoProvision();


    /*
     * global config
     */
    public abstract ZLdapFilter globalConfig();

    /*
     * identity
     */
    public abstract ZLdapFilter allIdentities();
    public abstract ZLdapFilter identityByName(String name);

    /*
     * mime enrty
     */
    public abstract ZLdapFilter allMimeEntries();
    public abstract ZLdapFilter mimeEntryByMimeType(String mimeType);

    /*
     * server
     */
    public abstract ZLdapFilter allServers();
    public abstract ZLdapFilter serverById(String id);
    public abstract ZLdapFilter serverByService(String service);

    /*
     * share locator
     */
    public abstract ZLdapFilter shareLocatorById(String id);

    /*
     * signature
     */
    public abstract ZLdapFilter allSignatures();
    public abstract ZLdapFilter signatureById(String id);

    /*
     * XMPPComponent
     */
    public abstract ZLdapFilter allXMPPComponents();
    public abstract ZLdapFilter imComponentById(String id);
    public abstract ZLdapFilter xmppComponentById(String id);

    /*
     * zimlet
     */
    public abstract ZLdapFilter allZimlets();


    /*
     * AD
     */
    public abstract ZLdapFilter memberOf(String dnOfGroup);

    /*
     * Velodrome
     */
    public abstract ZLdapFilter velodromeAllAccountsByDomain(String domainName);
    public abstract ZLdapFilter velodromeAllAccountsOnlyByDomain(String domainName);
    public abstract ZLdapFilter velodromeAllCalendarResourcesByDomain(String domainName);
    public abstract ZLdapFilter velodromeAllAccountsByDomainAndServer(
            String domainName, String serverServiceHostname);
    public abstract ZLdapFilter velodromeAllAccountsOnlyByDomainAndServer(
            String domainName, String serverServiceHostname);
    public abstract ZLdapFilter velodromeAllCalendarResourcesByDomainAndServer(
            String domainName, String serverServiceHostname);


    /*
     * util
     */
    public abstract ZLdapFilter dnSubtreeMatch(String... dns);
}
