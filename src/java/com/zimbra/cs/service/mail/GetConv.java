/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetConv extends DocumentHandler  {
    
    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
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
