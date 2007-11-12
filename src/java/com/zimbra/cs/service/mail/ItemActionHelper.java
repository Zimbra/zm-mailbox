/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;

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
                List<Integer> ids, byte type, TargetConstraint tcon, byte color)
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

    public static ItemActionHelper TRASH(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, 
                byte type, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.TRASH, type, true, tcon);
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
                String tags, byte color)
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
                
    
    public static enum Op {
        TAG("tag"),
        FLAG("flag"),
        READ("read"),
        COLOR("color"),
        HARD_DELETE("delete"),
        MOVE("move"),
        COPY("copy"),
        SPAM("spam"),
        TRASH("trash"),
        RENAME("rename"),
        UPDATE("update")
        ;
        
        private Op(String str) {
            mStr = str;
        }
        
        public String toString() { return mStr; }
        
        private String mStr;
    }
    
    protected String mResult;
    protected List<String> mCreatedIds;

    protected SoapProtocol mResponseProtocol;
    protected Op mOperation;
    protected int[] mIds;
    protected byte mItemType;
    protected boolean mFlagValue;
    protected TargetConstraint mTargetConstraint;

    // only when Op=TAG
    protected int mTagId;

    // only when OP=COLOR or OP=UPDATE
    protected byte mColor;

    // only when OP=RENAME or OP=UPDATE
    protected String mName; 

    // only when OP=MOVE or OP=COPY or OP=RENAME or OP=UPDATE or OP=SPAM
    protected ItemId mIidFolder; 

    // only when OP=UPDATE
    protected String mFlags;
    protected String mTags;

    protected ItemIdFormatter mIdFormatter;
    protected Account mAuthenticatedAccount;


    public String toString() {
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

        if (mOperation == Op.MOVE || mOperation == Op.UPDATE || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.SPAM) 
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
    public void setColor(byte color) { 
        assert(mOperation == Op.COLOR || mOperation == Op.UPDATE);
        mColor = color; 
    }
    public void setName(String name) { 
        assert(mOperation == Op.RENAME || mOperation == Op.UPDATE);
        mName = name; 
    }
    public void setIidFolder(ItemId iidFolder)  { 
        assert(mOperation == Op.MOVE || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE);
        mIidFolder = iidFolder; 
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
        boolean movable = mOperation == Op.MOVE || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE;

        // deal with local mountpoints pointing at local folders here
        if (movable && mIidFolder.belongsTo(mMailbox) && mIidFolder.getId() > 0) {
            Folder folder = mMailbox.getFolderById(mOpCtxt, mIidFolder.getId());
            if (folder instanceof Mountpoint && !((Mountpoint) folder).getOwnerId().equals(mIidFolder.getAccountId()))
                mIidFolder = new ItemId(((Mountpoint) folder).getOwnerId(), ((Mountpoint) folder).getRemoteId());
        }

        try {
            if (mOperation == Op.SPAM) {
                // make sure to always do the spam training before moving to the target folder
                for (int id : mIds)
                    SpamHandler.getInstance().handle(getMailbox(), id, mItemType, mFlagValue);
            }

            if (movable && !mIidFolder.belongsTo(mMailbox))
                executeRemote();
            else
                executeLocal();
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
            case TRASH:
                try {
                    // in general, everything will work fine, so just blindly try to move the targets to trash
                    getMailbox().move(getOpCtxt(), mIds, mItemType, Mailbox.ID_FOLDER_TRASH, mTargetConstraint);
                } catch (ServiceException e) {
                    if (!e.getCode().equals(MailServiceException.ALREADY_EXISTS))
                        throw e;
                    moveWithRename(Mailbox.ID_FOLDER_TRASH);
                }
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
                if (mColor >= 0)
                    getMailbox().setColor(getOpCtxt(), mIds, mItemType, mColor);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unknown operation: " + mOperation, null);
        }
    }

    private void moveWithRename(int targetId) throws ServiceException {
        // naming conflict; handle on an item-by-item basis
        for (int id : mIds) {
            try {
                // still more likely than not to succeed...
                getMailbox().move(getOpCtxt(), id, mItemType, targetId, mTargetConstraint);
            } catch (ServiceException e) {
                if (!e.getCode().equals(MailServiceException.ALREADY_EXISTS))
                    throw e;

                // rename the item being moved instead of the one already there...
                String name = getMailbox().getItemById(getOpCtxt(), id, mItemType).getName();
                String uuid = '{' + UUID.randomUUID().toString() + '}', newName;
                if (name.length() + uuid.length() > MailItem.MAX_NAME_LENGTH)
                    newName = name.substring(0, MailItem.MAX_NAME_LENGTH - uuid.length()) + uuid;
                else
                    newName = name + uuid;
                // FIXME: relying on the fact that conversations collect things that don't cause naming conflicts
                getMailbox().rename(getOpCtxt(), id, mItemType, newName, targetId);
            }
        }
    }


    private void executeRemote() throws ServiceException, IOException {
        String authtoken;
        try {
            authtoken = new AuthToken(mAuthenticatedAccount).getEncoded();
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("could not get auth token", e);
        }

        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mIidFolder.getAccountId());
        ZMailbox.Options zoptions = new ZMailbox.Options(authtoken, AccountUtil.getSoapUri(target));
        zoptions.setNoSession(true);
        zoptions.setResponseProtocol(mResponseProtocol);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);

        boolean deleteOriginal = mOperation != Op.COPY;
        String folderStr = mIidFolder.toString();
        mCreatedIds = new ArrayList<String>(mIds.length);

        for (MailItem item : mMailbox.getItemById(mOpCtxt, mIds, mItemType)) {
            if (item == null)
                continue;
            if (deleteOriginal && (mMailbox.getEffectivePermissions(mOpCtxt, item.getId(), item.getType()) & ACL.RIGHT_DELETE) == 0)
                throw ServiceException.PERM_DENIED("cannot delete existing copy of " + MailItem.getNameForType(item) + " " + item.getId());

            // since we can't apply tags to a remote object, hardwiring "tags" to null below...
            String flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : item.getFlagString());
            String name = ((mOperation == Op.RENAME || mOperation == Op.UPDATE) && mName != null ? mName : item.getName());
            String createdId = null;

            switch (item.getType()) {
                case MailItem.TYPE_CONTACT:
                    createdId = zmbx.createContact(folderStr, null, ((Contact) item).getFields());
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_VIRTUAL_CONVERSATION:
                    item = mMailbox.getMessageById(mOpCtxt, ((VirtualConversation) item).getMessageId());
                    // fall through...

                case MailItem.TYPE_MESSAGE:
                    createdId = zmbx.addMessage(folderStr, flags, null, item.getDate(), ((Message) item).getContent(), true);
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_CONVERSATION:
                    for (Message msg : mMailbox.getMessagesByConversation(mOpCtxt, item.getId(), Conversation.SORT_DATE_ASCENDING)) {
                        if (!TargetConstraint.checkItem(mTargetConstraint, msg))
                            continue;
                        flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : msg.getFlagString());
                        createdId = zmbx.addMessage(folderStr, flags, null, msg.getDate(), msg.getContent(), true);
                        mCreatedIds.add(createdId);
                    }
                    break;

                case MailItem.TYPE_DOCUMENT:
                    Document doc = (Document) item;
                    String uploadId = zmbx.uploadAttachment(name, doc.getContent(), doc.getContentType(), 4000);
                    createdId = zmbx.createDocument(folderStr, name, uploadId);
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_WIKI:
                    createdId = zmbx.createWiki(folderStr, name, new String(((WikiItem) item).getContent(), "utf-8"));
                    mCreatedIds.add(createdId);
                    break;

                case MailItem.TYPE_APPOINTMENT:
                case MailItem.TYPE_TASK:
                    // Move the item to remote mailbox using SetAppointmentRequest/SetTaskRequest.
                    QName qname = (item.getType() == MailItem.TYPE_TASK ? MailConstants.SET_TASK_REQUEST : MailConstants.SET_APPOINTMENT_REQUEST);
                    Element request = new Element.XMLElement(qname).addAttribute(MailConstants.A_FOLDER, folderStr).addAttribute(MailConstants.A_FLAGS, flags);
                    CalendarItem cal = (CalendarItem) item;
                    ToXML.encodeAlarmTimes(request, cal);

                    boolean isOrganizer = false;
                    Invite invDefault = cal.getDefaultInviteOrNull();
                    if (invDefault != null) {
                        if (invDefault.isOrganizer())
                            isOrganizer = true;
                        addCalendarPart(request.addUniqueElement(MailConstants.A_DEFAULT), cal, invDefault, zmbx, target);
                    }

                    for (Invite inv : cal.getInvites()) {
                        if (inv == null || inv == invDefault)
                            continue;
                        if (inv.isOrganizer())
                            isOrganizer = true;
                        String elem = inv.isCancel() ? MailConstants.E_CAL_CANCEL : MailConstants.E_CAL_EXCEPT;
                        addCalendarPart(request.addElement(elem), cal, inv, zmbx, target);
                    }

                    ToXML.encodeCalendarReplies(request, cal, null);

                    createdId = zmbx.invoke(request).getAttribute(MailConstants.A_CAL_ID);
                    mCreatedIds.add(createdId);

                    if (isOrganizer) {
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
                if (deleteOriginal && !mIdFormatter.formatItemId(item).equals(createdId))
                    mMailbox.delete(mOpCtxt, item.getId(), item.getType());
            } catch (ServiceException e) {
                if (e.getCode() != ServiceException.PERM_DENIED)
                    throw e;
                // something funky happened permissions-wise between the getEffectivePermissions check and here...
                ZimbraLog.misc.info("could not delete original item " + item.getId() + "; treating operation as a copy instead");
            }
        }
    }

    private void addCalendarPart(Element parent, CalendarItem cal, Invite inv, ZMailbox zmbx, Account target) throws ServiceException {
        parent.addAttribute(MailConstants.A_CAL_PARTSTAT, inv.getPartStat());
        Element m = parent.addUniqueElement(MailConstants.E_MSG);

        Pair<MimeMessage, Integer> spinfo = cal.getSubpartMessageData(inv.getMailItemId());
        if (spinfo != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(spinfo.getSecond());
                spinfo.getFirst().writeTo(baos);
                String uploadId = zmbx.uploadAttachment("message", baos.toByteArray(), Mime.CT_MESSAGE_RFC822, 6000);
                m.addAttribute(MailConstants.A_ATTACHMENT_ID, uploadId);
            } catch (IOException ioe) {
                ZimbraLog.misc.info("could not read subpart message for part " + inv.getComponentNum() + " of item " + cal.getId(), ioe);
            } catch (MessagingException me) {
                ZimbraLog.misc.info("could not read subpart message for part " + inv.getComponentNum() + " of item " + cal.getId(), me);
            }
        }

        if (inv.isOrganizer() && inv.hasOrganizer()) {
            Invite invCopy = inv.newCopy();
            invCopy.setInviteId(inv.getMailItemId());
            // Increment SEQUENCE and bring DTSTAMP current because we're changing organizer.
            invCopy.setSeqNo(inv.getSeqNo() + 1);
            invCopy.setDtStamp(System.currentTimeMillis());
            ZOrganizer org = invCopy.getOrganizer();
            org.setAddress(target.getName());
            org.setCn(target.getAttr(Provisioning.A_displayName));
            // Preserve any SENT-BY on organizer by leaving it alone.
            inv = invCopy;
        }

        // explicitly add the invite metadata here
        ToXML.encodeInvite(m, mIdFormatter, getOpCtxt(), cal, inv, true);
    }
}
