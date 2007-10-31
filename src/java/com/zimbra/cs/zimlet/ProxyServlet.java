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
package com.zimbra.cs.zimlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.NetUtil;

/**
 * @author jylee
 */
@SuppressWarnings("serial")
public class ProxyServlet extends ZimbraServlet {
    private static final String TARGET_PARAM = "target";
    private static final String UPLOAD_PARAM = "upload";
    private static final String USER_PARAM = "user";
    private static final String PASS_PARAM = "pass";
    private static final String FORMAT_PARAM = "fmt";
    private static final String FILENAME_PARAM = "filename";
    private static final String AUTH_PARAM = "auth";
    private static final String AUTH_BASIC = "basic";

    private Set<String> getAllowedDomains(AuthToken auth) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, auth.getAccountId());
        return prov.getCOS(acct).getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
    }
    
    private boolean checkPermissionOnTarget(HttpServletRequest req, URL target, AuthToken auth) {
        String host = target.getHost().toLowerCase();
        Set<String> domains;
        try {
            domains = getAllowedDomains(auth);
        } catch (ServiceException se) {
            ZimbraLog.zimlet.info("error getting allowedDomains: "+se.getMessage());
            return false;
        }
        for (String domain : domains) {
            if (domain.equals("*")) {
                return true;
            }
            if (domain.charAt(0) == '*') {
                domain = domain.substring(1);
            }
            if (host.endsWith(domain)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean canProxyHeader(String header) {
        if (header == null) return false;
        header = header.toLowerCase();
        if (header.startsWith("accept") ||
            header.equals("content-length") ||
            header.equals("connection") ||
            header.equals("keep-alive") ||
            header.equals("pragma") ||
            header.equals("host") ||
            //header.equals("user-agent") ||
            header.equals("cache-control") ||
            header.equals("cookie")) {
            return false;
        }
        return true;
    }
    
    private byte[] copyPostedData(HttpServletRequest req) throws IOException {
        int size = req.getContentLength();
        if (req.getMethod().equalsIgnoreCase("GET") || size <= 0) {
            return null;
        }
        InputStream is = req.getInputStream();
        ByteArrayOutputStream baos = null;
        try {
            if (size < 0)
                size = 0; 
            baos = new ByteArrayOutputStream(size);
            byte[] buffer = new byte[8192];
            int num;
            while ((num = is.read(buffer)) != -1) {
                baos.write(buffer, 0, num);
            }
            return baos.toByteArray();
        } finally {
            ByteUtil.closeStream(baos);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doProxy(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doProxy(req, resp);
    }

    private static final String DEFAULT_CTYPE = "text/xml";

    private void doProxy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ZimbraLog.clearContext();
        AuthToken authToken = getAuthTokenFromCookie(req, resp);
        if (authToken == null)
            return;

        // get the posted body before the server read and parse them.
        byte[] body = copyPostedData(req);

        // sanity check
        String target = req.getParameter(TARGET_PARAM);
        if (target == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // check for permission
        URL url = new URL(target);
        if (!checkPermissionOnTarget(req, url, authToken)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // determine whether to return the target inline or store it as an upload
        String uploadParam = req.getParameter(UPLOAD_PARAM);
        boolean asUpload = uploadParam != null && (uploadParam.equals("1") || uploadParam.equalsIgnoreCase("true"));

        HttpMethod method = null;
        try {
            HttpClient client = new HttpClient();
            NetUtil.configureProxy(client);
            String reqMethod = req.getMethod();
            if (reqMethod.equalsIgnoreCase("GET"))
                method = new GetMethod(target);
            else if (reqMethod.equalsIgnoreCase("POST")) {
                PostMethod post = new PostMethod(target);
                if (body != null)
                    post.setRequestEntity(new ByteArrayRequestEntity(body, req.getContentType()));
                method = post;
            } else {
                ZimbraLog.zimlet.info("unsupported request method: " + reqMethod);
                resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }

            // handle basic auth
            String auth, user, pass;
            auth = req.getParameter(AUTH_PARAM);
            user = req.getParameter(USER_PARAM);
            pass = req.getParameter(PASS_PARAM);
            if (auth != null && user != null && pass != null) {
                if (!auth.equals(AUTH_BASIC)) {
                    ZimbraLog.zimlet.info("unsupported auth type: " + auth);
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                HttpState state = new HttpState();
                state.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
                client.setState(state);
                method.setDoAuthentication(true);
            }
            
            Enumeration headers = req.getHeaderNames();
            while (headers.hasMoreElements()) {
                String hdr = (String) headers.nextElement();
                if (canProxyHeader(hdr)) {
                    //ZimbraLog.zimlet.info(hdr + ": " + req.getHeader(hdr));
                    method.addRequestHeader(hdr, req.getHeader(hdr));
                }
            }
            
            try {
                client.executeMethod(method);
            } catch (HttpException ex) {
                ZimbraLog.zimlet.info("exception while proxying " + target, ex);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            int status = method.getStatusLine() == null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : method.getStatusCode();

            // workaround for Alexa Thumbnails paid web service, which doesn't bother to return a content-type line
            Header ctHeader = method.getResponseHeader("Content-Type");
            String contentType = ctHeader == null || ctHeader.getValue() == null ? DEFAULT_CTYPE : ctHeader.getValue();

            if (asUpload) {
                String filename = req.getParameter(FILENAME_PARAM);
                if (filename == null || filename.equals(""))
                    filename = new ContentType(contentType).getParameter("name");
                if ((filename == null || filename.equals("")) && method.getResponseHeader("Content-Disposition") != null)
                    filename = new ContentDisposition(method.getResponseHeader("Content-Disposition").getValue()).getParameter("filename");
                if (filename == null || filename.equals(""))
                    filename = "unknown";

                List<Upload> uploads = null;
                try {
                    Upload up = FileUploadServlet.saveUpload(method.getResponseBodyAsStream(), filename, contentType, authToken.getAccountId());
                    uploads = Arrays.asList(up);
                } catch (ServiceException e) {
                    if (e.getCode().equals(MailServiceException.UPLOAD_REJECTED))
                        status = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
                    else
                        status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                }
                resp.setStatus(status);
                FileUploadServlet.sendResponse(resp, status, req.getParameter(FORMAT_PARAM), null, uploads, null);
            } else {
                resp.setStatus(status);
                resp.setContentType(contentType);
                for (Header h : method.getResponseHeaders())
                    if (canProxyHeader(h.getName()))
                        resp.addHeader(h.getName(), h.getValue());
                ByteUtil.copy(method.getResponseBodyAsStream(), false, resp.getOutputStream(), false);
            }
        } finally {
            if (method != null)
                method.releaseConnection();
        }
    }
}
