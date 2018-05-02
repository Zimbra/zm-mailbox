/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.google.common.collect.Maps;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.admin.GetServer;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.cs.util.ZTestWatchman;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;

import junit.framework.Assert;

public class GetServerEncodingTest {

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new ZTestWatchman();

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test4012@zimbra.com", "secret", attrs);
    }

    @Test
    public void test4012() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test4012@zimbra.com");
        com.zimbra.cs.account.Server server = acct.getServer();
        Map<String, Object> context = ServiceTestUtil.getRequestContext(acct);
        ZimbraSoapContext zsc = (ZimbraSoapContext) context.get(SoapEngine.ZIMBRA_CONTEXT);
        Element response = zsc.createElement(AdminConstants.GET_SERVER_RESPONSE);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraMailMode", "zimbraMailMode");
        attrs.put("zimbraRedoLogProvider", "com.zimbra.cs.redolog.MockRedoLogProvider");
        attrs.put("zimbraLowestSupportedAuthVersion", 1);
        attrs.put("zimbraId", "b961fa11-e333-49d1-93cf-5fbe675e8708");
        attrs.put("zimbraServiceHostname", "localhost");
        attrs.put("zimbraSmtpPort", 7030);
        attrs.put("zimbraSSLPrivateKey",
            "-----BEGIN PRIVATE KEY-----MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDj5CdJz5QNpk7skWqv7lC7gaymGiqbpEXAe8za1JTgjKkL0gzQNM79aGr+lnYKXU+rto15QT9uDyvVnY/iPlpeLtCEin9OIEccLsPivG0R8gL8O2HGF86ns+ZNbjjJVetr/qk1mFN/91qMfOhL/F6tBR5zSIVdCE4bdqCqpq4HywEaKyXxCwU4bKlmawrLRZeITiMQ6Je/VsFvz2wj7yrlCH5HKtoyaNuLR9KH1HHbFB/p9JCK9/qZpq5p4vNXr0fGs3PFQAfhau2ySmo1bEhYDIs3/nBLrXP3OHQqfEPEE1R7BrcEBBtUaY8t5JSduOQRr63qDyGjXnu+xcwA46v/AgMBAAECggEAUa57LoeKZ4IOk9hjRv/CTBLkkPyb/QFaRu2YtW6wlfOUu7nkAdSLxGRixTGkyX48ii16c9WhKI+jhINfCRaUSWG6N2d0zcnf8wgICgLDjUUTMNkP6HKsDYv7phE1pWR4Z1L1z1Hzy9Aa0nQKxwGD5bwJ+AQsWPYbGNjiKYhopD2/zbTnhhYCA9/aSf2vKUa9ITWpW7nbYK1dtOW+eX+CrBRBO54KQ0OlJBnzk2oZv7IOlp6PUck0HMurP/N/EV1lVwvsMaddy3osHrB1qjA/vSX+wYMaNRSyN8p9hRTAAPQKJKy3feXUrE/kzhV/MP0DYbHkV1SYDNt0nDarZ8wDAQKBgQD2r0UIdMmOwjGoR0y4/EXSvEf6+KP+t0GDEYfl9C5/Ei3BTFPU5bWg/1TEQJ5R4AsI6TchHE1k0o1tENZ09SO/9ESQPfRZHbbaMit64SAg3e8mvChEvVfaunDapOTuwiqzvTjc12plwXrzrkt2xtXLpWvZ8afWVaBhEZysieXvQQKBgQDsfzSc4AuUprqqLU7pyQGSy9zjv2Pt25aUdiyiVYawzDfcQly4x5b/b/NodQ2SG/5zN2/L+fvjHJvWcKsHx984siOGVMxEK5uUz00eKbVB/PZP+oT7MALStaI0U0gnwtnWgXCxMKl2QOfWnF3AMkF99xKywsuyFIg6ojoE9IYLPwKBgQDzxmi13pOAXC+uWCdddw+ZHS8UuLl3calv2NcvS4rXUCOfLcp6TTacDza5ahIKXxkIiU9NjSZ+SAQyj70ef1IA02ceE9twZYjZP1Lwb6DMWgWHhdFVfLdhE3WK3ADQYVjJnmie9NHUFMtoHAm/KucEBEj8a26sxJlk0368ktmDAQKBgBGMctv9J/7UzF8aU5O3bZ118SMZLZIVzDuh9Tfqfr8ZuD9o0TaI4OR9ayNiJCqmVyA3id0p5I36rnmgDKDcLO0pEsfB/RJF5hqJs2A8mg2WdrSCk2GMM3ltLucREvaYV8+59SHAyaJTuKBNJAvB7ugo8ENBfxnsuhsXtJRvjI7DAoGAG7HrReM3cX8k8jqopY8xlT03Q372v07PL8fs2aOP+zsA580IdDw0Xvea+dmykCzpj3DmBci+TOE7DYb1SW6+NPN4FOP6o6TFogrgNa/0LAWXPuct+1e5vy9F1/jFpvNbbD8uTJKdJWGKO78wLVybn9gTJ95ZZcBfytQMyXpqysk=-----END PRIVATE KEY-----");
        GetServer.encodeServer(response, server, true, null, null);
        Assert.assertEquals(true, response.prettyPrint().contains("<a n=\"zimbraSSLPrivateKey\">VALUE-BLOCKED</a>"));
    }

    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}