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

import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.MockHttpServletRequest;
import com.zimbra.cs.service.MockHttpServletResponse;
import com.zimbra.cs.servlet.ZimbraServlet;

public class ZimbraServletTest {
    
    private static String uri = "/Briefcase/上的发生的发";

    @Ignore("until bug 60345 is fixed")
    @Test
    public void proxyTest() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("test".getBytes("UTF-8"), new URL("http://localhost:7070/user1"+uri), "");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        ZimbraServlet.proxyServletRequest(req, resp, Provisioning.getInstance().getLocalServer(), uri, null);
    }
}
