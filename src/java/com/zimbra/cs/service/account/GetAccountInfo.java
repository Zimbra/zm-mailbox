/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAccountInfo extends AccountDocumentHandler  {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element a = request.getElement(AccountConstants.E_ACCOUNT);
        String key = a.getAttribute(AccountConstants.A_BY);
        String value = a.getText();

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.fromString(key), value, zsc.getAuthToken());

        // prevent directory harvest attack, mask no such account as permission denied
        if (account == null)
            throw ServiceException.PERM_DENIED("can not access account");

        Element response = zsc.createElement(AccountConstants.GET_ACCOUNT_INFO_RESPONSE);
        response.addAttribute(AccountConstants.E_NAME, account.getName(), Element.Disposition.CONTENT);
        response.addKeyValuePair(Provisioning.A_zimbraId, account.getId(), AccountConstants.E_ATTR, AccountConstants.A_NAME);
        response.addKeyValuePair(Provisioning.A_zimbraMailHost, account.getAttr(Provisioning.A_zimbraMailHost), AccountConstants.E_ATTR, AccountConstants.A_NAME);
        response.addKeyValuePair(Provisioning.A_displayName, account.getAttr(Provisioning.A_displayName), AccountConstants.E_ATTR, AccountConstants.A_NAME);
        addUrls(response, account);
        return response;
    }

    static void addUrls(Element response, Account account) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        Server server = prov.getServer(account);
        if (server == null) return;
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);        
        if (hostname == null) return;
        
        Domain domain = prov.getDomain(account);

        String httpSoap = URLUtil.getSoapPublicURL(server, domain, false);
        String httpsSoap = URLUtil.getSoapPublicURL(server, domain, true);
        
        if (httpSoap != null)
            response.addAttribute(AccountConstants.E_SOAP_URL, httpSoap, Element.Disposition.CONTENT);

        if (httpsSoap != null && !httpsSoap.equalsIgnoreCase(httpSoap))
            response.addAttribute(AccountConstants.E_SOAP_URL, httpsSoap, Element.Disposition.CONTENT);
        
        String pubUrl = URLUtil.getPublicURLForDomain(server, domain, "", true);
        if (pubUrl != null)
            response.addAttribute(AccountConstants.E_PUBLIC_URL, pubUrl, Element.Disposition.CONTENT);
        
        String changePasswordUrl = null;
        if (domain != null)
            changePasswordUrl = domain.getAttr(Provisioning.A_zimbraChangePasswordURL);
        if (changePasswordUrl != null)
            response.addAttribute(AccountConstants.E_CHANGE_PASSWORD_URL, changePasswordUrl, Element.Disposition.CONTENT);
    }
}
