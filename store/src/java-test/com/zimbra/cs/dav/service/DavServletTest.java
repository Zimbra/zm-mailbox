package com.zimbra.cs.dav.service;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthProvider.class})
public class DavServletTest {

    private static final String TEST_ACCOUNT_NAME = "davtest@example.zimbra.com";

    private static Account testAccount;

    private DavServlet servlet;

    private HttpServletRequest mockReq;

    private HttpServletResponse mockResp;

    private ByteArrayOutputStream responseBody;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        testAccount = prov.createAccount(TEST_ACCOUNT_NAME, "password", new HashMap<>());
    }

    @Before
    public void setUp() throws Exception {
        servlet = new DavServlet();

        mockReq = mock(HttpServletRequest.class);
        mockResp = mock(HttpServletResponse.class);
        responseBody = new ByteArrayOutputStream();

        when(mockResp.getOutputStream()).thenReturn(new javax.servlet.ServletOutputStream() {
            @Override public boolean isReady() {
                return true;
            }

            @Override public void setWriteListener(javax.servlet.WriteListener l) {

            }

            @Override public void write(int b) {
                responseBody.write(b);
            }
        });

        // common request setup
        when(mockReq.getMethod()).thenReturn("GET");
        when(mockReq.getPathInfo()).thenReturn("/dav/" + TEST_ACCOUNT_NAME + "/");

        PowerMockito.mockStatic(AuthProvider.class);
    }

    @After
    public void tearDown() {
        responseBody.reset();
    }

    @Test
    public void testValidAuthUsageTokenAuthenticatesSuccessfully() throws Exception {
        AuthToken mockToken = mock(AuthToken.class);
        when(mockToken.getAccountId()).thenReturn(testAccount.getId());
        PowerMockito.when(AuthProvider.getAuthToken(any(HttpServletRequest.class), eq(false)))
                .thenReturn(mockToken);
        PowerMockito.when(AuthProvider.validateAuthToken(
                        any(Provisioning.class), eq(mockToken), eq(true), eq(Usage.AUTH)))
                .thenReturn(testAccount);

        try {
            servlet.service(mockReq, mockResp);
        } catch (Exception e) {
            // Post-auth processing may fail due to incomplete DAV request setup — acceptable.
            // We only care that auth was attempted with Usage.AUTH.
        }

        // verify validateAuthToken was called with Usage.AUTH
        PowerMockito.verifyStatic();
        AuthProvider.validateAuthToken(any(Provisioning.class), eq(mockToken), eq(true), eq(Usage.AUTH));

        // verify that Auth must have passed — 401 should NOT be set
        verify(mockResp, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void testPreMfaTfaTokenOnDavEndpointReturns401() throws Exception {
        AuthToken mockTfaToken = mock(AuthToken.class);
        when(mockTfaToken.getAccountId()).thenReturn(testAccount.getId());

        ServiceException authExpired = ServiceException.AUTH_EXPIRED();

        PowerMockito.when(AuthProvider.getAuthToken(any(HttpServletRequest.class), eq(false)))
                .thenReturn(mockTfaToken);
        PowerMockito.when(AuthProvider.validateAuthToken(
                        any(Provisioning.class), eq(mockTfaToken), eq(true), eq(Usage.AUTH)))
                .thenThrow(authExpired);

        servlet.service(mockReq, mockResp);

        // verify validateAuthToken was called with Usage.AUTH
        PowerMockito.verifyStatic();
        AuthProvider.validateAuthToken(
                any(Provisioning.class), eq(mockTfaToken), eq(true), eq(Usage.AUTH));

        // 401 with WWW-Authenticate
        verify(mockResp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(mockResp).setHeader(eq("WWW-Authenticate"), anyString());

        // full RFC 4918 XML body verification
        String body = responseBody.toString("UTF-8");
        assertTrue("Must contain <D:error>", body.contains("D:error"));
        assertTrue("Must contain <D:status>", body.contains("D:status"));
        assertTrue("Must contain 401 status text", body.contains("401"));
        assertTrue("Must contain <D:responsedescription>", body.contains("D:responsedescription"));
        assertTrue("Must contain auth credentials message",
                body.contains("auth credentials have expired"));
        assertTrue("Must contain <D:code>", body.contains("D:code"));
        assertTrue("Must contain AUTH_EXPIRED code", body.contains("service.AUTH_EXPIRED"));
    }
}
