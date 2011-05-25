/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetVersionInfoResponse;
import com.zimbra.soap.admin.type.VersionInfo;

import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.util.BuildInfo;

public class GetVersionInfo extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        VersionInfo versionInfo = new VersionInfo();
        String fullVersionInfo = BuildInfo.VERSION;
        if (!StringUtil.isNullOrEmpty(BuildInfo.TYPE)) {
            fullVersionInfo = fullVersionInfo + "." + BuildInfo.TYPE;
            versionInfo.setType(BuildInfo.TYPE);
        }    
        versionInfo.setVersion(fullVersionInfo);
        versionInfo.setRelease(BuildInfo.RELEASE);
        versionInfo.setBuildDate(BuildInfo.DATE);
        versionInfo.setHost(BuildInfo.HOST);
        
        versionInfo.setMajorVersion(BuildInfo.MAJORVERSION);
        versionInfo.setMinorVersion(BuildInfo.MINORVERSION);
        versionInfo.setMicroVersion(BuildInfo.MICROVERSION);
        versionInfo.setPlatform(BuildInfo.PLATFORM);
        GetVersionInfoResponse resp = new GetVersionInfoResponse(versionInfo);
        return lc.jaxbToElement(resp);
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}
