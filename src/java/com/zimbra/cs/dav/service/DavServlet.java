/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.service.method.*;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;

@SuppressWarnings("serial")
public class DavServlet extends ZimbraServlet {

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
	
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		if (!isRequestOnAllowedPort(req)) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}
		
		DavContext ctxt = new DavContext(req, resp);
		try {
			Account authUser = basicAuthRequest(req, resp, true);
			if (authUser == null)
				return;
			ctxt.setOperationContext(authUser);
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
		
		try {
			method.handle(ctxt);
		} catch (DavException e) {
			if (e.getCause() instanceof MailServiceException.NoSuchItemException ||
					e.getStatus() == HttpServletResponse.SC_NOT_FOUND)
				ZimbraLog.dav.debug(ctxt.getUri()+" not found");
			else
				ZimbraLog.dav.debug("error handling method "+method.getName(), e);
			
			if (e.isStatusSet())
				resp.sendError(e.getStatus());
			else
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			ZimbraLog.dav.debug("error handling method "+method.getName(), e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
