/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.zmime.ZSharedFileInputStream;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

public class RemoveAttachments extends MailDocumentHandler {

    private static final String[] TARGET_MSG_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_ID };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    @Override protected String[] getProxiedIdPath(Element request)  { return TARGET_MSG_PATH; }
    @Override protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element msgElem = request.getElement(MailConstants.E_MSG);
        ItemId iid = new ItemId(msgElem.getAttribute(MailConstants.A_ID), zsc);
        Collection<String> parts = sortPartIds(msgElem.getAttribute(MailConstants.A_PART));

        Message msg = mbox.getMessageById(octxt, iid.getId());

        InputStream is = null;
        try {
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), is = StoreManager.getReaderSMInstance(msg.getLocator()).getContent(msg.getBlob().getLocalBlob()));
            // do not allow removing attachments of encrypted/pkcs7-signed messages
            if (Mime.isEncrypted(mm.getContentType()) || Mime.isPKCS7Signed(mm.getContentType())) {
                throw ServiceException.OPERATION_DENIED("not allowed to remove attachments of encrypted/pkcs7-signed message");

            }
            for (String part : parts)
                stripPart(mm, part);
            mm.saveChanges();

            ParsedMessage pm = new ParsedMessage(mm, msg.getDate(), mbox.attachmentsIndexingEnabled());
            msg = removeAttachmentOperation(mbox, octxt, pm, msg);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error reading existing message blob", ioe);
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("error reading existing message blob", me);
        } finally {
            ByteUtil.closeStream(is);
        }

        Element response = zsc.createElement(MailConstants.REMOVE_ATTACHMENTS_RESPONSE);
        // FIXME: inefficient -- this recalculates the MimeMessage (but RemoveAttachments is called rarely)
        ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, -1, true, true, null, true, false, LC.mime_encode_missing_blob.booleanValue());
        return response;
    }

    private static class PartIdComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            // short-circuit simple comparisons
            if (o1.equalsIgnoreCase(o2))  return 0;
            if (o1.equals(""))  return 1;
            if (o2.equals(""))  return -1;

            String[] parts1 = o1.split("\\."), parts2 = o2.split("\\.");
            for (int depth = 0; depth < parts1.length && depth < parts2.length; depth++) {
                String subpart1 = parts1[depth], subpart2 = parts2[depth];
                if (subpart1.equalsIgnoreCase("TEXT") && depth == parts1.length - 1)
                    return 1;
                if (subpart2.equalsIgnoreCase("TEXT") && depth == parts2.length - 1)
                    return -1;

                int delta = Integer.valueOf(subpart2) - Integer.valueOf(subpart1);
                if (delta != 0)
                    return delta;
            }
            // matched up to where one is a prefix of the other
            return parts2.length - parts1.length;
        }
    }

    private static Collection<String> sortPartIds(String partnames) throws ServiceException {
        String[] parts = partnames.split(",");

        if (parts.length == 1 && !parts[0].trim().equals(""))
            return Arrays.asList(parts);
        if (parts.length < 2)
            return Collections.emptyList();

        // using a Set has the nice side-effect of removing duplicates
        Set<String> sorted = new TreeSet<String>(new PartIdComparator());
        for (String part : parts) {
            try {
                if (!part.trim().equals(""))
                    sorted.add(part);
            } catch (NumberFormatException nfe) {
                throw ServiceException.INVALID_REQUEST("invalid part id: " + part, null);
            }
        }
        return sorted;
    }

    private static void stripPart(MimeMessage mm, String part) throws IOException, MessagingException, ServiceException {
        MimePart mp = mm;
        int dot = (part = part.trim()).lastIndexOf('.');
        if (dot > 0)
            mp = Mime.getMimePart(mm, part.substring(0, dot));
        if (mp == null)
            throw MailServiceException.NO_SUCH_PART(part);

        String subpart = dot > 0 ? part.substring(dot + 1) : part;
        if (subpart.equalsIgnoreCase("TEXT")) {
            if (!(mp instanceof MimeMessage))
                throw MailServiceException.NO_SUCH_PART(part);
            mp.setText("");
        } else {
            try {
                int partid = Integer.parseInt(subpart);
                if (Mime.getContentType(mp, MimeConstants.CT_DEFAULT).startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
                    MimeMultipart mmp = Mime.getMultipartContent(mp, mp.getContentType());
                    if (partid <= 0 || partid > mmp.getCount())
                        throw MailServiceException.NO_SUCH_PART(part);
                    mmp.removeBodyPart(partid - 1);
                } else if (mp instanceof MimeMessage && partid == 1) {
                    mp.setText("");
                } else {
                    throw MailServiceException.NO_SUCH_PART(part);
                }
            } catch (NumberFormatException nfe) {
                throw ServiceException.INVALID_REQUEST("invalid part id: " + part, null);
            }
        }
    }

    public static void main(String[] args) throws ServiceException {
        System.out.println(sortPartIds("1"));
        System.out.println(sortPartIds("1,2,3"));
        System.out.println(sortPartIds("1,1,2,1"));
        System.out.println(sortPartIds("1.1,TEXT,1.TEXT,10,2.1"));

        InputStream is = null;
        try {
            java.io.File file = new java.io.File(args[0]);
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), is = new ZSharedFileInputStream(file));
            stripPart(mm, "1.2");
            mm.saveChanges();
            mm.writeTo(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    public static Message removeAttachmentOperation(Mailbox mbox, OperationContext octxt, ParsedMessage pm, Message msg) throws IOException, ServiceException {
        if (msg.isDraft()) {
            msg = mbox.saveDraft(octxt, pm, msg.getId());
        } else {
            int itemId = msg.getId();
            DeliveryOptions dopt = new DeliveryOptions();
            dopt.setFolderId(msg.getFolderId()).setNoICal(true);
            dopt.setFlags(msg.getFlagBitmask()).setTags(msg.getTags());
            if (msg.getConversationId() > 0)
                dopt.setConversationId(msg.getConversationId());
            msg = mbox.addMessage(octxt, pm, dopt, null);
            // Activesync fails to add new mail_item as it has all the details of old mail_item, so it considers it as a change in mail_item Bug:102084
            // Hence, set previous folder field to the mail_item with dummy folder, because of which activesync code thinks it is moved item and adds it on device.
            String prevFolders = msg.getModifiedSequence() + ":" + Mailbox.ID_FOLDER_USER_ROOT;
            mbox.setPreviousFolder(octxt, msg.getId(), prevFolders);
            msg.getUnderlyingData().setPrevFolders(prevFolders);
            // and clean up the existing message...
            mbox.delete(octxt, itemId, MailItem.Type.MESSAGE);
        }
        return msg;
    }
}
