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
		HttpServletResponse resp = ctxt.getResponse();
		resp.setContentType(resource.getContentType());
		
		// in some cases getContentLength() returns an estimate, and the exact 
		// content length is not known until DavResource.getContent() is called.
		// the estimate is good enough for PROPFIND, but not when returning 
		// the contents.  just leave off setting content length explicitly, 
		// and have the servlet deal with it by doing chunking or 
		// setting content-length header on its own.
		
		//resp.setContentLength(resource.getContentLength());
		if (!resource.hasContent())
			return;
		ByteUtil.copy(resource.getContent(), true, ctxt.getResponse().getOutputStream(), false);
	}
}
