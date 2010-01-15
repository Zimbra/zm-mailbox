/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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

import org.dom4j.Document;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavResponse;

public class PropFind extends DavMethod {
	
	public static final String PROPFIND  = "PROPFIND";
	
	public String getName() {
		return PROPFIND;
	}
	
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		
		if (ctxt.hasRequestMessage()) {
			Document req = ctxt.getRequestMessage();
			Element top = req.getRootElement();
			if (!top.getName().equals(DavElements.P_PROPFIND))
				throw new DavException("msg "+top.getName()+" not allowed in PROPFIND", HttpServletResponse.SC_BAD_REQUEST, null);

		}
		
		RequestProp reqProp = ctxt.getRequestProp();
		
		DavResponse resp = ctxt.getDavResponse();
		if (ctxt.getDepth() == Depth.one) {
			resp.addResources(ctxt, ctxt.getAllRequestedResources(), reqProp);
		} else {
			DavResource resource = ctxt.getRequestedResource();
			resp.addResource(ctxt, resource, reqProp, false);
		}

		sendResponse(ctxt);
	}
}
