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
package com.zimbra.cs.ldap.jndi;

import com.zimbra.cs.account.ldap.legacy.LegacyLdapFilter;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

public class JNDILdapFilterFactory extends ZLdapFilterFactory {
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }

    
    /*
     * operational
     */
    @Override
    public ZLdapFilter hasSubordinates() {
        return new JNDILdapFilter(LegacyLdapFilter.hasSubordinates());
    }
    
    @Override
    public ZLdapFilter createdLaterOrEqual(String generalizedTime) {
        return new JNDILdapFilter(LegacyLdapFilter.createdLaterOrEqual(generalizedTime));
    }
    
    /*
     * general
     */
    @Override
    public ZLdapFilter anyEntry() {
        return new JNDILdapFilter(LegacyLdapFilter.anyEntry());
    }
    
    @Override
    public ZLdapFilter fromFilterString(FilterId filterId, String filterString) 
    throws LdapException {
        return new JNDILdapFilter(filterString);
    }
    
    
    /*
     * Mail target (accounts and groups)
     */
    @Override
    public ZLdapFilter addrsExist(String[] addrs) {
        return new JNDILdapFilter(LegacyLdapFilter.addrsExist(addrs));
    }
    
    
    /*
     * account
     */
    @Override
    public ZLdapFilter allAccounts() {
        return new JNDILdapFilter(LegacyLdapFilter.allAccounts());
    }
    
    @Override
    public ZLdapFilter allNonSystemAccounts() {
        return new JNDILdapFilter(LegacyLdapFilter.allNonSystemAccounts());
    }

    @Override
    public ZLdapFilter accountByForeignPrincipal(String foreignPrincipal) {
        return new JNDILdapFilter(LegacyLdapFilter.accountByForeignPrincipal(foreignPrincipal));
    }

    @Override
    public ZLdapFilter accountById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.accountById(id));
    }
    
    @Override
    public ZLdapFilter accountByMemberOf(String dynGroupId) {
        return new JNDILdapFilter(LegacyLdapFilter.accountByMemberOf(dynGroupId));
    }
    
    @Override
    public ZLdapFilter accountByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.accountByName(name));
    }

    @Override
    public ZLdapFilter adminAccountByAdminFlag() {
        return new JNDILdapFilter(LegacyLdapFilter.adminAccountByAdminFlag());
    }
    
    @Override
    public ZLdapFilter adminAccountByRDN(String namingRdnAttr, String name) {
        return new JNDILdapFilter(LegacyLdapFilter.adminAccountByRDN(namingRdnAttr, name));
    }

    @Override
    public ZLdapFilter accountsHomedOnServer(String serverServiceHostname) {
        return new JNDILdapFilter(LegacyLdapFilter.accountsHomedOnServer(serverServiceHostname));
    }
    
    @Override
    public ZLdapFilter accountsHomedOnServerAccountsOnly(String serverServiceHostname) {
        return new JNDILdapFilter(LegacyLdapFilter.accountsHomedOnServerAccountsOnly(serverServiceHostname));
    }

    @Override
    public ZLdapFilter homedOnServer(String serverName) {
        return new JNDILdapFilter(LegacyLdapFilter.homedOnServer(serverName));
    }

    @Override
    public ZLdapFilter accountsOnServerAndCosHasSubordinates(String serverServiceHostname, String cosId) {
        return new JNDILdapFilter(LegacyLdapFilter.accountsOnServerOnCosHasSubordinates(serverServiceHostname, cosId));
    }

    
    /*
     * calendar resource
     */
    @Override
    public ZLdapFilter allCalendarResources() {
        return new JNDILdapFilter(LegacyLdapFilter.allCalendarResources());
    }

    @Override
    public ZLdapFilter calendarResourceByForeignPrincipal(String foreignPrincipal) {
        return new JNDILdapFilter(LegacyLdapFilter.calendarResourceByForeignPrincipal(foreignPrincipal));
    }

    @Override
    public ZLdapFilter calendarResourceById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.calendarResourceById(id));
    }

    @Override
    public ZLdapFilter calendarResourceByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.calendarResourceByName(name));       
    }

    
    /*
     * cos
     */
    @Override
    public ZLdapFilter allCoses() {
        return new JNDILdapFilter(LegacyLdapFilter.allCoses());
    }

    @Override
    public ZLdapFilter cosById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.cosById(id));
    }

    @Override
    public ZLdapFilter cosesByMailHostPool(String server) {
        return new JNDILdapFilter(LegacyLdapFilter.cosesByMailHostPool(server));
    }

    
    /*
     * data source
     */
    @Override
    public ZLdapFilter allDataSources() {
        return new JNDILdapFilter(LegacyLdapFilter.allDataSources());
    }

    @Override
    public ZLdapFilter dataSourceById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.dataSourceById(id));
    }

    @Override
    public ZLdapFilter dataSourceByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.dataSourceByName(name));
    }

    
    /*
     * distribution list
     */
    @Override
    public ZLdapFilter allDistributionLists() {
        return new JNDILdapFilter(LegacyLdapFilter.allDistributionLists());
    }

    @Override
    public ZLdapFilter distributionListById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.distributionListById(id));
    }

    @Override
    public ZLdapFilter distributionListByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.distributionListByName(name));
    }

    
    /*
     * dynamic group
     */
    @Override
    public ZLdapFilter dynamicGroupById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.dynamicGroupById(id));
    }
    
    @Override
    public ZLdapFilter dynamicGroupByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.dynamicGroupByName(name));
    }
    
    /*
     * groups (distribution list or dynamic group)
     */
    @Override
    public ZLdapFilter allGroups() {
        return new JNDILdapFilter(LegacyLdapFilter.allGroups());
    }
    
    public ZLdapFilter groupById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.groupById(id));
    }
    
    public ZLdapFilter groupByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.groupByName(name));
    }
    
    /*
     * domain
     */
    @Override
    public ZLdapFilter allDomains() {
        return new JNDILdapFilter(LegacyLdapFilter.allDomains());
    }

    @Override
    public ZLdapFilter domainById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.domainById(id));
    }

    @Override
    public ZLdapFilter domainByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.domainByName(name));
    }

    @Override
    public ZLdapFilter domainByKrb5Realm(String krb5Realm) {
        return new JNDILdapFilter(LegacyLdapFilter.domainByKrb5Realm(krb5Realm));
    }
    
    @Override
    public ZLdapFilter domainByVirtualHostame(String virtualHostname) {
        return new JNDILdapFilter(LegacyLdapFilter.domainByVirtualHostame(virtualHostname));
    }
    
    @Override
    public ZLdapFilter domainByForeignName(String foreignName) {
        return new JNDILdapFilter(LegacyLdapFilter.domainByForeignName(foreignName));
    }

    @Override
    public ZLdapFilter domainLabel() {
        return new JNDILdapFilter(LegacyLdapFilter.domainLabel());
    }

    @Override
    public ZLdapFilter domainLockedForEagerAutoProvision() {
        return new JNDILdapFilter(LegacyLdapFilter.domainLockedForEagerAutoProvision());
    }
    
    
    /*
     * global config
     */
    public ZLdapFilter globalConfig() {
        return new JNDILdapFilter(LegacyLdapFilter.globalConfig());
    }
    
    
    /*
     * identity
     */
    @Override
    public ZLdapFilter allIdentities() {
        return new JNDILdapFilter(LegacyLdapFilter.allIdentities());
    }

    @Override
    public ZLdapFilter identityByName(String name) {
        return new JNDILdapFilter(LegacyLdapFilter.identityByName(name));
    }

    
    /*
     * mime enrty
     */
    @Override
    public ZLdapFilter allMimeEntries() {
        return new JNDILdapFilter(LegacyLdapFilter.allMimeEntries());
    }

    @Override
    public ZLdapFilter mimeEntryByMimeType(String mimeType) {
        return new JNDILdapFilter(LegacyLdapFilter.mimeEntryByMimeType(mimeType));
    }
    

    /*
     * server
     */
    @Override
    public ZLdapFilter allServers() {
        return new JNDILdapFilter(LegacyLdapFilter.allServers());
    }

    @Override
    public ZLdapFilter serverById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.serverById(id));
    }

    @Override
    public ZLdapFilter serverByService(String service) {
        return new JNDILdapFilter(LegacyLdapFilter.serverByService(service));
    }

    
    /*
     * signature
     */
    @Override
    public ZLdapFilter allSignatures() {
        return new JNDILdapFilter(LegacyLdapFilter.allSignatures());
    }

    @Override
    public ZLdapFilter signatureById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.signatureById(id));
    }

    
    /* 
     * XMPPComponent
     */
    @Override
    public ZLdapFilter allXMPPComponents() {
        return new JNDILdapFilter(LegacyLdapFilter.allXMPPComponents());
    }
    
    @Override
    public ZLdapFilter imComponentById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.imComponentById(id));
    }

    @Override
    public ZLdapFilter xmppComponentById(String id) {
        return new JNDILdapFilter(LegacyLdapFilter.xmppComponentById(id));
    }


    /*
     * zimlet
     */
    @Override
    public ZLdapFilter allZimlets() {
        return new JNDILdapFilter(LegacyLdapFilter.allZimlets());
    }
    
    
    /*
     * AD
     */
    public ZLdapFilter memberOf(String dnOfGroup) {
        return new JNDILdapFilter(LegacyLdapFilter.memberOf(dnOfGroup));
    }
}
