/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.dom4j.QName;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zclient.ZContact;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMountpoint;

public class ItemActionHelper {

    public static ItemActionHelper TAG(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, boolean flagValue, TargetConstraint tcon, int tagId)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, 
                    ids, Op.TAG, type, flagValue, tcon);
        ia.setTagId(tagId);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionHelper FLAG(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, boolean flagValue, TargetConstraint tcon)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.FLAG, type, flagValue, tcon);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionHelper READ(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, boolean flagValue, TargetConstraint tcon)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.READ, type, flagValue, tcon);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionHelper COLOR(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, TargetConstraint tcon, MailItem.Color color)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.COLOR, type, true, tcon);
        ia.setColor(color);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper HARD_DELETE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, TargetConstraint tcon)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.HARD_DELETE, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper MOVE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, TargetConstraint tcon, ItemId iidFolder)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.MOVE, type, true, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper COPY(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, TargetConstraint tcon, ItemId iidFolder)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.COPY, type, true, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper SPAM(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, boolean flagValue, 
                TargetConstraint tcon, ItemId iidFolder)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.SPAM, type, flagValue, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper RENAME(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, TargetConstraint tcon, String name, ItemId iidFolder)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.RENAME, type, true, tcon);
        ia.setName(name);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper UPDATE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, byte type, TargetConstraint tcon, String name, ItemId iidFolder, String flags,
                String tags, MailItem.Color color)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.UPDATE, type, true, tcon);
        ia.setName(name);
        ia.setIidFolder(iidFolder);
        ia.setFlags(flags);
        ia.setTags(tags);
        ia.setColor(color);
        ia.schedule();
        return ia;
    }
                
    public static ItemActionHelper LOCK(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, byte type, TargetConstraint tcon)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.LOCK, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper UNLOCK(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, byte type, TargetConstraint tcon)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.UNLOCK, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static enum Op {
        TAG("tag"),
        FLAG("flag"),
        READ("read"),
        COLOR("color"),
        HARD_DELETE("delete"),
        MOVE("move"),
        COPY("copy"),
        SPAM("spam"),
        RENAME("rename"),
        UPDATE("update"),
        LOCK("lock"),
        UNLOCK("unlock");
        
        private String mStr;

        private Op(String str)  { mStr = str; }

        @Override public String toString()  { return mStr; }
    }
    
    protected String mResult;
    protected List<String> mCreatedIds;

    protected SoapProtocol mResponseProtocol;
    protected Op mOperation;
    protected int[] mIds;
    protected byte mItemType;
    protected boolean mFlagValue;
    protected TargetConstraint mTargetConstraint;
    protected int mHopCount;

    // only when Op=TAG
    protected int mTagId;

    // only when OP=COLOR or OP=UPDATE
    protected MailItem.Color mColor;

    // only when OP=RENAME or OP=UPDATE
    protected String mName; 

    // only when OP=MOVE or OP=COPY or OP=RENAME or OP=UPDATE or OP=SPAM
    protected ItemId mIidFolder, mIidRequestedFolder; 

    // only when OP=UPDATE
    protected String mFlags;
    protected String mTags;

    protected ItemIdFormatter mIdFormatter;
    protected Account mAuthenticatedAccount;


    @Override public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        
        toRet.append(" Op=").append(mOperation.toString());
        toRet.append(" Type=").append(mItemType);
        toRet.append(" FlagValue=").append(mFlagValue);
        if (mTargetConstraint != null) 
            toRet.append(" TargetConst=").append(mTargetConstraint.toString());

        if (mOperation == Op.TAG) 
            toRet.append(" TagId=").append(mTagId);

        if (mOperation == Op.COLOR || mOperation == Op.UPDATE)
            toRet.append(" Color=").append(mColor);

        if (mOperation == Op.MOVE || mOperation == Op.SPAM || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE) 
            toRet.append(" iidFolder=").append(mIidFolder);

        if (mOperation == Op.UPDATE) {
            if (mFlags != null) 
                toRet.append(" flags=").append(mFlags);
            if (mTags != null) 
                toRet.append(" tags=").append(mTags);
        }
        return toRet.toString();
    }
    
    public void setTagId(int tagId) {
        assert(mOperation == Op.TAG);
        mTagId = tagId;
    }
    public void setColor(MailItem.Color color) { 
        assert(mOperation == Op.COLOR || mOperation == Op.UPDATE);
        mColor = color; 
    }
    public void setName(String name) { 
        assert(mOperation == Op.RENAME || mOperation == Op.UPDATE);
        mName = name; 
    }
    public void setIidFolder(ItemId iidFolder)  { 
        assert(mOperation == Op.MOVE || mOperation == Op.SPAM || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE);
        mIidRequestedFolder = mIidFolder = iidFolder; 
    }
    public void setFlags(String flags) {
        assert(mOperation == Op.UPDATE);
        mFlags = flags; 
    }
    public void setTags(String tags) {                        
        assert(mOperation == Op.UPDATE);
        mTags = tags; 
    }

    ItemActionHelper(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, Op op, byte type, boolean flagValue, TargetConstraint tcon)
    throws ServiceException {
        
        mOpCtxt = octxt;
        mMailbox = mbox;

        mAuthenticatedAccount = octxt == null ? null : octxt.getAuthenticatedUser();
        if (mAuthenticatedAccount == null)
            mAuthenticatedAccount = mbox.getAccount();
        mIdFormatter = new ItemIdFormatter(mAuthenticatedAccount, mbox, false);
        mResponseProtocol = responseProto;

        int i = 0;
        mIds = new int[ids.size()];
        for (int id : ids)
            mIds[i++] = id;

        mOperation = op;
        if (mOperation == null)
            throw ServiceException.INVALID_REQUEST("unknown operation: null", null);
        mItemType = type;
        mFlagValue = flagValue;
        mTargetConstraint = tcon;
    }
    
    private OperationContext mOpCtxt;
    private Mailbox mMailbox;
    protected Mailbox getMailbox() { return mMailbox; }
    protected OperationContext getOpCtxt() { return mOpCtxt; }

    protected void schedule() throws ServiceException {
        boolean targeted = mOperation == Op.MOVE || mOperation == Op.SPAM || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE;

        // deal with local mountpoints pointing at local folders here
        if (targeted && mIidFolder.belongsTo(mMailbox) && mIidFolder.getId() > 0 && mIidFolder.getId() != Mailbox.ID_FOLDER_TRASH && mIidFolder.getId() != Mailbox.ID_FOLDER_SPAM) {
            try {
                Folder folder = mMailbox.getFolderById(mOpCtxt, mIidFolder.getId());
                if (folder instanceof Mountpoint && !((Mountpoint) folder).getOwnerId().equals(mIidFolder.getAccountId())) {
                    mIidFolder = ((Mountpoint) folder).getTarget();
                    mHopCount++;
                }
            } catch (ServiceException e) {
                // could be a PERM_DENIED, could be something else -- this is not the right place to fail, however
            }
        }

        try {
            if (!targeted || mIidFolder.belongsTo(mMailbox))
                executeLocal();
            else
                executeRemote();
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("exception reading item blob", ioe);
        }

        StringBuilder successes = new StringBuilder();
        for (int id : mIds)
            successes.append(successes.length() > 0 ? "," : "").append(mIdFormatter.formatItemId(id));
        mResult = successes.toString();
    }

    public String getResult() {
        return mResult;
    }

    public List<String> getCreatedIds() {
        return mCreatedIds;
    }


    private void executeLocal() throws ServiceException {
        // iterate over the local items and perform the requested operation
        switch (mOperation) {
            case FLAG:
                getMailbox().alterTag(getOpCtxt(), mIds, mItemType, Flag.ID_FLAG_FLAGGED, mFlagValue, mTargetConstraint);
                break;
            case READ:
                getMailbox().alterTag(getOpCtxt(), mIds, mItemType, Flag.ID_FLAG_UNREAD, !mFlagValue, mTargetConstraint);
                break;
            case TAG:
                getMailbox().alterTag(getOpCtxt(), mIds, mItemType, mTagId, mFlagValue, mTargetConstraint);
                break;
            case COLOR:
                getMailbox().setColor(getOpCtxt(), mIds, mItemType, mColor);
                break;
            case HARD_DELETE:
                getMailbox().delete(getOpCtxt(), mIds, mItemType, mTargetConstraint);
                break;
            case SPAM:
            case MOVE:
                getMailbox().move(getOpCtxt(), mIds, mItemType, mIidFolder.getId(), mTargetConstraint);
                break;
            case COPY:
                List<MailItem> copies = getMailbox().copy(getOpCtxt(), mIds, mItemType, mIidFolder.getId());
                mCreatedIds = new ArrayList<String>(mIds.length);
                for (MailItem item : copies)
                    mCreatedIds.add(mIdFormatter.formatItemId(item));
                break;
            case RENAME:
                for (int id : mIds)
                    getMailbox().rename(getOpCtxt(), id, mItemType, mName, mIidFolder.getId());
                break;
            case UPDATE:
                if (mName != null) {
                    for (int id : mIds)
                        getMailbox().rename(getOpCtxt(), id, mItemType, mName, mIidFolder.getId());
                } else if (mIidFolder.getId() > 0) {
                    getMailbox().move(getOpCtxt(), mIds, mItemType, mIidFolder.getId(), mTargetConstraint);
                }
                if (mTags != null || mFlags != null)
                    getMailbox().setTags(getOpCtxt(), mIds, mItemType, mFlags, mTags, mTargetConstraint);
                if (mColor != null)
                    getMailbox().setColor(getOpCtxt(), mIds, mItemType, mColor);
                break;
            case LOCK:
                for (int id : mIds)
                    getMailbox().lock(getOpCtxt(), id, mItemType, mAuthenticatedAccount.getId());
                break;
            case UNLOCK:
                for (int id : mIds)
                    getMailbox().unlock(getOpCtxt(), id, mItemType, mAuthenticatedAccount.getId());
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unknown operation: " + mOperation, null);
        }
    }

    private AuthToken getAuthToken() throws ServiceException {
        AuthToken authToken = null;
        
        if (mOpCtxt != null)
            authToken = mOpCtxt.getAuthToken();
        
        if (authToken == null)
            authToken = AuthProvider.getAuthToken(mAuthenticatedAccount);

        return authToken;
    }
    
    private void executeRemote() throws ServiceException, IOException {
        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mIidFolder.getAccountId());
        
        AuthToken at = getAuthToken();
        String pxyAuthToken = Provisioning.onLocalServer(target) ? null : at.getProxyAuthToken();
        ZAuthToken zat = pxyAuthToken == null ? at.toZAuthToken() : new ZAuthToken(pxyAuthToken);
        
        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(target.getId());
        zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);

        // check for mountpoints before going any further...
        ZFolder zfolder = zmbx.getFolderById(mIidFolder.toString(mAuthenticatedAccount));
        if (zfolder instanceof ZMountpoint) {
            ItemId iidTarget = new ItemId(((ZMountpoint) zfolder).getCanonicalRemoteId(), mAuthenticatedAccount.getId());
            if (!mIidFolder.equals(iidTarget)) {
                mIidFolder = iidTarget;
                if (++mHopCount > com.zimbra.soap.ZimbraSoapContext.MAX_HOP_COUNT)
                    throw MailServiceException.TOO_MANY_HOPS(mIidRequestedFolder);
                schedule();
                return;
            }
        }

        boolean deleteOriginal = mOperation != Op.COPY;
        String folderStr = mIidFolder.toString();
        mCreatedIds = new ArrayList<String>(mIds.length);

        boolean toSpam = mIidFolder.getId() == Mailbox.ID_FOLDER_SPAM;
        boolean toMailbox = !toSpam && mIidFolder.getId() != Mailbox.ID_FOLDER_TRASH;

        for (MailItem item : mMailbox.getItemById(mOpCtxt, mIds, mItemType)) {
            if (item == null)
                continue;

            List<Message> msgs = null;
            if (item instanceof Conversation)
                msgs = mMailbox.getMessagesByConversation(mOpCtxt, item.getId(), SortBy.DATE_ASCENDING);

            if (deleteOriginal) {
                if (msgs != null) {
                    // determine which of the conversation's component messages are actually able to be moved
                    boolean permDenied = false;
                    for (Iterator<Message> it = msgs.iterator(); it.hasNext(); ) {
                        Message msg = it.next();
                        if (!TargetConstraint.checkItem(mTargetConstraint, msg)) {
                            it.remove();
                        } else if (!canDelete(msg)) {
                            it.remove();  permDenied = true;
                        }
                    }
                    // stop here if no messages would be moved...
                    if (msgs.isEmpty()) {
                        if (permDenied)
                            throw ServiceException.PERM_DENIED("cannot delete any messages in " + MailItem.getNameForType(item) + " " + item.getId());
                        // all messages were excluded by the TargetConstraint, so there's no failure...
                        continue;
                    }
                } else {
                    if (!canDelete(item))
                        throw ServiceException.PERM_DENIED("cannot delete existing copy of " + MailItem.getNameForType(item) + " " + item.getId());
                }
            }

            boolean fromSpam = item.inSpam();
            if ((fromSpam && toMailbox) || (!fromSpam && toSpam)) {
                try {
                    SpamHandler.getInstance().handle(mOpCtxt, mMailbox, item.getId(), item.getType(), toSpam);
                    ZimbraLog.mailop.info("sent to spam filter for training (marked as " + (toSpam ? "" : "not ") + "spam): " + new ItemId(item).toString());
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);
                } catch (Throwable t) {
                    ZimbraLog.mailop.info("could not train spam filter: " + new ItemId(item).toString(), t);
                }
            }

            // since we can't apply tags to a remote object, hardwiring "tags" to null below...
            String flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : item.getFlagString());
            String name = ((mOperation == Op.RENAME || mOperation == Op.UPDATE) && mName != null ? mName : item.getName());
            String createdId = null;
            InputStream in = null;

            switch (item.getType()) {
                case MailItem.TYPE_CONTACT:
                    ZContact contact = zmbx.createContact(folderStr, null, ((Contact) item).getFields());
                    createdId = contact.getId();
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_MESSAGE:
                    try {
                        in = StoreManager.getInstance().getContent(item.getBlob());
                        createdId = zmbx.addMessage(folderStr, flags, null, item.getDate(), in, item.getSize(), true);
                    } finally {
                        ByteUtil.closeStream(in);
                    }
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_VIRTUAL_CONVERSATION:
                case MailItem.TYPE_CONVERSATION:
                    for (Message msg : msgs) {
                        flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : msg.getFlagString());
                        try {
                            in = StoreManager.getInstance().getContent(msg.getBlob());
                            createdId = zmbx.addMessage(folderStr, flags, null, msg.getDate(), in, msg.getSize(), true);
                        } finally {
                            ByteUtil.closeStream(in);
                        }
                        mCreatedIds.add(createdId);
                    }
                    break;

                case MailItem.TYPE_DOCUMENT:
                    Document doc = (Document) item;
                    try {
                        in = StoreManager.getInstance().getContent(doc.getBlob());
                        String uploadId = zmbx.uploadContentAsStream(name, in, doc.getContentType(), doc.getSize(), 4000);
                        createdId = zmbx.createDocument(folderStr, name, uploadId);
                    } finally {
                        ByteUtil.closeStream(in);
                    }
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_WIKI:
                    createdId = zmbx.createWiki(folderStr, name, new String(((WikiItem) item).getContent(), "utf-8"));
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_APPOINTMENT:
                case MailItem.TYPE_TASK:
                    CalendarItem cal = (CalendarItem) item;
                    // private calendar item may not be moved by non-owner unless permission was granted
                    if (!cal.isPublic()) {
                        boolean asAdmin = mOpCtxt != null ? mOpCtxt.isUsingAdminPrivileges() : false;
                        if (!cal.allowPrivateAccess(mAuthenticatedAccount, asAdmin))
                            throw ServiceException.PERM_DENIED(
                                    "you do not have permission to move/copy a private calendar item from the current folder/mailbox");
                    }

                    // Move the item to remote mailbox using SetAppointmentRequest/SetTaskRequest.
                    QName qname = (item.getType() == MailItem.TYPE_TASK ? MailConstants.SET_TASK_REQUEST : MailConstants.SET_APPOINTMENT_REQUEST);
                    Element request = new Element.XMLElement(qname).addAttribute(MailConstants.A_FOLDER, folderStr).addAttribute(MailConstants.A_FLAGS, flags);
                    ToXML.encodeAlarmTimes(request, cal);

                    Invite invDefault = cal.getDefaultInviteOrNull();

                    // Takeover as organizer if we're doing a MOVE and source mailbox is the organizer.
                    // Don't takeover in a COPY operation.
                    boolean takeoverAsOrganizer = false;
                    if (Op.MOVE.equals(mOperation)) {
                        Invite inv = invDefault;
                        if (inv == null) {
                            // no default invite; let's use the first invite
                            Invite[] invs = cal.getInvites();
                            if (invs != null && invs.length > 0)
                                inv = invs[0];
                        }
                        takeoverAsOrganizer = inv != null && inv.isOrganizer();
                    }

                    if (invDefault != null) {
                        addCalendarPart(request.addUniqueElement(MailConstants.A_DEFAULT), cal, invDefault, zmbx, target, takeoverAsOrganizer);
                    }

                    for (Invite inv : cal.getInvites()) {
                        if (inv == null || inv == invDefault)
                            continue;
                        String elem = inv.isCancel() ? MailConstants.E_CAL_CANCEL : MailConstants.E_CAL_EXCEPT;
                        addCalendarPart(request.addElement(elem), cal, inv, zmbx, target, takeoverAsOrganizer);
                    }

                    ToXML.encodeCalendarReplies(request, cal);

                    createdId = zmbx.invoke(request).getAttribute(MailConstants.A_CAL_ID);
                    mCreatedIds.add(createdId);

                    if (takeoverAsOrganizer) {
                        // Announce organizer change to attendees.
                        request = new Element.XMLElement(MailConstants.ANNOUNCE_ORGANIZER_CHANGE_REQUEST);
                        request.addAttribute(MailConstants.A_ID, createdId);
                        zmbx.invoke(request);
                    }
                    break;

                default:
                    throw MailServiceException.CANNOT_COPY(item.getId());
            }

            try {
                if (deleteOriginal && !mIdFormatter.formatItemId(item).equals(createdId)) {
                    if (msgs == null) {
                        mMailbox.delete(mOpCtxt, item.getId(), item.getType());
                    } else {
                        for (Message msg : msgs)
                            mMailbox.delete(mOpCtxt, msg.getId(), msg.getType());
                    }
                }
            } catch (ServiceException e) {
                if (e.getCode() != ServiceException.PERM_DENIED)
                    throw e;
                // something funky happened permissions-wise between the getEffectivePermissions check and here...
                ZimbraLog.misc.info("could not delete original item " + item.getId() + "; treating operation as a copy instead");
            }
        }
    }

    private void addCalendarPart(Element parent, CalendarItem cal, Invite inv, ZMailbox zmbx, Account target, boolean takeoverAsOrganizer)
    throws ServiceException {
        parent.addAttribute(MailConstants.A_CAL_PARTSTAT, inv.getPartStat());
        Element m = parent.addUniqueElement(MailConstants.E_MSG);

        Pair<MimeMessage, Integer> spinfo = cal.getSubpartMessageData(inv.getMailItemId());
        if (spinfo != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(spinfo.getSecond());
                spinfo.getFirst().writeTo(baos);
                String uploadId = zmbx.uploadAttachment("message", baos.toByteArray(), MimeConstants.CT_MESSAGE_RFC822, 6000);
                m.addAttribute(MailConstants.A_ATTACHMENT_ID, uploadId);
            } catch (IOException ioe) {
                ZimbraLog.misc.info("could not read subpart message for part " + inv.getComponentNum() + " of item " + cal.getId(), ioe);
            } catch (MessagingException me) {
                ZimbraLog.misc.info("could not read subpart message for part " + inv.getComponentNum() + " of item " + cal.getId(), me);
            }
        }

        if (takeoverAsOrganizer && inv.isOrganizer() && inv.hasOrganizer()) {
            Invite invCopy = inv.newCopy();
            invCopy.setInviteId(inv.getMailItemId());
            // Increment SEQUENCE and bring DTSTAMP current because we're changing organizer.
            invCopy.setSeqNo(inv.getSeqNo() + 1);
            invCopy.setDtStamp(System.currentTimeMillis());
            ZOrganizer org = invCopy.getOrganizer();
            org.setAddress(target.getName());
            org.setCn(target.getDisplayName());
            Account authAcct = mOpCtxt != null ? mOpCtxt.getAuthenticatedUser() : target;
            if (authAcct == null || authAcct.equals(target))
                org.setSentBy(null);
            else
                org.setSentBy(authAcct.getName());
            inv = invCopy;
        }

        // explicitly add the invite metadata here
        ToXML.encodeInvite(m, mIdFormatter, getOpCtxt(), cal, inv, true);
    }

    private boolean canDelete(MailItem item) throws ServiceException {
        return (mMailbox.getEffectivePermissions(mOpCtxt, item.getId(), item.getType()) & ACL.RIGHT_DELETE) != 0;
    }
}
