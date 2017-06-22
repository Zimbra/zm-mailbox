/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.AutoSendDraftTask;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.MsgContent;

/**
 * @since Jun 11, 2005
 * @author dkarp
 */
public class SaveDraft extends MailDocumentHandler {

    private static final String[] TARGET_DRAFT_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_ID };
    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return getXPath(request, TARGET_DRAFT_PATH) != null ? TARGET_DRAFT_PATH : TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return getXPath(request, TARGET_DRAFT_PATH) == null;
    }

    @Override
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        boolean wantImapUid = request.getAttributeBool(MailConstants.A_WANT_IMAP_UID, false);
        boolean wantModSeq = request.getAttributeBool(MailConstants.A_WANT_MODIFIED_SEQUENCE, false);
        Element msgElem = request.getElement(MailConstants.E_MSG);

        int id = (int) msgElem.getAttributeLong(MailConstants.A_ID, Mailbox.ID_AUTO_INCREMENT);
        String originalId = msgElem.getAttribute(MailConstants.A_ORIG_ID, null);
        ItemId iidOrigid = originalId == null ? null : new ItemId(originalId, zsc);
        String replyType = msgElem.getAttribute(MailConstants.A_REPLY_TYPE, null);
        String identity = msgElem.getAttribute(MailConstants.A_IDENTITY_ID, null);
        String account = msgElem.getAttribute(MailConstants.A_FOR_ACCOUNT, null);

        // allow the caller to update the draft's metadata at the same time as they save the draft
        String folderId = msgElem.getAttribute(MailConstants.A_FOLDER, null);
        ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
        if (!iidFolder.belongsTo(mbox)) {
            throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
        } else if (folderId != null && iidFolder.getId() <= 0) {
            throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
        }
        String flags = msgElem.getAttribute(MailConstants.A_FLAGS, null);
        String[] tags = TagUtil.parseTags(msgElem, mbox, octxt);
        Color color = ItemAction.getColor(msgElem);

        // check to see whether the entire message has been uploaded under separate cover
        String attachment = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        long autoSendTime = new Long(msgElem.getAttribute(MailConstants.A_AUTO_SEND_TIME, "0"));

        ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
        Message msg;
        try {
            MimeMessage mm;
            if (attachment != null) {
                mm = SendMsg.parseUploadedMessage(zsc, attachment, mimeData);
            } else {
                mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem, null, mimeData, true);
            }

            long date = System.currentTimeMillis();
            try {
                Date d = new Date();
                mm.setSentDate(d);
                date = d.getTime();
            } catch (Exception ignored) { }

            try {
                mm.saveChanges();
            } catch (MessagingException me) {
                throw ServiceException.FAILURE("completing MIME message object", me);
            }

            ParsedMessage pm = new ParsedMessage(mm, date, mbox.attachmentsIndexingEnabled());

            if (autoSendTime != 0) {
                AccountUtil.checkQuotaWhenSendMail(mbox);
            }

            String origid = iidOrigid == null ? null : iidOrigid.toString(account == null ? mbox.getAccountId() : account);

            msg = mbox.saveDraft(octxt, pm, id, origid, replyType, identity, account, autoSendTime);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while saving draft", e);
        } finally {
            // purge the messages fetched from other servers.
            if (mimeData.fetches != null) {
                FileUploadServlet.deleteUploads(mimeData.fetches);
            }
        }

        // we can now purge the uploaded attachments
        if (mimeData.uploads != null) {
            FileUploadServlet.deleteUploads(mimeData.uploads);
        }

        // try to set the metadata on the new/revised draft
        if (folderId != null || flags != null || !ArrayUtil.isEmpty(tags) || color != null) {
            try {
                // best not to fail if there's an error here...
                ItemActionHelper.UPDATE(octxt, mbox, zsc.getResponseProtocol(), Arrays.asList(msg.getId()),
                        MailItem.Type.MESSAGE, null, null, iidFolder, flags, tags, color);
                // and make sure the Message object reflects post-update reality
                msg = mbox.getMessageById(octxt, msg.getId());
            } catch (ServiceException e) {
                ZimbraLog.soap.warn("error setting metadata for draft " + msg.getId() + "; skipping that operation", e);
            }
        }

        if (schedulesAutoSendTask()) {
            if (id != Mailbox.ID_AUTO_INCREMENT) {
                // Cancel any existing auto-send task for this draft
                AutoSendDraftTask.cancelTask(id, mbox.getId());
            }
            if (autoSendTime != 0) {
                // schedule a new auto-send-draft task
                AutoSendDraftTask.scheduleTask(msg.getId(), mbox.getId(), autoSendTime);
            }
        }

        return generateResponse(zsc, ifmt, octxt, mbox, msg, wantImapUid, wantModSeq);
    }

    protected boolean schedulesAutoSendTask() {
        return true;
    }

    protected Element generateResponse(ZimbraSoapContext zsc, ItemIdFormatter ifmt, OperationContext octxt,
            Mailbox mbox, Message msg, boolean wantImapUid, boolean wantModSeq) {
        Element response = zsc.createElement(MailConstants.SAVE_DRAFT_RESPONSE);
        try {
            int fields = ToXML.NOTIFY_FIELDS;
            if (wantImapUid) {
                fields |= Change.IMAP_UID;
            }
            if (wantModSeq) {
                fields |= Change.MODSEQ;
            }
            ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, -1, true, true, null, true, false, false,
                    MsgContent.full, fields);
        } catch (NoSuchItemException nsie) {
            ZimbraLog.soap.info("draft was deleted while serializing response; omitting <m> from response");
        } catch (ServiceException e) {
            ZimbraLog.soap.warn("problem serializing draft structure to response", e);
        }
        return response;
    }
}
