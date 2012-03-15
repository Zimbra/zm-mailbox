package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 VMware, Inc.
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
