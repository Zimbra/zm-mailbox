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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.Provisioning.MailMode;
import com.zimbra.cs.account.ldap.entry.LdapDomain;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchScope;

public class BUG_29978 extends UpgradeOp {

    private static String genQuery(List<Server> servers) {
        StringBuffer q = new StringBuffer();
        
        int numValues = 0;
        
        for (Server server : servers) {
            String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname);
            if (!StringUtil.isNullOrEmpty(serviceName)) {
                q.append("(" + Provisioning.A_zimbraPublicServiceHostname + "=" + serviceName + ")");
                numValues++;
            }
        }
        
        String query;
        
        if (numValues > 1)
            query = "(|" + q.toString() + ")";
        else
            query = q.toString();
        
        // zimbraPublicServiceHostname is not indexed, put objectClass=zimbraDomain in the front, objectClass is indexed.
        return "(&(objectClass=zimbraDomain)" + query + ")";
    }
        
    private static class Bug29978Visitor extends SearchLdapOptions.SearchLdapVisitor {
        
        class ServerInfo {
            String mServerName;
            String mProto;
            String mPort;
            
            ServerInfo(String serverName, String proto, String port) {
                mServerName = serverName;
                mProto = proto;
                mPort = port;
            }
        }
        
        private UpgradeOp upgradeOp;
        private ZLdapContext modZlc;
        private Map<String, ServerInfo> serverMap; // map keyed by server.zimbraServiceHostname
        private int domainsVisited;
        
        Bug29978Visitor(UpgradeOp upgradeOp, ZLdapContext modZlc, List<Server> servers) {
            super(false);
            this.upgradeOp = upgradeOp;
            this.modZlc = modZlc;
            
            serverMap = new HashMap<String, ServerInfo>();
            for (Server server : servers) {
                /*
                 * it should have zimbraServiceHostname, otherwise it should not be found,
                 * but sanity check to be on the safe side.  This is upgrade, not production.
                 * We should be liberal.
                 */
                String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname);
                if (!StringUtil.isNullOrEmpty(serviceName)) {
                    /*
                     * check if there is another server has the same service name,
                     * if so only the first server returned by the LDAP search is honored
                     * and all others are ignored.
                     */
                    ServerInfo sInfo = serverMap.get(serviceName);
                    if (sInfo != null) {
                        upgradeOp.printer.println("Found duplicated " + Provisioning.A_zimbraServiceHostname +
                                           ", server honored:" + sInfo.mServerName +
                                           ", server ignored:" + server.getName());
                    } else {
                        String mailModeStr = server.getAttr(Provisioning.A_zimbraMailMode);
                        MailMode mailMode = null;
                        if (mailModeStr == null) {
                            // server does not have mail mode, ignore it
                            continue;
                        } else {
                            try {
                                mailMode = MailMode.fromString(mailModeStr);
                            } catch (ServiceException e) {
                                // server has an invalid mail mode, ignore it
                                continue;
                            }
                        }
                        
                        String proto = null; 
                        String port = null;
                        if (mailMode == MailMode.http) {
                            proto = "http";
                            port = server.getAttr(Provisioning.A_zimbraMailPort);
                        } else if (mailMode == MailMode.https){
                            proto = "https";
                            port = server.getAttr(Provisioning.A_zimbraMailSSLPort);
                        } else {
                            // don't try to select a mode otherwise
                        }
                        
                        if (proto != null)
                            serverMap.put(server.getAttr(Provisioning.A_zimbraServiceHostname), 
                                           new ServerInfo(server.getName(), proto, port));
                    }
                }
            }
        }
        
        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            Domain domain;
            try {
                domain = new LdapDomain(dn, (ZAttributes)ldapAttrs, 
                        upgradeOp.prov.getConfig().getDomainDefaults(), upgradeOp.prov);
                visit(domain);
            } catch (ServiceException e) {
                upgradeOp.printer.println("entry skipped, encountered error while processing entry at:" + dn);
                upgradeOp.printer.printStackTrace(e);
            }
            
        }
        
        private void visit(Domain domain) {
            
            domainsVisited++;
            
            String domainPublicHostname = domain.getAttr(Provisioning.A_zimbraPublicServiceHostname);
            String domainPublicProtocol = domain.getAttr(Provisioning.A_zimbraPublicServiceProtocol);
            String domainPublicPort = domain.getAttr(Provisioning.A_zimbraPublicServicePort);
            
            // should not be null/empty, otherwise the domain would not have been found, sanity check
            if (StringUtil.isNullOrEmpty(domainPublicHostname)) {
                if (upgradeOp.verbose) {
                    upgradeOp.printer.format("Not updating domain %d: domain does not have %s\n",
                                       domain.getName(), Provisioning.A_zimbraPublicServiceHostname);
                }
                return;
            }
            
            // update only if either protocol or port is already set
            if (domainPublicProtocol != null) {
                if (upgradeOp.verbose) {
                    upgradeOp.printer.format("Not updating domain %s: %s already set to %s on domain\n", 
                                      domain.getName(), Provisioning.A_zimbraPublicServiceProtocol, domainPublicProtocol);
                }
                return;
            }
            
            if (domainPublicPort != null) {
                if (upgradeOp.verbose) {
                    upgradeOp.printer.format("Not updating domain %s: %s already set to %s on domain\n", 
                                      domain.getName(), Provisioning.A_zimbraPublicServicePort, domainPublicPort);
                }
                return;
            }
                
            
            ServerInfo serverInfo = serverMap.get(domainPublicHostname);
            // should not be null, otherwise the domain would not have been found, sanity check
            if (serverInfo == null) {
                if (upgradeOp.verbose) {
                    upgradeOp.printer.format("Not updating domain %s\n", domain.getName());
                }
                return;
            }
            
            Map<String, Object> attrs = new HashMap<String, Object>(); 
            // proto should not be null, sanity check
            if (serverInfo.mProto != null)
                attrs.put(Provisioning.A_zimbraPublicServiceProtocol, serverInfo.mProto);
            if (serverInfo.mPort != null && !"0".equals(serverInfo.mPort))
                attrs.put(Provisioning.A_zimbraPublicServicePort, serverInfo.mPort);
                    
            try {
                upgradeOp.printer.format("Updating domain %-30s: proto => %-5s   port => %-5s\n",
                                  domain.getName(),
                                  attrs.get(Provisioning.A_zimbraPublicServiceProtocol),
                                  attrs.get(Provisioning.A_zimbraPublicServicePort));
                upgradeOp.modifyAttrs(modZlc, domain, attrs);
            } catch (ServiceException e) {
                // log the exception and continue
                upgradeOp.printer.println("Caught ServiceException while modifying domain " + domain.getName());
                upgradeOp.printer.printStackTrace(e);
            }
        }
        
        void reportStat() {
            upgradeOp.printer.println();
            upgradeOp.printer.println("Number of domains found = " + domainsVisited);
            upgradeOp.printer.println();
        }
    }
    
    /**
     * for each domain, if domain has zimbraPublicServiceHostname, and that zPSH has a 
     * corresponding zimbraServer, then set public service port/protocol on domain from 
     * that zimbraServer.
     */
    @Override
    void doUpgrade() throws ServiceException {
        
        List<Server> servers = prov.getAllServers();
        
        String query = genQuery(servers);
       
        String bases[] = prov.getDIT().getSearchBases(Provisioning.SD_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_objectClass,
                                       Provisioning.A_zimbraId,
                                       Provisioning.A_zimbraDomainName,
                                       Provisioning.A_zimbraPublicServiceHostname,
                                       Provisioning.A_zimbraPublicServiceProtocol,
                                       Provisioning.A_zimbraPublicServicePort};
        
        ZLdapContext zlc = null; 
        Bug29978Visitor visitor = new Bug29978Visitor(this, zlc, servers);
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
            
            for (String base : bases) {
                // should really have one base, but iterate thought the arrya anyway
                if (verbose) {
                    printer.println("LDAP search base: " + base);
                    printer.println("LDAP search query: " + query);
                    printer.println();
                }

                SearchLdapOptions searchOpts = new SearchLdapOptions(base, getFilter(query), 
                        attrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                        ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);
                
                zlc.searchPaged(searchOpts);
            }
        } finally {
            LdapClient.closeContext(zlc);
            visitor.reportStat();
        }
    }
    
    @Override
    Description getDescription() {
        return new Description(
                this, 
                new String[] {Provisioning.A_zimbraPublicServiceProtocol, Provisioning.A_zimbraPublicServicePort}, 
                new EntryType[] {EntryType.DOMAIN},
                null, 
                String.format("value of %s and %s on the matching server",
                        Provisioning.A_zimbraPublicServiceProtocol, 
                        Provisioning.A_zimbraPublicServicePort),  
                "For each domain, if domain has zimbraPublicServiceHostname, " +
                "and that zPSH has a corresponding zimbraServer, then set " + 
                "public service port/protocol on domain from that zimbraServer.");
    }
    
    /* =============
     *   testers
     * =============
     */
    private Server createTestServer(String serverName, String mailMode, String mailPort, String mailSSLPort) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        attrs.put(Provisioning.A_zimbraServiceHostname, serverName + "-serviceName");
        if (mailMode != null)
            attrs.put(Provisioning.A_zimbraMailMode, mailMode);
        
        if (mailPort != null)
            attrs.put(Provisioning.A_zimbraMailPort, mailPort);
        
        if (mailSSLPort != null)
            attrs.put(Provisioning.A_zimbraMailSSLPort, mailSSLPort);
        
        printer.println("Creating server " + serverName);
        return prov.createServer(serverName, attrs);
    }
    
    private Domain createTestDomain(String domainName, String publicServiceHostname) throws ServiceException {
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        if (publicServiceHostname != null)
            attrs.put(Provisioning.A_zimbraPublicServiceHostname, publicServiceHostname);
        
        printer.println("Creating domain " + domainName);
        return prov.createDomain(domainName, attrs);
    }
    
    /**
     * for testing protocol and/or port is already present scenarios
     */
    private Domain createTestDomain(String domainName, 
                                    String publicServiceHostname, 
                                    String publicServiceProtocol,
                                    String publicServicePort) throws ServiceException {
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        if (publicServiceHostname != null)
            attrs.put(Provisioning.A_zimbraPublicServiceHostname, publicServiceHostname);
        if (publicServiceProtocol != null)
            attrs.put(Provisioning.A_zimbraPublicServiceProtocol, publicServiceProtocol);
        if (publicServicePort != null)
            attrs.put(Provisioning.A_zimbraPublicServicePort, publicServicePort);
        
        printer.println("Creating domain " + domainName);
        return prov.createDomain(domainName, attrs);
    }
    
    private static final int NUM_DOMAIN_SETS = 10;
    
    private void populateTestData() throws ServiceException {
        
        for (Server s : prov.getAllServers()) {
            if (!s.getName().equals("phoebe.mac")) {
                try {
                    prov.deleteServer(s.getId());
                    printer.println("Deleted server " + s.getName());
                } catch (ServiceException e) {
                    // ignore
                }
            }
        }
        
        printer.println();
        
        Server http            = createTestServer("http.server",             MailMode.http.toString(),     "1000", "1001"); 
        Server http_portis0    = createTestServer("http_portis0.server",     MailMode.http.toString(),     "0",    "1001"); 
        Server http_noport     = createTestServer("http_noport.server",      MailMode.http.toString(),     null,   "1001"); 
        
        Server https           = createTestServer("https.server",            MailMode.https.toString(),    "2000", "2001"); 
        Server https_portis0   = createTestServer("https_portis0.server",    MailMode.https.toString(),    "2000", "0");
        Server https_noport    = createTestServer("https_noport.server",     MailMode.https.toString(),    "2000", null);
        
        Server mixed           = createTestServer("mixed.server",            MailMode.mixed.toString(),    "3000", "3001"); 
        Server both            = createTestServer("both.server",             MailMode.both.toString(),     "4000", "4001"); 
        Server redirect        = createTestServer("redirect.server",         MailMode.redirect.toString(), "5000", "5001");
        // Server bogus           = createTestServer("bogus.server",            "bogus",                      "6000", "6001");
        Server nomailmode      = createTestServer("nomailmode.server",       null,                         "7000", "7001");
        
        for (int i = 0; i < NUM_DOMAIN_SETS;  i++) {
            createTestDomain("http_" + i + ".test",         http.getAttr(Provisioning.A_zimbraServiceHostname)); 
            createTestDomain("http_portis0_" + i + ".test", http_portis0.getAttr(Provisioning.A_zimbraServiceHostname)); 
            createTestDomain("http_noport_" + i + ".test",  http_noport.getAttr(Provisioning.A_zimbraServiceHostname)); 
            
            createTestDomain("https_" + i + ".test",         https.getAttr(Provisioning.A_zimbraServiceHostname)); 
            createTestDomain("https_portis0_" + i + ".test", https_portis0.getAttr(Provisioning.A_zimbraServiceHostname)); 
            createTestDomain("https_noport_" + i + ".test",  https_noport.getAttr(Provisioning.A_zimbraServiceHostname));
            
            createTestDomain("mixed_" + i + ".test",         mixed.getAttr(Provisioning.A_zimbraServiceHostname)); 
            createTestDomain("both_" + i + ".test",          both.getAttr(Provisioning.A_zimbraServiceHostname)); 
            createTestDomain("redirect_" + i + ".test",      redirect.getAttr(Provisioning.A_zimbraServiceHostname)); 
            // createTestDomain("bogus_" + i + ".test",         bogus.getAttr(Provisioning.A_zimbraServiceHostname)); 
            createTestDomain("nomailmode_" + i + ".test",    nomailmode.getAttr(Provisioning.A_zimbraServiceHostname)); 
            
            // domain's zimbraPublicServiceHostname does not match any server, domain should NOT be found by the search
            createTestDomain("notmatch" + i + ".test",       "notmatch");
            
            // domain without zimbraPublicServiceHostname, domain should NOT be found by the search
            createTestDomain("no_PSH" + i + ".test",         null);
            
            // domain matching a server but already has protocol present, it should not be updated
            createTestDomain("proto_present_" + i + ".test",   http.getAttr(Provisioning.A_zimbraServiceHostname), "https", null);

            // domain matching a server but already has port present, it should not be updated
            createTestDomain("port_present_" + i + ".test",   http.getAttr(Provisioning.A_zimbraServiceHostname), null, "8888");

        }
        
        printer.println("\ndone");
    }
    
    public static void main(String[] args) throws Exception {
        BUG_29978 op = (BUG_29978)LdapUpgrade.getUpgradeOpUnitTest("29978");
         
        // op.describe();
        // op.populateTestData();
        
        op.doUpgrade();
    }

}
