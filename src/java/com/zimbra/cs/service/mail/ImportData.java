/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.mailbox.DataSourceManager;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class ImportData extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);

        for (Element elem : request.listElements()) {
        	if (elem.getName().equals(MailService.E_DS_POP3)) {
        		try {
        			String id = elem.getAttribute(MailService.A_ID);
                    DataSource ds = prov.get(account, DataSourceBy.id, id);
        			new Thread(new Pop3Client(account, ds)).start();
        		} catch (Exception e) {
        			ZimbraLog.account.error("error handling ImportData", e);
        			// then continue onto the next pop3 element.
        			// we may need to propogate the error back to the client
        			// for reporting.
        		}
        	}
        }
        
        Element response = zsc.createElement(MailService.IMPORT_DATA_RESPONSE);
        return response;
    }
    
    private static class Pop3Client implements Runnable {
        Account mAccount;
    	DataSource mDataSource;
        
    	public Pop3Client(Account account, DataSource ds) {
            mAccount = account;
    		mDataSource = ds;
    	}
    	public void run() {
    	    DataSourceManager.importData(mAccount, mDataSource);
    	}
    }
}
