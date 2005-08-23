/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
import java.util.Comparator;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.fortuna.ical4j.model.Calendar;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.TnefConverter;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.ZimbraLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.property.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.calendar.*;
import com.zimbra.cs.redolog.op.CreateMessage;


/**
 * @author schemers
 */
public class Message extends MailItem {

    static Log sLog = LogFactory.getLog(Message.class);
    
	private String mSender;
    private String mRecipients;
    private int    mImapUID;
	private String mFragment;
    private String mNormalizedSubject;
    
    /**
     * @author tim
     *
     * The Invite is located by the triplet: (appointmentId, invMsgId, componentNo)
     */
    public static class ApptInfo {
        private int mAppointmentId;
        private int mComponentNo;
        
        public ApptInfo(int apptId, int componentNo) {
            mAppointmentId = apptId;
            mComponentNo = componentNo;
        }
        
        public int getAppointmentId() { return mAppointmentId; }
        public int getComponentNo() { return mComponentNo; }
        
        private static final String FN_APPTID = "a";
        private static final String FN_COMPNO = "c";
        
        Metadata encodeMetadata() {
            Metadata md = new Metadata();
            md.put(FN_APPTID, mAppointmentId);
            md.put(FN_COMPNO, mComponentNo);
            return md; 
        }
        
        static ApptInfo decodemetadata(Metadata md) throws ServiceException {
            int apptId = (int)md.getLong(FN_APPTID);
            int componentNo = (int)md.getLong(FN_COMPNO);
            return new ApptInfo(apptId, componentNo);
        }
    }
    
    ArrayList /* ApptInfo */ mApptInfos;
    
    public Iterator /* ApptInfo */ getApptInfoIterator() {
        return mApptInfos.iterator();
    }
    
    public ApptInfo getApptInfo(int componentId) {
        if (componentId < mApptInfos.size()) {
            return (ApptInfo)(mApptInfos.get(componentId));
        } else {
            return null;
        }
    }
    
    private DraftInfo mDraftInfo;


