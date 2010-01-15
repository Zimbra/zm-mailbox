/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;


public class GetAdminSavedSearches extends AdminDocumentHandler {    
   
    /**
     * must be careful and only allow on accounts domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        
        checkAccountRight(zsc, acct, Admin.R_viewAdminSavedSearch);

        Element response = zsc.createElement(AdminConstants.GET_ADMIN_SAVED_SEARCHES_RESPONSE);
        
        HashSet<String> specificSearches = null;
        for (Iterator it = request.elementIterator(AdminConstants.E_SEARCH); it.hasNext(); ) {
            if (specificSearches == null)
                specificSearches = new HashSet<String>();
            Element e = (Element) it.next();
            String name = e.getAttribute(AdminConstants.A_NAME);
            if (name != null)
                specificSearches.add(name);
        }

        handle(acct, response, specificSearches);
        return response;
    }
    
    public void handle(Account acct, Element response, HashSet<String> specificSearches) throws ServiceException {
        String[] searches = acct.getMultiAttr(Provisioning.A_zimbraAdminSavedSearches);
        
        for (int i = 0; i < searches.length; i++) {
            String search = searches[i];
            AdminSearch as = AdminSearch.parse(search);
            
            if (specificSearches == null || specificSearches.contains(as.getName()))
                response.addElement(AdminConstants.E_SEARCH).addAttribute(AdminConstants.A_NAME, as.getName()).setText(as.getQuery());
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_viewAdminSavedSearch);
    }
    
    public static class AdminSearch {
        
        private static final String DELIM = " : ";
        private static final int DELIM_LEN = DELIM.length();
        
        // mName contains unescaped search name
        String mName = "";
        String mQuery = "";
        
        public AdminSearch(String name, String query) {
            if (name != null)
                mName = name;
            
            if (query != null)
                mQuery = query;
        }
        
        public String getName() { return mName; }
        public String getQuery() { return mQuery; }
        
        public static AdminSearch parse(String search) {
            // parse search into name and query
            // search is in the format of {name}SPACE:SPACE{query}
            // colons (:) in name are escaped as \:
            
            String name = null;
            String query = null;
            int delimAt = search.indexOf(DELIM);
            if (delimAt == -1) {
                // no name, returning the entire attr as query
                query = search;
            } else {
                name = search.substring(0, delimAt);
                if (search.length() > delimAt + DELIM_LEN)
                    query = search.substring(delimAt + DELIM_LEN);
                // else there is no query after the delimiter
            }

            // unescape colons in name
            if (name != null)
                name = name.replaceAll("\\\\:", ":");
            
            return new AdminSearch(name, query);
        }
        
        public String encode() {
            return mName.replaceAll(":", "\\\\:") + DELIM + mQuery;
        }
    }
    
    private static void test(String search) {
        AdminSearch as = AdminSearch.parse(search);
        
        System.out.println("[" + search + "] => [" + as.getName() + "] [" + as.getQuery() + "]" );
        String encoded = as.encode();
        assert(search.equals(encoded));
    }
    
    private static void test(AdminSearch search) {
        String encoded = search.encode();
        System.out.println("[" + search.getName() + "] [" + search.getQuery() + "] => [" + encoded + "]");
        
        AdminSearch parsed = AdminSearch.parse(encoded);
        String encoded2 = parsed.encode();
        assert(encoded.equals(encoded2));
    }
    
    public static void main(String args[]) {
        test("name : query");
        test("n a m e : q u e r y");
        test("name\\:\\ : que : ry");
        test("name \\: : query");
        test("\\: : query");
        
        test(new AdminSearch("name :::", ":::query"));
        test(new AdminSearch("\\:", " \\:::query"));
    }
}
