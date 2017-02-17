/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavResponse;

public class PropPatch extends DavMethod {
    public static final String PROPPATCH  = "PROPPATCH";
    @Override
    public String getName() {
        return PROPPATCH;
    }
    @Override
    public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {

        if (!ctxt.hasRequestMessage()) {
            throw new DavException("empty request", HttpServletResponse.SC_BAD_REQUEST);
        }

        Document req = ctxt.getRequestMessage();
        Element top = req.getRootElement();
        if (!top.getName().equals(DavElements.P_PROPERTYUPDATE)) {
            throw new DavException("msg "+top.getName() + " not allowed in PROPPATCH",
                    HttpServletResponse.SC_BAD_REQUEST, null);
        }
        DavResource resource = ctxt.getRequestedResource();
        handlePropertyUpdate(ctxt, top, resource, false, PROPPATCH);
        DavResponse resp = ctxt.getDavResponse();

        resp.addResource(ctxt, resource, ctxt.getResponseProp(), false);
        sendResponse(ctxt);
    }

    /**
     *
     * @param top
     * @param isCreate - any "remove" elements will be ignored (should they be an error?)
     * @param method - used for logging to identify what HTTP method we are doing
     * @return pair of lists of elements under "set" and those under "remove" in the request
     * @throws DavException
     * @throws IOException
     */
    public static Pair<List<Element>,List<Element>> getSetsAndRemoves(Element top, boolean isCreate, String method)
    throws DavException, IOException {
        List<Element> set = Lists.newArrayList();
        List<Element> remove = Lists.newArrayList();
        if (top == null) {
            return null;
        }
        for (Object obj : top.elements()) {
            if (!(obj instanceof Element)) {
                continue;
            }
            Element e = (Element)obj;
            String nodeName = e.getName();
            boolean isSet;
            if (nodeName.equals(DavElements.P_SET)) {
                isSet = true;
            } else if (nodeName.equals(DavElements.P_REMOVE)) {
                if (isCreate) {
                    continue;
                }
                isSet = false;
            } else  {
                continue;
            }
            e = e.element(DavElements.E_PROP);
            if (e == null) {
                throw new DavException("missing <D:prop> in " + method, HttpServletResponse.SC_BAD_REQUEST, null);
            }

            for (Object propObj : e.elements()) {
                if (propObj instanceof Element) {
                    Element propElem = (Element)propObj;
                    if (isSet) {
                        set.add(propElem);
                    } else {
                        remove.add(propElem);
                    }
                }
            }
        }
        return new Pair<List<Element>, List<Element>>(set, remove);
    }

    /**
     * @param setElems - list of elements under "set" in the request.  Must NOT be null
     * @param removeElems - list of elements under "remove" in the request.  Must NOT be null
     * @return Pair - first is list of elements representing properties to set.  second is names of
     *                properties to remove.
     */
    public static Pair<List<Element>,List<QName>> processSetsAndRemoves(DavContext ctxt,
            DavResource resource, List<Element> setElems, List<Element> removeElems, boolean isCreate)
    throws DavException, IOException {
        List<Element> set = Lists.newArrayList();
        List<QName> remove = Lists.newArrayList();
        RequestProp rp = new RequestProp(true);
        ctxt.setResponseProp(rp);
        for (Element propElem : setElems) {
            QName propName = propElem.getQName();
            ResourceProperty prop = resource.getProperty(propName);
            if (prop == null || !prop.isProtected()) {
                set.add(propElem);
                rp.addProp(propElem);
            } else if (isCreate && prop.isAllowSetOnCreate()){
                set.add(propElem);
            } else {
                rp.addPropError(propName, new DavException.CannotModifyProtectedProperty(propName));
            }
        }
        for (Element propElem : removeElems) {
            QName propName = propElem.getQName();
            ResourceProperty prop = resource.getProperty(propName);
            if (prop == null || !prop.isProtected()) {
                remove.add(propName);
                rp.addProp(propElem);
            } else {
                rp.addPropError(propName, new DavException.CannotModifyProtectedProperty(propName));
            }
        }
        return new Pair<List<Element>, List<QName>>(set, remove);
    }

    public static void handlePropertyUpdate(DavContext ctxt, Element top, DavResource resource, boolean isCreate,
            String method)
    throws DavException, IOException {
        if (top == null) {
            return;
        }
        Pair<List<Element>,List<Element>> elemPair = getSetsAndRemoves(top, isCreate, method);
        Pair<List<Element>,List<QName>> pair = processSetsAndRemoves(ctxt,
            resource, elemPair.getFirst(), elemPair.getSecond(), isCreate);
        resource.patchProperties(ctxt, pair.getFirst(), pair.getSecond());
    }
}
