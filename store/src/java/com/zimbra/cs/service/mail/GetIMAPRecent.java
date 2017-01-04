package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.mail.MailDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetIMAPRecentRequest;
import com.zimbra.soap.mail.message.GetIMAPRecentResponse;

public class GetIMAPRecent extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Mailbox mbox = getRequestedMailbox(zsc);
	    OperationContext octxt = getOperationContext(zsc, context);
	    GetIMAPRecentRequest req = zsc.elementToJaxb(request);
	    return zsc.jaxbToElement(new GetIMAPRecentResponse(mbox.getImapRecent(octxt, Integer.parseInt(req.getId()))));
	}

}
