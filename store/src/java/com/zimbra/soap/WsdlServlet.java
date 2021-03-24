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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
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
                Domain domain = getBestDomain(req.getServerName());
                if (!WsdlGenerator.handleRequestForWsdl(pathInfo, resp.getOutputStream(),
                        getSoapURL(domain), getSoapAdminURL(domain))) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } finally {
            ZimbraLog.clearContext();
        }
    }

    private Domain getBestDomain(String serverName) {
        Provisioning prov = Provisioning.getInstance();
        Domain domain;
        try {
            domain = prov.getDomainByName(serverName);
            if (domain != null) {
                return domain;
            }
        } catch (ServiceException e) {
        }
        try {
            Server server = prov.getLocalServer();
            if (server != null) {
                domain = prov.getDomainByName(serverName);
                if (domain != null) {
                    return domain;
                }
            }
        } catch (ServiceException e) {
        }
        try {
            return prov.getDefaultDomain();
        } catch (ServiceException e) {
            return null;
        }
    }

    private static String getSoapAdminURL(Domain domain) {
        try {
            return URLUtil.getPublicAdminConsoleSoapURLForDomain(Provisioning.getInstance().getLocalServer(), domain);
        } catch (ServiceException e) {
            return WsdlServiceInfo.localhostSoapAdminHttpsURL;
        }
    }

    private static String getSoapURL(Domain domain) {
        try {
            return URLUtil.getPublicURLForDomain(Provisioning.getInstance().getLocalServer(),
                    domain, AccountConstants.USER_SERVICE_URI, true);
        } catch (ServiceException e) {
            return WsdlServiceInfo.localhostSoapHttpURL;
        }
    }


}
