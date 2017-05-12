package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.IMAPCopyRequest;
import com.zimbra.soap.mail.message.IMAPCopyResponse;
import com.zimbra.soap.mail.type.IMAPItemInfo;

public class ImapCopy extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        IMAPCopyRequest req = zsc.elementToJaxb(request);
        String[] stringIds = req.getIds().split(",");
        List<Integer> itemIds = Lists.newArrayList(); 
        for(String id : stringIds) {
            try {
                itemIds.add(Integer.parseInt(id));
            } catch (Exception e) {
                throw ServiceException.INVALID_REQUEST("Invalid item ids", e);
            }
        }
        String typeString = req.getType();
        List<MailItem> items = mbox.imapCopy(octxt, Ints.toArray(itemIds), MailItem.Type.valueOf(typeString), req.getFolder());
        IMAPCopyResponse resp = new IMAPCopyResponse();
        for(MailItem item : items) {
            resp.addItem(new IMAPItemInfo(item.getId(), item.getImapUid()));
        }
        return zsc.jaxbToElement(resp);
    }

}
