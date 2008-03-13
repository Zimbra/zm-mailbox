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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Removes a custom logger from the given account.
 * 
 * @author bburtin
 */
public class RemoveAccountLogger extends AdminDocumentHandler {

    // Support for proxying if the account isn't on this server 
    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        // Look up account
        String id = request.getElement(AdminConstants.E_ID).getText();
        Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        if (account == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);
        }
        
        // Remove logger
        Element eLogger = request.getElement(AdminConstants.E_LOGGER);
        String category = eLogger.getAttribute(AdminConstants.A_CATEGORY);
        
        ZimbraLog.misc.info("Deleting custom logger: account=%s, category=%s",
            account.getName(), category);
        Log.deleteAccountLogger(category, account.getName());

        // Send response
        Element response = zsc.createElement(AdminConstants.REMOVE_ACCOUNT_LOGGER_RESPONSE);
        return response;
    }
}
