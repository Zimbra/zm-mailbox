/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.ByteUtil;

import com.zimbra.cs.service.util.*;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * The content servlet returns an attachment document in its original format.
 * If attachment needs to be converted, the control is passed down the filter chain
 * to ConversionServlet.
 */

public class ContentServlet extends ZimbraServlet {

    protected static final String CONVERSION_SERVLET = "ConversionServlet";
    
    protected static final String PREFIX_CNV = "/cnv";
    protected static final String PREFIX_GET = "/get";

    protected static final String PARAM_MSGID = "id";
    protected static final String PARAM_PART = "part";
    protected static final String PARAM_FORMAT = "fmt";
    protected static final String PARAM_SYNC = "sync";

    protected static final String ATTR_MIMEPART = "mimepart";
    protected static final String ATTR_MSGDIGEST = "msgdigest";

    protected static final String MSGPAGE_BLOCK = "errorpage.attachment.blocked";
    private String mBlockPage = null;

    private static Log mLog = LogFactory.getLog(ContentServlet.class);

    private void getCommand(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken)
    throws ServletException, IOException
    {
        String idStr = req.getParameter(PARAM_MSGID);
        if (idStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing id parameter");
            return;
        }
        
        ParsedItemID id = null;
        
        try {
            id = ParsedItemID.Parse(idStr);
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid id requested");
            return;
        }

        String part = req.getParameter(PARAM_PART);
        String fmt = req.getParameter(PARAM_FORMAT);
        
        try {
            Mailbox mbox;
            if (!id.isLocal()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "wrong server");
                return;
            }
            if (!id.hasMailboxID()) {
                mbox = Mailbox.getMailboxByAccountId(authToken.getAccountId());
            } else {
                if (!authToken.isAdmin()) {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "admin auth token required");
                    return;
                }
                mbox = Mailbox.getMailboxById(id.getMailboxIDInt());
            }
            if (mbox == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
                return;				
            }
            
            MailItem mi = mbox.getItemById(id.getItemIDInt(), MailItem.TYPE_UNKNOWN);
            if (mi== null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "message not found");
                return;				
            }

            try {
                if (part == null) {
                    if (mi instanceof Message) {
                        Message msg = (Message)mi;
                        resp.setContentType(Mime.CT_TEXT_PLAIN);
                        if ("1".equals(req.getParameter(PARAM_SYNC))) {
                            // for sync, return metadata as headers to avoid extra SOAP round-trips
                            StringBuffer sb = new StringBuffer();
                            sb.append("X-Zimbra-Tags: ").append(msg.getTagString()).append("\n");
                            sb.append("X-Zimbra-Flags: ").append(msg.getFlagString()).append("\n");
                            sb.append("X-Zimbra-Conv: ").append(msg.getConversationId()).append("\n");
                            sb.append("X-Zimbra-Received: ").append(msg.getDate()).append("\n");
                            resp.getOutputStream().write(sb.toString().getBytes());
                        }
                        InputStream is = msg.getRawMessage();
                        ByteUtil.copy(is, resp.getOutputStream());
                        is.close();
                    } else if (mi instanceof Appointment) {
                        resp.setContentType(Mime.CT_TEXT_PLAIN);
                        if (id.hasSubId()) {
                            MimeMessage mm = ((Appointment)mi).getMimeMessage(id.getSubIdInt());
                            mm.writeTo(resp.getOutputStream());
                        } else { 
                            InputStream is;
                            is = ((Appointment)mi).getRawMessage();
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
                        if (id.hasSubId()) {
                            MimeMessage mbp = ((Appointment)mi).getMimeMessage(id.getSubIdInt());
                            mp = Mime.getMimePart(mbp, part);
                        } else {
                            mp = getMimePart((Appointment)mi, part);
                        }
                    }
                    if (mp != null) {
                        String contentType = mp.getContentType();
                        if (contentType == null)
                            contentType = Mime.CT_APPLICATION_OCTET_STREAM;
                        //mLog.info(contentType+" "+fmt);
                        if (contentType.toLowerCase().startsWith(Mime.CT_TEXT_HTML) && ("htmldf".equals(fmt) || "htmldfi".equals(fmt))) {
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
        } catch (ServiceException e) {
            throw new ServletException(e);
		}
        /*
         out.println("hello world "+req.getParameter("id"));
         out.println("path info: "+req.getPathInfo());
         out.println("pathtrans: "+req.getPathTranslated());
         */
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
        throws IOException, MessagingException 
    {
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
    
    protected void sendbackOriginalDoc(InputStream is, String contentType, HttpServletResponse resp) throws IOException {
        resp.setContentType(contentType);
        try {
            ByteUtil.copy(is, resp.getOutputStream());
        } finally {
            if (is != null)
                is.close();
        }
    }

    static void sendbackDefangedHtml(MimePart mp, String contentType, HttpServletResponse resp, String fmt) 
    throws IOException, MessagingException 
	{
        resp.setContentType(contentType);
        InputStream is = null;
        try {
            is = mp.getInputStream();
            String html = HtmlDefang.defang(is, "htmldf".equals(fmt));
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
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, 
        "The attachment download has been disabled per security policy.");
        
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
        if (pathInfo.equals(PREFIX_GET)) {
            getCommand(req, resp, authToken);
        } else if (pathInfo.startsWith(PREFIX_CNV)) {
            RequestDispatcher dispatcher = this.getServletContext().getNamedDispatcher(CONVERSION_SERVLET);
            dispatcher.forward(req, resp);
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
