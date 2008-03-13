/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.AccountLogger;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllAccountLoggers extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Map<String, Element> accountElements = new HashMap<String, Element>();
        
        Element response = zsc.createElement(AdminConstants.GET_ALL_ACCOUNT_LOGGERS_RESPONSE);
        for (AccountLogger al : LogFactory.getAllAccountLoggers()) {
            // Look up account
            Account account = prov.get(AccountBy.name, al.getAccountName(), zsc.getAuthToken());
            if (account == null) {
                ZimbraLog.misc.info("GetAllAccountLoggers: unable to find account '%s'.  Ignoring account logger.",
                    al.getAccountName());
                continue;
            }
            
            // Add elements
            Element eAccountLogger = accountElements.get(account.getId());
            if (eAccountLogger == null) {
                eAccountLogger = response.addElement(AdminConstants.E_ACCOUNT_LOGGER);
                accountElements.put(account.getId(), eAccountLogger);
            }
            eAccountLogger.addAttribute(AdminConstants.A_ID, account.getId());
            eAccountLogger.addAttribute(AdminConstants.A_NAME, account.getName());
            
            Element eLogger = eAccountLogger.addElement(AdminConstants.E_LOGGER);
            eLogger.addAttribute(AdminConstants.A_CATEGORY, al.getCategory());
            eLogger.addAttribute(AdminConstants.A_LEVEL, al.getLevel().toString());
        }
        
        return response;
    }
}
