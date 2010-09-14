/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ListDocumentRevisions extends WikiDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element doc = request.getElement(MailConstants.E_DOC);
        String id = doc.getAttribute(MailConstants.A_ID);
        int version = (int) doc.getAttributeLong(MailConstants.A_VERSION, -1);
        int count = (int) doc.getAttributeLong(MailConstants.A_COUNT, 1);

        Element response = zsc.createElement(MailConstants.LIST_DOCUMENT_REVISIONS_RESPONSE);

        Document item;

        ItemId iid = new ItemId(id, zsc);
        item = mbox.getDocumentById(octxt, iid.getId());
        
        byte view = mbox.getFolderById(octxt, item.getFolderId()).getDefaultView();
        if (view == MailItem.TYPE_WIKI)
    		checkNotebookEnabled(zsc);
        else if (view == MailItem.TYPE_DOCUMENT)
    		checkBriefcaseEnabled(zsc);

        if (version < 0)
        	version = item.getVersion();
        byte type = item.getType();
        while (version > 0 && count > 0) {
        	item = (Document) mbox.getItemRevision(octxt, iid.getId(), type, version);
        	if (item != null)
        	    ToXML.encodeDocument(response, ifmt, octxt, item);
        	version--; count--;
        }

        return response;
	}
}
