/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.*;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * 
 * @author jylee
 * @author Andy Clark
 */
@SuppressWarnings("serial")
public class ZimletFilter extends ZimbraServlet implements Filter {

    public static final String ALLOWED_ZIMLETS = "com.zimbra.cs.zimlet.Allowed";

	private static final String ZIMLET_URL = "^/service/zimlet/([^/\\?]+)[/\\?]?.*$";
    private static final String ZIMLET_RES_URL_PREFIX = "/service/zimlet/res/";
    private Pattern mPattern;
	
	public void init(FilterConfig config) throws ServletException {
		mPattern = Pattern.compile(ZIMLET_URL);
	}

	private boolean isHttpReq(ServletRequest req, ServletResponse res) {
		return (req instanceof HttpServletRequest &&
				res instanceof HttpServletResponse);
	}

	private AuthToken getAuthTokenForApp(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServiceException {
		Config config = Provisioning.getInstance().getConfig();
		int adminPort = config.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
		if (adminPort == req.getLocalPort()) {
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
        
        if (authToken == null) {
	    	ZimbraLog.zimlet.info("no authToken in the request");
        	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    		return;
        }

		boolean isAdminAuth = authToken.isAdmin() || authToken.isDomainAdmin();
//		ZimbraLog.zimlet.info(">>> isAdminAuth: "+isAdminAuth);

        // get list of allowed zimlets
        Provisioning prov = Provisioning.getInstance();
		Set<String> allowedZimletNames = new HashSet<String>();
		try {
			// add all available zimlets
			if (!isAdminAuth) {
				Account account = prov.get(AccountBy.id, authToken.getAccountId());
				allowedZimletNames.addAll(prov.getCOS(account).getMultiAttrSet(Provisioning.A_zimbraZimletAvailableZimlets));
			}

			// add the admin zimlets
			else {
				List<Zimlet> allZimlets = prov.listAllZimlets();
				for (Zimlet zimlet : allZimlets) {
					if (zimlet.isExtension()) {
						allowedZimletNames.add(zimlet.getName());
					}
				}
			}
		}
		catch (ServiceException e) {
			ZimbraLog.zimlet.info("unable to get list of zimlets");
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

        // get list of zimlets for request
        Set<String> zimletNames = new HashSet<String>();
        String uri = req.getRequestURI();
		boolean isZimletRes = uri.startsWith(ZIMLET_RES_URL_PREFIX);
		if (isZimletRes) {
			zimletNames.addAll(allowedZimletNames);
        }
        else {
            Matcher matcher = mPattern.matcher(uri);
            if (!matcher.matches()) {
                ZimbraLog.zimlet.info("no zimlet specified in request");
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
			zimletNames.add(matcher.group(1));
        }

        // check access
        Iterator<String> iter = zimletNames.iterator();
        while (iter.hasNext()) {
            String zimletName = iter.next();
            try {
                Zimlet zimlet = prov.getZimlet(zimletName);
                if (zimlet == null) {
                    ZimbraLog.zimlet.info("no such zimlet: "+zimletName);
                    iter.remove();
                    continue;
                }

                if (!allowedZimletNames.contains(zimletName)) {
                    ZimbraLog.zimlet.info("unauthorized request to zimlet "+zimletName+" from user "+authToken.getAccountId());
                    iter.remove();
                    continue;
                }

				if (zimlet.isExtension() && !isAdminAuth) {
//					ZimbraLog.zimlet.info("!!!!! removing extension zimlet: "+zimletName);
					iter.remove();
				}
			}
            catch (ServiceException se) {
                ZimbraLog.zimlet.info("service exception to zimlet "+zimletName+" from user "+authToken.getAccountId()+": "+se.getMessage());
                iter.remove();
            }
        }

		if (!isZimletRes) {
			Matcher matcher = mPattern.matcher(uri);
			if (matcher.matches()) {
				String zimletName = matcher.group(1);
				if (!zimletNames.contains(zimletName)) {
					resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}
		}

        // process request
        req.setAttribute(ZimletFilter.ALLOWED_ZIMLETS, zimletNames);
        chain.doFilter(req, resp);
	}

	public void destroy() {

	}
}
