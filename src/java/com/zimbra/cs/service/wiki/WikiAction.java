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

package com.zimbra.cs.service.wiki;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.mail.ItemAction;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class WikiAction extends ItemAction {

	public static final String OP_RENAME = "rename";
	
	public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        String successes;
        if (operation.equals(OP_RENAME)) {
    		Account author = lc.getAuthtokenAccount();
    		String id = action.getAttribute(MailService.A_ID);
    		if (id.indexOf(",") > 0)
    			throw WikiServiceException.ERROR("cannot use more than one id for rename");
    		String name = action.getAttribute(MailService.A_NAME);
    		WikiContext ctxt = new WikiContext(lc.getOperationContext(), lc.getRawAuthToken());
    		Wiki wiki = Wiki.getInstance(ctxt, author.getId());
    		wiki.renameDocument(ctxt, Integer.parseInt(id), name, author.getName());
    		successes = id;
        } else {
        	successes = handleCommon(context, request, operation, MailItem.TYPE_WIKI);
        }
        
        Element response = lc.createElement(MailService.WIKI_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
	}
}
