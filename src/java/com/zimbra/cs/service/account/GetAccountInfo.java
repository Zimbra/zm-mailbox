/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAccountInfo extends AccountDocumentHandler  {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
   
        Element a = request.getElement(AccountService.E_ACCOUNT);
        String key = a.getAttribute(AccountService.A_BY);
        String value = a.getText();

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.fromString(key), value);

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

        Element response = lc.createElement(AccountService.GET_ACCOUNT_INFO_RESPONSE);
        response.addElement(AccountService.E_NAME).setText(account.getName());
        addAttr(response, Provisioning.A_zimbraId, account.getId());
        addAttr(response, Provisioning.A_zimbraMailHost, account.getAttr(Provisioning.A_zimbraMailHost));
 
        addUrls(response, account);
        
        return response;
    }

    static void addUrls(Element response, Account account) throws ServiceException {
        Server server = Provisioning.getInstance().getServer(account);
        if (server == null) return;
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);        
        if (hostname == null) return;
        
        String http = URLUtil.getSoapURL(server, false);
        String https = URLUtil.getSoapURL(server, true);

        if (http != null)
            response.addElement(AccountService.E_SOAP_URL).setText(http);
        
        if (https != null && !https.equalsIgnoreCase(http))
            response.addElement(AccountService.E_SOAP_URL).setText(https);

    }

    private static void addAttr(Element response, String name, String value) {
        if (value != null && !value.equals("")) {
            Element e = response.addElement(AccountService.E_ATTR);
            e.addAttribute(AccountService.A_NAME, name);
            e.setText(value);
        }
    }
}
