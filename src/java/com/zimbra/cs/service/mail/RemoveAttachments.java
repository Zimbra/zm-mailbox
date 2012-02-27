/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobInputStream;
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
            Blob blob = msg.getBlob().getLocalBlob();
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), is = new BlobInputStream(blob));
            for (String part : parts)
                stripPart(mm, part);
            mm.saveChanges();

            ParsedMessage pm = new ParsedMessage(mm, msg.getDate(), mbox.attachmentsIndexingEnabled());
            if (msg.isDraft()) {
                msg = mbox.saveDraft(octxt, pm, msg.getId());
            } else {
                DeliveryOptions dopt = new DeliveryOptions();
                dopt.setFolderId(msg.getFolderId()).setNoICal(true);
                dopt.setFlags(msg.getFlagBitmask()).setTags(msg.getTags());
                if (msg.getConversationId() > 0)
                    dopt.setConversationId(msg.getConversationId());
                // FIXME: copy custom metadata to new item
                msg = mbox.addMessage(octxt, pm, dopt, null);
                // and clean up the existing message...
                mbox.delete(octxt, iid.getId(), MailItem.Type.MESSAGE);
            }
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error reading existing message blob", ioe);
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("error reading existing message blob", me);
        } finally {
            ByteUtil.closeStream(is);
        }

        Element response = zsc.createElement(MailConstants.REMOVE_ATTACHMENTS_RESPONSE);
        // FIXME: inefficient -- this recalculates the MimeMessage (but RemoveAttachments is called rarely)
        ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, -1, true, true, null, true, false);
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
}
