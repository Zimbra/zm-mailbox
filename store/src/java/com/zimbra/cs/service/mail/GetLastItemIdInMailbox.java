package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetLastItemIdInMailboxResponse;


public class GetLastItemIdInMailbox extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        int lastItemId = mbox.getLastItemId();
        GetLastItemIdInMailboxResponse resp = new GetLastItemIdInMailboxResponse();
        resp.setId(lastItemId);
        return zsc.jaxbToElement(resp);
    }
}
