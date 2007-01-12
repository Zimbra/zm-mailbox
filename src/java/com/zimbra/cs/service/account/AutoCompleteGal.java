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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class AutoCompleteGal extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        String n = request.getAttribute(AccountConstants.E_NAME);

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AccountConstants.AUTO_COMPLETE_GAL_RESPONSE);
        Account acct = getRequestedAccount(getZimbraSoapContext(context));

        while (n.endsWith("*"))
            n = n.substring(0, n.length() - 1);

        String typeStr = request.getAttribute(AccountConstants.A_TYPE, "account");
        int max = (int) request.getAttributeLong(AccountConstants.A_LIMIT);
        Provisioning.GAL_SEARCH_TYPE type;
        if (typeStr.equals("all"))
            type = Provisioning.GAL_SEARCH_TYPE.ALL;
        else if (typeStr.equals("account"))
            type = Provisioning.GAL_SEARCH_TYPE.USER_ACCOUNT;
        else if (typeStr.equals("resource"))
            type = Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE;
        else
            throw ServiceException.INVALID_REQUEST("Invalid search type: " + typeStr, null);

        
        Provisioning prov = Provisioning.getInstance();
        SearchGalResult result = prov.autoCompleteGal(prov.getDomain(acct), n, type, max);

        response.addAttribute(AccountConstants.A_MORE, result.hadMore);
        
        for (GalContact contact : result.matches)
            addContact(response, contact);

        return response;
    }

    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    public static void addContact(Element response, GalContact contact) {
        Element cn = response.addElement(MailConstants.E_CONTACT);
        cn.addAttribute(MailConstants.A_ID, contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        for (Map.Entry entry : attrs.entrySet()) {
            Object value = entry.getValue();
            // can't use DISP_ELEMENT because some GAL contact attributes
            //   (e.g. "objectClass") are multi-valued
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++) {
                    cn.addElement(MailConstants.E_ATTRIBUTE)
                      .addAttribute(MailConstants.A_ATTRIBUTE_NAME, (String) entry.getKey())
                      .setText(sa[i]);
                }
            } else {
                cn.addElement(MailConstants.E_ATTRIBUTE)
                  .addAttribute(MailConstants.A_ATTRIBUTE_NAME, (String) entry.getKey())
                  .setText((String) entry.getValue());
            }
        }
    }
}
