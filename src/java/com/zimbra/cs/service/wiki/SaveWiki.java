/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.wiki;

import java.io.IOException;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.soap.ZimbraSoapContext;

public class SaveWiki extends WikiDocumentHandler {

	@Override
    public boolean isReadOnly() {
        return false;
    }
    
	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);

        Element msgElem = request.getElement(MailConstants.E_WIKIWORD);
        String subject = msgElem.getAttribute(MailConstants.A_NAME, null);
        String id = msgElem.getAttribute(MailConstants.A_ID, null);
        int ver = (int)msgElem.getAttributeLong(MailConstants.A_VERSION, 0);
        int itemId;
        if (id == null) {
        	itemId = 0;
        } else {
        	ItemId iid = new ItemId(id, zsc);
        	itemId = iid.getId();
        }

        byte[] rawData;
        try {
        	rawData = msgElem.getText().getBytes("UTF-8");
        } catch (IOException ioe) {
        	throw ServiceException.FAILURE("cannot convert", ioe);
        }
        WikiContext ctxt = new WikiContext(octxt, zsc.getAuthToken());
        WikiPage page = WikiPage.create(subject, getAuthor(zsc), rawData);
        Wiki.addPage(ctxt, page, itemId, ver, getRequestedFolder(request, zsc));
        Document wikiItem = page.getWikiItem(ctxt);
        
        Element response = zsc.createElement(MailConstants.SAVE_WIKI_RESPONSE);
        Element m = response.addElement(MailConstants.E_WIKIWORD);
        m.addAttribute(MailConstants.A_ID, new ItemIdFormatter(zsc).formatItemId(wikiItem));
        m.addAttribute(MailConstants.A_VERSION, wikiItem.getVersion());
        return response;
	}
}
