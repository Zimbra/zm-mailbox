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
package com.zimbra.cs.account.ldap.upgrade.legacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.MailMode;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapProvisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;

class DomainPublicServiceProtocolAndPort extends LegacyLdapUpgrade {
    
    DomainPublicServiceProtocolAndPort() throws ServiceException {
    }
    
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
    
    static class DomainPuclicServiceProtocolAndPortVisitor extends LegacyLdapUpgrade.UpgradeVisitor implements NamedEntry.Visitor {
        
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
        
        Map<String, ServerInfo> mServerMap; // map keyed by server.zimbraServiceHostname
        int mDomainsVisited;
        
        DomainPuclicServiceProtocolAndPortVisitor(LdapProv prov, LegacyZimbraLdapContext zlcForMod, 
                List<Server> servers, boolean verbose) {
            super(prov, zlcForMod, verbose);
            
            mServerMap = new HashMap<String, ServerInfo>();
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
                    ServerInfo sInfo = mServerMap.get(serviceName);
                    if (sInfo != null)
                        System.out.println("Found duplicated " + Provisioning.A_zimbraServiceHostname +
                                           ", server honored:" + sInfo.mServerName +
                                           ", server ignored:" + server.getName());
                    else {
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
                            mServerMap.put(server.getAttr(Provisioning.A_zimbraServiceHostname), 
                                           new ServerInfo(server.getName(), proto, port));
                    }
                }
            }
        }
        
        public void visit(NamedEntry entry) {
            if (!(entry instanceof Domain)) {
                // should not happen
                System.out.println("Encountered non domain object: " + entry.getName() + ", skipping");
                return;
            }
            
            mDomainsVisited++;
            
            Domain domain = (Domain)entry;
            
            String domainPublicHostname = domain.getAttr(Provisioning.A_zimbraPublicServiceHostname);
            String domainPublicProtocol = domain.getAttr(Provisioning.A_zimbraPublicServiceProtocol);
            String domainPublicPort = domain.getAttr(Provisioning.A_zimbraPublicServicePort);
            
            // should not be null/empty, otherwise the domain would not have been found, sanity check
            if (StringUtil.isNullOrEmpty(domainPublicHostname)) {
                if (mVerbose)
                    System.out.format("Not updating domain %d: domain does not have %s\n",
                                       domain.getName(), Provisioning.A_zimbraPublicServiceHostname);
                return;
            }
            
            // update only if either protocol or port is already set
            if (domainPublicProtocol != null) {
                if (mVerbose)
                    System.out.format("Not updating domain %s: %s already set to %s on domain\n", 
                                      domain.getName(), Provisioning.A_zimbraPublicServiceProtocol, domainPublicProtocol);
                return;
            }
            
            if (domainPublicPort != null) {
                if (mVerbose)
                    System.out.format("Not updating domain %s: %s already set to %s on domain\n", 
                                      domain.getName(), Provisioning.A_zimbraPublicServicePort, domainPublicPort);
                return;
            }
                
            
            ServerInfo serverInfo = mServerMap.get(domainPublicHostname);
            // should not be null, otherwise the domain would not have been found, sanity check
            if (serverInfo == null) {
                if (mVerbose)
                    System.out.format("Not updating domain %s\n", domain.getName());
                return;
            }
            
            Map<String, Object> attrs = new HashMap<String, Object>(); 
            // proto should not be null, sanity check
            if (serverInfo.mProto != null)
                attrs.put(Provisioning.A_zimbraPublicServiceProtocol, serverInfo.mProto);
            if (serverInfo.mPort != null && !"0".equals(serverInfo.mPort))
                attrs.put(Provisioning.A_zimbraPublicServicePort, serverInfo.mPort);
                    
            try {
                System.out.format("Updating domain %-30s: proto => %-5s   port => %-5s\n",
                                  domain.getName(),
                                  attrs.get(Provisioning.A_zimbraPublicServiceProtocol),
                                  attrs.get(Provisioning.A_zimbraPublicServicePort));
                LegacyLdapUpgrade.modifyAttrs(domain, mZlcForMod, attrs);
            } catch (ServiceException e) {
                // log the exception and continue
                System.out.println("Caught ServiceException while modifying domain " + domain.getName());
                e.printStackTrace();
            } catch (NamingException e) {
                // log the exception and continue
                System.out.println("Caught NamingException while modifying domain " + domain.getName());
                e.printStackTrace();
            }
        }
        
        void reportStat() {
            System.out.println();
            System.out.println("Number of domains found = " + mDomainsVisited);
            System.out.println();
        }
    }
    
    /**
     * bug 29978
     * 
     * for each domain, if domain has zimbraPublicServiceHostname, and that zPSH has a 
     * corresponding zimbraServer, then set public service port/protocol on domain from 
     * that zimbraServer.
     * 
     * @throws ServiceException
     */
    void doUpgrade() throws ServiceException {
        
        List<Server> servers = mProv.getAllServers();
        
        String query = genQuery(servers);
       
        String bases[] = mProv.getDIT().getSearchBases(Provisioning.SD_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_objectClass,
                                       Provisioning.A_zimbraId,
                                       Provisioning.A_zimbraDomainName,
                                       Provisioning.A_zimbraPublicServiceHostname,
                                       Provisioning.A_zimbraPublicServiceProtocol,
                                       Provisioning.A_zimbraPublicServicePort};
        
        LegacyZimbraLdapContext zlc = null; 
        DomainPuclicServiceProtocolAndPortVisitor visitor = null;
        try {
            zlc = new LegacyZimbraLdapContext(true);
            
            visitor = new DomainPuclicServiceProtocolAndPortVisitor(mProv, zlc, servers, mVerbose);
            
            for (String base : bases) {
                // should really have one base, but iterate thought the arrya anyway
                if (mVerbose) {
                    System.out.println("LDAP search base: " + base);
                    System.out.println("LDAP search query: " + query);
                    System.out.println();
                }
                
                ((LegacyLdapProvisioning) mProv).searchObjects(query, attrs, base,
                                    Provisioning.SO_NO_FIXUP_OBJECTCLASS | Provisioning.SO_NO_FIXUP_RETURNATTRS, // turn off fixup for objectclass and return attrs
                                    visitor, 
                                    0,      // return all entries that satisfy filter.
                                    false,  // do not use connection pool, for the OpenLdap bug (see bug 24168) might still be there
                                    true);  // use LDAP master
             
            }
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
            if (visitor != null)
                visitor.reportStat();
        }
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
        
        System.out.println("Creating server " + serverName);
        return mProv.createServer(serverName, attrs);
    }
    
    private Domain createTestDomain(String domainName, String publicServiceHostname) throws ServiceException {
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        if (publicServiceHostname != null)
            attrs.put(Provisioning.A_zimbraPublicServiceHostname, publicServiceHostname);
        
        System.out.println("Creating domain " + domainName);
        return mProv.createDomain(domainName, attrs);
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
        
        System.out.println("Creating domain " + domainName);
        return mProv.createDomain(domainName, attrs);
    }
    
    private static final int NUM_DOMAIN_SETS = 1;
    
    private void populateTestData() throws ServiceException {
        
        for (Server s : mProv.getAllServers()) {
            if (!s.getName().equals("phoebe.mac")) {
                try {
                    mProv.deleteServer(s.getId());
                    System.out.println("Deleted server " + s.getName());
                } catch (ServiceException e) {
                    // ignore
                }
            }
        }
        
        System.out.println();
        
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
        
        System.out.println("\ndone");
    }
    
    public static void main(String[] args) throws ServiceException {
        DomainPublicServiceProtocolAndPort upgrade = new DomainPublicServiceProtocolAndPort();
        upgrade.setVerbose(true);
        upgrade.populateTestData();
    }
}
