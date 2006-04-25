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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Domain.SearchGalResult;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class SearchGal extends DocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        String n = request.getAttribute(AccountService.E_NAME);

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AccountService.SEARCH_GAL_RESPONSE);
        Account acct = getRequestedAccount(getZimbraSoapContext(context));

        while (n.endsWith("*"))
            n = n.substring(0, n.length() - 1);

        String typeStr = request.getAttribute(AdminService.A_TYPE, "all");
        Provisioning.GAL_SEARCH_TYPE type;
        if (typeStr.equals("all"))
            type = Provisioning.GAL_SEARCH_TYPE.ALL;
        else if (typeStr.equals("account"))
            type = Provisioning.GAL_SEARCH_TYPE.USER_ACCOUNT;
        else if (typeStr.equals("resource"))
            type = Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE;
        else
            throw ServiceException.INVALID_REQUEST("Invalid search type: " + typeStr, null);

        SearchGalResult result = acct.getDomain().searchGal(n, type, null);
        for (GalContact contact : result.matches)
            addContact(response, contact);
        return response;
    }

    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    public static void addContact(Element response, GalContact contact) throws ServiceException {
        Element cn = response.addElement(MailService.E_CONTACT);
        cn.addAttribute(MailService.A_ID, contact.getId());
        Map attrs = contact.getAttrs();
        for (Map.Entry entry : (Set<Map.Entry>) attrs.entrySet()) {
            Object value = entry.getValue();
            // can't use DISP_ELEMENT because some GAL contact attributes
            //   (e.g. "objectClass") are multi-valued
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++) {
                    cn.addElement(MailService.E_ATTRIBUTE)
                      .addAttribute(MailService.A_ATTRIBUTE_NAME, (String) entry.getKey())
                      .setText(sa[i]);
                }
            } else {
                cn.addElement(MailService.E_ATTRIBUTE)
                  .addAttribute(MailService.A_ATTRIBUTE_NAME, (String) entry.getKey())
                  .setText((String) entry.getValue());
            }
        }
    }
}