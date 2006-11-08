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

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavResponse;

/*
 * draft-dusseault-caldav section 9.10
 * 
 *     <!ELEMENT calendar-multiget ((DAV:allprop |
 *                                   DAV:propname |
 *                                   DAV:prop)?, DAV:href+)>
 *                                
 */
public class CalendarMultiget extends Report {
	public void handle(DavContext ctxt) throws DavException {
		Element query = ctxt.getRequestMessage().getRootElement();
		if (!query.getQName().equals(DavElements.E_CALENDAR_MULTIGET))
			throw new DavException("msg "+query.getName()+" is not calendar-multiget", HttpServletResponse.SC_BAD_REQUEST, null);

		DavResponse resp = ctxt.getDavResponse();
		RequestProp reqProp = getRequestProp(ctxt);
		for (Object obj : query.elements(DavElements.E_HREF)) {
			if (obj instanceof Element) {
				Element href = (Element) obj;
				DavResource rs = UrlNamespace.getResourceAtUrl(ctxt, href.getText());
				resp.addResource(ctxt, rs, reqProp, false);
			}
		}
	}
}
