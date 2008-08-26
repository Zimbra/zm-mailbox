/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class SyncGal extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        if (!(account.getBooleanAttr(Provisioning.A_zimbraFeatureGalSyncEnabled , false) && 
              account.getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled , false)))
            throw ServiceException.PERM_DENIED("cannot sync GAL");

        Domain d = Provisioning.getInstance().getDomain(account);
        String tokenAttr = request.getAttribute(MailConstants.A_TOKEN, "");
        SearchGalResult result = Provisioning.getInstance().searchGal(d, "", Provisioning.GAL_SEARCH_TYPE.ALL, tokenAttr);

        Element response = zsc.createElement(AccountConstants.SYNC_GAL_RESPONSE);
        if (result.token != null)
            response.addAttribute(MailConstants.A_TOKEN, result.token);
        for (GalContact contact : result.matches)
            SearchGal.addContact(response, contact);
        return response;
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }
}
