/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.2
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimlets
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * 
 * @author jylee
 *
 */
@SuppressWarnings("serial")
public class ProxyServlet extends ZimbraServlet {
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String TARGET_PARAM = "target";
	private static final String USER_PARAM = "user";
	private static final String PASS_PARAM = "pass";
	private static final String AUTH_PARAM = "auth";
	private static final String AUTH_BASIC = "basic";
	
	private Set<String> getAllowedDomains(AuthToken auth) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, auth.getAccountId());
        return prov.getCOS(acct).getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
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
	
	private boolean canProxyHeader(String header) {
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
			header.equals("cookie")) {
			return false;
		}
		return true;
	}
	
	private byte[] copyPostedData(HttpServletRequest req) throws IOException {
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
            if (baos != null)
                baos.close();
    	}
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
        
        // get the posted body before the server read and parse them.
        byte[] body = copyPostedData(req);
        
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

		HttpMethod method = null;
		try {
			HttpClient client = new HttpClient();
            NetUtil.configureProxy(client);
			String reqMethod = req.getMethod();
			if (reqMethod.equalsIgnoreCase("GET"))
				method = new GetMethod(target);
			else if (reqMethod.equalsIgnoreCase("POST")) {
				PostMethod post = new PostMethod(target);
				post.setRequestEntity(new ByteArrayRequestEntity(body, req.getContentType()));
				method = post;
			} else {
				ZimbraLog.zimlet.info("unsupported request method: "+reqMethod);
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
					ZimbraLog.zimlet.info("unsupported auth type: "+auth);
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
				HttpState state = new HttpState();
				state.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
				client.setState(state);
				method.setDoAuthentication(true);
			}
			
			Enumeration headers = req.getHeaderNames();
			while (headers.hasMoreElements()) {
				String hdr = (String) headers.nextElement();
				if (canProxyHeader(hdr)) {
					//ZimbraLog.zimlet.info(hdr + ": " + req.getHeader(hdr));
					method.addRequestHeader(hdr, req.getHeader(hdr));
				}
			}
			
			try {
				client.executeMethod(method);
			} catch (HttpException ex) {
				ZimbraLog.zimlet.info("exception while proxying "+target, ex);
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			try {
				resp.setContentType(method.getResponseHeader(CONTENT_TYPE).getValue());
			} catch (Exception ex) {
				// workaround for Alexa Thumbnails paid web service, which doesn't bother to return a content-type line.
				resp.setContentType("text/xml");
			}
			ByteUtil.copy(method.getResponseBodyAsStream(), false, resp.getOutputStream(), false);
		} finally {
			if (method != null)
				method.releaseConnection();
		}
	}
}
