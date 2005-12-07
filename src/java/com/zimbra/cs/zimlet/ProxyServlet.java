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
package com.zimbra.cs.zimlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.ZimbraLog;

/**
 * 
 * @author jylee
 *
 */
public class ProxyServlet extends ZimbraServlet {
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	private static final String CONTENT_ENCODING = "Content-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	
	private static final String TARGET_PARAM = "target";
	private static final String USER_PARAM = "user";
	private static final String PASS_PARAM = "pass";
	private static final String AUTH_PARAM = "auth";
	private static final String AUTH_BASIC = "basic";
	
	private static final boolean COMPRESS_CONTENTS = true;
	private static final long CACHE_TTL = 3600 * 1000;  // 1 hour
	
	private Map<Integer,URLContents> mContentCache;
	
	public void init() throws ServletException {
		super.init();
		mContentCache = new HashMap<Integer,URLContents>();
	}
	
	private Set<String> getAllowedDomains(AuthToken auth) throws ServiceException {
		return Provisioning.getInstance().getAccountById(auth.getAccountId()).getCOS().getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
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
	
	private static class URLContents {
		public String url;
		public String contentType;
		public boolean isCompressed;
		public byte[] data;
		public int returnCode;
		public long createTime;

		public URLContents(URL url, int error) {
			this.url = url.toString();
			this.returnCode = error;
			this.createTime = System.currentTimeMillis();
		}
		public URLContents(URLConnection conn, boolean compress) throws IOException {
			this.url = conn.getURL().toString();
			this.contentType = conn.getContentType();
			this.isCompressed = compress;
			this.returnCode = HttpServletResponse.SC_OK;
			this.createTime = System.currentTimeMillis();
			InputStream stream = null;
			if (conn instanceof HttpURLConnection) {
				HttpURLConnection hconn = (HttpURLConnection) conn;
				
				this.returnCode = hconn.getResponseCode();
				if (returnCode != HttpServletResponse.SC_OK) {
					stream = hconn.getErrorStream();
				}
			}
			if (stream == null) stream = conn.getInputStream();
			byte[] rawData = ByteUtil.getContent(stream, conn.getContentLength());
			//ZimbraLog.zimlet.info("******\n"+new String(rawData)+"\n******");
			if (compress) {
				this.data = ByteUtil.compress(rawData);
			} else {
				this.data = rawData;
			}
		}
	}

	private URLContents checkCachedURLContent(URL url) {
		int key = url.hashCode();
		URLContents content = mContentCache.get(key);
		if (content != null) {
			long now = System.currentTimeMillis();
			if (content.createTime + CACHE_TTL < now) {
				mContentCache.remove(key);
				content = null;
			}
		}
		return content;
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
			header.equals("user-agent") ||
			header.equals("cache-control") ||
			header.equals("cookie")) {
			return false;
		}
		return true;
	}
	
	private void copyPostedData(HttpServletRequest req, HttpURLConnection conn) throws IOException {
		if (req.getMethod().equals("GET") || req.getContentLength() <= 0) {
			return;
		}
		conn.setDoOutput(true);
		ByteUtil.copy(req.getInputStream(), conn.getOutputStream());
	}
	
	private URLContents fetchURLContent(HttpServletRequest req, URL url) throws IOException {
		URLConnection conn = url.openConnection();
		
		// handle basic auth
		String auth, user, pass;
		auth = req.getParameter(AUTH_PARAM);
		user = req.getParameter(USER_PARAM);
		pass = req.getParameter(PASS_PARAM);
		if (auth != null && user != null && pass != null) {
			if (!auth.equals(AUTH_BASIC)) {
				ZimbraLog.zimlet.info("unsupported auth type: "+auth);
				return new URLContents(url, HttpServletResponse.SC_BAD_REQUEST);
			}
			String cred = user + ":" + pass;
			conn.setRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(cred.getBytes())));
		}
		
		Enumeration headers = req.getHeaderNames();
		while (headers.hasMoreElements()) {
			String hdr = (String) headers.nextElement();
			if (canProxyHeader(hdr)) {
				//ZimbraLog.zimlet.info(hdr + ": " + req.getHeader(hdr));
				conn.setRequestProperty(hdr, req.getHeader(hdr));
			}
		}
		if (conn instanceof HttpURLConnection) {
			HttpURLConnection httpconn = (HttpURLConnection) conn;
			
			httpconn.setRequestMethod(req.getMethod());
			copyPostedData(req, httpconn);
			int status = httpconn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				ZimbraLog.zimlet.info("remote host returned error on proxy request: "+status);
			}
		}

		return new URLContents(conn, COMPRESS_CONTENTS);
	}
	
	private Set<String> getCacheableContentTypes(AuthToken auth) throws ServiceException {
		return Provisioning.getInstance().getAccountById(auth.getAccountId()).getCOS().getMultiAttrSet(Provisioning.A_zimbraProxyCacheableContentTypes);
	}
	
	private boolean canCacheProxyContent(HttpServletRequest req, URL url, URLContents content, AuthToken authToken) 
			throws ServiceException {
		if (content.contentType == null) {
			return false;
		}
		// don't cache protected resources.
		String auth = req.getParameter(AUTH_PARAM);
		if (auth != null) {
			return false;
		}
		// cache only the approved content types
		Set<String> contentTypes = getCacheableContentTypes(authToken);
		for (String ct : contentTypes) {
			if (content.contentType.equalsIgnoreCase(ct)) {
				return true;
			}
		}
		return false;
	}
	
	private URLContents getURLContent(HttpServletRequest req, URL url, AuthToken authToken) 
			throws IOException {
		// check the cache first
		URLContents content = checkCachedURLContent(url);
		if (content == null) {
			
			// fetch from the internet
			content = fetchURLContent(req, url);

			try {
				// cache the result for later use
				if (canCacheProxyContent(req, url, content, authToken)) {
					ZimbraLog.zimlet.info("adding new proxy cache content for " + url.toString());
					mContentCache.put(url.hashCode(), content);
				}
			} catch (ServiceException se) {
				ZimbraLog.zimlet.info("error trying to cache the proxy content");
			}
		}
		return content;
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		doProxy(req, resp);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		doProxy(req, resp);
	}

	private void doProxy(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        AuthToken authToken = getAuthTokenFromCookie(req, resp);
        if (authToken == null) {
        	return;
        }
        
        // sanity check
		String target = req.getParameter(TARGET_PARAM);
		if (target == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		URL url = new URL(target);
		
		// check for permission
		if (!checkPermissionOnTarget(req, url, authToken)) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		// fetch the contents
		URLContents content = getURLContent(req, url, authToken);
		if (content.returnCode != HttpServletResponse.SC_OK) {
			resp.setStatus(content.returnCode);
		}
		
		InputStream data = new ByteArrayInputStream(content.data);

		// uncompress the result if the browser doesn't accept gzip encoding
		if (content.isCompressed) {
			String acceptEncoding = req.getHeader(ACCEPT_ENCODING);
			if (acceptEncoding == null || acceptEncoding.indexOf(ENCODING_GZIP) == -1) {
				// uncompress
				ZimbraLog.zimlet.debug("compression not supported");
				data = new GZIPInputStream(data);
			} else {
				resp.addHeader(CONTENT_ENCODING, ENCODING_GZIP);
			}
		}
		resp.setContentType(content.contentType);
		ByteUtil.copy(data, resp.getOutputStream());
		data.close();
	}
}
