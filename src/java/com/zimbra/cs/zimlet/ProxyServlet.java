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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.ZimbraLog;

/**
 * 
 * @author jylee
 *
 */
public class ProxyServlet extends ZimbraServlet {
	private static final String TARGET_PARAM = "target";
	private static final String USER_PARAM = "user";
	private static final String PASS_PARAM = "pass";
	private static final String AUTH_PARAM = "auth";
	private static final String AUTH_BASIC = "basic";
	
	private List getWhitelistDomains(HttpServletRequest req) {
		List l = new ArrayList();
		// TODO: fetch from ldap.
		l.add(new String("*.zimbra.com"));
		l.add(new String("*.liquidsys.com"));
		return l;
	}
	
	private boolean checkPermissionOnTarget(HttpServletRequest req, URL target) {
		String host = target.getHost().toLowerCase();
		List l = getWhitelistDomains(req);
		Iterator iter = l.iterator();
		while (iter.hasNext()) {
			String domain = (String) iter.next();
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
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		doPost(req, resp);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        AuthToken authToken = getAuthTokenFromCookie(req, resp);
        if (authToken == null) {
        	return;
        }
        
		String target = req.getParameter(TARGET_PARAM);
		if (target == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		URL url = new URL(target);
		
		if (!checkPermissionOnTarget(req, url)) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		URLConnection conn = url.openConnection();
		
		String auth, user, pass;
		auth = req.getParameter(AUTH_PARAM);
		user = req.getParameter(USER_PARAM);
		pass = req.getParameter(PASS_PARAM);
		if (auth != null && user != null && pass != null) {
			if (!auth.equals(AUTH_BASIC)) {
				ZimbraLog.zimlet.info("unsupported auth type: "+auth);
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			String cred = user + ":" + pass;
			conn.setRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(cred.getBytes())));
		}
		
		if (conn instanceof HttpURLConnection) {
			HttpURLConnection httpconn = (HttpURLConnection) conn;
			int status = httpconn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				ZimbraLog.zimlet.info("remote host returned error: "+status);
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
		
		int clen = conn.getContentLength();
		if (clen > 0) {
			resp.setContentLength(clen);
		}
		String ctype = conn.getContentType();
		if (ctype != null) {
			resp.setContentType(ctype);
		}
		
		ByteUtil.copy(conn.getInputStream(), resp.getOutputStream());
	}
}
