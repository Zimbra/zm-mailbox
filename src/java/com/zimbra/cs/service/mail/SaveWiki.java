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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiWord;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraContext;

public class SaveWiki extends WriteOpDocumentHandler {

	@Override
	public Element handle(Element request, Map context)
			throws ServiceException, SoapFaultException {
		Wiki wiki = Wiki.getInstance();
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = Mailbox.getMailboxByAccountId(wiki.getWikiAccountId());
        OperationContext octxt = lc.getOperationContext();

        Element msgElem = request.getElement(MailService.E_MSG);
        String subject = msgElem.getAttribute(MailService.A_SUBJECT, null);
        int fid = (int)msgElem.getAttributeLong(MailService.A_FOLDER, wiki.getWikiFolderId());

        byte[] rawData = msgElem.getText().getBytes();
        
        WikiItem wikiItem;
        synchronized (wiki) {
            WikiWord ww = wiki.lookupWiki(subject);
            if (ww == null) {
                wikiItem = mbox.createWiki(octxt, fid, subject, rawData, null);
            } else {
            	wikiItem = ww.getWikiItem(octxt);
            	mbox.addDocumentRevision(octxt, wikiItem, rawData);
            }
    		wiki.addWiki(wikiItem);
        }
        
        Element response = lc.createElement(MailService.SAVE_WIKI_RESPONSE);
        Element m = response.addElement(MailService.E_MSG);
        m.addAttribute(MailService.A_ID, lc.formatItemId(wikiItem));
        return response;
	}

}
