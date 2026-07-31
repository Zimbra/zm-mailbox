/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
package com.zimbra.common.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SamlTestNonceTest {

    private static final long NOW = 1_700_000_000_000L;

    @Test
    public void generateProducesUniqueUrlSafeValues() {
        final String a = SamlTestNonce.generate();
        final String b = SamlTestNonce.generate();
        assertNotEquals(a, b);
        assertFalse(a.isEmpty());
        // URL-safe base64 without padding: no '+', '/', or '='
        assertFalse(a.contains("+"));
        assertFalse(a.contains("/"));
        assertFalse(a.contains("="));
    }

    @Test
    public void storedValueRoundTrips() {
        final String value = SamlTestNonce.formatStoredValue("abc123", NOW);
        assertEquals("abc123 notAfter=" + NOW, value);
        final SamlTestNonce.Stored parsed = SamlTestNonce.parseStoredValue(value);
        assertEquals("abc123", parsed.getNonce());
        assertEquals(NOW, parsed.getNotAfterMillis());
    }

    @Test
    public void parseMalformedReturnsNull() {
        assertNull(SamlTestNonce.parseStoredValue(null));
        assertNull(SamlTestNonce.parseStoredValue(""));
        assertNull(SamlTestNonce.parseStoredValue("nonceWithoutMarker"));
        assertNull(SamlTestNonce.parseStoredValue("abc notAfter=notANumber"));
        assertNull(SamlTestNonce.parseStoredValue(" notAfter=123")); // empty nonce
    }

    @Test
    public void hasActiveNonceReflectsExpiry() {
        assertFalse(SamlTestNonce.hasActiveNonce(null, NOW));
        assertFalse(SamlTestNonce.hasActiveNonce("", NOW));
        assertTrue(SamlTestNonce.hasActiveNonce(SamlTestNonce.formatStoredValue("n", NOW + 1000), NOW));
        assertFalse(SamlTestNonce.hasActiveNonce(SamlTestNonce.formatStoredValue("n", NOW - 1000), NOW));
    }

    @Test
    public void isValidChecksMatchAndExpiry() {
        final String stored = SamlTestNonce.formatStoredValue("secret", NOW + 1000);
        assertTrue(SamlTestNonce.isValid("secret", stored, NOW));
        // wrong nonce
        assertFalse(SamlTestNonce.isValid("other", stored, NOW));
        // empty candidate
        assertFalse(SamlTestNonce.isValid("", stored, NOW));
        assertFalse(SamlTestNonce.isValid(null, stored, NOW));
        // expired
        assertFalse(SamlTestNonce.isValid("secret", stored, NOW + 5000));
        // no stored value
        assertFalse(SamlTestNonce.isValid("secret", null, NOW));
    }

    @Test
    public void relayStateRoundTripsNonceAndExtras() {
        final Map<String, String> extras = new HashMap<>();
        extras.put("domain", "example.com");
        final String relayState = SamlTestNonce.encodeRelayState("my-nonce", extras);
        // transport-safe: base64url only
        assertFalse(relayState.contains("="));
        assertFalse(relayState.contains("&"));

        final Map<String, String> decoded = SamlTestNonce.decodeRelayState(relayState);
        assertEquals("my-nonce", decoded.get("nonce"));
        assertEquals("example.com", decoded.get("domain"));
        assertEquals("my-nonce", SamlTestNonce.getNonceFromRelayState(relayState));
    }

    @Test
    public void relayStatePreservesSpecialCharactersInExtras() {
        final Map<String, String> extras = new HashMap<>();
        extras.put("redirect", "https://host/path?a=b&c=d");
        final String relayState = SamlTestNonce.encodeRelayState("n", extras);
        final Map<String, String> decoded = SamlTestNonce.decodeRelayState(relayState);
        assertEquals("n", decoded.get("nonce"));
        assertEquals("https://host/path?a=b&c=d", decoded.get("redirect"));
    }

    @Test
    public void encodeRelayStateWithNullExtrasCarriesOnlyNonce() {
        final String relayState = SamlTestNonce.encodeRelayState("only-nonce", null);
        final Map<String, String> decoded = SamlTestNonce.decodeRelayState(relayState);
        assertEquals(1, decoded.size());
        assertEquals("only-nonce", decoded.get("nonce"));
    }

    @Test
    public void encodeRelayStateIgnoresNonceOverrideAndNullValues() {
        final Map<String, String> extras = new HashMap<>();
        extras.put("nonce", "attacker-supplied"); // must not override the real nonce
        extras.put("empty", null);                // null values are skipped
        final Map<String, String> decoded =
                SamlTestNonce.decodeRelayState(SamlTestNonce.encodeRelayState("real", extras));
        assertEquals("real", decoded.get("nonce"));
        assertFalse(decoded.containsKey("empty"));
    }

    @Test
    public void decodeGarbageReturnsEmptyMap() {
        assertTrue(SamlTestNonce.decodeRelayState(null).isEmpty());
        assertTrue(SamlTestNonce.decodeRelayState("").isEmpty());
        // not valid base64url
        assertNull(SamlTestNonce.getNonceFromRelayState("!!!not base64!!!"));
    }
}
