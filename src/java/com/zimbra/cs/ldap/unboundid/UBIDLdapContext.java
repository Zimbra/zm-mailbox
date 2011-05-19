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
package com.zimbra.cs.ldap.unboundid;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLContext;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.PostConnectProcessor;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.ldap.sdk.schema.Schema;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ldap.LdapServerConfig;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapServerConfig.ZimbraLdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapSchema;
import com.zimbra.cs.ldap.ZModificationList;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;

public class UBIDLdapContext extends ZLdapContext {
    
    private static boolean initialized = false;
    
    private static ZimbraLdapConfig replicaConfig;
    private static ZimbraLdapConfig masterConfig;
    
    private static LDAPConnectionPool replicaConnPool;
    private static LDAPConnectionPool masterConnPool;
    
    private LDAPConnectionPool connPool;
    private LDAPConnection conn;
    private boolean isZimbraLdap;
    private DereferencePolicy derefAliasPolicy;
    
    public static synchronized void init() throws LdapException {
        assert(!initialized);
        
        if (initialized) {
            return;
        }
        
        initialized = true;
        
        replicaConfig = new ZimbraLdapConfig(LdapServerType.REPLICA);
        masterConfig = new ZimbraLdapConfig(LdapServerType.MASTER);
        
        replicaConnPool = ConnectionPool.createConnectionPool(
                ConnectionPool.CP_ZIMBRA_REPLICA, replicaConfig);
        masterConnPool = ConnectionPool.createConnectionPool(
                ConnectionPool.CP_ZIMBRA_MASTER, masterConfig);
    }
    
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    // fo zmprov 
    static synchronized void alwaysUseMaster() {
        replicaConnPool = masterConnPool;
    }
    
    /*
     * Zimbra LDAP
     */
    public UBIDLdapContext(LdapServerType serverType) throws LdapException {
        isZimbraLdap = true;
        derefAliasPolicy = DereferencePolicy.NEVER;
        
        if (serverType.isMaster()) {
            connPool = masterConnPool;
        } else {
            connPool = replicaConnPool;
        }
        conn = getConnection(connPool);
    }
    
    /*
     * External LDAP
     */
    public UBIDLdapContext(ExternalLdapConfig config) throws LdapException {
        isZimbraLdap = false;
        setDerefAliasPolicy(config);
        
        connPool = ConnectionPool.getConnPoolByConfig(config);
        conn = getConnection(connPool);
    }

    // for unittest
    public LDAPConnection getNative() {
        return conn;
    }
    
    private LDAPConnection getConnection(LDAPConnectionPool pool) 
    throws LdapException {
        try {
            LDAPConnection conn = pool.getConnection();
            ConnectionPool.debugCheckOut(pool, conn);
            return conn;
        } catch (LDAPException e) {
            throw mapToLdapException("unable to get connection", e);
        }
    }
    
    public LDAPConnectionPool getConnectionPool() {
        String connPoolName = conn.getConnectionPoolName();
        return ConnectionPool.getConnPoolByName(connPoolName);
    }
    
    public String getConnectionName() {
        return conn.getConnectionName();
    }
    
    private LdapException mapToLdapException(String message, LDAPException e) {
        if (isZimbraLdap) {
            return UBIDLdapException.mapToLdapException(message, e);
        } else {
            // need more precise mapping for external LDAP exceptions so we
            // can report config error better
            return UBIDLdapException.mapToExternalLdapException(message, e);
        }
    }

    private void setDerefAliasPolicy(ExternalLdapConfig config) {
        
        String derefPolicy = config.getDerefAliasPolicy();

        if (derefPolicy == null) {
            derefAliasPolicy = DereferencePolicy.NEVER;
        } if ("always".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.ALWAYS;
        } else if ("never".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.NEVER;
        } else if ("finding".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.FINDING;
        } else if ("searching".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.SEARCHING;
        } else {
            ZimbraLog.ldap.warn("invalid deref alias policy: " + derefPolicy +
                    ", default to never");
            derefAliasPolicy = DereferencePolicy.NEVER;
        }
    }
  
    
    @Override
    public void closeContext() {
        
        ConnectionPool.debugCheckIn(connPool, conn);
        
        // NOTE: do NOT call conn.close() because it will unbind from the server 
        //       and close the connection and defunt the connection from the pool.
        //       Just release it to the pool.
        // 
        connPool.releaseConnection(conn);
        conn = null;
        // TODO check out this creature releaseConnectionAfterException
    }
    
