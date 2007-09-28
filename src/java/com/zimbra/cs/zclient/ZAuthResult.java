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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;

import java.util.List;
import java.util.Map;

public class ZAuthResult {
    private String mAuthToken;
    private long mExpires;
    private long mLifetime;
    private String mRefer;
    private String mSessionId;
    private Map<String, List<String>> mAttrs;
    private Map<String, List<String>> mPrefs;
	private String mSkin;

    public ZAuthResult(Element e) throws ServiceException {
        mAuthToken = e.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        mLifetime = e.getAttributeLong(AccountConstants.E_LIFETIME);
        mExpires = System.currentTimeMillis() + mLifetime;
        Element re = e.getOptionalElement(AccountConstants.E_REFERRAL);
        if (re != null) mRefer = re.getText();
        mAttrs = ZGetInfoResult.getMap(e, AccountConstants.E_ATTRS, AccountConstants.E_ATTR);
        mPrefs = ZGetInfoResult.getMap(e, AccountConstants.E_PREFS, AccountConstants.E_PREF);
		Element skinEl = e.getOptionalElement(AccountConstants.E_SKIN);
		if (skinEl != null) {
			mSkin = skinEl.getText();
		}
	}

    public String getAuthToken() {
        return mAuthToken;
    }

    public String getSessionId() {
        return mSessionId;
    }

    void setSessionId(String id) {
        mSessionId = id;
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

    public Map<String, List<String>> getAttrs() {
        return mAttrs;
    }

    public Map<String, List<String>> getPrefs() {
        return mPrefs;
    }

    public String getSkin() {
        return mSkin;
    }
}
