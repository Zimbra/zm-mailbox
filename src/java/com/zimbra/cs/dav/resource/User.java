/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.resource;

import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.property.CardDavProperty;

public class User extends Principal {

    public User(DavContext ctxt, Account account, String url) throws ServiceException {
        super(account, url);
        setAccount(account);
        String user = getOwner();
        addProperty(CalDavProperty.getCalendarHomeSet(user));
        addProperty(CalDavProperty.getCalendarUserType(this));
        addProperty(CalDavProperty.getScheduleInboxURL(user));
        addProperty(CalDavProperty.getScheduleOutboxURL(user));
        if (ctxt.getAuthAccount().equals(account)) {
            if (ctxt.useIcalDelegation()) {
                addProperty(new CalendarProxyReadFor(getAccount()));
                addProperty(new CalendarProxyWriteFor(getAccount()));
                addProperty(new ProxyGroupMembership(getAccount()));
            }
        }
        addProperty(Acl.getPrincipalUrl(this));
        ArrayList<String> addrs = new ArrayList<String>();
        for (String addr : account.getMailDeliveryAddress())
            addrs.add(addr);
        for (String alias : account.getMailAlias())
            addrs.add(alias);
        String principalAddr = UrlNamespace.getPrincipalUrl(account);
        if (principalAddr.endsWith("/")) {
            principalAddr = principalAddr.substring(0, principalAddr.length() - 1);
        }
        addrs.add(principalAddr);
        addProperty(CalDavProperty.getCalendarUserAddressSet(addrs));
        addProperty(CardDavProperty.getAddressbookHomeSet(user));
        setProperty(DavElements.E_HREF, url);
        String cn = account.getAttr(Provisioning.A_cn);
        if (cn == null)
            cn = account.getName();
        setProperty(DavElements.E_DISPLAYNAME, cn);
        mUri = url;
    }

    @Override
    public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
        ArrayList<DavResource> proxies = new ArrayList<DavResource>();
        if (ctxt.useIcalDelegation()) {
            try {
                proxies.add(new CalendarProxyRead(getOwner(), mUri));
                proxies.add(new CalendarProxyWrite(getOwner(), mUri));
            } catch (ServiceException e) {
            }
        }
        return proxies;
    }

    @Override
    public void delete(DavContext ctxt) throws DavException {
        throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    private static QName[] SUPPORTED_REPORTS = {
            DavElements.E_ACL_PRINCIPAL_PROP_SET,
            DavElements.E_PRINCIPAL_MATCH,
            DavElements.E_PRINCIPAL_PROPERTY_SEARCH,
            DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET,
            DavElements.E_EXPAND_PROPERTY
    };

    @Override
    protected QName[] getSupportedReports() {
        return SUPPORTED_REPORTS;
    }
}