    @Override
    public void createEntry(ZMutableEntry entry) throws ServiceException {
        try {
            conn.add(((UBIDMutableEntry) entry).getNative());
        } catch (LDAPException e) {
            throw mapToLdapException("unable to create entry", e);
        }
        
    }
    
    @Override
    public void createEntry(String dn, String objectClass, String[] attrs)
            throws ServiceException {
        UBIDMutableEntry entry = new UBIDMutableEntry();
        entry.setDN(dn);
        
        entry.setAttr(LdapConstants.ATTR_OBJECTCLASS, objectClass);
        
        for (int i=0; i < attrs.length; i += 2) {
            entry.setAttr(attrs[i], attrs[i+1]);
        }
        
        createEntry(entry);
    }

    @Override
    public void createEntry(String dn, String[] objectClasses, String[] attrs)
            throws ServiceException {
        UBIDMutableEntry entry = new UBIDMutableEntry();
        entry.setDN(dn);
        
        Set<String> ocs = new HashSet<String>(Arrays.asList(objectClasses));
        entry.addAttr(LdapConstants.ATTR_OBJECTCLASS, ocs);
        
        for (int i=0; i < attrs.length; i += 2) {
            entry.setAttr(attrs[i], attrs[i+1]);
        }
        
        createEntry(entry);
    }
    
    @Override
    public ZModificationList createModiftcationList() {
        return new UBIDModificationList();
    }

    @Override
    public void deleteChildren(String dn) throws ServiceException {
        
        try {
            SearchRequest searchRequest = new SearchRequest(dn, 
                    SearchScope.ONE,
                    derefAliasPolicy,
                    0,  // size limit
                    0,  // time limit
                    false, // getTypesOnly
                    Filter.createPresenceFilter(LdapConstants.ATTR_OBJECTCLASS)  // (objectclass=*)
                    ); 
                    
            searchRequest.setAttributes("dn");
            SearchResult result = conn.search(searchRequest);
            
            List<SearchResultEntry> entries = result.getSearchEntries();
            for (SearchResultEntry entry : entries) {
                deleteEntry(entry.getDN());
            }
        } catch (LDAPException e) {
            throw mapToLdapException("unable to delete children", e);
        }
    }
    
    @Override
    public ZAttributes getAttributes(String dn) throws LdapException {
        try {
            SearchResultEntry entry = conn.getEntry(dn);
            if (entry == null) {
                throw LdapException.ENTRY_NOT_FOUND("entry not found at " + dn, null);
            }
            return new UBIDAttributes(entry);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to get attributes", e);
        }
    }
    
    @Override
    public ZLdapSchema getSchema() throws LdapException {
        try {
            Schema schema = conn.getSchema();
            return new UBIDLdapSchema(schema);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to get schema", e);
        }
    }
    
    @Override
    @TODO
    public void modifyAttributes(String dn, ZModificationList modList) 
    throws LdapException {
        try {
            // TODO: need to check result? or can rely on the exception?
            LDAPResult result = conn.modify(dn, ((UBIDModificationList)modList).getModList());
        } catch (LDAPException e) {
            throw mapToLdapException("unable to modify attributes", e);
        }
    }
    
