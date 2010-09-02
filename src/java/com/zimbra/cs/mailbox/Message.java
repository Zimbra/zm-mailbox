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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.ZAttrProvisioning.PrefCalendarApptVisibility;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageOptions;
import com.zimbra.cs.mime.TnefConverter;
import com.zimbra.cs.mime.UUEncodeConverter;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.op.CreateCalendarItemPlayer;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.util.AccountUtil;

public class Message extends MailItem {

    static class DraftInfo {
        String accountId;
        String identityId;
        String replyType;
        String origId;

        // time in UTC millis at which the draft is intended to be auto-sent by the server
        // zero value implies a normal draft, i.e. no auto-send intended
        long autoSendTime;

        public DraftInfo(String rt, String id, String ident, String account, long autoSendTime) {
            replyType = rt;
            origId = id;
            identityId = ident;
            accountId = account;
            this.autoSendTime = autoSendTime;
        }

        public DraftInfo(Metadata meta) throws ServiceException {
            accountId = meta.get(Metadata.FN_ACCOUNT_ID, null);
            identityId = meta.get(Metadata.FN_IDENTITY_ID, null);
            replyType = meta.get(Metadata.FN_REPLY_TYPE, null);
            origId = meta.get(Metadata.FN_REPLY_ORIG, null);
            autoSendTime = meta.getLong(Metadata.FN_AUTO_SEND_TIME, 0);
        }
    }

    public static class CalendarItemInfo {
        // special calItem id value meaning no calendar item was created
        public static final int CALITEM_ID_NONE = 0;

        private int mCalendarItemId;
        private int mComponentNo;
        private Invite mInvite;  // set only when mCalendarItemId == CALITEM_ID_NONE

        CalendarItemInfo(int calItemId, int componentNo, Invite inv) {
            mCalendarItemId = calItemId;
            mComponentNo = componentNo;
            mInvite = inv;
        }

        public int getCalendarItemId()  { return mCalendarItemId; }
        public int getComponentNo()    { return mComponentNo; }
        public Invite getInvite() { return mInvite; }
        public boolean calItemCreated() { return mCalendarItemId != CALITEM_ID_NONE; }

        private static final String FN_CALITEMID = "a";
        private static final String FN_COMPNO = "c";
        private static final String FN_INV = "inv";

        Metadata encodeMetadata() {
            Metadata meta = new Metadata();
            meta.put(FN_CALITEMID, mCalendarItemId);
            meta.put(FN_COMPNO, mComponentNo);
            if (mInvite != null)
                meta.put(FN_INV, Invite.encodeMetadata(mInvite));
            return meta; 
        }

