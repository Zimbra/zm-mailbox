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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.calendar.*;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.TnefConverter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;


/**
 * @author schemers
 */
public class Message extends MailItem {

    static class DraftInfo {
        String identityId;
        String replyType;
        String origId;

        public DraftInfo()  { }
        public DraftInfo(String ident)                        { identityId = ident; }
        public DraftInfo(String rt, String id)                { replyType = rt;  origId = id; }
        public DraftInfo(String rt, String id, String ident)  { replyType = rt;  origId = id;  identityId = ident; }
        public DraftInfo(Metadata meta) {
            identityId = meta.get(Metadata.FN_IDENTITY_ID, null);
            replyType = meta.get(Metadata.FN_REPLY_TYPE, null);
            origId = meta.get(Metadata.FN_REPLY_ORIG, null);
        }
    }

    public static class CalendarItemInfo {
        private int mCalendarItemId;
        private int mComponentNo;

        public CalendarItemInfo(int calItemId, int componentNo) {
            mCalendarItemId = calItemId;
            mComponentNo = componentNo;
        }
        public int getCalendarItemId()  { return mCalendarItemId; }
        public int getComponentNo()    { return mComponentNo; }

        private static final String FN_CALITEMID = "a";
        private static final String FN_COMPNO = "c";

        Metadata encodeMetadata() {
            Metadata md = new Metadata();
            md.put(FN_CALITEMID, mCalendarItemId);
            md.put(FN_COMPNO, mComponentNo);
            return md; 
        }

        static CalendarItemInfo decodeMetadata(Metadata md) throws ServiceException {
            int calItemId = (int) md.getLong(FN_CALITEMID);
            int componentNo = (int) md.getLong(FN_COMPNO);
            return new CalendarItemInfo(calItemId, componentNo);
        }
    }

    /** Class logger for Message class */
    static Log sLog = LogFactory.getLog(Message.class);

    private String mSender;
    private String mRecipients;
    private String mFragment;
    private String mRawSubject;

    private DraftInfo mDraftInfo;
    private ArrayList<CalendarItemInfo> mCalendarItemInfos;


    /**
     * this one will call back into decodeMetadata() to do our initialization
     * 
     * @param mbox
     * @param ud
     * @throws ServiceException
     */
    Message(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        if (mData.type != TYPE_MESSAGE  && mData.type != TYPE_CHAT)
            throw new IllegalArgumentException();
        if (mData.parentId < 0)
            mData.parentId = -mId;
    }
    
    /** Returns whether the Message was created as a draft.  Note that this
     *  can only be set when the Message is created; it cannot be altered
     *  thereafter. */
    public boolean isDraft() {
        return isTagged(mMailbox.mDraftFlag);
    }

    /** Returns the <code>To:</code> header of the message, if the message
     *  was sent by the user.  Retuns <code>""</code> otherwise. */
    public String getRecipients() {
        return (mRecipients == null ? "" : mRecipients);
    }

    /** Returns the first 100 characters of the message's content.  The system
     *  does its best to remove quoted text, etc. before calculating the
     *  fragment.
     *
     * @see com.zimbra.cs.index.Fragment */
    public String getFragment() {
        return (mFragment == null ? "" : mFragment);
    }

    /** Returns the normalized subject of the message.  This is done by
     *  taking the <code>Subject:</code> header and removing prefixes (e.g.
     *  <code>"Re:"</code>) and suffixes (e.g. <code>"(fwd)"</code>) and
     *  the like.
     * 
     * @see ParsedMessage#normalizeSubject */
    public String getNormalizedSubject() {
        return super.getSubject();
    }

    /** Returns the raw subject of the message.  This is taken directly from
     *  the <tt>Subject:</tt> header with no processing. */
    @Override public String getSubject() {
        return (mRawSubject == null ? "" : mRawSubject);
    }

    /** Returns the <tt>From:</tt> header of the message if available;
     *  if not, returns the <tt>Sender:</tt> header. */
    @Override public String getSender() {
        return (mSender == null ? "" : mSender);
    }

