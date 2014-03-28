/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.ResourceTypeProperty;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavResponse;
import com.zimbra.cs.mailbox.MailItem;

/**
 * https://tools.ietf.org/html/rfc5689
 * Extended MKCOL for Web Distributed Authoring and Versioning (WebDAV)
 */

public class MkCol extends DavMethod {
    public static final String MKCOL  = "MKCOL";
    @Override
    public String getName() {
        return MKCOL;
    }
    @Override
    public void handle(DavContext ctxt) throws DavException, IOException {
        String user = ctxt.getUser();
        String name = ctxt.getItem();

        if (user == null || name == null)
            throw new DavException("invalid uri", HttpServletResponse.SC_NOT_ACCEPTABLE, null);

        Collection col = UrlNamespace.getCollectionAtUrl(ctxt, ctxt.getPath());
        Pair<List<Element>,List<Element>> elemPair = null;
        if (ctxt.hasRequestMessage()) {
            Document req = ctxt.getRequestMessage();
            Element top = req.getRootElement();
            if (!top.getName().equals(DavElements.P_MKCOL)) {
                throw new DavException("body "+top.getName()+" not allowed in MKCOL",
                        HttpServletResponse.SC_BAD_REQUEST, null);
            }
            elemPair = PropPatch.getSetsAndRemoves(top, true, MKCOL);
        }
        if (elemPair == null) {
            col.mkCol(ctxt, name);
            ctxt.setStatus(HttpServletResponse.SC_CREATED);
        } else {
            Element resourcetypeElem = null;
            MailItem.Type view = null;
            List<Element> set = elemPair.getFirst();
            Iterator<Element> setIter = set.iterator();
            while (setIter.hasNext()) {
                Element elem = setIter.next();
                String propName=elem.getName();
                if (propName.equals(DavElements.P_RESOURCETYPE)) {
                    ResourceTypeProperty prop = new ResourceTypeProperty(elem);
                    if (prop.isAddressBook()) {
                        view = MailItem.Type.CONTACT;
                    } else if (prop.isCalendar()) {
                        view = MailItem.Type.APPOINTMENT;
                    } else if (!prop.isCollection()) {
                        throw new DavException("resourcetype must include collection for MKCOL",
                            HttpServletResponse.SC_BAD_REQUEST, null);
                    }
                    resourcetypeElem = elem;
                    setIter.remove();
                    break;
                }
            }
            col = col.mkCol(ctxt, name, view);
            Pair<List<Element>,List<QName>> pair = PropPatch.processSetsAndRemoves(ctxt,
                col, elemPair.getFirst(), elemPair.getSecond(), true);
            col.patchProperties(ctxt, pair.getFirst(), pair.getSecond());
            ctxt.getResponseProp().addProp(resourcetypeElem);
            ctxt.setStatus(HttpServletResponse.SC_CREATED);
            DavResponse resp = ctxt.getDavResponse();
            resp.addResource(ctxt, col, ctxt.getResponseProp(), false);
            sendResponse(ctxt);
        }
    }
}
