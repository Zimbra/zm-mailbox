/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.client;

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

    public String getCsrfToken() {
        return data.getCsrfToken();
    }
}
