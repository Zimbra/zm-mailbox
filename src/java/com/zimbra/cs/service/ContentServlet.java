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

package com.zimbra.cs.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.util.*;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;

/**
 * The content servlet returns an attachment document in its original format.
 * If attachment needs to be converted, the control is passed down the filter chain
 * to ConversionServlet.
 */

public class ContentServlet extends ZimbraServlet {

    protected static final String CONVERSION_SERVLET = "ConversionServlet";

    protected static final String SERVLET_PATH = "/service/content";

    protected static final String PREFIX_CNV = "/cnv";
    protected static final String PREFIX_GET = "/get";
    protected static final String PREFIX_PROXY = "/proxy";

    protected static final String PARAM_MSGID = "id";
    protected static final String PARAM_UPLOAD_ID = "aid";
    protected static final String PARAM_PART = "part";
    protected static final String PARAM_FORMAT = "fmt";
    protected static final String PARAM_SYNC = "sync";

    protected static final String FORMAT_RAW = "raw";
    protected static final String FORMAT_DEFANGED_HTML = "htmldf";
    protected static final String FORMAT_DEFANGED_HTML_NOT_IMAGES = "htmldfi";

    protected static final String ATTR_MIMEPART = "mimepart";
    protected static final String ATTR_MSGDIGEST = "msgdigest";

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";
    private String mBlockPage = null;

    private static Log mLog = LogFactory.getLog(ContentServlet.class);

