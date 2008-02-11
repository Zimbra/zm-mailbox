/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.util.*;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * The content servlet returns an attachment document in its original format.
 * If attachment needs to be converted, the control is passed down the filter chain
 * to ConversionServlet.
 */

public class ContentServlet extends ZimbraServlet {
    private static final long serialVersionUID = 6466028729668217319L;

    public static final String SERVLET_PATH = "/service/content";

    public static final String PREFIX_GET = "/get";
    protected static final String PREFIX_PROXY = "/proxy";

    public static final String PARAM_MSGID = "id";
    protected static final String PARAM_UPLOAD_ID = "aid";
    protected static final String PARAM_PART = "part";
    protected static final String PARAM_FORMAT = "fmt";
    protected static final String PARAM_SYNC = "sync";
    protected static final String PARAM_EXPUNGE = "expunge";

    protected static final String FORMAT_RAW = "raw";
    protected static final String FORMAT_DEFANGED_HTML = "htmldf";
    protected static final String FORMAT_DEFANGED_HTML_NOT_IMAGES = "htmldfi";

    protected static final String CONVERSION_PATH = "/extension/convertd";
    protected static final String ATTR_MIMEPART   = "mimepart";
    protected static final String ATTR_MSGDIGEST  = "msgdigest";
    protected static final String ATTR_CONTENTURL = "contenturl";

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";
    private String mBlockPage = null;

    private static Log mLog = LogFactory.getLog(ContentServlet.class);

    private void getCommand(HttpServletRequest req, HttpServletResponse resp, AuthToken token)
    throws ServletException, IOException {
        ItemId iid = null;
        try {
            iid = new ItemId(req.getParameter(PARAM_MSGID), (String) null);
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid id requested");
            return;
        }

        String part = req.getParameter(PARAM_PART);
        String fmt = req.getParameter(PARAM_FORMAT);

        try {
            // need to proxy the fetch if the mailbox lives on another server
            if (!iid.isLocal()) {
                // wrong server; proxy to the right one...
                proxyServletRequest(req, resp, iid.getAccountId());
                return;
            }

            String authId = token.getAccountId();
            String accountId = iid.getAccountId() != null ? iid.getAccountId() : authId;
            Account.addAccountToLogContext(accountId, ZimbraLog.C_NAME, ZimbraLog.C_ID);
            if (!accountId.equalsIgnoreCase(authId))
                ZimbraLog.addToContext(ZimbraLog.C_AID, authId);
            addRemoteIpToLoggingContext(req);

            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId);
            if (mbox == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
                return;				
            }
            ZimbraLog.addMboxToContext(mbox.getId());

            MailItem item = mbox.getItemById(new Mailbox.OperationContext(token), iid.getId(), MailItem.TYPE_UNKNOWN);
            if (item == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "message not found");
                return;				
            }

