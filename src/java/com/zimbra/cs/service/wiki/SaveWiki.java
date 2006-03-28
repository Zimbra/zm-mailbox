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
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
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
		Wiki wiki = getRequestedWiki(request, lc);

        Element msgElem = request.getElement(MailService.E_WIKIWORD);
        String subject = msgElem.getAttribute(MailService.A_NAME, null);
        int fid = (int)msgElem.getAttributeLong(MailService.A_FOLDER, wiki.getWikiFolderId());

        Mailbox mbox = Mailbox.getMailboxByAccountId(wiki.getWikiAccountId());

        byte[] rawData = msgElem.getText().getBytes();
        
        WikiItem wikiItem;
        synchronized (wiki) {
            WikiWord ww = wiki.lookupWiki(subject);
            if (ww == null) {
                wikiItem = mbox.createWiki(octxt, fid, subject, getAuthor(lc), rawData, null);
            } else {
                Document doc = ww.getWikiItem(octxt);
                if (doc.getType() != MailItem.TYPE_WIKI) {
                	throw ServiceException.FAILURE("requested MailItem is not WikiItem", null);
                }
            	wikiItem = (WikiItem)doc;
            	mbox.addDocumentRevision(octxt, wikiItem, rawData, getAuthor(lc));
            }
    		wiki.addWiki(wikiItem);
        }
        
        Element response = lc.createElement(MailService.SAVE_WIKI_RESPONSE);
        Element m = response.addElement(MailService.E_WIKIWORD);
        m.addAttribute(MailService.A_ID, lc.formatItemId(wikiItem));
        return response;
	}

}
