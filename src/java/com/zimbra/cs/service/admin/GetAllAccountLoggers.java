/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.AccountLogger;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllAccountLoggers extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(zsc, context, localServer, Admin.R_manageAccountLogger);
        
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
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageAccountLogger);
    }
}
