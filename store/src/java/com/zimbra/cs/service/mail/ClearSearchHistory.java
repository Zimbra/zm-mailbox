package com.zimbra.cs.service.mail;

import java.util.Map;


import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.ClearSearchHistoryResponse;

public class ClearSearchHistory extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
    ZimbraSoapContext zsc = getZimbraSoapContext(context);
    Mailbox mbox = getRequestedMailbox(zsc);
    OperationContext octxt = getOperationContext(zsc, context);
    mbox.deleteSearchHistory(octxt);
    return zsc.jaxbToElement(new ClearSearchHistoryResponse());
    }

}
