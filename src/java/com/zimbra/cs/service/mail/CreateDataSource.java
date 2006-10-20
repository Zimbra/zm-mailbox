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
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        
        // Create the MailItemDataSource.  Currently only POP3 is supported.
        Element ePop3 = request.getElement(MailService.E_DS_POP3);
        String name = ePop3.getAttribute(MailService.A_NAME);
        int folderId = (int) ePop3.getAttributeLong(MailService.A_FOLDER);
        MailItemDataSource ds =
            new MailItemDataSource(mbox.getId(), MailItemDataSource.TYPE_POP3, name, folderId);
        ds.setEnabled(ePop3.getAttributeBool(MailService.A_DS_IS_ENABLED));
        ds.setHost(ePop3.getAttribute(MailService.A_DS_HOST));
        ds.setPort((int) ePop3.getAttributeLong(MailService.A_DS_PORT));
        ds.setUsername(ePop3.getAttribute(MailService.A_DS_USERNAME));
        ds.setPassword(ePop3.getAttribute(MailService.A_DS_PASSWORD));
        MailItemDataSource.create(mbox, zsc.getOperationContext(), ds);
        
        Element response = zsc.createElement(MailService.CREATE_DATA_SOURCE_RESPONSE);
        return response;
    }
}
