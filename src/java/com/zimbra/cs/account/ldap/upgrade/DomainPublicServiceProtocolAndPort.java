package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.MailMode;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProvisioning;

class DomainPublicServiceProtocolAndPort extends LdapUpgrade {
    
    DomainPublicServiceProtocolAndPort(boolean verbose) throws ServiceException {
        super(verbose);
    }
    
    private static String genQuery(List<Server> servers) {
        StringBuffer query = new StringBuffer();
        
        for (Server server : servers) {
            String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname);
            if (!StringUtil.isNullOrEmpty(serviceName))
                query.append("(" + Provisioning.A_zimbraPublicServiceHostname + "=" + serviceName + ")");
        }
        
        if (servers.size() > 1)
            return "(|" + query.toString() + ")";
        else
            return query.toString();
    }
    
    static class DomainPuclicServiceProtocolAndPortVisitor implements NamedEntry.Visitor {
        
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
        
        boolean mVerbose;
        LdapProvisioning mProv;
        Map<String, ServerInfo> mServerMap; // map keyed by server.zimbraServiceHostname
        int mDomainsVisited;
        
        DomainPuclicServiceProtocolAndPortVisitor(LdapProvisioning prov, List<Server> servers, boolean verbose) {
            mVerbose = verbose;
            mProv = prov;
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
            
            String domainPublicName = domain.getAttr(Provisioning.A_zimbraPublicServiceHostname);
            // should not be null/empty, otherwise the domain would not have been found, sanity check
            if (!StringUtil.isNullOrEmpty(domainPublicName)) {
                ServerInfo serverInfo = mServerMap.get(domainPublicName);
                // should not be null, otherwise the domain would not have been found, sanity check
                if (serverInfo != null) {
                    Map<String, Object> attrs = new HashMap<String, Object>(); 
                    // proto should not be null, sanity check
                    if (serverInfo.mProto != null)
                        attrs.put(Provisioning.A_zimbraPublicServiceProtocol, serverInfo.mProto);
                    if (serverInfo.mPort != null && !"0".equals(serverInfo.mPort))
                        attrs.put(Provisioning.A_zimbraPublicServicePort, serverInfo.mPort);
                    
                    try {
                        System.out.println("Updating domain " + domain.getName() + 
                                               ", proto => " + attrs.get(Provisioning.A_zimbraPublicServiceProtocol) + 
                                               ", port => " + attrs.get(Provisioning.A_zimbraPublicServicePort));
                        mProv.modifyAttrs(domain, attrs);
                    } catch (ServiceException e) {
                        System.out.println("Caught exception while modifying domain " + domain.getName());
                    }
                } else {
                    if (mVerbose)
                        System.out.println("Not updating domain " + domain.getName() + ", no matching server");
                }
            } else {
                if (mVerbose)
                    System.out.println("Not updating domain " + domain.getName() + ", domain does not have " + Provisioning.A_zimbraPublicServiceHostname);
            }
        }
        
        void reportStat() {
            System.out.println();
            System.out.println("Domains found = " + mDomainsVisited);
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
       
        String bases[] = mProv.getSearchBases(Provisioning.SA_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_zimbraPublicServiceHostname};
        
        DomainPuclicServiceProtocolAndPortVisitor visitor = new DomainPuclicServiceProtocolAndPortVisitor(mProv, servers, mVerbose); 
        
        for (String base : bases) {
            // should really have one base, but iterate thought the arrya anyway
            if (mVerbose) {
                System.out.println("LDAP search base: " + base);
                System.out.println("LDAP search query: " + query);
                System.out.println();
            }
            
            mProv.searchObjects(query, attrs, base,
                                Provisioning.SA_DOMAIN_FLAG, 
                                visitor, 
                                0,      // return all entries that satisfy filter.
                                false); // do not use connection pool, for the OpenLdap bug (see bug 24168) might still be there
         
        }
        
        visitor.reportStat();

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
    
    private static final int NUM_DOMAIN_SETS = 1;
    
    private void populateTestData() throws ServiceException {
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
        }
    }
    
    public static void main(String[] args) throws ServiceException {
        DomainPublicServiceProtocolAndPort upgrade = new DomainPublicServiceProtocolAndPort(true);
        upgrade.populateTestData();
    }
}
