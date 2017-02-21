package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class ClearCookie extends AdminDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        HttpServletResponse servletResp = (HttpServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);
        
        for (Element eCookie : request.listElements(AdminConstants.E_COOKIE)) {
            String cookie = eCookie.getAttribute(AdminConstants.A_NAME);
            ZimbraCookie.clearCookie(servletResp, cookie);
        }
        
        Element resp = zsc.createElement(AdminConstants.CLEAR_COOKIE_RESPONSE);
        return resp;
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
