package com.zimbra.cs.ldap.unboundid;

import java.util.ArrayList;
import java.util.List;
import javax.net.SocketFactory;

import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SingleServerSet;

import com.zimbra.cs.ldap.LdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapException;

/**
 * Represent a list of LDAP servers.  ZCS will attempt to establish 
 * connections in the order they are provided for failover. 
 * If the first server is unavailable, then it will attempt to connect 
 * to the second, then to the third, etc
 * 
 * @author pshao
 *
 */
public class LdapServerPool {
    List<LDAPURL> urls;
    LdapServerType serverType;  // do we need this?
    LdapConnType connType;
    LDAPConnectionOptions connOpts;
    
    ServerSet serverSet;

    /**
     * 
     * @param serverType
     * @param urls space separated urls
     */
    public LdapServerPool(String urls, LdapServerType serverType, 
            LdapConnType connType, LdapConfig config) throws LdapException {
        this.urls = new ArrayList<LDAPURL>();
        
        String[] ldapUrls = urls.split(" ");
        for (String ldapUrl : ldapUrls) {
            try {
                LDAPURL url = new LDAPURL(ldapUrl);
                this.urls.add(url);
            } catch (LDAPException e) {
                throw LdapException.INVALID_CONFIG(e);
            }
            
        }
        
        this.serverType = serverType;
        this.connType = connType;
        this.connOpts = LdapConnUtil.getConnectionOptions(config);
        
        SocketFactory socketFactory = 
            LdapConnUtil.getSocketFactory(connType, config.sslAllowUntrustedCerts());
        
        this.serverSet = createServerSet(socketFactory);
    }
    
    public List<LDAPURL> getUrls() {
        return urls;
    }
    
    public boolean isMaster() {
        return serverType.isMaster();
    }
    
    public LdapConnType getConnectionType() {
        return connType;
    }
    
    public ServerSet getServerSet() {
        return serverSet;
    }
    
    private ServerSet createServerSet(SocketFactory socketFactory){
        
        if (urls.size() == 1) {
            LDAPURL url = urls.get(0);
            if (socketFactory == null) {
                return new SingleServerSet(url.getHost(), url.getPort(), connOpts);
            } else {
                return new SingleServerSet(url.getHost(), url.getPort(), socketFactory, connOpts);
            }
        } else {
            int numUrls = urls.size();
            
            final String[] hosts = new String[numUrls];
            final int[]    ports = new int[numUrls];
            
            for (int i=0; i < numUrls; i++) {
                LDAPURL url = urls.get(i);
                hosts[i] = url.getHost();
                ports[i] = url.getPort();
            }
            
            if (socketFactory == null) {
                return new FailoverServerSet(hosts, ports, connOpts);
            } else {
                return new FailoverServerSet(hosts, ports, socketFactory, connOpts);
            }
        }
    }
}
