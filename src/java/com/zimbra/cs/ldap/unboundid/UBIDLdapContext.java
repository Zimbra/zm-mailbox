package com.zimbra.cs.ldap.unboundid;

import java.io.IOException;
import java.util.Set;

import javax.net.SocketFactory;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.PostConnectProcessor;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;


import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil.SearchLdapVisitor;
import com.zimbra.cs.ldap.LdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZModificationList;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.jndi.JNDIAttributes;
import com.zimbra.cs.ldap.LdapServerType;

public class UBIDLdapContext extends ZLdapContext {

    private static LdapConfig zimbraConfig;
    
    private static LdapHost replicaHosts;
    private static LdapHost masterHosts;
    
    private static LDAPConnectionPool replicaConnPool;
    private static LDAPConnectionPool masterConnPool;
    
    private LDAPConnectionPool connPool;
    private LDAPConnection conn;
    
    public static synchronized void init() throws LdapException {
        zimbraConfig = LdapConfig.loadZimbraConfig();
        
        LdapConnType replicaConnType = LdapConnType.getConnType(
                zimbraConfig.getReplicaURL(), zimbraConfig.wantStartTLS());
        LdapConnType masterConnType = LdapConnType.getConnType(
                zimbraConfig.getMasterURL(), zimbraConfig.wantStartTLS());
        
        SocketFactory replicaSocketFactory = LdapConnUtils.getSocketFactory(replicaConnType, zimbraConfig.sslAllowUntrustedCerts());
        SocketFactory masterSocketFactory = LdapConnUtils.getSocketFactory(masterConnType, zimbraConfig.sslAllowUntrustedCerts());
        
        replicaHosts = new LdapHost(zimbraConfig.getReplicaURL(), LdapServerType.REPLICA, replicaConnType, replicaSocketFactory);
        masterHosts = new LdapHost(zimbraConfig.getMasterURL(), LdapServerType.MASTER, masterConnType, masterSocketFactory);
        
        try {
            replicaConnPool = LdapConnUtils.createConnectionPool(zimbraConfig, replicaHosts);
            masterConnPool = LdapConnUtils.createConnectionPool(zimbraConfig, masterHosts);
        } catch (LDAPException e) {
            throw LdapException.LDAP_ERROR(e);
        }
    }
    
    public UBIDLdapContext(LdapServerType serverType) throws LdapException {
        connPool = getConnectionPool(serverType);
        conn = getConnection(connPool);
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }

    private LDAPConnectionPool getConnectionPool(LdapServerType serverType) {
        if (serverType.isMaster()) {
            return masterConnPool;
        } else {
            return replicaConnPool;
        }
    }
    
    private LDAPConnection getConnection(LDAPConnectionPool pool) throws LdapException {
        try {
            return pool.getConnection();
        } catch (LDAPException e) {
            throw LdapException.LDAP_ERROR(e);
        }
    }
    
    @TODO
    private LdapException mapToLdapException(LDAPException e) {
        return LdapException.LDAP_ERROR(e);
    }
    
    
    @Override
    public void closeContext() {
        // NOTE: do NOT call conn.close() because it will unbind from the server 
        //       and close the connection and defunt the connection from the pool.
        //       Just release it to the pool.
        // 
        connPool.releaseConnection(conn);
        // TODO check out this creature releaseConnectionAfterException
    }
    
    @Override
    public ZModificationList createModiftcationList() {
        return new UBIDModificationList();
    }

    @Override
    public void deleteChildren(String dn) throws ServiceException {
        LdapTODO.TODO();
    }
    
    @Override
    public ZAttributes getAttributes(String dn) throws LdapException {
        try {
            SearchResultEntry entry = conn.getEntry(dn);
            return new UBIDAttributes(entry);
        } catch (LDAPException e) {
            throw mapToLdapException(e);
        }
    }
    
    @Override
    @TODO
    public void modifyAttributes(String dn, ZModificationList modList) throws LdapException {
        try {
            // TODO: need to check result? or can rely on the exception?
            LDAPResult result = conn.modify(dn, ((UBIDModificationList)modList).getModList());
        } catch (LDAPException e) {
            throw mapToLdapException(e);
        }
    }
    
    @Override
    public void search(SearchLdapOptions searchOptions) throws LdapException {
        int maxResults = 0; // no limit
        String base = searchOptions.getSearchBase();
        String query = searchOptions.getQuery();
        Set<String> binaryAttrs = searchOptions.getBinaryAttrs();
        SearchLdapVisitor visitor = searchOptions.getVisitor();
        
        try {
            SearchRequest searchRequest = new SearchRequest(base, 
                    SearchScope.SUB,
                    DereferencePolicy.NEVER,
                    maxResults,
                    0,
                    false,
                    query);


            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = searchOptions.getResultPageSize();
            ASN1OctetString cookie = null;
            
            do {
                searchRequest.setControls(
                        new Control[] { new SimplePagedResultsControl(pageSize, cookie) });
                SearchResult result = conn.search(searchRequest);

                for (SearchResultEntry entry : result.getSearchEntries()) {
                    String dn = entry.getDN();
                    UBIDAttributes ubidAttrs = new UBIDAttributes(entry);
                    visitor.visit(dn, ubidAttrs.getAttrs(binaryAttrs), ubidAttrs);
                }

                cookie = null;
                for (Control c : result.getResponseControls()) {
                    if (c instanceof SimplePagedResultsControl) {
                        cookie = ((SimplePagedResultsControl) c).getCookie();
                    }
                }
            } while ((cookie != null) && (cookie.getValueLength() > 0));
            
        } catch (LDAPException e) {
            throw LdapException.LDAP_ERROR("unable to search ldap", e);
        }
    }
    
    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, String filter, 
            ZSearchControls searchControls) throws LdapException {
        
        UBIDSearchControls sc = (UBIDSearchControls)searchControls;
        
        try {
            SearchRequest searchRequest = new SearchRequest(baseDN, 
                    sc.getSearchScope(),
                    sc.getDerefPolicy(),
                    sc.getSizeLimit(),
                    sc.getTimeLimit(),
                    sc.getTypesOnly(),
                    filter);
            
            searchRequest.setAttributes(sc.getReturnAttrs());
            SearchResult result = conn.search(searchRequest);
            
            return new UBIDSearchResultEnumeration(result);
        } catch (LDAPException e) {
            throw mapToLdapException(e);
        }
    }
    
    @Override
    public void unbindEntry(String dn) throws LdapException {
        LdapTODO.TODO();
    }




    
}
