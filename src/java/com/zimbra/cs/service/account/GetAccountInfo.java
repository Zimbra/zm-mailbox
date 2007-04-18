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
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAccountInfo extends AccountDocumentHandler  {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element a = request.getElement(AccountConstants.E_ACCOUNT);
        String key = a.getAttribute(AccountConstants.A_BY);
        String value = a.getText();

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.fromString(key), value);

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

        Element response = lc.createElement(AccountConstants.GET_ACCOUNT_INFO_RESPONSE);
        response.addAttribute(AccountConstants.E_NAME, account.getName(), Element.Disposition.CONTENT);
        response.addKeyValuePair(Provisioning.A_zimbraId, account.getId(), AccountConstants.E_ATTR, AccountConstants.A_NAME);
        response.addKeyValuePair(Provisioning.A_zimbraMailHost, account.getAttr(Provisioning.A_zimbraMailHost), AccountConstants.E_ATTR, AccountConstants.A_NAME);
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
            response.addAttribute(AccountConstants.E_SOAP_URL, http, Element.Disposition.CONTENT);

        if (https != null && !https.equalsIgnoreCase(http))
            response.addAttribute(AccountConstants.E_SOAP_URL, https, Element.Disposition.CONTENT);
    }
}
