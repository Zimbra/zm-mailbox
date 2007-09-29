/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.GetItemListOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetTag extends MailDocumentHandler  {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		Mailbox mbox = getRequestedMailbox(lc);
		OperationContext octxt = lc.getOperationContext();
		Session session = getSession(context);
        
		GetItemListOperation op = new GetItemListOperation(session, octxt, mbox, Requester.SOAP, MailItem.TYPE_TAG);
		op.schedule();
		List tags = op.getResults();

		Element response = lc.createElement(MailService.GET_TAG_RESPONSE);
		if (tags != null) {
			for (Iterator it = tags.iterator(); it.hasNext(); ) {
				Tag tag = (Tag) it.next();
				if (tag == null || tag instanceof Flag)
					continue;
				ToXML.encodeTag(response, lc, tag);
			}
		}
		return response;
	}
}
