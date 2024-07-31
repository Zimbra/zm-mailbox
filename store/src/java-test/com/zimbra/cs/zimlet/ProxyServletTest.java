package com.zimbra.cs.zimlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.ProtocolException;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ProxyServlet.class)
@PowerMockIgnore({"javax.management.*"})
public class ProxyServletTest {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String PROXY_SERVLET_WHITELIST = " 127.0.0.69, 127.0.0.70, 127.0.0.72/32, 10.0.0.0/8 ,fe80::/10";

    private static final String[] ALLOWED_DOMAINS = new String[] {
        "*.example.com",
        "localhost",
        "sneaky.com",
        "whitelisted.com",
        "badapple.biz",
        "ipv6white.com",
        "ipv6black.com",
        "ipv46home.org",
    };

    private static final String COSPREFIX = "proxyservlettestcos";
    private static int cosNumber = 0;

    private Cos cos;
    private Account account;
    private AuthToken authToken = mock(AuthToken.class);

    private static final Map<String, InetAddress[]> dnsMapping = new HashMap<>();;

    private static InetAddress getaddrbyname(final String host) throws Exception {
        return InetAddress.getByName(host);
    }

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();

        dnsMapping.put("foo.example.com", new InetAddress[]{ getaddrbyname("69.68.67.66"), getaddrbyname("68.67.66.65") });
        dnsMapping.put("sneaky.com",      new InetAddress[]{ getaddrbyname("67.66.65.64"), getaddrbyname("192.168.18.42") });
        dnsMapping.put("fazigu.org",      new InetAddress[]{ getaddrbyname("69.55.75.59") });
        dnsMapping.put("localhost",       new InetAddress[]{ getaddrbyname("127.0.0.1") });
        dnsMapping.put("whitelisted.com", new InetAddress[]{ getaddrbyname("127.0.0.69") });
        dnsMapping.put("badapple.biz",    new InetAddress[]{ getaddrbyname("127.0.0.72"), getaddrbyname("127.0.0.71") });

