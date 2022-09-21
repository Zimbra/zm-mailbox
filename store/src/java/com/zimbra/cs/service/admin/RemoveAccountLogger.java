/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
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
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(zsc, context, localServer, Admin.R_manageAccountLogger);
        
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
            if (category.equalsIgnoreCase(AddAccountLogger.CATEGORY_ALL)) {
                category = null;
            } else if (!((LoggerContext) LogManager.getContext(false)).hasLogger(category)) {
                throw ServiceException.INVALID_REQUEST("Log category " + category + " does not exist.", null);
            }
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
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageAccountLogger);
    }
}