    private void getCommand(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken)
    throws ServletException, IOException {
        ItemId iid = null;
        try {
            iid = new ItemId(req.getParameter(PARAM_MSGID), null);
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid id requested");
            return;
        }

        String part = req.getParameter(PARAM_PART);
        String fmt = req.getParameter(PARAM_FORMAT);

        try {
            if (!iid.isLocal()) {
                // wrong server; proxy to the right one...
                proxyServletRequest(req, resp, iid.getAccountId());
                return;
            }
            String authId = authToken.getAccountId();
            String accountId = iid.getAccountId() != null ? iid.getAccountId() : authId;
            // need to proxy the fetch if the mailbox lives on another server
            Mailbox mbox = Mailbox.getMailboxByAccountId(accountId);
            if (mbox == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
                return;				
            }

            MailItem mi = mbox.getItemById(new Mailbox.OperationContext(authId), iid.getId(), MailItem.TYPE_UNKNOWN);
            if (mi == null) {
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
                        hdr.append("X-Zimbra-Tags: ").append(mi.getTagString()).append("\n");
                        hdr.append("X-Zimbra-Flags: ").append(mi.getFlagString()).append("\n");
                        hdr.append("X-Zimbra-Received: ").append(mi.getDate()).append("\n");
                    }
                    
                    if (mi instanceof Message) {
                        Message msg = (Message) mi;
                        if (sync) {
                            hdr.append("X-Zimbra-Conv: ").append(msg.getConversationId()).append("\n");
                            resp.getOutputStream().write(hdr.toString().getBytes());
                        }

                        resp.setContentType(Mime.CT_TEXT_PLAIN);
                        InputStream is = msg.getRawMessage();
                        ByteUtil.copy(is, resp.getOutputStream());
                        is.close();
                    } else if (mi instanceof Appointment) {
                        Appointment appt = (Appointment) mi;
                        if (sync) {
                            hdr.append("X-Zimbra-DefaultInvId: ").append(appt.getDefaultInvite().getMailItemId()).append("\n");
                            resp.getOutputStream().write(hdr.toString().getBytes());
                        }
                        
                        resp.setContentType(Mime.CT_TEXT_PLAIN);
                        if (iid.hasSubpart()) {
                            MimeMessage mm = ((Appointment) mi).getMimeMessage(iid.getSubpartId());
                            mm.writeTo(resp.getOutputStream());
                        } else { 
//                            Invite[] invites = appt.getInvites();
//                            for (int i = 0; i < invites.length; i++)
//                            {
//                                Calendar cal = invites[i].toICalendar();
//                                cal.toString();
//                                resp.getWriter().write(cal.toString());
//                            }
                            
                            InputStream is = ((Appointment) mi).getRawMessage();
                            ByteUtil.copy(is, resp.getOutputStream());
                            is.close();
                        }
                    }
                    return;
                } else {
                    MimePart mp;
                    if (mi instanceof Message) {
                        mp = getMimePart((Message)mi, part); 
                    } else {
                        Appointment appt = (Appointment)mi;
                        if (iid.hasSubpart()) {
                            MimeMessage mbp = appt.getMimeMessage(iid.getSubpartId());
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
                                sendbackOriginalDoc(mp, contentType, resp);
                            } else {
                                req.setAttribute(ATTR_MIMEPART, mp);
                                req.setAttribute(ATTR_MSGDIGEST, mi.getDigest());
                                RequestDispatcher dispatcher = this.getServletContext().getNamedDispatcher(CONVERSION_SERVLET);
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
            throw new ServletException(e);
		}
        /*
         out.println("hello world "+req.getParameter("id"));
         out.println("path info: "+req.getPathInfo());
         out.println("pathtrans: "+req.getPathTranslated());
         */
    }

    private void retrieveUpload(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken)
    throws ServletException, IOException {
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
                Server server = Provisioning.getInstance().getServerById(serverId);
                proxyServletRequest(req, resp, server);
                return;
            }

            Upload up = FileUploadServlet.fetchUpload(authToken.getAccountId(), uploadId, authToken.getEncoded());
            if (up == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no such upload");
                return;
            }

            ContentDisposition cd = new ContentDisposition(Part.INLINE);
            String filename = up.getName();
            cd.setParameter("filename", filename == null ? "unknown" : filename);
            resp.addHeader("Content-Disposition", cd.toString());
            sendbackOriginalDoc(up.getInputStream(), up.getContentType(), resp);

            FileUploadServlet.deleteUpload(up);
        } catch (ServiceException e) {
            throw new ServletException(e);
        } catch (MessagingException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (AuthTokenException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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
            Account account = prov.getAccountById(accountId);
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
    
    protected void sendbackOriginalDoc(MimePart mp, String contentType, HttpServletResponse resp) 
    throws IOException, MessagingException {
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
    
    protected void sendbackOriginalDoc(InputStream is, String contentType, HttpServletResponse resp)
    throws IOException {
        resp.setContentType(contentType);
        try {
            ByteUtil.copy(is, resp.getOutputStream());
        } finally {
            if (is != null)
                is.close();
        }
    }

    static void sendbackDefangedHtml(MimePart mp, String contentType, HttpServletResponse resp, String fmt) 
    throws IOException, MessagingException {
        resp.setContentType(contentType);
        InputStream is = null;
        try {
            is = mp.getInputStream();
            String html = HtmlDefang.defang(is, FORMAT_DEFANGED_HTML.equals(fmt));
            ByteArrayInputStream bais = new ByteArrayInputStream(html.getBytes());
            ByteUtil.copy(bais, resp.getOutputStream());
        } finally {
            if (is != null)
                is.close();
        }
	}

    /**
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    private void sendbackBlockMessage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RequestDispatcher dispatcher = null;
        if ((dispatcher = getDispatcher(mBlockPage)) != null) {
            dispatcher.forward(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The attachment download has been disabled per security policy.");
    }
    
    /**
     * @param path
     * @return
     */
    protected RequestDispatcher getDispatcher(String path) {
        if (path == null)
            return null;
        RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher(path);
        return dispatcher;
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        mLog.debug("request url: " + req.getRequestURL() + " path info: " + req.getPathInfo());
        
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
        } else if (pathInfo != null && pathInfo.startsWith(PREFIX_CNV)) {
            RequestDispatcher dispatcher = this.getServletContext().getNamedDispatcher(CONVERSION_SERVLET);
            dispatcher.forward(req, resp);
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
