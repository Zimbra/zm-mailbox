/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.prov.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.AssertionFailedError;
import org.junit.*;

import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

public class TestLdapProvRenameDomain extends LdapTest {
    private static LdapProv ldapProv;
    private static Provisioning prov;
    private static String TEST_ID;
    
    private static String PASSWORD = "test123";
    
    private static int NUM_ACCOUNTS    = 3;
    private static int NUM_CAS         = 3;  // calendar resources, TODO
    private static int NUM_DLS_NESTED  = 2;
    private static int NUM_DLS_TOP     = 2;
    private static int NUM_DYNAMIC_GROUPS = 2;
    private static int NUM_DOMAINS     = 3;
    private static int NUM_SUB_DOMAINS = 2;  // number of sub domains under the old domain(domain to be renamed)
    private static int NUM_XMPPCOMPONENTS = 3; 
    
    /*
     * NUM_IDENTITIES and NUM_DATASOURCES must be >= NUM_SIGNATURES, each identity/datasource get one signature of the same index.
     * identities and datasources that do not have corresponding signature will get no signature. 
     * e.g. 
     *     identity-1 -> signature-1
     *     identity-2 -> signature-2 
     *     identity-3 -> (no signature)
     * 
     */ 
    private static int NUM_SIGNATURES  = 2; 
    private static int NUM_IDENTITIES  = 2;
    private static int NUM_DATASOURCES = 2;
    
    private static String NAME_LEAF_OLD_DOMAIN = "olddomain";
    private static String NAME_LEAF_NEW_DOMAIN = "newdomain";
    private static String UNICODESTR = "\u4e2d\u6587";   // \u5f35\u611b\u73b2   // for testing IDN
    // private static String UNICODESTR = "english";
    
    private static String NAMEPREFIX_ACCOUNT     = "acct-";
    private static String NAMEPREFIX_ALIAS       = "alias-";
    private static String NAMEPREFIX_DATASOURCE  = "datasource-";
    private static String NAMEPREFIX_DL_NESTED   = "nesteddl-";
    private static String NAMEPREFIX_DL_TOP      = "topdl-";
    private static String NAMEPREFIX_DYNAMIC_GROUP      = "dynamicgroup-";
    private static String NAMEPREFIX_IDENTITY    = "identity-";
    private static String NAMEPREFIX_OTHERDOMAIN = "otherdomain-";
    private static String NAMEPREFIX_SIGNATURE   = "signature-";
    private static String NAMEPREFIX_SUB_DOMAIN  = "subdomain-";
    
    // pseudo domain index for the old domain and new domain, so that we can use the unified interfaces
    private static int OLD_DOMAIN  = 0;
    private static int NEW_DOMAIN  = -1;

    private static final int OBJ_ACCT = 0x1;
    private static final int OBJ_DL_NESTED = 0x2;
    private static final int OBJ_DL_TOP = 0x4;
    private static final int OBJ_DYNAMIC_GROUP = 0x8;

    private static final Set<String> sAttrsToVerify;
    
    static {
        sAttrsToVerify = new HashSet<String>();
        
        sAttrsToVerify.add(Provisioning.A_zimbraMailCanonicalAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailForwardingAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailCatchAllAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailCatchAllCanonicalAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailCatchAllForwardingAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender);
    }
    
    int NUM_OBJS(int objType) throws Exception {
        switch (objType) {
            case OBJ_ACCT:
                return TestLdapProvRenameDomain.NUM_ACCOUNTS;
            case OBJ_DL_NESTED:
                return TestLdapProvRenameDomain.NUM_DLS_NESTED;
            case OBJ_DL_TOP:
                return TestLdapProvRenameDomain.NUM_DLS_TOP;
            case OBJ_DYNAMIC_GROUP:
                return TestLdapProvRenameDomain.NUM_DYNAMIC_GROUPS;
        }
        throw new Exception();
    }
        
    String OBJ_NAME(int objType, int index, int domainIdx) throws Exception {
        switch (objType) {
            case OBJ_ACCT:
                return ACCOUNT_NAME(index, domainIdx);
            case OBJ_DL_NESTED:
                return NESTED_DL_NAME(index, domainIdx);
            case OBJ_DL_TOP:
                return TOP_DL_NAME(index, domainIdx);
            case OBJ_DYNAMIC_GROUP:
                return DYNAMIC_GROUP_NAME(index, domainIdx);
        }
        throw new Exception();
    }
    
    String GET_ALIAS_NAME(int objType, int targetIdx, int targetDomainIdx, int aliasDomainIdx) throws Exception {
        switch (objType) {
            case OBJ_ACCT:
                return ACCOUNT_ALIAS_NAME(targetIdx, targetDomainIdx, aliasDomainIdx);
            case OBJ_DL_NESTED:
                return NESTED_DL_ALIAS_NAME(targetIdx, targetDomainIdx, aliasDomainIdx);
            case OBJ_DL_TOP:
                return TOP_DL_ALIAS_NAME(targetIdx, targetDomainIdx, aliasDomainIdx);
            case OBJ_DYNAMIC_GROUP:
                return DYNAMIC_GROUP_ALIAS_NAME(targetIdx, targetDomainIdx, aliasDomainIdx);
        }
        throw new Exception();
    }
    
    private void setLdapProv() {
        prov = ldapProv;
    }
    
