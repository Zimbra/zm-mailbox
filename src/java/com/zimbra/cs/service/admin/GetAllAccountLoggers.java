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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
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
        
        Element response = zsc.createElement(AdminConstants.GET_ALL_ACCOUNT_LOGGERS_RESPONSE);
        for (AccountLogger al : LogFactory.getAllAccountLoggers()) {
            // Look up account
            Account account = prov.get(AccountBy.name, al.getAccountName());
            if (account == null) {
                ZimbraLog.misc.info("GetAllAccountLoggers: unable to find account '%s'.  Ignoring account logger.",
                    al.getAccountName());
                continue;
            }
            
            // Add elements
            Element alElement = response.addElement(AdminConstants.E_ACCOUNT_LOGGER);
            
            Element accountElement = alElement.addElement(AdminConstants.E_ACCOUNT);
            accountElement.addAttribute(AdminConstants.A_ID, account.getId());
            accountElement.addAttribute(AdminConstants.A_NAME, account.getName());
            
            Element loggerElement = alElement.addElement(AdminConstants.E_LOGGER);
            loggerElement.addAttribute(AdminConstants.A_CATEGORY, al.getCategory());
            loggerElement.addAttribute(AdminConstants.A_LEVEL, al.getLevel().toString());
        }
        
        return response;
    }

}
