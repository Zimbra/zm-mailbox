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

package com.zimbra.cs.account.auth;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for the {@link PasswordUtil} hash families: SSHA, SSHA512, SHA1/{SHA}, MD5.
 * Each test exercises the generate -> verify round trip (the real-world password workflow) plus
 * the prefix-detection and mismatch (failure) paths. No harness needed: these are pure crypto.
 */
public class PasswordUtilTest {

    // ---------- SSHA ----------

    @Test
    public void generateAndVerifySSHACorrectPasswordRoundTripsSuccessfully() {
        // Arrange
        String plain = "secretPass1";

        // Act
        String encoded = PasswordUtil.SSHA.generateSSHA(plain, null);

        // Assert
        assertTrue("must carry the {SSHA} prefix", encoded.startsWith("{SSHA}"));
        assertTrue("isSSHA must recognize its own output", PasswordUtil.SSHA.isSSHA(encoded));
        assertTrue("the original password must verify", PasswordUtil.SSHA.verifySSHA(encoded, plain));
    }

    @Test
    public void verifySSHAWrongPasswordReturnsFalse() {
        // Arrange
        String encoded = PasswordUtil.SSHA.generateSSHA("rightPass", null);

        // Act
        boolean ok = PasswordUtil.SSHA.verifySSHA(encoded, "wrongPass");

        // Assert
        assertFalse("a different password must not verify", ok);
    }

    @Test
    public void verifySSHANotSSHAEncodedReturnsFalse() {
        // Act
        boolean ok = PasswordUtil.SSHA.verifySSHA("{MD5}abcdef", "anything");

        // Assert
        assertFalse("non-{SSHA} input must short-circuit to false", ok);
        assertFalse("isSSHA must reject a {MD5} string", PasswordUtil.SSHA.isSSHA("{MD5}abcdef"));
    }

    @Test
    public void generateSSHANullSaltRandomSaltDiffersAcrossCalls() {
        // Two null-salt generations must differ because the salt is filled by SecureRandom
        // (PasswordUtil.SSHA.generateSSHA L66-67). If the sr.nextBytes(salt) call is removed
        // (VoidMethodCallMutator, L67) the salt stays all-zero and both encodings become
        // identical, so this assertNotEquals fails. Both must still verify the same password.
        String a = PasswordUtil.SSHA.generateSSHA("randSaltPass", null);
        String b = PasswordUtil.SSHA.generateSSHA("randSaltPass", null);

        assertNotEquals("random salt must make two null-salt hashes differ", a, b);
        assertTrue("first random-salt hash must verify", PasswordUtil.SSHA.verifySSHA(a, "randSaltPass"));
        assertTrue("second random-salt hash must verify", PasswordUtil.SSHA.verifySSHA(b, "randSaltPass"));
    }

    @Test
    public void generateSSHAFixedSaltIsDeterministic() {
        // Arrange
        byte[] salt = new byte[] {1, 2, 3, 4};

        // Act
        String a = PasswordUtil.SSHA.generateSSHA("samePass", salt);
        String b = PasswordUtil.SSHA.generateSSHA("samePass", salt);

        // Assert
        assertEquals("same password + same salt must hash identically", a, b);
        assertTrue("encoded form must still verify", PasswordUtil.SSHA.verifySSHA(a, "samePass"));
    }

    // ---------- SSHA512 ----------

    @Test
    public void generateAndVerifySSHA512CorrectPasswordRoundTripsSuccessfully() {
        // Arrange
        String plain = "longerSecret512";

        // Act
        String encoded = PasswordUtil.SSHA512.generateSSHA512(plain, null);

        // Assert
        assertTrue("must carry the {SSHA512} prefix", encoded.startsWith("{SSHA512}"));
        assertTrue(PasswordUtil.SSHA512.isSSHA512(encoded));
        assertTrue(PasswordUtil.SSHA512.verifySSHA512(encoded, plain));
    }

