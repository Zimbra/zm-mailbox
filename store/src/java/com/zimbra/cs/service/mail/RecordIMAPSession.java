package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RecordIMAPSessionResponse;


public class RecordIMAPSession extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        com.zimbra.soap.mail.message.RecordIMAPSessionRequest req = zsc.elementToJaxb(request);
        int folderId = req.getFolderId();
        Mailbox mbox = getRequestedMailbox(zsc);
        mbox.recordImapSession(folderId);

        int lastItemId = mbox.getLastItemId();
        Folder folder = mbox.getFolderById(null, folderId);

        RecordIMAPSessionResponse resp = new RecordIMAPSessionResponse(lastItemId, folder.getUuid());
        return zsc.jaxbToElement(resp);
    }
}
