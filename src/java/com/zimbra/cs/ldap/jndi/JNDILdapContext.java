package com.zimbra.cs.ldap.jndi;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapException.LdapInvalidNameException;
import com.zimbra.cs.ldap.LdapException.LdapNameNotFoundException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;
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
    public ZSearchResultEnumeration searchDir(String baseDN, String filter, 
            ZSearchControls searchControls) throws LdapException {
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
