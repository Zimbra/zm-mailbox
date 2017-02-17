/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavResponse;

public class PropFind extends DavMethod {

    public static final String PROPFIND  = "PROPFIND";

    @Override
    public String getName() {
        return PROPFIND;
    }

    @Override
    public void checkPrecondition(DavContext ctxt) throws DavException, ServiceException {
        super.checkPrecondition(ctxt);
        if (ctxt.getDepth() == Depth.infinity) {
            throw new DavException.PropFindInfiniteDepthForbidden();
        }
    }

    @Override
    public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {

        if (ctxt.hasRequestMessage()) {
            Document req = ctxt.getRequestMessage();
            Element top = req.getRootElement();
            if (!top.getName().equals(DavElements.P_PROPFIND)) {
                throw new DavException("msg "+top.getName()+" not allowed in PROPFIND",
                        HttpServletResponse.SC_BAD_REQUEST, null);
            }

        }

        RequestProp reqProp = ctxt.getRequestProp();

        DavResponse resp = ctxt.getDavResponse();
        if (ctxt.getDepth() == Depth.one) {
            resp.addResources(ctxt, ctxt.getAllRequestedResources(), reqProp);
        } else {
            DavResource resource = ctxt.getRequestedResource();
            resp.addResource(ctxt, resource, reqProp, false);
        }

        sendResponse(ctxt);
    }
}
