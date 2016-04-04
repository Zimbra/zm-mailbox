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
