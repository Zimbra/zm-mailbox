/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Sep 8, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class GetNote extends MailDocumentHandler {

    private static final String[] TARGET_NOTE_PATH = new String[] { MailConstants.E_NOTE, MailConstants.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_NOTE_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Mailbox mbox = getRequestedMailbox(zsc);
		Mailbox.OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
		
		Element enote = request.getElement(MailConstants.E_NOTE);
        ItemId iid = new ItemId(enote.getAttribute(MailConstants.A_ID), zsc);

		Note note = mbox.getNoteById(octxt, iid.getId());

		if (note == null)
			throw MailServiceException.NO_SUCH_NOTE(iid.getId());
		
		Element response = zsc.createElement(MailConstants.GET_NOTE_RESPONSE);
		ToXML.encodeNote(response, ifmt, note);
		return response;
	}
}
