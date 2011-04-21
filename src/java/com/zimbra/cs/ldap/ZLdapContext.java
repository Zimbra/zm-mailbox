package com.zimbra.cs.ldap;

import com.zimbra.common.service.ServiceException;

public abstract class ZLdapContext extends ZLdapElement implements ILdapContext {

    public abstract void closeContext();
    
    public abstract void createEntry(ZMutableEntry entry) throws ServiceException;
    
    public abstract void createEntry(String dn, String objectClass, String[] attrs) throws ServiceException;
    
    public abstract void createEntry(String dn, String[] objectClasses, String[] attrs) throws ServiceException;
    
    public abstract ZModificationList createModiftcationList();
    
    public abstract void deleteChildren(String dn) throws ServiceException;
    
    public abstract ZAttributes getAttributes(String dn) throws LdapException;
    
    public abstract void modifyAttributes(String dn, ZModificationList modList) throws LdapException;
    
    public abstract void moveChildren(String oldDn, String newDn) throws ServiceException;
    
    public abstract void renameEntry(String oldDn, String newDn) throws LdapException;
    
    public abstract void replaceAttributes(String dn, ZAttributes attrs) throws LdapException;
    
    /**
     * Important Note: caller is responsible to close the ZimbraLdapContext
     * 
     * This API does paged search, results are returned via the search visitor interface
     */
    public abstract void searchPaged(SearchLdapOptions searchOptions) throws ServiceException;
    
    public abstract ZSearchResultEnumeration searchDir(
            String baseDN, String query, ZSearchControls searchControls) throws LdapException;
    
    public abstract void unbindEntry(String dn) throws LdapException;

}
