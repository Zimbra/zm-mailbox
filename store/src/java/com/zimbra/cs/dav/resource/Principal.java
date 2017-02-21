/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
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

    protected Account setAccount(Account acct) {
        mAccount = acct;
        return mAccount;
    }

    public Account getAccount() {
        if (mAccount != null) {
            return mAccount;
        } else {
            try {
                mAccount = Provisioning.getInstance().get(AccountBy.name, mOwner);
            } catch (ServiceException e) {
                ZimbraLog.dav.info("No account associated with Principal owner='%s' href='%s'", mOwner, this.getHref());
                mAccount = null;
            }
            return mAccount;
        }
    }

    /** deliberately private.  Use getAccount() to make sure is initialized if wasn't provided in constructor */
    private Account mAccount;
}
