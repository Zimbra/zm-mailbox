/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
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

/*
 * SoapHttpTransport.java
 */

package com.zimbra.common.soap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicHeader;
import org.dom4j.DocumentException;
import org.dom4j.ElementHandler;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.auth.ZJWToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.httpclient.HttpProxyConfig;
import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.ProxyHostConfiguration;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;

public class SoapHttpTransport extends SoapTransport {
    private HttpClientBuilder mClientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient();
    private Map<String, String> mCustomHeaders;
    private ProxyHostConfiguration mHostConfig = null;
    private HttpDebugListener mHttpDebugListener;
    private boolean mKeepAlive = defaultKeepAlive;
    private int mRetryCount = defaultRetryCount;
    private int mTimeout = defaultTimeout;
    private final String mUri;
    private static boolean defaultKeepAlive = LC.httpclient_soaphttptransport_keepalive_connections.booleanValue();
    private static int defaultRetryCount = LC.httpclient_soaphttptransport_retry_count.intValue();
    private static int defaultTimeout = LC.httpclient_soaphttptransport_so_timeout.intValue();

    public interface HttpDebugListener {
        public void sendSoapMessage(HttpPost postMethod, Element envelope, BasicCookieStore httpState);
        public void receiveSoapMessage(HttpPost postMethod, Element envelope);
    }

    @Override public String toString() {
        return "SoapHTTPTransport(uri=" + mUri + ")";
    }

    /**
     * Create a new SoapHttpTransport object for the specified URI
     *
     * @param uri the origin server URL
     */
    public SoapHttpTransport(String uri) {
        super();
        mUri = uri;
        mHostConfig = HttpProxyConfig.getProxyConfig(uri);
    }

    public void setHttpDebugListener(HttpDebugListener listener) {
        mHttpDebugListener = listener;
    }

    public HttpDebugListener getHttpDebugListener() {
        return mHttpDebugListener;
    }

    /**
     * Frees any resources such as connection pool held by this transport.
     */
    public void shutdown() {
        if (mClientBuilder != null && mClientBuilder != ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient()) {
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().closeIdleConnections();
            mClientBuilder = null;
            mHostConfig = null;
        }
    }

    public Map<String, String> getCustomHeaders() {
        if (mCustomHeaders == null)
            mCustomHeaders = new HashMap<String, String>();
        return mCustomHeaders;
    }

    /**
     * Whether to use HTTP keep-alive connections
     *
     * <p> Default value is <code>true</code>.
     */
    public void setKeepAlive(boolean keepAlive) {
        mKeepAlive = keepAlive;
    }


    /**
     * The number of times the invoke method retries
     *
     * <p> Default value is <code>1</code>.
     */
    public void setRetryCount(int newRetryCount) {
        mRetryCount = newRetryCount < 0 ? defaultRetryCount : newRetryCount;
    }

    /**
     * Get the mRetryCount value.
     */
    public int getRetryCount() {
        return mRetryCount;
    }

    /**
     * Sets the number of milliseconds to wait when reading data
     * during a invoke call.
     */
    @Override public void setTimeout(int newTimeout) {
        mTimeout = newTimeout < 0 ? defaultTimeout : newTimeout;
    }

    /**
     * Get the mTimeout value in milliseconds.  The default is specified by
     * the <tt>httpclient_soaphttptransport_so_timeout</tt> localconfig variable.
     */
    public int getTimeout() {
        return mTimeout;
    }

    /**
     *  Gets the URI
     */
    public String getURI() {
        return mUri;
    }

    @Override
    public Element invoke(Element document, boolean raw, boolean noSession,
            String requestedAccountId, String changeToken, String tokenType)
    throws IOException, ServiceException {
        return invoke(document, raw, noSession, requestedAccountId, changeToken, tokenType, NotificationFormat.DEFAULT, null);
    }

