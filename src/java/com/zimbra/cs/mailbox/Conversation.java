/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 13, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public class Conversation extends MailItem {
    private   String     mEncodedSenders;
    protected SenderList mSenderList;

    Conversation(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_CONVERSATION && mData.type != TYPE_VIRTUAL_CONVERSATION)
            throw new IllegalArgumentException();
    }

    /** Returns the normalized subject of the conversation.  This is done by
     *  taking the <tt>Subject:</tt> header of the first message and removing
     *  prefixes (e.g. <tt>"Re:"</tt>) and suffixes (e.g. <tt>"(fwd)"</tt>)
     *  and the like.
     * 
     * @see ParsedMessage#normalizeSubject */
    public String getNormalizedSubject() {
        return ParsedMessage.normalize(getSubject());
    }

    @Override public String getSender() {
        return "";
    }

    /** Returns the number of messages in the conversation, as calculated from
     *  its list of children as fetched from the database.  This <u>should</u>
     *  always be equal to {@link MailItem#getSize()}; if it isn't, an error
     *  has occurred and been persisted to the database. */
    public int getMessageCount() {
        return (int) mData.size;
    }

    @Override public int getInternalFlagBitmask() {
        return 0;
    }


    // do *not* make this public, as it'd skirt Mailbox-level synchronization and caching
    SenderList getSenderList() throws ServiceException {
        loadSenderList();
        return mSenderList;
    }

    void instantiateSenderList() {
        if (mSenderList != null && mSenderList.size() == mData.size)
            return;

        mSenderList = null;
        // first, attempt to decode the existing sender list (if there is one)
        if (mEncodedSenders != null) {
            String encoded = mEncodedSenders;
            mEncodedSenders = null;
            try {
                // if the first message has been removed, this should throw
                //   an exception and force the list to be recalculated
                mSenderList = SenderList.parse(encoded);
                if (mSenderList.size() != mData.size)
                    mSenderList = null;
            } catch (Exception e) { }
        }
    }

    /** Makes certain the Conversation's {@link SenderList} is loaded and
     *  valid.  If it's not, first tries to instantiate it from the loaded
     *  metadata.  If that's either missing or invalid, recalculates the
     *  Conversation's metadata from its constituent messages and rewrites
     *  the database row.
     *  
     * @return <tt>true</tt> if the metadata was recalculated, <tt>false</tt>
     *         if the SenderList was generated from existing data */
    boolean loadSenderList() throws ServiceException {
        instantiateSenderList();
        if (mSenderList != null)
            return false;

        // failed to parse or too few senders are listed -- have to recalculate
        //   (go through the Mailbox because we need to be in a transaction)
        recalculateMetadata();
        return true;
    }

    void recalculateCounts(List<Message> msgs) {
        markItemModified(Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD);
        mData.tags = mData.flags = mData.unreadCount = 0;
        for (Message msg : msgs) {
            mData.unreadCount += msg.getUnreadCount();
            mData.flags |= msg.getInternalFlagBitmask();
            mData.tags |= msg.getTagBitmask();
        }
    }

    private static final int RECALCULATE_CHANGE_MASK = Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS  | Change.MODIFIED_SENDERS |
                                                       Change.MODIFIED_SIZE | Change.MODIFIED_UNREAD | Change.MODIFIED_METADATA;

    SenderList recalculateMetadata() throws ServiceException {
        return recalculateMetadata(getMessages());
    }

    SenderList recalculateMetadata(List<Message> msgs) throws ServiceException {
        Collections.sort(msgs, new Message.SortDateAscending());
        
        markItemModified(RECALCULATE_CHANGE_MASK);

        mEncodedSenders = null;
        mSenderList = new SenderList(msgs);
        mData.size = msgs.size();

        mData.tags = mData.flags = mData.unreadCount = 0;
        mExtendedData = null;
        for (Message msg : msgs) {
            // unread count is updated via MailItem.addChild() for some reason...
            super.addChild(msg);
            mData.unreadCount += (msg.isUnread() ? 1 : 0);
            mData.flags |= msg.getInternalFlagBitmask();
            mData.tags |= msg.getTagBitmask();
            mExtendedData = MetadataCallback.duringConversationAdd(mExtendedData, msg);
        }

        // need to rewrite the overview metadata
        ZimbraLog.mailbox.debug("resetting metadata: cid=" + mId + ", size was=" + mData.size + " is=" + mSenderList.size());
        saveData(null);
        return mSenderList;
    }

    /** Returns all the {@link Message}s in this conversation.  The messages
     *  are fetched from the {@link Mailbox}'s cache, if possible; if not,
     *  they're fetched from the database.  The returned messages are not
     *  guaranteed to be sorted in any way. */
    List<Message> getMessages() throws ServiceException {
        return getMessages(SortBy.NONE);
    }

    /** Returns all the {@link Message}s in this conversation.  The messages
     *  are fetched from the {@link Mailbox}'s cache, if possible; if not,
     *  they're fetched from the database.
     * 
     * @param sort  The sort order for the messages, specified by one of the
     *              <code>SORT_XXX</code> constants from {@link DbMailItem}. */
    List<Message> getMessages(SortBy sort) throws ServiceException {
        List<Message> msgs = new ArrayList<Message>(getMessageCount());
        List<UnderlyingData> listData = DbMailItem.getByParent(this, sort);
        for (UnderlyingData data : listData)
            msgs.add(mMailbox.getMessage(data));
        return msgs;
    }

    @Override boolean canAccess(short rightsNeeded) {
        return true;
    }

    @Override boolean canAccess(short rightsNeeded, Account authuser, boolean asAdmin) {
        return true;
    }


    @Override boolean isTaggable()      { return false; }
    @Override boolean isCopyable()      { return false; }
    @Override boolean isMovable()       { return true; }
    @Override boolean isMutable()       { return true; }
    @Override boolean isIndexed()       { return false; }
    @Override boolean canHaveChildren() { return true; }

    @Override boolean canParent(MailItem item) { return (item instanceof Message); }


    static Conversation create(Mailbox mbox, int id, Message[] msgs) throws ServiceException {
        if (ZimbraLog.mailop.isDebugEnabled()) {
            StringBuilder msgIds = new StringBuilder();
            for (int i = 0; i < msgs.length; i++)
                msgIds.append(i > 0 ? "," : "").append(msgs[i].getId());
            ZimbraLog.mailop.debug("Adding Conversation: id=%d, message(s): %s.", id, msgIds);
        }

        assert(id != Mailbox.ID_AUTO_INCREMENT && msgs.length > 0);
        Arrays.sort(msgs, new Message.SortDateAscending());

        int date = 0, unread = 0, flags = 0;
        long tags = 0;
        CustomMetadataList extended = null;
        for (int i = 0; i < msgs.length; i++) {
            Message msg = msgs[i];
            if (msg == null)
                throw ServiceException.FAILURE("null Message in list", null);
            date = Math.max(date, msg.mData.date);
            unread += msg.mData.unreadCount;
            flags  |= msg.mData.flags;
            tags   |= msg.mData.tags;
            extended = MetadataCallback.duringConversationAdd(extended, msg);
        }

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_CONVERSATION;
        data.folderId    = Mailbox.ID_FOLDER_CONVERSATIONS;
        data.subject     = msgs.length > 0 ? msgs[0].getSubject() : "";
        data.date        = date;
        data.size        = msgs.length;
        data.unreadCount = unread;
        data.flags       = flags;
        data.tags        = tags;
        data.metadata    = encodeMetadata(DEFAULT_COLOR_RGB, 1, extended, new SenderList(msgs));
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data, null);

        Conversation conv = new Conversation(mbox, data);
        conv.finishCreation(null);

        DbMailItem.setParent(msgs, conv);
        for (int i = 0; i < msgs.length; i++) {
            mbox.markItemModified(msgs[i], Change.MODIFIED_PARENT);
            msgs[i].mData.parentId = id;
            msgs[i].mData.metadataChanged(mbox);
        }
        return conv;
    }

    void open(String hash) throws ServiceException {
        DbMailItem.openConversation(hash, this);
    }

    void close(String hash) throws ServiceException {
        DbMailItem.closeConversation(hash, this);
    }

    @Override void detach() throws ServiceException {
        close(Mailbox.getHash(getNormalizedSubject()));
    }

    /** Updates the unread state of all messages in the conversation.
     *  Persists the change to the database and cache, and also updates
     *  the unread counts for the affected items' {@link Folder}s and
     *  {@link Tag}s appropriately.<p>
     * 
     *  Messages in the conversation are omitted from this operation if
     *  one or more of the following applies:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_WRITE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  marked read/unread.
     * 
     * @perms {@link ACL#RIGHT_WRITE} on all the messages */
    @Override void alterUnread(boolean unread) throws ServiceException {
        markItemModified(Change.MODIFIED_UNREAD);

        boolean excludeAccess = false;

        // Decrement the in-memory unread count of each message.  Each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();
        List<Integer> targets = new ArrayList<Integer>();
        for (Message msg : getMessages()) {
            // skip messages that don't need to be changed, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (msg.isUnread() == unread ) {
                continue;
            } else if (!msg.canAccess(ACL.RIGHT_WRITE)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            }

            int delta = unread ? 1 : -1;
            msg.updateUnread(delta, msg.isTagged(Flag.ID_FLAG_DELETED) ? delta : 0);
            msg.mData.metadataChanged(mMailbox);
            targets.add(msg.getId());
        }

        // mark the selected messages in this conversation as read in the database
        if (targets.isEmpty()) {
            if (excludeAccess)
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
        } else {
            DbMailItem.alterUnread(mMailbox, targets, unread);
        }
    }

    /** Tags or untags all messages in the conversation.  Persists the change
     *  to the database and cache.  If the conversation includes at least one
     *  unread {@link Message} whose tagged state is changing, updates the 
     *  {@link Tag}'s unread count appropriately.<p>
     * 
     *  Messages in the conversation are omitted from this operation if
     *  one or more of the following applies:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_WRITE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  tagged/untagged.
     * 
     * @perms {@link ACL#RIGHT_WRITE} on all the messages */
    @Override void alterTag(Tag tag, boolean add) throws ServiceException {
        if (tag == null)
            throw ServiceException.FAILURE("missing tag argument", null);
        if (!add && !isTagged(tag))
            return;
        if (tag.getId() == Flag.ID_FLAG_UNREAD)
            throw ServiceException.FAILURE("unread state must be set with alterUnread", null);
        // don't let the user tag things as "has attachments" or "draft"
        if (tag instanceof Flag && (tag.getBitmask() & Flag.FLAG_SYSTEM) != 0)
            throw MailServiceException.CANNOT_TAG(tag, this);

        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);

        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();
        boolean excludeAccess = false;

        List<Message> msgs = getMessages();
        List<Integer> targets = new ArrayList<Integer>(msgs.size());
        for (Message msg : msgs) {
            // skip messages that don't need to be changed, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (msg.isTagged(tag) == add) {
                continue;
            } else if (!msg.canAccess(ACL.RIGHT_WRITE)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            } else if (add && !tag.canTag(msg)) {
                throw MailServiceException.CANNOT_TAG(tag, this);
            }

            targets.add(msg.getId());
            msg.tagChanged(tag, add);

            // since we're adding/removing a tag, the tag's unread count may change
            int delta = add ? 1 : -1;
            if (tag.trackUnread() && msg.isUnread())
                tag.updateUnread(delta, isTagged(Flag.ID_FLAG_DELETED) ? delta : 0);

            // if we're adding/removing the \Deleted flag, update the folder and tag "deleted" and "deleted unread" counts
            if (tag.getId() == Flag.ID_FLAG_DELETED) {
                getFolder().updateSize(0, delta, 0);
                // note that Message.updateUnread() calls updateTagUnread()
                if (msg.isUnread())
                    msg.updateUnread(0, delta);
            }
        }

        if (targets.isEmpty()) {
            if (excludeAccess)
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
        } else {
            if (ZimbraLog.mailop.isDebugEnabled()) {
                String operation = add ? "Setting" : "Unsetting";
                ZimbraLog.mailop.debug("%s %s for %s.  Affected ids: %s",
                    operation, getMailopContext(tag), getMailopContext(this), StringUtil.join(",", targets));
            }
            recalculateCounts(msgs);
            DbMailItem.alterTag(tag, targets, add);
        }
    }

    @Override protected void inheritedTagChanged(Tag tag, boolean add) throws ServiceException {
        if (tag == null || add == isTagged(tag))
            return;

        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);
        if (add)
            tagChanged(tag, add);
        else
            DbMailItem.completeConversation(mMailbox, mData);
    }

    protected void inheritedCustomDataChanged(Message msg, CustomMetadata custom) throws ServiceException {
        if (custom == null)
            return;

        markItemModified(Change.MODIFIED_METADATA);
        if (!custom.isEmpty()) {
            mExtendedData = MetadataCallback.duringConversationAdd(mExtendedData, msg);
            saveMetadata();
        } else {
            recalculateMetadata();
        }
    }

    /** Moves all the conversation's {@link Message}s to a different
     *  {@link Folder}.  Persists the change to the database and the in-memory
     *  cache.  Updates all relevant unread counts, folder sizes, etc.<p>
     * 
     *  Messages moved to the Trash folder are automatically marked read.
     *  Conversations moved to the Junk folder will not receive newly-delivered
     *  messages.<p>
     * 
     *  Messages in the conversation are omitted from this operation if
     *  one or more of the following applies:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_WRITE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  moved.
     * 
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on the messages' source folders */
    @Override boolean move(Folder target) throws ServiceException {
        if (!target.canContain(TYPE_MESSAGE))
            throw MailServiceException.CANNOT_CONTAIN();
        markItemModified(Change.UNMODIFIED);

        List<Message> msgs = getMessages();
        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();
        boolean toTrash = target.inTrash();
        int oldUnread = 0;
        for (Message msg : msgs)
            if (msg.isUnread())
                oldUnread++;
        // if mData.unread is wrong, what to do?  right now, always use the calculated value
        mData.unreadCount = oldUnread;

        boolean excludeAccess = false;

        List<Integer> markedRead = new ArrayList<Integer>();
        List<Message> moved = new ArrayList<Message>();
        List<Message> indexUpdated = new ArrayList<Message>();
        
        for (Message msg : msgs) {
            Folder source = msg.getFolder();

            // skip messages that don't need to be moved, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (source.getId() == target.getId()) {
                continue;
            } else if (!source.canAccess(ACL.RIGHT_DELETE)) {
                excludeAccess = true;  continue;
            } else if (target.getId() != Mailbox.ID_FOLDER_TRASH && target.getId() != Mailbox.ID_FOLDER_SPAM && !target.canAccess(ACL.RIGHT_INSERT)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            }

            boolean isDeleted = msg.isTagged(Flag.ID_FLAG_DELETED);
            if (msg.isUnread()) {
                if (!toTrash || msg.inTrash()) {
                    source.updateUnread(-1, isDeleted ? -1 : 0);
                    target.updateUnread(1, isDeleted ? 1 : 0);
                } else {
                    // unread messages moved from Mailbox to Trash need to be marked read:
                    //   update cached unread counts (message, conversation, folder, tags)
                    msg.updateUnread(-1, isDeleted ? -1 : 0);
                    //   note that we need to update this message in the DB
                    markedRead.add(msg.getId());
                }
            }
            
            // moved an item out of the spam folder, need to index it
            if (msg.inSpam() && !target.inSpam()) {
                if (msg.isIndexed() && msg.getIndexId() != null) {
                    msg.indexIdChanged(msg.getMailbox().generateIndexId(msg.getId()));
                    indexUpdated.add(msg);
                }
            }

            // handle folder message counts
            source.updateSize(-1, isDeleted ? -1 : 0, -msg.getTotalSize());
            target.updateSize(1, isDeleted ? 1 : 0, msg.getTotalSize());

            moved.add(msg);
            msg.folderChanged(target, 0);
        }

        // mark unread messages moved from Mailbox to Trash/Spam as read in the DB
        if (!markedRead.isEmpty())
            DbMailItem.alterUnread(target.getMailbox(), markedRead, false);

        if (moved.isEmpty()) {
            if (excludeAccess)
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
        } else {
            // moving a conversation to spam closes it
            if (target.inSpam())
                detach();
            if (ZimbraLog.mailop.isInfoEnabled()) {
                StringBuilder ids = new StringBuilder();
                for (int i = 0; i < moved.size(); i++) {
                    if (i > 0) {
                        ids.append(',');
                    }
                    ids.append(moved.get(i).getId());
                }
                ZimbraLog.mailop.info("Moving %s to %s.  Affected message ids: %s.",
                    getMailopContext(this), getMailopContext(target), ids);
            }
            DbMailItem.setFolder(moved, target);
            
            if (!indexUpdated.isEmpty()) { 
                DbMailItem.setIndexIds(mMailbox, indexUpdated);
                for (Message msg : indexUpdated) {
                    mMailbox.queueForIndexing(msg, false, null);
                }
            }
        }

        return !moved.isEmpty();
    }

    /** please call this *after* adding the child row to the DB */
    @Override void addChild(MailItem child) throws ServiceException {
        if (!(child instanceof Message))
            throw MailServiceException.CANNOT_PARENT();
        Message msg = (Message) child;

        super.addChild(msg);

        // update inherited flags
        int oldFlags = mData.flags;
        mData.flags |= msg.getInternalFlagBitmask();
        if (mData.flags != oldFlags)
            markItemModified(Change.MODIFIED_FLAGS);

        // update inherited tags
        long oldTags = mData.tags;
        mData.tags |= msg.getTagBitmask();
        if (mData.tags != oldTags)
            markItemModified(Change.MODIFIED_TAGS);

        // update unread counts
        if (msg.isUnread()) {
            markItemModified(Change.MODIFIED_UNREAD);
            updateUnread(child.mData.unreadCount, child.isTagged(Flag.ID_FLAG_DELETED) ? child.mData.unreadCount : 0);
        }

        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_SENDERS | Change.MODIFIED_METADATA);

        MetadataCallback.duringConversationAdd(mExtendedData, msg);

        // FIXME: this ordering is to work around the fact that when getSenderList has to
        //   recalc the metadata, it uses the already-updated DB message state to do it...
        mData.date = mMailbox.getOperationTimestamp();
        mData.contentChanged(mMailbox);

        if (!mMailbox.hasListeners(Session.Type.SOAP)) {
            instantiateSenderList();
            mData.size++;
            try {
                if (mSenderList != null)
                    mSenderList.add(msg);
            } catch (SenderList.RefreshException slre) {
                mSenderList = null;
            }
            saveMetadata();
        } else {
            boolean recalculated = loadSenderList();
            if (!recalculated) {
                mData.size++;
                try {
                    mSenderList.add(msg);
                    saveMetadata();
                } catch (SenderList.RefreshException slre) {
                    recalculateMetadata();
                }
            }
        }
    }

    @Override void removeChild(MailItem child) throws ServiceException {
        super.removeChild(child);

        // remove the last message and the conversation goes away
        if (getMessageCount() == 0) {
            delete();
            return;
        }

        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_SENDERS);

        if (!mMailbox.hasListeners(Session.Type.SOAP)) {
            // update unread counts
            if (child.isUnread()) {
                markItemModified(Change.MODIFIED_UNREAD);
                updateUnread(-child.mData.unreadCount, child.isTagged(Flag.ID_FLAG_DELETED) ? -child.mData.unreadCount : 0);
            }

            // update inherited tags, if applicable
            if (child.mData.tags != 0 || child.mData.flags != 0) {
                int oldFlags = mData.flags;
                long oldTags = mData.tags;

                DbMailItem.completeConversation(mMailbox, mData);

                if (mData.flags != oldFlags)
                    markItemModified(Change.MODIFIED_FLAGS);
                if (mData.tags != oldTags)
                    markItemModified(Change.MODIFIED_TAGS);
            }

            mEncodedSenders = null;
            mSenderList = null;
            mData.size--;
            saveMetadata(null);
        } else {
            List<Message> msgs = getMessages();
            msgs.remove(child);
            recalculateMetadata(msgs);
        }
    }

    /*
    private void merge(Conversation other) throws ServiceException {
        if (other == this)
            return;
        markItemModified(Change.MODIFIED_CHILDREN);

        mData.size += other.getSize();

        // update conversation data
        getSenderList();
        other.getSenderList();
        int firstId = mSenderList.getEarliest().messageId;
        mSenderList = SenderList.merge(mSenderList, other.mSenderList);
        int newFirstId = mSenderList.getEarliest().messageId;
        if (firstId != newFirstId)
            try {
                recalculateSubject(mMailbox.getMessageById(newFirstId));
            } catch (MailServiceException.NoSuchItemException nsie) {
                sLog.warn("can't fetch message " + newFirstId + " to calculate conv subject");
            }
        saveData(null);

        // change the messages' parent relation
        DbMailItem.reparentChildren(other, this);
        if (other.mData.children != null) {
            if (mData.children == null)
                mData.children = new ArrayList<Integer>();
            for (int childId : other.mData.children) {
                mData.children.add(childId);
                Message msg = mMailbox.getCachedMessage(childId);
                if (msg != null) {
                    msg.markItemModified(Change.MODIFIED_PARENT);
                    msg.mData.parentId = mId;
                    msg.mData.metadataChanged(mMailbox);
                }
            }
        }

        // delete the old conversation (must do this after moving the messages because of cascading delete)
        other.delete();
    }
    */

    /** Determines the set of {@link Message}s to be deleted from this
     *  <code>Conversation</code>.  Assembles a new {@link PendingDelete}
     *  object encapsulating the data on the items to be deleted.<p>
     * 
     *  A message will be deleted unless:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_DELETE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  deleted.
     * 
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><code>mail.MODIFY_CONFLICT</code> - if the caller specified a
     *        max change number and a modification check, and the modified
     *        change number of the <code>Message</code> is greater
     *    <li><code>service.FAILURE</code> - if there's a database
     *        failure fetching the message list</ul> */
    @Override PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = new PendingDelete();
        info.rootId = mId;
        info.itemIds.add(getType(), mId);

        if (mData.size == 0)
            return info;
        List<Message> msgs = getMessages();
        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();

        boolean excludeModify = false, excludeAccess = false;
        for (Message child : msgs) {
            // silently skip explicitly excluded messages, PERMISSION_DENIED messages, and MODIFY_CONFLICT messages
            if (!TargetConstraint.checkItem(tcon, child))
                continue;
            else if (!child.canAccess(ACL.RIGHT_DELETE))
                excludeAccess = true;
            else if (!child.checkChangeID())
                excludeModify = true;
            else
                info.add(child.getDeletionInfo());
        }

        int totalDeleted = info.itemIds.size();
        if (totalDeleted == 1) {
            // if all messages have been excluded, some for "error" reasons, throw an exception
            if (excludeAccess)
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            if (excludeModify)
                throw MailServiceException.MODIFY_CONFLICT();
        }
        if (totalDeleted != msgs.size() + 1)
            info.incomplete = true;
        return info;
    }

    @Override void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        // if *some* of the messages remain, recalculate the data based on this
        if (info.incomplete)
            recalculateMetadata();

        super.purgeCache(info, purgeItem);
    }


    @Override void decodeMetadata(String metadata) {
        // when a folder is deleted, DbMailItem.markDeletionTargets() nulls out metadata for all affected conversations
        //   in that case, leave mSenderList unset and fault it in as necessary
        if (metadata != null) {
            try {
                Metadata meta = new Metadata(metadata, this);
                if (meta.containsKey(Metadata.FN_PARTICIPANTS))
                    decodeMetadata(meta);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.info("Unable to parse conversation metadata: id= " + mId + ", data='" + metadata + "'", e);
            }
        }
    }

    @Override void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mEncodedSenders = meta.get(Metadata.FN_PARTICIPANTS, null);
    }

    @Override Metadata encodeMetadata(Metadata meta) {
        String encoded = mEncodedSenders;
        if (encoded == null && mSenderList != null)
            encoded = mSenderList.toString();
    	return encodeMetadata(meta, mRGBColor, mVersion, mExtendedData, encoded);
    }

    static String encodeMetadata(Color color, int version, CustomMetadataList extended, SenderList senders) {
        return encodeMetadata(new Metadata(), color, version, extended, senders.toString()).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int version, CustomMetadataList extended, String encodedSenders) {
        meta.put(Metadata.FN_PARTICIPANTS, encodedSenders);
        return MailItem.encodeMetadata(meta, color, version, extended);
    }
    
    @Override public String getSortSubject() {
        // not actually used since Conversations aren't indexed...but here for correctness/completeness
        String subject = getNormalizedSubject();
        return subject.toUpperCase().substring(0, Math.min(DbMailItem.MAX_SUBJECT_LENGTH, subject.length()));
    }

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("conversation: {");
        appendCommonMembers(sb);
        sb.append("}");
        return sb.toString();
    }
}