    @Override public String getSortSubject() {
        String subject = getNormalizedSubject();
        return subject.toUpperCase().substring(0, Math.min(DbMailItem.MAX_SUBJECT_LENGTH, subject.length()));
    }

    @Override public String getSortSender() {
        String sender = new ParsedAddress(getSender()).getSortString();
        return sender.toUpperCase().substring(0, Math.min(DbMailItem.MAX_SENDER_LENGTH, sender.length()));
    }

    /** Returns whether the Message was sent by the owner of this mailbox.
     *  Note that this can only be set when the Message is created; it cannot
     *  be altered thereafter.*/
    public boolean isFromMe() {
        return (mData.flags & Flag.BITMASK_FROM_ME) != 0;
    }

    /** Returns the ID of the {@link Conversation} the Message belongs to.
     *  If the ID is negative, it refers to a single-message
     *  {@link VirtualConversation}.*/
    public int getConversationId() {
        return mData.parentId;
    }

    /** Returns the ID of the Message that this draft message is in reply to.
     * 
     * @return The ID of the Message that this draft message is in reply to,
     *         or -1 for Messages that are not drafts or not replies/forwards.
     * @see #getDraftReplyType */
    public String getDraftOrigId() {
        return (mDraftInfo == null || mDraftInfo.origId == null ? "" : mDraftInfo.origId);
    }

    /** Returns the "reply type" for a draft message.
     * 
     * @return <code>"f"</code> if this draft message is a forward,
     *         <code>"r"</code> if this draft message is a reply, or
     *         <code>""</code> for Messages that are not drafts or not
     *         replies/forwards.
     * @see #getDraftOrigId
     * @see com.zimbra.cs.service.mail.SendMsg#TYPE_FORWARD
     * @see com.zimbra.cs.service.mail.SendMsg#TYPE_REPLY */
    public String getDraftReplyType() {
        return (mDraftInfo == null || mDraftInfo.replyType == null ? "" : mDraftInfo.replyType);
    }

    /** Returns the ID of the {@link com.zimbra.cs.account.Identity} that was
     *  used to compose this draft message.
     * 
     * @return The ID of the Identity used to compose this draft message, or ""
     *         for Messages that are not drafts or did not specify an identity.
     * @see #getDraftReplyType
     * @see #getDraftOrigId() */
    public String getDraftIdentityId() {
        return (mDraftInfo == null || mDraftInfo.identityId == null ? "" : mDraftInfo.identityId);
    }

    /** Returns whether the Message has a vCal attachment. */
    public boolean isInvite() {
        return (mData.flags & Flag.BITMASK_INVITE) != 0;
    }
    
    public boolean hasCalendarItemInfos() {
        return mCalendarItemInfos != null;
    }

    public Iterator<CalendarItemInfo> getCalendarItemInfoIterator() {
        if (mCalendarItemInfos != null)
            return mCalendarItemInfos.iterator();
        else
            return new ArrayList<CalendarItemInfo>().iterator();
    }

    public CalendarItemInfo getCalendarItemInfo(int componentId) {
        if (mCalendarItemInfos != null &&
            (componentId < 0 || componentId < mCalendarItemInfos.size()))
            return mCalendarItemInfos.get(componentId);
        else
            return null;
    }

    /** Returns a JavaMail {@link javax.mail.internet.MimeMessage}
     *  encapsulating the message content.  If possible, TNEF and uuencoded
     *  attachments are expanded and their components are presented as
     *  standard MIME attachments.  If TNEF or uuencode decoding fails, the
     *  MimeMessage wraps the raw message content.
     * 
     * @return A MimeMessage wrapping the RFC822 content of the Message.
     * @throws ServiceException when errors occur opening, reading,
     *                          uncompressing, or parsing the message file,
     *                          or when the file does not exist.
     * @see MailItem#getContentStream()
     * @see MailItem#getContent()
     * @see TnefConverter
     * @see UUEncodeConverter */
    public MimeMessage getMimeMessage() throws ServiceException {
        return getMimeMessage(true);
    }

