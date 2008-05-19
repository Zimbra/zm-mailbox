/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
import java.util.Collection;
import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.wiki.Diff;
import com.zimbra.cs.wiki.Diff.Chunk;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class DiffDocument extends WikiDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		checkEnabled(zsc);
		Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element doc = request.getElement(MailConstants.E_DOC);
        String idStr = doc.getAttribute(MailConstants.A_ID, null);
        int v1 = (int) doc.getAttributeLong(MailConstants.A_V1, -1);
        int v2 = (int) doc.getAttributeLong(MailConstants.A_V2, -1);

        ItemId id = new ItemId(idStr, zsc);
    	Document r1 = (Document) mbox.getItemRevision(octxt, id.getId(), MailItem.TYPE_UNKNOWN, v1);
    	Document r2 = (Document) mbox.getItemRevision(octxt, id.getId(), MailItem.TYPE_UNKNOWN, v2);
    	
        Element response = zsc.createElement(MailConstants.DIFF_DOCUMENT_RESPONSE);
        
    	try {
        	Collection<Chunk> diffResult = Diff.getResult(r1.getContentStream(), r2.getContentStream());
        	for (Chunk c : diffResult) {
        		Element chunk = response.addElement(MailConstants.E_CHUNK);
    			chunk.addAttribute(MailConstants.A_DISP, c.disposition.toString());
    			chunk.setText(StringUtil.join("\n", c.content));
        	}
    	} catch (IOException e) {
			throw ServiceException.FAILURE("can't diff documents", e);
    	}
    	
        return response;
	}
}
