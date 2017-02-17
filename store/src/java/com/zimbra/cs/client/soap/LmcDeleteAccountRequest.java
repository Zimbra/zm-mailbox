/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.soap.AdminConstants;

public class LmcDeleteAccountRequest extends LmcSoapRequest {
    String mAccountId;

    public LmcDeleteAccountRequest(String accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId cannot be null");
        }
        mAccountId = accountId;
    }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AdminConstants.DELETE_ACCOUNT_REQUEST);
        DomUtil.add(request, AdminConstants.E_ID, mAccountId);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) {
        return new LmcDeleteAccountResponse();
    }
}
