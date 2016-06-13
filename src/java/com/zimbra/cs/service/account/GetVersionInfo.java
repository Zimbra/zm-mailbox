/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
