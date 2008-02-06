/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.soap.SoapServlet;

public class ZimbraAuthProvider extends AuthProvider{

    ZimbraAuthProvider() {
        super(ZIMBRA_AUTH_PROVIDER);
    }
    
    protected AuthToken authToken(HttpServletRequest req, boolean isAdmin) throws AuthTokenException {
        throw new AuthTokenException("not yet implemented");
    }

    /*
     * isAdmin is not looked at by the zimbra auth provider
     */
    protected AuthToken authToken(Element soapCtxt, Map engineCtxt) throws AuthTokenException  {
        String rawAuthToken = (soapCtxt == null ? null : soapCtxt.getAttribute(HeaderConstants.E_AUTH_TOKEN, null));
        
        // check for auth token in engine context if not in header  
        if (rawAuthToken == null)
            rawAuthToken = (String) engineCtxt.get(SoapServlet.ZIMBRA_AUTH_TOKEN);
        
        if (!StringUtil.isNullOrEmpty(rawAuthToken))
            return AuthToken.getAuthToken(rawAuthToken);
        else
            return null;
    }
}
