/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;

public class NativeFormatter extends Formatter {
    
    private static final String CONVERSION_PATH = "/extension/convertd";
    public static final String ATTR_INPUTSTREAM = "inputstream";
    public static final String ATTR_MSGDIGEST  = "msgdigest";
    public static final String ATTR_FILENAME  = "filename";
    public static final String ATTR_CONTENTURL = "contenturl";
    public static final String ATTR_CONTENTTYPE = "contenttype";

    public static final String FMT_NATIVE = "native";

    public String getType() {
        return FMT_NATIVE;
    }

    public String getDefaultSearchTypes() {
        // TODO: all?
        return MailboxIndex.SEARCH_FOR_MESSAGES;
    }

    public void formatCallback(Context context) throws IOException, ServiceException, UserServletException, ServletException {
        try {
            if (context.target instanceof Message) {
                handleMessage(context, (Message) context.target);
            } else if (context.target instanceof CalendarItem) {
                // Don't return private appointments/tasks if the requester is not the mailbox owner.
                CalendarItem calItem = (CalendarItem) context.target;
                if (calItem.isPublic() || calItem.allowPrivateAccess(context.authAccount))
                    handleCalendarItem(context, calItem);
                else
                    context.resp.sendError(HttpServletResponse.SC_FORBIDDEN, "permission denied");
            } else if (context.target instanceof Document) {
                handleDocument(context, (Document) context.target);
            } else if (context.target instanceof Contact) {
                handleContact(context, (Contact) context.target);
            } else {
                throw UserServletException.notImplemented("can only handle messages/appointments/tasks/documents");
            }
        } catch (MessagingException me) {
            throw ServiceException.FAILURE(me.getMessage(), me);
        }
    }

    private void handleMessage(Context context, Message msg) throws IOException, ServiceException, MessagingException, ServletException {
        if (context.hasBody()) {
            List<MPartInfo> parts = Mime.getParts(msg.getMimeMessage());
            MPartInfo body = Mime.getTextBody(parts, false);
            if (body != null) {
                handleMessagePart(context, body.getMimePart(), msg);
            } else {
                context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "body not found");
            }
        } else if (context.hasPart()) {
            MimePart mp = getMimePart(msg, context.getPart());            
            handleMessagePart(context, mp, msg);
        } else {
            context.resp.setContentType(Mime.CT_TEXT_PLAIN);
            InputStream is = msg.getContentStream();
            ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }

    private void handleCalendarItem(Context context, CalendarItem calItem) throws IOException, ServiceException, MessagingException, ServletException {
        if (context.hasPart()) {
            MimePart mp = null;
            if (context.itemId.hasSubpart()) {
                MimeMessage mbp = calItem.getSubpartMessage(context.itemId.getSubpartId());
                mp = Mime.getMimePart(mbp, context.getPart());
            } else {
                mp = getMimePart(calItem, context.getPart());
            }
            handleMessagePart(context, mp, calItem);
        } else {
            context.resp.setContentType(Mime.CT_TEXT_PLAIN);
            InputStream is = calItem.getRawMessage();
            ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }

    private void handleContact(Context context, Contact con) throws IOException, ServiceException, MessagingException, ServletException {
        if (!con.hasAttachment()) {
            context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "body not found");
        } else if (context.hasPart()) {
            MimePart mp = Mime.getMimePart(con.getMimeMessage(false), context.getPart());
            handleMessagePart(context, mp, con);
        } else {
            context.resp.setContentType(Mime.CT_TEXT_PLAIN);
            InputStream is = new ByteArrayInputStream(con.getContent());
            ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }

    private static final String HTML_VIEW = "html";
    
    private void handleMessagePart(Context context, MimePart mp, MailItem item) throws IOException, MessagingException, ServletException {
        if (mp == null) {
            context.resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "part not found");
        } else {
            String contentType = mp.getContentType();
            if (contentType == null)
                contentType = Mime.CT_APPLICATION_OCTET_STREAM;
            boolean html = checkGlobalOverride(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, context.authAccount) ||
                            (context.hasView() && context.getView().equals(HTML_VIEW));
            if (!html) {
                String defaultCharset = context.targetAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
            	sendbackOriginalDoc(mp, contentType, defaultCharset, context.req, context.resp);
            } else {
            	handleConversion(context, mp.getInputStream(), Mime.getFilename(mp), mp.getContentType(), item.getDigest());
            }
        }
    }
    
