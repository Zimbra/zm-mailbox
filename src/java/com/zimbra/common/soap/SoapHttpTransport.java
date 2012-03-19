/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dom4j.DocumentException;
import org.dom4j.ElementHandler;
import org.dom4j.io.SAXReader;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.httpclient.HttpProxyConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.ProxyHostConfiguration;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraHttpConnectionManager;

public class SoapHttpTransport extends SoapTransport {
    private HttpClient mClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient();
    private Map<String, String> mCustomHeaders;
    private ProxyHostConfiguration mHostConfig = null;
    private HttpDebugListener mHttpDebugListener;
    private boolean mKeepAlive = defaultKeepAlive;
    private int mRetryCount = defaultRetryCount;
    private int mTimeout = defaultTimeout;
    private String mUri;
    private static boolean defaultKeepAlive = ZimbraHttpConnectionManager.getInternalHttpConnMgr().getKeepAlive();
    private static int defaultRetryCount = LC.httpclient_soaphttptransport_retry_count.intValue();
    private static int defaultTimeout = LC.httpclient_soaphttptransport_so_timeout.intValue();

    public interface HttpDebugListener {
        public void sendSoapMessage(PostMethod postMethod, Element envelope, HttpState httpState);
        public void receiveSoapMessage(PostMethod postMethod, Element envelope);
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
        mHostConfig = HttpProxyConfig.getProxyConfig(mClient.getHostConfiguration(), uri);
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
        if (mClient != null && mClient != ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient()) {
            mClient.getHttpConnectionManager().closeIdleConnections(0);
            mClient = null;
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
    throws IOException, HttpException, ServiceException {
        return invoke(document, raw, noSession, requestedAccountId, changeToken, tokenType, null);
    }

    public Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId,
            String changeToken, String tokenType, ResponseHandler respHandler)
            throws IOException, HttpException, ServiceException {
        PostMethod method = null;

        try {
            // Assemble post method.  Append document name, so that the request
            // type is written to the access log.
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
            uri += getDocumentName(document);
            method = new PostMethod(uri + query);

            // Set user agent if it's specified.
            String agentName = getUserAgentName();
            if (agentName != null) {
                String agentVersion = getUserAgentVersion();
                if (agentVersion != null)
                    agentName += " " + agentVersion;
                method.setRequestHeader(new Header("User-Agent", agentName));
            }

            // the content-type charset will determine encoding used
            // when we set the request body
            method.setRequestHeader("Content-Type", getRequestProtocol().getContentType());
            if (getClientIp() != null)
                method.setRequestHeader(RemoteIP.X_ORIGINATING_IP_HEADER, getClientIp());

            Element soapReq = generateSoapMessage(document, raw, noSession, requestedAccountId, changeToken, tokenType);
            String soapMessage = SoapProtocol.toString(soapReq, getPrettyPrint());
            HttpMethodParams params = method.getParams();

            method.setRequestEntity(new StringRequestEntity(soapMessage, null, "UTF-8"));

            if (getRequestProtocol().hasSOAPActionHeader())
                method.setRequestHeader("SOAPAction", mUri);

            if (mCustomHeaders != null) {
                for (Map.Entry<String, String> entry : mCustomHeaders.entrySet())
                    method.setRequestHeader(entry.getKey(), entry.getValue());
            }

            String host = method.getURI().getHost();
            HttpState state = HttpClientUtil.newHttpState(getAuthToken(), host, false);

            params.setCookiePolicy(state.getCookies().length == 0 ? CookiePolicy.IGNORE_COOKIES : CookiePolicy.BROWSER_COMPATIBILITY);
            params.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(mRetryCount - 1, true));
            params.setSoTimeout(mTimeout);
            params.setVersion(HttpVersion.HTTP_1_1);
            method.setRequestHeader("Connection", mKeepAlive ? "Keep-alive" : "Close");

            if (mHostConfig != null && mHostConfig.getUsername() != null && mHostConfig.getPassword() != null) {
                state.setProxyCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(mHostConfig.getUsername(), mHostConfig.getPassword()));
            }
            
            if (mHttpDebugListener != null) {
                mHttpDebugListener.sendSoapMessage(method, soapReq, state);
            }
            
            int responseCode = mClient.executeMethod(mHostConfig, method, state);
            // SOAP allows for "200" on success and "500" on failure;
            //   real server issues will probably be "503" or "404"
            if (responseCode != HttpServletResponse.SC_OK && responseCode != HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                throw ServiceException.PROXY_ERROR(method.getStatusLine().toString(), uri);

            // Read the response body.  Use the stream API instead of the byte[]
            // version to avoid HTTPClient whining about a large response.
            InputStreamReader reader = new InputStreamReader(method.getResponseBodyAsStream(), SoapProtocol.getCharset());
            String responseStr = "";

            try {
                if (respHandler != null) {
                    respHandler.process(reader);
                    return null;
                } else {
                    responseStr = ByteUtil.getContent(reader, (int) method.getResponseContentLength(), false);
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
                mClient.getHttpConnectionManager().closeIdleConnections(0);
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
            SAXReader reader = com.zimbra.common.soap.Element.getSAXReader();
            for(Map.Entry<String, ElementHandler> entry : handlers.entrySet()) {
                reader.addHandler(entry.getKey(), entry.getValue());
            }

            try {
                reader.read(src);
            } catch (DocumentException e) {
                throw ServiceException.SAX_READER_ERROR(e.getMessage(), e.getCause());
            }
        }
    }
}
