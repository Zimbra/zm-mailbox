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

package com.zimbra.soap;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.util.WsdlGenerator;
import com.zimbra.soap.util.WsdlServiceInfo;

/**
 * The wsdl service servlet - serves up files comprising Zimbra's WSDL definition
 */
public class WsdlServlet extends ZimbraServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -2727046288266393825L;

    @Override
    public void init() throws ServletException {
        LogFactory.init();

        String name = getServletName();
        ZimbraLog.soap.info("Servlet " + name + " starting up");
        super.init();
    }

    @Override
    public void destroy() {
        String name = getServletName();
        ZimbraLog.soap.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    @Override
    protected void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp)
    throws javax.servlet.ServletException, IOException {
        ZimbraLog.clearContext();
        try {
            addRemoteIpToLoggingContext(req);
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            ZimbraLog.soap.debug("WSDL SERVLET Received a GET pathInfo=" + pathInfo);
            if (pathInfo.matches("^[a-zA-Z]+\\.xsd$")) {
                InputStream is = JaxbUtil.class.getResourceAsStream(pathInfo);
                if (is == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                resp.setContentType(MimeConstants.CT_TEXT_XML);
                ByteUtil.copy(is, true /* closeIn */, resp.getOutputStream(), false /* closeOut */);
            } else {
                if (!WsdlGenerator.handleRequestForWsdl(pathInfo, resp.getOutputStream(),
                        getSoapURL(), getSoapAdminURL())) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } finally {
            ZimbraLog.clearContext();
        }
    }
    
    private static String getSoapAdminURL() {
        try {
            return URLUtil.getAdminURL(Provisioning.getInstance().getLocalServer());
        } catch (ServiceException e) {
            return WsdlServiceInfo.localhostSoapAdminHttpsURL;
        }
    }

    private static String getSoapURL() {
        try {
            return URLUtil.getServiceURL(Provisioning.getInstance().getLocalServer(),
                    AccountConstants.USER_SERVICE_URI, true);
        } catch (ServiceException e) {
            return WsdlServiceInfo.localhostSoapHttpURL;
        }
    }


}
