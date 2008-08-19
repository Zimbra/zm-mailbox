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
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Removes a custom logger from the given account.
 * 
 * @author bburtin
 */
public class RemoveAccountLogger extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // Look up account, if specified.
        Account account = null;
        String accountName = null;
        if (request.getOptionalElement(AdminConstants.E_ID) != null ||
            request.getOptionalElement(AdminConstants.E_ACCOUNT) != null) {
            account = AddAccountLogger.getAccountFromLoggerRequest(request);
            accountName = account.getName();
        }
        
        // Look up log category, if specified.
        Element eLogger = request.getOptionalElement(AdminConstants.E_LOGGER);
        String category = null;
        if (eLogger != null) {
            category = eLogger.getAttribute(AdminConstants.A_CATEGORY);
        }

        // Do the work.
        for (Log log : LogFactory.getAllLoggers()) {
            if (category == null || log.getCategory().equals(category)) {
                if (accountName != null) {
                    boolean removed = log.removeAccountLogger(accountName);
                    if (removed) {
                        ZimbraLog.misc.info("Removed logger for account %s from category %s.",
                            accountName, log.getCategory());
                    }
                } else {
                    int count = log.removeAccountLoggers();
                    if (count > 0) {
                        ZimbraLog.misc.info("Removed %d custom loggers from category %s.",
                            count, log.getCategory());
                    }
                }
            }
        }
        
        // Send response.
        Element response = zsc.createElement(AdminConstants.REMOVE_ACCOUNT_LOGGER_RESPONSE);
        return response;
    }
}
