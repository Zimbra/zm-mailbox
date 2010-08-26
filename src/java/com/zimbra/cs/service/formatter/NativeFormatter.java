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
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
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
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.mime.MimeConstants;
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
    public static final String ATTR_CONTENTLENGTH = "contentlength";

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
        	sendZimbraHeaders(context.resp, context.target);
            if (context.target instanceof Message) {
                handleMessage(context, (Message) context.target);
            } else if (context.target instanceof CalendarItem) {
                // Don't return private appointments/tasks if the requester is not the mailbox owner.
                CalendarItem calItem = (CalendarItem) context.target;
                if (calItem.isPublic() || calItem.allowPrivateAccess(context.authAccount, context.isUsingAdminPrivileges()))
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
            context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
            long size = msg.getSize();
            if (size > 0)
                context.resp.setContentLength((int)size);
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
            context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
            InputStream is = calItem.getRawMessage();
            if (is != null)
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
            context.resp.setContentType(MimeConstants.CT_TEXT_PLAIN);
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
            String shortContentType = Mime.getContentType(mp);

            if (contentType == null) {
                contentType = MimeConstants.CT_TEXT_PLAIN;
            } else if (shortContentType.equalsIgnoreCase(MimeConstants.CT_APPLICATION_OCTET_STREAM)) {
                if ((contentType = MimeDetect.getMimeDetect().detect(Mime.getFilename(mp), mp.getInputStream())) == null)
                    contentType = MimeConstants.CT_APPLICATION_OCTET_STREAM;
                else
                    shortContentType = contentType;
            }
            // CR or LF in Content-Type causes Chrome to barf, unfortunately
            contentType = contentType.replace('\r', ' ').replace('\n', ' ');

            // IE displays garbage if the content-type header is too long
            String ua = context.req.getHeader("User-Agent");
            if (ua != null && ua.indexOf("MSIE") != -1 && contentType.length() > 80)
                contentType = shortContentType;
            
            boolean html = checkGlobalOverride(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, context.authAccount) ||
                            (context.hasView() && context.getView().equals(HTML_VIEW));
            if (!html) {
                String defaultCharset = context.targetAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
            	sendbackOriginalDoc(mp, contentType, defaultCharset, context.req, context.resp);
            } else {
            	handleConversion(context, mp.getInputStream(), Mime.getFilename(mp), contentType, item.getDigest(), mp.getSize());
            }
        }
    }
    
    private void handleDocument(Context context, Document doc) throws IOException, ServiceException, ServletException {
        String v = context.params.get(UserServlet.QP_VERSION);
        int version = v != null ? Integer.parseInt(v) : -1;
        String contentType = doc.getContentType();

        doc = (version > 0 ? (Document)doc.getMailbox().getItemRevision(context.opContext, doc.getId(), doc.getType(), version) : doc);
        InputStream is = doc.getContentStream();
    	if (HTML_VIEW.equals(context.getView())) {
    		handleConversion(context, is, doc.getName(), doc.getContentType(), doc.getDigest(), doc.getSize());
    	} else {
            String defaultCharset = context.targetAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
            boolean neuter = doc.getAccount().getBooleanAttr(Provisioning.A_zimbraNotebookSanitizeHtml, true);
            if (neuter)
            	sendbackOriginalDoc(is, contentType, defaultCharset, doc.getName(), null, doc.getSize(), context.req, context.resp);
            else
            	sendbackBinaryData(context.req, context.resp, is, null, doc.getName(), doc.getSize());
        }
    }
    
    private void handleConversion(Context ctxt, InputStream is, String filename, String ct, String digest, long length) throws IOException, ServletException {
        try {
            ctxt.req.setAttribute(ATTR_INPUTSTREAM, is);
            ctxt.req.setAttribute(ATTR_MSGDIGEST, digest);
            ctxt.req.setAttribute(ATTR_FILENAME, filename);
            ctxt.req.setAttribute(ATTR_CONTENTTYPE, ct);
            ctxt.req.setAttribute(ATTR_CONTENTURL, ctxt.req.getRequestURL().toString());
            ctxt.req.setAttribute(ATTR_CONTENTLENGTH, length);
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
        String enc = mp.getEncoding();
        if (enc != null)
            enc = enc.toLowerCase();
        long size = enc == null || enc.equals("7bit") || enc.equals("8bit") || enc.equals("binary") ? mp.getSize() : 0;
        sendbackOriginalDoc(mp.getInputStream(), contentType, defaultCharset, Mime.getFilename(mp), mp.getDescription(), size, req, resp);
    }

    public static void sendbackOriginalDoc(InputStream is, String contentType, String defaultCharset, String filename, String desc,
        HttpServletRequest req, HttpServletResponse resp) throws IOException {
        sendbackOriginalDoc(is, contentType, defaultCharset, filename, desc, 0, req, resp);
    }
    
    public static void sendbackOriginalDoc(InputStream is, String contentType, String defaultCharset, String filename, String desc, long size,
        HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String disp = req.getParameter(UserServlet.QP_DISP);
        
        disp = (disp == null || disp.toLowerCase().startsWith("i") ) ? Part.INLINE : Part.ATTACHMENT;
        if (desc != null)
            resp.addHeader("Content-Description", desc);

        // defang when the html attachment was requested with disposition inline
        if (contentType.startsWith(MimeConstants.CT_TEXT_HTML) &&
                disp.equals(Part.INLINE)) {
            String charset = Mime.getCharset(contentType);
            String content;
            
            if (charset != null && !charset.equals("")) {
                Reader reader = Mime.getTextReader(is, contentType, defaultCharset);
                
                contentType = MimeConstants.CT_TEXT_HTML + "; charset=" + charset; 
                content = HtmlDefang.defang(reader, false);
            } else {
                contentType = MimeConstants.CT_TEXT_HTML;
                content = HtmlDefang.defang(is, false);
            }
            resp.setContentType(contentType);
            if (content.length() > 0)
                resp.setContentLength(content.length());
            resp.getWriter().write(content);
        } else {
            resp.setContentType(contentType);
            sendbackBinaryData(req, resp, is, disp, filename, size);
        }
    }

    public boolean canBeBlocked() {
        return true;
    }

    public boolean supportsSave() {
        return true;
    }

    public void saveCallback(Context context, String contentType, Folder folder, String filename) throws IOException, ServiceException, UserServletException {
        Mailbox mbox = folder.getMailbox();
        MailItem item = null;
        if (filename == null) {
            try {
                ParsedMessage pm = new ParsedMessage(context.getPostBody(), mbox.attachmentsIndexingEnabled());
                item = mbox.addMessage(context.opContext, pm, folder.getId(), true, 0, null);
                return;
            } catch (ServiceException e) {
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "error parsing message");
            }
        }

        String creator = (context.authAccount == null ? null : context.authAccount.getName());
        InputStream is = context.getRequestInputStream();
        ParsedDocument pd = null;

        try {
            pd = new ParsedDocument(is, filename, contentType, System.currentTimeMillis(), creator, context.req.getHeader("X-Zimbra-Description"));
            item = mbox.getItemByPath(context.opContext, filename, folder.getId());
            // XXX: should we just overwrite here instead?
            if (!(item instanceof Document))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "cannot overwrite existing object at that path");

            item = mbox.addDocumentRevision(context.opContext, item.getId(), pd);
        } catch (NoSuchItemException nsie) {
            item = mbox.createDocument(context.opContext, folder.getId(), pd, MailItem.TYPE_DOCUMENT);
        } finally {
            is.close();
        }
        sendZimbraHeaders(context.resp, item);
    }
    
    private void sendZimbraHeaders(HttpServletResponse resp, MailItem item) {
    	if (resp == null || item == null)
    		return;
    	resp.addHeader("X-Zimbra-ItemId", item.getId() + "");
    	resp.addHeader("X-Zimbra-Version", item.getVersion() + "");
    	resp.addHeader("X-Zimbra-Modified", item.getChangeDate() + "");
    	resp.addHeader("X-Zimbra-Change", item.getModifiedSequence() + "");
    	resp.addHeader("X-Zimbra-Revision", item.getSavedSequence() + "");
    	resp.addHeader("X-Zimbra-ItemType", MailItem.getNameForType(item));
    	resp.addHeader("X-Zimbra-ItemName", item.getName());
    	try {
    		resp.addHeader("X-Zimbra-ItemPath", item.getPath());
    	} catch (ServiceException e) {
    	}
    }
    
    private static final int READ_AHEAD_BUFFER_SIZE = 256;
    private static final byte[][] SCRIPT_PATTERN = { 
        { '<', 's', 'c', 'r', 'i', 'p', 't' }, 
        { '<', 'S', 'C', 'R', 'I', 'P', 'T' } 
    };

    public static void sendbackBinaryData(HttpServletRequest req, HttpServletResponse resp, InputStream in, String disposition, String filename, long size) throws IOException {
    	if (disposition == null) {
            String disp = req.getParameter(UserServlet.QP_DISP);
            disposition = (disp == null || disp.toLowerCase().startsWith("i") ) ? Part.INLINE : Part.ATTACHMENT;
    	}
        PushbackInputStream pis = new PushbackInputStream(in, READ_AHEAD_BUFFER_SIZE);
        boolean isSafe = false;
        String ua = req.getHeader("User-Agent");
        if (ua == null || ua.indexOf("MSIE") == -1)
            isSafe = true;
        if (disposition != null && disposition.equals(Part.ATTACHMENT))
            isSafe = true;

        if (!isSafe) {
            byte[] buf = new byte[READ_AHEAD_BUFFER_SIZE];
            int bytesRead = pis.read(buf, 0, READ_AHEAD_BUFFER_SIZE);
            boolean hasScript = false;
            for (int i = 0; i < bytesRead; i++) {
                if (buf[i] == SCRIPT_PATTERN[0][0] || buf[i] == SCRIPT_PATTERN[1][0]) {
                    hasScript = true;
                    for (int pos = 1; pos < 7 && (i + pos) < bytesRead; pos++) {
                        if (buf[i+pos] != SCRIPT_PATTERN[0][pos] &&
                                buf[i+pos] != SCRIPT_PATTERN[1][pos]) {
                            hasScript = false;
                            break;
                        }
                    }
                    if (hasScript) {
                        resp.addHeader("Cache-Control", "no-transform");
                        disposition = Part.ATTACHMENT;
                        break;
                    }
                }
            }
            if (bytesRead > 0)
                pis.unread(buf, 0, bytesRead);
        }
        if (disposition != null) {
            String cd = disposition + "; filename=" + HttpUtil.encodeFilename(req, filename == null ? "unknown" : filename);
            resp.addHeader("Content-Disposition", cd);
        }
        if (size > 0)
            resp.setContentLength((int)size);
        ByteUtil.copy(pis, true, resp.getOutputStream(), false);
    }
}
