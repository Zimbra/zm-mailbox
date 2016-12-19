package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GetLastItemIdInMailboxResponse;


public class GetLastItemIdInMailbox extends AccountDocumentHandler {

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
