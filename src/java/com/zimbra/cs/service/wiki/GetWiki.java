/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.wiki;

import java.io.IOException;
import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetWiki extends WikiDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        if (true)
            throw WikiServiceException.ERROR("Deprecated");
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		checkNotebookEnabled(zsc);
		Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element wElem = request.getElement(MailConstants.E_WIKIWORD);
        String word = wElem.getAttribute(MailConstants.A_NAME, null);
        String id = wElem.getAttribute(MailConstants.A_ID, null);
        int traverse = (int)wElem.getAttributeLong(MailConstants.A_TRAVERSE, 0);
        int version = (int) wElem.getAttributeLong(MailConstants.A_VERSION, -1);
        int count = (int) wElem.getAttributeLong(MailConstants.A_COUNT, -1);

        Element response = zsc.createElement(MailConstants.GET_WIKI_RESPONSE);

        WikiItem wikiItem;

        if (word != null) {
        	ItemId fid = getRequestedFolder(request, zsc);
            MailItem item = mbox.getItemByPath(octxt, word, fid.getId());
            if (!(item instanceof WikiItem))
                throw WikiServiceException.NOT_WIKI_ITEM(word);
            wikiItem = (WikiItem) item;
        } else if (id != null) {
        	ItemId iid = new ItemId(id, zsc);
        	wikiItem = mbox.getWikiById(octxt, iid.getId());
        } else {
        	throw ServiceException.FAILURE("missing attribute w or id", null);
        }

        WikiItem rev = wikiItem;
        if (version > 0)
        	rev = (WikiItem) mbox.getItemRevision(octxt, wikiItem.getId(), wikiItem.getType(), version);
        Element wikiElem = ToXML.encodeWiki(response, ifmt, octxt, rev);
        
        if (count > 1) {
        	if (version <= 0)
        		version = wikiItem.getVersion();
        	while (--version > 0 && --count > 0) {
            	rev = (WikiItem) mbox.getItemRevision(octxt, wikiItem.getId(), wikiItem.getType(), version);
                ToXML.encodeWiki(response, ifmt, octxt, rev);
        	}
        } else {
        	Document revision = (version > 0 ? (Document) mbox.getItemRevision(octxt, wikiItem.getId(), wikiItem.getType(), version) : wikiItem); 
        	try {
        		// when the revisions get pruned after each save, the contents of
        		// old revision is gone, and revision.getContent() returns null.
        		if (revision != null) {
        			byte[] raw = revision.getContent();
        			wikiElem.addAttribute(MailConstants.A_BODY, new String(raw, "UTF-8"), Element.Disposition.CONTENT);
        		}
        	} catch (IOException ioe) {
        		ZimbraLog.wiki.error("cannot read the wiki message body", ioe);
        		throw WikiServiceException.CANNOT_READ(wikiItem.getWikiWord());
        	}
        }
        return response;
	}
}