    @Override
    public void moveChildren(String oldDn, String newDn) throws ServiceException {
        try {
            SearchRequest searchRequest = new SearchRequest(oldDn, 
                    SearchScope.ONE,
                    derefAliasPolicy,
                    0,  // size limit
                    0,  // time limit
                    false, // getTypesOnly
                    Filter.createPresenceFilter(LdapConstants.ATTR_OBJECTCLASS)
                    ); 
                    
            searchRequest.setAttributes("dn");
            SearchResult result = conn.search(searchRequest);
            
            DN newDN = new DN(newDn);
            
            List<SearchResultEntry> entries = result.getSearchEntries();
            for (SearchResultEntry entry : entries) {
                DN entryDN = entry.getParsedDN();
                String childDn = entryDN.toNormalizedString();
                String childRdn = entryDN.getRDNString();
                conn.modifyDN(childDn, childRdn, true, newDn);
            }
        } catch (LDAPException e) {
            throw mapToLdapException("unable to move children", e);
        }
    }
    
    @Override
    public void renameEntry(String oldDn, String newDn) throws LdapException {
        try {
            DN newDN = new DN(newDn);
            String newRDN = newDN.getRDNString();
            String newSuperiorDN = newDN.getParentString();
            conn.modifyDN(oldDn, newRDN, true, newSuperiorDN);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to rename entry", e);
        }
        
    }
    
