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

/*
 * Created on Sep 8, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.operation.CreateNoteOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class CreateNote extends WriteOpDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_NOTE, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Mailbox.OperationContext octxt = lc.getOperationContext();
        Session session = getSession(context);

        Element t = request.getElement(MailService.E_NOTE);
        ItemId iidFolder = new ItemId(t.getAttribute(MailService.A_FOLDER), lc);
        String content = t.getAttribute(MailService.E_CONTENT);
        byte color = (byte) t.getAttributeLong(MailService.A_COLOR, MailItem.DEFAULT_COLOR);
        String strBounds = t.getAttribute(MailService.A_BOUNDS, null);
        Rectangle bounds = new Rectangle(strBounds);

        CreateNoteOperation op = new CreateNoteOperation(session, octxt, mbox, Requester.SOAP,
        			content, bounds, color, iidFolder);
        op.schedule();
        Note note = op.getNote();

        Element response = lc.createElement(MailService.CREATE_NOTE_RESPONSE);
        if (note != null)
        	ToXML.encodeNote(response, lc, note);
        return response;
	}
}
