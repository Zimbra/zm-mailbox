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
package com.zimbra.cs.service.formatter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.mime.MimeConstants;

public class SyncFormatter extends Formatter {

    public static final String QP_NOHDR = "nohdr";

    @Override
    public String getType() {
        return "sync";
    }

    @Override
    public Set<MailItem.Type> getDefaultSearchTypes() {
        // TODO: all?
        return EnumSet.of(MailItem.Type.MESSAGE);
    }

    /**
     * add to content as well as http headers for now (unless told not to)...
     */
    private static List<Pair<String, String>> getXZimbraHeaders(MailItem item) {
        List<Pair<String, String>> hdrs = new ArrayList<Pair<String, String>>();
        hdrs.add(new Pair<String, String>("X-Zimbra-ItemId", item.getId() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-FolderId", item.getFolderId() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Tags", item.getTagString()));
        hdrs.add(new Pair<String, String>("X-Zimbra-Flags", item.getFlagString()));
        hdrs.add(new Pair<String, String>("X-Zimbra-Received", item.getDate() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Modified", item.getChangeDate() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Change", item.getModifiedSequence() + ""));
        hdrs.add(new Pair<String, String>("X-Zimbra-Revision", item.getSavedSequence() + ""));
        if (item instanceof Message)
            hdrs.add(new Pair<String, String>("X-Zimbra-Conv", ((Message) item).getConversationId() + ""));
        return hdrs;
    }

    private static byte[] getXZimbraHeadersBytes(List<Pair<String, String>> hdrs) {
        StringBuilder sb = new StringBuilder();
        for (Pair<String, String> pair : hdrs)
            sb.append(pair.getFirst()).append(": ").append(pair.getSecond()).append("\r\n");
        return sb.toString().getBytes();
    }

    public static byte[] getXZimbraHeadersBytes(MailItem item) {
        return getXZimbraHeadersBytes(getXZimbraHeaders(item));
    }

    private static void addXZimbraHeaders(Context context, MailItem item, long size) throws IOException {
        List<Pair<String, String>> hdrs = getXZimbraHeaders(item);
        for (Pair<String, String> pair : hdrs)
            context.resp.addHeader(pair.getFirst(), pair.getSecond());

        // inline X-Zimbra headers with response body if nohdr parameter is not present
        // also explicitly set the Content-Length header, as it's only done implicitly for short payloads
        if (context.params.get(QP_NOHDR) == null) {
            byte[] inline = getXZimbraHeadersBytes(hdrs);
            if (size > 0)
                context.resp.setContentLength(inline.length + (int) size);
            context.resp.getOutputStream().write(inline);
        } else if (size > 0) {
            context.resp.setContentLength((int) size);
        }
    }

    @Override
    public void formatCallback(Context context) throws IOException, ServiceException, UserServletException {
        try {
            if (context.hasPart()) {
                handleMessagePart(context, context.target);
            } else if (context.target instanceof Message) {
                handleMessage(context, (Message) context.target);
            } else if (context.target instanceof CalendarItem) {
                // Don't return private appointments/tasks if the requester is not the mailbox owner.
                CalendarItem calItem = (CalendarItem) context.target;
                if (calItem.isPublic() || calItem.allowPrivateAccess(context.authAccount, context.isUsingAdminPrivileges()))
                    handleCalendarItem(context, calItem);
                else
                    context.resp.sendError(HttpServletResponse.SC_FORBIDDEN, "permission denied");
            }
        } catch (MessagingException me) {
            throw ServiceException.FAILURE(me.getMessage(), me);
        }
    }

