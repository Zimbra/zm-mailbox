/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.service.admin.GetAdminSavedSearches;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyAdminSavedSearches extends AdminDocumentHandler {
    
    /**
     * must be careful and only allow on accounts domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        
        checkAccountRight(zsc, acct, Admin.R_setAdminSavedSearch);

        Element response = zsc.createElement(AdminConstants.MODIFY_ADMIN_SAVED_SEARCHES_RESPONSE);
        
        HashMap<String, String> searches = null;
        for (Iterator it = request.elementIterator(AdminConstants.E_SEARCH); it.hasNext(); ) {
            if (searches == null)
                searches = new HashMap<String, String>();
            Element e = (Element) it.next();
            String name = e.getAttribute(AdminConstants.A_NAME);
            String query = e.getText();
            if (name != null && name.length() != 0)
                searches.put(name, query);
            else
                ZimbraLog.account.warn("ModifyAdminSavedSearches: empty search name ignored");
        }

        handle(acct, response, searches);
        return response;
    }
    
    public void handle(Account acct, Element response, HashMap<String, String> modSearches) throws ServiceException {
        String[] searches = acct.getMultiAttr(Provisioning.A_zimbraAdminSavedSearches);
        Map<String, GetAdminSavedSearches.AdminSearch> curSearches = new HashMap<String, GetAdminSavedSearches.AdminSearch>();
        for (int i = 0; i < searches.length; i++) {
            GetAdminSavedSearches.AdminSearch as = GetAdminSavedSearches.AdminSearch.parse(searches[i]);
            curSearches.put(as.getName(), as);
        }
        
        for (Iterator it = modSearches.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            String query = (String)entry.getValue();
            GetAdminSavedSearches.AdminSearch mod = new GetAdminSavedSearches.AdminSearch(name, query);
            if (curSearches.containsKey(name)) {
                if (query.length() == 0)
                    curSearches.remove(name);             
                else
                    curSearches.put(name, mod);
            } else {
                if (query.length() == 0)
                    throw ServiceException.INVALID_REQUEST("query for " + name + " is empty", null);
                else
                    curSearches.put(name, mod);
            }
        }

        String[] mods = new String[curSearches.size()];
        int i = 0;
        for (Iterator it = curSearches.values().iterator(); it.hasNext(); ) {
            GetAdminSavedSearches.AdminSearch m = (GetAdminSavedSearches.AdminSearch)it.next();
            mods[i++] = m.encode();
        }
            
        Provisioning prov = Provisioning.getInstance();
        Map<String,String[]> modmap = new HashMap<String,String[]>();
        modmap.put(Provisioning.A_zimbraAdminSavedSearches, mods);
        prov.modifyAttrs(acct, modmap);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_setAdminSavedSearch);
    }
}
