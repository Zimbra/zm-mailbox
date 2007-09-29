/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAvailableSkins extends DocumentHandler  {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        
        if (!canAccessAccount(lc, acct))
            throw ServiceException.PERM_DENIED("can not access account");

        String[] skins = acct.getMultiAttr(Provisioning.A_zimbraAvailableSkin);
        if (skins.length == 0) {
            Provisioning prov = Provisioning.getInstance();
            skins = prov.getConfig().getMultiAttr(Provisioning.A_zimbraInstalledSkin);
        }

        Element response = lc.createElement(AccountService.GET_AVAILABLE_SKINS_RESPONSE);
        for (String skin : skins) {
            Element skinElem = response.addElement(AccountService.E_SKIN);
            skinElem.addAttribute(AccountService.A_NAME, skin);
        }
        return response;
    }
}
