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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.wiki;

import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiWord;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;

public class SaveWiki extends WikiDocumentHandler {

	@Override
    public boolean isReadOnly() {
        return false;
    }
    
	@Override
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException, SoapFaultException {
        ZimbraContext lc = getZimbraContext(context);
        OperationContext octxt = lc.getOperationContext();
		Wiki wiki = getRequestedWikiNotebook(request, lc);

        Element msgElem = request.getElement(MailService.E_WIKIWORD);
        String subject = msgElem.getAttribute(MailService.A_NAME, null);

        validateRequest(wiki,
        				(int)msgElem.getAttributeLong(MailService.A_ID, 0),
        				msgElem.getAttributeLong(MailService.A_VERSION, 0),
        				subject);
        
        byte[] rawData = msgElem.getText().getBytes();
        
        WikiWord ww = wiki.createWiki(octxt, subject, getAuthor(lc), rawData);
        Document wikiItem = ww.getWikiItem(octxt);
        
        Element response = lc.createElement(MailService.SAVE_WIKI_RESPONSE);
        Element m = response.addElement(MailService.E_WIKIWORD);
        m.addAttribute(MailService.A_ID, lc.formatItemId(wikiItem));
        m.addAttribute(MailService.A_VERSION, wikiItem.getVersion());
        return response;
	}
}
