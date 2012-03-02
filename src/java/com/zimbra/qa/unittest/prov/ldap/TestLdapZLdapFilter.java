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
package com.zimbra.qa.unittest.prov.ldap;

import java.util.List;

import org.junit.*;

import com.google.common.collect.Lists;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.qa.QA.Bug;

import static org.junit.Assert.*;

public class TestLdapZLdapFilter extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static ZLdapFilterFactory filterDactory;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        filterDactory = ZLdapFilterFactory.getInstance();
    }
    
    private static class LegacyLdapFilter {

        private static final String FILTER_ACCOUNT_OBJECTCLASS = "(objectClass=zimbraAccount)";
        private static final String FILTER_ACCOUNT_ONLY_OBJECTCLASS = "(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))";
        private static final String FILTER_ALIAS_OBJECTCLASS = "(objectClass=zimbraAlias)";
        private static final String FILTER_CALENDAR_RESOURCE_OBJECTCLASS = "(objectClass=zimbraCalendarResource)";
        private static final String FILTER_DISTRIBUTION_LIST_OBJECTCLASS = "(objectClass=zimbraDistributionList)";
        private static final String FILTER_DYNAMIC_GROUP_OBJECTCLASS = "(objectClass=zimbraGroup)";
        private static final String FILTER_GROUP_OBJECTCLASS = "(|(objectClass=zimbraGroup)(objectClass=zimbraDistributionList))";

        /*
         * operational
         */
        public static String hasSubordinates() {
            return "(hasSubordinates=TRUE)";
        }

        public static String createdLaterOrEqual(String generalizedTime) {
            return "(createTimestamp>=" + generalizedTime + ")";
        }

        /*
         * general
         */
        public static String anyEntry() {
            return "(objectClass=*)";
        }


        public static String presenceFilter(String attr) {
            return "(" + attr + "=*" + ")";
        }

        public static String equalityFilter(String attr, String value) {
            return "(" + attr + "=" + value + ")";
        }

        public static String greaterOrEqualFilter(String attr, String value) {
            return "(" + attr + ">=" + value + ")";
        }

        public static String lessOrEqualFilter(String attr, String value) {
            return "(" + attr + "<=" + value + ")";
        }

        public static String startsWithFilter(String attr,
                String value) {
            return "(" + attr + "=" + value + "*)";
        }

        public static String endsWithFilter(String attr,
                String value) {
            return "(" + attr + "=*" + value + ")";
        }

        public static String substringFilter(String attr,
                String value) {
            return "(" + attr + "=*" + value + "*)";
        }

        public static String andWith(String filter, String otherFilter) {
            return "(&" + filter + otherFilter + ")";
        }

        public static String negate(String filter) {
            return "(!" + filter + ")";
        }

        /*
         * Mail target (accounts and groups)
         */
        public static String addrsExist(String[] addrs) {
            StringBuilder buf = new StringBuilder();
            buf.append("(|");
            for (int i=0; i < addrs.length; i++) {
                buf.append(String.format("(%s=%s)", Provisioning.A_zimbraMailDeliveryAddress, addrs[i]));
                buf.append(String.format("(%s=%s)", Provisioning.A_zimbraMailAlias, addrs[i]));
            }
            buf.append(")");

            return buf.toString();
        }

        /*
         * account
         */
        public static String allAccounts() {
            return FILTER_ACCOUNT_OBJECTCLASS;
        }

        public static String allAccountsOnly() {
            return FILTER_ACCOUNT_ONLY_OBJECTCLASS;
        }

        public static String allAdminAccounts() {
            return "(&" + FILTER_ACCOUNT_OBJECTCLASS +
                          "(|(zimbraIsAdminAccount=TRUE)(zimbraIsDelegatedAdminAccount=TRUE)(zimbraIsDomainAdminAccount=TRUE))" +
                   ")";
        }

        public static String allNonSystemAccounts() {
            StringBuilder buf = new StringBuilder();
            buf.append("(&");
            buf.append("(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))");
            buf.append("(!(zimbraIsSystemResource=TRUE))");
            buf.append(")");

            return buf.toString();
        }

        public static String accountByForeignPrincipal(String foreignPrincipal) {
            return "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
        }

        public static String accountById(String id) {
            return "(&(zimbraId=" + id + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
        }

        public static String accountByName(String name) {
            return "(&(|(zimbraMailDeliveryAddress=" + name + ")(zimbraMailAlias=" + name + "))" + FILTER_ACCOUNT_OBJECTCLASS + ")";
        }

        public static String accountByMemberOf(String dynGroupId) {
            return "(&(zimbraMemberOf=" + dynGroupId + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
        }

        public static String adminAccountByRDN(String namingRdnAttr, String name) {
            return "(&(" + namingRdnAttr + "=" + name + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
        }

        public static String accountsHomedOnServer(String serverServiceHostname) {
            return "(&" + FILTER_ACCOUNT_OBJECTCLASS + homedOnServer(serverServiceHostname) + ")";
        }

        public static String accountsHomedOnServerAccountsOnly(String serverServiceHostname) {
            return "(&" + FILTER_ACCOUNT_ONLY_OBJECTCLASS + homedOnServer(serverServiceHostname) + ")";
        }

        public static String externalAccountsHomedOnServer(String serverServiceHostname) {
            return "(&" + FILTER_ACCOUNT_ONLY_OBJECTCLASS + externalAccount() + homedOnServer(serverServiceHostname) + ")";
        }

        public static String externalAccount() {
            return "(" + Provisioning.A_zimbraIsExternalVirtualAccount + "=TRUE)";
        }

        public static String accountsByExternalGrant(String granteeEmail) {
            return String.format("(&%s(zimbraSharedItem=granteeId:%s*))", FILTER_ACCOUNT_OBJECTCLASS, granteeEmail);
        }

        public static String accountsByGrants(List<String> granteeIds,
                boolean includePublicShares, boolean includeAllAuthedShares) {
            StringBuilder searchQuery = new StringBuilder().append("(&" + FILTER_ACCOUNT_OBJECTCLASS + "(|");
            for (String id : granteeIds) {
                searchQuery.append(String.format("(zimbraSharedItem=granteeId:%s*)", id));
            }
            if (includePublicShares) {
                searchQuery.append("(zimbraSharedItem=*granteeType:pub*)");
            }
            if (includeAllAuthedShares) {
                searchQuery.append("(zimbraSharedItem=*granteeType:all*)");
            }
            searchQuery.append("))");

            return searchQuery.toString();
        }

        public static String CMBSearchAccountsOnly() {
            return "(&" + FILTER_ACCOUNT_ONLY_OBJECTCLASS +
                          "(|(!(" + Provisioning.A_zimbraExcludeFromCMBSearch + "=*))(" +
                                    Provisioning.A_zimbraExcludeFromCMBSearch + "=FALSE))" +
                   ")";
        }

        public static String CMBSearchAccountsOnlyWithArchive() {
            return "(&" + FILTER_ACCOUNT_ONLY_OBJECTCLASS +
                          "(&" +
                          "(" + Provisioning.A_zimbraArchiveAccount + "=*)" +
                          "(|(!(" + Provisioning.A_zimbraExcludeFromCMBSearch + "=*))(" +
                          Provisioning.A_zimbraExcludeFromCMBSearch + "=FALSE))" +
                          ")" +
                   ")";
        }

        public static String CMBSearchNonSystemResourceAccountsOnly() {
            return "(&" + FILTER_ACCOUNT_ONLY_OBJECTCLASS +
                         "(&" +
                         "(!(" + Provisioning.A_zimbraIsSystemResource + "=*))" +
                         "(|(!(" + Provisioning.A_zimbraExcludeFromCMBSearch + "=*))(" +
                         Provisioning.A_zimbraExcludeFromCMBSearch + "=FALSE))" +
                         ")" +
                   ")";
        }

        public static String homedOnServer(String serverServiceHostname) {
            return "(" + Provisioning.A_zimbraMailHost + "=" + serverServiceHostname + ")";
        }

        public static String accountsOnServerOnCosHasSubordinates(String serverServiceHostname, String cosId) {
            return "(&" + LegacyLdapFilter.allAccounts() +
            LegacyLdapFilter.homedOnServer(serverServiceHostname) +
            LegacyLdapFilter.hasSubordinates() +
            "(|(!(" + Provisioning.A_zimbraCOSId + "=*))" + "(" + Provisioning.A_zimbraCOSId + "=" + cosId + ")))";
        }

        /*
         * alias
         */
        public static String allAliases() {
            return FILTER_ALIAS_OBJECTCLASS;
        }

        /*
         * calendar resource
         */
        public static String allCalendarResources() {
            return "(objectClass=zimbraCalendarResource)";
        }

        public static String calendarResourceByForeignPrincipal(String foreignPrincipal) {
            return "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
        }

        public static String calendarResourceById(String id) {
            return "(&(zimbraId=" + id + ")" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
        }

        public static String calendarResourceByName(String name) {
            return "(&(|(zimbraMailDeliveryAddress=" + name + ")(zimbraMailAlias=" + name + "))" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
        }

        public static String calendarResourcesHomedOnServer(String serverServiceHostname) {
            return "(&" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + homedOnServer(serverServiceHostname) + ")";
        }

        /*
         * cos
         */
        public static String allCoses() {
            return "(objectClass=zimbraCOS)";
        }

        public static String cosById(String id) {
            return "(&(zimbraId=" + id + ")(objectClass=zimbraCOS))";
        }

        public static String cosesByMailHostPool(String server) {
            return "(&(objectClass=zimbraCOS)(zimbraMailHostPool=" + server + "))";
        }

        /*
         * data source
         */
        public static String allDataSources() {
            return "(objectClass=zimbraDataSource)";
        }

        public static String dataSourceById(String id) {
            return "(&(objectClass=zimbraDataSource)(zimbraDataSourceId=" + id + "))";
        }

        public static String dataSourceByName(String name) {
            return "(&(objectClass=zimbraDataSource)(zimbraDataSourceName=" + name + "))";
        }

        /*
         * distribution list
         */
        public static String allDistributionLists() {
            return "(objectClass=zimbraDistributionList)";
        }

        public static String distributionListById(String id) {
            return "(&(zimbraId=" + id + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
        }

        public static String distributionListByName(String name) {
            return "(&(zimbraMailAlias=" + name + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
        }

        public static String distributionListsByMemberAddrs(String[] memberAddrs) {
            StringBuilder sb = new StringBuilder();
            if (memberAddrs.length > 1) {
                sb.append("(|");
            }
            for (int i=0; i < memberAddrs.length; i++) {
                sb.append(String.format("(%s=%s)", Provisioning.A_zimbraMailForwardingAddress, memberAddrs[i]));
            }
            if (memberAddrs.length > 1) {
                sb.append(")");
            }

            return "(&" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + sb.toString() + ")";
        }

        /*
         * dynamic group
         */
        public static String dynamicGroupById(String id) {
            return "(&(zimbraId=" + id + ")" + FILTER_DYNAMIC_GROUP_OBJECTCLASS + ")";
        }

        public static String dynamicGroupByName(String name) {
            return "(&(zimbraMailAlias=" + name + ")" + FILTER_DYNAMIC_GROUP_OBJECTCLASS + ")";
        }


        /*
         * groups (distribution list or dynamic group)
         */
        public static String allGroups() {
            return FILTER_GROUP_OBJECTCLASS;
        }

        public static String groupById(String id) {
            return "(&(zimbraId=" + id + ")" + FILTER_GROUP_OBJECTCLASS + ")";
        }

        public static String groupByName(String name) {
            return "(&(zimbraMailAlias=" + name + ")" + FILTER_GROUP_OBJECTCLASS + ")";
        }


        /*
         * domain
         */
        public static String allDomains() {
            return "(objectClass=zimbraDomain)";
        }

        public static String domainById(String id) {
            return "(&(zimbraId=" + id + ")(objectClass=zimbraDomain))";
        }

        public static String domainByName(String name) {
            return "(&(zimbraDomainName=" + name + ")(objectClass=zimbraDomain))";
        }

        public static String domainByKrb5Realm(String krb5Realm) {
            return "(&(zimbraAuthKerberos5Realm=" + krb5Realm + ")(objectClass=zimbraDomain))";
        }

        public static String domainByVirtualHostame(String virtualHostname) {
            return "(&(zimbraVirtualHostname=" + virtualHostname + ")(objectClass=zimbraDomain))";
        }

        public static String domainByForeignName(String foreignName) {
            return "(&(zimbraForeignName=" + foreignName + ")(objectClass=zimbraDomain))";
        }

        public static String domainLabel() {
            return "(objectClass=dcObject)";
        }

        public static String domainLockedForEagerAutoProvision() {
            return "(!(zimbraAutoProvLock=*))";
        }


        /*
         * global config
         */
        public static String globalConfig() {
            return "(cn=config)";
        }


        /*
         * identity
         */
        public static String allIdentities() {
            return "(objectClass=zimbraIdentity)";
        }

        public static String identityByName(String name) {
            return "(&(objectClass=zimbraIdentity)(zimbraPrefIdentityName=" + name + "))";
        }

        /*
         * mime enrty
         */
        public static String allMimeEntries() {
            return "(objectClass=zimbraMimeEntry)";
        }

        public static String mimeEntryByMimeType(String mimeType) {
            return "(zimbraMimeType=" + mimeType + ")";
        }

        /*
         * server
         */
        public static String allServers() {
            return "(objectClass=zimbraServer)";
        }

        public static String serverById(String id) {
            return "(&(zimbraId=" + id + ")(objectClass=zimbraServer))";
        }

        public static String serverByService(String service) {
            return "(&(objectClass=zimbraServer)(zimbraServiceEnabled=" + service + "))";
        }

        /*
         * share locator
         */
        public static String shareLocatorById(String id) {
            return "(&(cn=" + id + ")(objectClass=zimbraShareLocator))";
        }

        /*
         * signature
         */
        public static String allSignatures() {
            return "(objectClass=zimbraSignature)";
        }

        public static String signatureById(String id) {
            return "(&(objectClass=zimbraSignature)(zimbraSignatureId=" + id +"))";
        }


        /*
         * XMPPComponent
         */
        public static String allXMPPComponents() {
            return "(objectClass=zimbraXMPPComponent)";
        }

        public static String imComponentById(String id) {
            return "(&(objectClass=zimbraXMPPComponent)(zimbraXMPPComponentId=" + id + "))";
        }

        public static String xmppComponentById(String id) {
            return "(&(zimbraId=" + id + ")(objectClass=zimbraXMPPComponent))";
        }


        /*
         * zimlet
         */
        public static String allZimlets() {
            return "(objectClass=zimbraZimletEntry)";
        }


        /*
         * AD
         */
        public static String memberOf(String dnOfGroup) {
            return "(memberOf=" + dnOfGroup + ")";
        }


        /*
         * Velodrome
         */
        private static String velodromePrimaryEmailOnDomainFilter(String domainName) {
            return "(zimbraMailDeliveryAddress=*@" + domainName + ")";
        }

        public static String velodromeAllAccountsByDomain(String domainName) {
            return "(&" + FILTER_ACCOUNT_OBJECTCLASS + velodromePrimaryEmailOnDomainFilter(domainName) + ")";
        }

        public static String velodromeAllAccountsOnlyByDomain(String domainName) {
            return "(&" + FILTER_ACCOUNT_ONLY_OBJECTCLASS + velodromePrimaryEmailOnDomainFilter(domainName) + ")";
        }

        public static String velodromeAllCalendarResourcesByDomain(String domainName) {
            return "(&" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + velodromePrimaryEmailOnDomainFilter(domainName) + ")";
        }

        public static String velodromeAllAccountsByDomainAndServer(String domainName, String serverServiceHostname) {
            return "(&" + FILTER_ACCOUNT_OBJECTCLASS +
                          homedOnServer(serverServiceHostname) +
                          velodromePrimaryEmailOnDomainFilter(domainName) + ")";
        }

        public static String velodromeAllAccountsOnlyByDomainAndServer(String domainName, String serverServiceHostname) {
            return "(&" + FILTER_ACCOUNT_ONLY_OBJECTCLASS +
                          homedOnServer(serverServiceHostname) +
                          velodromePrimaryEmailOnDomainFilter(domainName) + ")";
        }

        public static String velodromeAllCalendarResourcesByDomainAndServer(String domainName, String serverServiceHostname) {
            return "(&" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS +
                          homedOnServer(serverServiceHostname) +
                          velodromePrimaryEmailOnDomainFilter(domainName) + ")";
        }

        public static String dnSubtreeMatch(String... dns) {
            StringBuffer sb = new StringBuffer();
            sb.append("(|");
            for (String dn : dns) {
                sb.append(String.format(LdapConstants.DN_SUBTREE_MATCH_FILTER_TEMPLATE, dn));
            }
            sb.append(")");

            return sb.toString();
        }


        private static void printFilter(String doc, String usage, String base, String filter) {
            System.out.println();
            System.out.println(doc);
            System.out.println("usage:  " + usage);
            System.out.println("base:   " + base);
            System.out.println("filter: " + filter);
        }

        /*
        private static void printFilters(LdapProvisioning prov) throws ServiceException {
            // account
            printFilter("all non system accounts",
                        "create account(count account for license)",
                        prov.getDIT().domainToAccountSearchDN("example.com"),
                        LdapFilter.allNonSystemAccounts());

            printFilter("account by foreign principal",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.accountByForeignPrincipal("{foreign principal}"));

            printFilter("account by id",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.accountById("{account id}"));

            printFilter("account by email",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.accountByName("{account email}"));

            printFilter("admin account by RDN",
                        "admin account access",
                        prov.getDIT().adminBaseDN(),
                        LdapFilter.adminAccountByRDN(prov.getDIT().accountNamingRdnAttr(), "{admin name}"));

            // calendar resource
            printFilter("all calendar resources",
                    "admin console, zmprov",
                    prov.getDIT().mailBranchBaseDN(),
                    LdapFilter.allCalendarResources());

            printFilter("calendar resource by foreign principal",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.calendarResourceByForeignPrincipal("{foreign principal}"));

            printFilter("calendar resource by id",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.calendarResourceById("{calendar resource id}"));

            printFilter("calendar resource by name",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.calendarResourceByName("{calendar resource name}"));

            // cos
            printFilter("all coses",
                        "admin console, zmprov",
                        prov.getDIT().cosBaseDN(),
                        LdapFilter.allCoses());

            printFilter("cos by id",
                        "general",
                        prov.getDIT().cosBaseDN(),
                        LdapFilter.cosById("{cos id}"));

            printFilter("coses have certain server in its server pool",
                        "delete server (to remove the server from all coses)",
                        prov.getDIT().cosBaseDN(),
                        LdapFilter.cosesByMailHostPool("{server name}"));

            // data source
            printFilter("all data sources",
                        "general",
                        "account DN",
                        LdapFilter.allDataSources());

            printFilter("data source by id",
                        "general",
                        "account DN",
                        LdapFilter.dataSourceById("{data source id}"));

            printFilter("data source by name",
                        "general",
                        "account DN",
                        LdapFilter.dataSourceByName("{data source name}"));

            // distribution list
            printFilter("all distribution lists",
                    "admin console, zmprov",
                    prov.getDIT().mailBranchBaseDN(),
                    LdapFilter.allDistributionLists());

            printFilter("distributuion list by id",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.distributionListById("{dist list id}"));

            printFilter("distributuion list by name",
                        "general",
                        prov.getDIT().mailBranchBaseDN(),
                        LdapFilter.distributionListByName("{dist list name}"));

            // domain
            printFilter("all domains",
                        "admin console, zmprov",
                        prov.getDIT().domainBaseDN(),
                        LdapFilter.allDomains());

            printFilter("domain by id",
                        "general",
                        prov.getDIT().domainBaseDN(),
                        LdapFilter.domainById("{domain id}"));

            printFilter("domain by krb5 realm",
                        "general",
                        prov.getDIT().domainBaseDN(),
                        LdapFilter.domainByKrb5Realm("{krb5 realm}"));

            printFilter("domain by name",
                        "general",
                        prov.getDIT().domainBaseDN(),
                        LdapFilter.domainByName("{domain name}"));

            printFilter("domain by virtual hostname",
                        "general",
                        prov.getDIT().domainBaseDN(),
                        LdapFilter.domainByVirtualHostame("{virtual hostname}"));

            printFilter("test if a domain label(dot separated segments) exists",
                        "create domain",
                        "{parent domain DN}",
                        LdapFilter.domainLabel());

            // identity
            printFilter("all identities",
                        "general",
                        "account DN",
                        LdapFilter.allIdentities());

            printFilter("identity by name",
                        "general",
                        "account DN",
                        LdapFilter.identityByName("{identity name}"));


            // mime
            printFilter("all mime entries",
                        "general",
                        prov.getDIT().mimeBaseDN(),
                        LdapFilter.allMimeEntries());

            printFilter("mime entry by mime type",
                        "general",
                        prov.getDIT().mimeBaseDN(),
                        LdapFilter.mimeEntryByMimeType("{mime type}"));

            // signature
            printFilter("all signatures",
                        "general",
                        "account DN",
                        LdapFilter.allSignatures());

            printFilter("signature by id",
                        "general",
                        "account DN",
                        LdapFilter.signatureById("{signature id}"));

            // server
            printFilter("all servers",
                        "admin console, zmprov",
                        prov.getDIT().serverBaseDN(),
                        LdapFilter.allServers());

            printFilter("server by id",
                        "general",
                        prov.getDIT().serverBaseDN(),
                        LdapFilter.serverById("{server id}"));

            printFilter("server by service",
                        "general",
                        prov.getDIT().serverBaseDN(),
                        LdapFilter.serverByService("{service}"));

            // zimlet
            printFilter("all zimlets",
                        "general",
                        prov.getDIT().zimletBaseDN(),
                        LdapFilter.allZimlets());

        }

        public static void main(String[] args) throws Exception {
            LdapProvisioning prov = (LdapProvisioning)Provisioning.getInstance();
            printFilters(prov);
        }
        */
    }
    
    private String genUUID() {
        return LdapUtil.generateUUID();
    }
    
    private void verifyStatString(FilterId filterId, ZLdapFilter zLdapFilter) throws Exception {
        assertEquals(filterId.getStatString(), zLdapFilter.getStatString());
    }
    
    private void verify(FilterId filterId, String expected, ZLdapFilter actual) 
    throws Exception {
        String filter = actual.toFilterString();
        assertEquals(expected, filter);
        verifyStatString(filterId, actual);
    }
    
    private void verify(FilterId filterId, ZLdapFilter expected, ZLdapFilter actual) 
    throws Exception {
        assertEquals(expected.toFilterString(), actual.toFilterString());
        verifyStatString(filterId, actual);
    }
    
    @Test
    public void hasSubordinates() throws Exception {
        String filter = LegacyLdapFilter.hasSubordinates();
        ZLdapFilter zLdapFilter = filterDactory.hasSubordinates();
        verify(FilterId.HAS_SUBORDINATES, filter, zLdapFilter);
    }
    
    @Test
    public void createdLaterOrEqual() throws Exception {
        String GENERALIZED_TIME = "20111005190522Z";
        
        String filter = LegacyLdapFilter.createdLaterOrEqual(GENERALIZED_TIME);
        ZLdapFilter zLdapFilter = filterDactory.createdLaterOrEqual(GENERALIZED_TIME);
        verify(FilterId.CREATED_LATEROREQUAL, filter, zLdapFilter);
    }
    
    @Test
    public void anyEntry() throws Exception {
        String filter = LegacyLdapFilter.anyEntry();
        ZLdapFilter zLdapFilter = filterDactory.anyEntry();
        verify(FilterId.ANY_ENTRY, filter, zLdapFilter);
    }
    
    @Test
    public void fromFilterString() throws Exception {
        String FILTER_STR = "(blah=123)";
        ZLdapFilter zLdapFilter = filterDactory.fromFilterString(
                FilterId.AUTO_PROVISION_SEARCH, FILTER_STR);
        verify(FilterId.AUTO_PROVISION_SEARCH, FILTER_STR, zLdapFilter);
    }
    
    @Test
    public void andWith() throws Exception {
        String FILTER1 = "(foo=1)";
        String FILTER2 = "(bar=2)";
        
        String filter = LegacyLdapFilter.andWith(FILTER1, FILTER2);
        ZLdapFilter zLdapFilter = filterDactory.andWith(
                /* use ADMIN_SEARCH instead of UNITTEST to distinguish with filter id for FILER2,
                 * filter id of the first filter should be used in the result filter
                 */
                filterDactory.fromFilterString(FilterId.ADMIN_SEARCH, FILTER1), 
                filterDactory.fromFilterString(FilterId.UNITTEST, FILTER2));
        verify(FilterId.ADMIN_SEARCH, filter, zLdapFilter);
    }
    
    @Test
    public void negate() throws Exception {
        String FILTER = "(foo=bar)";
        
        String filter = LegacyLdapFilter.negate(FILTER);
        ZLdapFilter zLdapFilter = filterDactory.negate(
                filterDactory.fromFilterString(FilterId.UNITTEST, FILTER));
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void presenceFilter() throws Exception {
        String ATTR = "foo";
        
        String filter = filterDactory.presenceFilter(ATTR);
        assertEquals("(foo=*)", filter);
    }
    
    @Test
    public void equalityFilter() throws Exception {
        String ATTR = "foo";
        String VALUE_RAW = "bar*()\\";
        String VALUE_ESCAPED = "bar\\2a\\28\\29\\5c";
        
        String filterFromRaw = filterDactory.equalityFilter(ATTR, VALUE_RAW, true);
        String filterFromEscaped = filterDactory.equalityFilter(ATTR, VALUE_ESCAPED, false);
        assertEquals(filterFromRaw, filterFromEscaped);
    }
        
    @Test
    public void greaterOrEqualFilter() throws Exception {
        String ATTR = "foo";
        String VALUE_RAW = "bar*()\\";
        String VALUE_ESCAPED = "bar\\2a\\28\\29\\5c";
        
        String filterFromRaw = filterDactory.greaterOrEqualFilter(ATTR, VALUE_RAW, true);
        String filterFromEscaped = filterDactory.greaterOrEqualFilter(ATTR, VALUE_ESCAPED, false);
        assertEquals(filterFromRaw, filterFromEscaped);
    }
    
    @Test
    public void lessOrEqualFilter() throws Exception {
        String ATTR = "foo";
        String VALUE_RAW = "bar*()\\";
        String VALUE_ESCAPED = "bar\\2a\\28\\29\\5c";
        
        String filterFromRaw = filterDactory.lessOrEqualFilter(ATTR, VALUE_RAW, true);
        String filterFromEscaped = filterDactory.lessOrEqualFilter(ATTR, VALUE_ESCAPED, false);
        assertEquals(filterFromRaw, filterFromEscaped);
    }
    
    @Test
    public void startsWithFilter() throws Exception {
        String ATTR = "foo";
        String VALUE_RAW = "bar*()\\";
        String VALUE_ESCAPED = "bar\\2a\\28\\29\\5c";
        
        String filterFromRaw = filterDactory.startsWithFilter(ATTR, VALUE_RAW, true);
        String filterFromEscaped = filterDactory.startsWithFilter(ATTR, VALUE_ESCAPED, false);
        assertEquals(filterFromRaw, filterFromEscaped);
    }
    
    @Test
    public void endsWithFilter() throws Exception {
        String ATTR = "foo";
        String VALUE_RAW = "bar*()\\";
        String VALUE_ESCAPED = "bar\\2a\\28\\29\\5c";
        
        String filterFromRaw = filterDactory.endsWithFilter(ATTR, VALUE_RAW, true);
        String filterFromEscaped = filterDactory.endsWithFilter(ATTR, VALUE_ESCAPED, false);
        assertEquals(filterFromRaw, filterFromEscaped);
    }
    
    @Test
    public void substringFilter() throws Exception {
        String ATTR = "foo";
        String VALUE_RAW = "bar*()\\";
        String VALUE_ESCAPED = "bar\\2a\\28\\29\\5c";
        
        String filterFromRaw = filterDactory.substringFilter(ATTR, VALUE_RAW, true);
        String filterFromEscaped = filterDactory.substringFilter(ATTR, VALUE_ESCAPED, false);
        assertEquals(filterFromRaw, filterFromEscaped);
    }
  
    @Test
    public void addrsExist() throws Exception {
        String[] ADDRS = new String[]{"addr1@test.com", "addr2@test.com", "addr2@test.com"};
        
        String filter = LegacyLdapFilter.addrsExist(ADDRS);
        ZLdapFilter zLdapFilter = filterDactory.addrsExist(ADDRS);
        verify(FilterId.ADDRS_EXIST, filter, zLdapFilter);
    }
    
    @Test
    public void allAccounts() throws Exception {
        String filter = LegacyLdapFilter.allAccounts();
        ZLdapFilter zLdapFilter = filterDactory.allAccounts();
        verify(FilterId.ALL_ACCOUNTS, filter, zLdapFilter);
    }
    
    @Test
    public void allAccountsOnly() throws Exception {
        String filter = LegacyLdapFilter.allAccountsOnly();
        ZLdapFilter zLdapFilter = filterDactory.allAccountsOnly();
        verify(FilterId.ALL_ACCOUNTS_ONLY, filter, zLdapFilter);
    }
    
    @Test
    public void allAdminAccounts() throws Exception {
        String filter = LegacyLdapFilter.allAdminAccounts();
        ZLdapFilter zLdapFilter = filterDactory.allAdminAccounts();
        verify(FilterId.ALL_ADMIN_ACCOUNTS, filter, zLdapFilter);
    }
    
    @Test
    public void allNonSystemAccounts() throws Exception {
        String filter = LegacyLdapFilter.allNonSystemAccounts();
        // (&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource))(!(zimbraIsSystemResource=TRUE)))
        
        ZLdapFilter zLdapFilter = filterDactory.allNonSystemAccounts();
        String zFilter = zLdapFilter.toFilterString();
        // (&(&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource)))(!(zimbraIsSystemResource=TRUE)))
        
        // assertEquals(filter, zFilter);  the diff is OK
        verifyStatString(FilterId.ALL_NON_SYSTEM_ACCOUNTS, zLdapFilter);
    }
    
    @Test
    public void accountByForeignPrincipal() throws Exception {
        String FOREIFN_PRINCIPAL = getTestName();
        
        String filter = LegacyLdapFilter.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        ZLdapFilter zLdapFilter = filterDactory.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        verify(FilterId.ACCOUNT_BY_FOREIGN_PRINCIPAL, filter, zLdapFilter);
    }
    
    @Test
    public void accountById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.accountById(ID);
        ZLdapFilter zLdapFilter = filterDactory.accountById(ID);
        verify(FilterId.ACCOUNT_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void accountByMemberOf() throws Exception {
        String MEMBEROF = getTestName();
            
        String filter = LegacyLdapFilter.accountByMemberOf(MEMBEROF);
        ZLdapFilter zLdapFilter = filterDactory.accountByMemberOf(MEMBEROF);
        verify(FilterId.ACCOUNT_BY_MEMBEROF, filter, zLdapFilter);
    }
    
    @Test
    public void accountByName() throws Exception {
        String NAME = getTestName();
            
        String filter = LegacyLdapFilter.accountByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.accountByName(NAME);
        verify(FilterId.ACCOUNT_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void adminAccountByRDN() throws Exception {
        String NAMING_RDN_ATTR = "uid";
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        ZLdapFilter zLdapFilter = filterDactory.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        verify(FilterId.ADMIN_ACCOUNT_BY_RDN, filter, zLdapFilter);
    }
    
    @Test
    public void accountsHomedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.accountsHomedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.accountsHomedOnServer(SERVER.getServiceHostname());
        verify(FilterId.ACCOUNTS_HOMED_ON_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void accountsHomedOnServerAccountOnly() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.accountsHomedOnServerAccountsOnly(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.accountsHomedOnServerAccountsOnly(SERVER.getServiceHostname());
        verify(FilterId.ACCOUNTS_HOMED_ON_SERVER_ACCOUNTS_ONLY, filter, zLdapFilter);
    }
    
    @Test
    public void homedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.homedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.homedOnServer(SERVER.getServiceHostname());
        verify(FilterId.HOMED_ON_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void accountsOnServerAndCosHasSubordinates() throws Exception {
        Server SERVER = prov.getLocalServer();
        String COS_ID = genUUID();
        
        String filter = LegacyLdapFilter.accountsOnServerOnCosHasSubordinates(SERVER.getServiceHostname(), COS_ID);
        ZLdapFilter zLdapFilter = filterDactory.accountsOnServerAndCosHasSubordinates(SERVER.getServiceHostname(), COS_ID);
        verify(FilterId.ACCOUNTS_ON_SERVER_AND_COS_HAS_SUBORDINATES, filter, zLdapFilter);
    }
    
    @Test
    public void accountsByExternalGrant() throws Exception {
        String GRANTEE_EMAIL = "accountsSharedWith@test.com";
        
        String legacyFilter = String.format("(&(objectClass=zimbraAccount)(zimbraSharedItem=granteeId:%s*))", GRANTEE_EMAIL);
        
        String filter = LegacyLdapFilter.accountsByExternalGrant(GRANTEE_EMAIL);
        ZLdapFilter zLdapFilter = filterDactory.accountsByExternalGrant(GRANTEE_EMAIL);
        
        assertEquals(legacyFilter, filter);
        verify(FilterId.ACCOUNTS_BY_EXTERNAL_GRANT, filter, zLdapFilter);
    }
    
    @Test
    public void accountsByGrants() throws Exception {
        List<String> GRANTEE_IDS = Lists.newArrayList("GRANTEE-ID-1", "GRANTEE-ID-2", "...");
        boolean includePublicShares = true;
        boolean includeAllAuthedShares = true;
        
        // legacy code
        StringBuilder searchQuery = new StringBuilder().append("(&(objectClass=zimbraAccount)(|");
        for (String id : GRANTEE_IDS) {
            searchQuery.append(String.format("(zimbraSharedItem=granteeId:%s*)", id));
        }
        if (includePublicShares) {
            searchQuery.append("(zimbraSharedItem=*granteeType:pub*)");
        }
        if (includeAllAuthedShares) {
            searchQuery.append("(zimbraSharedItem=*granteeType:all*)");
        }
        searchQuery.append("))");
        
        String legacyFilter = searchQuery.toString();
        
        String filter = LegacyLdapFilter.accountsByGrants(GRANTEE_IDS, includePublicShares, includeAllAuthedShares);
        ZLdapFilter zLdapFilter = filterDactory.accountsByGrants(GRANTEE_IDS, includePublicShares, includeAllAuthedShares);
        
        assertEquals(legacyFilter, filter);
        verify(FilterId.ACCOUNTS_BY_GRANTS, filter, zLdapFilter);
    }
    
    @Test
    public void CMBSearchAccountsOnly() throws Exception {
        
        /*
        orig filter before refactoring
        String legacyFilter = 
            "(&(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))";
        */
        
        // moved objectClass to the front
        String legacyFilter = 
            "(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))";

        String filter = LegacyLdapFilter.CMBSearchAccountsOnly();
        
        
        ZLdapFilter zLdapFilter = filterDactory.CMBSearchAccountsOnly();

        assertEquals(legacyFilter, filter);
        verify(FilterId.CMB_SEARCH_ACCOUNTS_ONLY, filter, zLdapFilter);
    }
    
    @Test
    public void CMBSearchAccountsOnlyWithArchive() throws Exception {
        /*
        orig filter before refactoring
        String legacyFilter = 
            "(&(&(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))";
        */
        
        // moved objectClass to the front
        String legacyFilter = 
            "(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(&(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE))))";
        
        String filter = LegacyLdapFilter.CMBSearchAccountsOnlyWithArchive();
        assertEquals(legacyFilter, filter);
        
        ZLdapFilter zLdapFilter = filterDactory.CMBSearchAccountsOnlyWithArchive();
        String zFilter = zLdapFilter.toFilterString();
        
        // This assertion fails because we optimized it in the new code
        // it is now:
        // (&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))
        // System.out.println(zLdapFilter.toFilterString());
        // assertEquals(filter, zFilter);
        // assertEquals(legacyFilter, zFilter);
        verifyStatString(FilterId.CMB_SEARCH_ACCOUNTS_ONLY_WITH_ARCHIVE, zLdapFilter);
    }
    
    @Test
    public void CMBSearchNonSystemResourceAccountsOnly() throws Exception {
        /*
        orig filter before refactoring
        String legacyFilter = 
            "(&(&(!(zimbraIsSystemResource=*))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))";
         */
        
        String legacyFilter = 
            "(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(&(!(zimbraIsSystemResource=*))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE))))";

        String filter = LegacyLdapFilter.CMBSearchNonSystemResourceAccountsOnly();
        assertEquals(legacyFilter, filter);
        
        ZLdapFilter zLdapFilter = filterDactory.CMBSearchNonSystemResourceAccountsOnly();
        String zFilter = zLdapFilter.toFilterString();
        
        // This assertion fails because we optimized it in the new code
        // it is now:
        // (&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(!(zimbraIsSystemResource=TRUE))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))
        // System.out.println(zLdapFilter.toFilterString());
        // assertEquals(filter, zFilter);
        // assertEquals(legacyFilter, zFilter);
        verifyStatString(FilterId.CMB_SEARCH_NON_SYSTEM_RESOURCE_ACCOUNTS_ONLY, zLdapFilter);
    }
    
    @Test
    public void allAliases() throws Exception {
        String filter = LegacyLdapFilter.allAliases();
        ZLdapFilter zLdapFilter = filterDactory.allAliases();
        verify(FilterId.ALL_ALIASES, filter, zLdapFilter);
    }
    
    @Test
    public void allCalendarResources() throws Exception {
        String filter = LegacyLdapFilter.allCalendarResources();
        ZLdapFilter zLdapFilter = filterDactory.allCalendarResources();
        verify(FilterId.ALL_CALENDAR_RESOURCES, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourceByForeignPrincipal() throws Exception {
        String FOREIGN_PRINCIPAL = getTestName();
        
        String filter = LegacyLdapFilter.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL);
        verify(FilterId.CALENDAR_RESOURCE_BY_FOREIGN_PRINCIPAL, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.calendarResourceById(ID);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceById(ID);
        verify(FilterId.CALENDAR_RESOURCE_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourceByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.calendarResourceByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceByName(NAME);
        verify(FilterId.CALENDAR_RESOURCE_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourcesHomedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.calendarResourcesHomedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.calendarResourcesHomedOnServer(SERVER.getServiceHostname());
        verify(FilterId.CALENDAR_RESOURCES_HOMED_ON_SERVER, filter, zLdapFilter);
    }

    
    @Test
    public void allCoses() throws Exception {
        String filter = LegacyLdapFilter.allCoses();
        ZLdapFilter zLdapFilter = filterDactory.allCoses();
        verify(FilterId.ALL_COSES, filter, zLdapFilter);
    }
    
    @Test
    public void cosById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.cosById(ID);
        ZLdapFilter zLdapFilter = filterDactory.cosById(ID);
        verify(FilterId.COS_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void cosesByMailHostPool() throws Exception {
        String SERVER_ID = genUUID();
        
        String filter = LegacyLdapFilter.cosesByMailHostPool(SERVER_ID);
        ZLdapFilter zLdapFilter = filterDactory.cosesByMailHostPool(SERVER_ID);
        verify(FilterId.COSES_BY_MAILHOST_POOL, filter, zLdapFilter);
    }
    
    @Test
    public void allDataSources() throws Exception {
        String filter = LegacyLdapFilter.allDataSources();
        ZLdapFilter zLdapFilter = filterDactory.allDataSources();
        verify(FilterId.ALL_DATA_SOURCES, filter, zLdapFilter);
    }
    
    @Test
    public void dataSourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.dataSourceById(ID);
        ZLdapFilter zLdapFilter = filterDactory.dataSourceById(ID);
        verify(FilterId.DATA_SOURCE_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void dataSourceByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.dataSourceByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.dataSourceByName(NAME);
        verify(FilterId.DATA_SOURCE_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void allDistributionLists() throws Exception {
        String filter = LegacyLdapFilter.allDistributionLists();
        ZLdapFilter zLdapFilter = filterDactory.allDistributionLists();
        verify(FilterId.ALL_DISTRIBUTION_LISTS, filter, zLdapFilter);
    }
    
    @Test
    public void distributionListById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.distributionListById(ID);
        ZLdapFilter zLdapFilter = filterDactory.distributionListById(ID);
        verify(FilterId.DISTRIBUTION_LIST_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void distributionListByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.distributionListByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.distributionListByName(NAME);
        verify(FilterId.DISTRIBUTION_LIST_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void distributionListsByMemberAddrs() throws Exception {
        String[] MEMBER_ADDRS = new String[]{"addr1@test.com", "addr2@test.com", "addr3@test.com"};
        
        String filter = LegacyLdapFilter.distributionListsByMemberAddrs(MEMBER_ADDRS);
        ZLdapFilter zLdapFilter = filterDactory.distributionListsByMemberAddrs(MEMBER_ADDRS);
        verify(FilterId.DISTRIBUTION_LISTS_BY_MEMBER_ADDRS, filter, zLdapFilter);
    }
    
    @Test
    public void dynamicGroupById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.dynamicGroupById(ID);
        ZLdapFilter zLdapFilter = filterDactory.dynamicGroupById(ID);
        verify(FilterId.DYNAMIC_GROUP_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void dynamicGroupByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.dynamicGroupByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.dynamicGroupByName(NAME);
        verify(FilterId.DYNAMIC_GROUP_BY_NAME, filter, zLdapFilter);
    }
    
    
    @Test
    public void groupById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.groupById(ID);
        ZLdapFilter zLdapFilter = filterDactory.groupById(ID);
        verify(FilterId.GROUP_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void groupByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.groupByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.groupByName(NAME);
        verify(FilterId.GROUP_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void allDomains() throws Exception {
        String filter = LegacyLdapFilter.allDomains();
        ZLdapFilter zLdapFilter = filterDactory.allDomains();
        verify(FilterId.ALL_DOMAINS, filter, zLdapFilter);
    }
    
    @Test
    public void domainById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.domainById(ID);
        ZLdapFilter zLdapFilter = filterDactory.domainById(ID);
        verify(FilterId.DOMAIN_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void domainByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.domainByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByName(NAME);
        verify(FilterId.DOMAIN_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void domainByKrb5Realm() throws Exception {
        String REALM = getTestName();
        
        String filter = LegacyLdapFilter.domainByKrb5Realm(REALM);
        ZLdapFilter zLdapFilter = filterDactory.domainByKrb5Realm(REALM);
        verify(FilterId.DOMAIN_BY_KRB5_REALM, filter, zLdapFilter);
    }
    
    @Test
    public void domainByVirtualHostame() throws Exception {
        String VIRTUAL_HOST_NAME = getTestName();
        
        String filter = LegacyLdapFilter.domainByVirtualHostame(VIRTUAL_HOST_NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByVirtualHostame(VIRTUAL_HOST_NAME);
        verify(FilterId.DOMAIN_BY_VIRTUAL_HOSTNAME, filter, zLdapFilter);
    }
    
    @Test
    public void domainByForeignName() throws Exception {
        String FOREIGN_NAME = getTestName();
        
        String filter = LegacyLdapFilter.domainByForeignName(FOREIGN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByForeignName(FOREIGN_NAME);
        verify(FilterId.DOMAIN_BY_FOREIGN_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void domainLabel() throws Exception {
        String filter = LegacyLdapFilter.domainLabel();
        ZLdapFilter zLdapFilter = filterDactory.domainLabel();
        verify(FilterId.DOMAIN_LABEL, filter, zLdapFilter);
    }
    
    @Test
    public void domainLockedForEagerAutoProvision() throws Exception {
        String filter = LegacyLdapFilter.domainLockedForEagerAutoProvision();
        ZLdapFilter zLdapFilter = filterDactory.domainLockedForEagerAutoProvision();
        verify(FilterId.DOMAIN_LOCKED_FOR_AUTO_PROVISION, filter, zLdapFilter);
    }
    
    @Test
    public void globalConfig() throws Exception {
        String filter = LegacyLdapFilter.globalConfig();
        ZLdapFilter zLdapFilter = filterDactory.globalConfig();
        verify(FilterId.GLOBAL_CONFIG, filter, zLdapFilter);
    }
    
    @Test
    public void allIdentities() throws Exception {
        String filter = LegacyLdapFilter.allIdentities();
        ZLdapFilter zLdapFilter = filterDactory.allIdentities();
        verify(FilterId.ALL_IDENTITIES, filter, zLdapFilter);
    }
    
    @Test
    public void identityByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.identityByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.identityByName(NAME);
        verify(FilterId.IDENTITY_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void allMimeEntries() throws Exception {
        String filter = LegacyLdapFilter.allMimeEntries();
        ZLdapFilter zLdapFilter = filterDactory.allMimeEntries();
        verify(FilterId.ALL_MIME_ENTRIES, filter, zLdapFilter);
    }
    
    @Test
    public void mimeEntryByMimeType() throws Exception {
        String MIME_TYPE = getTestName();
        
        String filter = LegacyLdapFilter.mimeEntryByMimeType(MIME_TYPE);
        ZLdapFilter zLdapFilter = filterDactory.mimeEntryByMimeType(MIME_TYPE);
        verify(FilterId.MIME_ENTRY_BY_MIME_TYPE, filter, zLdapFilter);
    }
    
    @Test
    public void allServers() throws Exception {
        String filter = LegacyLdapFilter.allServers();
        ZLdapFilter zLdapFilter = filterDactory.allServers();
        verify(FilterId.ALL_SERVERS, filter, zLdapFilter);
    }
    
    @Test
    public void serverById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.serverById(ID);
        ZLdapFilter zLdapFilter = filterDactory.serverById(ID);
        verify(FilterId.SERVER_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void serverByService() throws Exception {
        String SERVICE = getTestName();
        
        String filter = LegacyLdapFilter.serverByService(SERVICE);
        ZLdapFilter zLdapFilter = filterDactory.serverByService(SERVICE);
        verify(FilterId.SERVER_BY_SERVICE, filter, zLdapFilter);
    }
    
    @Test
    public void allSignatures() throws Exception {
        String filter = LegacyLdapFilter.allSignatures();
        ZLdapFilter zLdapFilter = filterDactory.allSignatures();
        verify(FilterId.ALL_SIGNATURES, filter, zLdapFilter);
    }
    
    @Test
    public void signatureById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.signatureById(ID);
        ZLdapFilter zLdapFilter = filterDactory.signatureById(ID);
        verify(FilterId.SIGNATURE_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void allXMPPComponents() throws Exception {
        String filter = LegacyLdapFilter.allXMPPComponents();
        ZLdapFilter zLdapFilter = filterDactory.allXMPPComponents();
        verify(FilterId.ALL_XMPP_COMPONENTS, filter, zLdapFilter);
    }
    
    @Test
    public void imComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.imComponentById(ID);
        ZLdapFilter zLdapFilter = filterDactory.imComponentById(ID);
        verify(FilterId.XMPP_COMPONENT_BY_ZIMBRA_XMPP_COMPONENT_ID, filter, zLdapFilter);
    }
    
    @Test
    public void xmppComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.xmppComponentById(ID);
        ZLdapFilter zLdapFilter = filterDactory.xmppComponentById(ID);
        verify(FilterId.XMPP_COMPONENT_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void allZimlets() throws Exception {
        String filter = LegacyLdapFilter.allZimlets();
        ZLdapFilter zLdapFilter = filterDactory.allZimlets();
        verify(FilterId.ALL_ZIMLETS, filter, zLdapFilter);
    }
    
    @Test
    @Bug(bug=64260)
    public void bug64260() throws Exception {
        String badStringFilter = "ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*(EMC)))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))";
        
        ZLdapFilter filter;
        
        boolean caughtException = false;
        
        try {
            filter = filterDactory.fromFilterString(FilterId.UNITTEST, badStringFilter);
        } catch (LdapException e) {
            // e.printStackTrace();
            if (LdapException.INVALID_SEARCH_FILTER.equals(e.getCode())) {
                caughtException = true;
            }
        }
        
        assertTrue(caughtException);
        
        String goodStringFilter = "(&(|(displayName=*)(cn=*)(sn=*)(givenName=*)(mail=*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*\\28EMC\\29))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))";
        // String goodStringFilter = "(displayName=*\\28EMC\\29)";
        filter = filterDactory.fromFilterString(FilterId.UNITTEST, goodStringFilter);
        // System.out.println(filter.toFilterString());
        
        /*
DEV:
zmprov mcf -zimbraGalLdapFilterDef 'zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))'
zmprov mcf +zimbraGalLdapFilterDef 'zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource))(!(displayName=*\28EMC\29)))'


DF:
zmprov mds galsync@zimbra.com VMware -zimbraGalSyncLdapFilter '(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*EMC))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'
zmprov mds galsync@zimbra.com VMware +zimbraGalSyncLdapFilter '(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*\28EMC\29))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'

zmprov mcf -zimbraGalLdapFilterDef 'ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*EMC))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'
zmprov mcf +zimbraGalLdapFilterDef 'ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*\28EMC\29))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'
         */
    }
    
    @Test
    public void autoProvisionSearchCreatedLaterThan() throws Exception {
        String FILTER = "(foo=bar)";
        String GENERALIZED_TIME = "20111005190522Z";
        
        String filter = "(&" + FILTER + "(createTimestamp>=" + GENERALIZED_TIME + "))";
        
        ZLdapFilter createdLaterThanFilter = filterDactory.createdLaterOrEqual(GENERALIZED_TIME);
        String filter2 = "(&" + FILTER + createdLaterThanFilter.toFilterString() + ")";
        
        assertEquals(filter, filter2);
        
        ZLdapFilter zLdapFilter = filterDactory.fromFilterString(
                FilterId.AUTO_PROVISION_SEARCH_CREATED_LATERTHAN, filter2);
        verifyStatString(FilterId.AUTO_PROVISION_SEARCH_CREATED_LATERTHAN, zLdapFilter);
    }
    
    @Test
    public void memberOf() throws Exception {
        String DN = "dc=com";
        
        String filter = LegacyLdapFilter.memberOf(DN);
        ZLdapFilter zLdapFilter = filterDactory.memberOf(DN);
        verify(FilterId.MEMBER_OF, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsByDomain(DOMAIN_NAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsOnlyByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsOnlyByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsOnlyByDomain(DOMAIN_NAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllCalendarResourcesByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllCalendarResourcesByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllCalendarResourcesByDomain(DOMAIN_NAME);
        verify(FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN, filter, zLdapFilter);
    }

    @Test
    public void velodromeAllAccountsByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN_AND_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsOnlyByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsOnlyByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsOnlyByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN_AND_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllCalendarResourcesByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllCalendarResourcesByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllCalendarResourcesByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        verify(FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN_AND_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void dnSubtreeMatch() throws Exception {
        String DN1 = "ou=people,dc=test,dc=com";
        String DN2 = "cn=groups,dc=test,dc=com";
        
        String filter = LegacyLdapFilter.dnSubtreeMatch(DN1, DN2);
        ZLdapFilter zLdapFilter = filterDactory.dnSubtreeMatch(DN1, DN2);
        verify(FilterId.DN_SUBTREE_MATCH, filter, zLdapFilter);
    }
    
    @Test
    public void toIDNFilter() throws Exception {
        assertEquals("(!(zimbraDomainName=*\u4e2d\u6587*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(zimbraDomainName=*\u4e2d\u6587*))"));
        
        assertEquals("(objectClass=*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(objectClass=*)"));
        
        // this is actually an invalid filter, will throw during search
        assertEquals("(!(objectClass=**))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(objectClass=**))"));
        
        assertEquals("(!(objectClass=*abc))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(objectClass=*abc))"));
        
        assertEquals("(!(objectClass=abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(objectClass=abc*))"));
        
        assertEquals("(!(objectClass=*abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*abc*)"));

        assertEquals("(|(zimbraMailDeliveryAddress=*@test.xn--fiq228c.com)(zimbraMailAlias=*@test.xn--fiq228c.com))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(|(zimbraMailDeliveryAddress=*@test.\u4e2d\u6587.com)(zimbraMailAlias=*@test.\u4e2d\u6587.com))"));

        
        /*
         * legacy JNDI results
         */
        /*
        assertEquals("!(zimbraDomainName=*\u4e2d\u6587*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(zimbraDomainName=*\u4e2d\u6587*)"));
        
        assertEquals("(!(objectClass=*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*)"));
        
        assertEquals("(!(objectClass=**))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=**)"));

        assertEquals("(!(objectClass=*abc))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*abc)"));
        
        assertEquals("(!(objectClass=abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=abc*)"));
        
        assertEquals("(!(objectClass=*abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*abc*)"));
        
        assertEquals("(|(zimbraMailDeliveryAddress=*@test.xn--fiq228c.com)(zimbraMailAlias=*@test.xn--fiq228c.com))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(|(zimbraMailDeliveryAddress=*@test.\u4e2d\u6587.com)(zimbraMailAlias=*@test.\u4e2d\u6587.com))"));
        */
    }
    
    @Test
    @Bug(bug=68964)
    public void toIDNFilterTrailingDot() throws Exception {
        assertEquals("(zimbraMailDeliveryAddress=.)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=.)"));

        assertEquals("(zimbraMailDeliveryAddress=...)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=...)"));
        
        assertEquals("(zimbraMailDeliveryAddress=.a.)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=.a.)"));
        
        assertEquals("(zimbraMailDeliveryAddress=a.b.)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=a.b.)"));
        
        assertEquals("(zimbraMailDeliveryAddress=*.*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=*.*)"));
    }

    @Test
    @Bug(bug=68965)
    public void toIDNFilterWithCharsNeedEscaping() throws Exception {
        assertEquals("(objectClass=*\\2a*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(objectClass=*\\2a*)"));
        
        assertEquals("(objectClass=*\\28*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(objectClass=*\\28*)"));
        
        assertEquals("(objectClass=*\\29*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(objectClass=*\\29*)"));
        
        assertEquals("(objectClass=*\\5c*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(objectClass=*\\5c*)"));
    }
    
}
