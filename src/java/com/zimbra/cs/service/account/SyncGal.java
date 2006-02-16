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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Domain.SearchGalResult;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class SyncGal extends DocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Element response = lc.createElement(AccountService.SYNC_GAL_RESPONSE);
        String tokenAttr = request.getAttribute(MailService.A_TOKEN, "");        
        Account acct = getRequestedAccount(getZimbraContext(context));

        SearchGalResult result = acct.getDomain().searchGal("", Provisioning.GAL_SEARCH_TYPE.ALL, tokenAttr);
        if (result.token != null)
            response.addAttribute(MailService.A_TOKEN, result.token);
        for (GalContact contact : result.matches)
            SearchGal.addContact(response, contact);
        return response;
    }

    public boolean needsAuth(Map context) {
        return true;
    }
}
