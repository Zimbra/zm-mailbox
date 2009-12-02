package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class SetMailboxMetadata extends MailDocumentHandler {

    private static final int TOTAL_METADATA_LIMIT = 10000;
    
    private static enum SectionNames {
        zwc, bes
    }
    
    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element meta = request.getElement(MailConstants.E_METADATA);
        String section = meta.getAttribute(MailConstants.A_SECTION);
        section = section.trim();
        if (section.length() == 0 || section.length() > 36 || section.indexOf(':') < 1)
            throw ServiceException.INVALID_REQUEST("invalid mailbox metadata section name", null);
        SectionNames.valueOf(section.substring(0, section.indexOf(':'))); //will throw IllegalArgumentException if not known

        Metadata metadata = null;
        int roughSize = 0;
        List<Element.KeyValuePair> keyvals = meta.listKeyValuePairs();
        if (!keyvals.isEmpty()) {
            metadata = new Metadata();
            for (Element.KeyValuePair kvp : keyvals) {
                roughSize += kvp.getKey().length() + kvp.getValue().length();
                if (roughSize > TOTAL_METADATA_LIMIT)
                    throw MailServiceException.TOO_MUCH_METADATA(TOTAL_METADATA_LIMIT);
                metadata.put(kvp.getKey(), kvp.getValue());
            }
        }
        mbox.setConfig(octxt, section, metadata);

        Element response = zsc.createElement(MailConstants.SET_MAILBOX_METADATA_RESPONSE);
        return response;
    }
}
