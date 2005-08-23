/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class CreateNote extends WriteOpDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element t = request.getElement(MailService.E_NOTE);
        int folderId = (int) t.getAttributeLong(MailService.A_FOLDER);
        String content = t.getAttribute(MailService.E_CONTENT);
        byte color = (byte) t.getAttributeLong(MailService.A_COLOR, Note.DEFAULT_COLOR);
        String strBounds = t.getAttribute(MailService.A_BOUNDS, null);
        Rectangle bounds = new Rectangle(strBounds);

        Note note = mbox.createNote(null, content, bounds, color, folderId);

        Element response = lc.createElement(MailService.CREATE_NOTE_RESPONSE);
        if (note != null)
        	ToXML.encodeNote(response, note);
        return response;
	}
}
