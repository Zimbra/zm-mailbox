/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.dav.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.service.method.*;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.ZimbraServlet;

@SuppressWarnings("serial")
public class DavServlet extends ZimbraServlet {

	public static final String DAV_PATH = "/dav";
	
	private static Map<String, DavMethod> sMethods;
	
	public void init() throws ServletException {
		super.init();
		sMethods = new HashMap<String, DavMethod>();
		addMethod(new Copy());
		addMethod(new Delete());
		addMethod(new Get());
		addMethod(new Head());
		addMethod(new Lock());
		addMethod(new MkCol());
		addMethod(new Move());
		addMethod(new Options());
		addMethod(new Post());
		addMethod(new Put());
		addMethod(new PropFind());
		addMethod(new PropPatch());
		addMethod(new Unlock());
		addMethod(new MkCalendar());
		addMethod(new Report());
		addMethod(new Acl());
	}

	private void addMethod(DavMethod method) {
		sMethods.put(method.getName(), method);
	}
	
	public static void setAllowHeader(HttpServletResponse resp) {
		Set<String> methods = sMethods.keySet();
		StringBuilder buf = new StringBuilder();
		for (String method : methods) {
			if (buf.length() > 0)
				buf.append(", ");
			buf.append(method);
		}
		resp.setHeader(DavProtocol.HEADER_ALLOW, buf.toString());
	}
	
	enum RequestType { password, authtoken, both, none };
	
    private RequestType getAllowedRequestType(HttpServletRequest req) {
    	if (!super.isRequestOnAllowedPort(req))
    		return RequestType.none;
    	Server server = null;
    	try {
    		server = Provisioning.getInstance().getLocalServer();
    	} catch (Exception e) {
    		return RequestType.none;
    	}
    	boolean allowPassword = server.getBooleanAttr(Provisioning.A_zimbraCalendarCalDavClearTextPasswordEnabled, true);
    	int sslPort = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 443);
    	int mailPort = server.getIntAttr(Provisioning.A_zimbraMailPort, 80);
    	int incomingPort = req.getLocalPort();
    	if (incomingPort == sslPort)
    		return RequestType.both;
    	else if (incomingPort == mailPort && allowPassword)
    		return RequestType.both;
    	else
    		return RequestType.authtoken;
    }
    
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ZimbraLog.clearContext();
		addRemoteIpToLoggingContext(req);
		ZimbraLog.addUserAgentToContext(req.getHeader(DavProtocol.HEADER_USER_AGENT));

		RequestType rtype = getAllowedRequestType(req);
		
		if (rtype == RequestType.none) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}

		/*
		if (ZimbraLog.dav.isDebugEnabled()) {
			java.util.Enumeration en = req.getHeaderNames();
			while (en.hasMoreElements()) {
				String n = (String)en.nextElement();
				java.util.Enumeration vals = req.getHeaders(n);
				while (vals.hasMoreElements()) {
					String v = (String)vals.nextElement();
		        	ZimbraLog.dav.debug("HEADER "+n+": "+v);
				}
			}
		}
		*/
		DavContext ctxt;
		try {
            AuthToken at = AuthProvider.getAuthToken(req, false);
            if (at != null && at.isExpired())
                at = null;
            Account authUser = null;
            if (at != null && (rtype == RequestType.both || rtype == RequestType.authtoken))
            	authUser = Provisioning.getInstance().get(AccountBy.id, at.getAccountId());
            else if (at == null && (rtype == RequestType.both || rtype == RequestType.password))
    			authUser = basicAuthRequest(req, resp, true);
			if (authUser == null) {
				try {
					resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
				} catch (Exception e) {}
				return;
			}
			ZimbraLog.addToContext(ZimbraLog.C_ANAME, authUser.getName());
			ctxt = new DavContext(req, resp, authUser);
            if (ctxt.getUser() == null) {
                resp.sendRedirect(DAV_PATH + "/" + authUser.getName() + "/");
                return;
            }
		} catch (AuthTokenException e) {
			ZimbraLog.dav.error("error getting authenticated user", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (ServiceException e) {
			ZimbraLog.dav.error("error getting authenticated user", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		DavMethod method = sMethods.get(req.getMethod());
		if (method == null) {
			setAllowHeader(resp);
			resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}
		
		/*
		try {
			DavResource rs = ctxt.getRequestedResource();
			if (rs instanceof MailItemResource) {
				MailItemResource mir = (MailItemResource) rs;
				if (!mir.isLocal()) {
					sendProxyRequest(ctxt, method, mir);
					return;
				}
			}
		} catch (DavException de) {
		} catch (ServiceException se) {
		}
		*/
		
		try {
			long t0 = System.currentTimeMillis();
			if (ctxt.hasRequestMessage() && ZimbraLog.dav.isDebugEnabled())
				try {
					ZimbraLog.dav.debug("REQUEST:\n"+new String(ByteUtil.readInput(ctxt.getUpload().getInputStream(), -1, 1024), "UTF-8"));
				} catch (Exception e) {}
			method.checkPrecondition(ctxt);
			method.handle(ctxt);
			method.checkPostcondition(ctxt);
			if (!ctxt.isResponseSent())
				resp.setStatus(ctxt.getStatus());
			long t1 = System.currentTimeMillis();
			ZimbraLog.dav.info("DavServlet operation "+method.getName()+" to "+req.getPathInfo()+" (depth: "+ctxt.getDepth().name()+") finished in "+(t1-t0)+"ms");
		} catch (DavException e) {
			if (e.getCause() instanceof MailServiceException.NoSuchItemException ||
					e.getStatus() == HttpServletResponse.SC_NOT_FOUND)
				ZimbraLog.dav.info(ctxt.getUri()+" not found");
			else if (e.getStatus() == HttpServletResponse.SC_MOVED_TEMPORARILY ||
					 e.getStatus() == HttpServletResponse.SC_MOVED_PERMANENTLY) 
				ZimbraLog.dav.info("sending redirect");
			
			try {
				if (e.isStatusSet()) {
					resp.setStatus(e.getStatus());
					if (e.hasErrorMessage())
						e.writeErrorMsg(resp.getOutputStream());
				} else {
					ZimbraLog.dav.error("error handling method "+method.getName(), e);
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			} catch (IllegalStateException ise) {
			}
		} catch (ServiceException e) {
			if (e instanceof MailServiceException.NoSuchItemException) {
				ZimbraLog.dav.info(ctxt.getUri()+" not found");
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			ZimbraLog.dav.error("error handling method "+method.getName(), e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			ZimbraLog.dav.error("error handling method "+method.getName(), e);
			try {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (Exception ex) {}
		} finally {
			ctxt.cleanup();
		}
	}
	
	public static String getDavUrl(String user) throws DavException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, user);
        if (account == null)
			throw new DavException("unknown user "+user, HttpServletResponse.SC_NOT_FOUND, null);
        return getServiceUrl(account, DAV_PATH);
	}
}
