package com.zimbra.cs.account.auth.twofactor;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.CodeLength;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.HashAlgorithm;
import com.zimbra.cs.account.auth.twofactor.CredentialConfig.Encoding;

/** Tests all test vectors listed at https://tools.ietf.org/html/rfc6238
 *
 * @author iraykin
 *
 */
public class TestTOTPAuthenticator {
    private static long[] timestamps = {59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L};

    @Test
    public void testSHA1() throws ServiceException {
        String secret = "3132333435363738393031323334353637383930";
        String[] expected = {"94287082", "07081804", "14050471", "89005924", "69279037", "65353130"};
        doTest(HashAlgorithm.SHA1, secret, expected);
    }

    @Test
    public void testSHA256() throws ServiceException {
        String secret = "3132333435363738393031323334353637383930"
                + "313233343536373839303132";
        String[] expected = {"46119246", "68084774", "67062674", "91819424", "90698825", "77737706"};
        doTest(HashAlgorithm.SHA256, secret, expected);
    }

    @Test
    public void testSHA512() throws ServiceException {
        String secret = "3132333435363738393031323334353637383930"
                 + "3132333435363738393031323334353637383930"
                 + "3132333435363738393031323334353637383930"
                 + "31323334";
                 String[] expected = {"90693936", "25091201", "99943326", "93441116", "38618901", "47863826"};
        doTest(HashAlgorithm.SHA512, secret, expected);
    }

    private void doTest(HashAlgorithm algo, String secret, String[] expected) throws ServiceException {
        AuthenticatorConfig config = new AuthenticatorConfig();
        config.setHashAlgorithm(algo)
        .setNumCodeDigits(CodeLength.EIGHT)
        .setWindowSize(30);
        TOTPAuthenticator authenticator = new TOTPAuthenticator(config);
        for (int i = 0; i < timestamps.length; i++) {
            assertEquals(expected[i], authenticator.generateCode(hexToBytes(secret), timestamps[i]));
        }
    }

    @Test
    public void testGenerateSecret() throws ServiceException {
        testSecret(Encoding.BASE32, 16);
        testSecret(Encoding.BASE64, 16);
        testSecret(Encoding.BASE32, 24);
        testSecret(Encoding.BASE64, 24);
    }

    public void testSecret(Encoding encoding, int length) throws ServiceException {
        TOTPCredentials credentials = getCredentials(encoding, Encoding.BASE32, length, 8, 4);
        String secret = credentials.getSecret();
        assertEquals(length, secret.length());
        assertEquals(secret.toUpperCase(), secret);
        //check that it can be used to generate a code
        AuthenticatorConfig config = new AuthenticatorConfig();
        config.setHashAlgorithm(HashAlgorithm.SHA1)
        .setNumCodeDigits(CodeLength.SIX)
        .setWindowSize(30);
        TOTPAuthenticator authenticator = new TOTPAuthenticator(config);
        String code = authenticator.generateCode(secret, 59L, encoding);
        assertEquals(6, code.length());
    }

    @Test
    public void testGenerateScratchCodes() throws ServiceException {
        testScratchCodes(Encoding.BASE32, 8, 4);
        testScratchCodes(Encoding.BASE64, 16, 4);
    }

    private void testScratchCodes(Encoding encoding, int length, int numCodes) {
        TOTPCredentials credentials = getCredentials(Encoding.BASE32, encoding, 16, length, numCodes);
        Set<String> codes = credentials.getScratchCodes();
        assertEquals(numCodes, codes.size());
        for (String code: codes) {
            assertEquals(length, code.length());
        }
    }

    private TOTPCredentials getCredentials(Encoding secretEncoding, Encoding scratchCodeEncoding, int secretLength, int scratchCodeLength, int numScratchCodes) {
        CredentialConfig config = new CredentialConfig()
        .setNumScratchCodes(numScratchCodes)
        .setSecretLength(secretLength)
        .setScratchCodeLength(scratchCodeLength)
        .setEncoding(secretEncoding)
        .setScratchCodeEncoding(scratchCodeEncoding);
        return new CredentialGenerator(config).generateCredentials();
    }

    private static byte[] hexToBytes(String hex){
        byte[] bArray = new BigInteger("10" + hex,16).toByteArray();
        byte[] ret = new byte[bArray.length - 1];
        for (int i = 0; i < ret.length; i++)
            ret[i] = bArray[i+1];
        return ret;
    }
}
