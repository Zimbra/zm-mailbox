/*
 * Created on May 29, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class ItemAction extends WriteOpDocumentHandler {

    public static final String OP_TAG         = "tag";
    public static final String OP_FLAG        = "flag";
    public static final String OP_READ        = "read";
    public static final String OP_HARD_DELETE = "delete";
    public static final String OP_MOVE        = "move";
    public static final String OP_SPAM        = "spam";
    public static final String OP_UPDATE      = "update";

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        String successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_UNKNOWN);

        Element response = lc.createElement(MailService.ITEM_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }

    String handleCommon(OperationContext octxt, String opAttr, Element action, Mailbox mbox, byte type) throws ServiceException {
        String op;
        boolean flagValue = true;
        if (opAttr.length() > 1 && opAttr.startsWith("!")) {
            flagValue = false;
            op = opAttr.substring(1);
        } else
            op = opAttr;
        String[] targets = action.getAttribute(MailService.A_ID).split(",");
        StringBuffer successes = new StringBuffer();

        String constraint = action.getAttribute(MailService.A_TARGET_CONSTRAINT, null);
        TargetConstraint tcon = TargetConstraint.parseConstraint(mbox, constraint);

        int id;
        for (int i = 0; i < targets.length; i++) {
            try {
                id = Integer.parseInt(targets[i].trim());
            } catch (NumberFormatException nfe) {
                throw ServiceException.INVALID_REQUEST("error in syntax of message ID list", null);
            }
            if (op.equals(OP_FLAG))
                mbox.alterTag(octxt, id, type, Flag.ID_FLAG_FLAGGED, flagValue, tcon);
            else if (op.equals(OP_READ))
                mbox.alterTag(octxt, id, type, Flag.ID_FLAG_UNREAD, !flagValue, tcon);
            else if (op.equals(OP_TAG)) {
                int tagId = (int) action.getAttributeLong(MailService.A_TAG);
                mbox.alterTag(octxt, id, type, tagId, flagValue, tcon);
            } else if (op.equals(OP_HARD_DELETE))
                mbox.delete(octxt, id, type, tcon);
            else if (op.equals(OP_MOVE)) {
                int folderId = (int) action.getAttributeLong(MailService.A_FOLDER);
                mbox.move(octxt, id, type, folderId, tcon);
            } else if (op.equals(OP_SPAM)) {
                int defaultFolder = flagValue ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX;
                int folderId = (int) action.getAttributeLong(MailService.A_FOLDER, defaultFolder);
                mbox.move(octxt, id, type, folderId, tcon);
                // XXX: send items to spamfilter for training purposes
            } else if (op.equals(OP_UPDATE)) {
                int folderId = (int) action.getAttributeLong(MailService.A_FOLDER, -1);
                String flags = action.getAttribute(MailService.A_FLAGS, null);
                String tags  = action.getAttribute(MailService.A_TAGS, null);
                if (folderId > 0)
                    mbox.move(octxt, id, type, folderId, tcon);
                if (tags != null || flags != null)
                    mbox.setTags(octxt, id, type, flags, tags, tcon);
            } else
                throw ServiceException.INVALID_REQUEST("unknown operation: " + op, null);

            if (successes.length() > 0)
                successes.append(',');
            successes.append(id);
        }

        return successes.toString();
    }
}
