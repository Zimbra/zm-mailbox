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

import java.io.IOException;
import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class SaveWiki extends WikiDocumentHandler {

	@Override
    public boolean isReadOnly() {
        return false;
    }
    
	@Override
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException, SoapFaultException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        OperationContext octxt = lc.getOperationContext();
		Wiki wiki = getRequestedWikiNotebook(request, lc);

        Element msgElem = request.getElement(MailService.E_WIKIWORD);
        String subject = msgElem.getAttribute(MailService.A_NAME, null);
        String id = msgElem.getAttribute(MailService.A_ID, null);
        int itemId;
        if (id == null) {
        	itemId = 0;
        } else {
        	ItemId iid = new ItemId(id, lc);
        	itemId = iid.getId();
        }

        validateRequest(wiki,
        				itemId,
        				msgElem.getAttributeLong(MailService.A_VERSION, 0),
        				subject);
        
        byte[] rawData;
        try {
        	rawData = msgElem.getText().getBytes("UTF-8");
        } catch (IOException ioe) {
        	throw ServiceException.FAILURE("cannot convert", ioe);
        }
        
        WikiContext ctxt = new WikiContext(octxt, lc.getRawAuthToken());
        WikiPage ww = wiki.createWiki(ctxt, subject, getAuthor(lc), rawData);
        Document wikiItem = ww.getWikiItem(ctxt);
        
        Element response = lc.createElement(MailService.SAVE_WIKI_RESPONSE);
        Element m = response.addElement(MailService.E_WIKIWORD);
        m.addAttribute(MailService.A_ID, lc.formatItemId(wikiItem));
        m.addAttribute(MailService.A_VERSION, wikiItem.getVersion());
        return response;
	}
}
