/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.soap;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.Element;

class ZSoapAuthResult {
    private String mAuthToken;
    private long mExpires;
    private long mLifetime;
    private String mRefer;

    ZSoapAuthResult(Element e) throws ServiceException {
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
