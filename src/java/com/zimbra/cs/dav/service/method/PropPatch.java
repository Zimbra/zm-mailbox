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

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavResponse;

public class PropPatch extends DavMethod {
	public static final String PROPPATCH  = "PROPPATCH";
	public String getName() {
		return PROPPATCH;
	}
	public void handle(DavContext ctxt) throws DavException, IOException {
		HashSet<Element> set = new HashSet<Element>();
		HashSet<QName> remove = new HashSet<QName>();
		
		if (ctxt.hasRequestMessage()) {
			Document req = ctxt.getRequestMessage();
			Element top = req.getRootElement();
			if (!top.getName().equals(DavElements.P_PROPERTYUPDATE))
				throw new DavException("msg "+top.getName()+" not allowed in PROPPATCH", HttpServletResponse.SC_BAD_REQUEST, null);
			for (Object obj : top.elements()) {
				if (!(obj instanceof Element))
					continue;
				Element e = (Element)obj;
				if (e.getName().equals(DavElements.P_SET)) {
					e = e.element(DavElements.E_PROP);
					for (Object s : e.elements())
						if (s instanceof Element)
							set.add((Element)s);
				} else if (e.getName().equals(DavElements.P_REMOVE)) {
					e = e.element(DavElements.E_PROP);
					for (Object r : e.elements())
						if (r instanceof Element)
							remove.add(((Element)r).getQName());
				}
			}
		}
		
		DavResource resource = UrlNamespace.getResource(ctxt);
		resource.patchProperties(ctxt, set, remove);
		DavResponse resp = ctxt.getDavResponse();
		
		resp.addResource(ctxt, resource, getRequestProp(set, remove), false);
		sendResponse(ctxt);
	}
}
