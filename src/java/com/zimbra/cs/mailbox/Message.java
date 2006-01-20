/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 13, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.calendar.*;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.TnefConverter;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author schemers
 */
public class Message extends MailItem {

    static class DraftInfo {
        String replyType;
        int    origId = -1;

        public DraftInfo()  { }
        public DraftInfo(String rt, int id)  { replyType = rt;  origId = id; }
        public DraftInfo(Metadata meta) throws ServiceException {
            replyType = meta.get(Metadata.FN_REPLY_TYPE);
            origId = (int) meta.getLong(Metadata.FN_REPLY_ORIG, -1);
        }
    }

    public static class ApptInfo {
        private int mAppointmentId;
        private int mComponentNo;

        public ApptInfo(int apptId, int componentNo) {
            mAppointmentId = apptId;
            mComponentNo = componentNo;
        }

        public int getAppointmentId()  { return mAppointmentId; }
        public int getComponentNo()    { return mComponentNo; }

        private static final String FN_APPTID = "a";
        private static final String FN_COMPNO = "c";

        Metadata encodeMetadata() {
            Metadata md = new Metadata();
            md.put(FN_APPTID, mAppointmentId);
            md.put(FN_COMPNO, mComponentNo);
            return md; 
        }

        static ApptInfo decodeMetadata(Metadata md) throws ServiceException {
            int apptId = (int) md.getLong(FN_APPTID);
            int componentNo = (int) md.getLong(FN_COMPNO);
            return new ApptInfo(apptId, componentNo);
        }
    }

    /** Class logger for Message class */
    static Log sLog = LogFactory.getLog(Message.class);

	private String mSender;
    private String mRecipients;
    private int    mImapUID;
	private String mFragment;
    private String mRawSubject;

    private DraftInfo mDraftInfo;
    private ArrayList<ApptInfo> mApptInfos;


    /**
     * this one will call back into decodeMetadata() to do our initialization
     * 
     * @param mbox
     * @param ud
     * @throws ServiceException
     */
	Message(Mailbox mbox, UnderlyingData ud) throws ServiceException {
		super(mbox, ud);
        if (mData.type != TYPE_MESSAGE)
			throw new IllegalArgumentException();
        if (mData.parentId == -1)
            mData.parentId = -mId;
	}

    /** Returns whether the Message was created as a draft.  Note that this
     *  can only be set when the Message is created; it cannot be altered
     *  thereafter. */
    public boolean isDraft() {
        return isTagged(mMailbox.mDraftFlag);
    }

    /** Returns the message's originator.  This is the <code>From:</code>
     *  header from the message or, if none exists, the <code>Sender:</code>
     *  header. */
    public String getSender() {
        return (mSender == null ? "" : mSender);
    }

    /** Returns the <code>To:</code> header of the message, if the message
     *  was sent by the user.  Retuns <code>""</code> otherwise. */
    public String getRecipients() {
        return (mRecipients == null ? "" : mRecipients);
    }

