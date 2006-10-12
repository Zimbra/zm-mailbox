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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Identity;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class IdentityAction extends ItemAction {
	public static final String OP_CREATE  = "create";
	
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        String successes = null;
        if (operation.equals(OP_CREATE)) {
        	Element identity = action.getElement(MailService.E_IDENTITY);
        	successes = Identity.create(getRequestedAccount(zsc), zsc.getOperationContext(), identity).getId();
        } else if (operation.equals(OP_UPDATE)) {
        	String id = action.getAttribute(MailService.A_ID);
        	Element identity = action.getElement(MailService.E_IDENTITY);
        	successes = Identity.update(id, getRequestedAccount(zsc), zsc.getOperationContext(), identity, false).getId();
        } else if (operation.equals(OP_HARD_DELETE)) {
        	String id = action.getAttribute(MailService.A_ID);
        	successes = Identity.update(id, getRequestedAccount(zsc), zsc.getOperationContext(), null, true).getId();
        }
        
        Element response = zsc.createElement(MailService.IDENTITY_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }
}
