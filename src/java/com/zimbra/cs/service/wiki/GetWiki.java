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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetWiki extends WikiDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        OperationContext octxt = lc.getOperationContext();
        Element wElem = request.getElement(MailService.E_WIKIWORD);
        String word = wElem.getAttribute(MailService.A_NAME, null);
        String id = wElem.getAttribute(MailService.A_ID, null);
        int traverse = (int)wElem.getAttributeLong(MailService.A_TRAVERSE, 0);
        int rev = (int)wElem.getAttributeLong(MailService.A_VERSION, -1);
        int count = (int)wElem.getAttributeLong(MailService.A_COUNT, -1);

        Element response = lc.createElement(MailService.GET_WIKI_RESPONSE);

        WikiItem wikiItem;
        
        if (word != null) {
        	ItemId fid = getRequestedFolder(request);
        	if (fid == null)
        		fid = new ItemId("", Mailbox.ID_FOLDER_USER_ROOT);
        	MailItem item = Wiki.findWikiByPath(new WikiContext(octxt, lc.getRawAuthToken()), 
        										 lc.getRequestedAccountId(), 
        										 fid.getId(), 
        										 word,
        										 traverse == 1);
        	if (item == null) {
        		throw new WikiServiceException.NoSuchWikiException(word);
        	}
        	if (item.getType() != MailItem.TYPE_WIKI) {
        		throw WikiServiceException.NOT_WIKI_ITEM(word);
        	}
            wikiItem = (WikiItem)item;
        } else if (id != null) {
        	ItemId iid = new ItemId(id, lc);
        	Mailbox mbox = getRequestedMailbox(lc);
        	wikiItem = mbox.getWikiById(octxt, iid.getId());
        } else {
        	throw ServiceException.FAILURE("missing attribute w or id", null);
        }
        
        Element wikiElem = ToXML.encodeWiki(response, lc, wikiItem, rev);
        
        if (count > 1) {
    		count--;  // head version was already printed
        	if (rev <= 0) {
        		rev = wikiItem.getVersion();
        	}
        	while (--rev > 0) {
                ToXML.encodeWiki(response, lc, wikiItem, rev);
        		count--;
        		if (count == 0) {
        			break;
        		}
        	}
        } else {
        	Document.DocumentRevision revision = (rev > 0) ? wikiItem.getRevision(rev) : wikiItem.getLastRevision(); 
        	try {
        		byte[] raw = ByteUtil.getContent(revision.getContent(), 0);
        		wikiElem.addAttribute(MailService.A_BODY, new String(raw, "UTF-8"), Element.DISP_CONTENT);
        	} catch (IOException ioe) {
        		ZimbraLog.wiki.error("cannot read the wiki message body", ioe);
        		throw WikiServiceException.CANNOT_READ(wikiItem.getWikiWord());
        	}
        }
        return response;
	}
}
