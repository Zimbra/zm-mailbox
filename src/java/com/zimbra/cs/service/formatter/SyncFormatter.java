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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletResponse;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.ByteUtil;

public class SyncFormatter extends Formatter {

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
        sb.append(name).append(": ").append(value).append("\n");
        context.resp.addHeader(name, value);
    }
    
    private static void addXZimbraHeaders(Context context, MailItem mailItem) throws IOException {
        StringBuffer hdr = new StringBuffer();
        addHeader(context, hdr, "X-Zimbra-Tags", mailItem.getTagString());
        addHeader(context, hdr, "X-Zimbra-Flags", mailItem.getFlagString());
        addHeader(context, hdr, "X-Zimbra-Received", mailItem.getDate()+"");
                
        if (mailItem instanceof Appointment) {
            //hdr.append("X-Zimbra-DefaultInvId: ").append(((Appointment)mailItem).getDefaultInvite().getMailItemId()).append("\n");
            addHeader(context, hdr, "X-Zimbra-DefaultInvId", ((Appointment)mailItem).getDefaultInvite().getMailItemId()+"");
        } else if (mailItem instanceof Message) {
            //hdr.append("X-Zimbra-Conv: ").append(((Message)mailItem).getConversationId()).append("\n");
            addHeader(context, hdr, "X-Zimbra-Conv", ((Message)mailItem).getConversationId()+"");
        }
        // dump headers to content
        context.resp.getOutputStream().write(hdr.toString().getBytes());
    }

    public void format(Context context, MailItem mailItem) throws IOException, ServiceException, UserServletException {
        try {
            if (context.hasPart()) {
                handleMessagePart(context, mailItem);
            } else if (mailItem instanceof Message) {
                handleMessage(context, (Message) mailItem);
            } else if (mailItem instanceof Appointment) {
                handleAppointment(context, (Appointment) mailItem);                
            }
        } catch (MessagingException me) {
            throw ServiceException.FAILURE(me.getMessage(), me);
        }
    }

    private void handleAppointment(Context context, Appointment appt) throws IOException, ServiceException, MessagingException {
        addXZimbraHeaders(context, appt);
        context.resp.setContentType(Mime.CT_TEXT_PLAIN);
        if (context.itemId.hasSubpart()) {
            MimeMessage mm = appt.getMimeMessage(context.itemId.getSubpartId());
            mm.writeTo(context.resp.getOutputStream());
        } else { 
            InputStream is = appt.getRawMessage();
            ByteUtil.copy(is, context.resp.getOutputStream());
            is.close();
        }        
    }
    
    private void handleMessage(Context context, Message message) throws IOException, ServiceException, MessagingException {
        addXZimbraHeaders(context, message);
        context.resp.setContentType(Mime.CT_TEXT_PLAIN);
        InputStream is = message.getRawMessage();
        ByteUtil.copy(is, context.resp.getOutputStream());
        is.close();
    }

    private void handleMessagePart(Context context, MailItem mailItem) throws IOException, ServiceException, MessagingException, UserServletException {
        if (!(mailItem instanceof Message))
            throw UserServletException.notImplemented("can only handle messages");
        Message message = (Message) mailItem;
        
        MimePart mp = getMimePart(message, context.getPart());
        if (mp != null) {
            String contentType = mp.getContentType();
            if (contentType == null) {
                contentType = Mime.CT_APPLICATION_OCTET_STREAM;
            }
            sendbackOriginalDoc(mp, contentType, context.resp);
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

    public static void sendbackOriginalDoc(MimePart mp, String contentType, HttpServletResponse resp) throws IOException, MessagingException {
        ContentDisposition cd = new ContentDisposition(Part.INLINE);
        String filename = mp.getFileName();
        if (filename == null)
            filename = "unknown";
        cd.setParameter("filename", filename);
        resp.addHeader("Content-Disposition", cd.toString());
        String desc = mp.getDescription();
        if (desc != null)
            resp.addHeader("Content-Description", desc);
        sendbackOriginalDoc(mp.getInputStream(), contentType, resp);
    }

    public static void sendbackOriginalDoc(InputStream is, String contentType, HttpServletResponse resp) throws IOException {
        resp.setContentType(contentType);
        try {
            ByteUtil.copy(is, resp.getOutputStream());
        } finally {
            if (is != null)
                is.close();
        }
    }
}