    @BeforeClass
    public static void init() throws Exception {
        // ZimbraLog.toolSetupLog4j("INFO", "/Users/pshao/p4/main/ZimbraServer/conf/log4j.properties.cli");
        ZimbraLog.toolSetupLog4j("INFO", "/opt/zimbra/conf/log4j.properties");
        
        TEST_ID = genTestId();
        
        System.out.println("\nTest " + TEST_ID + "\n");
        
        Provisioning prov = Provisioning.getInstance();
        assertTrue(prov instanceof LdapProv);
        ldapProv = (LdapProv) prov;
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    
    private void prepareDomain() throws Exception {    
        setLdapProv();
         
        /*
         * Create NUM_DOMAINS domains: one domain(the first one) to be renamed, and NUM_DOMAINS-1 other domains.  
         *     - Each domain:
         *           - has NUM_ACCOUNTS accounts
         *           - has NUM_DLS_TOP top level dls (dl that is not a nested DL)
         *           - has NUM_DLS_NESTED nested dls (dl under another DL)
         *           - has NUM_DYNAMIC_GROUPS dynamic groups
         * 
         *     - Each account:
         *           - has NUM_DOMAINS aliases, one in each domain
         *           - is a member of all DLs in all domains
         *           
         *     - Each alias:
         *           - is a member of all DLs in all domains  
         *           
         *     - Each top dl:
         *           - has NUM_DOMAINS aliases, one in each domain
         *                 
         *     - Each nested dl:
         *           - has NUM_DOMAINS aliases, one in each domain
         *           - is a member of all top DLs in all domains
         *           
         *     - Each dynamic group:
         *           - has NUM_DOMAINS aliases, one in each domain
         */
        
        // create domains
        for (int i = 0; i < NUM_DOMAINS; i++)
            createDomain(DOMAIN_NAME(i));
        
        // create sub domains under the domain-to-rename
        for (int i = 0; i < NUM_SUB_DOMAINS; i++) 
            createDomain(SUB_DOMAIN_NAME(i, OLD_DOMAIN));
        
        // setup entries in domains
        for (int i = 0; i < NUM_DOMAINS; i++)     
            populateDomain(i);
        
        for (int i = 0; i < NUM_DOMAINS; i++)     
            setupDLs(i);
        
        // create XMPPComponents pointing/not pointing to the domain to be renamed
        Domain oldDomain = prov.get(Key.DomainBy.name, DOMAIN_NAME(OLD_DOMAIN));
        for (int i = 0; i < NUM_XMPPCOMPONENTS; i++)     
            createXMPPComponent(i, oldDomain);

    }
        
    private int DOMAIN_INDEX_AFTER_RENAME(int domainIdx) {
        if (domainIdx == OLD_DOMAIN)
            return NEW_DOMAIN;
        else
            return domainIdx;
    }
    
    private String DOMAIN_NAME(String leafDomainName) {
        return leafDomainName + "." + UNICODESTR + "." + baseDomainName();
    }
    
    private String SUB_DOMAIN_NAME(int index, int parentDomain) {
        int idx = index + 1;
        String parentDomainName = DOMAIN_NAME(parentDomain);
        return NAMEPREFIX_SUB_DOMAIN + idx + "." + parentDomainName;
    }
    
    private String LEAF_DOMAIN_NAME(int index) {
        if (index == OLD_DOMAIN)
            return NAME_LEAF_OLD_DOMAIN;
        else if (index == NEW_DOMAIN)
            return NAME_LEAF_NEW_DOMAIN;
        else
            return NAMEPREFIX_OTHERDOMAIN + index;
    }
        
    private String DOMAIN_NAME(int index) {
        return DOMAIN_NAME(LEAF_DOMAIN_NAME(index));
    }
    
    private String XMPPCOMPONENT_NAME(int index, String domainName) {
        return "xmppcomponent-" + index + "." + domainName;
    }
    
    private String ACCOUNT_LOCAL(int index) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        return NAMEPREFIX_ACCOUNT + String.valueOf(index+1);
    }
    
    private String ACCOUNT_NAME(int index, int domainIdx) {
        return ACCOUNT_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
    }
    
    
    
    /*
     * returns account name in domain and all its aliases
     */
    private Set<String> ACCOUNT_NAMES(int index, int domainIdx, boolean afterRename) {
        int dIdx = afterRename?DOMAIN_INDEX_AFTER_RENAME(domainIdx):domainIdx;
        Set<String> names = new HashSet<String>();
        String name = ACCOUNT_NAME(index, dIdx);
        names.add(name);
        for (int d = 0; d < NUM_DOMAINS; d++) {
            String aliasName = ACCOUNT_ALIAS_NAME(index, d, afterRename?DOMAIN_INDEX_AFTER_RENAME(d):d);
            names.add(aliasName);
        }
        return names;
    }
    
    private String ACCOUNT_ALIAS_NAME(int targetIdx, int targetDomainIdx, int aliasDomainIdx) {
        return NAMEPREFIX_ALIAS + ACCOUNT_LOCAL(targetIdx) + "-" + LEAF_DOMAIN_NAME(targetDomainIdx) + "@" + DOMAIN_NAME(aliasDomainIdx);
    }
    
