package com.zimbra.cs.account.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link AuthContext} — constants and {@link AuthContext.Protocol} enum.
 * No dependencies, no mocks needed.
 */
public class AuthContextTest {

    // =========================================================================
    // String constant values
    // =========================================================================

    @Test
    public void constantValues_areAsDocumented() {
        assertEquals("ocip",     AuthContext.AC_ORIGINATING_CLIENT_IP);
        assertEquals("remoteip", AuthContext.AC_REMOTE_IP);
        assertEquals("soapport", AuthContext.AC_SOAP_PORT);
        assertEquals("anp",      AuthContext.AC_ACCOUNT_NAME_PASSEDIN);
        assertEquals("ua",       AuthContext.AC_USER_AGENT);
        assertEquals("asAdmin",  AuthContext.AC_AS_ADMIN);
        assertEquals("authedByMech", AuthContext.AC_AUTHED_BY_MECH);
        assertEquals("proto",    AuthContext.AC_PROTOCOL);
        assertEquals("did",      AuthContext.AC_DEVICE_ID);
    }

    // =========================================================================
    // Protocol enum
    // =========================================================================

    @Test
    public void protocol_allValuesPresent() {
        AuthContext.Protocol[] values = AuthContext.Protocol.values();
        assertEquals(10, values.length);
    }

    @Test
    public void protocol_fromStringRoundTrip() {
        for (AuthContext.Protocol p : AuthContext.Protocol.values()) {
            assertNotNull(p);
            assertEquals(p, AuthContext.Protocol.valueOf(p.name()));
        }
    }

    @Test
    public void protocol_knownNames() {
        assertEquals(AuthContext.Protocol.client_certificate, AuthContext.Protocol.valueOf("client_certificate"));
        assertEquals(AuthContext.Protocol.http_basic,         AuthContext.Protocol.valueOf("http_basic"));
        assertEquals(AuthContext.Protocol.http_dav,           AuthContext.Protocol.valueOf("http_dav"));
        assertEquals(AuthContext.Protocol.im,                 AuthContext.Protocol.valueOf("im"));
        assertEquals(AuthContext.Protocol.imap,               AuthContext.Protocol.valueOf("imap"));
        assertEquals(AuthContext.Protocol.pop3,               AuthContext.Protocol.valueOf("pop3"));
        assertEquals(AuthContext.Protocol.soap,               AuthContext.Protocol.valueOf("soap"));
        assertEquals(AuthContext.Protocol.spnego,             AuthContext.Protocol.valueOf("spnego"));
        assertEquals(AuthContext.Protocol.zsync,              AuthContext.Protocol.valueOf("zsync"));
        assertEquals(AuthContext.Protocol.test,               AuthContext.Protocol.valueOf("test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void protocol_unknownName_throwsIllegalArgumentException() {
        AuthContext.Protocol.valueOf("nonexistent_protocol");
    }
}
