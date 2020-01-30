/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.session.PendingModifications.Change;

/**
 * @since Jun 14, 2005
 */
public class VirtualConversation extends Conversation {

    VirtualConversation(Mailbox mbox, Message msg) throws ServiceException {
        this(mbox, wrapMessage(msg));
    }

    VirtualConversation(Mailbox mbox, UnderlyingData data) throws ServiceException {
        this(mbox, data, false);
    }

    VirtualConversation(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        super(mbox, data, skipCache);
        if (mData.type != Type.VIRTUAL_CONVERSATION.toByte()) {
            throw new IllegalArgumentException();
        }
    }

    public int getMessageId() {
        return -mId;
    }

    @Override
    boolean loadSenderList() throws ServiceException {
        mSenderList = new SenderList(getMessage());
        return false;
    }

    @Override
    SenderList recalculateMetadata(List<Message> msgs) throws ServiceException {
        Message msg = msgs.get(0);
        mData = wrapMessage(msg);
        mExtendedData = MetadataCallback.duringConversationAdd(null, msg);
        return getSenderList();
    }


    Message getMessage() throws ServiceException {
        return mMailbox.getMessageById(getMessageId());
    }

    @Override
    public List<Message> getMessages(SortBy sort, int limit) throws ServiceException {
        return Collections.singletonList(getMessage());
    }

    static VirtualConversation create(Mailbox mbox, Message msg) throws ServiceException {
        VirtualConversation vconv = new VirtualConversation(mbox, msg);
        vconv.markItemCreated();
        return vconv;
    }

    private static UnderlyingData wrapMessage(Message msg) {
        CustomMetadataList extended = MetadataCallback.duringConversationAdd(null, msg);

        UnderlyingData data = new UnderlyingData();
        data.id = -msg.getId();
        data.type = Type.VIRTUAL_CONVERSATION.toByte();
        data.folderId = Mailbox.ID_FOLDER_CONVERSATIONS;
        data.setSubject(msg.getSubject());
        data.date = (int) (msg.getDate() / 1000);
        data.modMetadata = (int) msg.getSavedSequence();
        data.modContent = msg.getSavedSequence();
        data.size = 1;
        data.unreadCount = msg.getUnreadCount();
        data.setFlags(msg.getInternalFlagBitmask());
        data.setTags(new Tag.NormalizedTags(msg.getTags()));
        data.metadata = encodeMetadata(DEFAULT_COLOR_RGB, 1, 1, extended, new SenderList(msg));
        return data;
    }

    @Override
    void open(String hash) throws ServiceException {
        DbMailItem.openConversation(hash, getMessage());
    }

    @Override
    void close(String hash) throws ServiceException {
        DbMailItem.closeConversation(hash, getMessage());
    }

    @Override
    void alterTag(Tag tag, boolean add) throws ServiceException {
        getMessage().alterTag(tag, add);
    }

    @Override
    protected void inheritedTagChanged(Tag tag, boolean add) throws ServiceException {
        if (tag == null || add == isTagged(tag)) {
            return;
        }
        markItemModified(tag instanceof Flag ? Change.FLAGS : Change.TAGS);
        tagChanged(tag, add);
    }

    @Override
    protected void inheritedCustomDataChanged(Message msg, CustomMetadata custom) throws ServiceException {
        markItemModified(Change.METADATA);
        mExtendedData = MetadataCallback.duringConversationAdd(null, msg);
    }

    @Override
    void addChild(MailItem child) throws ServiceException {
        throw MailServiceException.CANNOT_PARENT();
    }

    @Override
    void removeChild(MailItem child) throws ServiceException {
        if (child.getId() != getMessageId()) {
            throw MailServiceException.IS_NOT_CHILD();
        }
        markItemDeleted();
        mMailbox.uncache(this);
    }

    @Override
    public MailItem snapshotItem() throws ServiceException {
        UnderlyingData data = getUnderlyingData().clone();
        data.setFlag(Flag.FlagInfo.UNCACHED);
        return new VirtualConversation(mMailbox, data);
    }
}
