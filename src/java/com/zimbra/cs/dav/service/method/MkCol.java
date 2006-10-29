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

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;

public class MkCol extends DavMethod {
	public static final String MKCOL  = "MKCOL";
	public String getName() {
		return MKCOL;
	}
	public void handle(DavContext ctxt) throws DavException, IOException {
		String path = ctxt.getPath();
		if (path.endsWith("/"))
			path = path.substring(0, path.length()-1);
		int pos = path.lastIndexOf('/');
		if (pos == -1)
			throw new DavException("invalid path", HttpServletResponse.SC_CONFLICT, null);
		String col = path.substring(0, pos);
		String item = path.substring(pos+1);
		DavResource rsc;
		try {
			rsc = UrlNamespace.getResourceAt(ctxt, col);
		} catch (DavException de) {
			throw new DavException("no collection at "+col, HttpServletResponse.SC_CONFLICT, de);
		}
		if (!rsc.isCollection())
			throw new DavException(col+" is not a collection", HttpServletResponse.SC_CONFLICT, null);
		Collection collection = (Collection) rsc;
		collection.mkCol(ctxt, item);
		ctxt.setStatus(HttpServletResponse.SC_CREATED);
	}
}