    @Override
    public void replaceAttributes(String dn, ZAttributes attrs) throws LdapException {
        Map<String, Object> attrMap = attrs.getAttrs();
        
        UBIDModificationList modList = new UBIDModificationList();
        modList.replaceAll(attrMap);
        modifyAttributes(dn, modList);
    }
    
    
    @Override
    public void searchPaged(SearchLdapOptions searchOptions) throws ServiceException {
        int maxResults = searchOptions.getMaxResults();
        String base = searchOptions.getSearchBase();
        String query = searchOptions.getFilter();
        Set<String> binaryAttrs = searchOptions.getBinaryAttrs();
        SearchScope searchScope = ((UBIDSearchScope) searchOptions.getSearchScope()).getNative();
        SearchLdapOptions.SearchLdapVisitor visitor = searchOptions.getVisitor();
        
        boolean wantPartialResult = true;  // TODO: this is the legacy behavior, we can make it a param
        
        try {
            SearchRequest searchRequest = new SearchRequest(base, 
                    searchScope,
                    derefAliasPolicy,
                    maxResults,
                    0,
                    false,
                    query);

            searchRequest.setAttributes(searchOptions.getReturnAttrs());
            
            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = searchOptions.getResultPageSize();
            ASN1OctetString cookie = null;
            
            do {
                searchRequest.setControls(
                        new Control[] { new SimplePagedResultsControl(pageSize, cookie) });
                
                SearchResult result = null;
                try {
                    result = conn.search(searchRequest);
                } catch (LDAPException e) {
                    if (ResultCode.SIZE_LIMIT_EXCEEDED == e.getResultCode() && wantPartialResult) {
                        // if callsite wants partial result, return them
                        LDAPResult ldapResult = e.toLDAPResult();
                        if (ldapResult instanceof SearchResult) {
                            SearchResult searchResult = (SearchResult) ldapResult;
                            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                                String dn = entry.getDN();
                                UBIDAttributes ubidAttrs = new UBIDAttributes(entry);
                                if (visitor.wantAttrMapOnVisit()) {
                                    visitor.visit(dn, ubidAttrs.getAttrs(binaryAttrs), ubidAttrs);
                                } else {
                                    visitor.visit(dn, ubidAttrs);
                                }
                            }
                        }
                    }
                    // always re-throw
                    throw e;
                }
                
                for (SearchResultEntry entry : result.getSearchEntries()) {
                    String dn = entry.getDN();
                    UBIDAttributes ubidAttrs = new UBIDAttributes(entry);
                    if (visitor.wantAttrMapOnVisit()) {
                        visitor.visit(dn, ubidAttrs.getAttrs(binaryAttrs), ubidAttrs);
                    } else {
                        visitor.visit(dn, ubidAttrs);
                    }
                }

                cookie = null;
                for (Control c : result.getResponseControls()) {
                    if (c instanceof SimplePagedResultsControl) {
                        cookie = ((SimplePagedResultsControl) c).getCookie();
                    }
                }
            } while ((cookie != null) && (cookie.getValueLength() > 0));
        } catch (SearchLdapOptions.StopIteratingException e) { 
            // break out of the loop and close the ne
        } catch (LDAPException e) {
            throw mapToLdapException("unable to search ldap", e);
        }
    }
    
    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, ZLdapFilter filter, 
            ZSearchControls searchControls) throws LdapException {
        
        UBIDSearchControls sc = (UBIDSearchControls)searchControls;
        
        try {
            SearchRequest searchRequest = new SearchRequest(baseDN, 
                    sc.getSearchScope(),
                    derefAliasPolicy,
                    sc.getSizeLimit(),
                    sc.getTimeLimit(),
                    sc.getTypesOnly(),
                    ((UBIDLdapFilter) filter).getNative());
            
            searchRequest.setAttributes(sc.getReturnAttrs());
            
            SearchResult result = conn.search(searchRequest);
            return new UBIDSearchResultEnumeration(result);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to search ldap", e);
        }
    }
    
    @Override
    public void deleteEntry(String dn) throws LdapException {
        try {
            conn.delete(dn);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to delete entry", e);
        }
    }

    /**
     * authenticate to LDAP server.
     * 
     * This is method is called for:
     *   - external LDAP auth
     *   - auth to ZImbra LDAP server when the stored password is not SSHA.
     *   
     * @param urls
     * @param wantStartTLS
     * @param bindDN
     * @param password
     * @param note
     * @throws ServiceException
     */
    private static void ldapAuthenticate(LdapServerConfig config, String bindDN, String password) 
    throws ServiceException {
        /*
         * About dereferencing alias.
         * 
         * The legacy JNDI implementation supports specifying deref 
         * alias policy during bind, via the "java.naming.ldap.derefAliases" 
         * DirContext env property.
         * 
         * Doesn't look like unboundid has an obvious way to specify 
         * deref alias policy during bind.
         * 
         * The LDAP protocol http://tools.ietf.org/html/rfc4511 disallows
         * LDAP server to deref alias during bind anyway.
         * 
         * section 4.2 
         * ..., it SHALL NOT perform alias dereferencing.
         * 
         * Therefore, we do *not* support dereferencing alias during bind anymore.
         * 
         */

                
        LdapServerPool serverPool = new LdapServerPool(config);
        LDAPConnection connection = null;
        
        try {
            connection = serverPool.getServerSet().getConnection();
            if (serverPool.getConnectionType() == LdapConnType.STARTTLS) {
                SSLContext startTLSContext = 
                    LdapSSLUtil.createSSLContext(config.sslAllowUntrustedCerts());
                ExtendedResult extendedResult = connection.processExtendedOperation(
                        new StartTLSExtendedRequest(startTLSContext));
                
                // NOTE:  
                // The processExtendedOperation method will only throw an exception
                // if a problem occurs while trying to send the request or read the
                // response.  It will not throw an exception because of a non-success
                // response.
                if (extendedResult.getResultCode() != ResultCode.SUCCESS) {
                    throw ServiceException.FAILURE(
                            "unable to send or receive startTLS extended operation", null);
                }
            }
            
            BindResult bindResult = connection.bind(bindDN, password);
            if (bindResult.getResultCode() != ResultCode.SUCCESS) {
                throw ServiceException.FAILURE("unable to bind", null);
            }
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToExternalLdapException("unable to ldap authenticate", e);    
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        
    }

    static void externalLdapAuthenticate(String[] urls, boolean wantStartTLS,
            String bindDN, String password, String note)
    throws ServiceException {
        ExternalLdapConfig config = new ExternalLdapConfig(urls, wantStartTLS,
                null, bindDN, password, null, note);
        ldapAuthenticate(config, bindDN, password);
    }
    
    static void zimbraLdapAuthenticate(String bindDN, String password) 
    throws ServiceException {
        ldapAuthenticate(replicaConfig, bindDN, password); 
    }
}
