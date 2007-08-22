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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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
import com.zimbra.common.util.Log.Level;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Creates a custom logger for the given account.
 * 
 * @author bburtin
 */
public class AddAccountLogger extends AdminDocumentHandler {

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
        Account account = prov.get(AccountBy.id, id);
        if (account == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);
        }
        
        // Add logger
        Element eLogger = request.getElement(AdminConstants.E_LOGGER);
        String category = eLogger.getAttribute(AdminConstants.A_CATEGORY);
        String sLevel = eLogger.getAttribute(AdminConstants.A_LEVEL);
        Level level = null;
        try {
            level = Level.valueOf(sLevel);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("Invalid value for level: " + sLevel, null);
        }
        ZimbraLog.misc.info("Adding custom logger: account=%s, category=%s, level=%s",
            account.getName(), category, level);
        Log.addAccountLogger(category, account.getName(), level);

        // Send response
        Element response = zsc.createElement(AdminConstants.ADD_ACCOUNT_LOGGER_RESPONSE);
        return response;
    }
    
}