            try {
                if (part == null) {
                    // they want the entire message...
                    boolean sync = "1".equals(req.getParameter(PARAM_SYNC));
                    StringBuffer hdr = new StringBuffer();
                    if (sync) {
                        // for sync, return metadata as headers to avoid extra SOAP round-trips
                        resp.addHeader("X-Zimbra-Tags", item.getTagString());
                        resp.addHeader("X-Zimbra-Flags", item.getFlagString());
                        resp.addHeader("X-Zimbra-Received", Long.toString(item.getDate()));
                        resp.addHeader("X-Zimbra-Modified", Long.toString(item.getChangeDate()));
                        // also return metadata inline in the message content for now
                        hdr.append("X-Zimbra-Tags: ").append(item.getTagString()).append("\n");
                        hdr.append("X-Zimbra-Flags: ").append(item.getFlagString()).append("\n");
                        hdr.append("X-Zimbra-Received: ").append(item.getDate()).append("\n");
                        hdr.append("X-Zimbra-Modified: ").append(item.getChangeDate()).append("\n");
                    }
                    
                    if (item instanceof Message) {
                        Message msg = (Message) item;
                        if (sync) {
                            resp.addHeader("X-Zimbra-Conv", Integer.toString(msg.getConversationId()));
                            hdr.append("X-Zimbra-Conv: ").append(msg.getConversationId()).append("\n");
                            resp.getOutputStream().write(hdr.toString().getBytes());
                        }

                        resp.setContentType(Mime.CT_TEXT_PLAIN);
                        InputStream is = msg.getContentStream();
                        ByteUtil.copy(is, true, resp.getOutputStream(), false);
                    } else if (item instanceof Appointment) {
                        Appointment appt = (Appointment) item;
                        if (sync) {
                            resp.getOutputStream().write(hdr.toString().getBytes());
                        }
                        
                        resp.setContentType(Mime.CT_TEXT_PLAIN);
                        if (iid.hasSubpart()) {
                            MimeMessage mm = appt.getSubpartMessage(iid.getSubpartId());
                            mm.writeTo(resp.getOutputStream());
                        } else { 
//                            Invite[] invites = appt.getInvites();
//                            for (int i = 0; i < invites.length; i++)
//                            {
//                                Calendar cal = invites[i].toICalendar();
//                                cal.toString();
//                                resp.getWriter().write(cal.toString());
//                            }
                            
                            InputStream is = appt.getRawMessage();
                            ByteUtil.copy(is, true, resp.getOutputStream(), false);
                        }
                    }
                    return;
                } else {
                    MimePart mp;
                    if (item instanceof Message) {
                        mp = getMimePart((Message) item, part); 
                    } else {
                        Appointment appt = (Appointment) item;
                        if (iid.hasSubpart()) {
                            MimeMessage mbp = appt.getSubpartMessage(iid.getSubpartId());
                            mp = Mime.getMimePart(mbp, part);
                        } else {
                            mp = getMimePart(appt, part);
                        }
                    }
                    if (mp != null) {
                        String contentType = mp.getContentType();
                        if (contentType == null) {
                            contentType = Mime.CT_APPLICATION_OCTET_STREAM;
                        }
                        if (contentType.toLowerCase().startsWith(Mime.CT_TEXT_HTML) && (FORMAT_DEFANGED_HTML.equals(fmt) || FORMAT_DEFANGED_HTML_NOT_IMAGES.equals(fmt))) {
                            sendbackDefangedHtml(mp, contentType, resp, fmt);
                        } else {
                            if (!isTrue(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, mbox.getAccountId())) {
                                sendbackOriginalDoc(mp, contentType, req, resp);
                            } else {
                                req.setAttribute(ATTR_MIMEPART, mp);
                                req.setAttribute(ATTR_MSGDIGEST, item.getDigest());
                                req.setAttribute(ATTR_CONTENTURL, req.getRequestURL().toString());
                                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(CONVERSION_PATH);
                                dispatcher.forward(req, resp);
                            }
                        }
                        return;
                    }
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "part not found");
                }
            } catch (MessagingException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } 
        } catch (NoSuchItemException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no such item");
        } catch (ServiceException e) {
        	returnError(resp, e);
		} finally {
            ZimbraLog.clearContext();      
        }
        /*
         out.println("hello world "+req.getParameter("id"));
         out.println("path info: "+req.getPathInfo());
         out.println("pathtrans: "+req.getPathTranslated());
         */
    }

    private void retrieveUpload(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken) throws IOException {
        // if it's another server fetching an already-uploaded file, just do that
        String uploadId = req.getParameter(PARAM_UPLOAD_ID);
        if (uploadId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing upload id");
            return;
        }

        try {
            if (!FileUploadServlet.isLocalUpload(uploadId)) {
                // wrong server; proxy to the right one...
                String serverId = FileUploadServlet.getUploadServerId(uploadId);
                Server server = Provisioning.getInstance().get(ServerBy.id, serverId);
                proxyServletRequest(req, resp, server, null);
                return;
            }

            Upload up = FileUploadServlet.fetchUpload(authToken.getAccountId(), uploadId, authToken);
            if (up == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no such upload");
                return;
            }

            String filename = up.getName();
            ContentDisposition cd = new ContentDisposition(Part.INLINE).setParameter("filename", filename == null ? "unknown" : filename);
            resp.addHeader("Content-Disposition", cd.toString());
            sendbackOriginalDoc(up.getInputStream(), up.getContentType(), resp);

            boolean expunge = "true".equalsIgnoreCase(req.getParameter(PARAM_EXPUNGE)) || "1".equals(req.getParameter(PARAM_EXPUNGE));
            if (expunge)
                FileUploadServlet.deleteUpload(up);
        } catch (ServiceException e) {
        	returnError(resp, e);
        }
    }

    /**
     * @param accountId
     * @return
     * @throws ServiceException
     */
    private boolean isTrue(String attr, String accountId) throws ServletException {
        Provisioning prov = Provisioning.getInstance();
        try {
            Account account = prov.get(AccountBy.id, accountId);
            return prov.getConfig().getBooleanAttr(attr, false)
                    || account.getBooleanAttr(attr, false);
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
    }

    public static MimePart getMimePart(Appointment appt, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(appt.getMimeMessage(), part);
    }
    
    public static MimePart getMimePart(Message msg, String part) throws IOException, MessagingException, ServiceException {
        return Mime.getMimePart(msg.getMimeMessage(), part);
    }
    
    public static void sendbackOriginalDoc(MimePart mp, String contentType, HttpServletRequest req, HttpServletResponse resp)
    throws IOException, MessagingException {
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
    
    public static void sendbackOriginalDoc(InputStream is, String contentType, HttpServletResponse resp)
    throws IOException {
        resp.setContentType(contentType);
        ByteUtil.copy(is, true, resp.getOutputStream(), false);
    }

    static void sendbackDefangedHtml(MimePart mp, String contentType, HttpServletResponse resp, String fmt) 
    throws IOException, MessagingException {
        resp.setContentType(contentType);
        InputStream is = null;
        try {
            String html = HtmlDefang.defang(is = mp.getInputStream(), FORMAT_DEFANGED_HTML.equals(fmt));
            ByteArrayInputStream bais = new ByteArrayInputStream(html.getBytes("utf-8"));
            ByteUtil.copy(bais, false, resp.getOutputStream(), false);
        } finally {
            ByteUtil.closeStream(is);
        }
	}

    /**
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    private void sendbackBlockMessage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(mBlockPage);
        if (dispatcher != null) {
            dispatcher.forward(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The attachment download has been disabled per security policy.");
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        ZimbraLog.clearContext();
        mLog.debug("request url: %s, path info: ", req.getRequestURL(), req.getPathInfo());
        
        AuthToken authToken = getAuthTokenFromCookie(req, resp);
        if (authToken == null) 
            return;
        
        if (isTrue(Provisioning.A_zimbraAttachmentsBlocked, authToken.getAccountId())) {
            sendbackBlockMessage(req, resp);
            return;
        }
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.equals(PREFIX_GET)) {
            getCommand(req, resp, authToken);
        } else if (pathInfo != null && pathInfo.equals(PREFIX_PROXY)) {
            retrieveUpload(req, resp, authToken);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid request");
        }
    }
    
    public void init() throws ServletException {
        String name = getServletName();
        mLog.info("Servlet " + name + " starting up");
        super.init();
        mBlockPage = getInitParameter(MSGPAGE_BLOCK);
    }

    public void destroy() {
        String name = getServletName();
        mLog.info("Servlet " + name + " shutting down");
        super.destroy();
    }
}
