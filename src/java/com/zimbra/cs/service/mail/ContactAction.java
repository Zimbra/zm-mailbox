/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class ContactAction extends ItemAction {

	public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_READ) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on contact: " + operation, null);
        String successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_CONTACT);

        Element response = lc.createElement(MailService.CONTACT_ACTION_RESPONSE);
        Element actionOut = response.addUniqueElement(MailService.E_ACTION);
        actionOut.addAttribute(MailService.A_ID, successes);
        actionOut.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }
}
