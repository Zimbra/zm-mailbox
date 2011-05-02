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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.SocketFactory;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.DN;
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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.ldap.LdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZModificationList;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.jndi.JNDIAttributes;
import com.zimbra.cs.ldap.LdapServerType;

public class UBIDLdapContext extends ZLdapContext {
    
    private static boolean initialized = false;

    private static LdapConfig zimbraConfig;
    
    private static LdapServerPool replicaHosts;
    private static LdapServerPool masterHosts;
    
    private static LDAPConnectionPool replicaConnPool;
    private static LDAPConnectionPool masterConnPool;
    
    private LDAPConnectionPool connPool;
    private LDAPConnection conn;
    private boolean isZimbra;  // whether this context is zimbra LDAP
    private DereferencePolicy derefAliasPolicy;
    
    public static synchronized void init() throws LdapException {
        assert(!initialized);
        
        if (initialized) {
            return;
        }
        
        initialized = true;
        
        zimbraConfig = LdapConfig.loadZimbraConfig();
        
        LdapConnType replicaConnType = LdapConnType.getConnType(
                zimbraConfig.getReplicaURL(), zimbraConfig.wantStartTLS());
        LdapConnType masterConnType = LdapConnType.getConnType(
                zimbraConfig.getMasterURL(), zimbraConfig.wantStartTLS());
        
        replicaHosts = new LdapServerPool(zimbraConfig.getReplicaURL(), LdapServerType.REPLICA, 
                replicaConnType, zimbraConfig);
        masterHosts = new LdapServerPool(zimbraConfig.getMasterURL(), LdapServerType.MASTER, 
                masterConnType, zimbraConfig);
        
        replicaConnPool = ConnectionPool.createConnectionPool(
                ConnectionPool.CP_ZIMBRA_REPLICA, zimbraConfig, replicaHosts);
        masterConnPool = ConnectionPool.createConnectionPool(
                ConnectionPool.CP_ZIMBRA_MASTER, zimbraConfig, masterHosts);
    }
    
    public UBIDLdapContext(LdapServerType serverType) throws LdapException {
        connPool = getConnectionPool(serverType);
        conn = getConnection(connPool);
        setIsZimbra(true);
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    // for unittest
    public LDAPConnection getNative() {
        return conn;
    }

    private void setIsZimbra(boolean isZimbraInternal) {
        isZimbra = isZimbraInternal;
        
        if (isZimbra) {
            derefAliasPolicy = DereferencePolicy.NEVER;
        } else {
            String derefAliasPolity = zimbraConfig.getDerefAliasPolicy();

            if (derefAliasPolity == null) {
                derefAliasPolicy = DereferencePolicy.NEVER;
            } if ("always".equalsIgnoreCase(derefAliasPolity)) {
                derefAliasPolicy = DereferencePolicy.ALWAYS;
            } else if ("never".equalsIgnoreCase(derefAliasPolity)) {
                derefAliasPolicy = DereferencePolicy.NEVER;
            } else if ("finding".equalsIgnoreCase(derefAliasPolity)) {
                derefAliasPolicy = DereferencePolicy.FINDING;
            } else if ("searching".equalsIgnoreCase(derefAliasPolity)) {
                derefAliasPolicy = DereferencePolicy.SEARCHING;
            } else {
                ZimbraLog.ldap.warn("invalid deref alias policy: " + derefAliasPolity +
                        ", default to never");
                derefAliasPolicy = DereferencePolicy.NEVER;
            }
        }
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
            throw UBIDLdapException.mapToLdapException(e);
        }
    }
    
    public LDAPConnectionPool getConnectionPool() {
        String connPoolName = conn.getConnectionPoolName();
        return ConnectionPool.getConnPoolByName(connPoolName);
    }
    
    public String getConnectionName() {
        return conn.getConnectionName();
    }
  
    
    @Override
    public void closeContext() {
        // NOTE: do NOT call conn.close() because it will unbind from the server 
        //       and close the connection and defunt the connection from the pool.
        //       Just release it to the pool.
        // 
        connPool.releaseConnection(conn);
        conn = null;
        // TODO check out this creature releaseConnectionAfterException
    }
    
    @Override
    @TODOEXCEPTIONMAPPING
    public void createEntry(ZMutableEntry entry) throws ServiceException {
        try {
            conn.add(((UBIDMutableEntry) entry).getNative());
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
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
                unbindEntry(entry.getDN());
            }
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
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
            throw UBIDLdapException.mapToLdapException(e);
        }
    }
    
    @Override
    @TODO
    public void modifyAttributes(String dn, ZModificationList modList) throws LdapException {
        try {
            // TODO: need to check result? or can rely on the exception?
            LDAPResult result = conn.modify(dn, ((UBIDModificationList)modList).getModList());
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
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
            throw UBIDLdapException.mapToLdapException(e);
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
            throw UBIDLdapException.mapToLdapException("unable to rename entry", e);
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
    @TODO  // figure out how to throw TOO_MANY_SEARCH_RESULTS (the equivalent of JNDI SizeLimitExceededException)
    public void searchPaged(SearchLdapOptions searchOptions) throws ServiceException {
        int maxResults = 0; // no limit
        String base = searchOptions.getSearchBase();
        String query = searchOptions.getQuery();
        Set<String> binaryAttrs = searchOptions.getBinaryAttrs();
        SearchLdapOptions.SearchLdapVisitor visitor = searchOptions.getVisitor();
        
        try {
            SearchRequest searchRequest = new SearchRequest(base, 
                    SearchScope.SUB,
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
            throw UBIDLdapException.mapToLdapException("unable to search ldap", e);
        }
    }
    
    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, String filter, 
            ZSearchControls searchControls) throws LdapException {
        
        UBIDSearchControls sc = (UBIDSearchControls)searchControls;
        
        try {
            SearchRequest searchRequest = new SearchRequest(baseDN, 
                    sc.getSearchScope(),
                    derefAliasPolicy,
                    sc.getSizeLimit(),
                    sc.getTimeLimit(),
                    sc.getTypesOnly(),
                    filter);
            
            searchRequest.setAttributes(sc.getReturnAttrs());
            SearchResult result = conn.search(searchRequest);
            
            return new UBIDSearchResultEnumeration(result);
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
        }
    }
    
    @Override
    @TODO // must throw LdapContextNotEmptyException
    public void unbindEntry(String dn) throws LdapException {
        try {
            conn.delete(dn);
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
        }
    }

    
}
