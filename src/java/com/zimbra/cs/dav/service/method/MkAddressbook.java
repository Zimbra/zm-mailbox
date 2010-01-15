/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.mailbox.MailItem;

public class MkAddressbook extends DavMethod {
    public static final String MKADDRESSBOOK = "MKADDRESSBOOK";
    public String getName() {
        return MKADDRESSBOOK;
    }

    public void handle(DavContext ctxt) throws DavException, IOException {
        String user = ctxt.getUser();
        String name = ctxt.getItem();

        if (user == null || name == null)
            throw new DavException("invalid uri", HttpServletResponse.SC_FORBIDDEN, null);
        Element top = null;
        if (ctxt.hasRequestMessage()) {
            Document doc = ctxt.getRequestMessage();
            top = doc.getRootElement();
            if (!top.getName().equals(DavElements.P_MKADDRESSBOOK))
                throw new DavException("msg "+top.getName()+" not allowed in MKADDRESSBOOK", HttpServletResponse.SC_BAD_REQUEST, null);
        }

        Collection col = UrlNamespace.getCollectionAtUrl(ctxt, ctxt.getPath());
        Collection newone = col.mkCol(ctxt, name, MailItem.TYPE_CONTACT);
        boolean success = false;
        try {
            PropPatch.handlePropertyUpdate(ctxt, top, newone);
            success = true;
        } finally {
            if (!success)
                newone.delete(ctxt);
        }
        ctxt.setStatus(HttpServletResponse.SC_CREATED);
        ctxt.getResponse().addHeader(DavProtocol.HEADER_CACHE_CONTROL, DavProtocol.NO_CACHE);
    }

    public void checkPrecondition(DavContext ctxt) throws DavException {
        // (DAV:resource-must-be-null): A resource MUST NOT exist at the 
        // Request-URI.

        // (CARDDAV:addressbook-collection-location-bad): The Request-URI 
        // MUST identify a location where an address book collection can be 
        // created.

        // (DAV:needs-privilege): The DAV:bind privilege MUST be granted to 
        // the current user.
    }

    public void checkPostcondition(DavContext ctxt) throws DavException {
        // (CARDDAV:initialize-addressbook-collection): A new address book
        // collection exists at the Request-URI.  The DAV:resourcetype of the
        // address book collection MUST contain both DAV:collection and
        // CARDDAV:addressbook XML elements.
    }
}
