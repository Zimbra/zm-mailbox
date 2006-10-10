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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.QName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.XMLElement;

public class PropFind extends DavMethod {
	
	public static final String PROPFIND  = "PROPFIND";
	
	public String getName() {
		return PROPFIND;
	}
	
	public void handle(DavContext ctxt) throws DavException, IOException {
		boolean nameOnly = false;
		Map<String,QName> requestedProps = null;
		
		if (ctxt.hasRequestMessage()) {
			Element req = ctxt.getRequestMessage();
			if (!req.getName().equals(DavElements.P_PROPFIND))
				throw new DavException("msg "+req.getName()+" not allowed in PROPFIND", HttpServletResponse.SC_BAD_REQUEST, null);

			List<Element> elems = req.listElements();
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
					requestedProps = new java.util.HashMap<String,QName>();
					List<Element> props = e.listElements();
					for (Element prop : props)
						requestedProps.put(prop.getName(), prop.getQName());
				}
			}
		}
		DavResource resource = UrlNamespace.getResource(ctxt);
		addComplianceHeader(ctxt, resource);
		ctxt.setStatus(DavProtocol.STATUS_MULTI_STATUS);
		Element resp = new XMLElement(DavElements.E_MULTISTATUS);
		addResourceToResponse(ctxt, resource, resp, nameOnly, requestedProps, false);

		if (ctxt.getDepth() != Depth.ZERO) {
			ZimbraLog.dav.debug("depth: "+ctxt.getDepth().name());

			List<DavResource> children = UrlNamespace.getChildren(ctxt, resource);
			for (DavResource child : children)
				addResourceToResponse(ctxt, child, resp, nameOnly, requestedProps, ctxt.getDepth() == Depth.INFINITY);
		}
		sendResponse(ctxt, resp);
	}
	
	private void addResourceToResponse(DavContext ctxt, DavResource rs, Element top, boolean nameOnly, Map<String,QName> requestedProps, boolean includeChildren) throws DavException {
		Element resp = top.addElement(DavElements.E_RESPONSE);
		resp.addElement(DavElements.E_HREF).setText(UrlNamespace.getResourceUrl(rs));
		Map<Integer,Element> propstatMap = new HashMap<Integer,Element>();
		Set<String> allPropNames = rs.getAllPropertyNames();
		Set<String> propNames;
		if (requestedProps == null)
			propNames = allPropNames;
		else
			propNames = requestedProps.keySet();
		if (requestedProps == null || requestedProps.containsKey(DavElements.P_RESOURCETYPE))
			addProperty(propstatMap, STATUS_OK, rs.getResourceTypeElement());
		for (String name : propNames) {
			if (name.equals(DavElements.P_RESOURCETYPE))
				continue;
			if (!allPropNames.contains(name)) {
				Element error;
				if (requestedProps == null)
					error = new XMLElement(name);
				else
					error = new XMLElement(requestedProps.get(name));
				addProperty(propstatMap, HttpServletResponse.SC_NOT_FOUND, error);
				continue;
			}
			Element e;
			if (requestedProps == null)
				e = new XMLElement(name);
			else
				e = new XMLElement(requestedProps.get(name));
			if (!nameOnly)
				e.setText(rs.getProperty(name));
			addProperty(propstatMap, STATUS_OK, e);
		}
		for (Entry<Integer, Element> entry : propstatMap.entrySet())
			resp.addElement(entry.getValue());
		if (includeChildren) {
			List<DavResource> children = UrlNamespace.getChildren(ctxt, rs);
			for (DavResource child : children)
				addResourceToResponse(ctxt, child, top, nameOnly, requestedProps, includeChildren);
		}
	}
	
	private void addProperty(Map<Integer,Element> propstatMap, int status, Element property) {
		Element props = propstatMap.get(status);
		if (props == null) {
			props = new Element.XMLElement(DavElements.E_PROPSTAT);
			props.addElement(DavElements.E_PROP);
			addStatusElement(props, status);
			propstatMap.put(status, props);
		}
		Element prop = props.getOptionalElement(DavElements.E_PROP);
		if (prop == null)
			return;
		prop.addElement(property);
	}
}
