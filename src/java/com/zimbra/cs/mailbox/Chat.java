package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mime.ParsedMessage;

public class Chat extends Message {
    
    /**
     * this one will call back into decodeMetadata() to do our initialization
     * 
     * @param mbox
     * @param ud
     * @throws ServiceException
     */
    Chat(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        if (mData.type != TYPE_CHAT)
            throw new IllegalArgumentException();
        if (mData.parentId < 0)
            mData.parentId = -mId;
    }
    
    static class ChatCreateFactory extends MessageCreateFactory {
        Message create(Mailbox mbox, UnderlyingData data) throws ServiceException {
            return new Chat(mbox, data);
        }
        byte getType() { return TYPE_CHAT; }
    }
    
    static Chat create(int id, Folder folder, ParsedMessage pm,
                int msgSize, String digest, short volumeId, boolean unread,
                int flags, long tags)  
    throws ServiceException {
        return (Chat)Message.createInternal(id, folder, null, pm, msgSize, digest, volumeId, unread, 
                    flags, tags, null, true, null,new ChatCreateFactory());
    }
    
    boolean isMutable() { return true; }
}
