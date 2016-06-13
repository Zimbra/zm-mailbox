/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.account.message.ChangePasswordResponse;

public class ZChangePasswordResult {
    private ZAuthToken mAuthToken;
    private long mExpires;
    private long mLifetime;

    public ZChangePasswordResult(Element e) throws ServiceException {
        String authToken = e.getAttribute(AccountConstants.E_AUTH_TOKEN);
        mAuthToken = new ZAuthToken(null, authToken, null);

        mLifetime = e.getAttributeLong(AccountConstants.E_LIFETIME);
        mExpires = System.currentTimeMillis() + mLifetime;
    }
    
    public ZChangePasswordResult(ChangePasswordResponse res) {
        mAuthToken = new ZAuthToken(null, res.getAuthToken());
        mLifetime = res.getLifetime();
        mExpires = System.currentTimeMillis() + mLifetime;
    }

    public ZAuthToken getAuthToken() {
        return mAuthToken;
    }

    public long getExpires() {
        return mExpires;
    }

    public long getLifetime() {
        return mLifetime;
    }
}