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
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapException.LdapInvalidNameException;
import com.zimbra.cs.ldap.LdapException.LdapEntryAlreadyExistException;
import com.zimbra.cs.ldap.LdapException.LdapNameNotFoundException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZModificationList;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.LdapServerType;

/**
 * Wraps a legacy ZimbraLdapContext
 *
 */
public class JNDILdapContext extends ZLdapContext {
    
    private ZimbraLdapContext zlc;
    
    public JNDILdapContext(LdapServerType serverType) throws ServiceException {
        zlc = new ZimbraLdapContext(serverType.isMaster());
    }
    
    public JNDILdapContext(LdapServerType serverType, boolean useConnPool) throws ServiceException {
        zlc = new ZimbraLdapContext(serverType.isMaster(), useConnPool);
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void closeContext() {
        ZimbraLdapContext.closeContext(zlc);
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
    public ZModificationList createModiftcationList() {
        return new JNDIModificationList();
    }
    
    @Override
    public void deleteChildren(String dn) throws ServiceException {
        zlc.deleteChildren(dn);
    }
    
    @Override
    public ZAttributes getAttributes(String dn) throws LdapException {
        try {
            Attributes attributes = zlc.getAttributes(dn);
            return new JNDIAttributes(attributes);
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
            zlc.replaceAttributes(dn, ((JNDIAttributes) attrs).get());
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    @Override
    public void searchPaged(SearchLdapOptions searchOptions) throws ServiceException {
        int maxResults = 0; // no limit
        String base = searchOptions.getSearchBase();
        String query = searchOptions.getQuery();
        Set<String> binaryAttrs = searchOptions.getBinaryAttrs();
        SearchLdapOptions.SearchLdapVisitor visitor = searchOptions.getVisitor();
        
        try {
            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, searchOptions.getReturnAttrs(), false, false);

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
                        visitor.visit(dn, jndiAttrs.getAttrs(binaryAttrs), jndiAttrs);
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } finally {
                if (ne != null) ne.close();
            }
        } catch (SizeLimitExceededException e) {
            throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("too many search results returned", e);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        } catch (IOException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }
    
    
    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, String filter, ZSearchControls searchControls) 
    throws LdapException {
        try {
            NamingEnumeration<SearchResult> result = zlc.searchDir(baseDN, filter, 
                    ((JNDISearchControls)searchControls).get());
            return new JNDISearchResultEnumeration(result);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        } 
    }
    
    @Override
    public void unbindEntry(String dn) throws LdapException {
        try {
            zlc.unbindEntry(dn);
        } catch (NamingException e) {
            throw JNDILdapException.mapToLdapException(e);
        }
    }


}
