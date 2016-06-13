/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Provisioning;

public class RobotsServlet extends HttpServlet {
    private static final long serialVersionUID = 1058982623987983L;
    
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        boolean keepOutCrawlers = false;
        try {
            keepOutCrawlers = Provisioning.getInstance().getLocalServer().isMailKeepOutWebCrawlers();
        } catch (ServiceException e) {
        }
        ServletOutputStream out = response.getOutputStream();
        try {
            out.println("User-agent: *");
            if (keepOutCrawlers) {
                out.println("Disallow: /");
            } else {
                out.println("Allow: /");
            }
            String extra = LC.robots_txt.value();
            File extraFile = new File(extra);
            if (extraFile.exists()) {
                FileInputStream in = new FileInputStream(extraFile);
                ByteUtil.copy(in, true, out, false);
            }
            out.flush();
        } finally {
            out.close();
        }
    }
}
