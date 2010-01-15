/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.carddav.Filter;
import com.zimbra.cs.dav.resource.AddressObject;
import com.zimbra.cs.dav.resource.AddressbookCollection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavResponse;

/*
 * draft-daboo-carddav-02 section 10.5
 * 
 *        <!ELEMENT addressbook-query ((DAV:allprop |
 *                                      DAV:propname |
 *                                      DAV:prop)?, filter)>
 */
public class AddressbookQuery extends Report {
    public void handle(DavContext ctxt) throws DavException, ServiceException {
        DavResource rsc = ctxt.getRequestedResource();
        if (!(rsc instanceof AddressbookCollection))
            throw new DavException("not an addressbook resource", HttpServletResponse.SC_BAD_REQUEST, null);
        
        Element query = ctxt.getRequestMessage().getRootElement();
        if (!query.getQName().equals(DavElements.CardDav.E_ADDRESSBOOK_QUERY))
            throw new DavException("msg "+query.getName()+" is not addressbook-query", HttpServletResponse.SC_BAD_REQUEST, null);
        Element f = query.element(DavElements.CardDav.E_FILTER);
        if (f == null)
            throw new DavException("msg "+query.getName()+" is missing filter", HttpServletResponse.SC_BAD_REQUEST, null);
        Filter filter = new Filter.PropFilter((Element)f.elementIterator().next());
        Collection<AddressObject> contacts = filter.match(ctxt, ((AddressbookCollection)rsc));
        RequestProp reqProp = ctxt.getRequestProp();
        DavResponse resp = ctxt.getDavResponse();
        resp.createResponse(ctxt);
        for (AddressObject c : contacts) {
            resp.addResource(ctxt, c, reqProp, false);
        }
    }
}
