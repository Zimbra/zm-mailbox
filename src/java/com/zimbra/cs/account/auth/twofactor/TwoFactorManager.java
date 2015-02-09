package com.zimbra.cs.account.auth.twofactor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.http.HttpStatus.Code;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.CodeLength;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.HashAlgorithm;
import com.zimbra.cs.account.auth.twofactor.CredentialConfig.Encoding;

/**
 * This class is the main entry point for two-factor authentication.
 * 
 * @author iraykin
 *
 */
public class TwoFactorManager {
    private Account account;
    private String secret;
    private Set<String> scratchCodes;
    private Encoding encoding;
    private Encoding scratchEncoding;
    boolean hasStoredSecret;
    boolean hasStoredScratchCodes;

    public TwoFactorManager(Account account) throws ServiceException {
        this.account = account;
        loadCredentials();
    }

    /* Determine if a second factor is necessary for authenticating this account */
    public static boolean twoFactorAuthRequired(Account account) throws ServiceException {
        boolean isRequired = account.getCOS().isTwoFactorAuthRequired();
        boolean isUserEnabled = account.isTwoFactorAuthUserEnabled();
        return isUserEnabled || isRequired;
    }

    /* Determine if two-factor authentication is properly set up */
    public static boolean twoFactorAuthEnabled(Account account) throws ServiceException {
        if (twoFactorAuthRequired(account)) {
            String secret = account.getTwoFactorAuthSecret();
            return Strings.isNullOrEmpty(secret);
        } else {
            return false;
        }
    }

    private void storeSharedSecret(String secret) throws ServiceException {
        String encrypted = encrypt(secret);
        account.setTwoFactorAuthSecret(encrypted);
    }

    private String loadSharedSecret() throws ServiceException {
        String secret = account.getTwoFactorAuthSecret();
        hasStoredSecret = secret != null;
        if (secret != null) {
            String decrypted = decrypt(secret);
            return decrypted;
        } else {
            return null;
        }
    }

    private String decrypt(String encrypted) throws ServiceException {
        return DataSource.decryptData(account.getId(), encrypted);
    }

    private void loadCredentials() throws ServiceException {
        secret = loadSharedSecret();
        scratchCodes = loadScratchCodes();
    }

    private Set<String> loadScratchCodes() throws ServiceException {
        String encryptedCodes = account.getTwoFactorAuthScratchCodes();
        if (Strings.isNullOrEmpty(encryptedCodes)) {
            hasStoredScratchCodes = false;
            return new HashSet<String>();
        } else {
            hasStoredScratchCodes = true;
        }
        String commaSeparatedCodes = decrypt(encryptedCodes);
        String[] codes = commaSeparatedCodes.split(",");
        Set<String> codeSet = new HashSet<String>();
        for (int i = 0; i < codes.length; i++) {
            codeSet.add(codes[i]);
        }
        return codeSet;
    }

    private void storeScratchCodes(Set<String> codes) throws ServiceException {
        String codeString = Joiner.on(",").join(codes);
        String encrypted = encrypt(codeString);
        account.setTwoFactorAuthScratchCodes(encrypted);
    }

    private String encrypt(String data) throws ServiceException {
        return DataSource.encryptData(account.getId(), data);
    }

    private void storeScratchCodes() throws ServiceException {
        if (scratchCodes != null) {
            storeScratchCodes(scratchCodes);
        }
    }

    public TOTPCredentials generateNewCredentials() throws ServiceException {
        CredentialConfig config = getCredentialConfig();
        TOTPCredentials credentials = new CredentialGenerator(config).generateCredentials();
        return credentials;
    }

    private void storeCredentials(TOTPCredentials credentials) throws ServiceException {
        storeSharedSecret(credentials.getSecret());
        storeScratchCodes(credentials.getScratchCodes());
    }

    private Encoding getSecretEncoding() throws ServiceException {
        if (encoding == null) {
            try {
                String enc = account.getTwoFactorAuthSecretEncodingAsString();
                this.encoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.error("no valid shared secret encoding specified, defaulting to BASE32");
                encoding = Encoding.BASE32;
            }
        }
        return encoding;
    }

