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
 * Created on Sep 8, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.operation.GetNoteOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class GetNote extends MailDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		Mailbox mbox = getRequestedMailbox(lc);
		Mailbox.OperationContext octxt = lc.getOperationContext();
		Session session = getSession(context);
		
		Element enote = request.getElement(MailService.E_NOTE);
		int noteId = (int) enote.getAttributeLong(MailService.A_ID);
		
		GetNoteOperation op = new GetNoteOperation(session, octxt, mbox, Requester.SOAP, noteId);
		op.schedule();
		Note note = op.getResult();
		
		if (note == null)
			throw MailServiceException.NO_SUCH_NOTE(noteId);
		
		Element response = lc.createElement(MailService.GET_NOTE_RESPONSE);
		ToXML.encodeNote(response, lc, note);
		return response;
	}
}
