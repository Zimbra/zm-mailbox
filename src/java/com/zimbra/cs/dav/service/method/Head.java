/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;

public class Head extends Get {
	public static final String HEAD  = "HEAD";
	public String getName() {
		return HEAD;
	}
	protected boolean returnContent() {
		return false;
	}
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		super.handle(ctxt);
		int cl = ctxt.getRequestedResource().getContentLength();
		if (cl > 0)
			ctxt.getResponse().setContentLength(cl);
	}
}
