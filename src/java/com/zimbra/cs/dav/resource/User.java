/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.CalDavProperty;

public class User extends DavResource {

    public User(String mainUrl, Account owner) throws ServiceException {
        super(mainUrl, owner);
        String user = owner.getUid();
        String url = UrlNamespace.getPrincipalUrl(user);
        addResourceType(DavElements.E_PRINCIPAL);
        addProperty(CalDavProperty.getCalendarHomeSet(user));
        addProperty(CalDavProperty.getScheduleInboxURL(user));
        addProperty(CalDavProperty.getScheduleOutboxURL(user));
        ArrayList<String> addrs = new ArrayList<String>();
        addrs.add(owner.getAttr(Provisioning.A_zimbraMailDeliveryAddress));
        addrs.add(url);
        addProperty(CalDavProperty.getCalendarUserAddressSet(addrs));
        setProperty(DavElements.E_HREF, url);
        setProperty(DavElements.E_DISPLAYNAME, owner.getName());
    }
    
    @Override
    public void delete(DavContext ctxt) throws DavException {
        throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
    }

    @Override
    public InputStream getContent(DavContext ctxt) throws IOException,
            DavException {
        return null;
    }

    @Override
    public boolean isCollection() {
        return false;
    }
}
