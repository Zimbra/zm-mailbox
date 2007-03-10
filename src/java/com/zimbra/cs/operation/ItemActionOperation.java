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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.ZimbraSoapContext;

public class ItemActionOperation extends Operation {

    // each of these is "per operation" -- real load is calculated as LOAD * local.size()
    private static final int DELETE_MULT = 3;
    private static final int SPAM_MULT = 4;

    protected static int LOAD = 3;
    private static int MAX = 20;
    private static int SCALE = 10;
    static {
        Operation.Config c = loadConfig(ItemActionOperation.class);
        if (c != null) {
            LOAD = c.mLoad;
            if (c.mScale > 0)
                SCALE = c.mScale;
            if (c.mMaxLoad > 0)
                MAX = c.mMaxLoad;
        }
    }
    
    public static ItemActionOperation TAG(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                boolean flagValue, TargetConstraint tcon, int tagId)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.TAG, type, flagValue, tcon);
        ia.setTagId(tagId);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionOperation FLAG(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                boolean flagValue, TargetConstraint tcon)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.FLAG, type, flagValue, tcon);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionOperation READ(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                boolean flagValue, TargetConstraint tcon)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.READ, type, flagValue, tcon);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionOperation COLOR(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                TargetConstraint tcon, byte color)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.COLOR, type, true, tcon);
        ia.setColor(color);
        ia.schedule();
        return ia;
    }

    public static ItemActionOperation HARD_DELETE(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                TargetConstraint tcon)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    DELETE_MULT * LOAD, ids, Op.HARD_DELETE, type, true, tcon);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionOperation MOVE(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                TargetConstraint tcon, ItemId iidFolder)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.MOVE, type, true, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionOperation COPY(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                TargetConstraint tcon, ItemId iidFolder)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.COPY, type, true, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionOperation SPAM(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                boolean flagValue, TargetConstraint tcon, int folderId)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    SPAM_MULT * LOAD, ids, Op.SPAM, type, flagValue, tcon);
        ia.setFolderId(folderId);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionOperation TRASH(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, 
                List<Integer> ids, byte type, 
                TargetConstraint tcon) throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.TRASH, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionOperation RENAME(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                TargetConstraint tcon, String name, ItemId iidFolder)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.RENAME, type, true, tcon);
        ia.setName(name);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }
    
    public static ItemActionOperation UPDATE(ZimbraSoapContext zc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, List<Integer> ids, byte type, 
                TargetConstraint tcon, String name, ItemId iidFolder,
                String flags, String tags, byte color)
    throws ServiceException {
        ItemActionOperation ia = new ItemActionOperation(zc, session, oc, mbox, req, 
                    LOAD, ids, Op.UPDATE, type, true, tcon);
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

    // only when OP=MOVE or OP=RENAME or OP=UPDATE
    protected ItemId mIidFolder; 
    
    // only when OP=SPAM
    protected int mFolderId;

    // only when OP=UPDATE
    protected String mFlags;
    protected String mTags;
    
    // TEMPORARY -- just until dan implements the ItemIdFormatter
    protected ZimbraSoapContext mSoapContext;
    
    
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
        
        if (mOperation == Op.MOVE || mOperation == Op.UPDATE) 
            toRet.append(" iidFolder=").append(mIidFolder);
        
        if (mOperation == Op.SPAM) 
            toRet.append(" folderId=").append(mFolderId);
        
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
        assert(mOperation == Op.MOVE || mOperation == Op.RENAME || mOperation == Op.UPDATE);
        mIidFolder = iidFolder; 
    }
    public void setFolderId(int folderId) {
        assert(mOperation == Op.SPAM);
        mFolderId = folderId; 
    }
    public void setFlags(String flags) {
        assert(mOperation == Op.UPDATE);
        mFlags = flags; 
    }
    public void setTags(String tags) {                        
        assert(mOperation == Op.UPDATE);
        mTags = tags; 
    }
    
    ItemActionOperation(ZimbraSoapContext zsc, Session session, OperationContext oc,
                Mailbox mbox, Requester req, int baseLoad, 
                List<Integer> ids, Op op, byte type, 
                boolean flagValue, TargetConstraint tcon)
    throws ServiceException {
        super(session, oc, mbox, req, req.getPriority(), Math.min(ids.size() > 0 ? ids.size() * (baseLoad / SCALE): baseLoad, MAX));
        mSoapContext = zsc;

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

    protected void callback() throws ServiceException {
        boolean movable = mOperation == Op.MOVE || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE;

        // deal with local mountpoints pointing at local folders here
        if (movable && mIidFolder.belongsTo(mMailbox) && mIidFolder.getId() > 0) {
            Folder folder = mMailbox.getFolderById(mOpCtxt, mIidFolder.getId());
            if (folder instanceof Mountpoint && ((Mountpoint) folder).getOwnerId().equals(mIidFolder.getAccountId()))
                mIidFolder = new ItemId(((Mountpoint) folder).getOwnerId(), ((Mountpoint) folder).getRemoteId());
        }

        try {
            if (movable && !mIidFolder.belongsTo(mMailbox))
                executeRemote();
            else
                executeLocal();
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("exception reading item blob", ioe);
        }

        StringBuilder successes = new StringBuilder();
        ItemIdFormatter ifmt = new ItemIdFormatter(mSoapContext);
        for (int id : mIds)
            successes.append(successes.length() > 0 ? "," : "").append(ifmt.formatItemId(id));
        mResult = successes.toString();
    }

    public String getResult() {
        return mResult;
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
            case MOVE:
                getMailbox().move(getOpCtxt(), mIds, mItemType, mIidFolder.getId(), mTargetConstraint);
                break;
            case COPY:
                getMailbox().copy(getOpCtxt(), mIds, mItemType, mIidFolder.getId());
                break;
            case SPAM:
                getMailbox().move(getOpCtxt(), mIds, mItemType, mFolderId, mTargetConstraint);
                for (int id : mIds)
                    SpamHandler.getInstance().handle(getMailbox(), id, mItemType, mFlagValue);
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
        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mIidFolder.getAccountId());
        ZMailbox.Options zoptions = new ZMailbox.Options(mSoapContext.getRawAuthToken(), AccountUtil.getSoapUri(target));
        zoptions.setNoSession(true);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);

        boolean deleteOriginal = mOperation != Op.COPY;
        String folderStr = mIidFolder.toString();

        for (MailItem item : mMailbox.getItemById(mOpCtxt, mIds, mItemType)) {
            if (item == null)
                continue;
            if (deleteOriginal && (mMailbox.getEffectivePermissions(mOpCtxt, item.getId(), item.getType()) & ACL.RIGHT_DELETE) == 0)
                throw ServiceException.PERM_DENIED("cannot delete existing copy of " + MailItem.getNameForType(item) + " " + item.getId());

            // since we can't apply tags to a remote object, hardwiring "tags" to null below...
            String flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : item.getFlagString());
            String name = ((mOperation == Op.RENAME || mOperation == Op.UPDATE) && mName != null ? mName : item.getName());

            switch (item.getType()) {
                case MailItem.TYPE_CONTACT:
                    zmbx.createContact(folderStr, null, ((Contact) item).getFields());
                    break;

                case MailItem.TYPE_VIRTUAL_CONVERSATION:
                    item = mMailbox.getMessageById(mOpCtxt, ((VirtualConversation) item).getMessageId());
                    // fall through...

                case MailItem.TYPE_MESSAGE:
                    zmbx.addMessage(folderStr, flags, null, item.getDate(), ((Message) item).getMessageContent(), true);
                    break;

                case MailItem.TYPE_CONVERSATION:
                    for (Message msg : mMailbox.getMessagesByConversation(mOpCtxt, item.getId(), Conversation.SORT_DATE_ASCENDING)) {
                        if (!TargetConstraint.checkItem(mTargetConstraint, msg))
                            continue;
                        flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : msg.getFlagString());
                        zmbx.addMessage(folderStr, flags, null, msg.getDate(), msg.getMessageContent(), true);
                    }
                    break;

                case MailItem.TYPE_DOCUMENT:
                    Document doc = (Document) item;
                    byte[] content = ByteUtil.getContent(doc.getRawDocument(), doc.getSize());
                    String uploadId = zmbx.uploadAttachment(name, content, doc.getContentType(), 4000);
                    zmbx.createDocument(folderStr, name, uploadId);
                    break;

                case MailItem.TYPE_WIKI:
                    zmbx.createWiki(folderStr, name, new String(((WikiItem) item).getMessageContent(), "utf-8"));
                    break;

                case MailItem.TYPE_APPOINTMENT:
                case MailItem.TYPE_TASK:
                    QName qname = (item.getType() == MailItem.TYPE_TASK ? MailConstants.SET_TASK_REQUEST : MailConstants.SET_APPOINTMENT_REQUEST);
                    Element request = new Element.XMLElement(qname).addAttribute(MailConstants.A_FOLDER, folderStr).addAttribute(MailConstants.A_FLAGS, flags);
                    CalendarItem cal = (CalendarItem) item;

                    Invite invDefault = cal.getDefaultInviteOrNull();
                    if (invDefault == null)
                        throw ServiceException.FAILURE("cannot copy: no default invite for " + MailItem.getNameForType(item) + " " + item.getId(), null);
                    addCalendarPart(request.addUniqueElement(MailConstants.A_DEFAULT), cal, invDefault, zmbx, target);

                    for (Invite inv : cal.getInvites()) {
                        if (inv == null || inv == invDefault)
                            continue;
                        else if (inv.isCancel())
                            addCalendarPart(request.addUniqueElement(MailConstants.E_CAL_CANCEL), cal, inv, zmbx, target);
                        else
                            addCalendarPart(request.addUniqueElement(MailConstants.E_CAL_EXCEPT), cal, inv, zmbx, target);
                    }

                    zmbx.invoke(request);
                    break;

                default:
                    throw MailServiceException.CANNOT_COPY(item.getId());
            }

            try {
                if (deleteOriginal)
                    mMailbox.delete(mOpCtxt, item.getId(), item.getType());
            } catch (ServiceException e) {
                if (e.getCode() != ServiceException.PERM_DENIED)
                    throw e;
                // something funky happened permissions-wise between the getEffectivePermissions check and here...
                ZimbraLog.misc.info("could not delete original item " + item.getId() + "; treating operation as a copy instead");
            }
        }
    }

    private Element addCalendarPart(Element parent, CalendarItem cal, Invite inv, ZMailbox zmbx, Account target) throws ServiceException {
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

        // explicitly add the invite metadata here
        ToXML.encodeInvite(m, new ItemIdFormatter(mSoapContext), cal, inv);

        // fix up the Organizer if needed
        for (Element comp : m.getElement(MailConstants.E_INVITE).listElements(MailConstants.E_INVITE_COMPONENT)) {
            if (comp.getAttributeBool(MailConstants.A_CAL_ISORG, false) && comp.getOptionalElement(MailConstants.E_CAL_ORGANIZER) != null) {
                comp.getOptionalElement(MailConstants.E_CAL_ORGANIZER).detach();
                comp.addUniqueElement(MailConstants.E_CAL_ORGANIZER).addAttribute(MailConstants.A_ADDRESS, target.getName())
                                                                    .addAttribute(MailConstants.A_DISPLAY, target.getAttr(Provisioning.A_displayName));
            }
        }

        return m;
    }
}
