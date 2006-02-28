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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.ZimbraLog;

public class NativeFormatter extends Formatter {

    private static final String CONVERSION_PATH = "/extension/convertd";
    public static final String ATTR_MIMEPART   = "mimepart";
    public static final String ATTR_MSGDIGEST  = "msgdigest";
    public static final String ATTR_CONTENTURL = "contenturl";
    
    public String getType() {
        return "native";
    }

    public String getDefaultSearchTypes() {
        // TODO: all?
        return MailboxIndex.SEARCH_FOR_MESSAGES;
    }

    public void format(Context context, MailItem item) throws IOException, ServiceException, UserServletException, ServletException {
        if (context.sync && !context.hasPart()) {
            // for sync, return metadata as headers to avoid extra SOAP round-trips
            context.resp.addHeader("X-Zimbra-Tags", item.getTagString());
            context.resp.addHeader("X-Zimbra-Flags", item.getFlagString());
            context.resp.addHeader("X-Zimbra-Received", Long.toString(item.getDate()));
            if (item.getChangeDate() != item.getDate())
                context.resp.addHeader("X-Zimbra-Modified", Long.toString(item.getChangeDate()));
        }

        try {
            if (item instanceof Message) {
                handleMessage(context, (Message) item);
            } else if (item instanceof Appointment) {
                handleAppt(context, (Appointment) item);
            } else if (mailItem instanceof WikiItem) {
                handleWiki(context, (WikiItem)mailItem);
            } else {
                throw UserServletException.notImplemented("can only handle messages/appts");
            }
        } catch (MessagingException me) {
            throw ServiceException.FAILURE(me.getMessage(), me);
        }
    }
    
    private void handleMessage(Context context, Message msg) throws IOException, ServiceException, MessagingException, ServletException {
        if (context.hasPart()) {
            MimePart mp = getMimePart(msg, context.getPart());            
            handleMessagePart(context, mp, msg);
        } else {
            if (context.sync) {
                // for sync, return metadata as headers to avoid extra SOAP round-trips
                context.resp.addHeader("X-Zimbra-Conv", Integer.toString(msg.getConversationId()));
            }
            context.resp.setContentType(Mime.CT_TEXT_PLAIN);
            InputStream is = msg.getRawMessage();
            ByteUtil.copy(is, context.resp.getOutputStream());
            is.close();
        }
    }

    private void handleAppt(Context context, Appointment appt) throws IOException, ServiceException, MessagingException, ServletException {
        if (context.hasPart()) {
            MimePart mp = null;
            if (context.itemId.hasSubpart()) {
                MimeMessage mbp = appt.getMimeMessage(context.itemId.getSubpartId());
                mp = Mime.getMimePart(mbp, context.getPart());
            } else {
                mp = getMimePart(appt, context.getPart());
            }
            handleMessagePart(context, mp, appt);
        } else {
            context.resp.setContentType(Mime.CT_TEXT_PLAIN);
            InputStream is = appt.getRawMessage();
            ByteUtil.copy(is, context.resp.getOutputStream());
            is.close();
        }
    }
    
    private void handleWiki(Context context, WikiItem wiki) throws IOException, ServiceException, MessagingException {
        context.resp.setContentType(Mime.CT_TEXT_PLAIN);
        InputStream is = wiki.getRawMessage();
        ByteUtil.copy(is, context.resp.getOutputStream());
        is.close();
    }
    
    private void handleMessagePart(Context context, MimePart mp, MailItem item) throws IOException, MessagingException, ServletException {
        if (mp == null) {
            context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "part not found");
        } else {
            String contentType = mp.getContentType();
            if (contentType == null) {
                contentType = Mime.CT_APPLICATION_OCTET_STREAM;
            }
            boolean html = checkGlobalOverride(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, context.authAccount) ||
                            (context.hasView() && context.getView().equals("html"));
            ZimbraLog.mailbox.info("view = "+context.getView());
            ZimbraLog.mailbox.info("html = "+html);
            

            if (!html) {
                sendbackOriginalDoc(mp, contentType, context.resp);
            } else {
                context.req.setAttribute(ATTR_MIMEPART, mp);
                context.req.setAttribute(ATTR_MSGDIGEST, item.getDigest());
                context.req.setAttribute(ATTR_CONTENTURL, context.req.getRequestURL().toString());
                RequestDispatcher dispatcher = context.req.getRequestDispatcher(CONVERSION_PATH);
                dispatcher.forward(context.req, context.resp);
            }
//            sendbackOriginalDoc(mp, contentType, context.resp);
            return;
        }
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

    public boolean canBeBlocked() {
        return true;
    }
}