    @Test
    public void verifySSHA512WrongPasswordReturnsFalse() {
        // Arrange
        String encoded = PasswordUtil.SSHA512.generateSSHA512("orig512", null);

        // Act / Assert
        assertFalse(PasswordUtil.SSHA512.verifySSHA512(encoded, "other512"));
    }

    @Test
    public void verifySSHA512NotSSHA512EncodedReturnsFalse() {
        // Act / Assert
        assertFalse("non-{SSHA512} input must short-circuit",
                PasswordUtil.SSHA512.verifySSHA512("{SSHA}abc", "x"));
        assertFalse(PasswordUtil.SSHA512.isSSHA512("{SSHA}abc"));
    }

    @Test
    public void generateSshaVsSsha512ProduceDistinctEncodings() {
        // Arrange
        String plain = "compareMe";
        byte[] salt = new byte[] {9, 9, 9, 9, 9, 9, 9, 9};

        // Act
        String ssha = PasswordUtil.SSHA.generateSSHA(plain, new byte[] {9, 9, 9, 9});
        String ssha512 = PasswordUtil.SSHA512.generateSSHA512(plain, salt);

        // Assert
        assertNotEquals("the two algorithms must not collide", ssha, ssha512);
    }

    // ---------- SHA1 / {SHA} ----------

    @Test
    public void generateAndVerifySHA1DefaultPrefixRoundTrips() {
        // Arrange
        String plain = "sha1pass";

        // Act
        String encoded = PasswordUtil.SHA1.generateSHA1(plain);

        // Assert
        assertTrue("default prefix is {SHA1}", encoded.startsWith("{SHA1}"));
        assertTrue(PasswordUtil.SHA1.isSHA1(encoded));
        assertTrue(PasswordUtil.SHA1.verifySHA1(encoded, plain));
    }

    @Test
    public void verifySHA1ShaPrefixVariantRoundTrips() {
        // Arrange
        String plain = "legacySha";

        // Act
        String encoded = PasswordUtil.SHA1.generateSHA1(plain, "{SHA}");

        // Assert
        assertTrue("the legacy {SHA} prefix must be recognized", encoded.startsWith("{SHA}"));
        assertTrue("isSHA1 must accept the {SHA} variant", PasswordUtil.SHA1.isSHA1(encoded));
        assertTrue(PasswordUtil.SHA1.verifySHA1(encoded, plain));
    }

    @Test
    public void verifySHA1WrongPasswordReturnsFalse() {
        // Arrange
        String encoded = PasswordUtil.SHA1.generateSHA1("correct");

        // Act / Assert
        assertFalse(PasswordUtil.SHA1.verifySHA1(encoded, "incorrect"));
    }

    @Test
    public void verifySHA1UnknownPrefixReturnsFalse() {
        // Act / Assert
        assertFalse("a non-SHA prefix must not be verified",
                PasswordUtil.SHA1.verifySHA1("{MD5}deadbeef", "x"));
        assertFalse(PasswordUtil.SHA1.isSHA1("{MD5}deadbeef"));
    }

    // ---------- MD5 ----------

    @Test
    public void generateAndVerifyMD5CorrectPasswordRoundTrips() {
        // Arrange
        String plain = "md5pass";

        // Act
        String encoded = PasswordUtil.MD5.generateMD5(plain);

        // Assert
        assertTrue(encoded.startsWith("{MD5}"));
        assertTrue(PasswordUtil.MD5.isMD5(encoded));
        assertTrue(PasswordUtil.MD5.verifyMD5(encoded, plain));
    }

    @Test
    public void verifyMD5WrongPasswordReturnsFalse() {
        // Arrange
        String encoded = PasswordUtil.MD5.generateMD5("a");

        // Act / Assert
        assertFalse(PasswordUtil.MD5.verifyMD5(encoded, "b"));
    }

    @Test
    public void verifyMD5NotMD5EncodedReturnsFalse() {
        // Act / Assert
        assertFalse("non-{MD5} input must short-circuit",
                PasswordUtil.MD5.verifyMD5("{SHA1}abc", "x"));
        assertFalse(PasswordUtil.MD5.isMD5("{SHA1}abc"));
    }