    private void handleCalendarItem(Context context, CalendarItem calItem) throws IOException, ServiceException, MessagingException {
        context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
        if (context.itemId.hasSubpart()) {
            Pair<MimeMessage,Integer> calItemMsgData = calItem.getSubpartMessageData(context.itemId.getSubpartId());
            if (calItemMsgData != null) {
                addXZimbraHeaders(context, calItem, calItemMsgData.getSecond());
                calItemMsgData.getFirst().writeTo(context.resp.getOutputStream());
            } else {
                // Backward compatibility for pre-5.0.16 ZCO/ZCB: Build a MIME message on the fly.
                // Let's first make sure the requested invite id is valid.
                int invId = context.itemId.getSubpartId();
                Invite[] invs = calItem.getInvites(invId);
                if (invs != null && invs.length > 0) {
                    Invite invite = invs[0];
                    MimeMessage mm = CalendarMailSender.createCalendarMessage(invite);
                    if (mm != null) {
                        // Go through ByteArrayInput/OutputStream to calculate the exact size in bytes.
                        int sizeHint = mm.getSize();
                        if (sizeHint < 0) sizeHint = 0;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(sizeHint);
                        mm.writeTo(baos);
                        byte[] bytes = baos.toByteArray();
                        addXZimbraHeaders(context, calItem, bytes.length);
                        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                        ByteUtil.copy(bais, true, context.resp.getOutputStream(), false);
                    }
                }
            }
        } else {
            addXZimbraHeaders(context, calItem, calItem.getSize());
            InputStream is = calItem.getRawMessage();
            if (is != null)
                ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }

    private void handleMessage(Context context, Message msg) throws IOException, ServiceException {
        context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
        InputStream is = msg.getContentStream();
        long size = msg.getSize();

        if (!context.shouldReturnBody()) {
            byte[] headers = HeadersOnlyInputStream.getHeaders(is);
            is = new ByteArrayInputStream(headers);
            size = headers.length;
        }
        addXZimbraHeaders(context, msg, size);
        ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
    }

    private void handleMessagePart(Context context, MailItem item) throws IOException, ServiceException, MessagingException, UserServletException {
        if (!(item instanceof Message))
            throw UserServletException.notImplemented("can only handle messages");
        Message message = (Message) item;

        MimePart mp = getMimePart(message, context.getPart());
        if (mp != null) {
            String contentType = mp.getContentType();
            if (contentType == null)
                contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            sendbackOriginalDoc(mp, contentType, context.req, context.resp);
            return;
        }
        context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "part not found");
    }

    public static MimePart getMimePart(CalendarItem calItem, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(calItem.getMimeMessage(), part);
    }

    public static MimePart getMimePart(Message msg, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(msg.getMimeMessage(), part);
    }

    public static void sendbackOriginalDoc(MimePart mp, String contentType, HttpServletRequest req, HttpServletResponse resp) throws IOException, MessagingException {
        String filename = Mime.getFilename(mp);
        if (filename == null)
            filename = "unknown";
        String cd = Part.INLINE + "; filename=" + HttpUtil.encodeFilename(req, filename);
        resp.addHeader("Content-Disposition", cd);
        String desc = mp.getDescription();
        if (desc != null)
            resp.addHeader("Content-Description", desc);
        sendbackOriginalDoc(mp.getInputStream(), contentType, resp);
    }

    public static void sendbackOriginalDoc(InputStream is, String contentType, HttpServletResponse resp) throws IOException {
        resp.setContentType(contentType);
        ByteUtil.copy(is, true, resp.getOutputStream(), false);
    }

    /**
     * This formatter downloads attachments as part of the mime blob so it can't be blocked. See bug 5310 for more details.
     */
    @Override
    public boolean canBeBlocked() {
        return false;
    }

    @Override
    public boolean supportsSave() {
        return true;
    }

    // FIXME: need to support tags, flags, date, etc...
    @Override
    public void saveCallback(Context context, String contentType, Folder folder, String filename)
            throws IOException, ServiceException, UserServletException {
        byte[] body = context.getPostBody();
        try {
            Mailbox mbox = folder.getMailbox();
            ParsedMessage pm = new ParsedMessage(body, mbox.attachmentsIndexingEnabled());
            mbox.addMessage(context.opContext, pm, folder.getId(), true, 0, null);
        } catch (ServiceException e) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "error parsing message");
        }
    }
}

