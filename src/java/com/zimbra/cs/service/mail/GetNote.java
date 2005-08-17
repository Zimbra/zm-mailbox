/*
 * Created on Sep 8, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Note;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.LiquidContext;

/**
 * @author dkarp
 */
public class GetNote extends DocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element enote = request.getElement(MailService.E_NOTE);
        int noteId = (int) enote.getAttributeLong(MailService.A_ID);

        Note note = mbox.getNoteById(noteId);
        if (note == null)
        	throw MailServiceException.NO_SUCH_NOTE(noteId);

        Element response = lc.createElement(MailService.GET_NOTE_RESPONSE);
        ToXML.encodeNote(response, note);
        return response;
	}
}
