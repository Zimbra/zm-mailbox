/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class AutoCompleteGal extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        if (!zsc.getAuthToken().isAdmin() && !zsc.getAuthToken().isDomainAdmin()) {
            if (!(account.getBooleanAttr(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled , false) &&
                  account.getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled , false)))
                throw ServiceException.PERM_DENIED("cannot auto complete GAL");
        }
        
        String n = request.getAttribute(AccountConstants.E_NAME);
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

        Element response = zsc.createElement(AccountConstants.AUTO_COMPLETE_GAL_RESPONSE);
        
        Provisioning prov = Provisioning.getInstance();
        Domain d = prov.getDomain(account);
        SearchGalResult result = prov.autoCompleteGal(d, n, type, max);
        com.zimbra.cs.service.account.SearchGal.toXML(response, result);

        return response;
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }
}
