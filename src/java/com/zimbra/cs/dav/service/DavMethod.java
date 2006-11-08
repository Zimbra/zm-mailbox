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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.DavResource;

public abstract class DavMethod {
	public abstract String getName();
	public abstract void handle(DavContext ctxt) throws DavException, IOException;
	
	// needs to throw either 403 Forbidden, or 409 Conflict in case of an error.
	public void checkPrecondition(DavContext ctxt) throws DavException {
	}
	
	public void checkPostcondition(DavContext ctxt) throws DavException {
	}
	
	public String toString() {
		return "DAV method " + getName();
	}
	
	protected static final int STATUS_OK = HttpServletResponse.SC_OK;
	
	protected void addComplianceHeader(DavContext ctxt, DavResource rs) throws IOException {
		HttpServletResponse resp = ctxt.getResponse();
		String comp = DavProtocol.getComplianceString(rs.getComplianceList());
		if (comp != null)
			resp.setHeader(DavProtocol.HEADER_DAV, comp);
	}
	
	protected void sendResponse(DavContext ctxt) throws IOException {
		HttpServletResponse resp = ctxt.getResponse();
		resp.setStatus(ctxt.getStatus());
		if (ctxt.hasResponseMessage()) {
			resp.setContentType(DavProtocol.DAV_CONTENT_TYPE);
			DavResponse respMsg = ctxt.getDavResponse();
			respMsg.writeTo(resp.getOutputStream());
		}
		ctxt.responseSent();
	}
	
	protected RequestProp getRequestProp(DavContext ctxt) throws DavException {
		if (ctxt.hasRequestMessage()) {
			Document req = ctxt.getRequestMessage();
			return new RequestProp(req.getRootElement());
		}
		return sEmptyProp;
	}
	
	protected static RequestProp sEmptyProp;
	
	static {
		sEmptyProp = new RequestProp();
	}
	
	public static class RequestProp {
		boolean nameOnly;
		boolean allProp;
		Collection<QName> props;

		RequestProp() {
			props = new ArrayList<QName>();
			nameOnly = false;
			allProp = true;
		}
		
		public RequestProp(Element top) {
			this();
			
			allProp = false;
			for (Object obj : top.elements()) {
				if (!(obj instanceof Element))
					continue;
				Element e = (Element) obj;
				String name = e.getName();
				if (name.equals(DavElements.P_ALLPROP))
					allProp = true;
				else if (name.equals(DavElements.P_PROPNAME))
					nameOnly = true;
				else if (name.equals(DavElements.P_PROP)) {
					@SuppressWarnings("unchecked")
					List<Element> propElems = e.elements();
					for (Element prop : propElems)
						props.add(prop.getQName());
				}
			}
		}
		
		public boolean isNameOnly() {
			return nameOnly;
		}
		public boolean isAllProp() {
			return allProp;
		}
		public Collection<QName> getProps() {
			return props;
		}
	}
}
