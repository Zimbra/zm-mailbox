/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Jun 13, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author schemers
 */
public class Conversation extends MailItem {

    protected final class TagSet {
        private List<Integer> mTags;

        TagSet()           { mTags = new ArrayList<Integer>(); }
        TagSet(Tag tag)    { (mTags = new ArrayList<Integer>()).add(tag.getId()); }
        TagSet(String csv) {
            mTags = new ArrayList<Integer>();
            if (csv == null || csv.equals(""))
                return;
            String[] tags = csv.split(",");
            for (int i = 0; i < tags.length; i++) {
                long value = 0;
                try {
                    value = Long.parseLong(tags[i]);
                } catch (NumberFormatException e) {
                    ZimbraLog.mailbox.error("Unable to parse tags: '" + csv + "'", e);
                    throw e;
                }
                if (value == 0)      continue;
                else if (value < 0)  updateFlags((int) (-value), true);
                else                 updateTags(value, true);
            }
        }

        boolean contains(Tag tag)   { return contains(tag.getId()); }
        boolean contains(int tagId) { return mTags.contains(tagId); }

        int count(Tag tag)   { return count(tag.getId()); }
        int count(int tagId) {
            int count = 0;
            for (int i : mTags)
                if (i == tagId)
                    count++;
            return count;
        }

        boolean update(Tag tag, boolean add)                    { return update(tag.getId(), add, false); }
        boolean update(Tag tag, boolean add, boolean affectAll) { return update(tag.getId(), add, affectAll); }
        boolean update(int tagId, boolean add)                  { return update(tagId, add, false); }
        boolean update(int tagId, boolean add, boolean affectAll) {
            if (add)
                mTags.add(tagId);
            else if (!affectAll)
                mTags.remove((Integer) tagId);
            else
                while (mTags.remove((Integer) tagId)) ;
            recalculate();
            return true;
        }

        TagSet updateFlags(int flags, boolean add) {
            for (int j = 0; flags != 0 && j < MAX_FLAG_COUNT; j++) {
                int mask = 1 << j; 
                if ((flags & mask) != 0) {
                    if (add)  mTags.add(-j - 1);
                    else      mTags.remove((Integer) (-j - 1));
                    flags &= ~mask;
                }
            }
            recalculate();
            return this;
        }
        TagSet updateTags(long tags, boolean add) {
            for (int j = 0; tags != 0 && j < MAX_TAG_COUNT; j++) {
                long mask = 1L << j; 
                if ((tags & mask) != 0) {
                    // should really check to make sure the tag is reasonable
                    if (add)  mTags.add(j + TAG_ID_OFFSET);
                    else      mTags.remove((Integer) (j + TAG_ID_OFFSET));
                    tags &= ~mask;
                }
            }
            recalculate();
            return this;
        }

        private void recalculate() {
            mData.flags = 0;
            mData.tags  = 0;
            for (int value : mTags) {
                if (value < 0)
                    mData.flags |= 1 << Flag.getIndex(value);
                else
                    mData.tags |= 1L << Tag.getIndex(value);
            }
        }

        public String toString() { return mTags.toString(); }
    }

    private   String     mEncodedSenders;
    protected SenderList mSenderList;
    protected TagSet     mInheritedTagSet;

