/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.PrefCalendarApptVisibility;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.logger.EventLogger;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteChanges;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageOptions;
import com.zimbra.cs.mime.TnefConverter;
import com.zimbra.cs.mime.UUEncodeConverter;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.op.CreateCalendarItemPlayer;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;

/**
 * @since Jun 13, 2004
 */
public class Message extends MailItem implements Classifiable {

    static class DraftInfo {
        String accountId;
        String identityId;
        String replyType;
        String origId;

        // time in UTC millis at which the draft is intended to be auto-sent by the server
        // zero value implies a normal draft, i.e. no auto-send intended
        long autoSendTime;

        private DraftInfo() {
        }

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

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof DraftInfo) {
                DraftInfo dInfo = (DraftInfo) obj;
                return StringUtil.equal(accountId, dInfo.accountId) &&
                        StringUtil.equal(identityId, dInfo.identityId) &&
                        StringUtil.equal(replyType, dInfo.replyType) &&
                        StringUtil.equal(origId, dInfo.origId) &&
                        autoSendTime == dInfo.autoSendTime;
            }
            return false;
        }
    }

    public static class CalendarItemInfo {
        // special calItem id value meaning no calendar item was created
        public static final int CALITEM_ID_NONE = 0;

        private final int mCalendarItemId;
        private final int mComponentNo;
        private final Invite mInvite;  // set only when mCalendarItemId == CALITEM_ID_NONE
        private final InviteChanges mInviteChanges;
        private final String calOwner;  // set only when matching calendar item is in someone else's calendar

        CalendarItemInfo(int calItemId, String owner, int componentNo, Invite inv, InviteChanges changes) {
            mCalendarItemId = calItemId;
            mComponentNo = componentNo;
            mInvite = inv;
            mInviteChanges = changes;
            calOwner = owner;
        }

        CalendarItemInfo(int calItemId, int componentNo, Invite inv, InviteChanges changes) {
            this(calItemId, null, componentNo, inv, changes);
        }

        public String getCalOwner() { return calOwner; }

        /**
         * @return Item Id of the corresponding calendar item or null if there isn't one.
         */
        public ItemId getCalendarItemId() {
            if (!calItemCreated()) {
                return null;
            }
            return new ItemId(calOwner, mCalendarItemId);
        }

        public int getComponentNo()    { return mComponentNo; }
        public Invite getInvite() { return mInvite; }
        public InviteChanges getInviteChanges() { return mInviteChanges; }
        public boolean calItemCreated() { return mCalendarItemId != CALITEM_ID_NONE; }

        private static final String FN_CALITEMID = "a";
        private static final String FN_COMPNO = "c";
        private static final String FN_INV = "inv";
        private static final String FN_INV_CHANGES = "invChg";
        private static final String FN_CAL_OWNER = "calOwner";

        Metadata encodeMetadata() {
            Metadata meta = new Metadata();
            meta.put(FN_CALITEMID, mCalendarItemId);
            meta.put(FN_COMPNO, mComponentNo);
            if (mInviteChanges != null && !mInviteChanges.noChange())
                meta.put(FN_INV_CHANGES, mInviteChanges.toString());
            if (mInvite != null)
                meta.put(FN_INV, Invite.encodeMetadata(mInvite));
            if (calOwner != null)
                meta.put(FN_CAL_OWNER, calOwner);
            return meta;
        }

        static CalendarItemInfo decodeMetadata(Metadata meta, Mailbox mbox) throws ServiceException {
            int calItemId = (int) meta.getLong(FN_CALITEMID, CalendarItemInfo.CALITEM_ID_NONE);
            int componentNo = (int) meta.getLong(FN_COMPNO);
            String changes = meta.get(FN_INV_CHANGES, null);
            String owner = meta.get(FN_CAL_OWNER, null);
            InviteChanges invChanges = changes != null ? new InviteChanges(changes) : null;
            Invite inv = null;
            Metadata metaInv = meta.getMap(FN_INV, true);
            if (metaInv != null) {
                int mboxId = mbox.getId();
                ICalTimeZone accountTZ = Util.getAccountTimeZone(mbox.getAccount());
                inv = Invite.decodeMetadata(mboxId, metaInv, null, accountTZ);
            }
            return new CalendarItemInfo(calItemId, owner, componentNo, inv, invChanges);
        }
    }

    private static final Log LOG = LogFactory.getLog(Message.class);

    private String sender;
    private String recipients;
    private String fragment;
    private String rawSubject;
    private String dsId;
    private boolean sentByMe;

    private DraftInfo draftInfo;
    private ArrayList<CalendarItemInfo> calendarItemInfos;
    private String calendarIntendedFor;

    Message(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        this(mbox, ud, false);
    }

    /**
     * this one will call back into decodeMetadata() to do our initialization.
     */
    Message(Mailbox mbox, UnderlyingData ud, boolean skipCache) throws ServiceException {
        super(mbox, ud, skipCache);
        init();
    }

    Message(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        super(acc, data, mailboxId);
        init();
    }

    private void init() throws ServiceException {
        if (type != Type.MESSAGE.toByte()  && type != Type.CHAT.toByte()) {
            throw new IllegalArgumentException();
        }
        if (state.getParentId() < 0) {
            state.setParentId(-mId);
        }
    }

    /** Returns whether the Message was created as a draft.  Note that this
     *  can only be set when the Message is created; it cannot be altered
     *  thereafter. */
    public boolean isDraft() {
        return isTagged(Flag.FlagInfo.DRAFT);
    }

    /**
     * Returns the {@code To:} header of the message, if the message was sent by the user, otherwise an empty string.
     */
    public String getRecipients() {
        return Strings.nullToEmpty(recipients);
    }

    /** Returns the first 100 characters of the message's content.  The system
     *  does its best to remove quoted text, etc. before calculating the
     *  fragment.
     *
     * @see com.zimbra.cs.index.Fragment */
    public String getFragment() {
        return Strings.nullToEmpty(fragment);
    }

    /**
     * Returns the normalized subject of the message.  This is done by taking the {@code Subject:} header and removing
     * prefixes (e.g. {@code "Re:"}) and suffixes (e.g. {@code "(fwd)"}) and the like.
     *
     * @see ParsedMessage#normalizeSubject
     */
    String getNormalizedSubject() {
        return super.getSubject();
    }

    /** Returns the raw subject of the message.  This is taken directly from
     *  the <tt>Subject:</tt> header with no processing. */
    @Override
    public String getSubject() {
        return Strings.nullToEmpty(rawSubject);
    }

    /** Returns the {@code From:} header of the message if available; if not, returns the {@code Sender:} header. */
    @Override
    public String getSender() {
        return Strings.nullToEmpty(sender);
    }

    @Override
    public String getSortSubject() {
        return getNormalizedSubject();
    }

    @Override
    public String getSortSender() {
        String sender = new ParsedAddress(getSender()).getSortString();
        // remove surrogate characters and trim to DbMailItem.MAX_SENDER_LENGTH
        return DbMailItem.normalize(sender, DbMailItem.MAX_SENDER_LENGTH);
    }

    @Override
    public String getSortRecipients() {
        List<InternetAddress> iaddrs = com.zimbra.common.mime.InternetAddress.parseHeader(getRecipients());
        if (iaddrs == null || iaddrs.isEmpty()) {
            return null;
        }
        List<ParsedAddress> paddrs = new ArrayList<ParsedAddress>(iaddrs.size());
        for (InternetAddress iaddr : iaddrs) {
            paddrs.add(new ParsedAddress(iaddr));
        }
        return DbMailItem.normalize(ParsedAddress.getSortString(paddrs), DbMailItem.MAX_RECIPIENTS_LENGTH);
    }

    /** Returns whether the Message was sent by the owner of this mailbox.
     *  Note that this can only be set when the Message is created; it cannot
     *  be altered thereafter.*/
    public boolean isFromMe() {
        return state.isSet(Flag.FlagInfo.FROM_ME);
    }

    /** Returns the ID of the {@link Conversation} the Message belongs to.
     *  If the ID is negative, it refers to a single-message
     *  {@link VirtualConversation}.*/
    public int getConversationId() {
        return getParentId();
    }

    /** Returns the ID of the Message that this draft message is in reply to.
     *
     * @return The ID of the Message that this draft message is in reply to,
     *         or -1 for Messages that are not drafts or not replies/forwards.
     * @see #getDraftReplyType */
    public String getDraftOrigId() {
        return (draftInfo == null || draftInfo.origId == null ? "" : draftInfo.origId);
    }

    void setDraftOrigId(String origId) {
        if (draftInfo == null) {
            draftInfo = new DraftInfo();
        }
        draftInfo.origId = origId;
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
        return (draftInfo == null || draftInfo.replyType == null ? "" : draftInfo.replyType);
    }

    void setDraftReplyType(String replyType) {
        if (draftInfo == null) {
            draftInfo = new DraftInfo();
        }
        draftInfo.replyType = replyType;
    }


    /** Returns the ID of the account that was used to compose this draft message.
     *
     * @return The ID of the account used to compose this draft message, or ""
     *         for Messages that are not drafts.
     * @see #getDraftReplyType
     * @see #getDraftOrigId() */
    public String getDraftAccountId() {
        return (draftInfo == null || draftInfo.accountId == null ? "" : draftInfo.accountId);
    }

    void setDraftAccountId(String accountId) {
        if (draftInfo == null) {
            draftInfo = new DraftInfo();
        }
        draftInfo.accountId = accountId;
    }

    /** Returns the ID of the {@link com.zimbra.cs.account.Identity} that was
     *  used to compose this draft message.
     *
     * @return The ID of the Identity used to compose this draft message, or ""
     *         for Messages that are not drafts or did not specify an identity.
     * @see #getDraftReplyType
     * @see #getDraftOrigId() */
    public String getDraftIdentityId() {
        return (draftInfo == null || draftInfo.identityId == null ? "" : draftInfo.identityId);
    }

    void setDraftIdentityId(String identityId) {
        if (draftInfo == null) {
            draftInfo = new DraftInfo();
        }
        draftInfo.identityId = identityId;
    }

    /** Returns the time (millis since epoch) at which the draft message is
     *  intended to be auto-sent by the server. Return value of zero implies
     *  that the draft message is not intended to be auto-sent by the server.
     *
     * @return Draft auto send time */
    public long getDraftAutoSendTime() {
        return draftInfo == null ? 0 : draftInfo.autoSendTime;
    }

    public void setDraftAutoSendTime(long autoSendTime) throws ServiceException {
        if (draftInfo == null && autoSendTime != 0) {
            draftInfo = new DraftInfo(null, null, null, null, autoSendTime);
            saveMetadata();
        } else if (draftInfo != null && draftInfo.autoSendTime != autoSendTime) {
            draftInfo.autoSendTime = autoSendTime;
            saveMetadata();
        }
    }

    /** Returns whether the Message has a vCal attachment. */
    public boolean isInvite() {
        return state.isSet(Flag.FlagInfo.INVITE);
    }

    public boolean hasCalendarItemInfos() {
        return calendarItemInfos != null && !calendarItemInfos.isEmpty();
    }

    public Iterator<CalendarItemInfo> getCalendarItemInfoIterator() {
        if (calendarItemInfos != null) {
            return calendarItemInfos.iterator();
        } else {
            return Collections.<CalendarItemInfo>emptyList().iterator();
        }
    }

    public CalendarItemInfo getCalendarItemInfo(int componentId) {
        if (calendarItemInfos != null && (componentId < 0 || componentId < calendarItemInfos.size())) {
            return calendarItemInfos.get(componentId);
        } else {
            return null;
        }
    }

    public String getCalendarIntendedFor() {
        return calendarIntendedFor;
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
        return getMimeMessage(runConverters, getAccount());
    }

    public MimeMessage getMimeMessage(boolean runConverters, Account acc) throws ServiceException {
        MimeMessage mm = MessageCache.getMimeMessage(this, runConverters);
        if (mm instanceof ZMimeMessage && ZMimeMessage.usingZimbraParser()) {
            try {
                mm = new Mime.FixedMimeMessage(mm,  acc);
            } catch (MessagingException e) {
                ZimbraLog.mailbox.info("could not copy MimeMessage; using original", e);
            }
        }
        return mm;
    }

    @Override
    boolean isTaggable() {
        return true;
    }

    @Override
    boolean isCopyable() {
        return true;
    }

    @Override
    boolean isMovable() {
        return true;
    }

    @Override
    boolean isMutable() {
        return isTagged(Flag.FlagInfo.DRAFT);
    }

    @Override
    boolean canHaveChildren() {
        return false;
    }

    @Override
    boolean canParent(MailItem item) {
        return false;
    }

    static class MessageCreateFactory {
        Message create(Mailbox mbox, UnderlyingData data) throws ServiceException {
            return new Message(mbox, data);
        }

        Type getType() {
            return Type.MESSAGE;
        }
    }

    static Message create(int id, Folder folder, Conversation conv, ParsedMessage pm, StagedBlob staged,
                          boolean unread, int flags, Tag.NormalizedTags ntags, DraftInfo dinfo,
                          boolean noICal, ZVCalendar cal, CustomMetadataList extended, String dsId)
    throws ServiceException {
        return createInternal(id, folder, conv, pm, staged, unread, flags, ntags,
                              dinfo, noICal, cal, extended, dsId, new MessageCreateFactory());
    }

    static Message createInternal(int id, Folder folder, Conversation conv, ParsedMessage pm, StagedBlob staged,
            boolean unread, int flags, Tag.NormalizedTags ntags, DraftInfo dinfo, boolean noICal, ZVCalendar cal,
            CustomMetadataList extended, String dsId, MessageCreateFactory fact)
    throws ServiceException {
        if (folder == null || !folder.canContain(Type.MESSAGE)) {
            throw MailServiceException.CANNOT_CONTAIN(folder, Type.MESSAGE);
        }
        if (!folder.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        Mailbox mbox = folder.getMailbox();
        Account acct = mbox.getAccount();

        String sender = pm.getSenderEmail();

        List<Invite> components = null;
        String methodStr = null;
        boolean sentByMe = false;
        if (!Strings.isNullOrEmpty(sender)) {
            sentByMe = AccountUtil.addressMatchesAccountOrSendAs(acct, sender);
        }
        // Skip calendar processing if message is being filed as spam or trash.
        if (cal != null && folder.getId() != Mailbox.ID_FOLDER_SPAM && folder.getId() != Mailbox.ID_FOLDER_TRASH) {
            // XXX: shouldn't we just be checking flags for Flag.FLAG_FROM_ME?
            //   boolean sentByMe = (flags & Flag.FLAG_FROM_ME) != 0;
            try {
                components = Invite.createFromCalendar(acct, pm.getFragment(acct.getLocale()), cal, sentByMe, mbox, id);
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
        if (date < 0 || date > now) {
            date = now;
        }
        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = fact.getType().toByte();
        if (conv != null) {
            data.parentId = conv.getId();
        }
        data.folderId = folder.getId();
        data.indexId = !folder.inSpam() || acct.isJunkMessagesIndexingEnabled() ? IndexStatus.DEFERRED.id() : IndexStatus.DONE.id();
        data.locator = staged.getLocator();
        data.imapId = id;
        data.date = (int) (date / 1000);
        data.size = staged.getSize();
        data.setBlobDigest(staged.getDigest());
        data.setFlags(flags & (Flag.FLAGS_MESSAGE | Flag.FLAGS_GENERIC));
        data.setTags(ntags);
        data.setSubject(pm.getNormalizedSubject());
        data.metadata = encodeMetadata(DEFAULT_COLOR_RGB, 1, 1, extended, pm, pm.getFragment(acct.getLocale()),
                dinfo, null, null, dsId, sentByMe).toString();
        data.unreadCount = unread ? 1 : 0;
        data.contentChanged(mbox);

        ZimbraLog.mailop.info(
                "Adding Message: id=%d, Message-ID=%s, parentId=%d, folderId=%d, folderName=%s acct=%s.",
                data.id, pm.getMessageID(), data.parentId, folder.getId(), folder.getName(),
                mbox.getAccountId());
        new DbMailItem(mbox)
            .setSender(pm.getParsedSender().getSortString())
            .setRecipients(ParsedAddress.getSortString(pm.getParsedRecipients()))
            .create(data);
        Message msg = fact.create(mbox, data);

        // process the components in this invite (must do this last so blob is created, etc)
        if (components != null) {
            try {
                msg.processInvitesAfterCreate(methodStr, folder.getId(), !noICal, pm, components);
            } catch (Exception e) {
                ZimbraLog.calendar.warn("Unable to process iCalendar attachment", e);
            }
        }

        msg.finishCreation(conv);
        return msg;
    }

    /**
     * @return true if have right "ACTION" for a calender owned by {@code ownerEmail}
     * @throws ServiceException
     */
    private boolean manageCalendar(String ownerEmail) throws ServiceException {
        if (Strings.isNullOrEmpty(ownerEmail)) {
            return false;
        }
        Account ownerAcct = Provisioning.getInstance().get(AccountBy.name, ownerEmail);
        if (ownerAcct == null) {
            ZimbraLog.account.info("Account = %s not present.", ownerEmail);
            return false;
        }
        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(ownerAcct);
        for (Mountpoint sharedCal : getMailbox().getCalendarMountpoints(getMailbox().getOperationContext(), SortBy.NONE)) {
            if (sharedCal.canAccess(ACL.RIGHT_ACTION)) {
                String calOwner = Provisioning.getInstance().get(AccountBy.id, sharedCal.getOwnerId()).getName();
                if (acctMatcher.matches(calOwner)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the value of the "X-Zimbra-Calendar-Intended-For" header if present and non-empty.
     */
    private static String getCalendarIntendedFor(MimeMessage msg) {
        String headerVal;
        try {
            headerVal = msg.getHeader(CalendarMailSender.X_ZIMBRA_CALENDAR_INTENDED_FOR, null);
        } catch (MessagingException e) {
            ZimbraLog.calendar.warn("ignoring error while checking for %s header on incoming message",
                    CalendarMailSender.X_ZIMBRA_CALENDAR_INTENDED_FOR, e);
            return null;
        }
        if (headerVal == null || headerVal.length() == 0) {
            return null;
        }
        return headerVal;
    }

    /**
     * If this invite doesn't appear in the mailbox for this user, this will retrieve details for it if it is in
     * a shared calendar that this user manages.
     */
    public com.zimbra.soap.mail.type.CalendarItemInfo getRemoteCalendarItem(Invite invite) {
        com.zimbra.soap.mail.type.CalendarItemInfo remoteCalendarItem = null;
        String headerVal;
        try {
            headerVal = getCalendarIntendedFor(getMimeMessage());
            if (headerVal != null && headerVal.length() > 0) {
                AccountAddressMatcher acctMatcher = new AccountAddressMatcher(mMailbox.getAccount());
                if (!acctMatcher.matches(headerVal) && manageCalendar(headerVal)) {
                    Provisioning prov = Provisioning.getInstance();
                    Account ownerAcct = prov.get(AccountBy.name, headerVal);
                    remoteCalendarItem = mMailbox.getRemoteCalItemByUID(ownerAcct, invite.getUid(), true, false);
                }
            }
        } catch (ServiceException e) {
            return null;
        }
        return remoteCalendarItem;
    }

    private class ProcessInvitesStatus {
        // since this is the first time we've seen this Invite Message, we need to process it
        // and see if it updates an existing CalendarItem in the database table, or whatever...
        boolean updatedMetadata = false;
        int calItemFolderId = Mailbox.ID_FOLDER_CALENDAR;

        // Is this invite intended for me?  If not, we don't want to auto-apply it.
        boolean isForwardedInvite = false;
        String intendedForAddress = null;
        boolean intendedForMe = true;
        boolean intendedForCalendarIManage = false;
        boolean addRevision = false;
        // Whether to automatically add to calendar when invited to a new appointment.  This only applies
        // to new appointments.  Cancels or updates to existing appointments are always processed immediately.
        boolean autoAddNew;
        AccountAddressMatcher acctMatcher = null;
        private final Account account;
        CalendarItem calItem = null;

        ProcessInvitesStatus(Account acct, ParsedMessage pm) throws ServiceException {
            account = acct;
            String headerVal = getCalendarIntendedFor(pm.getMimeMessage());
            if (headerVal != null && headerVal.length() > 0) {
                isForwardedInvite = true;
                intendedForAddress = headerVal;
                intendedForMe = getAcctMatcher().matches(headerVal);
                if (!intendedForMe) {
                    calendarIntendedFor = headerVal;
                    intendedForCalendarIManage = manageCalendar(calendarIntendedFor);
                }
            }
        }

        void initAddRevisionSetting(String uid, Set<String> calUidsSeen) {
            if (!calUidsSeen.contains(uid)) {
                addRevision = true;
                calUidsSeen.add(uid);
            } else {
                addRevision = false;
            }
        }

        void initAutoAddNew(OperationContext octxt) {
        if (octxt != null && (octxt.getPlayer() instanceof CreateCalendarItemPlayer)) {
            // We're executing redo.  Use the same auto-add choice as before, regardless of current pref.
            // Create appointment if redo item has valid appointment id.
            CreateCalendarItemPlayer player = (CreateCalendarItemPlayer) octxt.getPlayer();
            autoAddNew = player.getCalendarItemId() != CalendarItemInfo.CALITEM_ID_NONE;
        } else {
            autoAddNew = account.isPrefCalendarAutoAddInvites();
        }

        }

        AccountAddressMatcher getAcctMatcher() throws ServiceException {
            if (acctMatcher == null) {
                acctMatcher = new AccountAddressMatcher(account);
            }
            return acctMatcher;
        }
    }

    /**
     * This has to be done as a separate step, after the MailItem has been added, because of foreign key constraints on
     * the CalendarItems table.
     */
    private void processInvitesAfterCreate(String method, int folderId, boolean applyToCalendar, ParsedMessage pm,
            List<Invite> invites)
    throws ServiceException {
        if (pm == null) {
            throw ServiceException.INVALID_REQUEST("null ParsedMessage while processing invite in message " + mId, null);
        }

        Account acct = getAccount();
        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(acct);
        OperationContext octxt = getMailbox().getOperationContext();

        ProcessInvitesStatus status = new ProcessInvitesStatus(acct, pm);
        status.initAutoAddNew(octxt);

        boolean isOrganizerMethod = Invite.isOrganizerMethod(method);
        if (isOrganizerMethod && !invites.isEmpty() && status.intendedForMe) {
            // Check if the sender is allowed to invite this user.  Only do this for invite-type methods,
            // namely REQUEST/PUBLISH/CANCEL/ADD/DECLINECOUNTER.  REPLY/REFRESH/COUNTER don't undergo
            // the check because they are not organizer-to-attendee methods.
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
                if (senderEmail != null) {
                    senderAcct = Provisioning.getInstance().get(AccountBy.name, senderEmail);
                }
                canInvite = accessMgr.canDo(senderEmail, acct, User.R_invite, false);
            }
            if (!canInvite) {
                Invite invite = invites.get(0);
                CalendarMailSender.handleInviteAutoDeclinedNotification(octxt, getMailbox(), acct,
                        senderEmail, senderAcct, onBehalfOf, applyToCalendar, getId(), invite);
                String inviteSender = senderEmail != null ? senderEmail : "unknown sender";
                ZimbraLog.calendar.info("Calendar invite from %s to %s is not allowed", inviteSender, acct.getName());

                // Turn off auto-add.  We still have to run through the code below to save the invite's
                // data in message's metadata.
                status.autoAddNew = false;
            }
        }

        // Override CLASS property if preference says to mark everything as private.
        PrefCalendarApptVisibility prefClass = acct.getPrefCalendarApptVisibility();
        boolean forcePrivateClass = prefClass != null && !prefClass.equals(PrefCalendarApptVisibility.public_);

        // Ignore alarms set by organizer.
        boolean allowOrganizerAlarm = DebugConfig.calendarAllowOrganizerSpecifiedAlarms;

        if (calendarItemInfos == null) {
            calendarItemInfos = new ArrayList<CalendarItemInfo>();
        }

        // Clean up invalid missing organizer/attendee from some Exchanged-originated invite. (bug 43195)
        // We are looking for a multi-VEVENT structure where the series VEVENT has organizer and attendees
        // specified and exception instance VEVENTs omit them.  Exchange seems to expect you to inherit
        // organizer/attendee from series, but that's wrong.  Fix the data by duplicating the missing
        // properties.
        if (invites.size() > 1) {
            boolean hasSeries = false;
            ZOrganizer seriesOrganizer = null;
            boolean seriesIsOrganizer = false;
            List<ZAttendee> seriesAttendees = null;
            ParsedDateTime seriesDtStart = null;
            // Get organizer and attendees from series VEVENT.
            for (Invite inv : invites) {
                if (!inv.hasRecurId()) {
                    hasSeries = true;
                    seriesOrganizer = inv.getOrganizer();
                    seriesIsOrganizer = inv.isOrganizer();
                    seriesAttendees = inv.getAttendees();
                    seriesDtStart = inv.getStartTime();
                    break;
                }
            }
            if (hasSeries) {
                for (Invite inv : invites) {
                    RecurId rid = inv.getRecurId();
                    if (rid != null) {
                        if (seriesOrganizer != null && !inv.hasOrganizer()) {
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
                        if (!inv.isAllDayEvent() && seriesDtStart != null) {
                            // Exchange can send invalid RECURRENCE-ID with HHMMSS set to 000000.  Detect it and fix it up
                            // by copying the time from series DTSTART.
                            ParsedDateTime ridDt = rid.getDt();
                            if (ridDt != null && ridDt.hasZeroTime() &&
                                !seriesDtStart.hasZeroTime() && ridDt.sameTimeZone(seriesDtStart)) {
                                ParsedDateTime fixedDt = seriesDtStart.cloneWithNewDate(ridDt);
                                RecurId fixedRid = new RecurId(fixedDt, rid.getRange());
                                ZimbraLog.calendar.debug("Fixed up invalid RECURRENCE-ID with zero time; before=[%s], after=[%s]",
                                        rid, fixedRid);
                                inv.setRecurId(fixedRid);
                            }
                        }
                        // Exception instance invites shouldn't point to the same MIME part in the appointment blob
                        // as the series invite.  If they do, we will lose the series attachment when a new exception
                        // instance update is received.
                        inv.setMailItemId(0);
                    }
                }
            }
        }

        boolean publicInvites = true;  // used to check if any invite is non-public
        status.calItemFolderId = invites.size() > 0 && invites.get(0).isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
        CalendarItem firstCalItem = null;
        Set<String> calUidsSeen = new HashSet<String>();
        for (Invite cur : invites) {
            if (!cur.isPublic()) {
                publicInvites = false;
            }

            // Bug 38550/41239: If ORGANIZER is missing, set it to email sender.  If that sender
            // is the same user as the recipient, don't set organizer and clear attendees instead.
            // We don't want to set organizer to receiving user unless we're absolutely certain
            // it's the correct organizer.
            if (!cur.hasOrganizer() && cur.hasOtherAttendees()) {
                String fromEmail = pm.getSenderEmail(true);
                if (fromEmail != null) {
                    boolean dangerousSender = false;
                    // Is sender == recipient?  If so, clear attendees.
                    if (status.intendedForAddress != null) {
                        if (status.intendedForAddress.equalsIgnoreCase(fromEmail)) {
                            ZimbraLog.calendar.info(
                                    "Got malformed invite without organizer.  Clearing attendees to prevent inadvertent cancels.");
                            cur.clearAttendees();
                            dangerousSender = true;
                        }
                    } else if (acctMatcher.matches(fromEmail)) {
                        ZimbraLog.calendar.info(
                                "Got malformed invite without organizer.  Clearing attendees to prevent inadvertent cancels.");
                        cur.clearAttendees();
                        dangerousSender = true;
                    }
                    if (!dangerousSender) {
                        if (isOrganizerMethod = Invite.isOrganizerMethod(method)) {
                            // For organizer-originated methods, use email sender as default organizer.
                            ZOrganizer org = new ZOrganizer(fromEmail, null);
                            String senderEmail = pm.getSenderEmail(false);
                            if (senderEmail != null && !senderEmail.equalsIgnoreCase(fromEmail))
                                org.setSentBy(senderEmail);
                            cur.setOrganizer(org);
                            ZimbraLog.calendar.info(
                                    "Got malformed invite that lists attendees without specifying an organizer.  " +
                                    "Defaulting organizer to: " + org.toString());
                        } else {
                            // For attendee-originated methods, look up organizer from appointment on calendar.
                            // If appointment is not found, fall back to the intended-for address, then finally to self.
                            ZOrganizer org = null;
                            CalendarItem ci = mMailbox.getCalendarItemByUid(octxt, cur.getUid());
                            if (ci != null) {
                                Invite inv = ci.getInvite(cur.getRecurId());
                                if (inv == null) {
                                    inv = ci.getDefaultInviteOrNull();
                                }
                                if (inv != null) {
                                    org = inv.getOrganizer();
                                }
                            }
                            if (org == null) {
                                if (status.intendedForAddress != null) {
                                    org = new ZOrganizer(status.intendedForAddress, null);
                                } else {
                                    org = new ZOrganizer(acct.getName(), null);
                                }
                            }
                            cur.setOrganizer(org);
                            cur.setIsOrganizer(status.intendedForMe);
                            ZimbraLog.calendar.info("Got malformed reply missing organizer.  Defaulting to " + org.toString());
                        }
                    }
                }
            }

            cur.setLocalOnly(false);
            status.initAddRevisionSetting(cur.getUid(), calUidsSeen);

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

            getInfoForAssociatedCalendarItem(acct, cur, method, pm, applyToCalendar, status);
            if (firstCalItem == null) {
                firstCalItem = status.calItem;
            }
        }

        if (status.updatedMetadata) {
            saveMetadata();
        }


        // Forward a copy of the message to calendar admin user if preference says so.

        // Don't forward when processing as message-only.  (applyToCalendar == false)
        // Don't forward a forwarded invite.  Prevent infinite loop.
        // Don't forward the message being added to Sent folder.
        // Don't forward from a system account. (e.g. archiving, galsync, ham/spam)
        if (applyToCalendar && !status.isForwardedInvite && status.intendedForMe && folderId != Mailbox.ID_FOLDER_SENT &&
            !invites.isEmpty() && !acct.isIsSystemResource()) {
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
                        if (invCalItem != null && invCalItem.isOrganizer()) {
                            ZOrganizer org = invCalItem.getOrganizer();
                            if (org.hasSentBy()) {
                                forwardTo = new String[] { org.getSentBy() };
                            }
                        }
                    }
                }

                Account senderAcct = null;
                String senderEmail = pm.getSenderEmail(false);
                if (senderEmail != null)
                    senderAcct = Provisioning.getInstance().get(AccountBy.name, senderEmail);

                if (forwardTo != null && forwardTo.length > 0) {
                    List<String> rcptsUnfiltered = new ArrayList<String>();  // recipients to receive unfiltered message
                    List<String> rcptsFiltered = new ArrayList<String>();    // recipients to receive message filtered to remove private data
                    Folder calFolder = null;
                    try {
                        calFolder = getMailbox().getFolderById(status.calItemFolderId);
                    } catch (NoSuchItemException e) {
                        ZimbraLog.mailbox.warn("No such calendar folder (" + status.calItemFolderId + ") during invite auto-forwarding");
                    }
                    for (String fwd : forwardTo) {
                        if (fwd != null) {
                            fwd = fwd.trim();
                        }
                        if (StringUtil.isNullOrEmpty(fwd)) {
                            continue;
                        }
                        // Prevent forwarding to self.
                        if (acctMatcher.matches(fwd))
                            continue;
                        // Don't forward back to the sender.  It's redundant and confusing.
                        Account rcptAcct = Provisioning.getInstance().get(AccountBy.name, fwd);
                        boolean rcptIsSender = false;
                        if (rcptAcct != null) {
                            if (senderAcct != null) {
                                rcptIsSender = rcptAcct.getId().equalsIgnoreCase(senderAcct.getId());
                            } else {
                                rcptIsSender = AccountUtil.addressMatchesAccount(rcptAcct, senderEmail);
                            }
                        } else {
                            if (senderAcct != null) {
                                rcptIsSender = AccountUtil.addressMatchesAccount(senderAcct, fwd);
                            } else {
                                rcptIsSender = fwd.equalsIgnoreCase(senderEmail);
                            }
                        }
                        if (rcptIsSender) {
                            ZimbraLog.calendar.info("Not auto-forwarding to " + fwd + " because it is the sender of this message");
                            continue;
                        }
                        if (publicInvites) {
                            rcptsUnfiltered.add(fwd);
                        } else {
                            boolean allowed = false;
                            if (calFolder != null && rcptAcct != null) {
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
                                    CalendarMailSender.sendInviteAutoForwardMessage(octxt, getMailbox(), origMsgId, mm);
                                }
                            }
                            if (!rcptsFiltered.isEmpty()) {
                                MimeMessage mm = CalendarMailSender.createForwardedPrivateInviteMessage(
                                        acct, acct.getLocale(), method, invites, origSender, forwarder, rcptsFiltered.toArray(new String[0]));
                                if (mm != null) {
                                    ItemId origMsgId = new ItemId(getMailbox(), getId());
                                    CalendarMailSender.sendInviteAutoForwardMessage(octxt, getMailbox(), origMsgId, mm);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Update {@code status.calItem} and {@code calendarItemInfos}
     */
    private void getInfoForAssociatedCalendarItem(Account acct, Invite cur, String method, ParsedMessage pm,
            boolean applyToCalendar, ProcessInvitesStatus status) throws ServiceException {
        boolean calItemIsNew = false;
        boolean modifiedCalItem = false;
        boolean success = false;
        try {
            InviteChanges invChanges = null;
            // Look for organizer-provided change list.
            ZProperty changesProp = cur.getXProperty(ICalTok.X_ZIMBRA_CHANGES.toString());
            if (changesProp != null) {
                invChanges = new InviteChanges(changesProp.getValue());
                // Don't let the x-prop propagate further. This x-prop is used during transport only. Presence
                // of this x-prop in the appointment object can confuse clients.
                cur.removeXProp(ICalTok.X_ZIMBRA_CHANGES.toString());
            }
            if (!(status.intendedForMe || status.intendedForCalendarIManage)) {
                // Not intended for me. Just save the invite detail in metadata.
                CalendarItemInfo info = new CalendarItemInfo(CalendarItemInfo.CALITEM_ID_NONE, cur.getComponentNum(),
                        cur, invChanges);
                calendarItemInfos.add(info);
                status.updatedMetadata = true;
                success = true;
                return;
            }

            OperationContext octxt = getMailbox().getOperationContext();
            ICalTok methodTok = Invite.lookupMethod(method);
            AccountAddressMatcher acctMatcher = status.getAcctMatcher();
            cur.sanitize(true);
            if (status.intendedForCalendarIManage) {
                Provisioning prov = Provisioning.getInstance();
                Account ownerAcct = prov.get(AccountBy.name, calendarIntendedFor);
                com.zimbra.soap.mail.type.CalendarItemInfo cii =
                        getMailbox().getRemoteCalItemByUID(ownerAcct, cur.getUid(), false, false);
                CalendarItemInfo info;
                if (cii == null) {
                    info = new CalendarItemInfo(CalendarItemInfo.CALITEM_ID_NONE, cur.getComponentNum(), cur, invChanges);
                    calendarItemInfos.add(info);
                } else {
                    int calItemId;
                    String owner;
                    try {
                        ItemId iid = new ItemId(cii.getId(), (String)null);
                        calItemId = iid.getId();
                        owner = iid.getAccountId();
                    } catch (Exception e) {
                        calItemId = CalendarItemInfo.CALITEM_ID_NONE;
                        owner = null;
                    }
                    info = new CalendarItemInfo(calItemId, owner, cur.getComponentNum(), cur, invChanges);
                    calendarItemInfos.add(info);
                }
                status.updatedMetadata = true;
                success = true;
                return;
            }
            status.calItem = mMailbox.getCalendarItemByUid(octxt, cur.getUid());
            if (applyToCalendar &&
                !ICalTok.REPLY.equals(methodTok) && // replies are handled elsewhere (in Mailbox.addMessage())
                !ICalTok.COUNTER.equals(methodTok) && !ICalTok.DECLINECOUNTER.equals(methodTok)) {
                if (status.calItem == null) {
                    // ONLY create a calendar item if this is a REQUEST method...otherwise don't.
                    // Allow PUBLISH method as well depending on the preference.
                    if (ICalTok.REQUEST.equals(methodTok)
                            || (ICalTok.PUBLISH.equals(methodTok) && getAccount().getBooleanAttr(
                                    Provisioning.A_zimbraPrefCalendarAllowPublishMethodInvite, false))) {
                        if (status.autoAddNew) {
                            if (mMailbox.getAccount().sameAccount(cur.getOrganizerAccount())) {
                                // Bug 100456 ZCO sends out invites before adding a calendar entry.  If server adds
                                // Calendar entry and ZCO sees that before creating its entry, ZCO gets confused.
                                LOG.info("Mailbox %d Msg %d Don't create ORGANIZER calendar item for Invite %s",
                                        getMailboxId(), getId(), method);
                            } else {
                                int flags = 0;
                                // int flags = Flag.BITMASK_INDEXING_DEFERRED;
                                // mMailbox.incrementIndexDeferredCount(1);
                                int defaultFolder = cur.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
                                status.calItem = mMailbox.createCalendarItem(
                                        defaultFolder, flags, null, cur.getUid(), pm, cur, null);
                                calItemIsNew = true;
                                status.calItemFolderId = status.calItem.getFolderId();
                            }
                        }
                    } else {
                        LOG.info("Mailbox %d Message %d SKIPPING Invite %s b/c no CalendarItem could be found",
                                getMailboxId(), getId(), method);
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
                    Invite defInv = status.calItem.getDefaultInviteOrNull();
                    if (defInv != null && defInv.isOrganizer()) {
                        ZOrganizer org = cur.getOrganizer();
                        String orgAddress = org != null ? org.getAddress() : null;
                        if (acctMatcher.matches(orgAddress))
                            ignore = !acct.getBooleanAttr(
                                    Provisioning.A_zimbraPrefCalendarAllowCancelEmailToSelf, false);
                    }
                    if (ignore) {
                        ZimbraLog.calendar.info("Ignoring calendar request emailed from organizer to self, " +
                                "possibly in a mail loop involving mailing lists and/or forwards; " +
                                "calItemId=" + status.calItem.getId() + ", msgId=" + getId());
                    } else {
                        // When updating an existing calendar item, ignore the
                        // passed-in folderId which will usually be Inbox.  Leave
                        // the calendar item in the folder it's currently in.
                        if (status.addRevision)
                            status.calItem.snapshotRevision(false);
                        // If updating (but not canceling) an appointment in trash folder,
                        // use default calendar folder and discard all existing invites.
                        boolean discardExistingInvites = false;
                        int calFolderId = status.calItem.getFolderId();
                        if (!cur.isCancel() && status.calItem.inTrash()) {
                            discardExistingInvites = true;
                            if (status.calItem.getType() == MailItem.Type.TASK) {
                                calFolderId = Mailbox.ID_FOLDER_TASKS;
                            } else {
                                calFolderId = Mailbox.ID_FOLDER_CALENDAR;
                            }
                        }

                        // If organizer didn't provide X-ZIMBRA-CHANGES, calculate what changed by comparing
                        // against the current appointment data.
                        if (invChanges == null && !cur.isCancel()) {
                            Invite prev = status.calItem.getInvite(cur.getRecurId());
                            if (prev == null) {
                                // If incoming invite is a brand-new exception instance, we have to compare
                                // against the series data, but adjusted to the RECURRENCE-ID of the instance.
                                Invite series = status.calItem.getInvite(null);
                                if (series != null)
                                    prev = series.makeInstanceInvite(cur.getRecurId().getDt());
                            }
                            if (prev != null)
                                invChanges = new InviteChanges(prev, cur);
                        }

                        modifiedCalItem = status.calItem.processNewInvite(
                                pm, cur, calFolderId, CalendarItem.NEXT_ALARM_KEEP_CURRENT,
                                true /* preserveAlarms */, discardExistingInvites,
                                false /* updatePrevFolders */);
                        status.calItemFolderId = calFolderId;
                        status.calItem.getFolder().updateHighestMODSEQ();
                    }
                }
            }

            int calItemId = status.calItem != null ? status.calItem.getId() : CalendarItemInfo.CALITEM_ID_NONE;
            CalendarItemInfo info = new CalendarItemInfo(calItemId, cur.getComponentNum(), cur, invChanges);
            calendarItemInfos.add(info);
            status.updatedMetadata = true;
            if (status.calItem != null && (calItemIsNew || modifiedCalItem)) {
                mMailbox.indexItem(status.calItem);
            }
            success = true;
        } finally {
            if (!success && status.calItem != null) {
                // Error occurred and the calItem in memory may be out of sync with the database.
                // Uncache it here, because the error will be ignored by this method's caller.
                getMailbox().uncache(status.calItem);
            }
        }
    }

    public boolean hasDataSourceId() {
        return !Strings.isNullOrEmpty(dsId);
    }

    public String getDataSourceId() {
        return dsId;
    }

    public boolean isSentByMe() {
        return sentByMe;
    }

    public EventFlag getEventFlag() {
        return getState().getEventFlag();
    }

    void setEventFlag(EventFlag eventFlag) {
        getState().setEventFlag(eventFlag);
        try {
            mMailbox.cache(this);
        } catch (ServiceException e) {
            ZimbraLog.event.warn("unable to cache item %s with event flag %s; duplicate events may be generated", getId(), eventFlag.name(), e);
        }
    }

    /**
     * Try to advance the event flag on the message. If the provided flag is the next
     * step in the progression, log the appropriate event and update to the current flag
     * @return whether the event flag was advanced
     */
    public boolean advanceEventFlag(EventFlag nextEventFlag) {
        if (!isSentByMe() && getEventFlag().canAdvanceTo(nextEventFlag) && nextEventFlag != EventFlag.not_seen) {
            Event event = Event.generateMsgEvent(this, nextEventFlag.getEventType());
            EventLogger.getEventLogger().log(event);
            setEventFlag(nextEventFlag);
            return true;
        }
        return false;
    }

    void updateBlobData(MailboxBlob mblob) throws IOException, ServiceException {
        long size = mblob.getSize();
        if (getSize() == size && StringUtil.equal(getDigest(), mblob.getDigest()) && StringUtil.equal(getLocator(), mblob.getLocator()))
            return;

        long curSize = state.getSize();
        mMailbox.updateSize(size - curSize, true);
        getFolder().updateSize(0, 0, size - curSize);

        state.setSize(size);
        state.setLocator(mblob.getLocator());
        state.setBlobDigest(mblob.getDigest());
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
    @Override
    protected void updateUnread(int delta, int deletedDelta) throws ServiceException {
        if ((delta == 0 && deletedDelta == 0) || !trackUnread()) {
            return;
        }
        markItemModified(Change.UNREAD);

        // grab the parent *before* we make any other changes
        MailItem parent = getParent();

        // update our unread count (should we check that we don't have too many unread?)
        int newUnreadCount = getUnreadCount() + delta;
        state.setUnreadCount(newUnreadCount);
        if (newUnreadCount < 0) {
            mMailbox.isDirtyTransaction = true;
            ZimbraLog.mailbox.warn("inconsistent state: unread < 0 for %s %d", getClass().getName(), mId);
        }

        // update the folder's unread count
        getFolder().updateUnread(delta, deletedDelta);

        // update the conversation's unread count
        if (parent != null) {
            parent.updateUnread(delta, deletedDelta);
        }

        // tell the tags about the new read/unread item
        updateTagUnread(delta, deletedDelta);

        if (delta < 0 && EventLogger.getEventLogger().isEnabled()) {
            // log a READ event if this is the first time the message is being marked as unread
            mMailbox.advanceMessageEventFlag(this, EventFlag.read);
        }
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override MailItem copy(Folder folder, int id, String uuid, MailItem newParent) throws IOException, ServiceException {
        Message copy = (Message) super.copy(folder, id, uuid, newParent);

        if (isDraft()) {
            copy.setDraftAutoSendTime(0);
        }

        Conversation parent = (Conversation) getParent();
        if (parent instanceof VirtualConversation &&
                parent.getId() == (newParent == null ? -1 : newParent.mId) &&
                !isDraft() && inSpam() == folder.inSpam()) {
            Conversation conv = mMailbox.createConversation(Mailbox.ID_AUTO_INCREMENT, this, copy);
            DbMailItem.changeOpenTargets(this, conv.getId());
            parent.removeChild(this);
        }
        return copy;
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override
    MailItem icopy(Folder target, int copyId, String copyUuid) throws IOException, ServiceException {
        Message copy = (Message) super.icopy(target, copyId, copyUuid);
        if (isDraft()) {
            copy.setDraftAutoSendTime(0);
        }
        return copy;
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_DELETE} on the source folder */
    @Override
    boolean move(Folder target) throws ServiceException {
        boolean moved = super.move(target);
        if (moved && isDraft() && target.inTrash()) {
            setDraftAutoSendTime(0);
        }
        return moved;
    }

    public ParsedMessage getParsedMessage() throws ServiceException {
        ParsedMessage pm = null;
        try (final MailboxLock l = mMailbox.getWriteLockAndLockIt()) {
            // force the pm's received-date to be the correct one
            ParsedMessageOptions opt = new ParsedMessageOptions().setContent(getMimeMessage(false))
                .setReceivedDate(getDate())
                .setAttachmentIndexing(getMailbox().attachmentsIndexingEnabled())
                .setSize(getSize())
                .setDigest(getDigest());
            pm = new ParsedMessage(opt);
            return pm;
        }
    }

    @Override
    /**
     * Does not hold mailbox lock while retrieving message content
     */
    public List<IndexDocument> generateIndexDataAsync(boolean indexAttachments) throws TemporaryIndexingException {
        try {
            ParsedMessage pm = null;
            Account acc = Provisioning.getInstance().getAccountById(this.getAccountId());
            // force the pm's received-date to be the correct one
            ParsedMessageOptions opt = new ParsedMessageOptions().setContent(getMimeMessage(false,acc ))
                .setReceivedDate(getDate())
                .setAttachmentIndexing(indexAttachments)
                .setSize(getSize())
                .setDigest(getDigest());

            pm = new ParsedMessage(opt);
            pm.setDefaultCharset(acc.getPrefMailDefaultCharset());
            pm.analyzeFully();

            if (pm.hasTemporaryAnalysisFailure()) {
                throw new TemporaryIndexingException();
            }
            return checkNumIndexDocs(pm.getLuceneDocuments());
        } catch (ServiceException e) {
            ZimbraLog.index.warn("Unable to generate index data for Message %d. Item will not be indexed.", getId(), e);
            return Collections.emptyList();
        }
    }

    @Override
    void reanalyze(Object data, long newSize) throws ServiceException {
        if (!(data instanceof ParsedMessage)) {
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedMessage object", null);
        }

        Account acct = getAccount();
        ParsedMessage pm = (ParsedMessage) data;
        MailItem parent = getParent();

        // make sure the SUBJECT is correct
        if (!getSubject().equals(pm.getSubject())) {
            markItemModified(Change.SUBJECT);
        }

        rawSubject = pm.getSubject();
        state.setSubject(pm.getNormalizedSubject());

        markItemModified(Change.METADATA);

        // recipients may have changed
        recipients = pm.getRecipients();

        // the fragment may have changed
        fragment = pm.getFragment(acct.getLocale());

        // make sure the "attachments" FLAG is correct
        boolean hadAttachment = state.isSet(Flag.FlagInfo.ATTACHED);
        state.unsetFlag(Flag.FlagInfo.ATTACHED);
        if (pm.hasAttachments()) {
            state.setFlag(Flag.FlagInfo.ATTACHED);
        }
        if (hadAttachment != pm.hasAttachments()) {
            markItemModified(Change.FLAGS);
            parent.tagChanged(mMailbox.getFlagById(Flag.ID_ATTACHED), pm.hasAttachments());
        }

        // make sure the "urgency" FLAGs are correct
        int oldUrgency = state.getFlags() & (Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
        int urgency = pm.getPriorityBitmask();
        state.unsetFlag(Flag.FlagInfo.HIGH_PRIORITY);
        state.unsetFlag(Flag.FlagInfo.LOW_PRIORITY);
        state.setFlags(state.getFlags() | urgency);
        if (oldUrgency != urgency) {
            markItemModified(Change.FLAGS);
            if (urgency == Flag.BITMASK_HIGH_PRIORITY || oldUrgency == Flag.BITMASK_HIGH_PRIORITY) {
                parent.tagChanged(mMailbox.getFlagById(Flag.ID_HIGH_PRIORITY), urgency == Flag.BITMASK_HIGH_PRIORITY);
            }
            if (urgency == Flag.BITMASK_LOW_PRIORITY || oldUrgency == Flag.BITMASK_LOW_PRIORITY) {
                parent.tagChanged(mMailbox.getFlagById(Flag.ID_LOW_PRIORITY), urgency == Flag.BITMASK_LOW_PRIORITY);
            }
        }

        // update the SIZE and METADATA
        long curSize = state.getSize();
        if (curSize != newSize) {
            markItemModified(Change.SIZE);
            mMailbox.updateSize(newSize - curSize, false);
            getFolder().updateSize(0, 0, newSize - curSize);
            state.setSize(newSize);
        }

        updateIndexedDocCount(pm.getNumIndexDocs());

        // rewrite the DB row to reflect our new view
        saveData(new DbMailItem(mMailbox), encodeMetadata(state.getColor(), state.getMetadataVersion(), state.getVersion(), mExtendedData, pm, fragment,
                draftInfo, calendarItemInfos, calendarIntendedFor, dsId, sentByMe));

        if (parent instanceof VirtualConversation) {
            ((VirtualConversation) parent).recalculateMetadata(Collections.singletonList(this));
        }
    }

    @Override
    void detach() throws ServiceException {
        MailItem parent = getParent();
        if (!(parent instanceof Conversation)) {
            return;
        }

        if (parent.getSize() <= 1) {
            mMailbox.closeConversation((Conversation) parent, null);
        } else {
            // remove this message from its (real) conversation
            markItemModified(Change.PARENT);
            parent.removeChild(this);
            // and place it in a new, non-"opened", virtual conversation
            VirtualConversation vconv = new VirtualConversation(mMailbox, this);
            state.setParentId(vconv.getId());
            DbMailItem.setParent(this, vconv);
        }
    }

    @Override
    void delete(boolean writeTombstones) throws ServiceException {
        MailItem parent = getParent();
        if (parent instanceof Conversation && ((Conversation) parent).getMessageCount() == 1) {
            parent.delete(writeTombstones);
        } else {
            super.delete(writeTombstones);
        }
    }

    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        sender = meta.get(Metadata.FN_SENDER, null);
        recipients = meta.get(Metadata.FN_RECIPIENTS, null);
        fragment = meta.get(Metadata.FN_FRAGMENT, null);

        if (meta.containsKey(Metadata.FN_CALITEM_IDS)) {
            calendarItemInfos = new ArrayList<CalendarItemInfo>();
            MetadataList mdList = meta.getList(Metadata.FN_CALITEM_IDS);
            for (int i = 0; i < mdList.size(); i++) {
                Metadata md = mdList.getMap(i);
                calendarItemInfos.add(CalendarItemInfo.decodeMetadata(md, getMailbox()));
            }
        }
        calendarIntendedFor = meta.get(Metadata.FN_CAL_INTENDED_FOR, null);

        Metadata draftMeta = meta.getMap(Metadata.FN_DRAFT, true);
        if (draftMeta != null) {
            draftInfo = new DraftInfo(draftMeta);
        }

        String prefix = meta.get(Metadata.FN_PREFIX, null);
        String subject = state.getSubject();
        if (prefix != null) {
            rawSubject = (subject == null ? prefix : prefix + subject);
        } else {
            rawSubject = subject;
        }
        String rawSubj = meta.get(Metadata.FN_RAW_SUBJ, null);
        if (rawSubj != null) {
            rawSubject = rawSubj;
        }
        dsId = meta.get(Metadata.FN_DS_ID, null);
        sentByMe = meta.getBool(Metadata.FN_SENT_BY_ME, false);
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, state.getColor(), state.getMetadataVersion(), state.getVersion(), mExtendedData, sender, recipients, fragment,
                state.getSubject(), rawSubject, draftInfo, calendarItemInfos, calendarIntendedFor, dsId, sentByMe, state.getNumIndexDocs());
    }

    private static Metadata encodeMetadata(Color color, int metaVersion, int version, CustomMetadataList extended, ParsedMessage pm,
            String fragment, DraftInfo dinfo, List<CalendarItemInfo> calItemInfos, String calIntendedFor,
            String dsId, boolean sentByMe) {
        return encodeMetadata(new Metadata(), color, metaVersion, version, extended, pm.getSender(), pm.getRecipients(),
                fragment, pm.getNormalizedSubject(), pm.getSubject(), dinfo,
                calItemInfos, calIntendedFor, dsId, sentByMe, pm.getNumIndexDocs());
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, CustomMetadataList extended, String sender,
            String recipients, String fragment, String subject, String rawSubj, DraftInfo dinfo,
            List<CalendarItemInfo> calItemInfos, String calIntendedFor, String dsId, boolean sentByMe, int numIndexDocs) {
        // try to figure out a simple way to make the raw subject from the normalized one
        String prefix = null;
        if (rawSubj == null || rawSubj.equals(subject)) {
            rawSubj = null;
        } else if (rawSubj.endsWith(subject)) {
            prefix = rawSubj.substring(0, rawSubj.length() - subject.length());
            rawSubj = null;
        }

        meta.put(Metadata.FN_SENDER, sender);
        meta.put(Metadata.FN_RECIPIENTS, recipients);
        meta.put(Metadata.FN_FRAGMENT, fragment);
        meta.put(Metadata.FN_PREFIX, prefix);
        meta.put(Metadata.FN_RAW_SUBJ, rawSubj);
        meta.put(Metadata.FN_DS_ID, dsId);
        meta.put(Metadata.FN_SENT_BY_ME, sentByMe);

        if (calItemInfos != null) {
            MetadataList mdList = new MetadataList();
            for (CalendarItemInfo info : calItemInfos) {
                mdList.add(info.encodeMetadata());
            }
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

        return MailItem.encodeMetadata(meta, color, null, metaVersion, version, numIndexDocs, extended);
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

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        appendCommonMembers(helper);
        helper.add("sender", sender);
        if (recipients != null) {
            helper.add("to", recipients);
        }
        helper.add("fragment", fragment);
        return helper.toString();
    }

    @Override
    void alterTag(Tag tag, boolean add) throws ServiceException {
        if (add && (tag instanceof Flag)
                && ((Flag) tag).toBitmask() == FlagInfo.REPLIED.toBitmask()
                && EventLogger.getEventLogger().isEnabled()) {
            mMailbox.advanceMessageEventFlag(this, EventFlag.replied);
        }
        super.alterTag(tag, add);
    }

    protected static final Set<SortBy> SupportedSortsForMsgs = Sets.newHashSet(
            SortBy.DATE_ASC, SortBy.SIZE_ASC, SortBy.ID_ASC, SortBy.SUBJ_ASC, SortBy.NAME_ASC,
            SortBy.DATE_DESC, SortBy.SIZE_DESC, SortBy.ID_DESC, SortBy.SUBJ_DESC, SortBy.NAME_DESC,
            SortBy.NONE);

    protected static class SortAndIdComparator implements Comparator<Message> {
        private final SortBy sort;

        protected SortAndIdComparator(SortBy sort){
            this.sort = sort;
        }

        @Override
        public int compare(Message lhs, Message rhs) {
            try {
                int result = doCompare(lhs, rhs);
                if (result == 0) {
                    int lhsId = lhs.getId();
                    if (lhsId <= 0) {
                        lhsId = lhs.getConversationId();
                    }
                    int rhsId = rhs.getId();
                    if (rhsId <= 0) {
                        rhsId = rhs.getConversationId();
                    }

                    if (sort.getDirection() == SortBy.Direction.ASC) {
                        result = lhsId - rhsId;
                    } else {
                        result = rhsId - lhsId;
                    }
                }
                return result;
            } catch (ServiceException e) {
                ZimbraLog.search.error("Failed to compare %s and %s", lhs, rhs, e);
                return 0;
            }
        }
        int doCompare (Message lhs, Message rhs) throws ServiceException {
            switch (sort) {
            case DATE_ASC:
                return Long.signum(lhs.getDate() - rhs.getDate());
            case SIZE_ASC:
                return Long.signum(lhs.getSize() - rhs.getSize());
            case ID_ASC:
                return Integer.signum(lhs.getId() - rhs.getId());
            case SUBJ_ASC:
                return (lhs.getSubject()).toUpperCase().compareTo((rhs.getSubject()).toUpperCase());
            case NAME_ASC:
                return (lhs.getName()).toUpperCase().compareTo((rhs.getName()).toUpperCase());
            case DATE_DESC:
                return Long.signum(rhs.getDate() - lhs.getDate());
            case SIZE_DESC:
                return Long.signum(rhs.getSize() - lhs.getSize());
            case ID_DESC:
                return Integer.signum(rhs.getId() - lhs.getId());
            case SUBJ_DESC:
                return (rhs.getSubject()).toUpperCase().compareTo((lhs.getSubject()).toUpperCase());
            case NAME_DESC:
                return (rhs.getName()).toUpperCase().compareTo((lhs.getName()).toUpperCase());
            case NONE:
            default:
                throw new IllegalArgumentException(sort.name());
            }
        }
    }

    public static enum EventFlag {
        not_seen((byte) 0),
        seen((byte) 1, EventType.SEEN),
        read((byte) 2, EventType.READ),
        replied((byte) 3, EventType.REPLIED);

        private byte id;
        private EventType eventType;

        private static final Map<Byte, EventFlag> MAP;
        static {
            ImmutableMap.Builder<Byte, EventFlag> builder = ImmutableMap.builder();
            for (EventFlag flag: EventFlag.values()) {
                builder.put(flag.id, flag);
            }
            MAP = builder.build();
        }
        private EventFlag(byte id) {
            this.id = id;
        }

        private EventFlag(byte id, EventType eventType) {
            this.id = id;
            this.eventType = eventType;
        }

        public byte getId() {
            return id;
        }

        public EventType getEventType() {
            return eventType;
        }

        public boolean canAdvanceTo(EventFlag other) {
            return id == other.id - 1;
        }

        public static EventFlag of(byte id) {
            EventFlag flag = MAP.get(id);
            if (flag == null) {
                ZimbraLog.event.warn("encountered invalid event flag id %s, defaulting to not_seen", id);
                return not_seen;
            } else {
                return flag;
            }
        }
    }

    @Override
    protected MailItemState initFieldCache(UnderlyingData data) {
        return new LocalMessageState(data);
    }

    protected MessageState getState() {
        return (MessageState) state;
    }
}
