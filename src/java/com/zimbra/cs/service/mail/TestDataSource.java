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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.mailbox.DataSourceManager;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class TestDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account account = getRequestedAccount(zsc);

        Map<String, Object> testAttrs = new HashMap<String, Object>();
        String testId = "TestId";
        
        // Parse request
        Element ePop3 = request.getElement(MailService.E_DS_POP3);
        String id = ePop3.getAttribute(MailService.A_ID, null);
        if (id != null) {
            // Testing existing data source
            DataSource dsOrig = prov.get(account, DataSourceBy.id, id);
            Map<String, Object> origAttrs = dsOrig.getAttrs();
            for (String key : origAttrs.keySet()) {
                testAttrs.put(key, dsOrig.getAttr(key));
            }
        }

        // Get values from SOAP request.  If testing an existing data source,
        // values in the request override the persisted values.
        String value = ePop3.getAttribute(MailService.A_DS_HOST, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceHost, value);
        }
        value = ePop3.getAttribute(MailService.A_DS_PORT, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourcePort, value);
        }
        value = ePop3.getAttribute(MailService.A_DS_CONNECTION_TYPE, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, value);
        }
        value = ePop3.getAttribute(MailService.A_DS_USERNAME, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourceUsername, value);
        }
        value = ePop3.getAttribute(MailService.A_DS_PASSWORD, null);
        if (value != null) {
            testAttrs.put(Provisioning.A_zimbraDataSourcePassword, DataSource.encryptData(testId, value));
        }
        
        // Perform test and assemble response
        DataSource ds = new DataSource(DataSource.Type.pop3, "Test", "TestId", testAttrs);
        Element response = zsc.createElement(MailService.TEST_DATA_SOURCE_RESPONSE);
        ePop3 = response.addElement(MailService.E_DS_POP3);
        
        String error = DataSourceManager.test(ds);
        if (error == null) {
            ePop3.addAttribute(MailService.A_DS_SUCCESS, true);
        } else {
            ePop3.addAttribute(MailService.A_DS_SUCCESS, false);
            ePop3.addAttribute(MailService.A_DS_ERROR, error);
        }
        
        return response;
    }
}
