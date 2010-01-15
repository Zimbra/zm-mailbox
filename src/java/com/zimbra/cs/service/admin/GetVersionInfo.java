/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.util.BuildInfo;
import com.zimbra.common.soap.AdminConstants;

public class GetVersionInfo extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(AdminConstants.GET_VERSION_INFO_RESPONSE);
        Element infoEl = response.addElement(AdminConstants.A_VERSION_INFO_INFO);
        
        String fullVersionInfo = BuildInfo.VERSION;
        if (!StringUtil.isNullOrEmpty(BuildInfo.TYPE))
            fullVersionInfo = fullVersionInfo + "." + BuildInfo.TYPE;
            
        infoEl.addAttribute(AdminConstants.A_VERSION_INFO_VERSION, fullVersionInfo);
        infoEl.addAttribute(AdminConstants.A_VERSION_INFO_RELEASE, BuildInfo.RELEASE);
        infoEl.addAttribute(AdminConstants.A_VERSION_INFO_DATE, BuildInfo.DATE);
        infoEl.addAttribute(AdminConstants.A_VERSION_INFO_HOST, BuildInfo.HOST);
        return response;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

}
