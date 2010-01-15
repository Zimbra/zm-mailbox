/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2010 Zimbra, Inc.
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
