/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.zimbra.common.util.ngxlookup.NginxAuthServer;

public class NginxAuthServerTest {

    @Test
    public void testIpV4() {
        NginxAuthServer server = new NginxAuthServer("10.11.12.13", "8080", "user1");
        assertNotNull(server);
        assertNotNull(server.getNginxAuthServer());
        assertEquals("10.11.12.13:8080", server.getNginxAuthServer());
    }

    @Test
    public void testIpV6() {
        NginxAuthServer server = new NginxAuthServer("2a02:1800:1b3:3:0:0:f00:576", "443", "user1");
        assertNotNull(server);
        assertNotNull(server.getNginxAuthServer());
        assertEquals("[2a02:1800:1b3:3:0:0:f00:576]:443", server.getNginxAuthServer());
    }
}
