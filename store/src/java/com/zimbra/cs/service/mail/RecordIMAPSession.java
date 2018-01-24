package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RecordIMAPSessionResponse;

/**
 * Record that an IMAP client has seen all the messages in this folder as they are at this time.
 * This is used to determine which messages are considered by IMAP to be RECENT
 */
public class RecordIMAPSession extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_ID };
    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        com.zimbra.soap.mail.message.RecordIMAPSessionRequest req = zsc.elementToJaxb(request);
        String folderId = req.getFolderId();
        Mailbox mbox = getRequestedMailbox(zsc);
        ItemIdentifier folderIdent = ItemIdentifier.fromEncodedAndDefaultAcctId(folderId, mbox.getAccountId());
        mbox.recordImapSession(folderIdent.id);

        int lastItemId = mbox.getLastItemId();
        Folder folder = mbox.getFolderById(null, folderIdent.id);

        RecordIMAPSessionResponse resp = new RecordIMAPSessionResponse(lastItemId, folder.getUuid());
        return zsc.jaxbToElement(resp);
    }
}
