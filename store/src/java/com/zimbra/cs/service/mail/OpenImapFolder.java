package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.OpenIMAPFolderRequest;
import com.zimbra.soap.mail.message.OpenIMAPFolderResponse;
import com.zimbra.soap.mail.type.ImapCursorInfo;
import com.zimbra.soap.mail.type.ImapMessageInfo;

public class OpenImapFolder extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        OpenIMAPFolderRequest req = zsc.elementToJaxb(request);
        OpenIMAPFolderResponse resp = new OpenIMAPFolderResponse();
        ItemId folderId = new ItemId(req.getFolderId(), zsc);
        if (!folderId.belongsTo(mbox)) {
            ZimbraLog.imap.info("Proxying request: OpenImapFolder folder=%s", folderId);
            return proxyRequest(request, context, folderId.getAccountId());
        }
        int limit = req.getLimit();
        ImapCursorInfo cursor = req.getCursor();
        Integer cursorMsgId = null;
        if (cursor != null) {
            cursorMsgId = new ItemId(cursor.getId(), mbox.getAccountId()).getId();
        }
        Pair<List<ImapMessage>, Boolean> openFolderResults = mbox.openImapFolder(octxt, folderId.getId(), limit,
                cursorMsgId);
        List<ImapMessage> msgs = openFolderResults.getFirst();
        boolean hasMore = openFolderResults.getSecond();
        Integer msgId = null;
        for (ImapMessage msg: msgs) {
            msgId = msg.getMsgId();
            int imapUid = msg.getImapUid();
            String type = msg.getType().toString();
            int flags = msg.getFlags();
            String tags = msg.getTags() == null ? null : Joiner.on(",").join(msg.getTags());
            ImapMessageInfo info = new ImapMessageInfo(msgId, imapUid, type, flags, tags);
            resp.addImapMessageInfo(info);
        }
        if (hasMore && msgId != null) {
             resp.setCursor(new ImapCursorInfo(msgId.toString()));
        }
        resp.setHasMore(hasMore);
        return zsc.jaxbToElement(resp);
    }

}
