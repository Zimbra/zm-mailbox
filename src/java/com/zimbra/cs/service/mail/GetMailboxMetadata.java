package com.zimbra.cs.service.mail;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMailboxMetadata extends MailDocumentHandler {

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element meta = request.getElement(MailConstants.E_METADATA);
        String section = meta.getAttribute(MailConstants.A_SECTION);
        Metadata metadata = mbox.getConfig(octxt, section);

        Element response = zsc.createElement(MailConstants.GET_MAILBOX_METADATA_RESPONSE);
        meta = response.addElement(MailConstants.E_METADATA);
        meta.addAttribute(MailConstants.A_SECTION, section);
        
        if (metadata != null) {
            Set<Map.Entry<Object, Object>> entries = metadata.asMap().entrySet();
            for (Map.Entry<Object, Object> entry : entries)
                meta.addKeyValuePair(entry.getKey().toString(), entry.getValue().toString());
        }
        
        return response;
    }
}
