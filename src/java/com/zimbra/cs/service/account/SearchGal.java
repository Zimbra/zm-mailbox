/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class SearchGal extends DocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        String n = request.getAttribute(AccountService.E_NAME);

        ZimbraContext lc = getZimbraContext(context);
        Element response = lc.createElement(AccountService.SEARCH_GAL_RESPONSE);
        Account acct = getRequestedAccount(getZimbraContext(context));

        List contacts = acct.getDomain().searchGal(n);
        for (Iterator it = contacts.iterator(); it.hasNext();) {
            GalContact contact = (GalContact) it.next();
            addContact(response, contact);
        }
        return response;
    }

    public boolean needsAuth(Map context) {
        return true;
    }

    public static void addContact(Element response, GalContact contact) throws ServiceException {
        Element cn = response.addElement(MailService.E_CONTACT);
        cn.addAttribute(MailService.A_ID, contact.getId());
        Map attrs = contact.getAttrs();
        for (Iterator it = attrs.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            cn.addAttribute((String) entry.getKey(), (String) entry.getValue(),
                    Element.DISP_ELEMENT);
        }
    }
}