/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * Created by IntelliJ IDEA. User: ccao Date: Oct 1, 2008 Time: 12:17:14 PM To
 * change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class AdminFileDownload extends ZimbraServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            // check the auth token
            AuthToken authToken = getAdminAuthTokenFromCookie(req, resp);
            if (authToken == null) {
                return;
            }
            resp.sendRedirect("/service/extension/com_zimbra_bulkprovision/bulkdownload?" + req.getQueryString());
        } catch (Exception e) {
            ZimbraLog.webclient.error(e);
            return;
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
