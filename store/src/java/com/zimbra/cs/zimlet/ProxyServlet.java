/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.servlet.ZimbraServlet;
/**
 * @author jylee
 */
@SuppressWarnings("serial")
public class ProxyServlet extends ZimbraServlet {

    private static final String TARGET_PARAM = "target";

    private static final String UPLOAD_PARAM = "upload";
    private static final String FILENAME_PARAM = "filename";
    private static final String FORMAT_PARAM = "fmt";

    private static final String USER_PARAM = "user";
    private static final String PASS_PARAM = "pass";
    private static final String AUTH_PARAM = "auth";
    private static final String AUTH_BASIC = "basic";


    private static Set<String> getAllowedDomains(AuthToken auth) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, auth.getAccountId(), auth);

        Cos cos = prov.getCOS(acct);

        Set<String> allowedDomains = cos.getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);

        ZimbraLog.zimlet.debug("get allowedDomains result: "+allowedDomains);

        return allowedDomains;
    }

    protected static boolean checkPermissionOnTarget(URL target, AuthToken auth) {
        String host = target.getHost().toLowerCase();
        ZimbraLog.zimlet.debug("checking allowedDomains permission on target host: " + host);
        Set<String> domains;
        try {
            domains = getAllowedDomains(auth);
        } catch (ServiceException se) {
            ZimbraLog.zimlet.info("error getting allowedDomains: " + se.getMessage());
            return false;
        }
        for (String domain : domains) {
            if (domain.charAt(0) == '*') {
                domain = domain.substring(1);
                if (host.endsWith(domain)) {
                    return true;
                }
            }
            else if (host.equals(domain)) {
                return true;
            }
        }
        return false;
    }

    protected boolean canProxyHeader(String header) {
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
            header.equals("cookie") ||
            header.equals("transfer-encoding") ||
            Arrays.asList(LC.proxy_servlet_drop_headers.value().toString().split(",")).contains(header)) {
            return false;
        }
        return true;
    }

    protected byte[] copyPostedData(HttpServletRequest req) throws IOException {
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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doProxy(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doProxy(req, resp);
    }

    @Override
    protected boolean isAdminRequest(HttpServletRequest req) {
        return req.getServerPort() == LC.zimbra_admin_service_port.intValue();
    }

    protected static final String DEFAULT_CTYPE = "text/xml";

    protected static class RestrictiveRedirectStrategy extends DefaultRedirectStrategy {
        private final AuthToken authToken;

        protected RestrictiveRedirectStrategy(AuthToken authToken) {
            this.authToken = authToken;
        }

        @Override
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
        throws ProtocolException {
            Header header = response.getFirstHeader("Location");
            if (header == null) {
                return false;
            }

            String location = header.getValue();

            if (StringUtils.isEmpty(location)) {
                ZimbraLog.zimlet.info("refuse redirect to empty location");
                return false;
            }

            URL url = null;

            try {
                url = new URL(location);
            } catch (MalformedURLException ex) {
                // Malformed locations include relative ones.
                ZimbraLog.zimlet.info("refuse redirect to malformed location: " + location);
                return false;
            }

            if (!checkPermissionOnTarget(url, authToken)) {
                ZimbraLog.zimlet.info("refuse redirect to restricted location: " + location);
                return false;
            }

            return true;
        }
    }

    protected void doProxy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ZimbraLog.clearContext();
        boolean isAdmin = isAdminRequest(req);
        AuthToken authToken = isAdmin ?
                getAdminAuthTokenFromCookie(req, resp, true) : getAuthTokenFromCookie(req, resp, true);
        if (authToken == null) {
            String zAuthToken = req.getParameter(QP_ZAUTHTOKEN);
            if (zAuthToken != null) {
                try {
                    authToken = AuthProvider.getAuthToken(zAuthToken);
                    if (authToken.isExpired()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "authtoken expired");
                        return;
                    }
                } catch (AuthTokenException e) {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "unable to parse authtoken");
                    return;
                }
            }
        }
        if (authToken == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "no authtoken cookie");
            return;
        }
        if (!authToken.isRegistered()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "authtoken is invalid");
            return;
        }
        if (isAdmin && !authToken.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "permission denied");
            return;
        }

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
        if (!isAdmin && !checkPermissionOnTarget(url, authToken)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // determine whether to return the target inline or store it as an upload
        String uploadParam = req.getParameter(UPLOAD_PARAM);
        boolean asUpload = uploadParam != null && (uploadParam.equals("1") || uploadParam.equalsIgnoreCase("true"));

        HttpRequestBase method = null;
        try {
            HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(clientBuilder);
            String reqMethod = req.getMethod();
            if (reqMethod.equalsIgnoreCase("GET")) {
                method = new HttpGet(target);
            } else if (reqMethod.equalsIgnoreCase("POST")) {
                HttpPost post = new HttpPost(target);
                if (body != null)
                    post.setEntity(new ByteArrayEntity(body, org.apache.http.entity.ContentType.create(req.getContentType())));
                method = post;
            } else if (reqMethod.equalsIgnoreCase("PUT")) {
                HttpPut put = new HttpPut(target);
                if (body != null)
                    put.setEntity(new ByteArrayEntity(body, org.apache.http.entity.ContentType.create(req.getContentType())));
                method = put;
            } else if (reqMethod.equalsIgnoreCase("DELETE")) {
                method = new HttpDelete(target);
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
                CredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
                clientBuilder.setDefaultCredentialsProvider(provider);
            }

            Enumeration headers = req.getHeaderNames();
            while (headers.hasMoreElements()) {
                String hdr = (String) headers.nextElement();
                ZimbraLog.zimlet.debug("incoming: " + hdr + ": " + req.getHeader(hdr));
                if (canProxyHeader(hdr)) {
                    ZimbraLog.zimlet.debug("outgoing: " + hdr + ": " + req.getHeader(hdr));
                    method.addHeader(hdr, req.getHeader(hdr));
                }
            }

            HttpResponse httpResp = null;
            try {
                if (!(reqMethod.equalsIgnoreCase("POST") || reqMethod.equalsIgnoreCase("PUT"))) {
                    clientBuilder.setRedirectStrategy(new RestrictiveRedirectStrategy(authToken));
                }

                HttpClient client = clientBuilder.build();
                httpResp = HttpClientUtil.executeMethod(client, method);
            } catch (HttpException ex) {
                ZimbraLog.zimlet.info("exception while proxying " + target, ex);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            int status = httpResp.getStatusLine() == null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR :httpResp.getStatusLine().getStatusCode();

            // workaround for Alexa Thumbnails paid web service, which doesn't bother to return a content-type line
            Header ctHeader = httpResp.getFirstHeader("Content-Type");
            String contentType = ctHeader == null || ctHeader.getValue() == null ? DEFAULT_CTYPE : ctHeader.getValue();

            // getEntity may return null if no response body (e.g. HTTP 204)
            InputStream targetResponseBody = null;
            HttpEntity targetResponseEntity = httpResp.getEntity();
            if (targetResponseEntity != null) {
                targetResponseBody = targetResponseEntity.getContent();
            }

            if (asUpload) {
                String filename = req.getParameter(FILENAME_PARAM);
                if (filename == null || filename.equals(""))
                    filename = new ContentType(contentType).getParameter("name");
                if ((filename == null || filename.equals("")) && httpResp.getFirstHeader("Content-Disposition") != null)
                    filename = new ContentDisposition(httpResp.getFirstHeader("Content-Disposition").getValue()).getParameter("filename");
                if (filename == null || filename.equals(""))
                    filename = "unknown";

                List<Upload> uploads = null;

                if (targetResponseBody != null) {
                    try {
                        Upload up = FileUploadServlet.saveUpload(targetResponseBody, filename, contentType, authToken.getAccountId());
                        uploads = Arrays.asList(up);
                    } catch (ServiceException e) {
                        if (e.getCode().equals(MailServiceException.UPLOAD_REJECTED))
                            status = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
                        else
                            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    }
                }

                resp.setStatus(status);
                FileUploadServlet.sendResponse(resp, status, req.getParameter(FORMAT_PARAM), null, uploads, null);
            } else {
                resp.setStatus(status);
                resp.setContentType(contentType);
                for (Header h : httpResp.getAllHeaders())
                    if (canProxyHeader(h.getName()))
                        resp.addHeader(h.getName(), h.getValue());
                if (targetResponseBody != null)
                    ByteUtil.copy(targetResponseBody, true, resp.getOutputStream(), true);
            }
        } finally {
            if (method != null)
                method.releaseConnection();
        }
    }
}
