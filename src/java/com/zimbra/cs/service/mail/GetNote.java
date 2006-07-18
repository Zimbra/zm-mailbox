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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 8, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.operation.GetNoteOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
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
