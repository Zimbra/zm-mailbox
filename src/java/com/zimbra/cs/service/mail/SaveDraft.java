/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Jun 11, 2005
 */
package com.zimbra.cs.service.mail;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.AutoSendDraftTask;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * @author dkarp
 */
public class SaveDraft extends MailDocumentHandler {

    private static final String[] TARGET_DRAFT_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_ID };
    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    @Override protected String[] getProxiedIdPath(Element request) {
        return getXPath(request, TARGET_DRAFT_PATH) != null ? TARGET_DRAFT_PATH : TARGET_FOLDER_PATH;
    }
    @Override protected boolean checkMountpointProxy(Element request) {
        return getXPath(request, TARGET_DRAFT_PATH) == null;
    }
    @Override protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

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
        if (!iidFolder.belongsTo(mbox))
            throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
        else if (folderId != null && iidFolder.getId() <= 0)
            throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
        String flags = msgElem.getAttribute(MailConstants.A_FLAGS, null);
        String tags  = msgElem.getAttribute(MailConstants.A_TAGS, null);
        MailItem.Color color = ItemAction.getColor(msgElem);

        // check to see whether the entire message has been uploaded under separate cover
        String attachment = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

        ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
        MimeMessage mm;
        if (attachment != null)
            mm = SendMsg.parseUploadedMessage(zsc, attachment, mimeData);
        else
            mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem, null, mimeData);

        long date = System.currentTimeMillis();
        try {
            Date d = new Date();
            mm.setSentDate(d);
            date = d.getTime();
        } catch (Exception e) { }

        try {
            mm.saveChanges();
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("completing MIME message object", me);
        }

        ParsedMessage pm = new ParsedMessage(mm, date, mbox.attachmentsIndexingEnabled());

        long autoSendTime = new Long(msgElem.getAttribute(MailConstants.A_AUTO_SEND_TIME, "0"));

        Message msg;
        try {
            String origid = iidOrigid == null ? null : iidOrigid.toString(account == null ?
                mbox.getAccountId() : account);
            
            msg = mbox.saveDraft(octxt, pm, id, origid, replyType, identity, account, autoSendTime);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while saving draft", e);
        }

        // we can now purge the uploaded attachments
        if (mimeData.uploads != null)
            FileUploadServlet.deleteUploads(mimeData.uploads);

        // try to set the metadata on the new/revised draft
        if (folderId != null || flags != null || tags != null || color != null) {
            try {
                // best not to fail if there's an error here...
                ItemActionHelper.UPDATE(octxt, mbox, zsc.getResponseProtocol(), Arrays.asList(msg.getId()), MailItem.TYPE_MESSAGE,
                                           null, null, iidFolder, flags, tags, color);
                // and make sure the Message object reflects post-update reality
                msg = mbox.getMessageById(octxt, msg.getId());
            } catch (ServiceException e) {
                ZimbraLog.soap.warn("error setting metadata for draft " + msg.getId() + "; skipping that operation", e);
            }
        }

        if (id != Mailbox.ID_AUTO_INCREMENT) {
            // Cancel any existing auto-send task for this draft
            AutoSendDraftTask.cancelTask(id, mbox.getId());
        }
        if (autoSendTime != 0) {
            // schedule a new auto-send-draft task
            AutoSendDraftTask.scheduleTask(msg.getId(), mbox.getId(), autoSendTime);
        }

        Element response = zsc.createElement(MailConstants.SAVE_DRAFT_RESPONSE);
        // FIXME: inefficient -- this recalculates the MimeMessage (but SaveDraft is called rarely)
        ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, -1, true, true, null, true);
        return response;
    }
}
