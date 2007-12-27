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

/*
 * SoapHttpTransport.java
 */

package com.zimbra.common.soap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import com.zimbra.common.util.ByteUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 */

public class SoapHttpTransport extends SoapTransport {

    public static final String X_ORIGINATING_IP = "X-Originating-IP";
    
    private boolean mKeepAlive;
    private int mRetryCount;
    private int mTimeout;
    private String mUri;
	private HttpClient mClient;
    
    public String toString() { 
        return "SoapHTTPTransport(uri="+mUri+")";
    }

    private static final HttpClientParams sDefaultParams = new HttpClientParams();
        static {
            // we're doing the retry logic at the SoapHttpTransport level, so don't do it at the HttpClient level as well
            sDefaultParams.setParameter(HttpMethodParams.RETRY_HANDLER, new HttpMethodRetryHandler() {
                public boolean retryMethod(HttpMethod method, IOException exception, int executionCount)  { return false; }
            });
        }

    /**
     * Create a new SoapHttpTransport object for the specified URI.
     * Supported schemes are http and https. The connection
     * is not made until invoke or connect is called.
     *
     * Multiple threads using this transport must do their own
     * synchronization.
     */
    public SoapHttpTransport(String uri) {
    	this(uri, null, 0);
    }
    
    /**
     * Create a new SoapHttpTransport object for the specified URI, with specific proxy information.
     * 
     * @param uri the origin server URL
     * @param proxyHost hostname of proxy
     * @param proxyPort port of proxy
     */
    public SoapHttpTransport(String uri, String proxyHost, int proxyPort) {
    	this(uri, proxyHost, proxyPort, null, null);
    }
    
    /**
     * Create a new SoapHttpTransport object for the specified URI, with specific proxy information including
     * proxy auth credentials.
     * 
     * @param uri the origin server URL
     * @param proxyHost hostname of proxy
     * @param proxyPort port of proxy
     * @param proxyUser username for proxy auth
     * @param proxyPass password for proxy auth
     */
    public SoapHttpTransport(String uri, String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
    	super();
    	mClient = new HttpClient(sDefaultParams);
    	commonInit(uri);
    	
    	if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0) {
    		mClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
    		if (proxyUser != null && proxyUser.length() > 0 && proxyPass != null && proxyPass.length() > 0) {
    			mClient.getState().setProxyCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPass));
    		}
    	}
    }

    /**
     * Creates a new SoapHttpTransport that supports multiple connections
     * to the specified URI.  Multiple threads can call the invoke()
     * method safely without synchronization.
     *
     * @param uri
     * @param maxConnections Note RFC2616 recommends the default of 2.
     */
    public SoapHttpTransport(String uri, int maxConnections, boolean connectionStaleCheckEnabled) {
    	super();
    	MultiThreadedHttpConnectionManager connMgr = new MultiThreadedHttpConnectionManager();
    	connMgr.setMaxConnectionsPerHost(maxConnections);
    	connMgr.setConnectionStaleCheckingEnabled(connectionStaleCheckEnabled);
    	mClient = new HttpClient(sDefaultParams, connMgr);
    	commonInit(uri);
    }

    /**
     * Frees any resources such as connection pool held by this transport.
     */
    public void shutdown() {
    	HttpConnectionManager connMgr = mClient.getHttpConnectionManager();
    	if (connMgr instanceof MultiThreadedHttpConnectionManager) {
    		MultiThreadedHttpConnectionManager multiConnMgr = (MultiThreadedHttpConnectionManager) connMgr;
    		multiConnMgr.shutdown();
    	}
    	mClient = null;
    }

    private void commonInit(String uri) {
        mUri = uri;
        mKeepAlive = false;
        mRetryCount = 3;
        setTimeout(0);
    }

    /**
     *  Gets the URI
     */
    public String getURI() {
        return mUri;
    }
    
    /**
     * The number of times the invoke method retries when it catches a 
     * RetryableIOException.
     *
     * <p> Default value is <code>3</code>.
     */
    public void setRetryCount(int retryCount) {
        this.mRetryCount = retryCount;
    }


    /**
     * Get the mRetryCount value.
     */
    public int getRetryCount() {
        return mRetryCount;
    }

    /**
     * Whether or not to keep the connection alive in between
     * invoke calls.
     *
     * <p> Default value is <code>false</code>.
     */
    private void setKeepAlive(boolean keepAlive) {
        this.mKeepAlive = keepAlive;
    }

    /**
     * Get the mKeepAlive value.
     */
    private boolean getKeepAlive() {
        return mKeepAlive;
    }

    /**
     * The number of miliseconds to wait when connecting or reading
     * during a invoke call. 
     * <p>
     * Default value is <code>0</code>, which means no mTimeout.
     */
    public void setTimeout(int timeout) {
        mTimeout = timeout;
        mClient.setConnectionTimeout(mTimeout);
        mClient.setTimeout(mTimeout);
    }

    /**
     * Get the mTimeout value.
     */
    public int getTimeout() {
        return mTimeout;
    }

    public Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId, String changeToken, String tokenType) 
	throws SoapFaultException, IOException, HttpException {
    	int statusCode = -1;

        PostMethod method = null;
        try {
            // the content-type charset will determine encoding used
            // when we set the request body
            method = new PostMethod(mUri);
            method.setRequestHeader("Content-Type", getRequestProtocol().getContentType());
            if (getClientIp() != null)
            method.setRequestHeader(X_ORIGINATING_IP, getClientIp());

            String soapMessage = generateSoapMessage(document, raw, noSession, requestedAccountId, changeToken, tokenType);
            method.setRequestBody(soapMessage);
            method.setRequestContentLength(EntityEnclosingMethod.CONTENT_LENGTH_AUTO);
    	
            if (getRequestProtocol().hasSOAPActionHeader())
                method.setRequestHeader("SOAPAction", mUri);

            for (int attempt = 0; statusCode == -1 && attempt < mRetryCount; attempt++) {
                try {
                    // execute the method.
                    statusCode = mClient.executeMethod(method);
                } catch (HttpRecoverableException e) {
                    if (attempt == mRetryCount - 1)
                        throw e;
                    System.err.println("A recoverable exception occurred, retrying." + e.getMessage());
                }
            }

            // Read the response body.  Use the stream API instead of the byte[] one
            // to avoid HTTPClient whining about a large response.
            byte[] responseBody = ByteUtil.getContent(method.getResponseBodyAsStream(), (int) method.getResponseContentLength());

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            String responseStr = SoapProtocol.toString(responseBody);

            try {
            	return parseSoapResponse(responseStr, raw);
            } catch (SoapFaultException x) {
            	//attach request/response to the exception and rethrow for downstream consumption
            	x.setFaultRequest(soapMessage);
            	x.setFaultResponse(responseStr);
            	throw x;
            }
        } finally {
            // Release the connection.
            if (method != null)
                method.releaseConnection();        
        }
    }

}

/*
 * TODOs:
 * retry?
 * validation
 */
