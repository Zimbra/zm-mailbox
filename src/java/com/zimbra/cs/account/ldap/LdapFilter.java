/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;

public class LdapFilter {

    private static final String FILTER_ACCOUNT_OBJECTCLASS = "(objectclass=zimbraAccount)";
    private static final String FILTER_CALENDAR_RESOURCE_OBJECTCLASS = "(objectclass=zimbraCalendarResource)";
    private static final String FILTER_DISTRIBUTION_LIST_OBJECTCLASS = "(objectclass=zimbraDistributionList)";
    
    /*
     * account
     */
    public static String allNonSystemAccounts() {
        StringBuilder buf = new StringBuilder();
        buf.append("(&");
        buf.append("(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource))");
        buf.append("(!(zimbraIsSystemResource=TRUE))");
        buf.append(")");

        return buf.toString();
    }
    
    public static String allAccounts() {
        return FILTER_ACCOUNT_OBJECTCLASS;
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
    
    public static String adminAccountByRDN(String namingRdnAttr, String name) {
        return "(&(" + namingRdnAttr + "=" + name + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String adminAccountByAdminFlag() {
        return "(|(zimbraIsAdminAccount=TRUE)(zimbraIsDelegatedAdminAccount=TRUE)(zimbraIsDomainAdminAccount=TRUE))";
    }
    
    /*
     * calendar resource
     */
    public static String allCalendarResources() {
        return "(objectclass=zimbraCalendarResource)";
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
    
    /*
     * cos
     */
    public static String allCoses() {
        return "(objectclass=zimbraCOS)";
    }
    
    public static String cosById(String id) {
        return "(&(zimbraId=" + id + ")(objectclass=zimbraCOS))";
    }
    
    public static String cosesByMailHostPool(String server) {
        return "(&(objectclass=zimbraCOS)(zimbraMailHostPool=" + server + "))";
    }
    
    /*
     * data source
     */
    public static String allDataSources() {
        return "(objectclass=zimbraDataSource)";
    }
    
    public static String dataSourceById(String id) {
        return "(&(objectclass=zimbraDataSource)(zimbraDataSourceId=" + id + "))";
    }
    
    public static String dataSourceByName(String name) {
        return "(&(objectclass=zimbraDataSource)(zimbraDataSourceName=" + name + "))";
    }
    
    /* 
     * XMPPComponent
     */
    public static String imComponentById(String id) {
        return "(&(objectclass=zimbraXMPPComponent)(zimbraXMPPComponentId=" + id + "))";
    }
    
    /*
     * distribution list
     */
    public static String allDistributionLists() {
        return "(objectclass=zimbraDistributionList)";
    }
    
    public static String distributionListById(String id) {
        return "(&(zimbraId=" + id + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
    }
    
    public static String distributionListByName(String name) {
        return "(&(zimbraMailAlias=" + name + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
    }
    
    /*
     * domain
     */
    public static String allDomains() {
        return "(objectclass=zimbraDomain)";
    }
    
    public static String domainById(String id) {
        return "(&(zimbraId=" + id + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainByName(String name) {
        return "(&(zimbraDomainName=" + name + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainByKrb5Realm(String krb5Realm) {
        return "(&(zimbraAuthKerberos5Realm=" + krb5Realm + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainByVirtualHostame(String virtualHostname) {
        return "(&(zimbraVirtualHostname=" + virtualHostname + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainLabel() {
        return "objectclass=dcObject";
    }
    
    /*
     * identity
     */
    public static String allIdentities() {
        return "(objectclass=zimbraIdentity)";
    }
    
    public static String identityByName(String name) {
        return "(&(objectclass=zimbraIdentity)(zimbraPrefIdentityName=" + name + "))";
    }
    
    /*
     * mime enrty
     */
    public static String allMimeEntries() {
        return "(objectclass=zimbraMimeEntry)";
    }
    
    public static String mimeEntryByMimeType(String mimeType) {
        return "(zimbraMimeType=" + mimeType + ")";
    }
    
    /*
     * server
     */
    public static String allServers() {
        return "(objectclass=zimbraServer)";
    }
    
    public static String serverById(String id) {
        return "(&(zimbraId=" + id + ")(objectclass=zimbraServer))";
    }
    
    public static String serverByService(String service) {
        return "(&(objectclass=zimbraServer)(zimbraServiceEnabled=" + service + "))";
    }
    
    /*
     * signature
     */
    public static String allSignatures() {
        return "(objectclass=zimbraSignature)";
    }
    
    public static String signatureById(String id) {
        return "(&(objectclass=zimbraSignature)(zimbraSignatureId=" + id +"))";
    }
    
    /*
     * zimlet
     */
    public static String allZimlets() {
        return "(objectclass=zimbraZimletEntry)";
    }
    
    /*
     * xmppcomponent
     */
    public static String xmppComponentById(String id) {
        return "(&(zimbraId=" + id + ")(objectclass=zimbraXMPPComponent))";
    }
    public static String allXMPPComponents() {
        return "(objectclass=zimbraXMPPComponent)";
    }
    
    
    private static void printFilter(String doc, String usage, String base, String filter) {
        System.out.println();
        System.out.println(doc);
        System.out.println("usage:  " + usage);
        System.out.println("base:   " + base);
        System.out.println("filter: " + filter);
    }
    
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
}
