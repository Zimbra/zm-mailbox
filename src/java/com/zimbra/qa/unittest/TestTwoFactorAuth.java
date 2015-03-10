package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.CodeLength;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.HashAlgorithm;
import com.zimbra.cs.account.auth.twofactor.CredentialConfig.Encoding;
import com.zimbra.cs.account.auth.twofactor.TOTPAuthenticator;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.GenerateScratchCodesRequest;
import com.zimbra.soap.account.message.GenerateScratchCodesResponse;
import com.zimbra.soap.account.message.TwoFactorCredentials;

/**
 *
 * @author iraykin
 *
 */
public class TestTwoFactorAuth extends TestCase {
    private static final String USER_NAME = "user1";
    private static final String PASSWORD = "test123";
    private static ZMailbox mbox;
    private String secret;
    private List<String> scratchCodes;

    /*
     * Make sure these settings match those on the server! Otherwise the TOTP
     * codes will not match.
     */
    private static final int WINDOW_SIZE = 30;
    private static final HashAlgorithm HASH_ALGORITHM = HashAlgorithm.SHA1;
    private static final Encoding KEY_ENCODING = Encoding.BASE32;
    private static final int WINDOW_OFFSET = 1;
    private static final CodeLength NUM_CODE_DIGITS = CodeLength.SIX;

    @Override
    @BeforeClass
    public void setUp() throws ServiceException {
        mbox = TestUtil.getZMailbox(USER_NAME);
        EnableTwoFactorAuthResponse resp = mbox.enableTwoFactorAuth(PASSWORD);
        //have to re-authenticate since the previous auth token was invalidated by enabling two-factor auth
        mbox = TestUtil.getZMailbox(USER_NAME, resp.getCredentials().getScratchCodes().remove(0));
        TwoFactorCredentials creds = resp.getCredentials();
        secret = creds.getSharedSecret();
        scratchCodes = creds.getScratchCodes();
    }

    @Override
    @AfterClass
    public void tearDown() throws ServiceException {
        mbox.disableTwoFactorAuth(PASSWORD);
    }

    @Test
    public void testAuthenticateWithoutCode() throws ServiceException {
        try {
            TestUtil.testAuth(mbox, USER_NAME, PASSWORD);
            fail();
        } catch (ServiceException e) {
            assertEquals(ServiceException.TWO_FACTOR_AUTH_REQUIRED, e.getCode());
        }
    }

    @Test
    public void testTOTP() throws ServiceException {
        // make sure authentication succeeds for each window in the allowed
        // range
        for (int i = -1 * WINDOW_OFFSET; i <= WINDOW_OFFSET; i++) {
            tryCode(PASSWORD, generateCode(curTime() + WINDOW_SIZE * i), false, true);
        }
        // make sure authentication fails outside the window range
        tryCode(PASSWORD, generateCode(curTime() - WINDOW_SIZE
                * (WINDOW_OFFSET + 1)), false, false);
        tryCode(PASSWORD, generateCode(curTime() + WINDOW_SIZE
                * (WINDOW_OFFSET + 1)), false, false);
    }

    @Test
    public void testScratchCodes() throws ServiceException {
        // each code should only work once
        for (String code : scratchCodes) {
            tryCode(PASSWORD, code, true, true);
            tryCode(PASSWORD, code, true, false);
        }
    }

    @Test
    public void testBadCredentials() throws ServiceException {
        tryCode("wrongpassword", generateCode(curTime()), false, false);
        tryCode(PASSWORD, "badcode", false, false);
    }

    private long curTime() {
        return System.currentTimeMillis() / 1000;
    }

    private void tryCode(String password, String code, boolean isScratchCode, boolean shouldWork)
            throws ServiceException {
        try {
            if (isScratchCode) {
                TestUtil.testAuth(mbox, USER_NAME, password, null, code);
            } else {
                TestUtil.testAuth(mbox, USER_NAME, password, code, null);
            }
            if (!shouldWork) {
                fail();
            }
        } catch (ServiceException e) {
            if (shouldWork) {
                fail();
            } else {
                assertEquals(AccountServiceException.AUTH_FAILED, e.getCode());
            }
        }
    }

    private String generateCode(long timestamp) throws ServiceException {
        AuthenticatorConfig config = new AuthenticatorConfig();
        config.setHashAlgorithm(HASH_ALGORITHM);
        config.setNumCodeDigits(NUM_CODE_DIGITS);
        config.setWindowSize(WINDOW_SIZE);
        TOTPAuthenticator auth = new TOTPAuthenticator(config);
        return auth.generateCode(secret, timestamp, KEY_ENCODING);
    }

    @Test
    public void testAlreadyEnabled() throws ServiceException {
        EnableTwoFactorAuthResponse resp = mbox.enableTwoFactorAuth(PASSWORD);
        assertNull(resp.getCredentials());
    }

    @Test
    public void testGenerateNewScratchCodes() throws ServiceException {
        GenerateScratchCodesResponse resp = mbox.invokeJaxb(new GenerateScratchCodesRequest());
        List<String> newCodes = resp.getScratchCodes();
        tryCode(PASSWORD, newCodes.remove(0), true, true);
        List<String> oldCodes = newCodes;
        resp = mbox.invokeJaxb(new GenerateScratchCodesRequest());
        newCodes = resp.getScratchCodes();
        //check that the old codes don't work
        tryCode(PASSWORD, oldCodes.remove(0), true, false);
        //but the new ones do
        tryCode(PASSWORD, newCodes.remove(0), true, true);
        //store remaining in case some other test wants to use them
        this.scratchCodes = newCodes;
    }
}
