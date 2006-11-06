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

import com.zimbra.cs.mailbox.MailItemDataSource;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class TestDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        String host;
        int port;
        String username;
        String password;
        String connectionTypeString;
        
        // Parse request
        Element ePop3 = request.getElement(MailService.E_DS_POP3);
        if (attrExists(ePop3, MailService.A_ID)) {
            // Testing existing data source
            Mailbox mbox = getRequestedMailbox(zsc);
            OperationContext octxt = mbox.getOperationContext();
            int id = (int) ePop3.getAttributeLong(MailService.A_ID);
            
            // Read current values
            MailItemDataSource ds = mbox.getDataSource(octxt, id);
            host = ds.getHost();
            port = ds.getPort();
            username = ds.getUsername();
            password = ds.getPassword();
            connectionTypeString = ds.getConnectionTypeString();
            
            // Override with values in SOAP request
            if (attrExists(ePop3, MailService.A_DS_HOST)) {
                host = ePop3.getAttribute(MailService.A_DS_HOST);
            }
            if (attrExists(ePop3, MailService.A_DS_PORT)) {
                port = (int) ePop3.getAttributeLong(MailService.A_DS_PORT);
            }
            if (attrExists(ePop3, MailService.A_DS_CONNECTION_TYPE)) {
                connectionTypeString = ePop3.getAttribute(MailService.A_DS_CONNECTION_TYPE);
            }
            if (attrExists(ePop3, MailService.A_DS_USERNAME)) {
                username = ePop3.getAttribute(MailService.A_DS_USERNAME);
            }
            if (attrExists(ePop3, MailService.A_DS_PASSWORD)) {
                password = ePop3.getAttribute(MailService.A_DS_PASSWORD);
            }
        } else {
            // All attrs must be specified in SOAP request
            host = ePop3.getAttribute(MailService.A_DS_HOST);
            port = (int) ePop3.getAttributeLong(MailService.A_DS_PORT);
            connectionTypeString = ePop3.getAttribute(MailService.A_DS_CONNECTION_TYPE);
            username = ePop3.getAttribute(MailService.A_DS_USERNAME);
            password = ePop3.getAttribute(MailService.A_DS_PASSWORD);
        }
        
        // Perform test and assemble response
        Element response = zsc.createElement(MailService.TEST_DATA_SOURCE_RESPONSE);
        ePop3 = response.addElement(MailService.E_DS_POP3);
        MailItemDataSource.ConnectionType connectionType =
            MailItemDataSource.getConnectionType(connectionTypeString);
        
        String error = MailItemDataSource.test(
            MailItemDataSource.TYPE_POP3, host, port, connectionType, username, password);
        if (error == null) {
            ePop3.addAttribute(MailService.A_DS_SUCCESS, true);
        } else {
            ePop3.addAttribute(MailService.A_DS_SUCCESS, false);
            ePop3.addAttribute(MailService.A_DS_ERROR, error);
        }
        
        return response;
    }
    
    private boolean attrExists(Element e, String attrName) {
        return (e.getAttribute(attrName, null) != null);
    }
}
