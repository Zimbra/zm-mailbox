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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.soap.ZimbraSoapContext;


public class ImportData extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);

        for (Element elem : request.listElements()) {
            DataSource ds;
            
            String name, id = elem.getAttribute(MailConstants.A_ID, null);
            if (id != null) {
                ds = prov.get(account, DataSourceBy.id, id);
                if (ds == null) {
                    throw ServiceException.INVALID_REQUEST("Could not find Data Source with id " + id, null);
                }
            } else if ((name = elem.getAttribute(MailConstants.A_NAME, null)) != null) {
                ds = prov.get(account, DataSourceBy.name, name);
                if (ds == null) {
                    throw ServiceException.INVALID_REQUEST("Could not find Data Source with name " + name, null);
                }
            } else {
                throw ServiceException.INVALID_REQUEST("must specify either 'id' or 'name'", null);
            }
            
            ZimbraLog.addDataSourceNameToContext(ds.getName());
            DataSourceManager.importData(ds);
        }
        
        Element response = zsc.createElement(MailConstants.IMPORT_DATA_RESPONSE);
        return response;
    }
    
}
