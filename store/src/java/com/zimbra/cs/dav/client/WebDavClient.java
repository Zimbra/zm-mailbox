/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.dav.client;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.service.UserServlet.HttpInputStream;
import com.zimbra.cs.util.BuildInfo;

public class WebDavClient {

    public WebDavClient(String baseUrl) {
        this(baseUrl, "ZCS");
    }

    public WebDavClient(String baseUrl, String app) {
        mBaseUrl = baseUrl;
        mClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(mClient);
        setAppName(app);
    }

    public Collection<DavObject> listObjects(String path, Collection<QName> extraProps) throws IOException, DavException, HttpException {
        DavRequest propfind = DavRequest.PROPFIND(path);
        propfind.setDepth(Depth.one);
        if (extraProps == null) {
            propfind.addRequestProp(DavElements.E_DISPLAYNAME);
            propfind.addRequestProp(DavElements.E_RESOURCETYPE);
            propfind.addRequestProp(DavElements.E_CREATIONDATE);
            propfind.addRequestProp(DavElements.E_GETCONTENTLENGTH);
            propfind.addRequestProp(DavElements.E_GETCONTENTLANGUAGE);
            propfind.addRequestProp(DavElements.E_GETCONTENTTYPE);
            propfind.addRequestProp(DavElements.E_GETETAG);
            propfind.addRequestProp(DavElements.E_GETLASTMODIFIED);
        } else {
            for (QName p : extraProps)
                propfind.addRequestProp(p);
        }
        return sendMultiResponseRequest(propfind);
    }

