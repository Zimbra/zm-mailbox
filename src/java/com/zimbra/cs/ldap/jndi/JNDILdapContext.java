package com.zimbra.cs.ldap.jndi;

import java.io.IOException;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil.SearchLdapVisitor;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapException.LdapInvalidNameException;
import com.zimbra.cs.ldap.LdapException.LdapNameNotFoundException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZModificationList;
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
    
    
    @TODO // check all call sites see if this mapping is proper for all call sites
    private LdapException mapToLdapException(NamingException e) {
        if (e instanceof NameNotFoundException) {
            LdapTODO.FAIL(FailCode.NameNotFoundExceptionShouldNeverBeThrown);
            return new LdapNameNotFoundException(e);
        } else if (e instanceof InvalidNameException) {
            LdapTODO.FAIL(FailCode.LdapInvalidNameExceptionShouldNeverBeThrown);
            return new LdapInvalidNameException(e);
        } else {
            // anything else is mapped to a generic LDAP error
            return LdapException.LDAP_ERROR(e);
        }
    }
    
    
    @Override
    public void closeContext() {
        ZimbraLdapContext.closeContext(zlc);
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
    public void modifyAttributes(String dn, ZModificationList modList) throws LdapException {
        try {
            zlc.modifyAttributes(dn, ((JNDIModificationList)modList).getModListAsArray());
        } catch (NamingException e) {
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
        } catch (NamingException e) {
            throw LdapException.LDAP_ERROR("unable to search ldap", e);
        } catch (IOException e) {
            throw LdapException.LDAP_ERROR("unable to search ldap", e);
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
            throw mapToLdapException(e);
        } 
    }
    
    @Override
    public void unbindEntry(String dn) throws LdapException {
        try {
            zlc.unbindEntry(dn);
        } catch (NamingException e) {
            throw LdapException.LDAP_ERROR(e);
        }
    }



}
