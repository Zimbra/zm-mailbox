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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.mailbox.DataSourceManager;
import com.zimbra.soap.ZimbraSoapContext;


public class ImportData extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);

        for (Element elem : request.listElements()) {
            DataSource ds = null;
            DataSource.Type type = DataSource.Type.fromString(elem.getName());
            
            String id = elem.getAttribute(MailConstants.A_ID);
            ds = prov.get(account, DataSourceBy.id, id);
            if (ds.getType() != type) {
                String msg = String.format(
                    "Incorrect type specified for Data Source %s (%s).  Expected '%s', got '%s'.",
                    ds.getId(), ds.getName(), ds.getType(), type);
                throw ServiceException.INVALID_REQUEST(msg, null);
            }
            if (ds == null) {
                throw ServiceException.INVALID_REQUEST("Could not find Data Source with id " + id, null);
            }

            Thread thread = new Thread(new ImportDataThread(account, ds));
            thread.setName("ImportDataThread-" + account.getName());
            thread.start();
        }
        
        Element response = zsc.createElement(MailConstants.IMPORT_DATA_RESPONSE);
        return response;
    }
    
    private static class ImportDataThread implements Runnable {
        Account mAccount;
    	DataSource mDataSource;
        
    	public ImportDataThread(Account account, DataSource ds) {
            mAccount = account;
    		mDataSource = ds;
    	}
        
    	public void run() {
    	    DataSourceManager.importData(mAccount, mDataSource);
    	}
    }
}