    /**
     * this one will call back into decodemetadata() to do our initialization
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

    public boolean isDraft() {
        return isTagged(mMailbox.mDraftFlag);
    }

    public String getSender() {
        return (mSender == null ? "" : mSender);
    }

    public String getRecipients() {
        return (mRecipients == null ? "" : mRecipients);
    }
    
    public int getImapUID() {
        return (mImapUID > 0 ? mImapUID : mId);
    }

    public String getFragment() {
        return (mFragment == null ? "" : mFragment);
    }

    public String getNormalizedSubject() {
        return (mNormalizedSubject == null ? "" : mNormalizedSubject);
    }

    public boolean isFromMe() {
        return (mData.flags & Flag.FLAG_FROM_ME) != 0;
    }

    public int getConversationId() {
        return mData.parentId;
    }

    public int getDraftOrigId() {
        return (mDraftInfo == null ? -1 : mDraftInfo.origId);
    }

    public String getDraftReplyType() {
        return (mDraftInfo == null || mDraftInfo.replyType == null ? "" : mDraftInfo.replyType);
    }

    public boolean isInvite() {
        return mApptInfos != null;
    }
    
    public InputStream getRawMessage() throws IOException, ServiceException {
        MailboxBlob msgBlob = getBlob();
        if (msgBlob == null)
            throw ServiceException.FAILURE("missing blob for id: " + mId + ", change: " + mData.modContent, null);
        return StoreManager.getInstance().getContent(msgBlob);
    }

    public MimeMessage getMimeMessage() throws ServiceException {
        InputStream is = null;
        MimeMessage mm = null;
        try {
            is = getRawMessage();
            mm = new MimeMessage(JMSession.getSession(), is);
            is.close();

            try {
                Mime.accept(new TnefConverter(), mm);
            } catch (Exception e) {
                // If the conversion bombs for any reason, revert to the original
                ZimbraLog.mailbox.info("unable to convert TNEF attachment for message " + mId, e);
                is = getRawMessage();
                mm = new MimeMessage(JMSession.getSession(), is);
                is.close();
            }
            
            return mm;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while getting MimeMessage for item " + mId, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while getting MimeMessage for item " + mId, e);
        }
    }

    public static final class SortDateAscending implements Comparator {
        public int compare(Object o1, Object o2) {
            long t1 = ((Message) o1).getDate();
            long t2 = ((Message) o2).getDate();

            if (t1 < t2)        return -1;
            else if (t1 == t2)  return 0;
            else                return 1;
        }
    }

    public static final class SortDateDescending implements Comparator {
    	public int compare(Object o1, Object o2) {
    		long t1 = ((Message) o1).getDate();
    		long t2 = ((Message) o2).getDate();

    		if (t1 < t2)        return 1;
    		else if (t1 == t2)  return 0;
    		else                return -1;
    	}
    }

    public static final class SortImapUID implements Comparator {
        public int compare(Object o1, Object o2) {
            int uid1 = ((Message) o1).getImapUID();
            int uid2 = ((Message) o2).getImapUID();

            return uid1 - uid2;
        }
    }

    boolean isTaggable()      { return true; }
    boolean isCopyable()      { return true; }
    boolean isMovable()       { return true; }
    boolean isMutable()       { return true; }
    boolean isIndexed()       { return true; }
    boolean canHaveChildren() { return false; }

    // boolean canParent(MailItem item) { return (item instanceof Note); }
    boolean canParent(MailItem item)  { return false; }


    static Message create(
            CreateMessage redoRecorder,
            CreateMessage redoPlayer,            
            Folder folder, MailItem parent, ParsedMessage pm, int msgSize, String digest,
                          boolean unread, boolean noICal, int flags, long tags, DraftInfo dinfo, int id, Calendar cal)  
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_MESSAGE))
            throw MailServiceException.CANNOT_CONTAIN();
        
        Mailbox mbox = folder.getMailbox();
        
        TimeZoneMap tzmap = null;
        
        List /* Invite */ components = null;
        String methodStr = null;
        if ((cal != null) && (!noICal)) {
            Account acct = mbox.getAccount();
            
            boolean sentByMe = false;
            {
                String senderFull = pm.getSender();
                String sender = null;
                try {
                    sender = new InternetAddress(senderFull).getAddress();
                } catch (AddressException e) {
                    throw MailServiceException.INVALID_REQUEST(
                            "Unable to parse invite sender: " + senderFull, e);
                }
                sentByMe = AccountUtil.addressMatchesAccount(acct, sender);
            }
            
            ICalTimeZone accountTZ = mbox.getAccount().getTimeZone();
            tzmap = new TimeZoneMap(accountTZ);
            
            components = Invite.parseCalendarComponentsForNewMessage(sentByMe, mbox, cal, id, pm.getFragment(), tzmap);
            Method method = (Method) cal.getProperties().getProperty(Property.METHOD);
            methodStr = method != null ? method.getValue() : "PUBLISH";
        }

        Metadata meta = new Metadata();
        
        UnderlyingData data = createItemData(folder, parent, pm, msgSize, digest, unread,
                                             flags, tags, dinfo, id, TYPE_MESSAGE, 
                                             meta);
        
        DbMailItem.create(mbox, data);
        Message msg = new Message(mbox, data);

        // process the components in this invite (must do this last so blob is created, etc)
        if (components != null) {
            msg.processInvitesAfterCreate(methodStr, redoRecorder, redoPlayer, pm, components);
        }
        
