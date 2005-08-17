/*
 * Created on Sep 8, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Note;
import com.liquidsys.coco.mailbox.Note.Rectangle;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;
import com.liquidsys.soap.WriteOpDocumentHandler;

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
