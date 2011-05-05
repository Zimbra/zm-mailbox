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

import com.zimbra.cs.account.Server;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.prov.ldap.LdapFilter;

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
        return new JNDILdapFilter(LdapFilter.hasSubordinates());
    }
    
    /*
     * general
     */
    @Override
    public ZLdapFilter anyEntry() {
        return new JNDILdapFilter(LdapFilter.anyEntry());
    }
    
    @Override
    public ZLdapFilter fromFilterString(String filterString) throws LdapException {
        return new JNDILdapFilter(filterString);
    }
    
    
    /*
     * account
     */
    @Override
    public ZLdapFilter allAccounts() {
        return new JNDILdapFilter(LdapFilter.allAccounts());
    }
    
    @Override
    public ZLdapFilter allNonSystemAccounts() {
        return new JNDILdapFilter(LdapFilter.allNonSystemAccounts());
    }

    @Override
    public ZLdapFilter accountByForeignPrincipal(String foreignPrincipal) {
        return new JNDILdapFilter(LdapFilter.accountByForeignPrincipal(foreignPrincipal));
    }

    @Override
    public ZLdapFilter accountById(String id) {
        return new JNDILdapFilter(LdapFilter.accountById(id));
    }

    @Override
    public ZLdapFilter accountByName(String name) {
        return new JNDILdapFilter(LdapFilter.accountByName(name));
    }

    @Override
    public ZLdapFilter adminAccountByRDN(String namingRdnAttr, String name) {
        return new JNDILdapFilter(LdapFilter.adminAccountByRDN(namingRdnAttr, name));
    }

    @Override
    public ZLdapFilter adminAccountByAdminFlag() {
        return new JNDILdapFilter(LdapFilter.adminAccountByAdminFlag());
    }

    @Override
    public ZLdapFilter accountsHomedOnServer(Server server) {
        return new JNDILdapFilter(LdapFilter.accountsHomedOnServer(server));
    }

    @Override
    public ZLdapFilter homedOnServer(String serverName) {
        return new JNDILdapFilter(LdapFilter.homedOnServer(serverName));
    }

    @Override
    public ZLdapFilter accountsOnServerOnCosHasSubordinates(Server server, String cosId) {
        return new JNDILdapFilter(LdapFilter.accountsOnServerOnCosHasSubordinates(server, cosId));
    }

    
    /*
     * calendar resource
     */
    @Override
    public ZLdapFilter allCalendarResources() {
        return new JNDILdapFilter(LdapFilter.allCalendarResources());
    }

    @Override
    public ZLdapFilter calendarResourceByForeignPrincipal(String foreignPrincipal) {
        return new JNDILdapFilter(LdapFilter.calendarResourceByForeignPrincipal(foreignPrincipal));
    }

    @Override
    public ZLdapFilter calendarResourceById(String id) {
        return new JNDILdapFilter(LdapFilter.calendarResourceById(id));
    }

    @Override
    public ZLdapFilter calendarResourceByName(String name) {
        return new JNDILdapFilter(LdapFilter.calendarResourceByName(name));       
    }

    
    /*
     * cos
     */
    @Override
    public ZLdapFilter allCoses() {
        return new JNDILdapFilter(LdapFilter.allCoses());
    }

    @Override
    public ZLdapFilter cosById(String id) {
        return new JNDILdapFilter(LdapFilter.cosById(id));
    }

    @Override
    public ZLdapFilter cosesByMailHostPool(String server) {
        return new JNDILdapFilter(LdapFilter.cosesByMailHostPool(server));
    }

    
    /*
     * data source
     */
    @Override
    public ZLdapFilter allDataSources() {
        return new JNDILdapFilter(LdapFilter.allDataSources());
    }

    @Override
    public ZLdapFilter dataSourceById(String id) {
        return new JNDILdapFilter(LdapFilter.dataSourceById(id));
    }

    @Override
    public ZLdapFilter dataSourceByName(String name) {
        return new JNDILdapFilter(LdapFilter.dataSourceByName(name));
    }

    
    /*
     * distribution list
     */
    @Override
    public ZLdapFilter allDistributionLists() {
        return new JNDILdapFilter(LdapFilter.allDistributionLists());
    }

    @Override
    public ZLdapFilter distributionListById(String id) {
        return new JNDILdapFilter(LdapFilter.distributionListById(id));
    }

    @Override
    public ZLdapFilter distributionListByName(String name) {
        return new JNDILdapFilter(LdapFilter.distributionListByName(name));
    }

    
    /*
     * domain
     */
    @Override
    public ZLdapFilter allDomains() {
        return new JNDILdapFilter(LdapFilter.allDomains());
    }

    @Override
    public ZLdapFilter domainById(String id) {
        return new JNDILdapFilter(LdapFilter.domainById(id));
    }

    @Override
    public ZLdapFilter domainByName(String name) {
        return new JNDILdapFilter(LdapFilter.domainByName(name));
    }

    @Override
    public ZLdapFilter domainByKrb5Realm(String krb5Realm) {
        return new JNDILdapFilter(LdapFilter.domainByKrb5Realm(krb5Realm));
    }
    
    @Override
    public ZLdapFilter domainByVirtualHostame(String virtualHostname) {
        return new JNDILdapFilter(LdapFilter.domainByVirtualHostame(virtualHostname));
    }
    
    @Override
    public ZLdapFilter domainByForeignName(String foreignName) {
        return new JNDILdapFilter(LdapFilter.domainByForeignName(foreignName));
    }

    @Override
    public ZLdapFilter domainLabel() {
        return new JNDILdapFilter(LdapFilter.domainLabel());
    }

    
    /*
     * identity
     */
    @Override
    public ZLdapFilter allIdentities() {
        return new JNDILdapFilter(LdapFilter.allIdentities());
    }

    @Override
    public ZLdapFilter identityByName(String name) {
        return new JNDILdapFilter(LdapFilter.identityByName(name));
    }

    
    /*
     * mime enrty
     */
    @Override
    public ZLdapFilter allMimeEntries() {
        return new JNDILdapFilter(LdapFilter.allMimeEntries());
    }

    @Override
    public ZLdapFilter mimeEntryByMimeType(String mimeType) {
        return new JNDILdapFilter(LdapFilter.mimeEntryByMimeType(mimeType));
    }
    

    /*
     * server
     */
    @Override
    public ZLdapFilter allServers() {
        return new JNDILdapFilter(LdapFilter.allServers());
    }

    @Override
    public ZLdapFilter serverById(String id) {
        return new JNDILdapFilter(LdapFilter.serverById(id));
    }

    @Override
    public ZLdapFilter serverByService(String service) {
        return new JNDILdapFilter(LdapFilter.serverByService(service));
    }

    
    /*
     * signature
     */
    @Override
    public ZLdapFilter allSignatures() {
        return new JNDILdapFilter(LdapFilter.allSignatures());
    }

    @Override
    public ZLdapFilter signatureById(String id) {
        return new JNDILdapFilter(LdapFilter.signatureById(id));
    }

    
    /* 
     * XMPPComponent
     */
    @Override
    public ZLdapFilter allXMPPComponents() {
        return new JNDILdapFilter(LdapFilter.allXMPPComponents());
    }
    
    @Override
    public ZLdapFilter imComponentById(String id) {
        return new JNDILdapFilter(LdapFilter.imComponentById(id));
    }

    @Override
    public ZLdapFilter xmppComponentById(String id) {
        return new JNDILdapFilter(LdapFilter.xmppComponentById(id));
    }


    /*
     * zimlet
     */
    @Override
    public ZLdapFilter allZimlets() {
        return new JNDILdapFilter(LdapFilter.allZimlets());
    }
}