    private Encoding getScratchCodeEncoding() throws ServiceException {
        if (scratchEncoding == null) {
            try {
                String enc = account.getTwoFactorAuthScratchCodeEncodingAsString();
                this.scratchEncoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.error("scratch code encoding not specified, defaulting to BASE32");
                this.scratchEncoding = Encoding.BASE32;
            }
        }
        return scratchEncoding;
    }

    private CredentialConfig getCredentialConfig() throws ServiceException {
        CredentialConfig config = new CredentialConfig()
        .setSecretLength(account.getTwoFactorAuthSecretLength())
        .setScratchCodeLength(account.getTwoFactorScratchCodeLength())
        .setEncoding(getSecretEncoding())
        .setScratchCodeEncoding(getScratchCodeEncoding())
        .setNumScratchCodes(account.getTwoFactorAuthNumScratchCodes());
        return config;
    }

    public void authenticate(String password, String totp) throws ServiceException {
        if (totp == null) {
            ZimbraLog.account.error("TOTP code missing");
            throw AuthFailedServiceException.AUTH_FAILED("TOTP code missing");
        }
        long curTime = System.currentTimeMillis() / 1000;
        AuthenticatorConfig config = new AuthenticatorConfig();
        String algo = account.getTwoFactorAuthHashAlgorithmAsString();
        HashAlgorithm algorithm = HashAlgorithm.valueOf(algo);
        config.setHashAlgorithm(algorithm);
        int codeLength = account.getTwoFactorCodeLength();
        CodeLength numDigits = CodeLength.valueOf(codeLength);
        config.setNumCodeDigits(numDigits);
        config.setWindowSize(account.getTwoFactorTimeWindowLength() / 1000);
        config.allowedWindowOffset(account.getTwoFactorTimeWindowOffset());
        TOTPAuthenticator auth = new TOTPAuthenticator(config);
        if (!auth.validateCode(secret, curTime, totp, getSecretEncoding())) {
            ZimbraLog.account.error("invalid two-factor code");
            throw AuthFailedServiceException.AUTH_FAILED("invalid TOTP code");
        }
    }

    public void authenticateScratchCode(String password, String scratchCode) throws ServiceException {
        if (!checkScratchCodes(scratchCode)) {
            ZimbraLog.account.error("invalid scratch code");
            throw AuthFailedServiceException.AUTH_FAILED("invalid scratch code");
        }
    }

    private boolean checkScratchCodes(String totp) throws ServiceException {
        for (String code: scratchCodes) {
            if (code.equals(totp)) {
                invalidateScratchCode(code);
                return true;
            }
        }
        return false;
    }

    private void invalidateScratchCode(String code) throws ServiceException {
        scratchCodes.remove(code);
        storeScratchCodes();
    }

    public TOTPCredentials enableTwoFactorAuth() throws ServiceException {
        if (account.getCOS().isTwoFactorAuthRequired()) {
            ZimbraLog.account.info("two-factor auth is already required for this account");
            return null;
        }
        else {
            if (!account.isTwoFactorAuthUserEnabled()) {
                account.setTwoFactorAuthUserEnabled(true);
                TOTPCredentials creds = generateNewCredentials();
                storeCredentials(creds);
                return creds;
            } else {
                ZimbraLog.account.info("two-factor authentication already enabled");
                return null;
            }
        }
    }

    public boolean disableTwoFactorAuth() throws ServiceException {
        return disableTwoFactorAuth(true);
    }

    public boolean disableTwoFactorAuth(boolean deleteCredentials) throws ServiceException {
        if (account.getCOS().isTwoFactorAuthRequired()) {
            throw ServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH();
        } else if (account.isTwoFactorAuthUserEnabled()) {
                account.setTwoFactorAuthUserEnabled(false);
                if (deleteCredentials) {
                    account.setTwoFactorAuthSecret(null);
                    account.setTwoFactorAuthScratchCodes(null);
                }
                return true;
        } else {
            ZimbraLog.account.info("two-factor authentication already disabled");
            return false;
        }
    }
}
