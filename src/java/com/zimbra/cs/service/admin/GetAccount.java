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
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetAccount extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    public static final String BY_ADMIN_NAME = "adminName";
    
	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
        Provisioning prov = Provisioning.getInstance();

        boolean applyCos = request.getAttributeBool(AdminService.A_APPLY_COS, true);        
        Element a = request.getElement(AdminService.E_ACCOUNT);
	    String key = a.getAttribute(AdminService.A_BY);
        String value = a.getText();

	    Account account = null;

        if (key.equals(BY_NAME)) {
            account = prov.getAccountByName(value);
        } else if (key.equals(BY_ID)) {
            account = prov.getAccountById(value);
        } else if (key.equals(BY_ADMIN_NAME)) {
            account = prov.getAdminAccountByName(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

	    Element response = lc.createElement(AdminService.GET_ACCOUNT_RESPONSE);
        doAccount(response, account, applyCos);

	    return response;
	}

    public static void doAccount(Element e, Account a) throws ServiceException {
        doAccount(e, a, true);
    }
    
    public static void doAccount(Element e, Account a, boolean applyCos) throws ServiceException {
        Element account = e.addElement(AdminService.E_ACCOUNT);
        account.addAttribute(AdminService.A_NAME, a.getName());
        account.addAttribute(AdminService.A_ID, a.getId());        
        Map attrs = a.getAttrs(false, applyCos);
        doAttrs(account, attrs);
    }
    
    static void doAttrs(Element e, Map attrs) {
        for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            // TODO: might be being too paranoid, but there doesn't seem like a good reason to return this
            if (name.equals(Provisioning.A_userPassword))
                value = "VALUE-BLOCKED";
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    Element pref = e.addElement(AdminService.E_A);
                    pref.addAttribute(AdminService.A_N, name);
                    pref.setText(sv[i]);
                }
            } else if (value instanceof String) {
                Element pref = e.addElement(AdminService.E_A);
                pref.addAttribute(AdminService.A_N, name);
                pref.setText((String) value);
            }
        }       
    }
}