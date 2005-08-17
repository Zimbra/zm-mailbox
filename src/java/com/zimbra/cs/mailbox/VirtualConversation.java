/*
 * Created on Jun 14, 2005
 */
package com.zimbra.cs.mailbox;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.service.ServiceException;

/**
 * @author dkarp
 */
public class VirtualConversation extends Conversation {

    VirtualConversation(Mailbox mbox, Message msg) throws ServiceException {
        super(mbox, wrapMessage(msg));
    }


    public int getMessageId() {
        return -mId;
    }

    SenderList recalculateMetadata(boolean forceWrite) throws ServiceException {
        return (mSenderList = new SenderList(getMessage()));
    }

    private static final Message[] NO_MESSAGES = new Message[0];

    Message getMessage() throws ServiceException {
        return mMailbox.getMessageById(getMessageId());
    }
    Message[] getMessages(byte sort) throws ServiceException {
        return new Message[] { getMessage() };
    }
    Message[] getUnreadMessages() throws ServiceException {
        return isUnread() ? getMessages(SORT_ID_ASCENDING) : NO_MESSAGES;
    }


    static VirtualConversation create(Mailbox mbox, Message msg) throws ServiceException {
        VirtualConversation vconv = new VirtualConversation(mbox, msg);
        mbox.markItemCreated(vconv);
        return vconv;
    }

    private static UnderlyingData wrapMessage(Message msg) {
        UnderlyingData data = new UnderlyingData();
        data.id          = -msg.getId();
        data.type        = TYPE_VIRTUAL_CONVERSATION;
        data.folderId    = Mailbox.ID_FOLDER_CONVERSATIONS;
        data.subject     = msg.getNormalizedSubject();
        data.date        = (int) (msg.getDate() / 1000);
        data.modMetadata = msg.getSavedSequence();
        data.modContent  = msg.getSavedSequence();
        data.size        = 1;
        data.metadata    = encodeMetadata(new SenderList(msg));
        data.unreadCount = msg.getUnreadCount();
        data.children    = Integer.toString(msg.getId());
        data.inheritedTags = "-" + msg.mData.flags + ',' + msg.mData.tags;
        return data;
    }

    void open(String hash) throws ServiceException {
        DbMailItem.openConversation(hash, getMessage());
    }
    void close(String hash) throws ServiceException {
        DbMailItem.closeConversation(hash, getMessage());
    }

    void alterTag(Tag tag, boolean add) throws ServiceException {
        getMessage().alterTag(tag, add);
    }

    protected void addChild(MailItem child) throws ServiceException {
        throw MailServiceException.CANNOT_PARENT();
    }

    protected void removeChild(MailItem child) throws ServiceException {
        if (child.getId() != getMessageId())
            throw MailServiceException.IS_NOT_CHILD();
        markItemDeleted();
        mMailbox.uncache(this);
    }
}
