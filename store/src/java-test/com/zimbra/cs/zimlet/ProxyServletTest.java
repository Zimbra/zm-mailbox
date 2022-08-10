package com.zimbra.cs.zimlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
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
    private static final String COSNAME = "cosname";

    private static final String[] ALLOWED_DOMAINS = new String[] { "*.webex.com" };

    private static final String H_LOCATION = "Location";

    private Cos cos;
    private Account account;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws ServiceException {
        MockProvisioning prov = new MockProvisioning();

        cos = prov.createCos(COSNAME, Collections.emptyMap());
        cos.setProxyAllowedDomains(ALLOWED_DOMAINS);

        account = prov.createAccount(USERNAME, PASSWORD, Collections.emptyMap());
        account.setCOSId(cos.getId());

		Provisioning.setInstance(prov);
    }

    @Test
    public void testNonWhitelistedDomain() throws ProtocolException {
        assertFalse(isLocationRedirectable("http://fazigu.org"));
    }

    @Test
    public void testMalformedLocation() throws ProtocolException {
        assertFalse(isLocationRedirectable("http://f a z i g u.org"));
    }

    @Test
    public void testWhitelistedDomain() throws ProtocolException {
        assertTrue(isLocationRedirectable("http://foo.webex.com/foo"));
    }

    @Test
    public void testRelativeLocation() throws ProtocolException {
        assertTrue(isLocationRedirectable("/foo"));
    }

    public boolean isLocationRedirectable(final String location) throws ProtocolException {
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = mock(HttpResponse.class);
        Header header = new BasicHeader(H_LOCATION, location);
        when(response.getHeaders(H_LOCATION)).thenReturn(new Header[] { header });

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
        prov.deleteCos(cos.getId());
    }
}
