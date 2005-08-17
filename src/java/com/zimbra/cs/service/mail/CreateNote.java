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
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class CreateNote extends WriteOpDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
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
