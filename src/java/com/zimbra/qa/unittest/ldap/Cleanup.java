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
package com.zimbra.qa.unittest.ldap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;

public class Cleanup {
        
    static void deleteAll(String... domainNames) throws Exception {
        for (String domainName : domainNames) {
            deleteEntireBranch(domainName);
        }
        deleteAllNonDefaultCoses();
        deleteAllNonDefaultServers();
        deleteAllXMPPComponents();
    }
    
    /*
     * given a domain name like test.com, delete the entire tree under 
     * dc=com in LDAP
     */
    private static void deleteEntireBranch(String domainName) throws Exception {
        String parts[] = domainName.split("\\.");
        String[] dns = ((LdapProv) Provisioning.getInstance()).getDIT().domainToDNs(parts);
        String topMostRDN = dns[dns.length-1];
        deleteEntireBranchByDN(topMostRDN);
    }

    
    private static void deleteAllNonDefaultCoses() throws Exception {
        LdapDIT dit = ((LdapProv) Provisioning.getInstance()).getDIT();
        String cosBaseDN = dit.cosBaseDN();
        
        Set<String> defaultCosDN = new HashSet<String>();
        defaultCosDN.add(dit.cosNametoDN(Provisioning.DEFAULT_COS_NAME));
        defaultCosDN.add(dit.cosNametoDN(Provisioning.DEFAULT_EXTERNAL_COS_NAME));

        deleteAllChildrenUnderDN(cosBaseDN, defaultCosDN);
    }
    
    private static void deleteAllNonDefaultServers() throws Exception {
        LdapProv ldapProv = LdapProv.getInst();
        LdapDIT dit = ldapProv.getDIT();
        String serverBaseDN = dit.serverBaseDN();
        
        Set<String> defaultServerDN = new HashSet<String>();
        defaultServerDN.add(dit.serverNameToDN(ldapProv.getLocalServer().getName()));
        
        deleteAllChildrenUnderDN(serverBaseDN, defaultServerDN);
    }
    
    private static void deleteAllXMPPComponents() throws Exception {
        String xmppBaseDN = ((LdapProv) Provisioning.getInstance()).getDIT().xmppcomponentBaseDN();
        deleteAllChildrenUnderDN(xmppBaseDN, null);
    }

    
    // dn itself will also be deleted
    private static void deleteEntireBranchByDN(String dn) throws Exception {
        ZLdapContext zlc = null;
        
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
            deleteEntireBranch(zlc, dn);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    // dn itself will not be deleted
    private static void deleteAllChildrenUnderDN(String dn, Set<String> ignoreDNs) throws Exception {
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
            List<String> childrenDNs = getDirectChildrenDNs(zlc, dn);
            for (String childDN : childrenDNs) {
                if (ignoreDNs == null || !ignoreDNs.contains(childDN)) {
                    deleteEntireBranch(zlc, childDN);
                }
            }
        } finally {
            LdapClient.closeContext(zlc);
        }
        
    }
    
    private static void deleteEntireBranch(ZLdapContext zlc, String dn) throws Exception {
        
        if (isLeaf(zlc, dn)) {
            deleteEntry(dn);
            return;
        }
        
        List<String> childrenDNs = getDirectChildrenDNs(zlc, dn);
        for (String childDN : childrenDNs) {
            deleteEntireBranch(zlc, childDN);
        }
        deleteEntry(dn);
    }
    
    private static void deleteEntry(String dn) throws Exception {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
            zlc.deleteEntry(dn);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private static boolean isLeaf(ZLdapContext zlc, String dn) throws Exception {
        return getDirectChildrenDNs(zlc, dn).size() == 0;
    }
    
    private static List<String> getDirectChildrenDNs(ZLdapContext zlc, String dn) throws Exception {
        final List<String> childrenDNs = new ArrayList<String>();

        ZLdapFilter filter = ZLdapFilterFactory.getInstance().anyEntry();
        
        ZSearchControls searchControls = ZSearchControls.createSearchControls(
                ZSearchScope.SEARCH_SCOPE_ONELEVEL, 
                ZSearchControls.SIZE_UNLIMITED, new String[]{"objectClass"});
        
        ZSearchResultEnumeration sr = zlc.searchDir(dn, filter, searchControls);
        while (sr.hasMore()) {
            ZSearchResultEntry entry = sr.next();
            childrenDNs.add(entry.getDN());
        }
        sr.close();
        
        return childrenDNs;
    }
    
}
