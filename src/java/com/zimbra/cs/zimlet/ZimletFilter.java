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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

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
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.Provisioning.AccountBy;
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

	private AuthToken getAuthTokenForApp(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServiceException {
		Config config = Provisioning.getInstance().getConfig();
		int adminPort = config.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
		if (adminPort == req.getServerPort()) {
			return getAdminAuthTokenFromCookie(req, resp);
		}	
		return getAuthTokenFromCookie(req, resp, true);
	}
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (!isHttpReq(request, response)) {
			return;
		}
		HttpServletRequest  req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;

        AuthToken authToken;
        try {
        	authToken = getAuthTokenForApp(req, resp);
        } catch (ServiceException se) {
        	ZimbraLog.zimlet.info("can't get authToken: "+se.getMessage());
        	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        	return;
        }
        
    	String zimletName = getZimletName(req);
    	if (zimletName == null) {
	    	ZimbraLog.zimlet.info("no zimlet in the request");
        	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    		return;
    	}
    	
    	if (!zimletName.equals(com.zimbra.cs.zimlet.ZimletUtil.ZIMLET_DEV_DIR)) {
            try {
            	Provisioning prov = Provisioning.getInstance();
            	Account account = prov.get(AccountBy.ID, authToken.getAccountId());
            	Zimlet z = prov.getZimlet(zimletName);
            	boolean isAdmin = (authToken.isAdmin() || authToken.isDomainAdmin());
            	if (z.isExtension()) {
            		// admin zimlets are accessible only by admins through admin app.
            		if (!isAdmin) {
                    	ZimbraLog.zimlet.info("unauthorized request to zimlet "+zimletName+" from non admin user "+authToken.getAccountId());
                    	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                		return;
            		}
            	} else {
    	        	Set zimlets = prov.getCOS(account).getMultiAttrSet(Provisioning.A_zimbraZimletAvailableZimlets);
    	        	if (!zimlets.contains(zimletName)) {
    	            	ZimbraLog.zimlet.info("unauthorized request to zimlet "+zimletName+" from user "+authToken.getAccountId());
    	            	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    	        		return;
    	        	}
            	}
            } catch (ServiceException se) {
            	ZimbraLog.zimlet.info("cannot resolve account "+authToken.getAccountId());
            	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            	return;
            }
    	}
        chain.doFilter(req, resp);
	}

	public void destroy() {

	}
}
