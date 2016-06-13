/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
