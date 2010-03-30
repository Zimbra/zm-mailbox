/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.dav.client;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavContext.Depth;
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
	
	public Collection<DavObject> listObjects(String path, Collection<QName> extraProps) throws IOException, DavException {
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
	
	public Collection<DavObject> sendMultiResponseRequest(DavRequest req) throws IOException, DavException {
		ArrayList<DavObject> ret = new ArrayList<DavObject>();
		
		HttpMethod m = null;
		try {
			m = executeFollowRedirect(req);
			int status = m.getStatusCode();
			if (status >= 400)
				throw new DavException("DAV server returned an error: "+status, status);
			
			Document doc = com.zimbra.common.soap.Element.getSAXReader().read(m.getResponseBodyAsStream());
			Element top = doc.getRootElement();
			for (Object obj : top.elements(DavElements.E_RESPONSE)) {
				if (obj instanceof Element) {
					ret.add(new DavObject((Element)obj));
				}
			}
		} catch (DocumentException e) {
			throw new DavException("can't parse response", e);
		} finally {
			if (m != null)
				m.releaseConnection();
		}
		return ret;
	}
	
	public HttpInputStream sendRequest(DavRequest req) throws IOException, DavException {
		HttpMethod m = executeFollowRedirect(req);
		return new HttpInputStream(m);
	}
	
	public HttpInputStream sendGet(String href) throws IOException {
		GetMethod get = new GetMethod(mBaseUrl + href);
		executeMethod(get, Depth.zero);
		return new HttpInputStream(get);
	}
	
	public HttpInputStream sendPut(String href, byte[] buf, String contentType, String etag, Collection<Pair<String,String>> headers) throws IOException {
		boolean done = false;
		PutMethod put = null;
		while (!done) {
			put = new PutMethod(mBaseUrl + href);
			put.setRequestEntity(new ByteArrayRequestEntity(buf, contentType));
			if (mDebugEnabled && contentType.startsWith("text"))
				ZimbraLog.dav.debug("PUT payload: \n"+new String(buf, "UTF-8"));
			if (etag != null)
				put.setRequestHeader(DavProtocol.HEADER_IF_MATCH, etag);
			if (headers != null)
				for (Pair<String,String> h : headers)
					put.addRequestHeader(h.getFirst(), h.getSecond());
			executeMethod(put, Depth.zero);
			int ret = put.getStatusCode();
			if (ret == HttpStatus.SC_MOVED_PERMANENTLY || ret == HttpStatus.SC_MOVED_TEMPORARILY) {
				Header newLocation = put.getResponseHeader("Location");
				if (newLocation != null) {
					href = newLocation.getValue();
					ZimbraLog.dav.debug("redirect to new url = "+href);
					put.releaseConnection();
					continue;
				}
			}
			done = true;
		}
		return new HttpInputStream(put);
	}
	
	protected HttpMethod executeFollowRedirect(DavRequest req) throws IOException {
		HttpMethod method = null;
		boolean done = false;
		while (!done) {
			method = execute(req);
			int ret = method.getStatusCode();
			if (ret == HttpStatus.SC_MOVED_PERMANENTLY || ret == HttpStatus.SC_MOVED_TEMPORARILY) {
				Header newLocation = method.getResponseHeader("Location");
				if (newLocation != null) {
					String uri = newLocation.getValue();
					ZimbraLog.dav.debug("redirect to new url = "+uri);
					method.releaseConnection();
					req.setRedirectUrl(uri);
					continue;
				}
			}
			done = true;
		}
		return method;
	}
	
	protected HttpMethod execute(DavRequest req) throws IOException {
		if (mDebugEnabled)
			ZimbraLog.dav.debug("Request payload: \n"+req.getRequestMessageString());
		HttpMethod m = req.getHttpMethod(mBaseUrl);
        for (Pair<String,String> header : req.getRequestHeaders()) {
            m.addRequestHeader(header.getFirst(), header.getSecond());
        }
		return executeMethod(m, req.getDepth());
	}
	protected HttpMethod executeMethod(HttpMethod m, Depth d) throws IOException {
		ZimbraLog.dav.debug("WebDAV request (depth="+d+"): "+m.getPath());

		HttpMethodParams p = m.getParams();
		if ( p != null )
			p.setCredentialCharset("UTF-8");
		
		m.setDoAuthentication(true);
		m.setRequestHeader("User-Agent", mUserAgent);
		String depth = "0";
		switch (d) {
		case one:
			depth = "1";
			break;
		case infinity:
			depth = "infinity";
			break;
		}
		m.setRequestHeader("Depth", depth);
		HttpClientUtil.executeMethod(mClient, m);
		if (mDebugEnabled && m.getResponseBody() != null)
			ZimbraLog.dav.debug("WebDAV response:\n"+new String(m.getResponseBody(), "UTF-8"));

        return m;
	}
	
	public void setCredential(String user, String pass) {
		mUsername = user;
		mPassword = pass;
		HttpState state = new HttpState();
		Credentials cred = new UsernamePasswordCredentials(mUsername, mPassword);
		state.setCredentials(AuthScope.ANY, cred);
		mClient.setState(state);
		ArrayList<String> authPrefs = new ArrayList<String>();
		authPrefs.add(AuthPolicy.BASIC);
		mClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
		mClient.getParams().setAuthenticationPreemptive(true);
	}
	
	public void setAuthCookie(ZAuthToken auth) {
        Map<String, String> cookieMap = auth.cookieMap(false);
        if (cookieMap != null) {
        	String host = null;
        	try {
        		host = new URL(mBaseUrl).getHost();
        	} catch (Exception e) {
        	}
            HttpState state = new HttpState();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                state.addCookie(new org.apache.commons.httpclient.Cookie(host, ck.getKey(), ck.getValue(), "/", null, false));
            }
            mClient.setState(state);
            mClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
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
	
	private String mUserAgent;
	private String mBaseUrl;
	private String mUsername;
	private String mPassword;
	private HttpClient mClient;
	private boolean mDebugEnabled = false;
}
