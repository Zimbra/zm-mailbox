package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetIMAPRecentRequest;
import com.zimbra.soap.mail.message.GetIMAPRecentResponse;

public class GetIMAPRecent extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_ID };
    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        GetIMAPRecentRequest req = zsc.elementToJaxb(request);
        ItemIdentifier itemIdent = ItemIdentifier.fromOwnerAndRemoteId(mbox.getAccountId(), req.getId());
        if (!mbox.getAccountId().equals(itemIdent.accountId)) {
            throw MailServiceException.NO_SUCH_FOLDER(req.getId());
        }
        return zsc.jaxbToElement(new GetIMAPRecentResponse(mbox.getImapRecent(octxt, itemIdent.id)));
    }
}
