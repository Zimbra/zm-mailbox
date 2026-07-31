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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.common.util.StringUtil;

/**
 * Shared logic for the SAML "test config" flow, used by both the {@code GenerateSamlTest} admin
 * SOAP handler (zm-mailbox) and the SAML consumer extension (zm-saml-consumer-store).
 *
 * <p>It owns three contracts that must stay in sync across those two jars:
 * <ul>
 *   <li>the nonce itself,</li>
 *   <li>the value stored in {@code zimbraSamlTestNonce}: {@code "{nonce} notAfter={epochMillis}"},</li>
 *   <li>the {@code RelayState} codec that carries the nonce (and optional extras) through the IdP.</li>
 * </ul>
 */
public final class SamlTestNonce {

    /** Validity window for a generated nonce (30 minutes). */
    public static final long NONCE_VALIDITY_MS = 30 * 60 * 1000L;

    private static final String NOT_AFTER_MARKER = " notAfter=";
    private static final String RELAY_KEY_NONCE = "nonce";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SamlTestNonce() {
    }

    /** Generate a new cryptographically-random, URL-safe 128-bit nonce. */
    public static String generate() {
        final byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Format the value persisted in {@code zimbraSamlTestNonce}. */
    public static String formatStoredValue(String nonce, long notAfterMillis) {
        return nonce + NOT_AFTER_MARKER + notAfterMillis;
    }

    /** Parsed representation of a stored {@code zimbraSamlTestNonce} value. */
    public static final class Stored {
        private final String nonce;
        private final long notAfterMillis;

        private Stored(String nonce, long notAfterMillis) {
            this.nonce = nonce;
            this.notAfterMillis = notAfterMillis;
        }

        public String getNonce() {
            return nonce;
        }

        public long getNotAfterMillis() {
            return notAfterMillis;
        }
    }

    /**
     * Parse {@code "{nonce} notAfter={epochMillis}"}.
     *
     * @return parsed value, or {@code null} if the input is empty or malformed
     */
    public static Stored parseStoredValue(String storedValue) {
        if (StringUtil.isNullOrEmpty(storedValue)) {
            return null;
        }
        final int idx = storedValue.indexOf(NOT_AFTER_MARKER);
        if (idx <= 0) {
            return null;
        }
        final String nonce = storedValue.substring(0, idx);
        final String tsPart = storedValue.substring(idx + NOT_AFTER_MARKER.length()).trim();
        if (StringUtil.isNullOrEmpty(nonce)) {
            return null;
        }
        try {
            return new Stored(nonce, Long.parseLong(tsPart));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Whether a stored value represents a test that is still in progress (present and not expired).
     * Used by the admin API to decide whether to prompt for a force-clear.
     */
    public static boolean hasActiveNonce(String storedValue, long nowMillis) {
        final Stored stored = parseStoredValue(storedValue);
        return stored != null && nowMillis < stored.getNotAfterMillis();
    }

    /**
     * Validate a candidate nonce (typically decoded from RelayState) against the stored value.
     * The comparison is constant-time and enforces the {@code notAfter} expiry.
     */
    public static boolean isValid(String candidateNonce, String storedValue, long nowMillis) {
        if (StringUtil.isNullOrEmpty(candidateNonce)) {
            return false;
        }
        final Stored stored = parseStoredValue(storedValue);
        if (stored == null || nowMillis >= stored.getNotAfterMillis()) {
            return false;
        }
        return MessageDigest.isEqual(candidateNonce.getBytes(StandardCharsets.UTF_8),
                stored.getNonce().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build the base64 RelayState payload carrying the nonce and any optional extra properties.
     * The result is safe to place directly in a {@code RelayState} query parameter.
     */
    public static String encodeRelayState(String nonce, Map<String, String> extras) {
        final StringBuilder payload = new StringBuilder();
        payload.append(RELAY_KEY_NONCE).append('=').append(urlEncode(nonce));
        if (extras != null) {
            for (final Map.Entry<String, String> e : extras.entrySet()) {
                if (RELAY_KEY_NONCE.equals(e.getKey()) || e.getValue() == null) {
                    continue;
                }
                payload.append('&').append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
            }
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode a RelayState payload produced by {@link #encodeRelayState}.
     *
     * @return map of properties (never {@code null}); empty if the input is empty or undecodable
     */
    public static Map<String, String> decodeRelayState(String relayState) {
        final Map<String, String> result = new LinkedHashMap<>();
        if (StringUtil.isNullOrEmpty(relayState)) {
            return result;
        }
        final String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(relayState), StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            return result;
        }
        for (final String pair : payload.split("&")) {
            final int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            result.put(urlDecode(pair.substring(0, eq)), urlDecode(pair.substring(eq + 1)));
        }
        return result;
    }

    /** Convenience: extract just the nonce from a RelayState payload ({@code null} if absent). */
    public static String getNonceFromRelayState(String relayState) {
        return decodeRelayState(relayState).get(RELAY_KEY_NONCE);
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            return value;
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException | IllegalArgumentException e) {
            return value;
        }
    }
}
