/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.mailbox.Conversation;
import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetConv extends DocumentHandler  {
    
    public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element econv = request.getElement(MailService.E_CONV);
        int id = (int) econv.getAttributeLong(MailService.A_ID);
        Conversation conv = mbox.getConversationById(id);

        Element response = lc.createElement(MailService.GET_CONV_RESPONSE);
        if (conv != null)
        	ToXML.encodeConversation(response, conv);
        else
            throw MailServiceException.NO_SUCH_CONV(id);
        return response;
    }
}
