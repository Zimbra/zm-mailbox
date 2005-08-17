/*
 * Created on Sep 8, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class NoteAction extends ItemAction {

	public static final String OP_EDIT       = "edit";
	public static final String OP_REPOSITION = "pos";
	public static final String OP_COLOR      = "color";

	public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_READ) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on note: " + operation, null);
        String successes;
        if (operation.equals(OP_EDIT) || operation.equals(OP_REPOSITION) || operation.equals(OP_COLOR))
            successes = handleNote(octxt, operation, action, mbox);
        else
            successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_NOTE);

        Element response = lc.createElement(MailService.NOTE_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
	}

    private String handleNote(OperationContext octxt, String operation, Element action, Mailbox mbox)
    throws ServiceException {
        int id = (int) action.getAttributeLong(MailService.A_ID);

        if (operation.equals(OP_EDIT)) {
            String content = action.getAttribute(MailService.E_CONTENT);
            mbox.editNote(octxt, id, content);
        } else if (operation.equals(OP_REPOSITION)) {
            String strBounds = action.getAttribute(MailService.A_BOUNDS, null);
            mbox.repositionNote(octxt, id, new Rectangle(strBounds));
        } else if (operation.equals(OP_COLOR)) {
            byte color = (byte) action.getAttributeLong(MailService.A_COLOR, Note.DEFAULT_COLOR);
            mbox.colorNote(octxt, id, color);
        } else
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);

        return Integer.toString(id);
    }
}
