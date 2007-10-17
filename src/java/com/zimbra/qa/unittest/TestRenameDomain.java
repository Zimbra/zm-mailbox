/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.qa.unittest.TestProvisioningUtil.IDNName;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestRenameDomain  extends TestCase {
    private Provisioning mProv;
    private String TEST_ID;
    
    private static String PASSWORD = "test123";
    
    private static int NUM_ACCOUNTS    = 3;
    private static int NUM_CAS         = 3;  // calendar resources, TODO
    private static int NUM_DLS_NESTED  = 2;
    private static int NUM_DLS_TOP     = 2;
    private static int NUM_DOMAINS     = 3;
    private static int NUM_SUB_DOMAINS = 2;  // number of sub domains under the old domain(domain to be renamed)
     
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
    private static String NAMEPREFIX_IDENTITY    = "identity-";
    private static String NAMEPREFIX_OTHERDOMAIN = "otherdomain-";
    private static String NAMEPREFIX_SIGNATURE   = "signature-";
    private static String NAMEPREFIX_SUB_DOMAIN  = "subdomian-";
    
    // pseudo domain index for the old domain and new domain, so that we can use the unified interfaces
    private static int OLD_DOMAIN  = 0;
    private static int NEW_DOMAIN  = -1;

    private static final int OBJ_ACCT = 0x1;
    private static final int OBJ_DL_NESTED = 0x2;
    private static final int OBJ_DL_TOP = 0x4;

    private static final Set<String> sAttrsToVerify;
    
    static {
        sAttrsToVerify = new HashSet<String>();
        
        sAttrsToVerify.add(Provisioning.A_zimbraMailCanonicalAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailForwardingAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailCatchAllAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailCatchAllCanonicalAddress);
        sAttrsToVerify.add(Provisioning.A_zimbraMailCatchAllForwardingAddress);
    }
    
    int NUM_OBJS(int objType) throws Exception {
        switch (objType) {
        case OBJ_ACCT:
            return TestRenameDomain.NUM_ACCOUNTS;
        case OBJ_DL_NESTED:
            return TestRenameDomain.NUM_DLS_NESTED;
        case OBJ_DL_TOP:
            return TestRenameDomain.NUM_DLS_TOP;         }
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
        }
        throw new Exception();
    }
    
    public void setUp() throws Exception {
        
        TEST_ID = TestProvisioningUtil.genTestId();
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        
        mProv = Provisioning.getInstance();
        assertTrue(mProv instanceof LdapProvisioning);
        
         
        /*
         * Create NUM_DOMAINS domains: one domain(the first one) to be renamed, and NUM_DOMAINS-1 other domains.  
         *     - Each domain:
         *           - has NUM_ACCOUNTS accounts
         *           - has NUM_DLS_TOP top level dls (dl that is not a nested DL)
         *           - has NUM_DLS_NESTED nested dls (dl under another DL)
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

    }
        
    private int DOMAIN_INDEX_AFTER_RENAME(int domainIdx) {
        if (domainIdx == OLD_DOMAIN)
            return NEW_DOMAIN;
        else
            return domainIdx;
    }
    
    private String DOMAIN_NAME(String leafDomainName) {
        return leafDomainName + "." + UNICODESTR + "." + TestProvisioningUtil.baseDomainName("renamedomain", TEST_ID);
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
    
    private String TOP_DL_NAME(int index, int domainIdx) {
        return TOP_DL_LOCAL(index) + "@" + DOMAIN_NAME(domainIdx);
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
        Domain domain = mProv.createDomain(domainName, attrs);
    }
    
    private String[] createSignatures(Account acct) throws Exception {
        String[] sigIds = new String[NUM_SIGNATURES];
        
        for (int i = 0; i < NUM_SIGNATURES; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraPrefMailSignature, SIGNATURE_CONTENT(acct, i));
            Signature entry = mProv.createSignature(acct, SIGNATURE_NAME(acct, i), attrs);
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
            Identity entry = mProv.createIdentity(acct, IDENTITY_NAME(acct, i), attrs);
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
            DataSource entry = mProv.createDataSource(acct, DataSource.Type.pop3, DATASOURCE_NAME(acct, i), attrs);
        }
    }
    
    private Account createAccount(String acctName, IDNName domainName) throws Exception {
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        
        acctAttrs.put(Provisioning.A_zimbraMailCanonicalAddress, "canonical-address" + "@" + domainName.uName());
        // acctAttrs.put(Provisioning.A_zimbraMailDeliveryAddress, "delivery-address" + "@" + domainName.uName());
        acctAttrs.put(Provisioning.A_zimbraMailForwardingAddress, "forwarding-address" + "@" + domainName.uName());
        
        acctAttrs.put(Provisioning.A_zimbraMailCatchAllAddress, "" + "@" + domainName.uName());
        acctAttrs.put(Provisioning.A_zimbraMailCatchAllCanonicalAddress, "" + "@" + domainName.uName());
        acctAttrs.put(Provisioning.A_zimbraMailCatchAllForwardingAddress, "" + "@" + domainName.uName());
        
        Account acct = mProv.createAccount(acctName, PASSWORD, acctAttrs);
        return acct;
    }
    
    /*
     * create and setup entries in the domain
     */
    private void populateDomain(int domainIdx) throws Exception {
        
        IDNName domainName = new IDNName(DOMAIN_NAME(domainIdx));
        System.out.println("setupDomain: " + domainName.uName());
        
        // create accounts and their aliases
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            Account acct = createAccount(ACCOUNT_NAME(a, domainIdx), domainName);
            
            for (int d = 0; d < NUM_DOMAINS; d++)
                mProv.addAlias(acct, ACCOUNT_ALIAS_NAME(a, domainIdx, d));
            
            String[] signatureIds = createSignatures(acct);
            createIdentities(acct, signatureIds);
            createDataSources(acct, signatureIds);
        }
        
        // create nested dls and their aliases
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList(NESTED_DL_NAME(nd, domainIdx), dlAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++)
                mProv.addAlias(dl, NESTED_DL_ALIAS_NAME(nd, domainIdx, d));
        }
        
        // create top dls and their aliases
        for (int td = 0; td < NUM_DLS_TOP; td++) {
            Map<String, Object> dlAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList(TOP_DL_NAME(td, domainIdx), dlAttrs);
            
            for (int d = 0; d < NUM_DOMAINS; d++)
                mProv.addAlias(dl, TOP_DL_ALIAS_NAME(td, domainIdx, d));
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
        
        for (int d = 0; d < NUM_DOMAINS; d++) {
            for (int nd = 0; nd < NUM_DLS_TOP; nd++) {
                nestedDLMembers[d][nd] = new ArrayList();
            }
            for (int td = 0; td < NUM_DLS_TOP; td++) {
                topDLMembers[d][td] = new ArrayList();
            }
        }
        
        // add accounts and their aliases to top and nested dls
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
                DistributionList dl = mProv.get(Provisioning.DistributionListBy.name, NESTED_DL_NAME(nd, d));
                mProv.addMembers(dl, nestedDLMembers[d][nd].toArray(new String[0]));
            }
            for (int td = 0; td < NUM_DLS_TOP; td++) {
                DistributionList dl = mProv.get(Provisioning.DistributionListBy.name, TOP_DL_NAME(td, d));
                mProv.addMembers(dl, topDLMembers[d][td].toArray(new String[0]));
            }
        }
    }
    
    
    private void verifyOldDomain() throws Exception {
        String oldDomainName = DOMAIN_NAME(OLD_DOMAIN);
        Domain oldDomain = mProv.get(Provisioning.DomainBy.name, oldDomainName);
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
    
    private void dumpStrings(Collection<String> strings) {

        List<String> sorted = new ArrayList<String>(strings);
        Collections.sort(sorted);
        System.out.println();
        
        for (String s : sorted)
            System.out.println(s);

        System.out.println();
    }
    
    Set<String> namedEntryListToNameSet(List<NamedEntry> entries) {
        Set<String> nameSet = new HashSet<String>();
        for (NamedEntry entry : entries)
            nameSet.add(entry.getName());
        return nameSet;     
    }
    
    private NamedEntry getEntryByName(int objType, String name) throws Exception {
        switch (objType) {
        case OBJ_ACCT:
            return mProv.get(Provisioning.AccountBy.name, name);
        case OBJ_DL_NESTED:
        case OBJ_DL_TOP:
            return mProv.get(Provisioning.DistributionListBy.name, name);
        }
        throw new Exception();
    }
    
    private Domain verifyNewDomainBasic(String domainId) throws Exception {
        // get by name
        Domain domainByName = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME(NEW_DOMAIN));
        assertTrue(domainByName != null);
        
        // get by id
        Domain domainById = mProv.get(Provisioning.DomainBy.id, domainId);
        assertTrue(domainById != null);
        
        TestProvisioningUtil.verifySameEntry(domainByName, domainById);
        
        return domainById;
    }
    
    /*
     * verify all attrs of the old domain are carried over to the new domain
     */ 
    private void verifyNewDomainAttrs(Domain newDomain, Map<String, Object> oldDomainAttrs) throws Exception {
        Map<String, Object> newDomainAttrs = newDomain.getAttrs(false);

        // make a copy of the two attrs maps, becase we are deleting from them
        Map<String, Object> oldAttrs = new HashMap<String, Object>(oldDomainAttrs);
        Map<String, Object> newAttrs = new HashMap<String, Object>(newDomainAttrs);
                
        // dumpAttrs(oldAttrs);
        // dumpAttrs(newAttrs);
        
        oldAttrs.remove(Provisioning.A_dc);
        oldAttrs.remove(Provisioning.A_o);
        oldAttrs.remove(Provisioning.A_zimbraDomainName);

        newAttrs.remove(Provisioning.A_dc);
        newAttrs.remove(Provisioning.A_o);
        newAttrs.remove(Provisioning.A_zimbraDomainName);
        
        for (Map.Entry<String, Object> oldAttr : oldAttrs.entrySet()) {
            String oldKey = oldAttr.getKey();
            Object oldValue = oldAttr.getValue();
            
            Object newValue = newAttrs.get(oldKey);
            if (oldValue instanceof String[]) {
                assertTrue(newValue instanceof String[]);
                Set<String> oldV = new HashSet(Arrays.asList((String[])oldValue));
                Set<String> newV = new HashSet(Arrays.asList((String[])newValue));
                TestProvisioningUtil.verifyEquals(oldV, newV);
                
            } else if (oldValue instanceof String){
                assertEquals(oldValue, newValue);
            }
        }
    }
    
    private void verifyEntryAttrs(List<NamedEntry> list) throws Exception {
        for (NamedEntry e : list) {
            for (String attr : sAttrsToVerify) {
                String value = e.getAttr(attr);
                if (value != null) {
                    // TODO
                }
            }
        }
    }
    
    /*
     * verify all expected entries are in the domain and the domain only contains these entries
     */
    private void verifyEntries(int domainIdx, Domain domain) throws Exception {
     
        // get all the entries reside in the domain
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();
        int flags = Provisioning.SA_ACCOUNT_FLAG | Provisioning.SA_DISTRIBUTION_LIST_FLAG;
        options.setFlags(flags);
        options.setDomain(domain);
        List<NamedEntry> list = mProv.searchDirectory(options);
        
        // come up with all expected entries
        Set<String> expectedEntries = new HashSet<String>();
        for (int a = 0; a < NUM_ACCOUNTS; a++) {
            IDNName name = new IDNName(ACCOUNT_NAME(a, domainIdx));
            Account entry = mProv.get(Provisioning.AccountBy.name, name.uName());
            assertNotNull(entry);
            expectedEntries.add(name.aName());
        }
            
        for (int nd = 0; nd < NUM_DLS_NESTED; nd++) {
            IDNName name = new IDNName(NESTED_DL_NAME(nd, domainIdx));
            DistributionList entry = mProv.get(Provisioning.DistributionListBy.name, name.uName());
            assertNotNull(entry);
            expectedEntries.add(name.aName());
        }
            
        for (int td = 0; td < NUM_DLS_NESTED; td++){
            IDNName name = new IDNName(TOP_DL_NAME(td, domainIdx));
            DistributionList entry = mProv.get(Provisioning.DistributionListBy.name, name.uName());
            assertNotNull(entry);
            expectedEntries.add(name.aName());
        }
       
        // verify all our aliases are there
        Set<String> actualEntries = namedEntryListToNameSet(list);
        
        // dumpStrings(expectedEntries);
        // dumpStrings(actualEntries);
        TestProvisioningUtil.verifyEquals(expectedEntries, actualEntries);
        
        verifyEntryAttrs(list);
    }
    
    /*
     * verify all aliases in the domain 
     */
    private void verifyDomainAliases(int domainIdx, Domain domain) throws Exception {
        
        // get all the aliases reside in the domain
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();
        int flags = Provisioning.SA_ALIAS_FLAG;
        options.setFlags(flags);
        options.setDomain(domain);
        List<NamedEntry> list = mProv.searchDirectory(options);
        
        // come up with all expected aliases
        Set<String> expectedAliases = new HashSet<String>();
        for (int d = 0; d < NUM_DOMAINS; d++) {
            for (int a = 0; a < NUM_ACCOUNTS; a++)
                expectedAliases.add(new IDNName(ACCOUNT_ALIAS_NAME(a, d, domainIdx)).aName());
            
            for (int nd = 0; nd < NUM_DLS_NESTED; nd++)
                expectedAliases.add(new IDNName(NESTED_DL_ALIAS_NAME(nd, d, domainIdx)).aName());
            
            for (int td = 0; td < NUM_DLS_NESTED; td++)
                expectedAliases.add(new IDNName(TOP_DL_ALIAS_NAME(td, d, domainIdx)).aName());
        }
        
        
        // verify all our aliases are there
        Set<String> actualAliases = namedEntryListToNameSet(list);
        // dumpStrings(expectedAliases);
        // dumpStrings(actualAliases);
        TestProvisioningUtil.verifyEquals(expectedAliases, actualAliases);
        
        // verify the target of each alias can be found
        for (NamedEntry entry : list) {
            assertTrue(entry instanceof Alias);
            
            NamedEntry target = ((Alias)entry).searchTarget(true);
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
                TestProvisioningUtil.verifySameEntry(entry, entryByAlias);
            }
        }
    }
    
    private void verifyAliases(int domainIdx, Domain domain) throws Exception {
        verifyDomainAliases(domainIdx, domain);
        
        verifyAliasesOfEntriesInDomain(OBJ_ACCT, domain);
        verifyAliasesOfEntriesInDomain(OBJ_DL_NESTED, domain);
        verifyAliasesOfEntriesInDomain(OBJ_DL_TOP, domain);
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
                        expectedNames.add(new IDNName(NESTED_DL_NAME(dlIdx, dIdx)).aName());
                } 

                if ((dlTypes & OBJ_DL_TOP) != 0) {
                    for (int dlIdx = 0; dlIdx < NUM_DLS_TOP; dlIdx++)
                        expectedNames.add(new IDNName(TOP_DL_NAME(dlIdx, dIdx)).aName());
                } 
            }
            
            HashMap<String,String> via = new HashMap<String, String>();
            List lists;
            if (memberType == OBJ_ACCT)
                lists = mProv.getDistributionLists((Account)entry, false, via);
            else
                lists = mProv.getDistributionLists((DistributionList)entry, false, via);
            
            Set<String> actualNames = namedEntryListToNameSet(lists);
            // dumpStrings(expectedNames);
            // dumpStrings(actualNames);
            TestProvisioningUtil.verifyEquals(expectedNames, actualNames);
        }
    }
    
    private void verifyHasMembers(int dlType, int memberTypes, int domainIdx) throws Exception {
        
        for (int dlIdx = 0; dlIdx < NUM_OBJS(dlType); dlIdx++) {
            String name =  OBJ_NAME(dlType, dlIdx, domainIdx);
            DistributionList dl = mProv.get(Provisioning.DistributionListBy.name, name);
            assertNotNull(dl);
            
            Set<String> expectedNames = new HashSet<String>();
            
            for (int d = 0; d < NUM_DOMAINS; d++) {
                int dIdx = DOMAIN_INDEX_AFTER_RENAME(d);
                
                if ((memberTypes & OBJ_ACCT) != 0) {
                    for (int i = 0; i < NUM_ACCOUNTS; i++) {
                        Set<String> names = ACCOUNT_NAMES(i, dIdx, true);
                        for (String n : names)
                            expectedNames.add(new IDNName(n).aName());
                    }
                } 
                
                if ((memberTypes & OBJ_DL_NESTED) != 0) {
                    for (int i = 0; i < NUM_DLS_NESTED; i++) {
                        Set<String> names = NESTED_DL_NAMES(i, dIdx, true);
                        for (String n : names)
                            expectedNames.add(new IDNName(n).aName());
                    }
                } 

                if ((memberTypes & OBJ_DL_TOP) != 0) {
                    for (int i = 0; i < NUM_DLS_TOP; i++) {
                        Set<String> names = TOP_DL_NAMES(i, dIdx, true);
                        for (String n : names)
                            expectedNames.add(new IDNName(n).aName());
                    }
                } 
            }
            
            String[] members = dl.getAllMembers();
            Set<String> actualNames = new HashSet<String>(Arrays.asList(members));
            // dumpStrings(expectedNames);
            // dumpStrings(actualNames);
            TestProvisioningUtil.verifyEquals(expectedNames, actualNames);
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
       
    }
    
    private void verifyDomain(int domainIdx) throws Exception {
        System.out.println("Verifying domain " + DOMAIN_NAME(domainIdx));
        
        String domainName = DOMAIN_NAME(domainIdx);
        Domain domain = mProv.get(Provisioning.DomainBy.name, domainName);
        
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
    
    // TODO: 
    //  - stop the rename at different stages and test the restart
   

    private void execute() throws Exception {
        
        // setup
        Domain oldDomain = mProv.get(Provisioning.DomainBy.name, DOMAIN_NAME(OLD_DOMAIN));
        String oldDomainId = oldDomain.getId();
        Map<String, Object> oldDomainAttrs = oldDomain.getAttrs(false);
        
        System.out.println("rd " + oldDomain.getId() + " " +  DOMAIN_NAME(NEW_DOMAIN));
        
        // rename
        ((LdapProvisioning)mProv).renameDomain(oldDomain.getId(), DOMAIN_NAME(NEW_DOMAIN));
        
        // verify
        verifyOldDomain();
        verifyNewDomain(oldDomainId, oldDomainAttrs);
        verifyOtherDomains();        
        
    }
    
    public void testRenameDomain() throws Exception {
        try {
            System.out.println("\nTest " + TEST_ID + " starting\n");
            execute();
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
   
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("DEBUG");
        
        TestUtil.runTest(new TestSuite(TestRenameDomain.class));
        
        /*
        TestRenameDomain t = new TestRenameDomain();
        t.setUp();
        */
    }
}
