package com.zimbra.cs.zimlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.zimlet.ProxyServlet.RestrictiveRedirectStrategy;

public class ProxyServletTest {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String[] ALLOWED_DOMAINS = new String[] { "*.webex.com" };

    private static final String COSPREFIX = "proxyservlettestcos";
    private static int cosNumber = 0;

    private Cos cos;
    private Account account;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws ServiceException {
        MockProvisioning prov = new MockProvisioning();

        cos = prov.createCos(String.format("%s%d", COSPREFIX, cosNumber++), new HashMap<String,Object>());
        cos.setProxyAllowedDomains(ALLOWED_DOMAINS);

        account = prov.createAccount(USERNAME, PASSWORD, new HashMap<String,Object>());
        account.setCOSId(cos.getId());

        Provisioning.setInstance(prov);
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
        assertTrue(isLocationRedirectable("http://foo.webex.com/foo"));
    }

    @Test
    public void testRelativeLocationRejected() throws ProtocolException {
        assertFalse(isLocationRedirectable("/foo"));
    }

    public boolean isLocationRedirectable(final String location) throws ProtocolException {
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = mock(HttpResponse.class);
        Header header = new BasicHeader("Location", location);
        //when(response.getHeaders("Location")).thenReturn(new Header[] { header });
        when(response.getFirstHeader("Location")).thenReturn(header);

        HttpContext context = mock(HttpContext.class);
        AuthToken authToken = mock(AuthToken.class);
        when(authToken.getAccountId()).thenReturn(account.getId());

        RestrictiveRedirectStrategy strategy = new RestrictiveRedirectStrategy(authToken);
        return strategy.isRedirected(request, response, context);
    }

    @After
    public void tearDown() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        prov.deleteAccount(account.getId());
        // MockProvisioning doesn't support deleteCos as of this writing.
        //prov.deleteCos(cos.getId());
    }
}
