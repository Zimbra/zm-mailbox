/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Domain extends NamedEntry {

    public List getAllAccounts() throws ServiceException;

    /**
     * @param query LDAP search query
     * @param returnAttrs list of attributes to return. uid is always included.
     * @param sortAttr attr to sort on. if not specified, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @return an ArrayList of all the accounts that matched.
     * @throws ServiceException
     */
    public abstract ArrayList searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending) throws ServiceException;  

    /**
     * 
     * @param query Name search string
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public abstract List searchGal(String query)  throws ServiceException;
    
    public Map getAttrs(boolean applyConfig) throws ServiceException;
}