    @Test
    public void generateMD5SameInputIsDeterministic() {
        // MD5 here is unsalted, so it must be stable across calls.
        // Act
        String a = PasswordUtil.MD5.generateMD5("stable");
        String b = PasswordUtil.MD5.generateMD5("stable");

        // Assert
        assertEquals(a, b);
    }

    // ---------- short-buffer (<= SALT_LEN) failure branches ----------

    @Test
    public void verifySSHABufferShorterThanSaltReturnsFalse() {
        // Arrange — a valid {SSHA} prefix but a payload that decodes to <= SALT_LEN(4) bytes.
        // Base64 "AAA=" decodes to 2 bytes, which is shorter than the 4-byte salt.
        String tooShort = "{SSHA}AAA=";

        // Act
        boolean ok = PasswordUtil.SSHA.verifySSHA(tooShort, "anything");

        // Assert — the length guard must short-circuit to false
        assertFalse("decoded buffer shorter than salt must not verify", ok);
    }

    @Test
    public void verifySSHA512BufferShorterThanSaltReturnsFalse() {
        // Arrange — valid {SSHA512} prefix but a 2-byte decoded payload (<= SALT_LEN of 8)
        String tooShort = "{SSHA512}AAA=";

        // Act
        boolean ok = PasswordUtil.SSHA512.verifySSHA512(tooShort, "anything");

        // Assert
        assertFalse("decoded buffer shorter than salt must not verify", ok);
    }

    // ---------- the (buff.length == 28) salt-length branch ----------

    @Test
    public void verifySSHAEightByteSalt28ByteBufferRoundTrips() {
        // Arrange — SHA1 digest (20 bytes) + 8-byte salt = 28-byte buffer, which drives the
        // (buff.length == 28) -> slen = 8 branch inside verifySSHA.
        String plain = "eightByteSaltPass";
        byte[] eightByteSalt = new byte[] {10, 20, 30, 40, 50, 60, 70, 80};

        // Act
        String encoded = PasswordUtil.SSHA.generateSSHA(plain, eightByteSalt);
        boolean ok = PasswordUtil.SSHA.verifySSHA(encoded, plain);

        // Assert — the 28-byte path must still recover the salt and verify
        assertTrue("28-byte (8-salt) SSHA must verify via the ==28 branch", ok);
        assertFalse("a wrong password must still fail on the 28-byte path",
                PasswordUtil.SSHA.verifySSHA(encoded, "wrong"));
    }

    // ---------- main(): exercises every generate/verify path in one driver ----------

    @Test
    public void mainRunsAllAlgorithmsWithoutThrowing() throws Exception {
        // Act — main() prints every algorithm's encodings and a couple of SHA1 verifications.
        // It must complete without throwing for the default (no-LDAP) salt path.
        PasswordUtil.main(new String[0]);

        // Assert — the generate/verify primitives it drives remain self-consistent.
        String enc = PasswordUtil.SHA1.generateSHA1("testme", "{SHA}");
        assertTrue("main's {SHA} variant must round-trip", PasswordUtil.SHA1.verifySHA1(enc, "testme"));
    }