    /** Returns a JavaMail {@link javax.mail.internet.MimeMessage}
     *  encapsulating the message content.  If <tt>runConverters</tt> is
     *  <tt>true</tt>, TNEF and uuencoded attachments are expanded and their
     *  components are presented as standard MIME attachments.  If
     *  <tt>runConverters</tt> is <tt>false</tt> or if TNEF or uuencode
     *  decoding fails, the MimeMessage wraps the raw message content.
     * 
     * @return A MimeMessage wrapping the RFC822 content of the Message.
     * @throws ServiceException when errors occur opening, reading,
     *                          uncompressing, or parsing the message file,
     *                          or when the file does not exist.
     * @see #getContentStream()
     * @see #getContent()
     * @see TnefConverter
     * @see UUEncodeConverter */
    public MimeMessage getMimeMessage(boolean runConverters) throws ServiceException {
        return MessageCache.getMimeMessage(this, runConverters);
    }

    @Override boolean isTaggable()      { return true; }
    @Override boolean isCopyable()      { return true; }
    @Override boolean isMovable()       { return true; }
    @Override boolean isMutable()       { return isTagged(mMailbox.mDraftFlag); }
    @Override boolean isIndexed()       { return true; }
    @Override boolean canHaveChildren() { return false; }

    @Override boolean canParent(MailItem item)  { return false; }

    static class MessageCreateFactory {
        Message create(Mailbox mbox, UnderlyingData data) throws ServiceException {
            return new Message(mbox, data);
        }
        
        byte getType() {
            return TYPE_MESSAGE;
        }
    }
    
    static Message create(int id, Folder folder, Conversation conv, ParsedMessage pm,
                          int msgSize, String digest, short volumeId, boolean unread,
                          int flags, long tags, DraftInfo dinfo, boolean noICal, ZVCalendar cal)  
    throws ServiceException {
        return createInternal(id, folder, conv, pm, msgSize, digest, volumeId, unread, 
                    flags, tags, dinfo, noICal, cal,new MessageCreateFactory());
    }
    
