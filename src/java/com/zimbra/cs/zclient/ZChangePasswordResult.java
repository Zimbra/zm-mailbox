/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

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