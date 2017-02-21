/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetVersionInfoResponse;
import com.zimbra.soap.admin.type.VersionInfo;

public class GetVersionInfo extends AdminDocumentHandler {

    @Override
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

    @Override
    public boolean needsAdminAuth(Map<String, Object> context) {
        return true;
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}
