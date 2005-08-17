/*
 * Created on Jun 13, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailItem.LocationCount;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;

/**
 * @author schemers
 */
public class Conversation extends MailItem {

    private static Log sLog = LogFactory.getLog(Conversation.class);

    private   String     mEncodedSenders;
    protected SenderList mSenderList;

    Conversation(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_CONVERSATION && mData.type != TYPE_VIRTUAL_CONVERSATION)
            throw new IllegalArgumentException();
    }


    public int getMessageCount() {
        return (mChildren == null ? 0 : mChildren.length);
    }

    public SenderList getSenderList() throws ServiceException {
        getSenderList(false);
        return mSenderList;
    }
    private boolean getSenderList(boolean noGaps) throws ServiceException {
        if (mSenderList != null && !(noGaps && mSenderList.isElided()))
            return false;

        mSenderList = null;
        boolean forceWrite = true;
        // first, attempt to decode the existing sender list (if there is one)
        if (mEncodedSenders != null)
            try {
                String encoded = mEncodedSenders;
                mEncodedSenders = null;
                mSenderList = new SenderList(encoded);
                if (mSenderList.size() == mData.size) {
                    if (!(noGaps && mSenderList.isElided()))
                        return false;
                    else
                        forceWrite = false;
                }
            } catch (Exception e) { }

        // failed to parse or too few senders are listed -- have to recalculate
        //   (go through the Mailbox because we need to be in a transaction)
        mSenderList = mMailbox.recalculateSenderList(mId, forceWrite);
        return true;
    }

    SenderList recalculateMetadata(Message[] msgs, boolean forceWrite) throws ServiceException {
        mSenderList = new SenderList();
        mEncodedSenders = null;
        mInheritedTagSet = null;
        mChildren = null;

        mData.size = msgs.length;
        mData.unreadCount = 0;
        mData.messageCount = 0;

        // reconstruct the list of senders from scratch 
        markItemModified(Change.INTERNAL_ONLY);
        for (int i = 0; i < msgs.length; i++) {
            super.addChild(msgs[i]);
            mSenderList.add(msgs[i]);
        }
        assert(mSenderList.size() == msgs.length);

        if (mSenderList.size() == mData.size && !forceWrite)
            return mSenderList;
        // we're out of sync and need to rewrite the overview metadata
        sLog.info("resetting metadata: cid=" + mId + ", size was=" + mData.size + " is=" + mSenderList.size());
        mData.size = mSenderList.size();
        saveMetadata();
        return mSenderList;
    }

    static final byte SORT_ID_ASCENDING = DbMailItem.SORT_BY_ID | DbMailItem.SORT_ASCENDING;

    /** @return all the messages for this conversation.
     *  @param sort the sort order for the messages, specified by one of the
     *              <code>SORT_XXX</code> constants from {@link DbMailItem} */
    Message[] getMessages(byte sort) throws ServiceException {
        if ((sort & DbMailItem.SORT_FIELD_MASK) == DbMailItem.SORT_BY_ID) {
            // try to get all our info from the cache to avoid a database trip
            mChildren.sort((sort & DbMailItem.SORT_DIRECTION_MASK) == DbMailItem.SORT_ASCENDING);
            Message[] msgs = new Message[mChildren.length];
            int found;
            for (found = 0; found < mChildren.length; found++) {
                msgs[found] = (Message) mMailbox.getCachedItem(new Integer(mChildren.array[found]));
                if (msgs[found] == null)
                    break;
            }
            if (found == mChildren.length)
                return msgs;
        }

        List listData = DbMailItem.getByParent(this, sort);
        Message[] msgs = new Message[listData.size()];
        Iterator it = listData.iterator();
        for (int i = 0; it.hasNext(); i++)
            msgs[i] = mMailbox.getMessage((UnderlyingData) it.next());
        return msgs;
    }
    Message[] getUnreadMessages() throws ServiceException {
        List unreadData = DbMailItem.getUnreadMessages(this);
        if (unreadData == null)
            return null;
        Message[] msgs = new Message[unreadData.size()];
        Iterator it = unreadData.iterator();
        for (int i = 0; it.hasNext(); i++)
            msgs[i] = mMailbox.getMessage((UnderlyingData) it.next());
        return msgs;
    }


    boolean isTaggable()      { return false; }
    boolean isCopyable()      { return false; }
    boolean isMovable()       { return true; }
    boolean isMutable()       { return true; }
    boolean isIndexed()       { return false; }
    boolean canHaveChildren() { return true; }

    boolean trackTags()       { return true; }
    boolean canParent(MailItem item) { return (item instanceof Message); }


    static Conversation create(Mailbox mbox, int id, Message[] msgs) throws ServiceException {
        assert(id != Mailbox.ID_AUTO_INCREMENT && msgs.length > 0);
        int date = 0, unread = 0;
        StringBuffer children = new StringBuffer(), tags = new StringBuffer();
        SenderList sl = new SenderList();
        for (int i = 0; i < msgs.length; i++) {
            date = Math.max(date, msgs[i].mData.date);
            unread += msgs[i].mData.unreadCount;
            children.append(i == 0 ? "" : ",").append(msgs[i].mId);
            tags.append(i == 0 ? "-" : ",-").append(msgs[i].mData.flags)
                .append(',').append(msgs[i].mData.tags);
            sl.add(msgs[i]);
        }
        int changeID = mbox.getOperationChangeID();

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_CONVERSATION;
        data.folderId    = Mailbox.ID_FOLDER_CONVERSATIONS;
        data.subject     = msgs[0].getNormalizedSubject();
        data.date        = date;
        data.size        = msgs.length;
        data.metadata    = encodeMetadata(sl);
        data.modMetadata = changeID;
        data.modContent  = changeID;
        data.unreadCount = unread;
        data.children    = children.toString();
        data.inheritedTags = tags.toString();
        DbMailItem.create(mbox, data);

        Conversation conv = new Conversation(mbox, data);
        conv.finishCreation(null);

        DbMailItem.setParent(conv, msgs);
        for (int i = 0; i < msgs.length; i++) {
            mbox.markItemModified(msgs[i], Change.MODIFIED_PARENT);
            msgs[i].mData.parentId    = id;
            msgs[i].mData.modMetadata = changeID;
        }
        return conv;
    }

    void open(String hash) throws ServiceException {
        DbMailItem.openConversation(hash, this);
    }
    void close(String hash) throws ServiceException {
        DbMailItem.closeConversation(hash, this);
    }

    void detach() throws ServiceException {
        close(Mailbox.getHash(getSubject()));
    }

    void alterUnread(boolean unread, TargetConstraint tcon) throws ServiceException {
        markItemModified(Change.MODIFIED_UNREAD);

        // Decrement the in-memory unread count of each message.  Each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        Message[] msgs = getMessages(DbMailItem.DEFAULT_SORT_ORDER);
        Array targets = new Array();
        for (int i = 0; i < msgs.length; i++)
            if (msgs[i].isUnread() != unread && msgs[i].checkChangeID())
                if (tcon == null || tcon.checkItem(msgs[i])) {
                    msgs[i].updateUnread(unread ? 1 : -1);
                    targets.add(msgs[i].getId());
                }

        // mark the selected messages in this conversation as read in the database
        DbMailItem.alterUnread(mMailbox, targets, unread);
    }

    // conversations are a special case for tagging -- need to affect every child of the conversation
    void alterTag(Tag tag, boolean add, TargetConstraint tcon) throws ServiceException {
        if (tag == null)
            throw MailServiceException.CANNOT_TAG();
        if ((add ? mData.size : 0) == mInheritedTagSet.count(tag))
            return;
        if (tag.getId() == Flag.ID_FLAG_UNREAD)
            throw ServiceException.FAILURE("unread state must be set with alterUnread()", null);
        
        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);

        Message[] msgs = getMessages(SORT_ID_ASCENDING);
        Array targets = new Array();
        // since we're adding/removing a tag, the tag's unread count is going to change
        for (int i = 0; i < msgs.length; i++)
            if (msgs[i].isTagged(tag) != add && msgs[i].checkChangeID())
                if (tcon == null || tcon.checkItem(msgs[i])) {
                	if (tag.trackUnread() && msgs[i].isUnread())
                        tag.updateUnread(add ? 1 : -1);
                    targets.add(msgs[i].getId());
                    msgs[i].tagChanged(tag, add);
                    mInheritedTagSet.update(tag, add);
                }

        DbMailItem.alterTag(tag, targets, add);
    }

    void move(Folder target, TargetConstraint tcon) throws ServiceException {
        if (!target.canContain(TYPE_MESSAGE))
            throw MailServiceException.CANNOT_CONTAIN();
        markItemModified(Change.UNMODIFIED);

        Message[] msgs = getMessages(SORT_ID_ASCENDING);
        boolean toTrash = target.inTrash();
        int oldUnread = 0;
        for (int i = 0; i < msgs.length; i++)
            if (msgs[i].isUnread())
                oldUnread++;
        // if mData.unread is wrong, what to do?  right now, always use the calculated value
        mData.unreadCount = oldUnread;

        Array markedRead = new Array(), moved = new Array();
        for (int i = 0; i < msgs.length; i++) {
            Message msg = msgs[i];
            Folder source = msg.getFolder();

            // skip messages that the client doesn't know about or has explicitly excluded
            if (!msg.checkChangeID() || (tcon != null && !tcon.checkItem(msgs[i])))
                continue;

            if (msg.isUnread()) {
                if (!toTrash || msg.inTrash()) {
                    source.updateUnread(-1);
                    target.updateUnread(1);
                } else {
                    // unread messages moved from Mailbox to Trash need to be marked read:
                    //   update cached unread counts (message, conversation, folder, tags)
                    msg.updateUnread(-1);
                    //   note that we need to update this message in the DB
                    markedRead.add(msg.getId());
                }
            }

            // handle message counts and sizes
            source.updateMessageCount(-1);
            target.updateMessageCount(1);
            source.updateSize(-msg.getSize());
            target.updateSize(msg.getSize());

            moved.add(msg.getId());
            msg.folderChanged(target);
        }
        // mark unread messages moved from Mailbox to Trash/Spam as read in the DB
        if (markedRead.length > 0)
            DbMailItem.alterUnread(target.getMailbox(), markedRead, false);

        // moving a conversation to spam closes it
        //   XXX: what if only certain messages got moved?
        if (target.inSpam())
            detach();

        DbMailItem.setFolder(moved, target);
    }

    /** please call this *after* adding the child row to the DB */
    protected void addChild(MailItem child) throws ServiceException {
        super.addChild(child);
        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_SENDERS);

        mMailbox.updateSize(1);

        // FIXME: this ordering is to work around the fact that when getSenderList has to
        //   recalc the metadata, it uses the already-updated DB message state to do it...
        mData.date       = mMailbox.getOperationTimestamp();
        mData.modContent = mMailbox.getOperationChangeID();
        boolean recalculated = getSenderList(false);

        if (!recalculated) {
            mSenderList.add((Message) child);
            mData.size++;
            saveMetadata();
        }
    }

    protected void removeChild(MailItem child) throws ServiceException {
        super.removeChild(child);
        // remove the last message and the conversation goes away
        if (mChildren.length == 0) {
            delete();
            return;
        }
        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_SENDERS);

        Message msg = (Message) child;
        getSenderList();
        try {
            mSenderList.remove(msg);
        } catch (SenderList.RefreshException e) {
            // get a no-gaps version of the SenderList, so there's no chance that the remove can throw an exception 
            getSenderList(true);
            try { mSenderList.remove(msg); } catch (Exception e2) {}
        }
        mData.size--;
        mMailbox.updateSize(-1);
        saveMetadata();
    }

    private void merge(Conversation other) throws ServiceException {
        if (other == this)
            return;
        markItemModified(Change.MODIFIED_CHILDREN);

        // update conversation data
        getSenderList();
        other.getSenderList();
        mSenderList = SenderList.merge(mSenderList, other.mSenderList);
        saveMetadata();

        // change the messages' parent relation
        DbMailItem.reparentChildren(other, this);
        if (other.mChildren != null) {
            if (mChildren == null)
                mChildren = new Array();
            for (int i = 0; i < other.mChildren.length; i++) {
                int childId = other.mChildren.array[i];
                mChildren.add(childId);
                Message msg = mMailbox.getCachedMessage(new Integer(childId));
                if (msg != null) {
                    msg.markItemModified(Change.MODIFIED_PARENT);
                    msg.mData.parentId  = mId;
                    msg.mData.modMetadata = mMailbox.getOperationChangeID();
                }
            }
        }
        
        // delete the old conversation (must do this after moving the messages because of cascading delete)
        other.delete();
    }

    PendingDelete getDeletionInfo(TargetConstraint tcon) throws ServiceException {
        PendingDelete info = new PendingDelete();
        info.rootId = mId;
        info.itemIds.add(new Integer(mId));

        if (mChildren == null || mChildren.length == 0)
            return info;
        Message[] msgs = getMessages(SORT_ID_ASCENDING);

        for (int i = 0; i < msgs.length; i++) {
            Message child = msgs[i];
            if (!child.checkChangeID() || (tcon != null && !tcon.checkItem(child))) {
                info.incomplete = MailItem.DELETE_CONTENTS;
                continue;
            }
            Integer childId = new Integer(child.getId());

            info.size += child.getSize() + 1;   // +1 is for the reduction in conversation size
            info.itemIds.add(childId);
            if (child.isUnread())
                info.unreadIds.add(childId);

            if (child.getIndexId() > 0) {
                if (!isTagged(mMailbox.mCopiedFlag))
                    info.indexIds.add(new Integer(child.getIndexId()));
                else if (info.sharedIndex == null)
                    (info.sharedIndex = new HashSet()).add(new Integer(child.getIndexId()));
                else
                    info.sharedIndex.add(new Integer(child.getIndexId()));
            }

            try {
                info.blobs.add(child.getBlob());
            } catch (Exception e1) { }

            Integer folderId = new Integer(child.getFolderId());
            LocationCount count = (LocationCount) info.messages.get(folderId);
            if (count == null)
                info.messages.put(folderId, new LocationCount(1, child.getSize()));
            else
                count.increment(1, child.getSize());
        }

        return info;
    }

    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        if (info.incomplete) {
            // *some* of the messages remain; recalculate the data based on this
            int remaining = mChildren.length - info.itemIds.size();
            Message[] msgs = new Message[remaining];
            for (int i = 0, loc = 0; i < mChildren.length; i++)
                if (!info.itemIds.contains(new Integer(mChildren.array[i])))
                    msgs[loc++] = mMailbox.getMessageById(mChildren.array[i]);
            recalculateMetadata(msgs, true);
        }

        super.purgeCache(info, purgeItem);
    }


    Metadata decodeMetadata(String metadata) {
        mEncodedSenders = metadata;
        return null;
    }
    
    String encodeMetadata() {
        if (mEncodedSenders != null)
            return mEncodedSenders;
        else if (mSenderList != null)
            return encodeMetadata(mSenderList);
        else
            try {
                return encodeMetadata(getSenderList());
            } catch (ServiceException e) {
                return "";
            }
    }
    static String encodeMetadata(SenderList senders) {
        return (senders != null ? senders.toString() : "");
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("conversation: {");
        appendCommonMembers(sb);
        sb.append("}");
        return sb.toString();
    }
}