    /**
     * Captures main()'s stdout and asserts the exact content of every line it prints. This kills
     * the VoidMethodCallMutator survivors on the System.out.println calls (L236-L249, L256, L258):
     * removing any println drops a line the assertions below require. It also kills the
     * NegateConditionalsMutator on the two {@code result ? "good" : "bad"} ternaries (L256/L258):
     * both SHA1 verifications must succeed, so both must print "result is good" (never "bad").
     */
    @Test
    public void mainPrintsExpectedLinesForEveryAlgorithm() throws Exception {
        // Arrange — redirect stdout so we can inspect exactly what main() emits.
        java.io.PrintStream originalOut = System.out;
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        String printed;
        try {
            System.setOut(new java.io.PrintStream(captured, true, "UTF-8"));

            // Act
            PasswordUtil.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }
        printed = captured.toString("UTF-8");
        String[] lines = printed.split("\\r?\\n", -1);

        // Assert — the plain-text and prefix lines must all be present (each is one println).
        // The "plain:" line appears twice (test123 then helloWorld).
        int test123PlainCount = 0;
        int helloPlainCount = 0;
        int sshaPrefixCount = 0;
        int ssha512PrefixCount = 0;
        int sha1PrefixCount = 0;
        int md5PrefixCount = 0;
        int resultGoodCount = 0;
        int resultBadCount = 0;
        for (String line : lines) {
            if (line.equals("plain:        test123")) {
                test123PlainCount++;
            }
            if (line.equals("plain:        helloWorld")) {
                helloPlainCount++;
            }
            if (line.startsWith("encoded SSHA: {SSHA}")) {
                sshaPrefixCount++;
            }
            if (line.startsWith("encoded SSHA512: {SSHA512}")) {
                ssha512PrefixCount++;
            }
            if (line.startsWith("encoded SSH1: {SHA1}")) {
                sha1PrefixCount++;
            }
            if (line.startsWith("encoded MD5:  {MD5}")) {
                md5PrefixCount++;
            }
            if (line.equals("result is good")) {
                resultGoodCount++;
            }
            if (line.equals("result is bad")) {
                resultBadCount++;
            }
        }

        // Each "plain:" header is printed once per block (L236, L244).
        assertEquals("the test123 plain header must be printed exactly once", 1, test123PlainCount);
        assertEquals("the helloWorld plain header must be printed exactly once", 1, helloPlainCount);
        // Each encoding line is printed once per block, so twice total (L237-240, L245-248).
        assertEquals("SSHA must be printed for both blocks", 2, sshaPrefixCount);
        assertEquals("SSHA512 must be printed for both blocks", 2, ssha512PrefixCount);
        assertEquals("SHA1 must be printed for both blocks", 2, sha1PrefixCount);
        assertEquals("MD5 must be printed for both blocks", 2, md5PrefixCount);
        // Both verifications at the end (L255-258) succeed, so "good" twice and "bad" never.
        assertEquals("both SHA1 verifications must print good (kills the ternary negation)",
                2, resultGoodCount);
        assertEquals("no verification may print bad", 0, resultBadCount);

        // The exact MD5 encodings are deterministic (unsalted), so assert them verbatim. This
        // proves the MD5 println lines (L240, L248) carry the right value, not just any value.
        assertTrue("the {MD5} encoding of test123 must appear verbatim",
                printed.contains("encoded MD5:  " + PasswordUtil.MD5.generateMD5("test123")));
        assertTrue("the {MD5} encoding of helloWorld must appear verbatim",
                printed.contains("encoded MD5:  " + PasswordUtil.MD5.generateMD5("helloWorld")));
        // The deterministic unsalted SHA1 encodings must also appear verbatim (L239, L247).
        assertTrue("the {SHA1} encoding of test123 must appear verbatim",
                printed.contains("encoded SSH1: " + PasswordUtil.SHA1.generateSHA1("test123")));
        assertTrue("the {SHA1} encoding of helloWorld must appear verbatim",
                printed.contains("encoded SSH1: " + PasswordUtil.SHA1.generateSHA1("helloWorld")));
    }

    // ---------- explicit null/empty salt generate paths ----------

    @Test
    public void generateSSHA512NullSaltRandomSaltStillVerifies() {
        // Arrange / Act — null salt drives the random-salt generation branch
        String encoded = PasswordUtil.SSHA512.generateSSHA512("randSalt512", null);

        // Assert
        assertTrue(encoded.startsWith("{SSHA512}"));
        assertTrue("randomly salted SSHA512 must verify its own password",
                PasswordUtil.SSHA512.verifySSHA512(encoded, "randSalt512"));
        assertFalse(PasswordUtil.SSHA512.verifySSHA512(encoded, "different"));
    }
}
