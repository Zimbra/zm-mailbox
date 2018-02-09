/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.common.auth;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;

public class ZJWToken extends ZAuthToken {

    private String salt;

    public ZJWToken(String value, String salt) {
        super(value, null);
        this.salt = salt;
    }

    @Override
    public Element encodeSoapCtxt(Element ctxt) {
        Element ejwToken = ctxt.addNonUniqueElement(HeaderConstants.E_JWT_TOKEN);
        if (mProxyAuthToken != null) {
            ejwToken.setText(mProxyAuthToken);
        } else if (mValue != null) {
            ejwToken.setText(mValue);
        }
        Element saltEl = ctxt.addNonUniqueElement(HeaderConstants.E_JWT_SALT);
        saltEl.setText(salt);
        return ejwToken;
    }

    @Override
    public Element encodeAuthReq(Element authReq, boolean isAdmin) {
        return encodeSoapCtxt(authReq);
    }

    @Override
    public Map<String, String> cookieMap(boolean isAdmin) {
        Map<String, String> cookieMap = null;
        if (!StringUtil.isNullOrEmpty(salt)) {
            cookieMap = new HashMap<String, String>();
            cookieMap.put(ZimbraCookie.COOKIE_ZM_JWT, salt);
        }
        return cookieMap;  
    }

    public String getSalt() {
        return salt;
    }
}
