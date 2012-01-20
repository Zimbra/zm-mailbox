/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.cs.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.util.AuthUtil;


public class DoSFilter extends org.eclipse.jetty.servlets.DoSFilter {
    
    @Override
    protected String extractUserId(ServletRequest request) {
        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest) request;
                boolean isAdminRequest = AuthUtil.isAdminRequest(req);
                AuthToken at = AuthProvider.getAuthToken(req, isAdminRequest);
                if (at != null)
                    return at.getAccountId();
            } 
        } catch (Exception e) {
            // ignore
            ZimbraLog.misc.debug("error while extracting authtoken" , e);
        }
        return null;
    }
}
