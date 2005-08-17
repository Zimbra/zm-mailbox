/*
 * Created on Jun 25, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;

/**
 * @author dkarp
 */
public class ConvAction extends ItemAction {

    public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_UPDATE))
            throw ServiceException.INVALID_REQUEST("invalid operation on conv: " + operation, null);
        String successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_CONVERSATION);

        Element response = lc.createElement(MailService.CONV_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }
}