        static CalendarItemInfo decodeMetadata(Metadata meta, Mailbox mbox) throws ServiceException {
            int calItemId = (int) meta.getLong(FN_CALITEMID, CalendarItemInfo.CALITEM_ID_NONE);
            int componentNo = (int) meta.getLong(FN_COMPNO);
            Invite inv = null;
            Metadata metaInv = meta.getMap(FN_INV, true);
            if (metaInv != null) {
                long mboxId = mbox.getId();
                ICalTimeZone accountTZ = ICalTimeZone.getAccountTimeZone(mbox.getAccount());
                inv = Invite.decodeMetadata(mboxId, metaInv, null, accountTZ);
            }
            return new CalendarItemInfo(calItemId, componentNo, inv);
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
    private String mCalendarIntendedFor;


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
        return isTagged(Flag.ID_FLAG_DRAFT);
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


    /** Returns the ID of the account that was used to compose this draft message.
     * 
     * @return The ID of the account used to compose this draft message, or ""
     *         for Messages that are not drafts.
     * @see #getDraftReplyType
     * @see #getDraftOrigId() */
    public String getDraftAccountId() {
        return (mDraftInfo == null || mDraftInfo.accountId == null ? "" : mDraftInfo.accountId);
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
        return mCalendarItemInfos != null && !mCalendarItemInfos.isEmpty();
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

    public String getCalendarIntendedFor() {
        return mCalendarIntendedFor;
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
    @Override boolean isMutable()       { return isTagged(Flag.ID_FLAG_DRAFT); }
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
    
    static Message create(int id, Folder folder, Conversation conv, ParsedMessage pm, StagedBlob staged,
                          boolean unread, int flags, long tags, DraftInfo dinfo,
                          boolean noICal, ZVCalendar cal, CustomMetadataList extended)  
    throws ServiceException {
        return createInternal(id, folder, conv, pm, staged, unread, flags, tags,
                              dinfo, noICal, cal, extended, new MessageCreateFactory());
    }
    
    protected static Message createInternal(int id, Folder folder, Conversation conv, ParsedMessage pm, StagedBlob staged,
                                            boolean unread, int flags, long tags, DraftInfo dinfo,
                                            boolean noICal, ZVCalendar cal, CustomMetadataList extended, MessageCreateFactory fact)
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_MESSAGE))
            throw MailServiceException.CANNOT_CONTAIN(folder, TYPE_MESSAGE);
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        Mailbox mbox = folder.getMailbox();
        Account acct = mbox.getAccount();

        List<Invite> components = null;
        String methodStr = null;
        // Skip calendar processing if message is being filed as spam or trash.
        if (cal != null && folder.getId() != Mailbox.ID_FOLDER_SPAM && folder.getId() != Mailbox.ID_FOLDER_TRASH) {
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
                    if (ICalTok.PUBLISH.toString().equals(methodStr) &&
                            !acct.getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowPublishMethodInvite, false)) {
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

        // make sure the received date is not negative or in the future
        long date = pm.getReceivedDate(), now = System.currentTimeMillis();
        if (date < 0 || date > now)
            date = now;

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = fact.getType();
        if (conv != null)
            data.parentId = conv.getId();
        data.folderId    = folder.getId();
        if (!folder.inSpam() || acct.getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false))
            data.indexId = mbox.generateIndexId(id);
        data.locator     = staged.getStagedLocator();
        data.imapId      = id;
        data.date        = (int) (date / 1000);
        data.size        = staged.getStagedSize();
        data.setBlobDigest(staged.getStagedDigest());
        data.flags       = flags & (Flag.FLAGS_MESSAGE | Flag.FLAGS_GENERIC);
        data.tags        = tags;
        data.subject     = pm.getNormalizedSubject();
        data.metadata    = encodeMetadata(DEFAULT_COLOR_RGB, 1, extended, pm, flags, dinfo, null, null);
        data.unreadCount = unread ? 1 : 0; 
        data.contentChanged(mbox);

        ZimbraLog.mailop.info("Adding Message: id=%d, Message-ID=%s, parentId=%d, folderId=%d, folderName=%s.",
                              data.id, pm.getMessageID(), data.parentId, folder.getId(), folder.getName());
        DbMailItem.create(mbox, data, pm.getParsedSender().getSortString());
        Message msg = fact.create(mbox, data);

        // process the components in this invite (must do this last so blob is created, etc)
        if (components != null) {
            try {
                msg.processInvitesAfterCreate(methodStr, folder.getId(), !noICal, pm, components, cal);
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
    private void processInvitesAfterCreate(String method, int folderId, boolean createCalItem,
                                           ParsedMessage pm, List<Invite> invites, ZVCalendar cal)
    throws ServiceException {
        if (pm == null)
            throw ServiceException.INVALID_REQUEST("null ParsedMessage while processing invite in message " + mId, null);

        Account acct = getAccount();
        OperationContext octxt = getMailbox().getOperationContext();

        // Is this invite intended for me?  If not, we don't want to auto-apply it.
        boolean isForwardedInvite = false;
        String intendedForAddress = null;
        boolean intendedForMe = true;
        try {
            String headerVal = pm.getMimeMessage().getHeader(CalendarMailSender.X_ZIMBRA_CALENDAR_INTENDED_FOR, null);
            if (headerVal != null && headerVal.length() > 0) {
                isForwardedInvite = true;
                intendedForAddress = headerVal;
                intendedForMe = AccountUtil.addressMatchesAccount(acct, headerVal);
                if (!intendedForMe)
                    mCalendarIntendedFor = headerVal;
            }
        } catch (MessagingException e) {
            ZimbraLog.calendar.warn(
                    "ignoring error while checking for " + CalendarMailSender.X_ZIMBRA_CALENDAR_INTENDED_FOR +
                    " header on incoming message", e);
        }

        // Whether to automatically add to calendar when invited to a new appointment.  This only applies
        // to new appointments.  Cancels or updates to existing appointments are always processed immediately.
        boolean autoAddNew;
        if (octxt != null && (octxt.getPlayer() instanceof CreateCalendarItemPlayer)) {
            // We're executing redo.  Use the same auto-add choice as before, regardless of current pref.
            // Create appointment if redo item has valid appointment id.
            CreateCalendarItemPlayer player = (CreateCalendarItemPlayer) octxt.getPlayer();
            autoAddNew = player.getCalendarItemId() != CalendarItemInfo.CALITEM_ID_NONE;
        } else {
            autoAddNew = acct.isPrefCalendarAutoAddInvites();
        }

        boolean isOrganizerMethod = Invite.isOrganizerMethod(method);
        if (!invites.isEmpty() && intendedForMe) {
            // Check if the sender is allowed to invite this user.  Only do this for invite-type methods,
            // namely REQUEST/PUBLISH/CANCEL/ADD/DECLINECOUNTER.  REPLY/REFRESH/COUNTER don't undergo
            // the check because they are not organizer-to-attendee methods.
            if (isOrganizerMethod) {
                String senderEmail;
                Account senderAcct = null;
                boolean onBehalfOf = false;
                boolean canInvite;
                AccessManager accessMgr = AccessManager.getInstance();
                if (octxt != null && octxt.getAuthenticatedUser() != null) {
                    onBehalfOf = octxt.isDelegatedRequest(getMailbox());
                    senderAcct = octxt.getAuthenticatedUser();
                    senderEmail = senderAcct.getName();
                    canInvite = accessMgr.canDo(senderAcct, acct, User.R_invite, octxt.isUsingAdminPrivileges());
                } else {
                    senderEmail = pm.getSenderEmail(false);
                    if (senderEmail != null)
                        senderAcct = Provisioning.getInstance().get(AccountBy.name, senderEmail);
                    canInvite = accessMgr.canDo(senderEmail, acct, User.R_invite, false);
                }
                if (!canInvite) {
                    Invite invite = invites.get(0);
                    boolean sameDomain = false;
                    if (senderAcct != null) {
                        String senderDomain = senderAcct.getDomainName();
                        sameDomain = senderDomain != null && senderDomain.equalsIgnoreCase(acct.getDomainName());
                    }
                    boolean directAttendee =
                        DebugConfig.calendarEnableInviteDeniedReplyForUnlistedAttendee
                        || invite.getMatchingAttendee(acct) != null;
                    // Send auto replies only to users in the same domain and if addressed directly as an attendee,
                    // not indirectly via a mailing list.
                    if (acct.isPrefCalendarSendInviteDeniedAutoReply() && senderEmail != null && sameDomain && directAttendee) {
                        RedoableOp redoPlayer = octxt != null ? octxt.getPlayer() : null;
                        RedoLogProvider redoProvider = RedoLogProvider.getInstance();
                        // Don't generate auto-reply email during redo playback.
                        boolean needAutoReply =
                            redoProvider.isMaster() &&
                            (redoPlayer == null || redoProvider.getRedoLogManager().getInCrashRecovery());
                        if (needAutoReply) {
                            ItemId origMsgId = new ItemId(getMailbox(), getId());
                            CalendarMailSender.sendInviteDeniedMessage(
                                    octxt, acct, senderAcct, onBehalfOf, true, getMailbox(), origMsgId, senderEmail, invite);
                        }
                    }
                    String sender = senderEmail != null ? senderEmail : "unkonwn sender";
                    ZimbraLog.calendar.info("Calendar invite from " + sender + " to " + acct.getName() + " is not allowed");

                    // Turn off auto-add.  We still have to run through the code below to save the invite's
                    // data in message's metadata.
                    autoAddNew = false;
                }
            }
        }

        // since this is the first time we've seen this Invite Message, we need to process it
        // and see if it updates an existing CalendarItem in the database table, or whatever...
        boolean updatedMetadata = false;

        // Override CLASS property if preference says to mark everything as private.
        PrefCalendarApptVisibility prefClass = acct.getPrefCalendarApptVisibility();
        boolean forcePrivateClass = prefClass != null && !prefClass.equals(PrefCalendarApptVisibility.public_);

        // Ignore alarms set by organizer.
        boolean allowOrganizerAlarm = DebugConfig.calendarAllowOrganizerSpecifiedAlarms;

        if (mCalendarItemInfos == null)
            mCalendarItemInfos = new ArrayList<CalendarItemInfo>();

        // Clean up invalid missing organizer/attendee from some Exchanged-originated invite. (bug 43195)
        // We are looking for a multi-VEVENT structure where the series VEVENT has organizer and attendees
        // specified and exception instance VEVENTs omit them.  Exchange seems to expect you to inherit
        // organizer/attendee from series, but that's wrong.  Fix the data by duplicating the missing
        // properties.
        if (invites.size() > 1) {
            ZOrganizer seriesOrganizer = null;
            boolean seriesIsOrganizer = false;
            List<ZAttendee> seriesAttendees = null;
            // Get organizer and attendees from series VEVENT.
            for (Invite inv : invites) {
                if (!inv.hasRecurId()) {
                    seriesOrganizer = inv.getOrganizer();
                    seriesIsOrganizer = inv.isOrganizer();
                    seriesAttendees = inv.getAttendees();
                    break;
                }
            }
            if (seriesOrganizer != null) {
                for (Invite inv : invites) {
                    if (inv.hasRecurId() && !inv.hasOrganizer()) {
                        inv.setOrganizer(seriesOrganizer);
                        inv.setIsOrganizer(seriesIsOrganizer);
                        // Inherit attendees from series, iff no attendee is listed and also no organizer
                        // is given.  If organizer is specified, we will assume they really meant for this
                        // exception instance to have no attendee.
                        if (!inv.hasOtherAttendees() && seriesAttendees != null) {
                            for (ZAttendee at : seriesAttendees) {
                                inv.addAttendee(at);
                            }
                        }
                    }
                }
            }
        }

        boolean publicInvites = true;  // used to check if any invite is non-public
        int calItemFolderId =
            invites.size() > 0 && invites.get(0).isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
        CalendarItem firstCalItem = null;
        Set<String> calUidsSeen = new HashSet<String>();
        for (Invite cur : invites) {
            if (!cur.isPublic())
                publicInvites = false;

            // Bug 38550/41239: If ORGANIZER is missing, set it to email sender.  If that sender
            // is the same user as the recipient, don't set organizer and clear attendees instead.
            // We don't want to set organizer to receiving user unless we're absolutely certain
            // it's the correct organizer.
            if (!cur.hasOrganizer() && cur.hasOtherAttendees()) {
                String fromEmail = pm.getSenderEmail(true);
                if (fromEmail != null) {
                    boolean dangerousSender = false;
                    // Is sender == recipient?  If so, clear attendees.
                    if (intendedForAddress != null) {
                        if (intendedForAddress.equalsIgnoreCase(fromEmail)) {
                            ZimbraLog.calendar.info(
                                    "Got malformed invite without organizer.  Clearing attendees to prevent inadvertent cancels.");
                            cur.clearAttendees();
                            dangerousSender = true;
                        }
                    } else if (AccountUtil.addressMatchesAccount(acct, fromEmail)) {
                        ZimbraLog.calendar.info(
                                "Got malformed invite without organizer.  Clearing attendees to prevent inadvertent cancels.");
                        cur.clearAttendees();
                        dangerousSender = true;
                    }
                    if (!dangerousSender) {
                        ZOrganizer org = new ZOrganizer(fromEmail, null);
                        String senderEmail = pm.getSenderEmail(false);
                        if (senderEmail != null && !senderEmail.equalsIgnoreCase(fromEmail))
                            org.setSentBy(senderEmail);
                        cur.setOrganizer(org);
                        ZimbraLog.calendar.info(
                                "Got malformed invite that lists attendees without specifying an organizer.  " +
                                "Defaulting organizer to: " + org.toString());
                    }
                }
            }

            cur.setLocalOnly(false);
            String uid = cur.getUid();
            boolean addRevision;
            if (!calUidsSeen.contains(uid)) {
                addRevision = true;
                calUidsSeen.add(uid);
            } else {
                addRevision = false;
            }

            // If inviting a calendar resource, don't allow the organizer to specify intended free/busy value
            // other than BUSY.  And don't allow transparent meetings.  This will prevent double booking in the future.
            if (cur.isEvent() && (acct instanceof CalendarResource)) {
                cur.setFreeBusy(IcalXmlStrMap.FBTYPE_BUSY);
                cur.setTransparency(IcalXmlStrMap.TRANSP_OPAQUE);
            }

            if (forcePrivateClass) {
                cur.setClassProp(IcalXmlStrMap.CLASS_PRIVATE);
                cur.setClassPropSetByMe(true);
            }

            ICalTok methodTok = Invite.lookupMethod(method);

            // Discard alarms set by organizer.  Add a new one based on attendee's preferences.
            if (!allowOrganizerAlarm) {
                // only for non-cancel/non-declinecounter VEVENTs
                if (cur.isEvent() && isOrganizerMethod && !cur.isCancel() && !ICalTok.DECLINECOUNTER.equals(methodTok))
                    Invite.setDefaultAlarm(cur, acct);
            }

            boolean calItemIsNew = false;
            boolean modifiedCalItem = false;
            CalendarItem calItem = null;
            boolean success = false;
            try {
                if (intendedForMe) {
                    cur.sanitize(true);
                    calItem = mMailbox.getCalendarItemByUid(cur.getUid());
                    if (createCalItem && !ICalTok.COUNTER.equals(methodTok) && !ICalTok.DECLINECOUNTER.equals(methodTok)) {
                        if (calItem == null) {
                            // ONLY create a calendar item if this is a REQUEST method...otherwise don't.
                            // Allow PUBLISH method as well depending on the preference.
                            if (ICalTok.REQUEST.equals(methodTok) ||
                                (ICalTok.PUBLISH.equals(methodTok) &&
                                 getAccount().getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowPublishMethodInvite, false))) {
                                if (autoAddNew) {
                                    int flags = 0;
//                                  int flags = Flag.BITMASK_INDEXING_DEFERRED;
//                                  mMailbox.incrementIndexDeferredCount(1);
                                    int defaultFolder = cur.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
                                    calItem = mMailbox.createCalendarItem(defaultFolder, flags, 0, cur.getUid(), pm, cur, null);
                                    calItemIsNew = true;
                                    calItemFolderId = calItem.getFolderId();
                                }
                            } else {
                                sLog.info("Mailbox " + getMailboxId()+" Message "+getId()+" SKIPPING Invite "+method+" b/c no CalendarItem could be found");
                                success = true;
                                continue; // for now, just ignore this Invitation
                            }
                        } else {
                            // bug 27887: Ignore when calendar request email somehow made a loop back to the
                            // organizer address.  Necessary conditions are:
                            //
                            // 1) This mailbox is currently organizer.
                            // 2) ORGANIZER in the request is this mailbox.
                            // 3) User/COS preference doesn't explicitly allow it. (bug 29777)
                            //
                            // If ORGANIZER in the request is a different user this is an organizer change request,
                            // which should be allowed to happen.
                            boolean ignore = false;
                            Invite defInv = calItem.getDefaultInviteOrNull();
                            if (defInv != null && defInv.isOrganizer()) {
                                ZOrganizer org = cur.getOrganizer();
                                String orgAddress = org != null ? org.getAddress() : null;
                                if (AccountUtil.addressMatchesAccount(acct, orgAddress))
                                    ignore = !acct.getBooleanAttr(
                                            Provisioning.A_zimbraPrefCalendarAllowCancelEmailToSelf, false);
                            }
                            if (ignore) {
                                ZimbraLog.calendar.info(
                                        "Ignoring calendar request emailed from organizer to self, " +
                                        "possibly in a mail loop involving mailing lists and/or forwards; " +
                                        "calItemId=" + calItem.getId() + ", msgId=" + getId());
                            } else {
                                // When updating an existing calendar item, ignore the
                                // passed-in folderId which will usually be Inbox.  Leave
                                // the calendar item in the folder it's currently in.
                                if (addRevision)
                                    calItem.snapshotRevision();
                                // If updating (but not canceling) an appointment in trash folder,
                                // use default calendar folder and discard all existing invites.
                                boolean discardExistingInvites = false;
                                int calFolderId = calItem.getFolderId();
                                if (!cur.isCancel() && calFolderId == Mailbox.ID_FOLDER_TRASH) {
                                	discardExistingInvites = true;
                                    if (calItem.getType() == MailItem.TYPE_TASK)
                                    	calFolderId = Mailbox.ID_FOLDER_TASKS;
                                    else
                                    	calFolderId = Mailbox.ID_FOLDER_CALENDAR;
                                }
                                modifiedCalItem = calItem.processNewInvite(pm, cur, calFolderId, discardExistingInvites);
                                calItemFolderId = calFolderId;
                            }
                        }
                    }

                    int calItemId = calItem != null ? calItem.getId() : CalendarItemInfo.CALITEM_ID_NONE;
                    CalendarItemInfo info = new CalendarItemInfo(calItemId, cur.getComponentNum(), cur);
                    mCalendarItemInfos.add(info);
                    updatedMetadata = true;
                    if (calItem != null && (calItemIsNew || modifiedCalItem))
                        mMailbox.queueForIndexing(calItem, !calItemIsNew, null);
                } else {
                    // Not intended for me.  Just save the invite detail in metadata.
                    CalendarItemInfo info = new CalendarItemInfo(
                            CalendarItemInfo.CALITEM_ID_NONE, cur.getComponentNum(), cur);
                    mCalendarItemInfos.add(info);
                    updatedMetadata = true;
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
            if (firstCalItem == null)
                firstCalItem = calItem;
        }
        
        if (updatedMetadata)
            saveMetadata();


        // Forward a copy of the message to calendar admin user if preference says so.

        // Don't forward a forwarded invite.  Prevent infinite loop.
        // Don't forward the message being added to Sent folder.
        if (!isForwardedInvite && intendedForMe && folderId != Mailbox.ID_FOLDER_SENT && !invites.isEmpty()) {
            // Don't do the forwarding during redo playback.
            RedoableOp redoPlayer = octxt != null ? octxt.getPlayer() : null;
            RedoLogProvider redoProvider = RedoLogProvider.getInstance();
            boolean needToForward =
                redoProvider.isMaster() &&
                (redoPlayer == null || redoProvider.getRedoLogManager().getInCrashRecovery());
            if (needToForward) {
                String[] forwardTo = null;

                if (isOrganizerMethod) {
                    forwardTo = acct.getPrefCalendarForwardInvitesTo();
                } else {
                    // If this is an attendee-originated message (REPLY, COUNTER, etc.), we only want
                    // to forward it if the appointment was organized on-behalf-of, i.e. the appointment's
                    // ORGANIZER has SENT-BY.  Furthermore, we want to forward it to the SENT-BY user,
                    // not the users listed in the zimbraPrefCalendarForwardInvitesTo preference.
                    if (firstCalItem != null) {
                        Invite invCalItem = firstCalItem.getInvite(invites.get(0).getRecurId());
                        if (invCalItem == null)
                            invCalItem = firstCalItem.getDefaultInviteOrNull();
                        if (invCalItem != null) {
                            ZOrganizer org = invCalItem.getOrganizer();
                            if (org.hasSentBy()) {
                                forwardTo = new String[] { org.getSentBy() };
                            }
                        }
                    }
                }

                if (forwardTo != null && forwardTo.length > 0) {
                    List<String> rcptsUnfiltered = new ArrayList<String>();  // recipients to receive unfiltered message
                    List<String> rcptsFiltered = new ArrayList<String>();    // recipients to receive message filtered to remove private data
                    Folder calFolder = null;
                    try {
                        calFolder = getMailbox().getFolderById(calItemFolderId);
                    } catch (NoSuchItemException e) {
                        ZimbraLog.mailbox.warn("No such calendar folder (" + calItemFolderId + ") during invite auto-forwarding");
                    }
                    for (String fwd : forwardTo) {
                        // Prevent forwarding to self.
                        if (AccountUtil.addressMatchesAccount(acct, fwd))
                            continue;
                        if (publicInvites) {
                            rcptsUnfiltered.add(fwd);
                        } else {
                            boolean allowed = false;
                            if (calFolder != null) {
                                Account rcptAcct = Provisioning.getInstance().get(AccountBy.name, fwd);
                                if (rcptAcct != null)
                                    allowed = calFolder.canAccess(ACL.RIGHT_PRIVATE, rcptAcct, false);
                            }
                            if (allowed) {
                                rcptsUnfiltered.add(fwd);
                            } else if (acct instanceof CalendarResource) {
                                // Forward filtered invite from calendar resource accounts only.  Don't forward filtered
                                // invite from regular user account because the forwardee won't be able to accept/decline
                                // due to permission error.
                                rcptsFiltered.add(fwd);
                            }
                        }
                    }
                    if (!rcptsUnfiltered.isEmpty() || !rcptsFiltered.isEmpty()) {
                        MimeMessage mmOrig = pm.getMimeMessage();
                        if (mmOrig != null) {
                            String origSender = pm.getSenderEmail(false);
                            String forwarder = AccountUtil.getCanonicalAddress(acct);
                            if (!rcptsUnfiltered.isEmpty()) {
                                MimeMessage mm = CalendarMailSender.createForwardedInviteMessage(
                                        mmOrig, origSender, forwarder, rcptsUnfiltered.toArray(new String[0]));
                                if (mm != null) {
                                    ItemId origMsgId = new ItemId(getMailbox(), getId());
                                    CalendarMailSender.sendInviteForwardMessage(octxt, getMailbox(), origMsgId, mm);
                                }
                            }
                            if (!rcptsFiltered.isEmpty()) {
                                MimeMessage mm = CalendarMailSender.createForwardedPrivateInviteMessage(
                                        acct.getLocale(), method, invites, origSender, forwarder, rcptsFiltered.toArray(new String[0]));
                                if (mm != null) {
                                    ItemId origMsgId = new ItemId(getMailbox(), getId());
                                    CalendarMailSender.sendInviteForwardMessage(octxt, getMailbox(), origMsgId, mm);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void updateBlobData(MailboxBlob mblob) throws IOException, ServiceException {
        long size = mblob.getSize();
        if (getSize() == size && StringUtil.equal(getDigest(), mblob.getDigest()) && StringUtil.equal(getLocator(), mblob.getLocator()))
            return;

        mMailbox.updateSize(size - mData.size, true);
        getFolder().updateSize(0, 0, size - mData.size);

        mData.size    = size;
        mData.locator = mblob.getLocator();
        mData.setBlobDigest(mblob.getDigest());
        DbMailItem.saveBlobInfo(this);
    }

    @Override void setCustomData(CustomMetadata custom) throws ServiceException {
        // first update the message itself
        super.setCustomData(custom);
        // then update the message's conversation
        ((Conversation) getParent()).inheritedCustomDataChanged(this, custom);
    }

    /** Updates the in-memory unread counts for the item.  Also updates the
     *  item's folder, its tag, and its parent.  Note that the parent is not
     *  fetched from the database, so notifications may be off in the case of
     *  uncached {@link Conversation}s when a {@link Message} changes state.
     * 
     * @param delta  The change in unread count for this item. */
    @Override protected void updateUnread(int delta, int deletedDelta) throws ServiceException {
        if ((delta == 0 && deletedDelta == 0) || !trackUnread())
            return;
        markItemModified(Change.MODIFIED_UNREAD);
        
        // grab the parent *before* we make any other changes
        MailItem parent = getParent();

        // update our unread count (should we check that we don't have too many unread?)
        mData.unreadCount += delta;
        if (mData.unreadCount < 0)
            throw ServiceException.FAILURE("inconsistent state: unread < 0 for " + getClass().getName() + " " + mId, null);

        // update the folder's unread count
        getFolder().updateUnread(delta, deletedDelta);

        // update the conversation's unread count
        if (parent != null)
            parent.updateUnread(delta, deletedDelta);

        // tell the tags about the new read/unread item
        updateTagUnread(delta, deletedDelta);
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override MailItem copy(Folder folder, int id, int parentId) throws IOException, ServiceException {
        Message copy = (Message) super.copy(folder, id, parentId);

        Conversation parent = (Conversation) getParent();
        if (parent instanceof VirtualConversation && parent.getId() == parentId && !isDraft() && inSpam() == folder.inSpam()) {
            Conversation conv = mMailbox.createConversation(new Message[] { this, copy }, Mailbox.ID_AUTO_INCREMENT);
            DbMailItem.changeOpenTarget(Mailbox.getHash(getNormalizedSubject()), this, conv.getId());
            parent.removeChild(this);
        }
        return copy;
    }

    @Override public List<IndexDocument> generateIndexData(boolean doConsistencyCheck) throws MailItem.TemporaryIndexingException {
        try {
            ParsedMessage pm = null;
            synchronized (getMailbox()) {
                // force the pm's received-date to be the correct one
                ParsedMessageOptions opt = new ParsedMessageOptions().setContent(getMimeMessage(false))
                    .setReceivedDate(getDate())
                    .setAttachmentIndexing(getMailbox().attachmentsIndexingEnabled())
                    .setSize(getSize())
                    .setDigest(getDigest());
                pm = new ParsedMessage(opt);
            }
            
            if (doConsistencyCheck) {
                // because of bug 8263, we sometimes have fragments that are incorrect;
                //   check them here and correct them if necessary
                String fragment = pm.getFragment();
                boolean fragmentChanged = !getFragment().equals(fragment == null ? "" : fragment);

                // because of changes to normalization algorithm (notably bug 28536), normalized subject may be wrong;
                //   check here and correct if necessary
                String subject = pm.getNormalizedSubject();
                boolean subjectChanged = !getNormalizedSubject().equals(subject == null ? "" : subject);

                if (fragmentChanged || subjectChanged)
                    getMailbox().reanalyze(getId(), getType(), pm);
            }
            
            // don't hold the lock while extracting text!
            pm.analyzeFully();
            
            if (pm.hasTemporaryAnalysisFailure())
                throw new MailItem.TemporaryIndexingException();
            
            return pm.getLuceneDocuments();
        } catch (ServiceException e) {
            ZimbraLog.index.warn("Unable to generate index data for Message "+getId()+". Item will not be indexed", e);
            return new ArrayList<IndexDocument>(0);
        }
    }


    public void reanalyze() throws ServiceException {
        ParsedMessageOptions opt = new ParsedMessageOptions()
            .setContent(getMimeMessage(false))
            .setReceivedDate(getDate())
            .setAttachmentIndexing(getMailbox().attachmentsIndexingEnabled())
            .setSize(getSize())
            .setDigest(getDigest());
        ParsedMessage pm = new ParsedMessage(opt);
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

        MailItem parent = getParent();

        // make sure the SUBJECT is correct
        if (!getSubject().equals(pm.getSubject()))
            markItemModified(Change.MODIFIED_SUBJECT);
        mRawSubject = pm.getSubject();
        mData.subject = pm.getNormalizedSubject();

        // the fragment may have changed
        mFragment = pm.getFragment();

        // make sure the "attachments" FLAG is correct
        boolean hadAttachment = (mData.flags & Flag.BITMASK_ATTACHED) != 0;
        mData.flags &= ~Flag.BITMASK_ATTACHED;
        if (pm.hasAttachments()) {
            mData.flags |= Flag.BITMASK_ATTACHED;
        }
        if (hadAttachment != pm.hasAttachments()) {
            markItemModified(Change.MODIFIED_FLAGS);
            parent.tagChanged(mMailbox.getFlagById(Flag.ID_FLAG_ATTACHED), pm.hasAttachments());
        }

        // make sure the "urgency" FLAGs are correct
        int oldUrgency = mData.flags & (Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
        int urgency = pm.getPriorityBitmask();
        mData.flags &= ~(Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
        mData.flags |= urgency;
        if (oldUrgency != urgency) {
            markItemModified(Change.MODIFIED_FLAGS);
            if (urgency == Flag.BITMASK_HIGH_PRIORITY || oldUrgency == Flag.BITMASK_HIGH_PRIORITY)
                parent.tagChanged(mMailbox.getFlagById(Flag.ID_FLAG_HIGH_PRIORITY), urgency == Flag.BITMASK_HIGH_PRIORITY);
            if (urgency == Flag.BITMASK_LOW_PRIORITY || oldUrgency == Flag.BITMASK_LOW_PRIORITY)
                parent.tagChanged(mMailbox.getFlagById(Flag.ID_FLAG_LOW_PRIORITY), urgency == Flag.BITMASK_LOW_PRIORITY);
        }

        // update the SIZE and METADATA
        if (mData.size != size) {
            markItemModified(Change.MODIFIED_SIZE);
            mMailbox.updateSize(size - mData.size, false);
            getFolder().updateSize(0, 0, size - mData.size);
            mData.size = size;
        }

        String metadata = encodeMetadata(mRGBColor, mVersion, mExtendedData, pm, mData.flags, mDraftInfo, mCalendarItemInfos, mCalendarIntendedFor);

        // rewrite the DB row to reflect our new view
        saveData(pm.getParsedSender().getSortString(), metadata);

        if (parent instanceof VirtualConversation)
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
                mCalendarItemInfos.add(CalendarItemInfo.decodeMetadata(md, getMailbox()));
            }
        }
        mCalendarIntendedFor = meta.get(Metadata.FN_CAL_INTENDED_FOR, null);

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
        return encodeMetadata(meta, mRGBColor, mVersion, mExtendedData, mSender, mRecipients, mFragment,
                              mData.subject, mRawSubject, mDraftInfo, mCalendarItemInfos, mCalendarIntendedFor);
    }

    private static String encodeMetadata(Color color, int version, CustomMetadataList extended, ParsedMessage pm,
                                         int flags, DraftInfo dinfo,
                                         List<CalendarItemInfo> calItemInfos, String calIntendedFor) {
        // cache the "To" header only for messages sent by the user
        String recipients = ((flags & Flag.BITMASK_FROM_ME) == 0 ? null : pm.getRecipients());
        return encodeMetadata(new Metadata(), color, version, extended, pm.getSender(), recipients, pm.getFragment(),
                              pm.getNormalizedSubject(), pm.getSubject(), dinfo,
                              calItemInfos, calIntendedFor).toString();
    }


    static Metadata encodeMetadata(Metadata meta, Color color, int version, CustomMetadataList extended, String sender, String recipients,
                                   String fragment, String subject, String rawSubject, DraftInfo dinfo,
                                   List<CalendarItemInfo> calItemInfos, String calIntendedFor) {
        // try to figure out a simple way to make the raw subject from the normalized one
        String prefix = null;
        if (rawSubject == null || rawSubject.equals(subject)) {
            rawSubject = null;
        } else if (rawSubject.endsWith(subject)) {
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
        meta.put(Metadata.FN_CAL_INTENDED_FOR, calIntendedFor);

        if (dinfo != null) {
            Metadata dmeta = new Metadata();
            dmeta.put(Metadata.FN_REPLY_ORIG, dinfo.origId);
            dmeta.put(Metadata.FN_REPLY_TYPE, dinfo.replyType);
            dmeta.put(Metadata.FN_IDENTITY_ID, dinfo.identityId);
            dmeta.put(Metadata.FN_ACCOUNT_ID, dinfo.accountId);
            dmeta.put(Metadata.FN_AUTO_SEND_TIME, dinfo.autoSendTime);
            meta.put(Metadata.FN_DRAFT, dmeta);
        }

        return MailItem.encodeMetadata(meta, color, version, extended);
    }

    /**
     * Overrides the default value of {@code true}, to handle the
     * {@code zimbraMailAllowReceiveButNotSendWhenOverQuota} account
     * attribute.
     */
    @Override
    protected boolean isQuotaCheckRequired() throws ServiceException {
        Account account = getMailbox().getAccount();
        return !account.isMailAllowReceiveButNotSendWhenOverQuota();
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
