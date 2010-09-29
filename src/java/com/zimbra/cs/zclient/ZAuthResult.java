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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.util.MapUtil;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.account.type.Session;

public class ZAuthResult {
    
    private long expires;
    private AuthResponse data;
    
    /*
    public ZAuthResult(Element e) throws ServiceException {
        mAuthToken = new ZAuthToken(e.getElement(AccountConstants.E_AUTH_TOKEN), false);

        mLifetime = e.getAttributeLong(AccountConstants.E_LIFETIME);
        mExpires = System.currentTimeMillis() + mLifetime;
        mRefer = e.getAttribute(AccountConstants.E_REFERRAL, null);
        mAttrs = ZGetInfoResult.getMap(e, AccountConstants.E_ATTRS, AccountConstants.E_ATTR);
        mPrefs = ZGetInfoResult.getMap(e, AccountConstants.E_PREFS, AccountConstants.E_PREF);
        mSkin = e.getAttribute(AccountConstants.E_SKIN, null);
    }
    */
    
    public ZAuthResult(AuthResponse res) {
        data = res;
        expires = data.getLifetime() + System.currentTimeMillis();
    }

    public ZAuthToken getAuthToken() {
        return new ZAuthToken(data.getAuthToken());
    }

    public String getSessionId() {
        Session session = data.getSession();
        if (session == null) {
            return null;
        }
        return session.getId();
    }

    void setSessionId(String id) {
        Session session = data.getSession();
        if (session == null) {
            session = new Session();
            data.setSession(session);
        }
        session.setId(id);
    }
    
    public long getExpires() {
        return expires;
    }
    
    public long getLifetime() {
        return data.getLifetime();
    }
    
    public String getRefer() {
        return data.getRefer();
    }

    public Map<String, List<String>> getAttrs() {
        return MapUtil.multimapToMapOfLists(data.getAttrsMultimap());
    }

    public Map<String, List<String>> getPrefs() {
        return MapUtil.multimapToMapOfLists(data.getPrefsMultimap());
    }

    public String getSkin() {
        return data.getSkin();
    }
}
