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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ZimbraLog;

/**
 * 
 * @author jylee
 *
 */
public class ZimletFilter extends ZimbraServlet implements Filter {

	private static final String ZIMLET_URL = "^/service/zimlet/([^/\\?]+)[/\\?]?.*$";
	private Pattern mPattern;
	
	public void init(FilterConfig config) throws ServletException {
		mPattern = Pattern.compile(ZIMLET_URL);
	}

	private boolean isHttpReq(ServletRequest req, ServletResponse res) {
		return (req instanceof HttpServletRequest && 
				res instanceof HttpServletResponse);
	}
	
	private String getZimletName(HttpServletRequest req) throws ServletException {
		String uri = req.getRequestURI();
		if (uri == null) {
			return null;
		}
		Matcher matcher = mPattern.matcher(uri);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (!isHttpReq(request, response)) {
			return;
		}
		HttpServletRequest  req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;

        AuthToken authToken = getAuthTokenFromCookie(req, resp);
        if (authToken == null) {
	    	ZimbraLog.zimlet.info("no auth token");
        	return;
        }
        
        try {
        	Account account = Provisioning.getInstance().getAccountById(authToken.getAccountId());
        	String[] attrList = account.getCOS().getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
        	String zimletName = getZimletName(req);
        	
        	if (zimletName == null) {
    	    	ZimbraLog.zimlet.debug("no zimlet name");
        		return;
        	}
        	
        	boolean found = false;
        	for (int i = 0; i < attrList.length; i++) {
        		if (zimletName.equals(attrList[i])) {
        			found = true;
        			break;
        		}
        	}
        	if (!found) {
            	ZimbraLog.zimlet.info("unauthorized request to zimlet "+zimletName+" from user "+authToken.getAccountId());
        		return;
        	}
        } catch (ServiceException se) {
        	ZimbraLog.zimlet.info("cannot resolve account "+authToken.getAccountId());
        	return;
        }
        chain.doFilter(req, resp);
	}

	public void destroy() {

	}
}
