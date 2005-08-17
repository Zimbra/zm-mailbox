/*
 * Created on Jul 8, 2004
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
 * @author dkarp
 */
public class MsgAction extends ItemAction {

	public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        String successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_MESSAGE);

        Element response = lc.createElement(MailService.MSG_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
	}
}
