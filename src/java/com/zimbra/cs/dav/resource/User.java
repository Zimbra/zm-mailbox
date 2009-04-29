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
import com.zimbra.cs.dav.property.VersioningProperty;

public class User extends Principal {

    public User(DavContext ctxt, String mainUrl) throws ServiceException {
        this(ctxt.getAuthAccount(), mainUrl);
    }
    
    public User(Account authUser, String url) throws ServiceException {
    	super(authUser, url);
    	mUser = authUser;
        String user = getOwner();
        addProperty(CalDavProperty.getCalendarHomeSet(user));
        addProperty(CalDavProperty.getScheduleInboxURL(user));
        addProperty(CalDavProperty.getScheduleOutboxURL(user));
        addProperty(VersioningProperty.getSupportedReportSet());
		setProperty(DavElements.E_PRINCIPAL_URL, UrlNamespace.getPrincipalUrl(authUser), true);
        ArrayList<String> addrs = new ArrayList<String>();
        for (String addr : authUser.getMultiAttr(Provisioning.A_zimbraMailDeliveryAddress))
            addrs.add(addr);
        for (String alias : authUser.getMultiAttr(Provisioning.A_zimbraMailAlias))
            addrs.add(alias);
        addrs.add(url);
        addProperty(CalDavProperty.getCalendarUserAddressSet(addrs));
        setProperty(DavElements.E_HREF, url);
        String cn = authUser.getAttr(Provisioning.A_cn);
        if (cn == null)
            cn = authUser.getName();
        setProperty(DavElements.E_DISPLAYNAME, cn);
        mUri = url;
    }
    
    private Account mUser;
    
    @Override
	public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		ArrayList<DavResource> proxies = new ArrayList<DavResource>();
		try {
			proxies.add(new CalendarProxyRead(getOwner(), mUri));
			proxies.add(new CalendarProxyWrite(getOwner(), mUri));
		} catch (ServiceException e) {
		}
		return proxies;
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
        return true;
    }
    
    private class CalendarProxyRead extends Principal {
    	public CalendarProxyRead(String user, String url) throws ServiceException {
    		super(user, url+"calendar-proxy-read");
            addResourceType(DavElements.E_CALENDAR_PROXY_READ);
            addProperty(VersioningProperty.getSupportedReportSet());
    		setProperty(DavElements.E_PRINCIPAL_URL, UrlNamespace.getPrincipalUrl(mUser), true);
    	}
        @Override
        public boolean isCollection() {
            return true;
        }
    }
    
    private class CalendarProxyWrite extends Principal {
    	public CalendarProxyWrite(String user, String url) throws ServiceException {
    		super(user, url+"calendar-proxy-write");
            addResourceType(DavElements.E_CALENDAR_PROXY_WRITE);
            addProperty(VersioningProperty.getSupportedReportSet());
    		setProperty(DavElements.E_PRINCIPAL_URL, UrlNamespace.getPrincipalUrl(mUser), true);
    	}
        @Override
        public boolean isCollection() {
            return true;
        }
    }
}