    public Collection<DavObject> sendMultiResponseRequest(DavRequest req) throws IOException, DavException, HttpException {
        ArrayList<DavObject> ret = new ArrayList<DavObject>();

        HttpResponse response = null;
        try {
            response = executeFollowRedirect(req);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 400) {
                throw new DavException("DAV server returned an error: "+status, status);
            }

            Document doc = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(response.getEntity().getContent());
            Element top = doc.getRootElement();
            for (Object obj : top.elements(DavElements.E_RESPONSE)) {
                if (obj instanceof Element) {
                    ret.add(new DavObject((Element)obj));
                }
            }
        } catch (XmlParseException e) {
            throw new DavException("can't parse response", e);
        } finally {
            if (response != null) {
                EntityUtils.consume(response.getEntity());
            }
        }
        return ret;
    }

    public HttpInputStream sendRequest(DavRequest req) throws IOException, DavException, HttpException {
        HttpResponse response = executeFollowRedirect(req);
        return new HttpInputStream(response);
    }

    public HttpInputStream sendGet(String href) throws IOException, HttpException {
        HttpGet get = new HttpGet(mBaseUrl + href);
        HttpResponse response = executeMethod(get, Depth.zero);
        return new HttpInputStream(response);
    }

    public HttpInputStream sendPut(String href, byte[] buf, String contentType, String etag, Collection<Pair<String,String>> headers) throws IOException, HttpException {
        boolean done = false;
        HttpResponse response = null;
        while (!done) {
            HttpPut put = new HttpPut(mBaseUrl + href);
            put.setEntity(new ByteArrayEntity(buf, ContentType.create(contentType)));
            if (mDebugEnabled && contentType.startsWith("text"))
                ZimbraLog.dav.debug("PUT payload: \n"+new String(buf, "UTF-8"));
            if (etag != null)
                put.addHeader(DavProtocol.HEADER_IF_MATCH, etag);
            if (headers != null)
                for (Pair<String,String> h : headers)
                    put.addHeader(h.getFirst(), h.getSecond());
            response = executeMethod(put, Depth.zero);
            int ret = response.getStatusLine().getStatusCode();
            if (ret == HttpStatus.SC_MOVED_PERMANENTLY || ret == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header newLocation = response.getFirstHeader("Location");
                if (newLocation != null) {
                    href = newLocation.getValue();
                    ZimbraLog.dav.debug("redirect to new url = "+href);
                    put.releaseConnection();
                    continue;
                }
            }
            done = true;
        }
        return new HttpInputStream(response);
    }

    protected HttpResponse executeFollowRedirect(DavRequest req) throws IOException, HttpException {
        HttpResponse response = null;
        boolean done = false;
        while (!done) {
            response = execute(req);
            int ret = response.getStatusLine().getStatusCode();
            if (ret == HttpStatus.SC_MOVED_PERMANENTLY || ret == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header newLocation = response.getFirstHeader("Location");
                if (newLocation != null) {
                    String uri = newLocation.getValue();
                    ZimbraLog.dav.debug("redirect to new url = "+uri);
                    EntityUtils.consume(response.getEntity());
                    req.setRedirectUrl(uri);
                    continue;
                }
            }
            done = true;
        }
        return response;
    }

    protected void logRequestInfo(HttpRequestBase method, String body) throws IOException {
        if (!mDebugEnabled) {
            return;
        }
        StringBuilder reqLog = new StringBuilder();
        reqLog.append("WebDAV request:\n").append(method.getMethod()).append(" ").append(method.getURI().toString());
        reqLog.append('\n');
        Header headers[] = method.getAllHeaders();
        if (headers != null && headers.length > 0) {
            for (Header hdr : headers) {
                String hdrName = hdr.getName();
                reqLog.append(hdrName).append('=');
                if (hdrName.contains("Auth")  || (hdrName.contains(HttpHeaders.COOKIE))) {
                    reqLog.append("*** REPLACED ***\n");
                } else {
                    reqLog.append(hdr.getValue()).append('\n');
                }
            }
        }
        if (Strings.isNullOrEmpty(body) || !ZimbraLog.dav.isTraceEnabled()) {
            ZimbraLog.dav.debug(reqLog.toString());
        } else {
            ZimbraLog.dav.debug("%s\n%s", reqLog.toString(), body);
        }
    }

    protected void logResponseInfo(HttpResponse response) throws IOException {
        if (!mDebugEnabled) {
            return;
        }
        StringBuilder responseLog = new StringBuilder();
        responseLog.append("WebDAV response:\n").append(response.getStatusLine()).append('\n');
        Header headers[] = response.getAllHeaders();
        if (headers != null && headers.length > 0) {
            for (Header hdr : headers) {
                String hdrName = hdr.getName();
                responseLog.append(hdrName).append('=');
                if (hdrName.contains("Auth")  || (hdrName.contains(HttpHeaders.COOKIE))) {
                    responseLog.append("*** REPLACED ***\n");
                } else {
                    responseLog.append(hdr.getValue()).append('\n');
                }
            }
        }
        if (response.getEntity() == null || !ZimbraLog.dav.isTraceEnabled()) {
            ZimbraLog.dav.debug(responseLog.toString());
        } else {
            ZimbraLog.dav.debug("%s\n%s", responseLog.toString(), new String(EntityUtils.toByteArray(response.getEntity()), "UTF-8"));
        }
    }

    protected HttpResponse execute(DavRequest req) throws IOException, HttpException {
        HttpRequestBase m = req.getHttpMethod(mBaseUrl);
        for (Pair<String,String> header : req.getRequestHeaders()) {
            m.addHeader(header.getFirst(), header.getSecond());
        }
        return executeMethod(m, req.getDepth(), req.getRequestMessageString());
    }

    protected HttpResponse executeMethod(HttpRequestBase m, Depth d, String bodyForLogging) throws IOException, HttpException {
        
          
        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
            .register(AuthSchemes.BASIC, new BasicSchemeFactory(Consts.UTF_8)).build();
       mClient.setDefaultAuthSchemeRegistry(authSchemeRegistry);
       HttpClient client = mClient.build();
        m.addHeader("User-Agent", mUserAgent);
        String depth = "0";
        switch (d) {
        case one:
            depth = "1";
            break;
        case infinity:
            depth = "infinity";
            break;
        case zero:
            break;
        default:
            break;
        }
        m.addHeader("Depth", depth);
        logRequestInfo(m, bodyForLogging);
        HttpResponse response = client.execute(m, context);
        logResponseInfo(response);

        return response;
    }

    protected HttpResponse executeMethod(HttpRequestBase m, Depth d) throws IOException, HttpException {
        return executeMethod(m, d, null);
    }

    public void setCredential(String user, String pass, String targetUrl) {
        mUsername = user;
        mPassword = pass;
        Credentials cred = new UsernamePasswordCredentials(mUsername, mPassword);
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, cred);
        
        HttpHost targetHost = new HttpHost(targetUrl);
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());
         
        // Add AuthCache to the execution context
        context = HttpClientContext.create();
        context.setCredentialsProvider(provider);
        context.setAuthCache(authCache);
        
    }

    public void setAuthCookie(ZAuthToken auth) {
        Map<String, String> cookieMap = auth.cookieMap(false);
        if (cookieMap != null) {
            String host = null;
            try {
                host = new URL(mBaseUrl).getHost();
            } catch (Exception e) {
            }
            BasicCookieStore cookieStore = new BasicCookieStore();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                cookie.setDomain(host);
                cookie.setPath("/");
                cookie.setSecure(false);
                cookieStore.addCookie(cookie);
                
            }
            mClient.setDefaultCookieStore(cookieStore);
            RequestConfig reqConfig = RequestConfig.copy(
                ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
                .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

            mClient.setDefaultRequestConfig(reqConfig);
        }
    }

    public String getUsername() {
        return mUsername;
    }
    public String getPassword() {
        return mPassword;
    }

    public void setDebugEnabled(boolean b) {
        mDebugEnabled = b;
    }

    public void setUserAgent(String ua) {
        mUserAgent = ua;
    }

    public void setAppName(String app) {
        mUserAgent = "Zimbra " + app + "/" + BuildInfo.VERSION + " (" + BuildInfo.DATE + ")";
    }

    protected String mUserAgent;
    private final String mBaseUrl;
    private String mUsername;
    private String mPassword;
    protected final HttpClientBuilder  mClient;
    private boolean mDebugEnabled = false;
    private  HttpClientContext context;
}
