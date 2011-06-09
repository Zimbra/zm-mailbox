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