    /** Returns the IMAP UID for the message.  This will usually be the same
     *  as the Message's ID; it will differ only if the message has ever been
     *  {@link #move moved} to a folder that was currently SELECTed in an
     *  active IMAP session. */
    public int getImapUID() {
        return (mImapUID > 0 ? mImapUID : mId);
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
     *  the <code>Subject:</code> header with no processing. */
    public String getSubject() {
        return (mRawSubject == null ? "" : mRawSubject);
    }

    /** Returns whether the Message was sent by the owner of this mailbox.
     *  Note that this can only be set when the Message is created; it cannot
     *  be altered thereafter.*/
    public boolean isFromMe() {
        return (mData.flags & Flag.FLAG_FROM_ME) != 0;
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
    public int getDraftOrigId() {
        return (mDraftInfo == null ? -1 : mDraftInfo.origId);
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

    /** Returns whether the Message has a vCal attachment. */
    public boolean isInvite() {
        return mApptInfos != null;
    }

    public Iterator<ApptInfo> getApptInfoIterator() {
        return mApptInfos.iterator();
    }

    public ApptInfo getApptInfo(int componentId) {
        if (componentId < 0 || componentId < mApptInfos.size())
            return mApptInfos.get(componentId);
        else
            return null;
    }

    /** Returns an {@link InputStream} of the raw, uncompressed content of
     *  the message.  This is the message body as received via SMTP; no
     *  postprocessing has been performed to make opaque attachments (e.g.
     *  TNEF) visible.
     * 
     * @return The InputStream fetched from the {@link MessageCache}.
     * @throws ServiceException when the message file does not exist.
     * @see #getMimeMessage()
     * @see #getMessageContent() */
    public InputStream getRawMessage() throws ServiceException {
        return MessageCache.getRawContent(this);
    }

    /** Returns the raw, uncompressed content of the message as a byte array.
     *  This is the message body as received via SMTP; no postprocessing has
     *  been performed to make opaque attachments (e.g. TNEF) visible.
     * 
     * @return The InputStream returned by the {@link MessageCache}.
     * @throws ServiceException when the message file does not exist.
     * @see #getMimeMessage()
     * @see #getRawMessage() */
    public byte[] getMessageContent() throws ServiceException {
        return MessageCache.getItemContent(this);
    }

    /** Returns a JavaMail {@link javax.mail.internet.MimeMessage}
     *  encapsulating the message content.  If possible, TNEF attachments
     *  are expanded and their components are presented as standard MIME
     *  attachments.  If TNEF decoding fails, the MimeMessage wraps the raw
     *  message content.
     * 
     * @return A MimeMessage wrapping the RFC822 content of the Message.
     * @throws ServiceException when errors occur opening, reading,
     *                          uncompressing, or parsing the message file,
     *                          or when the file does not exist.
     * @see #getRawMessage()
     * @see #getMessageContent()
     * @see TnefConverter */
    public MimeMessage getMimeMessage() throws ServiceException {
        return MessageCache.getMimeMessage(this);
    }

    public static final class SortImapUID implements Comparator<Message> {
        public int compare(Message m1, Message m2) {
            int uid1 = m1.getImapUID();
            int uid2 = m2.getImapUID();

            return uid1 - uid2;
        }
    }

    boolean isTaggable()      { return true; }
    boolean isCopyable()      { return true; }
    boolean isMovable()       { return true; }
    boolean isMutable()       { return true; }
    boolean isIndexed()       { return true; }
    boolean canHaveChildren() { return false; }

    boolean canParent(MailItem item)  { return false; }


    static Message create(int id, Folder folder, Conversation conv, ParsedMessage pm,
                          int msgSize, String digest, short volumeId, boolean unread,
                          int flags, long tags, DraftInfo dinfo, boolean noICal, ZVCalendar cal)  
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_MESSAGE))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        
        Mailbox mbox = folder.getMailbox();
        
//        TimeZoneMap tzmap = null;
        
        List /* Invite */ components = null;
        String methodStr = null;
        if (cal != null) {
            Account acct = mbox.getAccount();

            // XXX: shouldn't we just be checking flags for Flag.FLAG_FROM_ME?
            //   boolean sentByMe = (flags & Flag.FLAG_FROM_ME) != 0;
            boolean sentByMe = false;
            try {
                String sender = new InternetAddress(pm.getSender()).getAddress();
                sentByMe = AccountUtil.addressMatchesAccount(acct, sender);
            } catch (AddressException e) {
                throw ServiceException.INVALID_REQUEST("unable to parse invite sender: " + pm.getSender(), e);
            }

//            tzmap = new TimeZoneMap(acct.getTimeZone());
//            components = Invite.parseCalendarComponentsForNewMessage(sentByMe, mbox, cal, id, pm.getFragment(), tzmap);
            components = Invite.createFromCalendar(acct, pm.getFragment(), cal, sentByMe, mbox, id);
//            method = (Method) cal.getProperties().getProperty(Property.METHOD);
            methodStr = cal.getPropVal(ICalTok.METHOD, ICalTok.PUBLISH.toString());
        }

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_MESSAGE;
        if (conv != null)
            data.parentId = conv.getId();
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.volumeId    = volumeId;
        data.date        = (int) (pm.getReceivedDate() / 1000);
        data.size        = msgSize;
        data.blobDigest  = digest;
        data.flags       = flags;
        data.tags        = tags;
        data.sender      = pm.getParsedSender().getSortString();
        data.subject     = pm.getNormalizedSubject();
        data.metadata    = encodeMetadata(DEFAULT_COLOR, pm, flags, dinfo, null);
        data.unreadCount = unread ? 1 : 0; 
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);
        Message msg = new Message(mbox, data);

        // process the components in this invite (must do this last so blob is created, etc)
        if (components != null)
            msg.processInvitesAfterCreate(methodStr, folder.getId(), volumeId, !noICal, pm, components);