    Conversation(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_CONVERSATION && mData.type != TYPE_VIRTUAL_CONVERSATION)
            throw new IllegalArgumentException();
        mInheritedTagSet = new TagSet(mData.inheritedTags);
        mData.inheritedTags = null;
    }

    /** Returns the normalized subject of the conversation.  This is done by
     *  taking the <tt>Subject:</tt> header of the first message and removing
     *  prefixes (e.g. <tt>"Re:"</tt>) and suffixes (e.g. <tt>"(fwd)"</tt>)
     *  and the like.
     * 
     * @see ParsedMessage#normalizeSubject */
    public String getNormalizedSubject() {
        return super.getSubject();
    }

    @Override public String getSender() {
        return "";
    }

    /** Returns the number of messages in the conversation, as calculated from
     *  its list of children as fetched from the database.  This <u>should</u>
     *  always be equal to {@link MailItem#getSize()}; if it isn't, an error
     *  has occurred and been persisted to the database. */
    public int getMessageCount() {
        return (mData.children == null ? 0 : mData.children.size());
    }

    /** Returns the total number of messages in the conversation not tagged
     *  with the IMAP \Deleted flag.  This will almost always equal the value
     *  returned from {@link #getMessageCount()}. */
    public int getNondeletedCount() {
        return Math.max(0, getMessageCount() - mInheritedTagSet.count(Flag.ID_FLAG_DELETED));
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
            try {
                String encoded = mEncodedSenders;
                mEncodedSenders = null;
                // if the first message has been removed, this should throw
                //   an exception and force mRawSubject to be recalculated
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
        List<Message> msgs = getMessages(DbSearch.DEFAULT_SORT_ORDER);
        recalculateMetadata(msgs);
        return true;
    }

    private static final int RECALCULATE_CHANGE_MASK = Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD |
                                                       Change.MODIFIED_SIZE | Change.MODIFIED_SENDERS;

    SenderList recalculateMetadata(List<Message> msgs) throws ServiceException {
        Collections.sort(msgs, new Message.SortDateAscending());

        mEncodedSenders = null;
        mSenderList = new SenderList(msgs);

        mData.size = msgs.size();
        mData.unreadCount = 0;

        markItemModified(RECALCULATE_CHANGE_MASK);

        mData.children = new ArrayList<Integer>(msgs.size());
        mInheritedTagSet = new TagSet();
        for (Message msg : msgs) {
            super.addChild(msg);
            mInheritedTagSet.updateFlags(msg.getInternalFlagBitmask(), true).updateTags(msg.getTagBitmask(), true);
        }

        // need to rewrite the overview metadata
        ZimbraLog.mailbox.debug("resetting metadata: cid=" + mId + ", size was=" + mData.size + " is=" + mSenderList.size());
        saveData(null);
        return mSenderList;
    }

    public static final byte SORT_ID_ASCENDING   = DbSearch.SORT_BY_ID | DbSearch.SORT_ASCENDING;
    public static final byte SORT_DATE_ASCENDING = DbSearch.SORT_BY_DATE | DbSearch.SORT_ASCENDING;

    /** Returns all the {@link Message}s in this conversation.  The messages
     *  are fetched from the {@link Mailbox}'s cache, if possible; if not,
     *  they're fetched from the database.
     * 
     * @param sort  The sort order for the messages, specified by one of the
     *              <code>SORT_XXX</code> constants from {@link DbMailItem}. */
    List<Message> getMessages(byte sort) throws ServiceException {
        List<Message> msgs = new ArrayList<Message>(getMessageCount());
        Comparator<MailItem> cmp = getComparator(sort); 
        if (mData.children != null && (cmp != null || (sort & DbSearch.SORT_FIELD_MASK) == DbSearch.SORT_NONE)) {
            // try to get all our info from the cache to avoid a database trip
            for (int childId : mData.children) {
                Message msg = mMailbox.getCachedMessage(childId);
                if (msg == null)
                    break;
                msgs.add(msg);
            }

            if (msgs.size() == mData.children.size()) {
                if (cmp != null)
                    Collections.sort(msgs, cmp);
                return msgs;
            }
            // cache miss, so start over and fall through...
            msgs.clear();
        }

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
        assert(id != Mailbox.ID_AUTO_INCREMENT && msgs.length > 0);
        Arrays.sort(msgs, new Message.SortDateAscending());
        int date = 0, unread = 0;
        List<Integer> children = new ArrayList<Integer>();
        StringBuilder tags = new StringBuilder();
        SenderList sl = new SenderList(msgs);
        for (int i = 0; i < msgs.length; i++) {
            Message msg = msgs[i];
            if (msg == null)
                throw ServiceException.FAILURE("null Message in list", null);
            if (!msg.canAccess(ACL.RIGHT_READ))
                throw ServiceException.PERM_DENIED("you do not have sufficient rights on one of the messages");
            date = Math.max(date, msg.mData.date);
            unread += msg.mData.unreadCount;
            children.add(msg.mId);
            tags.append(i == 0 ? "-" : ",-").append(msg.mData.flags).append(',').append(msg.mData.tags);
        }

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_CONVERSATION;
        data.folderId    = Mailbox.ID_FOLDER_CONVERSATIONS;
        data.subject     = msgs.length > 0 ? msgs[0].getNormalizedSubject() : "";
        data.date        = date;
        data.size        = msgs.length;
        data.metadata    = encodeMetadata(DEFAULT_COLOR, 1, sl);
        data.unreadCount = unread;
        data.children    = children;
        data.inheritedTags = tags.toString();
        data.contentChanged(mbox);
        
        if (ZimbraLog.mailop.isDebugEnabled()) {
            StringBuilder msgIds = new StringBuilder();
            for (int i = 0; i < msgs.length; i++) {
                if (i > 0) {
                    msgIds.append(',');
                }
                msgIds.append(msgs[i].getId());
            }
            ZimbraLog.mailop.debug("Adding Conversation: id=%d, message(s): %s.",
                data.id, msgIds);
        }
        DbMailItem.create(mbox, data);

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
        close(Mailbox.getHash(getSubject()));
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
        for (Message msg : getMessages(DbSearch.DEFAULT_SORT_ORDER)) {
            // skip messages that don't need to be changed, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (msg.isUnread() == unread ) {
                continue;
            } else if (!msg.canAccess(ACL.RIGHT_WRITE)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            }

            msg.updateUnread(unread ? 1 : -1);
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
        if ((add ? mData.size : 0) == mInheritedTagSet.count(tag))
            return;
        if (tag.getId() == Flag.ID_FLAG_UNREAD)
            throw ServiceException.FAILURE("unread state must be set with alterUnread", null);
        
        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);

        boolean excludeAccess = false;

        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();
        List<Integer> targets = new ArrayList<Integer>();
        for (Message msg : getMessages(SORT_ID_ASCENDING)) {
            // skip messages that don't need to be changed, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (msg.isTagged(tag) == add) {
                continue;
            } else if (!msg.canAccess(ACL.RIGHT_WRITE)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            }

            // don't let the user tag things as "has attachments" or "draft"
            if (tag instanceof Flag && (tag.getBitmask() & Flag.FLAG_SYSTEM) != 0)
                throw MailServiceException.CANNOT_TAG(tag, msg);
            // since we're adding/removing a tag, the tag's unread count may change
        	if (tag.trackUnread() && msg.isUnread())
                tag.updateUnread(add ? 1 : -1);

            targets.add(msg.getId());
            msg.tagChanged(tag, add);
            mInheritedTagSet.update(tag, add);
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
            DbMailItem.alterTag(tag, targets, add);
        }
    }

    @Override protected void inheritedTagChanged(Tag tag, boolean add) {
        if (tag == null)
            return;
        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);
        mInheritedTagSet.update(tag, add);
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

        List<Message> msgs = getMessages(SORT_ID_ASCENDING);
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
            
            // moved an item out of the spam folder, need to index it
            if (msg.inSpam() && !target.inSpam()) {
                if (msg.isIndexed() && msg.getIndexId() <= 0) {
                    msg.indexIdChanged(msg.getId());
                    indexUpdated.add(msg);
                }
            }

            // handle folder message counts
            source.updateSize(-1, -msg.getTotalSize());
            target.updateSize(1, msg.getTotalSize());

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
                ZimbraLog.mailop.info("Moving %s to %s.  Affected message ids: %s",
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
        super.addChild(child);

        // update inherited tags, if applicable
        if (child.mData.tags != 0 || child.mData.flags != 0) {
            int oldFlags = mData.flags;
            long oldTags = mData.tags;

            if (child.mData.tags != 0)
                mInheritedTagSet.updateTags(child.mData.tags, true);
            if (child.mData.flags != 0)
                mInheritedTagSet.updateFlags(child.mData.flags, true);

            if (mData.flags != oldFlags)  markItemModified(Change.MODIFIED_FLAGS);
            if (mData.tags != oldTags)    markItemModified(Change.MODIFIED_TAGS);
        }

        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_SENDERS);

        // FIXME: this ordering is to work around the fact that when getSenderList has to
        //   recalc the metadata, it uses the already-updated DB message state to do it...
        mData.date = mMailbox.getOperationTimestamp();
        mData.contentChanged(mMailbox);

        if (!mMailbox.hasListeners(Session.Type.SOAP)) {
            instantiateSenderList();
            mData.size++;
            try {
                if (mSenderList != null)
                    mSenderList.add((Message) child);
            } catch (SenderList.RefreshException slre) {
                mSenderList = null;
            }
            saveMetadata();
        } else {
            boolean recalculated = loadSenderList();
            if (!recalculated) {
                mData.size++;
                try {
                    mSenderList.add((Message) child);
                    saveMetadata();
                } catch (SenderList.RefreshException slre) {
                    recalculateMetadata(getMessages(SORT_ID_ASCENDING));
                }
            }
        }
    }

    @Override void removeChild(MailItem child) throws ServiceException {
        // superclass removeChild() updates the unread count and child list
        super.removeChild(child);

        // remove the last message and the conversation goes away
        if (getMessageCount() == 0) {
            delete();
            return;
        }

        // update inherited tags, if applicable
        if (child.mData.tags != 0 || child.mData.flags != 0) {
            int oldFlags = mData.flags;
            long oldTags = mData.tags;

            if (child.mData.tags != 0)
                mInheritedTagSet.updateTags(child.mData.tags, false);
            if (child.mData.flags != 0)
                mInheritedTagSet.updateFlags(child.mData.flags, false);

            if (mData.flags != oldFlags)  markItemModified(Change.MODIFIED_FLAGS);
            if (mData.tags != oldTags)    markItemModified(Change.MODIFIED_TAGS);
        }

        markItemModified(Change.MODIFIED_SIZE | Change.MODIFIED_SENDERS);

        if (!mMailbox.hasListeners(Session.Type.SOAP)) {
            mEncodedSenders = null;
            mSenderList = null;
            mData.size--;
            saveMetadata(null);
        } else {
            List<Message> msgs = getMessages(SORT_ID_ASCENDING);
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

        if (mData.children == null || mData.children.isEmpty())
            return info;
        List<Message> msgs = getMessages(SORT_ID_ASCENDING);
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
        if (info.incomplete) {
            // *some* of the messages remain; recalculate the data based on this
            int oldSize = getMessageCount(), remaining = oldSize - info.itemIds.size();
            List<Message> msgs = new ArrayList<Message>(remaining);
            for (int i = 0; i < oldSize; i++) {
                int childId = mData.children.get(i);
                if (!info.itemIds.contains(childId))
                    msgs.add(mMailbox.getMessageById(childId));
            }
            recalculateMetadata(msgs);
        }

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
    	return encodeMetadata(meta, mColor, mVersion, encoded);
    }

    static String encodeMetadata(byte color, int version, SenderList senders) {
        return encodeMetadata(new Metadata(), color, version, senders.toString()).toString();
    }

    static Metadata encodeMetadata(Metadata meta, byte color, int version, String encodedSenders) {
        meta.put(Metadata.FN_PARTICIPANTS, encodedSenders);
        return MailItem.encodeMetadata(meta, color, version);
    }


    private static final String CN_INHERITED_TAGS = "inherited";

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("conversation: {");
        appendCommonMembers(sb);
        sb.append(CN_INHERITED_TAGS).append(": [").append(mInheritedTagSet.toString()).append("], ");
        sb.append("}");
        return sb.toString();
    }
}
