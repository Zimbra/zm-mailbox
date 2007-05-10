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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.Acl.Ace;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.MailItemResource;
import com.zimbra.cs.dav.service.DavMethod;

public class Acl extends DavMethod {
	public static final String ACL = "ACL";

	public String getName() {
		return ACL;
	}

	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		DavResource rs = ctxt.getRequestedResource();
		if (!rs.isCollection() || !(rs instanceof MailItemResource))
			throw new DavException("acl not implemented for non-collection resource", HttpServletResponse.SC_NOT_IMPLEMENTED);

		if (!ctxt.hasRequestMessage())
			throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST);
		
		Document reqMsg = ctxt.getRequestMessage();
		Element acl = reqMsg.getRootElement();
		if (!acl.getQName().equals(DavElements.E_ACL))
			throw new DavException("request does not start with acl element", HttpServletResponse.SC_BAD_REQUEST);
		List<Element> aceElements = acl.elements(DavElements.E_ACE);
		ArrayList<Ace> aceList = new ArrayList<Ace>();
		for (Element ace : aceElements)
			aceList.add(new Ace(ace));

		MailItemResource mir = (MailItemResource) rs;
		mir.setAce(ctxt, aceList);
	}

}