        msg.finishCreation(parent);
        return msg;
    }

   static UnderlyingData createItemData(Folder folder, MailItem parent, ParsedMessage pm,
                                         int msgSize, String msgDigest, boolean unread,
                                         int flags, long tags, DraftInfo dinfo, int id,
                                         byte type,
                                         Metadata meta) 
    throws ServiceException {
        if (folder == null)
            throw MailServiceException.CANNOT_CONTAIN();

        if (meta == null)
            meta = new Metadata();
        meta.copy(assembleMetadata(pm, flags, dinfo, null));

        Mailbox mbox = folder.getMailbox();
        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = type;
        if (parent != null)
            data.parentId = parent.getId();
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.date        = (int) (pm.getReceivedDate() / 1000);
        data.size        = msgSize;
        data.blobDigest  = msgDigest;
        data.flags       = flags;
        data.tags        = tags;
        data.sender      = pm.getParsedSender().getSortString();
        data.subject     = pm.getNormalizedSubject();
        data.metadata    = meta.toString();
        data.modMetadata = mbox.getOperationChangeID();
        data.modContent  = mbox.getOperationChangeID();
        data.unreadCount = unread ? 1 : 0; 
        return data;
    }

   /**
    * This has to be done as a separate step, after the MailItem has been added, because of foreign key 
    * constraints on the Appointments table
    * @param invites
    */
   private void processInvitesAfterCreate(String method, CreateMessage redoRecorder, CreateMessage redoPlayer,
                               ParsedMessage pm, List /* Invite */ invites) 
   throws ServiceException {
       assert(redoRecorder != null);
       // redoPlayer may be null

       // since this is the first time we've seen this Invite Message, we need to process it
       // and see if it updates an existing Appointment in the Appointments table, or whatever...
       
       boolean updatedMetadata = false;
       
       Iterator /*<Invite>*/ iter = invites.iterator();
       while (iter.hasNext()) {
           Invite cur = (Invite) iter.next();

           Mailbox mbox = getMailbox();
           Appointment appt = mbox.getAppointmentByUid(cur.getUid());
           int newApptId = redoPlayer == null ? Mailbox.ID_AUTO_INCREMENT : redoPlayer.getAppointmentId();
           if (appt == null) { 
               // ONLY create an appointment if this is a REQUEST method...otherwise don't.
               if (method.equals(Method.REQUEST.getValue())) {
                   appt = mbox.createAppointment(Mailbox.ID_FOLDER_CALENDAR, "", cur.getUid(), pm, cur, newApptId);
                   redoRecorder.setAppointmentId(appt.getId());
               } else {
                   sLog.info("Mailbox " + getMailboxId()+" Message "+getId()+" SKIPPING Invite "+method+" b/c no Appointment could be found");
                   return; // for now, just ignore this Invitation
               }
           } else {
               // tim: this shouldn't be necessary with the schema update
//               try {
                   appt.processNewInvite(pm, cur);
//               } catch(MailServiceException.NoSuchItemException e) {
//                   // FIXME temp fix for bug 2019: for now, if any of the invites are missing for an 
//                   // appointment we'll just delete the entire appointment and re-create it. 
//                   appt.delete();
//                   
//                   // try it one more time
//                   appt = mbox.createAppointment(Mailbox.ID_FOLDER_CALENDAR, "", cur.getUid(), pm, cur, newApptId);
//                   redoRecorder.setAppointmentId(appt.getId());
//               }
           }
           
           ApptInfo info = new ApptInfo(appt.getId(), cur.getComponentNum());
           if (mApptInfos == null) {
               mApptInfos = new ArrayList();
           }
           mApptInfos.add(info);
           updatedMetadata = true;
       }
       
       if (updatedMetadata) {
           saveMetadata();
       }
   }

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
   
    void setImapUid(int imapId) throws ServiceException {
        if (mImapUID == imapId)
            return;
        markItemModified(Change.MODIFIED_IMAP_UID);
        mImapUID = imapId;
        saveMetadata();
    }

    MailItem copy(Folder folder, int id) throws IOException, ServiceException {
        Message copy = (Message) super.copy(folder, id);

        Conversation parent = (Conversation) getParent();
        if (parent instanceof VirtualConversation && !isDraft() && inSpam() == folder.inSpam()) {
            Conversation conv = mMailbox.createConversation(new Message[] { this, copy }, Mailbox.ID_AUTO_INCREMENT);
            DbMailItem.changeOpenTarget(this, conv);
            parent.removeChild(this);
        }
        return copy;
    }

    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        assert(indexData instanceof ParsedMessage);
        ParsedMessage pm = (ParsedMessage) indexData;
        if (pm == null)
            throw ServiceException.FAILURE("Message is missing data to index", null);

        // FIXME: need to note this as dirty so we can reindex if things fail
        if (!DebugConfig.disableIndexing)
            Indexer.GetInstance().indexMessage(redo, getMailboxId(), mId, pm);
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

        // figure out the *correct* item type
        Calendar cal = pm.getiCalendar();
//        List invites = null;
//        boolean isInvite = (cal != null);
        if (isInvite()) {
            assert(false); // FIXME!
//            InviteMessage.TimeZoneMap tzmap = new InviteMessage.TimeZoneMap();
//            invites = InviteMessage.parseComponentsForNewInvite(mMailbox, cal, mId, tzmap);
//            ((InviteMessage)this).processIncomingInvites(invites);
        }
//            mData.type = (isInvite ? TYPE_INVITE : TYPE_MESSAGE);
        mData.type = (TYPE_MESSAGE);

		// update the METADATA and SIZE
        if (mData.size != size) {
            mMailbox.updateSize(size - mData.size, false);
            getFolder().updateSize(size - mData.size);
            mData.size = size;
        }
        String metadata = encodeMetadata(pm, mData.flags, mDraftInfo, mApptInfos);
