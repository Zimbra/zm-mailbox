/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.util.BuildInfo;

public class GetVersionInfo extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(AdminService.GET_VERSION_INFO_RESPONSE);
        Element infoEl = response.addElement(AdminService.A_VERSION_INFO_INFO);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_VERSION, BuildInfo.VERSION);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_RELEASE, BuildInfo.RELEASE);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_DATE, BuildInfo.DATE);
        infoEl.addAttribute(AdminService.A_VERSION_INFO_HOST, BuildInfo.HOST);
        return response;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

}
