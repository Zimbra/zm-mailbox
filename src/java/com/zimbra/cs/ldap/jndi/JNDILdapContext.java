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
package com.zimbra.cs.ldap.jndi;

import java.io.IOException;
import java.util.Set;

import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapException.LdapInvalidNameException;
import com.zimbra.cs.ldap.LdapException.LdapEntryAlreadyExistException;
import com.zimbra.cs.ldap.LdapException.LdapEntryNotFoundException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapSchema;
import com.zimbra.cs.ldap.ZModificationList;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;


/**
 * Wraps a legacy ZimbraLdapContext
 *
 */
public class JNDILdapContext extends ZLdapContext {
    
    private LegacyZimbraLdapContext zlc;
    
    public static synchronized void init(boolean alwaysUseMaster) {
        if (alwaysUseMaster) {
            LegacyZimbraLdapContext.forceMasterURL();
        }
    }
    
    public JNDILdapContext(LdapServerType serverType, LdapUsage usage) 
    throws ServiceException {
        super(usage);
        zlc = new LegacyZimbraLdapContext(serverType.isMaster());
    }
    
    public JNDILdapContext(LdapServerType serverType, boolean useConnPool, LdapUsage usage) 
    throws ServiceException {
        super(usage);
        zlc = new LegacyZimbraLdapContext(serverType.isMaster(), useConnPool);
    }
    
    @TODO  // need to somehow expose  NamingException and IOException for check external auth/gal config
    public JNDILdapContext(ExternalLdapConfig config, LdapUsage usage)
    throws ServiceException {
        super(usage);
        try {
            zlc = new LegacyZimbraLdapContext(config.getLdapURL(), config.getWantStartTLS(), 
                    config.getAuthMech(), config.getAdminBindDN(), config.getAdminBindPassword(),
                    config.getBinaryAttrs(), config.getNotes());
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        } catch (IOException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void closeContext() {
        LegacyZimbraLdapContext.closeContext(zlc);
    }
    

    @Override
    @TODO // what to do with the method param to zlc.createEntry? see impl in zlc.createEntry, log the methods somewhere wlse?
    public void createEntry(ZMutableEntry entry) throws ServiceException {
        try {
            zlc.createEntry(entry.getDN(), ((JNDIMutableEntry)entry).getNativeAttributes(), "");
        } catch (NameAlreadyBoundException e) {
            throw JNDILdapException.mapToLdapException(e);
        } catch (ServiceException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }

    @Override
    public void createEntry(String dn, String objectClass, String[] attrs)
            throws ServiceException {
        try {
            zlc.simpleCreate(dn, objectClass, attrs);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }

    @Override
    public void createEntry(String dn, String[] objectClasses, String[] attrs)
            throws ServiceException {
        try {
            zlc.simpleCreate(dn, objectClasses, attrs);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override
    public ZModificationList createModificationList() {
        return new JNDIModificationList();
    }
    
    @Override
    public void deleteChildren(String dn) throws ServiceException {
        zlc.deleteChildren(dn);
    }
    
    @Override
    @TODO
    public ZAttributes getAttributes(String dn, String[] attrs) throws LdapException {
        if (attrs != null) {
            LdapTODO.TODO(); 
        }
        try {
            Attributes attributes = zlc.getAttributes(dn);
            return new JNDIAttributes(attributes);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override
    public ZLdapSchema getSchema() throws LdapException {
        try {
            DirContext schema = zlc.getSchema();
            return new JNDILdapSchema(schema);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override
    public void modifyAttributes(String dn, ZModificationList modList) throws LdapException {
        try {
            zlc.modifyAttributes(dn, ((JNDIModificationList)modList).getModListAsArray());
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override
    public boolean testAndModifyAttributes(String dn,
            ZModificationList modList, ZLdapFilter testFilter) throws LdapException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveChildren(String oldDn, String newDn)
            throws ServiceException {
        zlc.moveChildren(oldDn, newDn);
    }
    
    @Override
    public void renameEntry(String oldDn, String newDn) throws LdapException {
        try {
            zlc.renameEntry(oldDn, newDn);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
        
    }
    
    @Override
    public void replaceAttributes(String dn, ZAttributes attrs) throws LdapException {
        try {
            zlc.replaceAttributes(dn, ((JNDIAttributes) attrs).getNative());
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override
    public void searchPaged(SearchLdapOptions searchOptions) throws ServiceException {
        int maxResults = searchOptions.getMaxResults();
        String base = searchOptions.getSearchBase();
        String query = searchOptions.getFilter().toFilterString();
        Set<String> binaryAttrs = searchOptions.getBinaryAttrs();
        int searchScope = ((JNDISearchScope) searchOptions.getSearchScope()).getNative();
        SearchLdapOptions.SearchLdapVisitor visitor = searchOptions.getVisitor();
        
        try {
            SearchControls searchControls =
                new SearchControls(searchScope, maxResults, 0, searchOptions.getReturnAttrs(), false, false);

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = searchOptions.getResultPageSize();
            byte[] cookie = null;

            NamingEnumeration ne = null;
            
            try {
                do {
                    zlc.setPagedControl(pageSize, cookie, true);

                    ne = zlc.searchDir(base, query, searchControls);
                    while (ne != null && ne.hasMore()) {
                        SearchResult sr = (SearchResult) ne.nextElement();
                        String dn = sr.getNameInNamespace();
                        Attributes attrs = sr.getAttributes();
                        JNDIAttributes jndiAttrs = new JNDIAttributes(attrs);
                        if (visitor.wantAttrMapOnVisit()) {
                            visitor.visit(dn, jndiAttrs.getAttrs(binaryAttrs), jndiAttrs);
                        } else {
                            visitor.visit(dn, jndiAttrs);
                        }
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } catch (SearchLdapOptions.StopIteratingException e) { 
                // break out of the loop and close the ne    
            } finally {
                if (ne != null) ne.close();
            }
        } catch (SizeLimitExceededException e) {
            throw JNDILdapException.mapToLdapException(e);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        } catch (IOException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    
    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, ZLdapFilter filter, 
            ZSearchControls searchControls) throws LdapException {
        try {
            NamingEnumeration<SearchResult> result = zlc.searchDir(baseDN, 
                    ((JNDILdapFilter) filter).getNative(), 
                    ((JNDISearchControls)searchControls).getNative());
            
            return new JNDISearchResultEnumeration(result);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        } 
    }
    
    @Override
    public void deleteEntry(String dn) throws LdapException {
        try {
            zlc.unbindEntry(dn);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }

    @TODO // map all exceptions to meaningful user understandable exceptions
    static void externalLdapAuthenticate(String[] urls,
            boolean wantStartTLS, String bindDN, String password, String note)
    throws ServiceException {
        try {
            LegacyZimbraLdapContext.ldapAuthenticate(urls, wantStartTLS, 
                        bindDN, password, note);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e); // TODO
        } catch (IOException e) {
            throw JNDILdapException.mapToLdapException(e); // TODO
        }
    }

    @TODO // map all exceptions to meaningful user understandable exceptions
    static void zimbraLdapAuthenticate(String bindDN, String password)
    throws ServiceException {
        try {
            LegacyZimbraLdapContext.ldapAuthenticate(bindDN, password);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e); // TODO
        } catch (IOException e) {
            throw JNDILdapException.mapToLdapException(e); // TODO
        }
    }


}
