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

package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.cs.client.*;
import com.zimbra.cs.zclient.ZAuthToken;

public class LmcAdminAuthRequest extends LmcSoapRequest {

    private String mUsername;

    private String mPassword;

    public void setUsername(String u) {
        mUsername = u;
    }

    public void setPassword(String p) {
        mPassword = p;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;   // high security interface
    } 

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AdminConstants.AUTH_REQUEST);
        DomUtil.addAttr(request, AdminConstants.A_NAME, mUsername);
        DomUtil.addAttr(request, AdminConstants.E_PASSWORD, mPassword);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
            throws ServiceException 
    {
        // get the auth token out, no default, must be present or a service exception is thrown
        String authToken = DomUtil.getString(responseXML, AdminConstants.E_AUTH_TOKEN);
        ZAuthToken zat = new ZAuthToken(null, authToken, null);
        // get the session id, if not present, default to null
        String sessionId = DomUtil.getString(responseXML, HeaderConstants.E_SESSION_ID, null);

        LmcAdminAuthResponse responseObj = new LmcAdminAuthResponse();
        LmcSession sess = new LmcSession(zat, sessionId);
        responseObj.setSession(sess);
        return responseObj;
    }
}
