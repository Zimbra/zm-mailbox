package com.zimbra.cs.ldap;

import com.zimbra.common.service.ServiceException;

public abstract class ZLdapContext extends ZLdapElement implements ILdapContext {

    public abstract void closeContext();
    
    public abstract ZModificationList createModiftcationList();
    
    public abstract void deleteChildren(String dn) throws ServiceException;
    
    public abstract void modifyAttributes(String dn, ZModificationList modList) throws LdapException;
    
    /**
     * Important Note: caller is responsible to close the ZimbraLdapContext
     * 
     * Do paged search, results are returned via the search visitor interface
     */
    public abstract void search(SearchLdapOptions searchOptions) throws LdapException;
    
    public abstract ZSearchResultEnumeration searchDir(
            String baseDN, String query, ZSearchControls searchControls) throws LdapException;
    
    public abstract void unbindEntry(String dn) throws LdapException;

}