        msg.finishCreation(conv);
        return msg;
    }

   /**
    * This has to be done as a separate step, after the MailItem has been added, because of foreign key 
    * constraints on the Appointments table
    * @param invites
    */
   private void processInvitesAfterCreate(String method, int folderId, short volumeId, boolean createAppt,
                                          ParsedMessage pm, List /* Invite */ invites) 
   throws ServiceException {
       // since this is the first time we've seen this Invite Message, we need to process it
       // and see if it updates an existing Appointment in the Appointments table, or whatever...
       boolean updatedMetadata = false;
       
       Iterator /*<Invite>*/ iter = invites.iterator();
       while (iter.hasNext()) {
           Invite cur = (Invite) iter.next();

           Mailbox mbox = getMailbox();
           Appointment appt = mbox.getAppointmentByUid(cur.getUid());
           if (createAppt) {
               if (appt == null) { 
                   // ONLY create an appointment if this is a REQUEST method...otherwise don't.
                   if (method.equals(ICalTok.REQUEST.toString())) {
                       appt = mbox.createAppointment(Mailbox.ID_FOLDER_CALENDAR, volumeId, "", cur.getUid(), pm, cur);
                   } else {
                       sLog.info("Mailbox " + getMailboxId()+" Message "+getId()+" SKIPPING Invite "+method+" b/c no Appointment could be found");
                       return; // for now, just ignore this Invitation
                   }
               } else {
            	   // When updating an existing appointment, ignore the
            	   // passed-in folderId which will usually be Inbox.  Leave
            	   // the appointment in the folder it's currently in.
                   appt.processNewInvite(pm, cur, false, appt.getFolderId(), volumeId);
               }
           }
           
           if (appt != null) {
               ApptInfo info = new ApptInfo(appt.getId(), cur.getComponentNum());
               if (mApptInfos == null) {
                   mApptInfos = new ArrayList<ApptInfo>();
               }
               mApptInfos.add(info);
               updatedMetadata = true;
           }
       }
       
       if (updatedMetadata)
           saveMetadata();
   }

    void setImapUid(int imapId) throws ServiceException {
        if (mImapUID == imapId)
            return;
        markItemModified(Change.MODIFIED_IMAP_UID);
        mImapUID = imapId;
        saveMetadata();
    }

    MailItem copy(Folder folder, int id, short destVolumeId)
    throws IOException, ServiceException {
        Message copy = (Message) super.copy(folder, id, destVolumeId);

        Conversation parent = (Conversation) getParent();
        if (parent instanceof VirtualConversation && !isDraft() && inSpam() == folder.inSpam()) {
            Conversation conv = mMailbox.createConversation(new Message[] { this, copy }, Mailbox.ID_AUTO_INCREMENT);
            DbMailItem.changeOpenTarget(this, conv);
            parent.removeChild(this);
        }
        return copy;
    }

    void move(Folder folder) throws ServiceException {
        Folder oldFolder = getFolder();
        super.move(folder);

        oldFolder.updateMessageCount(-1);
        folder.updateMessageCount(1);
    }

    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        assert(indexData instanceof ParsedMessage);
        ParsedMessage pm = (ParsedMessage) indexData;
        if (pm == null)
            throw ServiceException.FAILURE("Message is missing data to index", null);

        // FIXME: need to note this as dirty so we can reindex if things fail
        if (!DebugConfig.disableIndexing)
            Indexer.GetInstance().indexMessage(redo, mMailbox.getMailboxIndex(), mId, pm);
    }

    public void reanalyze() throws ServiceException, IOException {
        ParsedMessage pm = new ParsedMessage(getMimeMessage(), getDate(), getMailbox().attachmentsIndexingEnabled());
        try {
			reanalyze(pm, pm.getRawSize());
		} catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
		}
    }
    public void reanalyze(ParsedMessage pm, int size) throws ServiceException {
        // make sure the "attachments" FLAG is correct
        mData.flags &= ~Flag.FLAG_ATTACHED;
        if (pm.hasAttachments())
            mData.flags |= Flag.FLAG_ATTACHED;

        // make sure the SUBJECT is correct
        boolean subjectChanged = !getSubject().equals(pm.getNormalizedSubject());
        mData.subject = pm.getNormalizedSubject();

        // handle the message's PARENT
        MailItem parent = getParent();
        if (subjectChanged)
            mData.parentId = -mId;

		// update the METADATA and SIZE
        if (mData.size != size) {
            mMailbox.updateSize(size - mData.size, false);
            getFolder().updateSize(size - mData.size);
            mData.size = size;
        }
        String metadata = encodeMetadata(mColor, pm, mData.flags, mDraftInfo, mApptInfos);

        // rewrite the DB row to reflect our new view
        saveData(pm.getParsedSender().getSortString(), metadata);

        // and, finally, uncache this item since it may be dirty
        mMailbox.uncache(this);

        // if the message is part of a real conversation, need to break it out
        if (subjectChanged)
            parent.removeChild(this);
//        else if (parent != null)
//        	mMailbox.recalculateSenderList(parent.getId(), true);
    }

    void setContent(ParsedMessage pm, String digest, int size, int imapId) throws ServiceException {
        //
        // WARNING: this code is currently duplicated in Appointment.java -- until the two
        // functions are unified in MailItem, make sure you keep both versions in sync!
        //
        
        // mark the old blob as ready for deletion
        PendingDelete info = getDeletionInfo();
        info.itemIds.clear();  info.unreadIds.clear();
        mMailbox.markOtherItemDirty(info);
        MessageCache.purge(this);

        markItemModified(Change.MODIFIED_CONTENT  | Change.MODIFIED_DATE |
                         Change.MODIFIED_IMAP_UID | Change.MODIFIED_SIZE);
        mData.blobDigest = digest;
        mData.date       = mMailbox.getOperationTimestamp();
        mData.contentChanged(mMailbox);
        mImapUID = imapId;
        mBlob = null;
        reanalyze(pm, size);
    }

    void detach() throws ServiceException {
        MailItem parent = getParent();
        if (!(parent instanceof Conversation))
            return;
        if (parent.getSize() <= 1)
            mMailbox.closeConversation((Conversation) parent, null);
        else {
            // remove this message from its (real) conversation
            markItemModified(Change.MODIFIED_PARENT);
            parent.removeChild(this);
            // and place it in a new, non-"opened", virtual conversation
            VirtualConversation vconv = new VirtualConversation(mMailbox, this);
            mData.parentId = vconv.getId();
            DbMailItem.setParent(vconv, this);
        }
    }

    void delete(TargetConstraint tcon) throws ServiceException {
		MailItem parent = getParent();
		if (parent instanceof Conversation && parent.mChildren.length == 1)
			parent.delete();
		else
			super.delete();
	}


    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        mSender = meta.get(Metadata.FN_SENDER, null);
        mRecipients = meta.get(Metadata.FN_RECIPIENTS, null);
        mFragment = meta.get(Metadata.FN_FRAGMENT, null);
        mImapUID = (int) meta.getLong(Metadata.FN_IMAP_ID, 0);

        if (meta.containsKey(Metadata.FN_APPT_IDS)) {
            mApptInfos = new ArrayList<ApptInfo>();
            MetadataList mdList = meta.getList(Metadata.FN_APPT_IDS);
            for (int i = 0; i < mdList.size(); i++) {
                Metadata md = mdList.getMap(i);
                mApptInfos.add(ApptInfo.decodeMetadata(md));
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

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mSender, mRecipients, mFragment, mData.subject, mRawSubject, mImapUID, mDraftInfo, mApptInfos);
    }
    private static String encodeMetadata(byte color, ParsedMessage pm, int flags, DraftInfo dinfo, ArrayList /*ApptInfo*/ apptInfos) {
        // cache the "To" header only for messages sent by the user
        String recipients = ((flags & Flag.FLAG_FROM_ME) == 0 ? null : pm.getRecipients());
        return encodeMetadata(new Metadata(), color, pm.getSender(), recipients, pm.getFragment(), pm.getNormalizedSubject(), pm.getSubject(), 0, dinfo, apptInfos).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, String sender, String recipients, String fragment, String subject, String rawSubject, int imapID, DraftInfo dinfo, ArrayList /*ApptInfo*/ apptInfos) {
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
        
        if (apptInfos != null) {
            MetadataList mdList = new MetadataList();
            for (Iterator iter = apptInfos.iterator(); iter.hasNext();) {
                ApptInfo info = (ApptInfo)iter.next();
                mdList.add(info.encodeMetadata());
            }
            meta.put(Metadata.FN_APPT_IDS, mdList); 
        }
                
        if (imapID > 0)
            meta.put(Metadata.FN_IMAP_ID, imapID);
        if (dinfo != null) {
            Metadata dmeta = new Metadata();
            dmeta.put(Metadata.FN_REPLY_ORIG, dinfo.origId);
            dmeta.put(Metadata.FN_REPLY_TYPE, dinfo.replyType);
            meta.put(Metadata.FN_DRAFT, dmeta);
        }

        return MailItem.encodeMetadata(meta, color);
    }


    private static final String CN_SENDER     = "sender";
    private static final String CN_RECIPIENTS = "to";
    private static final String CN_FRAGMENT   = "fragment";
    private static final String CN_IMAP_ID    = "imap_id";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("message: {");
        appendCommonMembers(sb).append(", ");
        if (mImapUID > 0)
            sb.append(CN_IMAP_ID).append(": ").append(mImapUID).append(", ");
        sb.append(CN_SENDER).append(": ").append(mSender).append(", ");
        if (mRecipients != null)
            sb.append(CN_RECIPIENTS).append(": ").append(mRecipients).append(", ");
        sb.append(CN_FRAGMENT).append(": ").append(mFragment);
        sb.append("}");
        return sb.toString();
    }
}
