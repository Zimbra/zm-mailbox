/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.AddressObject;
import com.zimbra.cs.dav.resource.AddressbookCollection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavResponse;

public class AddressbookMultiget extends Report {
    @Override
    public void handle(DavContext ctxt) throws ServiceException, DavException {
        Element query = ctxt.getRequestMessage().getRootElement();
        if (!query.getQName().equals(DavElements.CardDav.E_ADDRESSBOOK_MULTIGET))
            throw new DavException("msg "+query.getName()+" is not addressbook-multiget", HttpServletResponse.SC_BAD_REQUEST, null);

        DavResponse resp = ctxt.getDavResponse();
        DavResource reqResource = ctxt.getRequestedResource();
        if (!(reqResource instanceof AddressbookCollection))
            throw new DavException("requested resource is not an addressbook collection", HttpServletResponse.SC_BAD_REQUEST, null);
        RequestProp reqProp = ctxt.getRequestProp();
        for (Object obj : query.elements(DavElements.E_HREF)) {
            if (obj instanceof Element) {
                String href = ((Element)obj).getText();
                URI uri = URI.create(href);
                String[] fragments = HttpUtil.getPathFragments(uri);
                if (uri.getPath().toLowerCase().endsWith(AddressObject.VCARD_EXTENSION)) {
                    // double encode the last fragment
                    fragments[fragments.length - 1] = HttpUtil.urlEscapeIncludingSlash(fragments[fragments.length - 1]);
                }
                uri = HttpUtil.getUriFromFragments(fragments, uri.getQuery(), true, false);
                href = uri.getPath();
                DavResource rs = UrlNamespace.getResourceAtUrl(ctxt, href);
                if (rs != null)
                    resp.addResource(ctxt, rs, reqProp, false);
            }
        }
    }
}
