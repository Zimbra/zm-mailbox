/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class SearchAccounts extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
        Provisioning prov = Provisioning.getInstance();

        String query = request.getAttribute(AdminService.E_QUERY);

        int limit = (int) request.getAttributeLong(AdminService.A_LIMIT, Integer.MAX_VALUE);
        if (limit == 0)
            limit = Integer.MAX_VALUE;
        int offset = (int) request.getAttributeLong(AdminService.A_OFFSET, 0);        
        String domain = request.getAttribute(AdminService.A_DOMAIN, null);
        boolean applyCos = request.getAttributeBool(AdminService.A_APPLY_COS, true);
        String attrsStr = request.getAttribute(AdminService.A_ATTRS, null);
        String sortBy = request.getAttribute(AdminService.A_SORT_BY, null);        
        boolean sortAscending = request.getAttributeBool(AdminService.A_SORT_ASCENDING, true);        

        String[] attrs = attrsStr == null ? null : attrsStr.split(",");

        ArrayList accounts;

        Domain d = null;
        if (domain != null) {
            d = prov.getDomainByName(domain);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        }

        // TODO: this is the point we should check in the session to see if we already have this query cached
        // from the previous search
        if (d != null) {
            accounts = d.searchAccounts(query, attrs, sortBy, sortAscending);
        } else {
            accounts = prov.searchAccounts(query, attrs, sortBy, sortAscending);
        }

        Element response = lc.createElement(AdminService.SEARCH_ACCOUNTS_RESPONSE);
        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < accounts.size(); i++) {
            Account account = (Account) accounts.get(i);        
            GetAccount.doAccount(response, account, applyCos);
        }          

        response.addAttribute(AdminService.A_MORE, i < accounts.size());
        response.addAttribute(AdminService.A_SEARCH_TOTAL, accounts.size());        
        return response;
    }
}
