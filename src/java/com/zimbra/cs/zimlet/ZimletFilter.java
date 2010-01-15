/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import java.io.*;
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
import com.zimbra.cs.service.admin.AdminAccessControl;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.TemplateCompiler;

/**
 * 
 * @author jylee
 * @author Andy Clark
 */
@SuppressWarnings("serial")
public class ZimletFilter extends ZimbraServlet implements Filter {

    public static final String ALLOWED_ZIMLETS = "com.zimbra.cs.zimlet.Allowed";

	private static final String ZIMLET_URL = "^/service/zimlet/(?:_dev/)?([^/\\?]+)([/\\?]?)(.*)$";
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
        	resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        	return;
        }
        
        if (authToken == null) {
        	// error was already sent out
    		return;
        }

        boolean isAdminAuth = false;
//		ZimbraLog.zimlet.info(">>> isAdminAuth: "+isAdminAuth);

        // get list of allowed zimlets
        Provisioning prov = Provisioning.getInstance();
		List<Zimlet> allowedZimlets = new LinkedList<Zimlet>();
		try {
		    isAdminAuth = AdminAccessControl.getAdminAccessControl(authToken).isSufficientAdminForZimletFilterServlet();
		    
			// add all available zimlets
			if (!isAdminAuth) {
				// zimlets for this account's COS
				Account account = prov.get(AccountBy.id, authToken.getAccountId(), authToken);
				for (String zimletName : ZimletUtil.getAvailableZimlets(account).getZimletNamesAsArray()) {
					Zimlet zimlet = prov.getZimlet(zimletName);
					if (zimlet != null && zimlet.isEnabled()) {
						allowedZimlets.add(zimlet);
					}
				}
			}

			// add the admin zimlets
			else {
				List<Zimlet> allZimlets = prov.listAllZimlets();
				for (Zimlet zimlet : allZimlets) {
					if (zimlet.isExtension() && zimlet.isEnabled()) {
						allowedZimlets.add(zimlet);
					}
				}
			}
		}
		catch (ServiceException e) {
			ZimbraLog.zimlet.info("unable to get list of zimlets");
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// order by priority
		List<Zimlet> zimletList = ZimletUtil.orderZimletsByPriority(allowedZimlets);
		Set<String> allowedZimletNames = new LinkedHashSet<String>();
		for (Zimlet zimlet : zimletList) {
			allowedZimletNames.add(zimlet.getName());
		}

        // get list of zimlets for request
        Set<String> zimletNames = new LinkedHashSet<String>();
        String uri = req.getRequestURI();
		boolean isZimletRes = uri.startsWith(ZIMLET_RES_URL_PREFIX);
		if (isZimletRes) {
			zimletNames.addAll(allowedZimletNames);
        }
        else {
            Matcher matcher = mPattern.matcher(uri);
            if (!matcher.matches()) {
                ZimbraLog.zimlet.info("no zimlet specified in request");
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
			zimletNames.add(matcher.group(1));
        }

        // check access
		File basedir = new File(LC.zimlet_directory.value());
		File devdir = new File(basedir, ZimletUtil.ZIMLET_DEV_DIR);
        Iterator<String> iter = zimletNames.iterator();
        while (iter.hasNext()) {
            String zimletName = iter.next();
            try {
				File devfile = new File(devdir, zimletName);
				if (devfile.exists()) {
					continue;
				}

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
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}
		}

		// force compilation of template
		if (uri.endsWith(".template.js")) {
			Matcher matcher = mPattern.matcher(uri);
			if (matcher.matches()) {
				String zimletName = matcher.group(1);
				String opath = matcher.group(3);
				String ipath = opath.replaceAll(".js$", "");
				boolean isDevZimlet = uri.indexOf("_dev") != -1;
				File zimletDir = new File(isDevZimlet ? devdir : basedir, zimletName);
				File ifile = new File(zimletDir, ipath);
				File ofile = new File(zimletDir, opath);
				if (!ofile.exists() || (ifile.exists() && ifile.lastModified() > ofile.lastModified())) {
					String prefix = zimletName + ".";
					try {
						TemplateCompiler.compile(
							zimletDir, zimletDir, prefix, new String[] { ipath }, true, true
						);
					}
					catch (IOException e) {
						// ignore, let fail
					}
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
