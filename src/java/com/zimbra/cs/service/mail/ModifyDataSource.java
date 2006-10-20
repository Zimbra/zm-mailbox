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


public class ModifyDataSource extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        
        Element ePop3 = request.getElement(MailService.E_DS_POP3);
        String name = ePop3.getAttribute(MailService.A_NAME);
        MailItemDataSource ds = MailItemDataSource.get(mbox, zsc.getOperationContext(), name);
        
        String attr = ePop3.getAttribute(MailService.A_DS_IS_ENABLED, null);
        if (attr != null)
        	ds.setEnabled(Element.parseBool(MailService.A_DS_IS_ENABLED, attr));
        attr = ePop3.getAttribute(MailService.A_FOLDER, null);
        if (attr != null)
        	ds.setFolderId((int)Element.parseLong(MailService.A_FOLDER, attr));
        attr = ePop3.getAttribute(MailService.A_DS_HOST, null);
        if (attr != null)
        	ds.setHost(attr);
        attr = ePop3.getAttribute(MailService.A_DS_PORT, null);
        if (attr != null)
        	ds.setPort((int)Element.parseLong(MailService.A_DS_PORT, attr));
        attr = ePop3.getAttribute(MailService.A_DS_USERNAME, null);
        if (attr != null)
        	ds.setUsername(attr);
        attr = ePop3.getAttribute(MailService.A_DS_PASSWORD, null);
        if (attr != null)
        	ds.setPassword(attr);
        MailItemDataSource.modify(mbox, zsc.getOperationContext(), ds);
        
        Element response = zsc.createElement(MailService.MODIFY_DATA_SOURCE_RESPONSE);

        return response;
    }
}