//        if (isInvite) {
//            assert(false); // FIXME!
////            metadata = InviteMessage.encodeMetadata(metadata, invites);
//        }

        // rewrite the DB row to reflect our new view
        saveData(pm.getParsedSender().getSortString(), metadata);

        // and, finally, uncache this item since it may be dirty
        mMailbox.uncache(this);

        // if the message is part of a real conversation, need to break it out
        if (subjectChanged)
            parent.removeChild(this);
        else if (parent != null)
        	mMailbox.recalculateSenderList(parent.getId(), true);
    }

    void setContent(ParsedMessage pm, String digest, int size, int imapId) throws ServiceException {
        // mark the old blob as ready for deletion
        PendingDelete info = getDeletionInfo();
        info.itemIds.clear();  info.unreadIds.clear();
        mMailbox.markOtherItemDirty(info);

        markItemModified(Change.MODIFIED_CONTENT  | Change.MODIFIED_DATE |
                         Change.MODIFIED_IMAP_UID | Change.MODIFIED_SIZE);
        mData.blobDigest = digest;
        mData.date       = mMailbox.getOperationTimestamp();
        mData.modContent = mMailbox.getOperationChangeID();
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

    Metadata decodeMetadata(String metadata) throws ServiceException {
        Metadata meta = new Metadata(metadata, this);

        mSender = meta.get(Metadata.FN_SENDER, null);
        mRecipients = meta.get(Metadata.FN_RECIPIENTS, null);
        mFragment = meta.get(Metadata.FN_FRAGMENT, null);
        mImapUID = (int) meta.getLong(Metadata.FN_IMAP_ID, 0);

        if (meta.containsKey(Metadata.FN_APPT_IDS)) {
            mApptInfos = new ArrayList();
            MetadataList mdList = meta.getList(Metadata.FN_APPT_IDS);
            for (int i = 0; i < mdList.size(); i++) {
                Metadata md = mdList.getMap(i);
                mApptInfos.add(ApptInfo.decodemetadata(md));
            }
        }

        Metadata draftMeta = meta.getMap(Metadata.FN_DRAFT, true);
        if (draftMeta != null)
            mDraftInfo = new DraftInfo(draftMeta);

        mNormalizedSubject = mData.subject;
        String prefix = meta.get(Metadata.FN_PREFIX, null);
        if (prefix != null)
            mData.subject = (mData.subject == null ? prefix : prefix + mData.subject);
        String rawSubject = meta.get(Metadata.FN_RAW_SUBJ, null);
        if (rawSubject != null)
            mData.subject = rawSubject;
        
        return meta;
	}

    String encodeMetadata() {
        return assembleMetadata().toString();
    }
    static String encodeMetadata(ParsedMessage pm, int flags, DraftInfo dinfo, ArrayList /*ApptInfo*/ apptInfos) {
        return assembleMetadata(pm, flags, dinfo, apptInfos).toString();
    }
    static String encodeMetadata(String sender, String recipients, String fragment, String subject, String rawSubject, int imapID, DraftInfo dinfo, ArrayList /*ApptInfo*/ apptInfos) {
        return assembleMetadata(sender, recipients, fragment, subject, rawSubject, imapID, dinfo, apptInfos).toString();
    }

    Metadata assembleMetadata() {
        return assembleMetadata(mSender, mRecipients, mFragment, mNormalizedSubject, mData.subject, mImapUID, mDraftInfo, mApptInfos);
    }
    static Metadata assembleMetadata(ParsedMessage pm, int flags, DraftInfo dinfo, ArrayList /*ApptInfo*/ apptInfos) {
        // cache the "To" header only for messages sent by the user
        String recipients = ((flags & Flag.FLAG_FROM_ME) == 0 ? null : pm.getRecipients());
        return assembleMetadata(pm.getSender(), recipients, pm.getFragment(), pm.getNormalizedSubject(), pm.getSubject(), 0, dinfo, apptInfos);
    }
    static Metadata assembleMetadata(String sender, String recipients, String fragment, String subject, 
            String rawSubject, int imapID, DraftInfo dinfo, ArrayList /*ApptInfo*/ apptInfos) {
        String prefix = null;
        if (rawSubject == null || rawSubject.equals(subject))
            rawSubject = null;
        else if (rawSubject.endsWith(subject)) {
            prefix = rawSubject.substring(0, rawSubject.length() - subject.length());
            rawSubject = null;
        }

        Metadata meta = new Metadata();
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

        return meta;
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
