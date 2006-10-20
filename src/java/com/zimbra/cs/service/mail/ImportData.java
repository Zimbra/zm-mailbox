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

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItemDataSource;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class ImportData extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);

        for (Element elem : request.listElements()) {
        	if (elem.getName().equals(MailService.E_DS_POP3)) {
        		try {
        			String name = elem.getAttribute(MailService.A_NAME);
        			MailItemDataSource ds = MailItemDataSource.get(mbox, zsc.getOperationContext(), name);
        			new Thread(new Pop3Client(ds)).start();
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
    	MailItemDataSource ds;
    	public Pop3Client(MailItemDataSource ds) {
    		this.ds = ds;
    	}
    	public void run() {
    		try {
    			ds.importData();
    		} catch (ServiceException se) {
    			ZimbraLog.account.error("pop3 client", se);
    		}
    	}
    }
}
