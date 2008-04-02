/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
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
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavResponse;

public class PropPatch extends DavMethod {
	public static final String PROPPATCH  = "PROPPATCH";
	public String getName() {
		return PROPPATCH;
	}
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		
		if (!ctxt.hasRequestMessage()) {
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST);
		}
		
		Document req = ctxt.getRequestMessage();
		Element top = req.getRootElement();
		if (!top.getName().equals(DavElements.P_PROPERTYUPDATE))
			throw new DavException("msg "+top.getName()+" not allowed in PROPPATCH", HttpServletResponse.SC_BAD_REQUEST, null);
		DavResource resource = ctxt.getRequestedResource();
		Pair<HashSet<Element>,HashSet<QName>> props = handlePropertyUpdate(ctxt, top, resource);
		DavResponse resp = ctxt.getDavResponse();
		
		resp.addResource(ctxt, resource, getRequestProp(props.getFirst(), props.getSecond()), false);
		sendResponse(ctxt);
	}
	public static Pair<HashSet<Element>,HashSet<QName>> handlePropertyUpdate(DavContext ctxt, Element top, DavResource resource) throws DavException, IOException {
		HashSet<Element> set = new HashSet<Element>();
		HashSet<QName> remove = new HashSet<QName>();
		RequestProp rp = new RequestProp();
		for (Object obj : top.elements()) {
			if (!(obj instanceof Element))
				continue;
			Element e = (Element)obj;
			boolean isSet = e.getName().equals(DavElements.P_SET);
			e = e.element(DavElements.E_PROP);
			for (Object propObj : e.elements()) {
				if (propObj instanceof Element) {
					Element propElem = (Element)propObj;
					QName propName = propElem.getQName();
					ResourceProperty prop = resource.getProperty(propName);
					if (prop == null || !prop.isProtected()) {
						if (isSet)
							set.add(propElem);
						else
							remove.add(propName);
						rp.addProp(propName);
					} else {
						rp.addPropError(propName, new DavException.CannotModifyProtectedProperty(propName));
					}
				}
			}
		}
		
		resource.patchProperties(ctxt, set, remove);
		return new Pair<HashSet<Element>,HashSet<QName>>(set, remove);
	}
}
