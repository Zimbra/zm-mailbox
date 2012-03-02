/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since Sep 29, 2004
 */
public class AddMsg extends MailDocumentHandler {
    private static Log mLog = LogFactory.getLog(AddMsg.class);
    private static Pattern sNumeric = Pattern.compile("\\d+");

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        String folder = getXPath(request, TARGET_FOLDER_PATH);
        return folder != null && (folder.indexOf(':') > 0 || sNumeric.matcher(folder).matches()) ? TARGET_FOLDER_PATH : null;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element msgElem = request.getElement(MailConstants.E_MSG);

        String flagsStr = msgElem.getAttribute(MailConstants.A_FLAGS, null);
        String[] tags = TagUtil.parseTags(msgElem, mbox, octxt);
        String folderStr = msgElem.getAttribute(MailConstants.A_FOLDER);
        boolean noICal = msgElem.getAttributeBool(MailConstants.A_NO_ICAL, false);
        long date = msgElem.getAttributeLong(MailConstants.A_DATE, System.currentTimeMillis());
        boolean filterSent = request.getAttributeBool(MailConstants.A_FILTER_SENT, false);

        if (mLog.isDebugEnabled()) {
            StringBuffer toPrint = new StringBuffer("<AddMsg ");
            if (tags != null) {
                toPrint.append(" tags=\"").append(TagUtil.encodeTags(tags)).append("\"");
            }
            if (folderStr != null) {
                toPrint.append(" folder=\"").append(folderStr).append("\"");
            }
            toPrint.append(">");
            mLog.debug(toPrint);
        }

        int folderId = -1;
        Folder folder = null;
        try {
            folderId = new ItemId(folderStr, zsc).getId();
        } catch (ServiceException e) { }
        if (folderId > 0) {
            folder = mbox.getFolderById(octxt, folderId);
        } else {
            folder = mbox.getFolderByPath(octxt, folderStr);
            folderId = folder.getId();
        }
        mLog.debug("folder = %s", folder.getName());

        // check to see whether the entire message has been uploaded under separate cover
        String attachment = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

        ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
        Message msg;
        try {
            MimeMessage mm;
            if (attachment != null) {
                mm = SendMsg.parseUploadedMessage(zsc, attachment, mimeData);
            } else {
                mm = ParseMimeMessage.importMsgSoap(msgElem);
            }

            int flagsBitMask = Flag.toBitmask(flagsStr);

            ParsedMessage pm = new ParsedMessage(mm, date, mbox.attachmentsIndexingEnabled());
            if (filterSent && !DebugConfig.disableOutgoingFilter && folderId == MailSender.getSentFolderId(mbox) &&
                    (flagsBitMask & Flag.BITMASK_FROM_ME) != 0) {
                List<ItemId> addedItemIds =
                        RuleManager.applyRulesToOutgoingMessage(
                            octxt, mbox, pm, folderId, noICal, flagsBitMask, tags, Mailbox.ID_AUTO_INCREMENT);
                msg = addedItemIds.isEmpty() ? null : mbox.getMessageById(octxt, addedItemIds.get(0).getId());
            } else {
                DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setNoICal(noICal).setFlags(flagsBitMask).setTags(tags);
                msg = mbox.addMessage(octxt, pm, dopt, null);
            }
        } catch(IOException ioe) {
            throw ServiceException.FAILURE("Error While Delivering Message", ioe);
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

        Element response = zsc.createElement(MailConstants.ADD_MSG_RESPONSE);
        if (msg != null) {
            ToXML.encodeMessageSummary(response, ifmt, octxt, msg, null, GetMsgMetadata.SUMMARY_FIELDS);
        }

        return response;
    }
}
