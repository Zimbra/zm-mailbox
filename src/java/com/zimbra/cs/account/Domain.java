/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Domain extends NamedEntry {

    public List getAllAccounts() throws ServiceException;
    
    public void getAllAccounts(NamedEntry.Visitor visitor) throws ServiceException;

    public List getAllCalendarResources() throws ServiceException;

    public void getAllCalendarResources(NamedEntry.Visitor visitor)
    throws ServiceException;

    public List getAllDistributionLists() throws ServiceException;

    /**
     * @param query LDAP search query
     * @param returnAttrs list of attributes to return. uid is always included.
     * @param sortAttr attr to sort on. if not specified, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @return a list of all the accounts that matched.
     * @throws ServiceException
     */
    public abstract List searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException;  

    /**
     * 
     * @param query LDAP search query
     * @param type address type to search
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public SearchGalResult searchGal(String query,
                                     Provisioning.GAL_SEARCH_TYPE type,
                                     String token)
    throws ServiceException;
    
    
    public static class SearchGalResult {
        public String token;
        public List<GalContact> matches;
        public boolean hadMore; // for auto-complete only
    }

    /**
     * 
     * @param query LDAP search query
     * @param type address type to auto complete
     * @param limit max number to return
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public SearchGalResult autoCompleteGal(String query, Provisioning.GAL_SEARCH_TYPE type, int limit) throws ServiceException;

    public Map getAttrs(boolean applyConfig) throws ServiceException;

    /**
     * @param filter search filter
     * @param returnAttrs list of attributes to return. uid is always included
     * @param sortAttr attr to sort on. if not specified, sorting will be by account name
     * @param sortAscending sort ascending (true) or descending (false)
     * @return a list of all calendar resources that matched
     * @throws ServiceException
     */
    public abstract List searchCalendarResources(
        EntrySearchFilter filter,
        String returnAttrs[],
        String sortAttr,
        boolean sortAscending)
    throws ServiceException;
}
