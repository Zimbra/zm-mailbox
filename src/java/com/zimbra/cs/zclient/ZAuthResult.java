/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.Element;

public class ZAuthResult {
    private String mAuthToken;
    private long mExpires;
    private long mLifetime;
    private String mRefer;

    public ZAuthResult(Element e) throws ServiceException {
        mAuthToken = e.getElement(AccountService.E_AUTH_TOKEN).getText();
        mLifetime = e.getAttributeLong(AccountService.E_LIFETIME);
        mExpires = System.currentTimeMillis() + mLifetime;
        Element re = e.getOptionalElement(AccountService.E_REFERRAL); 
        if (re != null) mRefer = re.getText();
    }

    public String getAuthToken() {
        return mAuthToken;
    }
    
    public long getExpires() {
        return mExpires;
    }
    
    public long getLifetime() {
        return mLifetime;
    }
    
    public String getRefer() {
        return mRefer;
    }
}
