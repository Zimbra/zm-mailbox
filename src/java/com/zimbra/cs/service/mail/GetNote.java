/*
 * Created on Sep 8, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class GetNote extends DocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
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
