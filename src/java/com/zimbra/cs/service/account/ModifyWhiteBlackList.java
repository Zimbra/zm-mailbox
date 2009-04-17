/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyWhiteBlackList extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");

        HashMap<String, Object> addrs = new HashMap<String, Object>();
        
        doList(request.getOptionalElement(AccountConstants.E_WHITE_LIST),
               Provisioning.A_amavisWhitelistSender,
               addrs);
        
        doList(request.getOptionalElement(AccountConstants.E_BLACK_LIST),
                Provisioning.A_amavisBlacklistSender,
                addrs);

        // call modifyAttrs and pass true to checkImmutable
        Provisioning.getInstance().modifyAttrs(account, addrs, true, zsc.getAuthToken());

        Element response = zsc.createElement(AccountConstants.MODIFY_WHITE_BLACK_LIST_RESPONSE);
        return response;
    }
    
    private void doList(Element eList, String attrName, HashMap<String, Object> addrs) {
        if (eList == null)
            return;
        
        // empty list, means delete all
        if (eList.getOptionalElement(AccountConstants.E_ADDR) == null) {
            StringUtil.addToMultiMap(addrs, attrName, "");
            return;
        }
        
        for (Element eAddr : eList.listElements(AccountConstants.E_ADDR)) {
            String attr = eAddr.getAttribute(AccountConstants.A_OP, "") + attrName;
            String value = eAddr.getText();
            StringUtil.addToMultiMap(addrs, attr, value);
        }
    }

}