        dnsMapping.put("ipv46home.org",  new InetAddress[]{ getaddrbyname("::ffff:127.0.0.1") });         // IPv4 loopback mapped to IPv6 address
        dnsMapping.put("ipv6black.com",  new InetAddress[]{ getaddrbyname("::1") });                      // IPv6 loopback
        dnsMapping.put("ipv6white.com",  new InetAddress[]{ getaddrbyname("fe80::42:78ff:fe9a:5ab") });   // link-local address, and we've whitelisted them all
    }

    @Before
    public void setUp() throws Exception {
        MockProvisioning prov = new MockProvisioning();

        cos = prov.createCos(String.format("%s%d", COSPREFIX, cosNumber++), new HashMap<String,Object>());
        cos.setProxyAllowedDomains(ALLOWED_DOMAINS);

        account = prov.createAccount(USERNAME, PASSWORD, new HashMap<String,Object>());
        account.setCOSId(cos.getId());
        when(authToken.getAccountId()).thenReturn(account.getId());

        Provisioning.setInstance(prov);

        PowerMockito.mockStatic(ProxyServlet.class);

        when(ProxyServlet.getAllInetAddressesByName(anyString())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String host = String.valueOf(args[0]);

            if (dnsMapping.containsKey(host)) {
                return dnsMapping.get(host);
            }

            throw new UnknownHostException("failed to resolve " + host);
        });

        when(ProxyServlet.isRedirectStatus(anyInt())).thenCallRealMethod();
        when(ProxyServlet.getAllowedDomains(anyObject())).thenCallRealMethod();
        when(ProxyServlet.isRestrictedIp(anyObject())).thenCallRealMethod();
        when(ProxyServlet.isAllowedDomain(anyString(), anyObject())).thenCallRealMethod();
        when(ProxyServlet.isWhitelistedAddress(anyObject())).thenCallRealMethod();
        when(ProxyServlet.checkPermissionOnTarget(anyObject(), anyObject())).thenCallRealMethod();
        when(ProxyServlet.shouldFollowRedirectLocation(anyObject(), anyObject())).thenCallRealMethod();

        LC.zimbra_proxy_servlet_whitelist.setDefault(PROXY_SERVLET_WHITELIST);
    }

    @Test
    public void testRedirectStatusWhen3xx() {
        assertTrue("301", ProxyServlet.isRedirectStatus(301));
        assertTrue("300", ProxyServlet.isRedirectStatus(300));
    }

    @Test
    public void testRedirectStatusWhenNot3xx() {
        assertFalse("100", ProxyServlet.isRedirectStatus(100));
        assertFalse("204", ProxyServlet.isRedirectStatus(204));
        assertFalse("404", ProxyServlet.isRedirectStatus(404));
        assertFalse("500", ProxyServlet.isRedirectStatus(500));
    }

    @Test
    public void testNonWhitelistedDomainRejected() throws ProtocolException {
        assertFalse(isLocationRedirectable("http://fazigu.org"));
    }

    @Test
    public void testMalformedLocationRejected() throws ProtocolException {
        assertFalse(isLocationRedirectable("http://f a z i g u.org"));
    }

    @Test
    public void testWhitelistedDomainOK() throws ProtocolException {
        assertTrue(isLocationRedirectable("http://foo.example.com/foo"));
    }

    @Test
    public void testRelativeLocationRejected() throws ProtocolException {
        assertFalse(isLocationRedirectable("/foo"));
    }

    private boolean isLocationRedirectable(final String location) throws ProtocolException {
        Header header = new BasicHeader("Location", location);
        return ProxyServlet.shouldFollowRedirectLocation(header, authToken);
    }

    private void testCpot(final String url, final boolean expectedResult) {
        URL target;

        try {
            target = new URL(url);
        } catch (final MalformedURLException ex) {
            throw new Error("failed to build url from string " + url);
        }

        final boolean actualResult = ProxyServlet.checkPermissionOnTarget( target, authToken);
        if (actualResult != expectedResult) {
            fail(String.format("checkPermissionOnTarget(%s) was unexpectedly %s", url,
                    actualResult ? "accepted" : "rejected"));
        }
    }

    @Test
    public void testCpotNull() {
        assertFalse("checkPermissionOnTarget should reject nulls", ProxyServlet.checkPermissionOnTarget(null, authToken));
    }

    @Test
    public void testCpotUnknownDomain() {
        testCpot("http://unknown.foo", false);
    }

    @Test
    public void testCpotWildcardDomainOK() {
        testCpot("http://foo.example.com", true);
    }

    @Test
    public void testCpotLocalhostFail() {
        testCpot("http://localhost", false);
    }

    @Test
    public void testCpotKnownUnallowedDomainFail() {
        testCpot("http://fazigu.org", false);
    }

    @Test
    public void testCpotKnownDomainWithOneBadAddressFail() {
        testCpot("http://sneaky.com", false);
    }

    @Test
    public void testCpotWhitelistedDomainWithinPrivateRangeOK() {
        testCpot("http://whitelisted.com", true);
    }

    @Test
    public void testCpotHalflistedDomainWithinPrivateRange() {
        testCpot("http://badapple.biz", false);
    }

    @Test
    public void testCpotIPv6NonWhitelistedAddressFail() {
        testCpot("http://ipv6black.com", false);
    }

    @Test
    public void testCpoaIPv4LocalhostMappedToIPv6() {
        testCpot("http://ipv46home.org", false);
    }

    @Test
    public void testCpotIPv6WhitelistedAddressOK() {
        testCpot("http://ipv6white.com", true);
    }

    private void doCidrStreamFilterSpeedtest(final int iterations, final String host) throws Exception {
        InetAddress addr = InetAddress.getByName(host);
        long start = System.nanoTime();

        for (int counter = 0; counter < iterations; counter++) {
            ProxyServlet.isRestrictedIp(addr);
        }

        long end = System.nanoTime();
        long dur = end - start;

        System.err.printf("ProxyServlet.isRestrictedIp(\"%s\") * %,d =~ %,d millis (%,.3f nanos/call)\n",
             addr, iterations, dur/1_000_000, (double)dur/iterations);
    }

    private void doCidrStreamFilterSpeedtest(final String host) throws Exception {
        doCidrStreamFilterSpeedtest(100_000, host);
    }

    @Ignore
    @Test
    public void speedtestCidrStreamFilter() throws Exception {
        doCidrStreamFilterSpeedtest("127.0.0.1");
        doCidrStreamFilterSpeedtest("255.255.255.255");
    }

    @After
    public void tearDown() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        prov.deleteAccount(account.getId());
        prov.deleteCos(cos.getId());
    }
}

