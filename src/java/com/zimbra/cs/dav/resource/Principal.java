/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2012, 2013 Zimbra Software, LLC.
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
package com.zimbra.cs.dav.resource;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;

public class Principal extends DavResource {

    public Principal(Account authUser, String mainUrl) throws ServiceException {
        this(getOwner(authUser, mainUrl), mainUrl);
    }

    public Principal(String user, String mainUrl) throws ServiceException {
        super(mainUrl, user);
        if (!mainUrl.endsWith("/")) mainUrl = mainUrl + "/";
        setProperty(DavElements.E_HREF, mainUrl);
        setProperty(DavElements.E_GROUP_MEMBER_SET, null, true);
        setProperty(DavElements.E_GROUP_MEMBERSHIP, null, true);
        addResourceType(DavElements.E_PRINCIPAL);
        mUri = mainUrl;
    }
    public static String getOwner(Account acct, String url) throws ServiceException {
        // Originally used to sometimes return just the local part of the account name if the account was in the
        // default domain.  That behavior was also put in UrlNamespace.getPrincipalUrl(Account account) - but
        // has since gone from there, so removing from here for consistency.
        return acct.getName();
    }

    @Override
    public void delete(DavContext ctxt) throws DavException {
        throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    public Account getAccount() {
    	return mAccount;
    }

    protected Account mAccount;
}
