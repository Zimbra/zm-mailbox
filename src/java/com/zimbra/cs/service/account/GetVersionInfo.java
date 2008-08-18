/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.soap.ZimbraSoapContext;


public class GetVersionInfo extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        if (!Provisioning.getInstance().getLocalServer().getBooleanAttr(Provisioning.A_zimbraSoapExposeVersion, false)) {
            throw ServiceException.PERM_DENIED("Version info is not available.");
        }
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(AccountConstants.GET_VERSION_INFO_RESPONSE);
        Element infoEl = response.addElement(AccountConstants.E_VERSION_INFO_INFO);
        
        String fullVersionInfo = BuildInfo.VERSION;
        if (!StringUtil.isNullOrEmpty(BuildInfo.TYPE))
            fullVersionInfo = fullVersionInfo + "." + BuildInfo.TYPE;
            
        infoEl.addAttribute(AccountConstants.A_VERSION_INFO_VERSION, fullVersionInfo);
        infoEl.addAttribute(AccountConstants.A_VERSION_INFO_RELEASE, BuildInfo.RELEASE);
        infoEl.addAttribute(AccountConstants.A_VERSION_INFO_DATE, BuildInfo.DATE);
        infoEl.addAttribute(AccountConstants.A_VERSION_INFO_HOST, BuildInfo.HOST);
        return response;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }
}
