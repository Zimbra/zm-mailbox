/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
