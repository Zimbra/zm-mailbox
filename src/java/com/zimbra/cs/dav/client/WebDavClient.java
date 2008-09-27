/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.dav.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.service.UserServlet.HttpInputStream;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.NetUtil;

public class WebDavClient {

	public static final String UA = "ZCS/1.0 (" + BuildInfo.VERSION + ")";

	public WebDavClient(String baseUrl) {
		mBaseUrl = baseUrl;
		mClient = new HttpClient();
		NetUtil.configureProxy(mClient);
		mUserAgent = UA;
	}
	
	public WebDavClient(String baseUrl, String app) {
		this(baseUrl);
		mUserAgent = UA + " " + app;
	}
	
	public Collection<DavObject> listObjects(String path, Collection<QName> extraProps) throws IOException, DavException {
		DavRequest propfind = DavRequest.PROPFIND(path);
		propfind.setDepth(Depth.one);
		propfind.addRequestProp(DavElements.E_DISPLAYNAME);
		propfind.addRequestProp(DavElements.E_RESOURCETYPE);
		propfind.addRequestProp(DavElements.E_CREATIONDATE);
		propfind.addRequestProp(DavElements.E_GETCONTENTLENGTH);
		propfind.addRequestProp(DavElements.E_GETCONTENTLANGUAGE);
		propfind.addRequestProp(DavElements.E_GETCONTENTTYPE);
		propfind.addRequestProp(DavElements.E_GETETAG);
		propfind.addRequestProp(DavElements.E_GETLASTMODIFIED);
		if (extraProps != null)
			for (QName p : extraProps)
				propfind.addRequestProp(p);
		return sendMultiResponseRequest(propfind);
	}
	
	public Collection<DavObject> sendMultiResponseRequest(DavRequest req) throws IOException, DavException {
		ArrayList<DavObject> ret = new ArrayList<DavObject>();
		
		HttpMethod m = null;
		try {
			m = execute(req);
			Document doc = new SAXReader().read(m.getResponseBodyAsStream());
			
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
		HttpMethod m = execute(req);
		return new HttpInputStream(m);
	}
	
	public HttpInputStream sendGet(String href) throws IOException {
		GetMethod get = new GetMethod(mBaseUrl + href);
		executeMethod(get, Depth.zero);
		return new HttpInputStream(get);
	}
	
	public HttpInputStream sendPut(String href, byte[] buf, String contentType, String etag) throws IOException {
		PutMethod put = new PutMethod(mBaseUrl + href);
		put.setRequestEntity(new ByteArrayRequestEntity(buf, contentType));
		if (ZimbraLog.dav.isDebugEnabled() && contentType.startsWith("text"))
			ZimbraLog.dav.debug("PUT payload: \n"+new String(buf, "UTF-8"));
		if (etag != null)
			put.setRequestHeader(DavProtocol.HEADER_IF_MATCH, etag);
		executeMethod(put, Depth.zero);
		return new HttpInputStream(put);
	}
	
	protected HttpMethod execute(DavRequest req) throws IOException {
		if (ZimbraLog.dav.isDebugEnabled())
			ZimbraLog.dav.debug("Request payload: \n"+req.getRequestMessageString());
		return executeMethod(req.getHttpMethod(mBaseUrl), req.getDepth());
	}
	protected HttpMethod executeMethod(HttpMethod m, Depth d) throws IOException {
		ZimbraLog.dav.debug("WebDAV request (depth="+d+"): "+m.getPath());

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
		mClient.executeMethod(m);
		if (ZimbraLog.dav.isDebugEnabled())
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
	
	public String getUsername() {
		return mUsername;
	}
	public String getPassword() {
		return mPassword;
	}
	
	private String mUserAgent;
	private String mBaseUrl;
	private String mUsername;
	private String mPassword;
	private HttpClient mClient;
}
