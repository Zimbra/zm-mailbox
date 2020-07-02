/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import com.zimbra.common.account.Key.AccountBy;
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

    public static final String ALL_ZIMLETS = "com.zimbra.cs.zimlet.All";
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

	private AuthToken getAuthTokenForApp(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
			throws IOException, ServiceException {
		Config config = Provisioning.getInstance().getConfig();
		int adminPort = config.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
		if (adminPort == req.getLocalPort()) {
			return getAdminAuthTokenFromCookie(req, resp, doNotSendHttpError);
		}
		return getAuthTokenFromCookie(req, resp, doNotSendHttpError);
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (!isHttpReq(request, response)) {
			return;
		}
		HttpServletRequest  req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		String zimbraXZimletCompatibleWith = req.getParameter("zimbraXZimletCompatibleWith");
        AuthToken authToken;
		try {
        	authToken = getAuthTokenForApp(req, resp, false);
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
        List<Zimlet> allZimlets = new LinkedList<Zimlet>();
		try {
		    isAdminAuth = AdminAccessControl.getAdminAccessControl(authToken).isSufficientAdminForZimletFilterServlet();
		    
			// add all available zimlets
			if (!isAdminAuth) {
				// zimlets for this account's COS
				Account account = prov.get(AccountBy.id, authToken.getAccountId(), authToken);
				String[] array = ZimletUtil.getAvailableZimlets(account).getZimletNamesAsArray();
				for (String zimletName : ZimletUtil.getAvailableZimlets(account).getZimletNamesAsArray()) {
					Zimlet zimlet = prov.getZimlet(zimletName);
					if (zimlet == null) continue;
                    if (zimlet.isEnabled()) {
                        addZimlet(allowedZimlets, zimbraXZimletCompatibleWith, zimlet);
					}
                    addZimlet(allZimlets, zimbraXZimletCompatibleWith, zimlet);
				}
			}

			// add the admin zimlets
			else {
				allZimlets = prov.listAllZimlets();
                Iterator<Zimlet> iter = allZimlets.iterator();
				while (iter.hasNext()) {
                    Zimlet zimlet = iter.next();
					if (zimlet.isExtension()) {
	                    if (zimlet.isEnabled()) {
                            addZimlet(allowedZimlets, zimbraXZimletCompatibleWith, zimlet);
	                    }
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
		Set<String> allowedZimletNames = getZimletNames(zimletList);
        Set<String> allZimletNames = getZimletNames(allZimlets);

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
                    allZimlets.remove(zimlet);
                    continue;
                }

                if (!allowedZimletNames.contains(zimletName)) {
                    ZimbraLog.zimlet.info("unauthorized request to zimlet "+zimletName+" from user "+authToken.getAccountId());
                    iter.remove();
                    allZimlets.remove(zimlet);
                    continue;
                }

				if (zimlet.isExtension() && !isAdminAuth) {
//					ZimbraLog.zimlet.info("!!!!! removing extension zimlet: "+zimletName);
					iter.remove();
                    allZimlets.remove(zimlet);
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
        req.setAttribute(ZimletFilter.ALL_ZIMLETS, allZimletNames);
        chain.doFilter(req, resp);
	}

    private static void addZimlet(List zimletList, String zimbraXZimletCompatibleWith,
        Zimlet zimlet) {
        if (zimbraXZimletCompatibleWith != null && zimlet.getZimbraXCompatibleSemVer() != null) {
            // request is for ZimbraX zimlets and this is zimbraX zimlet
            // check version compatibility
            com.github.zafarkhaja.semver.Version v = com.github.zafarkhaja.semver.Version
                .valueOf(zimbraXZimletCompatibleWith);
            boolean result = v.satisfies(zimlet.getZimbraXCompatibleSemVer());
            if (result) {
                zimletList.add(zimlet);
            }
        } else if (zimbraXZimletCompatibleWith == null
            && zimlet.getZimbraXCompatibleSemVer() == null) {
            // request is not for ZimbraX zimlets and this is not zimbraX zimlet
            zimletList.add(zimlet);
        }
    }

	public void destroy() {

	}

    //
    // Private methods
    //

    private static Set<String> getZimletNames(List<Zimlet> zimlets) {
        Set<String> names = new LinkedHashSet<String>();
        for (Zimlet zimlet : zimlets) {
            names.add(zimlet.getName());
        }
        return names;
    }
    
}