    @Override
    public Element invoke(Element document, boolean raw, boolean noSession,
            String requestedAccountId, String changeToken, String tokenType, NotificationFormat nFormat, String curWaitSetID)
    throws IOException, ServiceException {
        return invoke(document, raw, noSession, requestedAccountId, changeToken, tokenType, nFormat, curWaitSetID, null);
    }

    public Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId,
            String changeToken, String tokenType, ResponseHandler respHandler)
            throws IOException, ServiceException {
        return invoke(document, raw, noSession, requestedAccountId, changeToken, tokenType, NotificationFormat.DEFAULT, null, respHandler);
    }

    private String getUriWithPath(Element document) {
        String uri, query;
        int i = mUri.indexOf('?');
        if (i >= 0) {
            uri = mUri.substring(0, i);
            query = mUri.substring(i);
        } else {
            uri = mUri;
            query = "";
        }
        if (!uri.endsWith("/"))
            uri += '/';

        return String.format("%s%s%s", uri, getDocumentName(document), query);
    }

    public Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId,
            String changeToken, String tokenType, NotificationFormat nFormat, String curWaitSetID, ResponseHandler respHandler)
            throws IOException, ServiceException {
        HttpPost method = null;
        HttpClient client = null;

        try {
            // Assemble post method.  Append document name, so that the request
            // type is written to the access log.
            String uri = getUriWithPath(document);
            method = new HttpPost(uri);

            // Set user agent if it's specified.
            String agentName = getUserAgentName();
            if (agentName != null) {
                String agentVersion = getUserAgentVersion();
                if (agentVersion != null)
                    agentName += " " + agentVersion;
                method.addHeader(new BasicHeader("User-Agent", agentName));
            }

            // Set the original user agent if it's specified.
            String originalUserAgent = getOriginalUserAgent();
            if (originalUserAgent != null) {
                method.addHeader(new BasicHeader(HeaderConstants.HTTP_HEADER_ORIG_USER_AGENT, originalUserAgent));
            }

            // the content-type charset will determine encoding used
            // when we set the request body
            method.addHeader("Content-Type", getRequestProtocol().getContentType());
            if (getClientIp() != null) {
                method.addHeader(RemoteIP.X_ORIGINATING_IP_HEADER, getClientIp());
                if (ZimbraLog.misc.isDebugEnabled()) {
                    ZimbraLog.misc.debug("set remote IP header [%s] to [%s]", RemoteIP.X_ORIGINATING_IP_HEADER, getClientIp());
                }
            }
            Element soapReq = generateSoapMessage(document, raw, noSession, requestedAccountId, changeToken, tokenType, nFormat, curWaitSetID);
            String soapMessage = SoapProtocol.toString(soapReq, getPrettyPrint());

            method.setEntity(new StringEntity(soapMessage, ContentType.create(ContentType.APPLICATION_XML.getMimeType(), "UTF-8")));

            if (getRequestProtocol().hasSOAPActionHeader())
                method.addHeader("SOAPAction", mUri);

            if (mCustomHeaders != null) {
                for (Map.Entry<String, String> entry : mCustomHeaders.entrySet())
                    method.addHeader(entry.getKey(), entry.getValue());
            }

            String host = method.getURI().getHost();
            ZAuthToken zToken = getAuthToken();

            BasicCookieStore cookieStore = HttpClientUtil.newHttpState(zToken, host, this.isAdmin());
            String trustedToken = getTrustedToken();
            if (trustedToken != null) {
                BasicClientCookie cookie = new BasicClientCookie(ZimbraCookie.COOKIE_ZM_TRUST_TOKEN, trustedToken);
                cookie.setDomain(host);
                cookie.setPath("/");
                cookie.setSecure(false);
                cookieStore.addCookie(cookie);
            }

            if (zToken instanceof ZJWToken) {
                method.addHeader(Constants.AUTH_HEADER, Constants.BEARER + " " + zToken.getValue());
            }

            ZimbraLog.soap.trace("Httpclient timeout: %s" , mTimeout);
            RequestConfig reqConfig = RequestConfig.custom().
                setCookieSpec(cookieStore.getCookies().size() == 0 ? CookieSpecs.IGNORE_COOKIES:
               CookieSpecs.BROWSER_COMPATIBILITY)
                .setSocketTimeout(mTimeout)
                .build();
            SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(mTimeout)
                .setTcpNoDelay(LC.httpclient_external_connmgr_tcp_nodelay.booleanValue()).build();
            method.setProtocolVersion(HttpVersion.HTTP_1_1);
            method.addHeader("Connection", mKeepAlive ? "Keep-alive" : "Close");


            client = mClientBuilder.setDefaultRequestConfig(reqConfig)
               .setDefaultSocketConfig(socketConfig)
               .setDefaultCookieStore(cookieStore).build();
            ZimbraLog.soap.trace("Httpclient request config timeout: %s" , reqConfig.getSocketTimeout());

            if (mHostConfig != null && mHostConfig.getUsername() != null && mHostConfig.getPassword() != null) {

                Credentials credentials = new UsernamePasswordCredentials(mHostConfig.getUsername(), mHostConfig.getPassword());
                AuthScope authScope = new AuthScope(null, -1);
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(authScope, credentials);
                client = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider)
                    .setDefaultRequestConfig(reqConfig)
                    .setDefaultSocketConfig(socketConfig)
                    .setDefaultCookieStore(cookieStore)
                    .build();
            }

            if (mHttpDebugListener != null) {
                mHttpDebugListener.sendSoapMessage(method, soapReq, cookieStore);
            }

            HttpResponse response = client.execute(method);
            int responseCode = response.getStatusLine().getStatusCode();
            // SOAP allows for "200" on success and "500" on failure;
            //   real server issues will probably be "503" or "404"
            if (responseCode != HttpServletResponse.SC_OK && responseCode != HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                throw ServiceException.PROXY_ERROR(response.getStatusLine().getReasonPhrase(), uri);

            // Read the response body.  Use the stream API instead of the byte[]
            // version to avoid HTTPClient whining about a large response.
            InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), SoapProtocol.getCharset());
            String responseStr = "";

            try {
                if (respHandler != null) {
                    respHandler.process(reader);
                    return null;
                } else {
                    HttpEntity httpEntity = response.getEntity();
                    httpEntity.getContentLength();
                    responseStr = ByteUtil.getContent(reader,  (int)httpEntity.getContentLength(), false);
                    Element soapResp = parseSoapResponse(responseStr, raw);

                    if (mHttpDebugListener != null) {
                        mHttpDebugListener.receiveSoapMessage(method, soapResp);
                    }
                    return soapResp;
                }
            } catch (SoapFaultException x) {
                // attach request/response to the exception and rethrow
                x.setFaultRequest(soapMessage);
                x.setFaultResponse(responseStr.substring(0, Math.min(10240, responseStr.length())));
                throw x;
            }
        } finally {
            // Release the connection to the connection manager
            if (method != null)
                method.releaseConnection();

            // really not necessary if running in the server because the reaper thread
            // of our connection manager will take care it.
            // if called from CLI, all connections will be closed when the CLI
            // exits.  Leave it here anyway.
            if (!mKeepAlive)
                ZimbraHttpConnectionManager.getInternalHttpConnMgr().closeIdleConnections();
        }
    }

    /**
     * Returns the document name.  If the given document is an <tt>Envelope</tt>
     * element, returns the name of the first child of the <tt>Body</tt> subelement.
     */
    private String getDocumentName(Element document) {
        if (document == null || document.getName() == null) {
            return null;
        }
        String name = document.getName();
        if (name.equals("Envelope")) {
            Element body = document.getOptionalElement("Body");
            if (body != null) {
                List<Element> children = body.listElements();
                if (children.size() > 0) {
                    name = children.get(0).getName();
                }
            }
        }
        return name;
    }

    public static interface ResponseHandler {
        void process(Reader src) throws ServiceException;
    }

    /**
     * Use SAXReader to parse large soap response. caller must provide list of handlers, which are <path, handler>
     * pairs. To reduce memory usage, a handler may call Element.detach() in ElementHandler.onEnd() to prune off
     * processed elements.
     */
    public static final class SAXResponseHandler implements ResponseHandler {
        private final Map<String, ElementHandler> handlers;

        public SAXResponseHandler(Map<String, ElementHandler> handlers) {
            this.handlers = handlers;
        }

        @Override
        public void process(Reader src) throws ServiceException {
            SAXReader reader;
            try {
                // This code is slower than less safe code used prior to 8.7 but appears to only be used
                // in ZimbraOffline.  Submitted Bugzilla Bug 104175 suggesting investigating replacing use of this
                // code with more modern, higher performing code.
                reader = W3cDomUtil.getDom4jSAXReaderWhichUsesSecureProcessing();
                for(Map.Entry<String, ElementHandler> entry : handlers.entrySet()) {
                    reader.addHandler(entry.getKey(), entry.getValue());
                }
                reader.read(src);
            } catch (XmlParseException | SAXException | DocumentException e) {
                ZimbraLog.misc.debug("Problem processing XML", e);
                throw ServiceException.SAX_READER_ERROR(e.getMessage(), e.getCause());
            }
        }
    }

    @Override
    public Future<HttpResponse> invokeAsync(Element document, boolean raw, boolean noSession, String requestedAccountId,
            String changeToken, String tokenType, NotificationFormat nFormat, String curWaitSetID, FutureCallback<HttpResponse> cb) throws IOException {
        HttpPost post = new HttpPost(getUriWithPath(document));
        // Set user agent if it's specified.
        String agentName = getUserAgentName();
        if (agentName != null) {
            String agentVersion = getUserAgentVersion();
            if (agentVersion != null)
                agentName += " " + agentVersion;
            post.setHeader("User-Agent", agentName);
        }

        // request headers
        post.setHeader("Content-Type", getRequestProtocol().getContentType());
        if (getClientIp() != null) {
            post.setHeader(RemoteIP.X_ORIGINATING_IP_HEADER, getClientIp());
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("set remote IP header [%s] to [%s]", RemoteIP.X_ORIGINATING_IP_HEADER, getClientIp());
            }
        }
        if (getRequestProtocol().hasSOAPActionHeader())
            post.setHeader("SOAPAction", mUri);

        if (mCustomHeaders != null) {
            for (Map.Entry<String, String> entry : mCustomHeaders.entrySet())
                post.setHeader(entry.getKey(), entry.getValue());
        }

        //SOAP message
        Element soapReq = generateSoapMessage(document, raw, noSession, requestedAccountId, changeToken, tokenType, nFormat, curWaitSetID);
        String soapMessage = SoapProtocol.toString(soapReq, getPrettyPrint());
        post.setEntity(new ByteArrayEntity(soapMessage.getBytes("UTF-8")));
        HttpClientContext context = HttpClientContext.create();
        String host = post.getURI().getHost();
        CookieStore cookieStore = HttpClientUtil.newCookieStore(getAuthToken(), host, isAdmin());
        String trustedToken = getTrustedToken();
        if (trustedToken != null) {
            BasicClientCookie cookie = new BasicClientCookie(ZimbraCookie.COOKIE_ZM_TRUST_TOKEN, trustedToken);
            cookie.setDomain(post.getURI().getHost());
            cookie.setPath("/");
            cookie.setSecure(false);
            cookieStore.addCookie(cookie);
        }
        context.setCookieStore(cookieStore);
        CloseableHttpAsyncClient httpClient = ZimbraHttpClientManager.getInstance().getInternalAsyncHttpClient();
        return httpClient.execute(post, context, cb);
    }

}
