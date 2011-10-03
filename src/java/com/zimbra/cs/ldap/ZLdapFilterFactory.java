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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.ldap.LdapTODO.TODO;

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
        ADDRS_EXIST(SINGLETON.addrsExist(new String[]{"{ADDR-1}", "ADDR-2", "..."})),
        ADMIN_ACCOUNT_BY_ADMIN_FLAG(SINGLETON.adminAccountByAdminFlag()),
        ADMIN_ACCOUNT_BY_RDN(SINGLETON.adminAccountByRDN("{NAMING-RDN-ATTR}", "{NAME}")),
        ALL_ACCOUNTS(SINGLETON.allAccounts()),
        ALL_NON_SYSTEM_ACCOUNTS(SINGLETON.allNonSystemAccounts()),
        ANY_ENTRY(SINGLETON.anyEntry()),
        HAS_SUBORDINATES(SINGLETON.hasSubordinates()),
        
        // ids for fromFilterString(0 calls
        AUTO_PROVISION_GET_EXTERNAL_ATTRS("Filter in " + Provisioning.A_zimbraAutoProvLdapSearchFilter),
        EXTERNAL_GROUP("Filter in " + Provisioning.A_zimbraExternalGroupLdapSearchFilter),
        LDAP_AUTHENTICATE("Filter in " + Provisioning.A_zimbraAuthLdapSearchFilter),
        NGINX_GET_DOMAIN_BY_SERVER_IP("Filter in "),
        NGINX_GET_PORT_BY_MAILHOST("Filter in "),
        NGINX_GET_MAILHOST("Filter in " + Provisioning.A_zimbraReverseProxyMailHostQuery),
        UNITTEST("UNITTEST");
        
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
    
    
    /*
     * operational
     */
    public abstract ZLdapFilter hasSubordinates();
    
    /*
     * general
     */
    public abstract ZLdapFilter anyEntry();
    public abstract ZLdapFilter fromFilterString(FilterId filterId, String filterString) throws LdapException;
    
    /*
     * Mail target (accounts and groups)
     */
    public abstract ZLdapFilter addrsExist(String[] addrs);
    
    /*
     * account
     */
    public abstract ZLdapFilter allAccounts();
    public abstract ZLdapFilter allNonSystemAccounts();
    public abstract ZLdapFilter accountByForeignPrincipal(String foreignPrincipal);
    public abstract ZLdapFilter accountById(String id);
    public abstract ZLdapFilter accountByMemberOf(String dynGroupId);
    public abstract ZLdapFilter accountByName(String name);
    public abstract ZLdapFilter adminAccountByAdminFlag();
    public abstract ZLdapFilter adminAccountByRDN(String namingRdnAttr, String name);
    
    
    @TODO  // TODO: refactor so com.zimbra.cs.ldap does not have dependency on com.zimbra.cs.account
    public abstract ZLdapFilter accountsHomedOnServer(Server server); 
    
    @TODO  // TODO: refactor so com.zimbra.cs.ldap does not have dependency on com.zimbra.cs.account
    public ZLdapFilter homedOnServer(Server server) {
        String serverName = server.getAttr(Provisioning.A_zimbraServiceHostname);
        return homedOnServer(serverName);
    }
    
    public abstract ZLdapFilter homedOnServer(String serverName);
    public abstract ZLdapFilter accountsOnServerOnCosHasSubordinates(Server server, String cosId);
    
    /*
     * calendar resource
     */
    public abstract ZLdapFilter allCalendarResources();
    public abstract ZLdapFilter calendarResourceByForeignPrincipal(String foreignPrincipal);
    public abstract ZLdapFilter calendarResourceById(String id);
    public abstract ZLdapFilter calendarResourceByName(String name);
    
    /*
     * cos
     */
    public abstract ZLdapFilter allCoses();
    public abstract ZLdapFilter cosById(String id);
    public abstract ZLdapFilter cosesByMailHostPool(String server);
    
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
     * signature
     */
    public abstract ZLdapFilter allSignatures();
    public abstract ZLdapFilter signatureById(String id);
    
    /*
     * zimlet
     */
    public abstract ZLdapFilter allZimlets();
    
    /* 
     * XMPPComponent
     */
    public abstract ZLdapFilter allXMPPComponents();
    public abstract ZLdapFilter imComponentById(String id);
    
    public abstract ZLdapFilter xmppComponentById(String id);
}