    private String TOP_DL_LOCAL(int index) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        return NAMEPREFIX_DL_TOP + String.valueOf(index+1);
    }
    
    private String DYNAMIC_GROUP_LOCAL(int index) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        return NAMEPREFIX_DYNAMIC_GROUP + String.valueOf(index+1);
    }
    
    private String TOP_DL_NAME(int index, int domainIdx) {
        return TOP_DL_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
    }
    
    private String DYNAMIC_GROUP_NAME(int index, int domainIdx) {
        return DYNAMIC_GROUP_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
    }
    
    /*
     * returns nested DL name in domain and all its aliases
     */
    private Set<String> TOP_DL_NAMES(int index, int domainIdx, boolean afterRename) {
        int dIdx = afterRename?DOMAIN_INDEX_AFTER_RENAME(domainIdx):domainIdx;
        Set<String> names = new HashSet<String>();
        String name = TOP_DL_NAME(index, dIdx);
        names.add(name);
        for (int d = 0; d < NUM_DOMAINS; d++) {
            String aliasName = TOP_DL_ALIAS_NAME(index, d, afterRename?DOMAIN_INDEX_AFTER_RENAME(d):d);
            names.add(aliasName);
        }
        return names;
    }
    
    private String TOP_DL_ALIAS_NAME(int targetIdx, int targetDomainIdx, int aliasDomainIdx) {
        return NAMEPREFIX_ALIAS + TOP_DL_LOCAL(targetIdx) + "-" + LEAF_DOMAIN_NAME(targetDomainIdx) + "@" + DOMAIN_NAME(aliasDomainIdx);
    }
    
    private String DYNAMIC_GROUP_ALIAS_NAME(int targetIdx, int targetDomainIdx, int aliasDomainIdx) {
        return NAMEPREFIX_ALIAS + DYNAMIC_GROUP_LOCAL(targetIdx) + "-" + LEAF_DOMAIN_NAME(targetDomainIdx) + "@" + DOMAIN_NAME(aliasDomainIdx);
    }
    
    private String NESTED_DL_LOCAL(int index) {
        // we want our names to be 1 relative, easier to spot in ldap brawser
        return NAMEPREFIX_DL_NESTED + String.valueOf(index+1);
    }
    
    private String NESTED_DL_NAME(int index, int domainIdx) {
        return NESTED_DL_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
    }
    
    /*
     * returns nested DL name in domain and all its aliases
     */
    private Set<String> NESTED_DL_NAMES(int index, int domainIdx, boolean afterRename) {
        int dIdx = afterRename?DOMAIN_INDEX_AFTER_RENAME(domainIdx):domainIdx;
        Set<String> names = new HashSet<String>();
        String name = NESTED_DL_NAME(index, dIdx);
        names.add(name);
        for (int d = 0; d < NUM_DOMAINS; d++) {
            String aliasName = NESTED_DL_ALIAS_NAME(index, d, afterRename?DOMAIN_INDEX_AFTER_RENAME(d):d);
            names.add(aliasName);
        }
        return names;
    }
    
    private String NESTED_DL_ALIAS_NAME(int targetIdx, int targetDomainIdx, int aliasDomainIdx) {
        return NAMEPREFIX_ALIAS + NESTED_DL_LOCAL(targetIdx) + "-" + LEAF_DOMAIN_NAME(targetDomainIdx) + "@" + DOMAIN_NAME(aliasDomainIdx);
    }
    
    private String SIGNATURE_NAME(Account acct, int index) {
        int idx = index+1;
        return NAMEPREFIX_SIGNATURE + idx + "of-acct-" + acct.getName();
    }
    
    private String SIGNATURE_CONTENT(Account acct, int index) {
        int idx = index+1;
        return "signature content of " + NAMEPREFIX_SIGNATURE + idx + "of-acct-" + acct.getName();
    }
    
    private String IDENTITY_NAME(Account acct, int index) {
        int idx = index+1;
        return NAMEPREFIX_IDENTITY + idx + "of-acct-" + acct.getName();
    }
    
    private String DATASOURCE_NAME(Account acct, int index) {
        int idx = index+1;
        return NAMEPREFIX_DATASOURCE + idx + "of-acct-" + acct.getName();
    }
    
    private void createDomain(String domainName) throws Exception {
        System.out.println("createDomain: " + domainName);
        Map<String, Object> attrs = new HashMap<String, Object>();
        Domain domain = prov.createDomain(domainName, attrs);
    }
    
    private String[] createSignatures(Account acct) throws Exception {
        String[] sigIds = new String[NUM_SIGNATURES];
        
        for (int i = 0; i < NUM_SIGNATURES; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraPrefMailSignature, SIGNATURE_CONTENT(acct, i));
            Signature entry = prov.createSignature(acct, SIGNATURE_NAME(acct, i), attrs);
            sigIds[i] = entry.getId();
        }
        
        return sigIds;
    }
    
    private void createIdentities(Account acct, String[] sigIds) throws Exception {
        for (int i = 0; i < NUM_IDENTITIES; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, LdapUtil.generateUUID());  // just some random id, not used anywhere
            attrs.put(Provisioning.A_zimbraPrefFromAddress, "micky.mouse@zimbra,com");
            attrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
            attrs.put(Provisioning.A_zimbraPrefReplyToEnabled, "TRUE");
            attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
            attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");
            if (i < NUM_SIGNATURES)
                attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, sigIds[i]);
            Identity entry = prov.createIdentity(acct, IDENTITY_NAME(acct, i), attrs);
        }
    }
    
    private void createDataSources(Account acct, String[] sigIds) throws Exception {
        for (int i = 0; i < NUM_DATASOURCES; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
            attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
            attrs.put(Provisioning.A_zimbraDataSourceFolderId, "inbox");
            attrs.put(Provisioning.A_zimbraDataSourceHost, "pop.google.com");
            attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "TRUE");
            attrs.put(Provisioning.A_zimbraDataSourcePassword, PASSWORD);
            attrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
            attrs.put(Provisioning.A_zimbraDataSourceUsername, "mickymouse");
            attrs.put(Provisioning.A_zimbraDataSourceEmailAddress, "micky@google.com");
            attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, LdapUtil.generateUUID()); // just some random id, not used anywhere
            attrs.put(Provisioning.A_zimbraPrefFromDisplay, "Micky Mouse");
            attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "goofy@yahoo.com");
            attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Micky");
            if (i < NUM_SIGNATURES)
                attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, sigIds[i]);
            DataSource entry = prov.createDataSource(acct, DataSourceType.pop3, DATASOURCE_NAME(acct, i), attrs);
        }
    }
    
    
    private String makeAttrValue(String entryName, String attrName, Names.IDNName domainIDNName) {
        
        /*
         * currently only aName will pass the test in verifyEntryAttrs, should probably 
         * fix renameDomain to take care uName in these attrs
         */
        String domainName = domainIDNName.uName();
        
        if (Provisioning.A_zimbraMailCatchAllAddress.equals(attrName) ||
            Provisioning.A_zimbraMailCatchAllCanonicalAddress.equals(attrName) ||
            Provisioning.A_zimbraMailCatchAllForwardingAddress.equals(attrName)) {
            return "@" + domainName;
        } else if (Provisioning.A_zimbraPrefAllowAddressForDelegatedSender.equals(attrName)) {
            return entryName;
        } else {
            return attrName + "@" + domainName;
        }
    }
    
    private Account createAccount(String acctName, Names.IDNName domainName) throws Exception {
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        
        /*
        acctAttrs.put(Provisioning.A_zimbraMailCanonicalAddress, "canonical-address" + "@" + domainName.uName());
        // acctAttrs.put(Provisioning.A_zimbraMailDeliveryAddress, "delivery-address" + "@" + domainName.uName());
        acctAttrs.put(Provisioning.A_zimbraMailForwardingAddress, "forwarding-address" + "@" + domainName.uName());
        
        acctAttrs.put(Provisioning.A_zimbraMailCatchAllAddress, "" + "@" + domainName.uName());
        acctAttrs.put(Provisioning.A_zimbraMailCatchAllCanonicalAddress, "" + "@" + domainName.uName());
        acctAttrs.put(Provisioning.A_zimbraMailCatchAllForwardingAddress, "" + "@" + domainName.uName());
        */
        
        for (String attr : sAttrsToVerify) {
            acctAttrs.put(attr, makeAttrValue(acctName, attr, domainName));
        }
        Account acct = prov.createAccount(acctName, PASSWORD, acctAttrs);
        return acct;
    }
    
    private XMPPComponent createXMPPComponent(int xmppIndex, Domain domain) throws Exception {
        
        Server server = prov.getLocalServer();
        
        String routableName = XMPPCOMPONENT_NAME(xmppIndex, domain.getName());
        
        Map<String, Object> xmppAttrs = new HashMap<String, Object>();
        xmppAttrs.put(Provisioning.A_zimbraXMPPComponentClassName, "myclass");
        xmppAttrs.put(Provisioning.A_zimbraXMPPComponentCategory, "mycategory");
        xmppAttrs.put(Provisioning.A_zimbraXMPPComponentType, "mytype");
        
        XMPPComponent xmpp = prov.createXMPPComponent(routableName, domain, server, xmppAttrs);
        return xmpp;
    }
    
    /*
     * create and setup entries in the domain
     */
    private void populateDomain(int domainIdx) throws Exception {
        
        Names.IDNName domainName = new Names.IDNName(DOMAIN_NAME(domainIdx));
        System.out.println("setupDomain: " + domainName.uName());
        
        // create accounts and their aliases
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            Account acct = createAccount(ACCOUNT_NAME(a, domainIdx), domainName);
            
            for (int d = 0; d < NUM_DOMAINS; d++)
                prov.addAlias(acct, ACCOUNT_ALIAS_NAME(a, domainIdx, d));
            
            String[] signatureIds = createSignatures(acct);
            createIdentities(acct, signatureIds);
            createDataSources(acct, signatureIds);
        }
        
        // create nested dls and their aliases
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = prov.createDistributionList(NESTED_DL_NAME(nd, domainIdx), dlAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++) {
                prov.addAlias(dl, NESTED_DL_ALIAS_NAME(nd, domainIdx, d));
            }
        }
        
        // create top dls and their aliases
        for (int td = 0; td < NUM_DLS_TOP; td++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = prov.createDistributionList(TOP_DL_NAME(td, domainIdx), dlAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++) {
                prov.addAlias(dl, TOP_DL_ALIAS_NAME(td, domainIdx, d));
            }
        }
        
        // create dynamic groups and their aliases
        for (int dg = 0; dg < NUM_DYNAMIC_GROUPS; dg++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DynamicGroup dynGroup = prov.createDynamicGroup(DYNAMIC_GROUP_NAME(dg, domainIdx), dlAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++) {
                prov.addGroupAlias(dynGroup, DYNAMIC_GROUP_ALIAS_NAME(dg, domainIdx, d));
            }
        }
    }
    
    /*
     * add all accounts, nested Dls, and their aliases of the domain to all DLs of all domains
     */
    private void setupDLs(int domainIdx) throws Exception {
        
        String domainName = DOMAIN_NAME(domainIdx);
        System.out.println("crossLinkDomain: " + domainName);
        
        // Domain sourceDomain = mProv.get(Provisioning.DomainBy.name, domainName);
        
        List<String>[][] nestedDLMembers = new ArrayList[NUM_DOMAINS][NUM_DLS_NESTED];
        List<String>[][] topDLMembers = new ArrayList[NUM_DOMAINS][NUM_DLS_TOP];   
        List<String>[][] dynamicGroupMembers = new ArrayList[NUM_DOMAINS][NUM_DYNAMIC_GROUPS];      
        
        for (int d = 0; d < NUM_DOMAINS; d++) {
            for (int nd = 0; nd < NUM_DLS_TOP; nd++) {
                nestedDLMembers[d][nd] = new ArrayList();
            }
            for (int td = 0; td < NUM_DLS_TOP; td++) {
                topDLMembers[d][td] = new ArrayList();
            }
            for (int dd = 0; dd < NUM_DYNAMIC_GROUPS; dd++) {
                dynamicGroupMembers[d][dd] = new ArrayList();
            }
        }
        
        // add accounts and their aliases to top and nested dls, and dynamic groups
        // note: for dynamic groups, the net outcome is the memberOf attr of account 
        // entries is set to the dynamic group.
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            Set<String> members = ACCOUNT_NAMES(a, domainIdx, false);
           
            for (int d = 0; d < NUM_DOMAINS; d++) {
                for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
                    for (String m : members)
                        nestedDLMembers[d][nd].add(m);
                }
                for (int td = 0; td < NUM_DLS_TOP; td++) {
                    for (String m : members)
                        topDLMembers[d][td].add(m);
                }
                for (int dd = 0; dd < NUM_DYNAMIC_GROUPS; dd++) {
                    for (String m : members)
                        dynamicGroupMembers[d][dd].add(m);
                }
            }
        }
        
        // add nested dls and their aliases to top dls
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            Set<String> members = NESTED_DL_NAMES(nd, domainIdx, false);
            
            for (int d = 0; d < NUM_DOMAINS; d++) {
                for (int td = 0; td < NUM_DLS_TOP; td++) {
                    for (String m : members)
                        topDLMembers[d][td].add(m);
                }
            }
        }
        
        // now add them
        for (int d = 0; d < NUM_DOMAINS; d++) {
            for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
                DistributionList dl = prov.get(Key.DistributionListBy.name, NESTED_DL_NAME(nd, d));
                prov.addMembers(dl, nestedDLMembers[d][nd].toArray(new String[0]));
            }
            for (int td = 0; td < NUM_DLS_TOP; td++) {
                DistributionList dl = prov.get(Key.DistributionListBy.name, TOP_DL_NAME(td, d));
                prov.addMembers(dl, topDLMembers[d][td].toArray(new String[0]));
            }
            for (int dd = 0; dd < NUM_DYNAMIC_GROUPS; dd++) {
                Group dynGroup = prov.getGroup(Key.DistributionListBy.name, DYNAMIC_GROUP_NAME(dd, d));
                prov.addGroupMembers(dynGroup, dynamicGroupMembers[d][dd].toArray(new String[0]));
            }
        }
    }
    
    
    private void verifyOldDomain() throws Exception {
        String oldDomainName = DOMAIN_NAME(OLD_DOMAIN);
        Domain oldDomain = prov.get(Key.DomainBy.name, oldDomainName);
        assertTrue(oldDomain == null);
    }
    
    private void dumpAttrs(Map<String, Object> attrsIn, Set<String> specificAttrs) {
        
        System.out.println();
        
        TreeMap<String, Object> attrs = new TreeMap<String, Object>(attrsIn);

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            if (specificAttrs == null || specificAttrs.contains(name.toLowerCase())) {
                Object value = entry.getValue();
                if (value instanceof String[]) {
                    String sv[] = (String[]) value;
                    for (String aSv : sv) {
                        System.out.println(name + ": " + aSv);
                    }
                } else if (value instanceof String){
                    System.out.println(name+": "+value);
                }
            }
        }
        
        System.out.println();
    }
    
    private void dumpNames(String desc, List<NamedEntry> entries) {
        System.out.println();
        
        System.out.println("===== " + ((desc==null)?"":desc) + " =====");
        for (NamedEntry entry : entries)
            System.out.println(entry.getName());
        
        System.out.println();
    }
    
    private void dumpAttrs(Map<String, Object> attrsIn) {
        dumpAttrs(attrsIn, null);
    }
    
    private void dumpStrings(String notes, Collection<String> strings) {

        List<String> sorted = new ArrayList<String>(strings);
        Collections.sort(sorted);
        System.out.println();
        System.out.println(notes);
        
        for (String s : sorted)
            System.out.println(s);

        System.out.println();
    }
    
    private static enum UnicodeOrACE {
        ACE,
        UNICODE,
    };
    
    Set<String> namedEntryListToNameSet(List<NamedEntry> entries, UnicodeOrACE unicodeOrACE) {
        Set<String> nameSet = new HashSet<String>();
        for (NamedEntry entry : entries) {
            String name;
            if (entry instanceof MailTarget) {
                if (unicodeOrACE == UnicodeOrACE.UNICODE) {
                    name = ((MailTarget) entry).getUnicodeName();
                } else {
                    name = entry.getName();
                }
            } else {
                name = entry.getName();
            }
            nameSet.add(name);
        }
        return nameSet;     
    }
    
    private NamedEntry getEntryByName(int objType, String name) throws Exception {
        switch (objType) {
        case OBJ_ACCT:
            return prov.get(Key.AccountBy.name, name);
        case OBJ_DL_NESTED:
        case OBJ_DL_TOP:
        case OBJ_DYNAMIC_GROUP:
            return prov.getGroup(Key.DistributionListBy.name, name);
        }
        throw new Exception();
    }
    
    private void verifyDomainStatus(String domainName) throws Exception {
        
        Domain domain = prov.get(Key.DomainBy.name, domainName);
        assertTrue(domain != null);
        
        String domainStatus = domain.getAttr(Provisioning.A_zimbraDomainStatus);
        assertEquals("active", domainStatus);
        String mailStatus = domain.getAttr(Provisioning.A_zimbraMailStatus);
        assertEquals("enabled", mailStatus);
    }
    
    private Domain verifyNewDomainBasic(String domainId) throws Exception {
        // get by name
        Domain domainByName = prov.get(Key.DomainBy.name, DOMAIN_NAME(NEW_DOMAIN));
        assertTrue(domainByName != null);
        
        // get by id
        Domain domainById = prov.get(Key.DomainBy.id, domainId);
        assertTrue(domainById != null);
        
        Verify.verifySameEntry(domainByName, domainById);
        
        verifyDomainStatus(DOMAIN_NAME(NEW_DOMAIN));
        
        return domainById;
    }
    
    /*
     * verify all attrs of the old domain are carried over to the new domain
     */ 
    private void verifyNewDomainAttrs(Domain newDomain, Map<String, Object> oldDomainAttrs) 
    throws Exception {
        Map<String, Object> newDomainAttrs = newDomain.getAttrs(false);

        // make a copy of the two attrs maps, becase we are deleting from them
        Map<String, Object> oldAttrs = new HashMap<String, Object>(oldDomainAttrs);
        Map<String, Object> newAttrs = new HashMap<String, Object>(newDomainAttrs);
                
        // dumpAttrs(oldAttrs);
        // dumpAttrs(newAttrs);
        
        oldAttrs.remove(Provisioning.A_dc);
        oldAttrs.remove(Provisioning.A_o);
        oldAttrs.remove(Provisioning.A_zimbraDomainName);
        oldAttrs.remove(Provisioning.A_zimbraCreateTimestamp);

        newAttrs.remove(Provisioning.A_dc);
        newAttrs.remove(Provisioning.A_o);
        newAttrs.remove(Provisioning.A_zimbraDomainName);
        newAttrs.remove(Provisioning.A_zimbraCreateTimestamp);
        
        for (Map.Entry<String, Object> oldAttr : oldAttrs.entrySet()) {
            String oldKey = oldAttr.getKey();
            Object oldValue = oldAttr.getValue();
            
            Object newValue = newAttrs.get(oldKey);
            if (oldValue instanceof String[]) {
                assertTrue(newValue instanceof String[]);
                Set<String> oldV = new HashSet(Arrays.asList((String[])oldValue));
                Set<String> newV = new HashSet(Arrays.asList((String[])newValue));
                Verify.verifyEquals(oldV, newV);
                
            } else if (oldValue instanceof String) {
                try {
                    assertEquals(oldValue, newValue);
                } catch (AssertionFailedError e) {
                    System.out.println("Attribute " + " " + oldKey + " does not match!");
                    throw e;
                }
            }
        }
    }
    
    private void verifyEntryAttrs(List<NamedEntry> list, Domain domain) throws Exception {
        
        Names.IDNName domainIDNName = new Names.IDNName(domain.getName());
        
        for (NamedEntry e : list) {
            for (String attr : sAttrsToVerify) {
                String value = e.getAttr(attr);
                // System.out.println("Entry: " + e.getName() + " " + attr + " " + value);
                if (e instanceof Account) {
                    assertEquals(makeAttrValue(((Account) e).getName(), attr, domainIDNName), value);
                }
            }
        }
    }
    
    /*
     * verify all expected entries are in the domain and the domain only contains these entries
     */
    private void verifyEntries(int domainIdx, Domain domain) throws Exception {
     
        // get all the entries reside in the domain
        SearchDirectoryOptions options = new SearchDirectoryOptions();
        options.setTypes(SearchDirectoryOptions.ObjectType.accounts, 
                SearchDirectoryOptions.ObjectType.distributionlists,
                SearchDirectoryOptions.ObjectType.dynamicgroups);
        options.setDomain(domain);
        options.setFilterString(FilterId.UNITTEST, null);
        List<NamedEntry> list = prov.searchDirectory(options);
        
        // come up with all expected entries
        Set<String> expectedEntries = new HashSet<String>();
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            Names.IDNName name = new Names.IDNName(ACCOUNT_NAME(a, domainIdx));
            Account entry = prov.get(Key.AccountBy.name, name.uName());
            assertNotNull(entry);
            expectedEntries.add(name.uName());
        }
            
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            Names.IDNName name = new Names.IDNName(NESTED_DL_NAME(nd, domainIdx));
            DistributionList entry = prov.get(Key.DistributionListBy.name, name.uName());
            assertNotNull(entry);
            expectedEntries.add(name.uName());
        }
            
        for (int td = 0; td < NUM_DLS_TOP; td++){
            Names.IDNName name = new Names.IDNName(TOP_DL_NAME(td, domainIdx));
            DistributionList entry = prov.get(Key.DistributionListBy.name, name.uName());
            assertNotNull(entry);
            expectedEntries.add(name.uName());
        }
        
        for (int td = 0; td < NUM_DYNAMIC_GROUPS; td++){
            Names.IDNName name = new Names.IDNName(DYNAMIC_GROUP_NAME(td, domainIdx));
            DynamicGroup entry = (DynamicGroup) prov.getGroup(Key.DistributionListBy.name, name.uName());
            assertNotNull(entry);
            expectedEntries.add(name.uName());
        }
       
        // verify all our aliases are there
        Set<String> actualEntries = namedEntryListToNameSet(list, UnicodeOrACE.UNICODE);
        
        dumpStrings("expectedEntries", expectedEntries);
        dumpStrings("actualEntries", actualEntries);
        Verify.verifyEquals(expectedEntries, actualEntries);
        
        verifyEntryAttrs(list, domain);
    }
    
    /*
     * verify all aliases in the domain 
     */
    private void verifyDomainAliases(int domainIdx, Domain domain) throws Exception {
        
        // get all the aliases reside in the domain
        SearchDirectoryOptions options = new SearchDirectoryOptions();
        options.setTypes(SearchDirectoryOptions.ObjectType.aliases);
        options.setDomain(domain);
        options.setFilterString(FilterId.UNITTEST, null);
        List<NamedEntry> list = prov.searchDirectory(options);
        
        // come up with all expected aliases
        Set<String> expectedAliases = new HashSet<String>();
        for (int d = 0; d < NUM_DOMAINS; d++) {
            for (int a = 0; a < NUM_ACCOUNTS; a++)
                expectedAliases.add(new Names.IDNName(ACCOUNT_ALIAS_NAME(a, d, domainIdx)).uName());
            
            for (int nd = 0; nd < NUM_DLS_NESTED; nd++)
                expectedAliases.add(new Names.IDNName(NESTED_DL_ALIAS_NAME(nd, d, domainIdx)).uName());
            
            for (int td = 0; td < NUM_DLS_TOP; td++)
                expectedAliases.add(new Names.IDNName(TOP_DL_ALIAS_NAME(td, d, domainIdx)).uName());
            
            for (int dg = 0; dg < NUM_DYNAMIC_GROUPS; dg++)
                expectedAliases.add(new Names.IDNName(DYNAMIC_GROUP_ALIAS_NAME(dg, d, domainIdx)).uName());
        }
        
        
        // verify all our aliases are there
        Set<String> actualAliases = namedEntryListToNameSet(list, UnicodeOrACE.UNICODE);
        // dumpStrings(expectedAliases);
        // dumpStrings(actualAliases);
        Verify.verifyEquals(expectedAliases, actualAliases);
        
        // verify the target of each alias can be found
        for (NamedEntry entry : list) {
            assertTrue(entry instanceof Alias);
            
            NamedEntry target = prov.searchAliasTarget((Alias)entry, true);
            assertNotNull(target);
        }        
    }
    
    /*
     * verify aliases of entries in a domain 
     */
    private void verifyAliasesOfEntriesInDomain(int objType, Domain domain) throws Exception {
        /*
         * verify the account aliases
         */
        for (int i = 0; i < NUM_OBJS(objType); i++) {
            // get the the entry by main name
            String entryName = OBJ_NAME(objType, i, NEW_DOMAIN);
            NamedEntry entry = getEntryByName(objType, entryName);
            assertNotNull(entry);
            
            // get the entry by its aliases
            for (int d = 0; d < NUM_DOMAINS; d++) {
                int aliasDomainIdx = DOMAIN_INDEX_AFTER_RENAME(d);
                String aliasName = GET_ALIAS_NAME(objType, i, OLD_DOMAIN, aliasDomainIdx);
                NamedEntry entryByAlias = getEntryByName(objType, aliasName);
                assertNotNull(entryByAlias);
                Verify.verifySameEntry(entry, entryByAlias);
            }
        }
    }
    
    private void verifyAliases(int domainIdx, Domain domain) throws Exception {
        verifyDomainAliases(domainIdx, domain);
        
        verifyAliasesOfEntriesInDomain(OBJ_ACCT, domain);
        verifyAliasesOfEntriesInDomain(OBJ_DL_NESTED, domain);
        verifyAliasesOfEntriesInDomain(OBJ_DL_TOP, domain);
        verifyAliasesOfEntriesInDomain(OBJ_DYNAMIC_GROUP, domain);
    }

    private void verifyMemberOf(int memberType, int dlTypes, int domainIdx) throws Exception {
        for (int i = 0; i < NUM_OBJS(memberType); i++) {
            String name = OBJ_NAME(memberType, i, domainIdx);
            NamedEntry entry = getEntryByName(memberType, name);
            assertNotNull(entry);
            
            Set<String> expectedNames = new HashSet<String>();
            
            for (int d = 0; d < NUM_DOMAINS; d++) {
                int dIdx = DOMAIN_INDEX_AFTER_RENAME(d);
                if ((dlTypes & OBJ_DL_NESTED) != 0) {
                    for (int dlIdx = 0; dlIdx < NUM_DLS_NESTED; dlIdx++)
                        expectedNames.add(new Names.IDNName(NESTED_DL_NAME(dlIdx, dIdx)).aName());
                } 

                if ((dlTypes & OBJ_DL_TOP) != 0) {
                    for (int dlIdx = 0; dlIdx < NUM_DLS_TOP; dlIdx++)
                        expectedNames.add(new Names.IDNName(TOP_DL_NAME(dlIdx, dIdx)).aName());
                } 
                
                // todo: DYNAMIC GROUP
            }
            
            HashMap<String,String> via = new HashMap<String, String>();
            List lists;
            if (memberType == OBJ_ACCT) {
                lists = prov.getDistributionLists((Account)entry, false, via);
            } else {
                lists = prov.getDistributionLists((DistributionList)entry, false, via);
            }
            
            Set<String> actualNames = namedEntryListToNameSet(lists, UnicodeOrACE.ACE);
            // dumpStrings(expectedNames);
            // dumpStrings(actualNames);
            Verify.verifyEquals(expectedNames, actualNames);
        }
    }
    
    private void verifyHasMembers(int dlType, int memberTypes, int domainIdx) throws Exception {
        
        for (int dlIdx = 0; dlIdx < NUM_OBJS(dlType); dlIdx++) {
            String name =  OBJ_NAME(dlType, dlIdx, domainIdx);
            DistributionList dl = prov.get(Key.DistributionListBy.name, name);
            assertNotNull(dl);
            
            Set<String> expectedNames = new HashSet<String>();
            
            for (int d = 0; d < NUM_DOMAINS; d++) {
                int dIdx = DOMAIN_INDEX_AFTER_RENAME(d);
                
                if ((memberTypes & OBJ_ACCT) != 0) {
                    for (int i = 0; i < NUM_ACCOUNTS; i++) {
                        Set<String> names = ACCOUNT_NAMES(i, dIdx, true);
                        for (String n : names) {
                            expectedNames.add(new Names.IDNName(n).uName());
                        }
                    }
                } 
                
                if ((memberTypes & OBJ_DL_NESTED) != 0) {
                    for (int i = 0; i < NUM_DLS_NESTED; i++) {
                        Set<String> names = NESTED_DL_NAMES(i, dIdx, true);
                        for (String n : names) {
                            expectedNames.add(new Names.IDNName(n).uName());
                        }
                    }
                } 

                if ((memberTypes & OBJ_DL_TOP) != 0) {
                    for (int i = 0; i < NUM_DLS_TOP; i++) {
                        Set<String> names = TOP_DL_NAMES(i, dIdx, true);
                        for (String n : names) {
                            expectedNames.add(new Names.IDNName(n).uName());
                        }
                    }
                } 
                
                // TODO: DYNAMIC GROUP
            }
            
            // if we are verifying using SoapProvisioning, members contains unicode addrs,
            // because they are converted in SOAP handlers.
            // if we are verifying using LdapProvisioning, members contains ACE addrs, which 
            // are values stored in LDAP.  convert them to unicode for verifying.
            String[] members = dl.getAllMembers();
            Set<String> actualNames = new HashSet<String>(Arrays.asList(members));
            
            if (prov instanceof LdapProvisioning) {
                Set<String> actualNamesUnicode = Sets.newHashSet();
                for (String addr : actualNames) {
                    String addrUnicode = IDNUtil.toUnicode(addr);
                    actualNamesUnicode.add(addrUnicode);
                }
                actualNames = actualNamesUnicode;
            }
            // dumpStrings("expectedNames", expectedNames);
            // dumpStrings("actualNames", actualNames);
            Verify.verifyEquals(expectedNames, actualNames);
        }
    }
    
    
    private void verifyDLMembership(int domainIdx, Domain domain) throws Exception {
        // accounts
        verifyMemberOf(OBJ_ACCT, OBJ_DL_NESTED | OBJ_DL_TOP, domainIdx);
        
        // nested DLs
        verifyMemberOf(OBJ_DL_NESTED, OBJ_DL_TOP, domainIdx);
        verifyHasMembers(OBJ_DL_NESTED, OBJ_ACCT, domainIdx);
        
        // top DLs
        verifyHasMembers(OBJ_DL_TOP, OBJ_ACCT | OBJ_DL_NESTED, domainIdx);
        
        // dynamic groups
        // TODO
       
    }
    
    private void verifyDomain(int domainIdx) throws Exception {
        System.out.println("Verifying domain " + DOMAIN_NAME(domainIdx));
        
        String domainName = DOMAIN_NAME(domainIdx);
        Domain domain = prov.get(Key.DomainBy.name, domainName);
        
        verifyEntries(domainIdx, domain);
        verifyAliases(domainIdx, domain);
        verifyDLMembership(domainIdx, domain);
    }
    
    private void verifyNewDomain(String domainId,  Map<String, Object> oldDomainAttrs) throws Exception {
        Domain newDomain = verifyNewDomainBasic(domainId);
        verifyNewDomainAttrs(newDomain, oldDomainAttrs);
        verifyDomain(NEW_DOMAIN);
    }
    
    private void verifyOtherDomains() throws Exception {
        for (int d = 0; d < NUM_DOMAINS; d++) {
            if (d != OLD_DOMAIN)
                verifyDomain(d);
        }
    }
    
    private void verifyXMPPComponent(int index, Domain newDomain) throws Exception {
        String newRoutableName = XMPPCOMPONENT_NAME(index, newDomain.getName());
        
        XMPPComponent xmpp = prov.get(Key.XMPPComponentBy.name, newRoutableName);
        assertNotNull(xmpp);
        
        String domainId = newDomain.getId();
        String xmppDomainId = xmpp.getAttr(Provisioning.A_zimbraDomainId);
        assertEquals(domainId, xmppDomainId);
    }
    
    private void verifyXMPPComponents() throws Exception {
        
        Domain newDomain = prov.get(Key.DomainBy.name, DOMAIN_NAME(NEW_DOMAIN));
        
        for (int i = 0; i < NUM_XMPPCOMPONENTS; i++) {
            verifyXMPPComponent(i, newDomain);
        }
    }
    
    // TODO: 
    //  - stop the rename at different stages and test the restart
   

    private void renameDomainTest() throws Exception {
        
        prepareDomain();
        Domain oldDomain = prov.get(Key.DomainBy.name, DOMAIN_NAME(OLD_DOMAIN));
        String oldDomainId = oldDomain.getId();
        Map<String, Object> oldDomainAttrs = oldDomain.getAttrs(false);
        
        System.out.println("rd " + oldDomain.getId() + " " +  DOMAIN_NAME(NEW_DOMAIN));
        
        // rename
        ((LdapProv) prov).renameDomain(oldDomain.getId(), DOMAIN_NAME(NEW_DOMAIN));
        
        // verify
        // switch to SoapProvisioning, because the new domain is still cached under the wrong id
        // instead of the old domain's id in the LdapProvisioning instance, and verifyNewDomainBasic 
        // would fail if we use the LdapProvisioning instance or verifying.  We did not remove it 
        // from cache in RenameDomain.endRenameDomain, we could though...
        // setSoapProv();
        
        ((LdapProv) prov).flushCache(CacheEntryType.domain, null);
        ((LdapProv) prov).flushCache(CacheEntryType.account, null);
        ((LdapProv) prov).flushCache(CacheEntryType.group, null);
        
        verifyOldDomain();
        verifyNewDomain(oldDomainId, oldDomainAttrs);
        verifyOtherDomains(); 
        
        /*
         * A little testing hack here:
         * SOAP newDomain.getName() would return the unicode domain name.
         * But SOAP GetXMPPComponent currently has a bug that it doesn't first 
         * translates the name to ASCII before looking for it.  
         * 
         * (see verifyXMPPComponent)
         * 
         * The verify fails if domain name contains IDN.
         * 
         * To work around, use LdapProvisioning
         */
        setLdapProv();
        verifyXMPPComponents();
        
    }
    
    private void renameToExistingDomainTest() throws Exception {
        setLdapProv();
        String srcDomainName = DOMAIN_NAME("src");
        String tgtDomainName = DOMAIN_NAME("target");
        Domain srcDomain = prov.createDomain(srcDomainName, new HashMap<String, Object>());
        assertNotNull(srcDomain);
        Domain tgtDomain = prov.createDomain(tgtDomainName, new HashMap<String, Object>());
        assertNotNull(tgtDomain);
        
        boolean ok = false;
        try {
            ((LdapProv) prov).renameDomain(srcDomain.getId(), tgtDomainName);
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertEquals("invalid request: domain " + new Names.IDNName(tgtDomainName).aName() + " already exists", e.getMessage());
        }
        
        verifyDomainStatus(srcDomainName);
        verifyDomainStatus(tgtDomainName);
    }
    
    @Test
    public void testRenameDomain() throws Exception {
        try {
            System.out.println("\nTest " + TEST_ID + " starting\n");
            
            renameDomainTest();
            renameToExistingDomainTest();
            
            System.out.println("\nTest " + TEST_ID + " done!");
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            System.out.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                               (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
            e.printStackTrace(System.out);
            System.out.println("\nTest " + TEST_ID + " failed!");
        } catch (AssertionFailedError e) {
            System.out.println("\n===== assertion failed =====");
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
        }
    }
    
    /*
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        init();
        TestLdapProvRenameDomain test = new TestLdapProvRenameDomain();
        test.testRenameDomain();
    }
    */

}
