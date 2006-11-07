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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavResponse;

public class PropFind extends DavMethod {
	
	public static final String PROPFIND  = "PROPFIND";
	
	public String getName() {
		return PROPFIND;
	}
	
	public void handle(DavContext ctxt) throws DavException, IOException {
		boolean nameOnly = false;
		Set<QName> requestedProps = null;
		
		if (ctxt.hasRequestMessage()) {
			Document req = ctxt.getRequestMessage();
			Element top = req.getRootElement();
			if (!top.getName().equals(DavElements.P_PROPFIND))
				throw new DavException("msg "+top.getName()+" not allowed in PROPFIND", HttpServletResponse.SC_BAD_REQUEST, null);

			@SuppressWarnings("unchecked")
			List<Element> elems = top.elements();
			if (elems.size() != 1)
				throw new DavException("msg propfind should contain one element", HttpServletResponse.SC_BAD_REQUEST, null);
			
			for (Element e : elems) {
				String name = e.getName();
				if (name.equals(DavElements.P_ALLPROP))
					requestedProps = null;
				else if (name.equals(DavElements.P_PROPNAME))
					nameOnly = true;
				else if (!name.equals(DavElements.P_PROP))
					throw new DavException("invalid element "+e.getName(), HttpServletResponse.SC_BAD_REQUEST, null);
				else {
					requestedProps = new HashSet<QName>();
					@SuppressWarnings("unchecked")
					List<Element> props = e.elements();
					for (Element prop : props)
						requestedProps.add(prop.getQName());
				}
			}
		}
		DavResource resource = UrlNamespace.getResource(ctxt);
		addComplianceHeader(ctxt, resource);
		DavResponse resp = ctxt.getDavResponse();
		
		resp.addResource(ctxt, resource, requestedProps, nameOnly, false);

		if (resource.isCollection() && ctxt.getDepth() != Depth.zero) {
			//ZimbraLog.dav.debug("depth: "+ctxt.getDepth().name());

			for (DavResource child : resource.getChildren(ctxt))
				resp.addResource(ctxt, child, requestedProps, nameOnly, ctxt.getDepth() == Depth.infinity);
		}
		sendResponse(ctxt);
	}
}
