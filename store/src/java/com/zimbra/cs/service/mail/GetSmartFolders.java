package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SmartFolder;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetSmartFoldersResponse;
import com.zimbra.soap.mail.type.TagInfo;

public class GetSmartFolders extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        List<SmartFolder> smartFolders = new ArrayList<>();
        for (SmartFolder sf: mbox.getSmartFolders(octxt)) {
            smartFolders.add(sf);
        }
        GetSmartFoldersResponse resp = new GetSmartFoldersResponse();
        for (SmartFolder sf: smartFolders) {
            TagInfo ti = new TagInfo(String.valueOf(sf.getId()));
            ti.setName(sf.getSmartFolderName());
            ti.setUnread(sf.getUnreadCount());
            ti.setCount(sf.getItemCount());
            resp.addSmartFolder(ti);
        }
        return zsc.jaxbToElement(resp);
    }
}
