/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 14, 2005
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbMailItem;

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

    @Override boolean loadSenderList() throws ServiceException {
        mSenderList = new SenderList(getMessage());
        return false;
    }

    @Override SenderList recalculateMetadata(List<Message> msgs) throws ServiceException {
        Message msg = msgs.get(0);
        mData = wrapMessage(msg);
        mInheritedTagSet = new TagSet().updateFlags(msg.getFlagBitmask(), true).updateTags(msg.getTagBitmask(), true);
        return getSenderList();
    }


    Message getMessage() throws ServiceException {
        return mMailbox.getMessageById(getMessageId());
    }

    @Override List<Message> getMessages(byte sort) throws ServiceException {
        List<Message> msgs = new ArrayList<Message>(1);
        msgs.add(getMessage());
        return msgs;
    }


    static VirtualConversation create(Mailbox mbox, Message msg) throws ServiceException {
        VirtualConversation vconv = new VirtualConversation(mbox, msg);
        mbox.markItemCreated(vconv);
        return vconv;
    }

    private static UnderlyingData wrapMessage(Message msg) {
        ArrayList<Integer> children = new ArrayList<Integer>();
        children.add(msg.getId());

        UnderlyingData data = new UnderlyingData();
        data.id          = -msg.getId();
        data.type        = TYPE_VIRTUAL_CONVERSATION;
        data.folderId    = Mailbox.ID_FOLDER_CONVERSATIONS;
        data.subject     = msg.getNormalizedSubject();
        data.date        = (int) (msg.getDate() / 1000);
        data.modMetadata = msg.getSavedSequence();
        data.modContent  = msg.getSavedSequence();
        data.size        = 1;
        data.metadata    = encodeMetadata(DEFAULT_COLOR, 1, new SenderList(msg));
        data.unreadCount = msg.getUnreadCount();
        data.children    = children;
        data.inheritedTags = "-" + msg.mData.flags + ',' + msg.mData.tags;
        return data;
    }

    @Override void open(String hash) throws ServiceException {
        DbMailItem.openConversation(hash, getMessage());
    }

    @Override void close(String hash) throws ServiceException {
        DbMailItem.closeConversation(hash, getMessage());
    }

    @Override void alterTag(Tag tag, boolean add) throws ServiceException {
        getMessage().alterTag(tag, add);
    }

    @Override void addChild(MailItem child) throws ServiceException {
        throw MailServiceException.CANNOT_PARENT();
    }

    @Override void removeChild(MailItem child) throws ServiceException {
        if (child.getId() != getMessageId())
            throw MailServiceException.IS_NOT_CHILD();
        markItemDeleted();
        mMailbox.uncache(this);
    }
}