    private void handleDocument(Context context, Document doc) throws IOException, ServiceException, ServletException {
        InputStream is = doc.getContentStream();
    	if (HTML_VIEW.equals(context.getView())) {
    		handleConversion(context, is, doc.getName(), doc.getContentType(), doc.getDigest());
    	} else {
        	context.resp.setContentType(doc.getContentType());
        	ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
        }
    }
    
    private void handleConversion(Context ctxt, InputStream is, String filename, String ct, String digest) throws IOException, ServletException {
        try {
            ctxt.req.setAttribute(ATTR_INPUTSTREAM, is);
            ctxt.req.setAttribute(ATTR_MSGDIGEST, digest);
            ctxt.req.setAttribute(ATTR_FILENAME, filename);
            ctxt.req.setAttribute(ATTR_CONTENTTYPE, ct);
            ctxt.req.setAttribute(ATTR_CONTENTURL, ctxt.req.getRequestURL().toString());
            RequestDispatcher dispatcher = ctxt.req.getRequestDispatcher(CONVERSION_PATH);
            dispatcher.forward(ctxt.req, ctxt.resp);
        } finally {
            ByteUtil.closeStream(is);
        }
    }
    
    public static MimePart getMimePart(CalendarItem calItem, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(calItem.getMimeMessage(), part);
    }

    public static MimePart getMimePart(Message msg, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(msg.getMimeMessage(), part);
    }

    public static void sendbackOriginalDoc(MimePart mp, String contentType, String defaultCharset, HttpServletRequest req, HttpServletResponse resp)
    throws IOException, MessagingException {
        sendbackOriginalDoc(mp.getInputStream(), contentType, defaultCharset, Mime.getFilename(mp), mp.getDescription(), req, resp);
    }

    public static void sendbackOriginalDoc(InputStream is, String contentType, String defaultCharset, String filename, String desc,
                                           HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        String disp = req.getParameter(UserServlet.QP_DISP);
        disp = (disp == null || disp.toLowerCase().startsWith("i") ) ? Part.INLINE : Part.ATTACHMENT;
        String cd = disp + "; filename=" + HttpUtil.encodeFilename(req, filename == null ? "unknown" : filename);
        resp.addHeader("Content-Disposition", cd);

        if (desc != null)
            resp.addHeader("Content-Description", desc);

        resp.setContentType(contentType);

        // defang when the html attachment was requested with disposition inline
        if (contentType.startsWith(Mime.CT_TEXT_HTML) &&
                disp.equals(Part.INLINE)) {
            String charset = Mime.getCharset(contentType);
            String content;
            if (charset != null && !charset.equals("")) {
                Reader reader = Mime.getTextReader(is, contentType, defaultCharset);
                content = HtmlDefang.defang(reader, false);
            } else {
                content = HtmlDefang.defang(is, false);
            }
            resp.getWriter().write(content);
        } else {
            ByteUtil.copy(is, true, resp.getOutputStream(), false);
        }
    }

    public boolean canBeBlocked() {
        return true;
    }

    public void saveCallback(byte[] body, Context context, String contentType, Folder folder, String filename) throws IOException, ServiceException, UserServletException {
        Mailbox mbox = folder.getMailbox();
        if (filename == null) {
            try {
                ParsedMessage pm = new ParsedMessage(body, mbox.attachmentsIndexingEnabled());
                mbox.addMessage(context.opContext, pm, folder.getId(), true, 0, null);
                return;
            } catch (MessagingException e) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "error parsing message");
            }
        }

        String creator = (context.authAccount == null ? null : context.authAccount.getName());
        ParsedDocument pd = new ParsedDocument(body, filename, contentType, System.currentTimeMillis(), creator);
        try {
            MailItem item = mbox.getItemByPath(context.opContext, filename, folder.getId());
            // XXX: should we just overwrite here instead?
            if (!(item instanceof Document))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "cannot overwrite existing object at that path");

            mbox.addDocumentRevision(context.opContext, item.getId(), item.getType(), pd);
        } catch (NoSuchItemException nsie) {
            mbox.createDocument(context.opContext, folder.getId(), pd, MailItem.TYPE_DOCUMENT);
        }
    }
}
