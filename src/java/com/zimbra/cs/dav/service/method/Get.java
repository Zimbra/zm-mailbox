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
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;

public class Get extends DavMethod {
	public static final String GET  = "GET";
	public String getName() {
		return GET;
	}
	public void handle(DavContext ctxt) throws DavException, IOException {
		DavResource resource = UrlNamespace.getResource(ctxt);
		if (!resource.hasContent())
			throw new DavException("empty content", HttpServletResponse.SC_NO_CONTENT, null);
		HttpServletResponse resp = ctxt.getResponse();
		resp.setContentType(resource.getContentType());
		resp.setContentLength(resource.getContentLength());
		ByteUtil.copy(resource.getContent(), true, ctxt.getResponse().getOutputStream(), false);
	}
}
