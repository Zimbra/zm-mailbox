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
import org.apache.commons.httpclient.methods.PostMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DomUtil;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.NetUtil;

public class WebDavClient {

	public static final String UA = "ZCS/1.0 (" + BuildInfo.VERSION + ")";

	public WebDavClient(String baseUrl) {
		mBaseUrl = baseUrl;
		mClient = new HttpClient();
		NetUtil.configureProxy(mClient);
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
	
	public byte[] sendRequest(DavRequest req) throws IOException, DavException {
		HttpMethod m = null;
		try {
			m = execute(req);
			return m.getResponseBody();
		} finally {
			if (m != null)
				m.releaseConnection();
		}
	}
	
	protected HttpMethod execute(DavRequest req) throws IOException, DavException {
		final String methodString = req.getMethod();
    	PostMethod m = new PostMethod(mBaseUrl + req.getUri()) {
    		public String getName() {
    			return methodString;
    		}
    	};
		m.setDoAuthentication(true);
		m.setRequestHeader("User-Agent", UA);
		if (req.getDepth() == Depth.one)
			m.setRequestHeader("Depth", "1");
		byte[] buf = DomUtil.getBytes(req.getRequestMessage());
		if (ZimbraLog.dav.isDebugEnabled())
			ZimbraLog.dav.debug("CalDAV request:\n"+new String(buf, "UTF-8"));
		ByteArrayRequestEntity re = new ByteArrayRequestEntity(buf, "text/xml");
		m.setRequestEntity(re);
		mClient.executeMethod(m);

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
	
	private String mBaseUrl;
	private String mUsername;
	private String mPassword;
	private HttpClient mClient;
}
