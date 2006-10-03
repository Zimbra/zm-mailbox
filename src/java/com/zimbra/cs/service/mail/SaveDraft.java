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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 11, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.operation.ItemActionOperation;
import com.zimbra.cs.operation.SaveDraftOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class SaveDraft extends MailDocumentHandler {

    private static final String[] TARGET_DRAFT_PATH = new String[] { MailService.E_MSG, MailService.A_ID };
    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_MSG, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request) {
        return getXPath(request, TARGET_DRAFT_PATH) != null ? TARGET_DRAFT_PATH : TARGET_FOLDER_PATH;
    }
    protected boolean checkMountpointProxy(Element request) {
        return getXPath(request, TARGET_DRAFT_PATH) == null;
    }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = zsc.getOperationContext();
        Session session = getSession(context);

        Element msgElem = request.getElement(MailService.E_MSG);

        int id = (int) msgElem.getAttributeLong(MailService.A_ID, Mailbox.ID_AUTO_INCREMENT);
        int origId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        String replyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, null);

        // allow the caller to update the draft's metadata at the same time as they save the draft
        String folderId = msgElem.getAttribute(MailService.A_FOLDER, null);
        ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
        if (!iidFolder.belongsTo(mbox))
            throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
        else if (folderId != null && iidFolder.getId() <= 0)
            throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
        String flags = msgElem.getAttribute(MailService.A_FLAGS, null);
        String tags  = msgElem.getAttribute(MailService.A_TAGS, null);
        byte color = (byte) msgElem.getAttributeLong(MailService.A_COLOR, -1);

        // check to see whether the entire message has been uploaded under separate cover
        String attachment = msgElem.getAttribute(MailService.A_ATTACHMENT_ID, null);

        ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
        MimeMessage mm;
        if (attachment != null)
            mm = SendMsg.parseUploadedMessage(zsc, attachment, mimeData);
        else
            mm = ParseMimeMessage.parseMimeMsgSoap(zsc, mbox, msgElem, null, mimeData);

        long date = System.currentTimeMillis();
        try {
            mm.setFrom(AccountUtil.getFriendlyEmailAddress(acct));
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

        SaveDraftOperation op = new SaveDraftOperation(session, octxt, mbox, Requester.SOAP,
                    pm, id, origId, replyType);
        op.schedule();
        Message msg = op.getMsg();

        // we can now purge the uploaded attachments
        if (mimeData.uploads != null)
            FileUploadServlet.deleteUploads(mimeData.uploads);

        // try to set the metadata on the new/revised draft
        if (folderId != null || flags != null || tags != null || color >= 0) {
            try {
                // best not to fail if there's an error here...
                ItemActionOperation.UPDATE(zsc, session, octxt, mbox, Requester.SOAP, Arrays.asList(msg.getId()),
                                           MailItem.TYPE_MESSAGE, null, iidFolder, flags, tags, color);
                // and make sure the Message object reflects post-update reality
                msg = mbox.getMessageById(octxt, msg.getId());
            } catch (ServiceException e) {
                ZimbraLog.soap.warn("error setting metadata for draft " + msg.getId() + "; skipping that operation", e);
            }
        }

        Element response = zsc.createElement(MailService.SAVE_DRAFT_RESPONSE);
        // FIXME: inefficient -- this recalculates the MimeMessage (but SaveDraft is called rarely)
        ToXML.encodeMessageAsMP(response, zsc, msg, null, false, true);
        return response;
    }
}
