package com.zimbra.cs.account.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link PasswordUtil} — all four inner classes.
 *
 * No mocks required: all methods are pure JDK crypto (MessageDigest + Base64).
 * The only external dependency is {@code InMemoryLdapServer.isOn()}, which is
 * bypassed by passing an explicit non-null {@code salt} byte[] to the generate
 * methods, so we never trigger the random-salt path.
 */
public class PasswordUtilTest {

    private static final String PASSWORD = "test123";
    private static final String WRONG_PASSWORD = "wrong";
    private static final byte[] FIXED_SALT = new byte[]{1, 2, 3, 4};
    private static final byte[] FIXED_SALT_8 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

    // =========================================================================
    // SSHA
    // =========================================================================

    @Test
    public void ssha_isSSHA_trueForSshaPrefix() {
        assertTrue(PasswordUtil.SSHA.isSSHA("{SSHA}somehash"));
    }

    @Test
    public void ssha_isSSHA_falseForOtherPrefix() {
        assertFalse(PasswordUtil.SSHA.isSSHA("{SSHA512}somehash"));
        assertFalse(PasswordUtil.SSHA.isSSHA("{MD5}somehash"));
        assertFalse(PasswordUtil.SSHA.isSSHA("plaintext"));
    }

    @Test
    public void ssha_generateSSHA_returnsSshaPrefix() {
        String encoded = PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT);
        assertTrue("expected {SSHA} prefix", encoded.startsWith("{SSHA}"));
    }

    @Test
    public void ssha_generateSSHA_isDeterministicWithFixedSalt() {
        String first  = PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT);
        String second = PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT);
        assertEquals(first, second);
    }

    @Test
    public void ssha_generateSSHA_differentPasswordsProduceDifferentHashes() {
        String h1 = PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT);
        String h2 = PasswordUtil.SSHA.generateSSHA(WRONG_PASSWORD, FIXED_SALT);
        assertFalse("different passwords must not produce the same hash", h1.equals(h2));
    }

    @Test
    public void ssha_generateSSHA_differentSaltsProduceDifferentHashes() {
        String h1 = PasswordUtil.SSHA.generateSSHA(PASSWORD, new byte[]{1, 1, 1, 1});
        String h2 = PasswordUtil.SSHA.generateSSHA(PASSWORD, new byte[]{2, 2, 2, 2});
        assertFalse("different salts must produce different hashes", h1.equals(h2));
    }

    @Test
    public void ssha_verifySSHA_correctPasswordReturnsTrue() {
        String encoded = PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT);
        assertTrue(PasswordUtil.SSHA.verifySSHA(encoded, PASSWORD));
    }

    @Test
    public void ssha_verifySSHA_wrongPasswordReturnsFalse() {
        String encoded = PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT);
        assertFalse(PasswordUtil.SSHA.verifySSHA(encoded, WRONG_PASSWORD));
    }

    @Test
    public void ssha_verifySSHA_wrongPrefixReturnsFalse() {
        assertFalse(PasswordUtil.SSHA.verifySSHA("{MD5}AAAA", PASSWORD));
    }

    @Test
    public void ssha_verifySSHA_emptyHashAfterPrefixReturnsFalse() {
        // After stripping "{SSHA}" there are 0 bytes — buff.length <= SALT_LEN
        assertFalse(PasswordUtil.SSHA.verifySSHA("{SSHA}", PASSWORD));
    }

    // =========================================================================
    // SSHA512
    // =========================================================================

    @Test
    public void ssha512_isSSHA512_trueForSsha512Prefix() {
        assertTrue(PasswordUtil.SSHA512.isSSHA512("{SSHA512}somehash"));
    }

    @Test
    public void ssha512_isSSHA512_falseForOtherPrefix() {
        assertFalse(PasswordUtil.SSHA512.isSSHA512("{SSHA}somehash"));
        assertFalse(PasswordUtil.SSHA512.isSSHA512("plaintext"));
    }

    @Test
    public void ssha512_generateSSHA512_returnsSsha512Prefix() {
        String encoded = PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8);
        assertTrue("expected {SSHA512} prefix", encoded.startsWith("{SSHA512}"));
    }

    @Test
    public void ssha512_generateSSHA512_isDeterministicWithFixedSalt() {
        String first  = PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8);
        String second = PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8);
        assertEquals(first, second);
    }

    @Test
    public void ssha512_generateSSHA512_differentPasswordsProduceDifferentHashes() {
        String h1 = PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8);
        String h2 = PasswordUtil.SSHA512.generateSSHA512(WRONG_PASSWORD, FIXED_SALT_8);
        assertFalse(h1.equals(h2));
    }

    @Test
    public void ssha512_verifySSHA512_correctPasswordReturnsTrue() {
        String encoded = PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8);
        assertTrue(PasswordUtil.SSHA512.verifySSHA512(encoded, PASSWORD));
    }

    @Test
    public void ssha512_verifySSHA512_wrongPasswordReturnsFalse() {
        String encoded = PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8);
        assertFalse(PasswordUtil.SSHA512.verifySSHA512(encoded, WRONG_PASSWORD));
    }

    @Test
    public void ssha512_verifySSHA512_wrongPrefixReturnsFalse() {
        assertFalse(PasswordUtil.SSHA512.verifySSHA512("{SSHA}AAAA", PASSWORD));
    }

    @Test
    public void ssha512_verifySSHA512_emptyHashAfterPrefixReturnsFalse() {
        assertFalse(PasswordUtil.SSHA512.verifySSHA512("{SSHA512}", PASSWORD));
    }

    // =========================================================================
    // SHA1
    // =========================================================================

    @Test
    public void sha1_isSHA1_trueForSha1Prefix() {
        assertTrue(PasswordUtil.SHA1.isSHA1("{SHA1}somehash"));
    }

    @Test
    public void sha1_isSHA1_trueForShortShaPrefix() {
        assertTrue(PasswordUtil.SHA1.isSHA1("{SHA}somehash"));
    }

    @Test
    public void sha1_isSHA1_falseForOtherPrefix() {
        assertFalse(PasswordUtil.SHA1.isSHA1("{SSHA}somehash"));
        assertFalse(PasswordUtil.SHA1.isSHA1("plaintext"));
    }

    @Test
    public void sha1_generateSHA1_returnsSha1Prefix() {
        String encoded = PasswordUtil.SHA1.generateSHA1(PASSWORD);
        assertTrue("expected {SHA1} prefix", encoded.startsWith("{SHA1}"));
    }

    @Test
    public void sha1_generateSHA1_isDeterministic() {
        assertEquals(
            PasswordUtil.SHA1.generateSHA1(PASSWORD),
            PasswordUtil.SHA1.generateSHA1(PASSWORD)
        );
    }

    @Test
    public void sha1_generateSHA1_differentPasswordsProduceDifferentHashes() {
        assertFalse(
            PasswordUtil.SHA1.generateSHA1(PASSWORD).equals(
            PasswordUtil.SHA1.generateSHA1(WRONG_PASSWORD))
        );
    }

    @Test
    public void sha1_generateSHA1WithPrefix_returnsSuppliedPrefix() {
        String encoded = PasswordUtil.SHA1.generateSHA1(PASSWORD, "{SHA}");
        assertTrue(encoded.startsWith("{SHA}"));
    }

    @Test
    public void sha1_generateSHA1WithNullPrefix_defaultsToSha1Prefix() {
        String encoded = PasswordUtil.SHA1.generateSHA1(PASSWORD, null);
        assertTrue(encoded.startsWith("{SHA1}"));
    }

    @Test
    public void sha1_verifySHA1_sha1PrefixCorrectPassword() {
        String encoded = PasswordUtil.SHA1.generateSHA1(PASSWORD);
        assertTrue(PasswordUtil.SHA1.verifySHA1(encoded, PASSWORD));
    }

    @Test
    public void sha1_verifySHA1_shortShaPrefixCorrectPassword() {
        String encoded = PasswordUtil.SHA1.generateSHA1(PASSWORD, "{SHA}");
        assertTrue(PasswordUtil.SHA1.verifySHA1(encoded, PASSWORD));
    }

    @Test
    public void sha1_verifySHA1_wrongPasswordReturnsFalse() {
        String encoded = PasswordUtil.SHA1.generateSHA1(PASSWORD);
        assertFalse(PasswordUtil.SHA1.verifySHA1(encoded, WRONG_PASSWORD));
    }

    @Test
    public void sha1_verifySHA1_wrongPrefixReturnsFalse() {
        assertFalse(PasswordUtil.SHA1.verifySHA1("{MD5}AAAA", PASSWORD));
    }

    // =========================================================================
    // MD5
    // =========================================================================

    @Test
    public void md5_isMD5_trueForMd5Prefix() {
        assertTrue(PasswordUtil.MD5.isMD5("{MD5}somehash"));
    }

    @Test
    public void md5_isMD5_falseForOtherPrefix() {
        assertFalse(PasswordUtil.MD5.isMD5("{SHA1}somehash"));
        assertFalse(PasswordUtil.MD5.isMD5("plaintext"));
    }

    @Test
    public void md5_generateMD5_returnsMd5Prefix() {
        String encoded = PasswordUtil.MD5.generateMD5(PASSWORD);
        assertTrue("expected {MD5} prefix", encoded.startsWith("{MD5}"));
    }

    @Test
    public void md5_generateMD5_isDeterministic() {
        assertEquals(
            PasswordUtil.MD5.generateMD5(PASSWORD),
            PasswordUtil.MD5.generateMD5(PASSWORD)
        );
    }

    @Test
    public void md5_generateMD5_differentPasswordsProduceDifferentHashes() {
        assertFalse(
            PasswordUtil.MD5.generateMD5(PASSWORD).equals(
            PasswordUtil.MD5.generateMD5(WRONG_PASSWORD))
        );
    }

    @Test
    public void md5_verifyMD5_correctPasswordReturnsTrue() {
        String encoded = PasswordUtil.MD5.generateMD5(PASSWORD);
        assertTrue(PasswordUtil.MD5.verifyMD5(encoded, PASSWORD));
    }

    @Test
    public void md5_verifyMD5_wrongPasswordReturnsFalse() {
        String encoded = PasswordUtil.MD5.generateMD5(PASSWORD);
        assertFalse(PasswordUtil.MD5.verifyMD5(encoded, WRONG_PASSWORD));
    }

    @Test
    public void md5_verifyMD5_wrongPrefixReturnsFalse() {
        assertFalse(PasswordUtil.MD5.verifyMD5("{SHA1}AAAA", PASSWORD));
    }

    // =========================================================================
    // Cross-algorithm: encodings must not be confused with each other
    // =========================================================================

    @Test
    public void crossAlgorithm_sshaEncodingNotAcceptedBySsha512() {
        String ssha = PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT);
        assertFalse(PasswordUtil.SSHA512.isSSHA512(ssha));
        assertFalse(PasswordUtil.SSHA512.verifySSHA512(ssha, PASSWORD));
    }

    @Test
    public void crossAlgorithm_ssha512EncodingNotAcceptedBySsha() {
        String ssha512 = PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8);
        assertFalse(PasswordUtil.SSHA.isSSHA(ssha512));
        assertFalse(PasswordUtil.SSHA.verifySSHA(ssha512, PASSWORD));
    }

    @Test
    public void crossAlgorithm_md5EncodingNotAcceptedBySha1() {
        String md5 = PasswordUtil.MD5.generateMD5(PASSWORD);
        assertFalse(PasswordUtil.SHA1.isSHA1(md5));
        assertFalse(PasswordUtil.SHA1.verifySHA1(md5, PASSWORD));
    }

    @Test
    public void crossAlgorithm_sha1EncodingNotAcceptedByMd5() {
        String sha1 = PasswordUtil.SHA1.generateSHA1(PASSWORD);
        assertFalse(PasswordUtil.MD5.isMD5(sha1));
        assertFalse(PasswordUtil.MD5.verifyMD5(sha1, PASSWORD));
    }

    // =========================================================================
    // Non-null / non-empty contract
    // =========================================================================

    @Test
    public void allAlgorithms_generateReturnsNonNullNonEmpty() {
        assertNotNull(PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT));
        assertNotNull(PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8));
        assertNotNull(PasswordUtil.SHA1.generateSHA1(PASSWORD));
        assertNotNull(PasswordUtil.MD5.generateMD5(PASSWORD));

        assertFalse(PasswordUtil.SSHA.generateSSHA(PASSWORD, FIXED_SALT).isEmpty());
        assertFalse(PasswordUtil.SSHA512.generateSSHA512(PASSWORD, FIXED_SALT_8).isEmpty());
        assertFalse(PasswordUtil.SHA1.generateSHA1(PASSWORD).isEmpty());
        assertFalse(PasswordUtil.MD5.generateMD5(PASSWORD).isEmpty());
    }
}
