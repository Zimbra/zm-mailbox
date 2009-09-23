/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.LockMgr;
import com.zimbra.cs.dav.service.DavMethod;

public class Unlock extends DavMethod {
	public static final String UNLOCK  = "UNLOCK";
	public String getName() {
		return UNLOCK;
	}
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		String token = ctxt.getRequest().getHeader(DavProtocol.HEADER_LOCK_TOKEN);
		if (token != null) {
			// RFC2518bis section 10.5
			// Lock-Token = "Lock-Token" ":" Coded-URL
			// Coded-URL  = "<" absolute-URI ">"
			int len = token.length();
			if (token.charAt(0) == '<' && token.charAt(len-1) == '>')
				LockMgr.getInstance().deleteLock(ctxt, ctxt.getUri(), token.substring(1, len-1));
		}
		ctxt.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
		ctxt.responseSent();
	}
}
