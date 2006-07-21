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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.HttpUtil;

public class SyncFormatter extends Formatter {
    
    public static class Format {};
    public static class Save {};
    static int sFormatLoad = Operation.setLoad(SyncFormatter.Format.class, 10);
    static int sSaveLoad = Operation.setLoad(SyncFormatter.Save.class, 10);
    int getFormatLoad() { return  sFormatLoad; }
    int getSaveLoad() { return sSaveLoad; }

    public String getType() {
        return "sync";
    }

    public String getDefaultSearchTypes() {
        // TODO: all?
        return MailboxIndex.SEARCH_FOR_MESSAGES;
    }

    /**
     * add to http headers as well as content for now... 
     */
    private static void addHeader(Context context, StringBuffer sb, String name, String value) {
        sb.append(name).append(": ").append(value).append("\r\n");
        context.resp.addHeader(name, value);
    }
    
    private static void addXZimbraHeaders(Context context, MailItem item, long size) throws IOException {
        StringBuffer hdr = new StringBuffer();
        addHeader(context, hdr, "X-Zimbra-Tags", item.getTagString());
        addHeader(context, hdr, "X-Zimbra-Flags", item.getFlagString());
        addHeader(context, hdr, "X-Zimbra-Received", item.getDate() + "");
        addHeader(context, hdr, "X-Zimbra-Modified", item.getChangeDate() + "");
        if (item instanceof Message)
            addHeader(context, hdr, "X-Zimbra-Conv", ((Message) item).getConversationId() + "");
        byte[] inline = hdr.toString().getBytes();

        // explicitly set the Content-Length header, as it's only done implicitly for short payloads
        if (size > 0)
            context.resp.setContentLength(inline.length + (int) size);

        // inline X-Zimbra headers with response body
        context.resp.getOutputStream().write(inline);
    }

    public void formatCallback(Context context, MailItem item) throws IOException, ServiceException, UserServletException {
        try {
            if (context.hasPart()) {
                handleMessagePart(context, item);
            } else if (item instanceof Message) {
                handleMessage(context, (Message) item);
            } else if (item instanceof Appointment) {
                handleAppointment(context, (Appointment) item);                
            }
        } catch (MessagingException me) {
            throw ServiceException.FAILURE(me.getMessage(), me);
        }
    }

    private void handleAppointment(Context context, Appointment appt) throws IOException, ServiceException, MessagingException {
        context.resp.setContentType(Mime.CT_TEXT_PLAIN);
        if (context.itemId.hasSubpart()) {
            MimeMessage mm = appt.getMimeMessage(context.itemId.getSubpartId());
            addXZimbraHeaders(context, appt, mm.getSize());
            mm.writeTo(context.resp.getOutputStream());
        } else { 
            InputStream is = appt.getRawMessage();
            addXZimbraHeaders(context, appt, appt.getSize());
            ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }        
    }

    private void handleMessage(Context context, Message msg) throws IOException, ServiceException {
        context.resp.setContentType(Mime.CT_TEXT_PLAIN);
        InputStream is = msg.getRawMessage();
        addXZimbraHeaders(context, msg, msg.getSize());
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
                contentType = Mime.CT_APPLICATION_OCTET_STREAM;
            sendbackOriginalDoc(mp, contentType, context.req, context.resp);
            return;
        }
        context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "part not found");
    }

    public static MimePart getMimePart(Appointment appt, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(appt.getMimeMessage(), part);
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

    public boolean canBeBlocked() {
        return true;
    }

    // FIXME: need to support tags, flags, date, etc...
    public void saveCallback(byte[] body, Context context, Folder folder) throws IOException, ServiceException, UserServletException {
        try {
            Mailbox mbox = folder.getMailbox();
            ParsedMessage pm = new ParsedMessage(body, mbox.attachmentsIndexingEnabled());
            mbox.addMessage(context.opContext, pm, folder.getId(), true, 0, null);
        } catch (MessagingException e) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "error parsing message");
        }
    }
}

