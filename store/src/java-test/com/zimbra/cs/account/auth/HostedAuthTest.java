/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

package com.zimbra.cs.account.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link HostedAuth}, the "hosted" custom auth handler.
 * Uses a real {@link Account} from the in-memory MockProvisioning harness and
 * drives authenticate() against an unreachable URL so the real HTTP attempt
 * fails and is wrapped in an {@link AuthFailedServiceException} — exercising
 * header assembly, GET/POST selection, the protocol header, and the error
 * path. Also verifies the handler's registration through {@link ZimbraCustomAuth}
 * and its public header-name constants.
 */
public class HostedAuthTest {

    /** A TCP port that nothing listens on, so the HTTP request fails fast. */
    private static final String UNREACHABLE_URL = "http://127.0.0.1:1/auth";

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        prov.createAccount("hosted@example.com", "secret", new HashMap<String, Object>());
    }

    private Account fixture() throws Exception {
        return prov.get(AccountBy.name, "hosted@example.com");
    }

    @Test
    public void getHandlerHostedIsRegisteredHostedAuthInstance() {
        // Act
        ZimbraCustomAuth handler = ZimbraCustomAuth.getHandler("hosted");

        // Assert
        assertNotNull("the 'hosted' handler is registered at class init", handler);
        assertSame("registered handler is a HostedAuth", HostedAuth.class, handler.getClass());
    }

    @Test
    public void headerConstantsHaveExpectedProtocolValues() {
        // Assert: load-bearing header names used in the remote auth protocol.
        assertEquals("Auth-User", HostedAuth.HEADER_AUTH_USER);
        assertEquals("Auth-Pass", HostedAuth.HEADER_AUTH_PASSWORD);
        assertEquals("Auth-Protocol", HostedAuth.HEADER_AUTH_PROTOCOL);
        assertEquals("Auth-Status", HostedAuth.HEADER_AUTH_STATUS);
        assertEquals("OK", HostedAuth.AUTH_STATUS_OK);
    }

    @Test
    public void authenticateGetMethodUnreachableServerThrowsAuthFailed() throws Exception {
        // Arrange
        HostedAuth auth = new HostedAuth();
        Account acct = fixture();
        Map<String, Object> context = new HashMap<String, Object>();
        List<String> args = new ArrayList<String>();
        args.add(UNREACHABLE_URL);
        args.add("GET");

        // Act / Assert
        try {
            auth.authenticate(acct, "secret", context, args);
            fail("expected AuthFailedServiceException when remote server is unreachable");
        } catch (AuthFailedServiceException e) {
            assertNotNull("exception carries a message", e.getMessage());
        }
    }

    @Test
    public void authenticatePostMethodUnreachableServerThrowsAuthFailed() throws Exception {
        // Arrange
        HostedAuth auth = new HostedAuth();
        Account acct = fixture();
        Map<String, Object> context = new HashMap<String, Object>();
        List<String> args = new ArrayList<String>();
        args.add(UNREACHABLE_URL);
        args.add("POST");

        // Act / Assert
        try {
            auth.authenticate(acct, "secret", context, args);
            fail("expected AuthFailedServiceException for POST to unreachable server");
        } catch (AuthFailedServiceException e) {
            assertNotNull("exception carries a message", e.getMessage());
        }
    }

    @Test
    public void authenticateDefaultMethodWhenNoVerbThrowsAuthFailed() throws Exception {
        // Arrange: only the URL arg -> defaults to GET, still fails on unreachable host.
        HostedAuth auth = new HostedAuth();
        Account acct = fixture();
        Map<String, Object> context = new HashMap<String, Object>();
        List<String> args = new ArrayList<String>();
        args.add(UNREACHABLE_URL);

        // Act / Assert
        try {
            auth.authenticate(acct, "secret", context, args);
            fail("expected AuthFailedServiceException with default GET method");
        } catch (AuthFailedServiceException e) {
            assertNotNull("exception carries a message", e.getMessage());
        }
    }

    @Test
    public void authenticateWithClientIpRemoteIpProtocolAndUserAgentStillFailsCleanly() throws Exception {
        // Arrange: populate every optional context header branch before the HTTP call.
        HostedAuth auth = new HostedAuth();
        Account acct = fixture();
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(AuthContext.AC_ORIGINATING_CLIENT_IP, "10.1.2.3");
        context.put(AuthContext.AC_REMOTE_IP, "10.9.8.7");
        context.put(AuthContext.AC_PROTOCOL, AuthContext.Protocol.imap);
        context.put(AuthContext.AC_USER_AGENT, "JUnit-Agent/1.0");
        List<String> args = new ArrayList<String>();
        args.add(UNREACHABLE_URL);
        args.add("GET");

        // Act / Assert: all header branches execute, then the unreachable host fails it.
        try {
            auth.authenticate(acct, "secret", context, args);
            fail("expected AuthFailedServiceException after assembling all optional headers");
        } catch (AuthFailedServiceException e) {
            assertNotNull("exception carries a message", e.getMessage());
        }
    }

    // ---- Local recording HTTP server: lets us observe the exact request HostedAuth sends ----

    /**
     * Minimal in-process HTTP server that records the method + headers of the single request it
     * receives, and replies with a configurable status code and optional {@code Auth-Status}
     * header. Reachable, so authenticate() runs to completion instead of failing on connect.
     */
    private static final class RecordingServer implements HttpHandler {
        private final HttpServer server;

        private volatile String method;

        private final Map<String, String> headers = new HashMap<String, String>();

        private final int replyStatus;

        private final boolean sendAuthStatusHeader;

        private final String authStatusValue;

        RecordingServer(int replyStatus, boolean sendAuthStatusHeader, String authStatusValue)
                throws IOException {
            this.replyStatus = replyStatus;
            this.sendAuthStatusHeader = sendAuthStatusHeader;
            this.authStatusValue = authStatusValue;
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            this.server.createContext("/auth", this);
            this.server.start();
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/auth";
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            method = ex.getRequestMethod();
            // capture the first value of every request header (lower-cased keys per the JDK API)
            for (Map.Entry<String, List<String>> e : ex.getRequestHeaders().entrySet()) {
                if (e.getValue() != null && !e.getValue().isEmpty()) {
                    headers.put(e.getKey().toLowerCase(), e.getValue().get(0));
                }
            }
            if (sendAuthStatusHeader) {
                ex.getResponseHeaders().add(HostedAuth.HEADER_AUTH_STATUS, authStatusValue);
            }
            byte[] body = new byte[0];
            ex.sendResponseHeaders(replyStatus, body.length);
            OutputStream os = ex.getResponseBody();
            os.write(body);
            os.close();
        }

        String header(String name) {
            return headers.get(name.toLowerCase());
        }

        void stop() {
            server.stop(0);
        }
    }

    /**
     * Happy path against a real reachable server replying 200 + {@code Auth-Status: OK}.
     * authenticate() must RETURN NORMALLY, and the server must have received a GET carrying every
     * header the production code assembles.
     *
     * <p>Kills the header VoidMethodCall mutations: L87 Auth-User, L88 Auth-Pass, L82 Client-IP,
     * L85 X-ZIMBRA-REMOTE-ADDR, L93 Auth-Protocol, L96 Auth-User-Agent. Also kills L74
     * (GET/POST branch: "GET" must produce a GET request) and the OK/return path.
     */
    @Test
    public void authenticateGetWithAllHeadersOkStatusSucceedsAndSendsHeaders() throws Exception {
        RecordingServer srv = new RecordingServer(200, true, "OK");
        try {
            HostedAuth auth = new HostedAuth();
            Account acct = fixture();
            Map<String, Object> context = new HashMap<String, Object>();
            context.put(AuthContext.AC_ORIGINATING_CLIENT_IP, "10.1.2.3");
            context.put(AuthContext.AC_REMOTE_IP, "10.9.8.7");
            context.put(AuthContext.AC_PROTOCOL, AuthContext.Protocol.imap);
            context.put(AuthContext.AC_USER_AGENT, "JUnit-Agent/1.0");
            List<String> args = new ArrayList<String>();
            args.add(srv.url());
            args.add("GET");

            // Act - must not throw on a 200/OK response
            auth.authenticate(acct, "secret", context, args);

            // Assert - GET method selected (L74 branch)
            assertEquals("explicit GET arg must produce an HTTP GET", "GET", srv.method);
            // Assert - each header the code adds was actually transmitted (one per VoidMethodCall)
            assertEquals("Auth-User header (L87)", acct.getName(),
                    srv.header(HostedAuth.HEADER_AUTH_USER));
            assertEquals("Auth-Pass header (L88)", "secret",
                    srv.header(HostedAuth.HEADER_AUTH_PASSWORD));
            assertEquals("Client-IP header (L82)", "10.1.2.3",
                    srv.header(HostedAuth.HEADER_CLIENT_IP));
            assertEquals("X-ZIMBRA-REMOTE-ADDR header (L85)", "10.9.8.7",
                    srv.header(HostedAuth.HEADER_X_ZIMBRA_REMOTE_ADDR));
            assertEquals("Auth-Protocol header (L93)", AuthContext.Protocol.imap.toString(),
                    srv.header(HostedAuth.HEADER_AUTH_PROTOCOL));
            assertEquals("Auth-User-Agent header (L96)", "JUnit-Agent/1.0",
                    srv.header(HostedAuth.HEADER_AUTH_USER_AGENT));
        } finally {
            srv.stop();
        }
    }

    /**
     * Optional headers absent: when the context carries no client IP / remote IP / protocol /
     * user-agent, those headers must NOT be added (the L82/L85/L93/L96 guards skip the add). The
     * mandatory Auth-User / Auth-Pass headers are still present. This pins the conditional guards
     * around each optional addHeader so removing the guarded calls is observable both ways.
     */
    @Test
    public void authenticateNoOptionalContextOmitsOptionalHeaders() throws Exception {
        RecordingServer srv = new RecordingServer(200, true, "OK");
        try {
            HostedAuth auth = new HostedAuth();
            Account acct = fixture();
            Map<String, Object> context = new HashMap<String, Object>();
            List<String> args = new ArrayList<String>();
            args.add(srv.url());
            args.add("GET");

            auth.authenticate(acct, "secret", context, args);

            // mandatory headers present
            assertEquals(acct.getName(), srv.header(HostedAuth.HEADER_AUTH_USER));
            assertEquals("secret", srv.header(HostedAuth.HEADER_AUTH_PASSWORD));
            // optional headers omitted
            assertNull("no client IP in context => Client-IP header omitted",
                    srv.header(HostedAuth.HEADER_CLIENT_IP));
            assertNull("no remote IP in context => X-ZIMBRA-REMOTE-ADDR header omitted",
                    srv.header(HostedAuth.HEADER_X_ZIMBRA_REMOTE_ADDR));
            assertNull("no protocol in context => Auth-Protocol header omitted",
                    srv.header(HostedAuth.HEADER_AUTH_PROTOCOL));
            assertNull("no user-agent in context => Auth-User-Agent header omitted",
                    srv.header(HostedAuth.HEADER_AUTH_USER_AGENT));
        } finally {
            srv.stop();
        }
    }

    /**
     * POST verb must produce an HTTP POST (the else side of the L74 GET/POST conditional).
     * Pairs with the GET test so both sides of L74 are pinned to distinct observable methods.
     */
    @Test
    public void authenticatePostVerbSendsPostRequest() throws Exception {
        RecordingServer srv = new RecordingServer(200, true, "OK");
        try {
            HostedAuth auth = new HostedAuth();
            Account acct = fixture();
            Map<String, Object> context = new HashMap<String, Object>();
            List<String> args = new ArrayList<String>();
            args.add(srv.url());
            args.add("POST");

            auth.authenticate(acct, "secret", context, args);

            assertEquals("explicit POST arg must produce an HTTP POST", "POST", srv.method);
        } finally {
            srv.stop();
        }
    }

    /**
     * Server replies 200 but the {@code Auth-Status} header value is NOT "OK": authenticate() must
     * throw AuthFailedServiceException whose message carries the server's status string. This pins
     * the L121 {@code equalsIgnoreCase(AUTH_STATUS_OK)} branch on its non-OK side and proves the
     * header value (not the HTTP code) drives the decision.
     */
    @Test
    public void authenticateOkHttpButNonOkAuthStatusThrowsWithServerMessage() throws Exception {
        RecordingServer srv = new RecordingServer(200, true, "DENIED");
        try {
            HostedAuth auth = new HostedAuth();
            Account acct = fixture();
            Map<String, Object> context = new HashMap<String, Object>();
            List<String> args = new ArrayList<String>();
            args.add(srv.url());
            args.add("GET");

            try {
                auth.authenticate(acct, "secret", context, args);
                fail("non-OK Auth-Status must fail authentication");
            } catch (AuthFailedServiceException e) {
                assertTrue("failure message must echo the server's Auth-Status value",
                        e.getMessage().contains("DENIED"));
            }
        } finally {
            srv.stop();
        }
    }

    /**
     * Server replies a non-200 HTTP status: authenticate() must throw and the message must include
     * the remote response code. Pins the L111 {@code status != SC_OK} branch.
     */
    @Test
    public void authenticateNon200HttpStatusThrowsWithResponseCode() throws Exception {
        RecordingServer srv = new RecordingServer(503, false, null);
        try {
            HostedAuth auth = new HostedAuth();
            Account acct = fixture();
            Map<String, Object> context = new HashMap<String, Object>();
            List<String> args = new ArrayList<String>();
            args.add(srv.url());
            args.add("GET");

            try {
                auth.authenticate(acct, "secret", context, args);
                fail("non-200 HTTP status must fail authentication");
            } catch (AuthFailedServiceException e) {
                assertTrue("failure message must include the remote response code 503",
                        e.getMessage().contains("503"));
            }
        } finally {
            srv.stop();
        }
    }

    /**
     * Server replies 200 but with NO {@code Auth-Status} header at all: authenticate() must throw
     * an "Empty response" failure (the L116 missing-header branch), distinct from the non-OK value
     * case above.
     */
    @Test
    public void authenticateOkHttpButMissingAuthStatusHeaderThrowsEmptyResponse() throws Exception {
        RecordingServer srv = new RecordingServer(200, false, null);
        try {
            HostedAuth auth = new HostedAuth();
            Account acct = fixture();
            Map<String, Object> context = new HashMap<String, Object>();
            List<String> args = new ArrayList<String>();
            args.add(srv.url());
            args.add("GET");

            try {
                auth.authenticate(acct, "secret", context, args);
                fail("missing Auth-Status header must fail authentication");
            } catch (AuthFailedServiceException e) {
                assertTrue("failure message must indicate an empty/absent response",
                        e.getMessage().toLowerCase().contains("empty"));
            }
        } finally {
            srv.stop();
        }
    }

    /**
     * Case-insensitive OK: an {@code Auth-Status: ok} (lower case) must still authenticate
     * successfully, pinning the {@code equalsIgnoreCase} (not {@code equals}) comparison on L121
     * and confirming the success/return path with a real reachable server.
     */
    @Test
    public void authenticateLowercaseOkAuthStatusSucceeds() throws Exception {
        RecordingServer srv = new RecordingServer(200, true, "ok");
        try {
            HostedAuth auth = new HostedAuth();
            Account acct = fixture();
            Map<String, Object> context = new HashMap<String, Object>();
            List<String> args = new ArrayList<String>();
            args.add(srv.url());
            args.add("GET");

            // Act / Assert - must return normally (no exception) for a case-insensitive OK
            boolean threw = false;
            try {
                auth.authenticate(acct, "secret", context, args);
            } catch (Exception e) {
                threw = true;
            }
            assertFalse("lower-case 'ok' Auth-Status must authenticate successfully", threw);
        } finally {
            srv.stop();
        }
    }

    @Test
    public void authenticateEmptyArgsThrowsIndexOutOfBounds() throws Exception {
        // Arrange: no URL provided -> args.get(0) fails before any HTTP work.
        HostedAuth auth = new HostedAuth();
        Account acct = fixture();
        Map<String, Object> context = new HashMap<String, Object>();
        List<String> args = new ArrayList<String>();

        // Act / Assert
        try {
            auth.authenticate(acct, "secret", context, args);
            fail("expected IndexOutOfBoundsException for missing URL argument");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull("missing target URL surfaces as an index error", e);
        }
    }
}