    protected static Message createInternal(int id, Folder folder, Conversation conv, ParsedMessage pm,
                int msgSize, String digest, short volumeId, boolean unread,
                int flags, long tags, DraftInfo dinfo, boolean noICal, ZVCalendar cal, 
                MessageCreateFactory fact) throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_MESSAGE))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        Mailbox mbox = folder.getMailbox();

        List<Invite> components = null;
        String methodStr = null;
        // Skip calendar processing if message is being filed as spam or trash.
        if (cal != null && folder.getId() != Mailbox.ID_FOLDER_SPAM && folder.getId() != Mailbox.ID_FOLDER_TRASH) {
            Account acct = mbox.getAccount();

            // XXX: shouldn't we just be checking flags for Flag.FLAG_FROM_ME?
            //   boolean sentByMe = (flags & Flag.FLAG_FROM_ME) != 0;
            boolean sentByMe = false;
            String pmSender = pm.getSenderEmail();
            if (pmSender != null && pmSender.length() > 0)
                sentByMe = AccountUtil.addressMatchesAccount(acct, pmSender);

            try {
                components = Invite.createFromCalendar(acct, pm.getFragment(), cal, sentByMe, mbox, id);
                methodStr = cal.getPropVal(ICalTok.METHOD, ICalTok.PUBLISH.toString()).toUpperCase();
                if (components != null) {
                    flags |= Flag.BITMASK_INVITE;
                    if (ICalTok.PUBLISH.toString().equals(methodStr)) {
                        // If method is PUBLISH, we don't process the calendar part.  Mark it as
                        // a regular attachment instead.
                        flags |= Flag.BITMASK_ATTACHED;
                        components = null;
                    }
                }
            } catch (Exception e) {
                ZimbraLog.calendar.warn("Unable to process iCalendar attachment", e);
            }
        }

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = fact.getType();
        if (conv != null)
            data.parentId = conv.getId();
        data.folderId    = folder.getId();
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false))
            data.indexId     = id;
        data.volumeId    = volumeId;
        data.imapId      = id;

        // Make sure the date is not negative or in the future (bug 17031)
        long date = pm.getReceivedDate();
        long now = System.currentTimeMillis();
        if (date < 0 || date > now) {
            date = now;
        }
        data.date        = (int) (date / 1000);
        
        data.size        = msgSize;
        data.setBlobDigest(digest);
        data.flags       = flags & (Flag.FLAGS_MESSAGE | Flag.FLAGS_GENERIC);
        data.tags        = tags;
        data.sender      = pm.getParsedSender().getSortString();
        data.subject     = pm.getNormalizedSubject();
        data.metadata    = encodeMetadata(DEFAULT_COLOR, 1, pm, flags, dinfo, null);
        data.unreadCount = unread ? 1 : 0; 
        data.contentChanged(mbox);

        ZimbraLog.mailop.info("Adding Message: id=%d, Message-ID=%s, parentId=%d, folderId=%d, folderName=%s.",
            data.id, pm.getMessageID(), data.parentId, folder.getId(), folder.getName());
        DbMailItem.create(mbox, data);
        Message msg = fact.create(mbox, data);

        // process the components in this invite (must do this last so blob is created, etc)
        if (components != null) {
            try {
                msg.processInvitesAfterCreate(methodStr, folder.getId(), volumeId, !noICal, pm, components);
            } catch (Exception e) {
                ZimbraLog.calendar.warn("Unable to process iCalendar attachment", e);
            }
        }

        msg.finishCreation(conv);
        return msg;
    }

    /** This has to be done as a separate step, after the MailItem has been
     *  added, because of foreign key constraints on the CalendarItems table
     * @param invites */
    private void processInvitesAfterCreate(String method, int folderId, short volumeId, boolean createCalItem, ParsedMessage pm, List<Invite> invites) 
    throws ServiceException {
        // since this is the first time we've seen this Invite Message, we need to process it
        // and see if it updates an existing CalendarItem in the database table, or whatever...
        boolean updatedMetadata = false;

        // Ignore alarms set by organizer.
        boolean ignoreOrganizerAlarm = false;

        for (Invite cur : invites) {
            if (ignoreOrganizerAlarm)
                cur.clearAlarms();

            boolean calItemIsNew = false;
            boolean modifiedCalItem = false;
            CalendarItem calItem = null;
            boolean success = false;
            try {
                calItem = mMailbox.getCalendarItemByUid(cur.getUid());
                if (createCalItem) {
                    if (calItem == null) {
                        // ONLY create a calendar item if this is a REQUEST method...otherwise don't.
                        if (method.equals(ICalTok.REQUEST.toString())) {
                            int flags = Flag.BITMASK_INDEXING_DEFERRED;
                            mMailbox.incrementIndexDeferredCount(1);
                            int defaultFolder = cur.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
                            calItem = mMailbox.createCalendarItem(defaultFolder, volumeId, flags, 0, cur.getUid(), pm, cur, 0);
                            calItemIsNew = true;
                        } else {
                            sLog.info("Mailbox " + getMailboxId()+" Message "+getId()+" SKIPPING Invite "+method+" b/c no CalendarItem could be found");
                            success = true;
                            continue; // for now, just ignore this Invitation
                        }
                    } else {
                        // When updating an existing calendar item, ignore the
                        // passed-in folderId which will usually be Inbox.  Leave
                        // the calendar item in the folder it's currently in.
                        modifiedCalItem = calItem.processNewInvite(pm, cur, calItem.getFolderId(), volumeId);
                    }
                }
                
                if (calItem != null) {
                    CalendarItemInfo info = new CalendarItemInfo(calItem.getId(), cur.getComponentNum());
                    if (mCalendarItemInfos == null) {
                        mCalendarItemInfos = new ArrayList<CalendarItemInfo>();
                    }
                    mCalendarItemInfos.add(info);
                    updatedMetadata = true;
                    
                    if (calItemIsNew || modifiedCalItem) {
                        mMailbox.queueForIndexing(calItem, !calItemIsNew, null);
                    }
                    
                }
                success = true;
            } finally {
                if (!success && calItem != null) {
                    // Error occurred and the calItem in memory may be out of sync with
                    // the database.  Uncache it here, because the error will be ignore
                    // by this method's caller.
                    getMailbox().uncache(calItem);
                }
            }
        }
        
        if (updatedMetadata)
            saveMetadata();
    }

    /** Updates the in-memory unread counts for the item.  Also updates the
     *  item's folder, its tag, and its parent.  Note that the parent is not
     *  fetched from the database, so notifications may be off in the case of
     *  uncached {@link Conversation}s when a {@link Message} changes state.
     * 
     * @param delta  The change in unread count for this item. */
    @Override protected void updateUnread(int delta) throws ServiceException {
        if (delta == 0 || !trackUnread())
            return;
        markItemModified(Change.MODIFIED_UNREAD);

        // update our unread count (should we check that we don't have too many unread?)
        mData.unreadCount += delta;
        if (mData.unreadCount < 0)
            throw ServiceException.FAILURE("inconsistent state: unread < 0 for " + getClass().getName() + " " + mId, null);

        // update the folder's unread count
        getFolder().updateUnread(delta);

        // update the parent's unread count
        MailItem parent = getCachedParent();
        if (parent != null)
            parent.updateUnread(delta);

        // tell the tags about the new unread item
        updateTagUnread(delta);
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override MailItem copy(Folder folder, int id, int parentId, short destVolumeId) throws IOException, ServiceException {
        Message copy = (Message) super.copy(folder, id, parentId, destVolumeId);

        Conversation parent = (Conversation) getParent();
        if (parent instanceof VirtualConversation && parent.getId() == parentId && !isDraft() && inSpam() == folder.inSpam()) {
            Conversation conv = mMailbox.createConversation(new Message[] { this, copy }, Mailbox.ID_AUTO_INCREMENT);
            DbMailItem.changeOpenTarget(Mailbox.getHash(getNormalizedSubject()), this, conv.getId());
            parent.removeChild(this);
        }
        return copy;
    }

    @Override public List<org.apache.lucene.document.Document> generateIndexData(boolean doConsistencyCheck) throws MailItem.TemporaryIndexingException {
        try {
            ParsedMessage pm = null;
            synchronized (getMailbox()) {
                // force the pm's received-date to be the correct one
                pm = new ParsedMessage(getMimeMessage(), getDate(), getMailbox().attachmentsIndexingEnabled());
            }
            
            if (doConsistencyCheck) {
                // because of bug 8263, we sometimes have fragments that are incorrect;
                //   check them here and correct them if necessary
                String fragment = pm.getFragment();
                if (!getFragment().equals(fragment == null ? "" : fragment))
                    getMailbox().reanalyze(getId(), getType(), pm);
            }
            
            // don't hold the lock while extracting text!
            pm.analyzeFully();
            
            if (pm.hasTemporaryAnalysisFailure())
                throw new MailItem.TemporaryIndexingException();
            
            return pm.getLuceneDocuments();
        } catch (ServiceException e) {
            ZimbraLog.index.warn("Unable to generate index data for Message "+getId()+". Item will not be indexed", e);
            return new ArrayList<org.apache.lucene.document.Document>(0);
        }
    }


    public void reanalyze() throws ServiceException {
        ParsedMessage pm = new ParsedMessage(getMimeMessage(), getDate(), getMailbox().attachmentsIndexingEnabled());
        reanalyze(pm);
    }

    @Override void reanalyze(Object data) throws ServiceException {
        if (!(data instanceof ParsedMessage))
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedMessage object", null);

        ParsedMessage pm = (ParsedMessage) data;
        int size;
        try {
            size = pm.getRawSize();
        } catch (Exception e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        }

        // make sure the SUBJECT is correct
        if (!getSubject().equals(pm.getSubject()))
            markItemModified(Change.MODIFIED_SUBJECT);
        boolean subjectChanged = !getNormalizedSubject().equals(pm.getNormalizedSubject());
        mRawSubject = pm.getSubject();
        mData.subject = pm.getNormalizedSubject();

        // the fragment may have changed
        mFragment = pm.getFragment();

        // handle the message's PARENT
        boolean parentChanged = false;
        MailItem parent = getParent();
        if (subjectChanged && mData.parentId != -mId) {
            mData.parentId = -mId;
            parentChanged = true;
        }

        // make sure the "attachments" FLAG is correct
        boolean hadAttachment = (mData.flags & Flag.BITMASK_ATTACHED) != 0;
        mData.flags &= ~Flag.BITMASK_ATTACHED;
        if (pm.hasAttachments())
            mData.flags |= Flag.BITMASK_ATTACHED;
        if (hadAttachment != pm.hasAttachments()) {
            markItemModified(Change.MODIFIED_FLAGS);
            parent.tagChanged(mMailbox.mAttachFlag, pm.hasAttachments());
        }

        // make sure the "urgency" FLAGs are correct
        int oldUrgency = mData.flags & (Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
        int urgency = pm.getPriorityBitmask();
        mData.flags &= ~(Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
        mData.flags |= urgency;
        if (oldUrgency != urgency) {
            markItemModified(Change.MODIFIED_FLAGS);
            parent.tagChanged(mMailbox.mUrgentFlag, urgency == Flag.BITMASK_HIGH_PRIORITY);
            parent.tagChanged(mMailbox.mBulkFlag, urgency == Flag.BITMASK_LOW_PRIORITY);
        }

        // update the SIZE and METADATA
        if (mData.size != size) {
            markItemModified(Change.MODIFIED_SIZE);
            mMailbox.updateSize(size - mData.size, false);
            getFolder().updateSize(0, size - mData.size);
            mData.size = size;
        }
        
        String metadata = encodeMetadata(mColor, mVersion, pm, mData.flags, mDraftInfo, mCalendarItemInfos);

        // rewrite the DB row to reflect our new view
        saveData(pm.getParsedSender().getSortString(), metadata);

        // if the message is part of a real conversation, need to break it out
        if (parentChanged)
            parent.removeChild(this);
        else if (parent instanceof VirtualConversation)
            ((VirtualConversation) parent).recalculateMetadata(Arrays.asList(new Message[] { this } ));
    }

    @Override void detach() throws ServiceException {
        MailItem parent = getParent();
        if (!(parent instanceof Conversation))
            return;
        if (parent.getSize() <= 1) {
            mMailbox.closeConversation((Conversation) parent, null);
        } else {
            // remove this message from its (real) conversation
            markItemModified(Change.MODIFIED_PARENT);
            parent.removeChild(this);
            // and place it in a new, non-"opened", virtual conversation
            VirtualConversation vconv = new VirtualConversation(mMailbox, this);
            mData.parentId = vconv.getId();
            DbMailItem.setParent(this, vconv);
        }
    }


    @Override void delete(DeleteScope scope, boolean writeTombstones) throws ServiceException {
        MailItem parent = getParent();
        if (parent instanceof Conversation && ((Conversation) parent).getMessageCount() == 1)
            parent.delete(DeleteScope.ENTIRE_ITEM, writeTombstones);
        else
            super.delete(scope, writeTombstones);
    }


    @Override void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        mSender = meta.get(Metadata.FN_SENDER, null);
        mRecipients = meta.get(Metadata.FN_RECIPIENTS, null);
        mFragment = meta.get(Metadata.FN_FRAGMENT, null);

        if (meta.containsKey(Metadata.FN_CALITEM_IDS)) {
            mCalendarItemInfos = new ArrayList<CalendarItemInfo>();
            MetadataList mdList = meta.getList(Metadata.FN_CALITEM_IDS);
            for (int i = 0; i < mdList.size(); i++) {
                Metadata md = mdList.getMap(i);
                mCalendarItemInfos.add(CalendarItemInfo.decodeMetadata(md));
            }
        }

        Metadata draftMeta = meta.getMap(Metadata.FN_DRAFT, true);
        if (draftMeta != null)
            mDraftInfo = new DraftInfo(draftMeta);

        mRawSubject = mData.subject;
        String prefix = meta.get(Metadata.FN_PREFIX, null);
        if (prefix != null)
            mRawSubject = (mData.subject == null ? prefix : prefix + mData.subject);
        String rawSubject = meta.get(Metadata.FN_RAW_SUBJ, null);
        if (rawSubject != null)
            mRawSubject = rawSubject;
    }

    @Override Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mVersion, mSender, mRecipients, mFragment, mData.subject, mRawSubject, mDraftInfo, mCalendarItemInfos);
    }

    private static String encodeMetadata(byte color, int version, ParsedMessage pm, int flags, DraftInfo dinfo, List<CalendarItemInfo> calItemInfos) {
        // cache the "To" header only for messages sent by the user
        String recipients = ((flags & Flag.BITMASK_FROM_ME) == 0 ? null : pm.getRecipients());
        return encodeMetadata(new Metadata(), color, version, pm.getSender(), recipients, pm.getFragment(), pm.getNormalizedSubject(), pm.getSubject(), dinfo, calItemInfos).toString();
    }

    static Metadata encodeMetadata(Metadata meta, byte color, int version, String sender, String recipients, String fragment, String subject, String rawSubject, DraftInfo dinfo, List<CalendarItemInfo> calItemInfos) {
        // try to figure out a simple way to make the raw subject from the normalized one
        String prefix = null;
        if (rawSubject == null || rawSubject.equals(subject))
            rawSubject = null;
        else if (rawSubject.endsWith(subject)) {
            prefix = rawSubject.substring(0, rawSubject.length() - subject.length());
            rawSubject = null;
        }

        meta.put(Metadata.FN_SENDER,     sender);
        meta.put(Metadata.FN_RECIPIENTS, recipients);
        meta.put(Metadata.FN_FRAGMENT,   fragment);
        meta.put(Metadata.FN_PREFIX,     prefix);
        meta.put(Metadata.FN_RAW_SUBJ,   rawSubject);
        
        if (calItemInfos != null) {
            MetadataList mdList = new MetadataList();
            for (CalendarItemInfo info : calItemInfos)
                mdList.add(info.encodeMetadata());
            meta.put(Metadata.FN_CALITEM_IDS, mdList); 
        }

        if (dinfo != null) {
            Metadata dmeta = new Metadata();
            dmeta.put(Metadata.FN_REPLY_ORIG, dinfo.origId);
            dmeta.put(Metadata.FN_REPLY_TYPE, dinfo.replyType);
            dmeta.put(Metadata.FN_IDENTITY_ID, dinfo.identityId);
            meta.put(Metadata.FN_DRAFT, dmeta);
        }

        return MailItem.encodeMetadata(meta, color, version);
    }


    private static final String CN_SENDER     = "sender";
    private static final String CN_RECIPIENTS = "to";
    private static final String CN_FRAGMENT   = "fragment";

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("message: {");
        appendCommonMembers(sb);
        sb.append(CN_SENDER).append(": ").append(mSender).append(", ");
        if (mRecipients != null)
            sb.append(CN_RECIPIENTS).append(": ").append(mRecipients).append(", ");
        sb.append(CN_FRAGMENT).append(": ").append(mFragment);
        sb.append("}");
        return sb.toString();
    }
}
