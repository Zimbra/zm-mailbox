package com.zimbra.cs.account.auth.twofactor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.AuthenticatorConfig.CodeLength;
import com.zimbra.common.auth.twofactor.AuthenticatorConfig.HashAlgorithm;
import com.zimbra.common.auth.twofactor.CredentialConfig;
import com.zimbra.common.auth.twofactor.CredentialConfig.Encoding;
import com.zimbra.common.auth.twofactor.TOTPAuthenticator;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AppSpecificPassword;
import com.zimbra.cs.account.AppSpecificPassword.PasswordData;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.TrustedDevice;
import com.zimbra.cs.account.TrustedDeviceToken;
import com.zimbra.cs.account.ldap.ChangePasswordListener;
import com.zimbra.cs.account.ldap.LdapLockoutPolicy;
import com.zimbra.cs.ldap.LdapDateUtil;

/**
 * This class is the main entry point for two-factor authentication.
 *
 * @author iraykin
 *
 */
public class TwoFactorManager {
    private Account account;
    private String acctNamePassedIn;
    private String secret;
    private List<String> scratchCodes;
    private Encoding encoding;
    private Encoding scratchEncoding;
    boolean hasStoredSecret;
    boolean hasStoredScratchCodes;
    private Map<String, AppSpecificPassword> appPasswords = new HashMap<String, AppSpecificPassword>();

    public TwoFactorManager(Account account) throws ServiceException {
        this(account, account.getName());
    }

    public TwoFactorManager(Account account, String acctNamePassedIn) throws ServiceException {
        this.account = account;
        this.acctNamePassedIn = acctNamePassedIn;
        disableTwoFactorAuthIfNecessary();
        if (account.isFeatureTwoFactorAuthAvailable()) {
            loadCredentials();
        }
    }

    private void disableTwoFactorAuthIfNecessary() throws ServiceException {
        String encryptedSecret = account.getTwoFactorAuthSecret();
        if (!Strings.isNullOrEmpty(encryptedSecret)) {
            String decrypted = decrypt(account, encryptedSecret);
            String[] parts = decrypted.split("\\|");
            Date timestamp;
            if (parts.length == 1) {
                // For backwards compatability with the server version
                // that did not store a timestamp.
                timestamp = null;
            } else if (parts.length > 2) {
                throw ServiceException.FAILURE("invalid shared secret format", null);
            }
            try {
                timestamp = LdapDateUtil.parseGeneralizedTime(parts[1]);
            } catch (NumberFormatException e) {
                throw ServiceException.FAILURE("invalid shared secret timestamp", null);
            }
            Date lastDisabledDate = account.getCOS().getTwoFactorAuthLastReset();
            if (lastDisabledDate == null) {
                return;
            }
            if (timestamp == null || lastDisabledDate.after(timestamp)) {
                clearData();
            }
        }
    }

    public void clearData() throws ServiceException {
        account.setTwoFactorAuthEnabled(false);
        deleteCredentials();
        revokeAllAppSpecificPasswords();
        revokeAllTrustedDevices();
    }

    /* Determine if a second factor is necessary for authenticating this account */
    public boolean twoFactorAuthRequired() throws ServiceException {
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            return false;
        } else {
            boolean isRequired = account.isFeatureTwoFactorAuthRequired();
            boolean isUserEnabled = account.isTwoFactorAuthEnabled();
            return isUserEnabled || isRequired;
        }
    }

    /* Determine if two-factor authentication is properly set up */
    public boolean twoFactorAuthEnabled() throws ServiceException {
        if (twoFactorAuthRequired()) {
            String secret = account.getTwoFactorAuthSecret();
            return !Strings.isNullOrEmpty(secret);
        } else {
            return false;
        }
    }

    /* Determine if app-specific passwords are enabled for the account.
     * Two-factor auth is a prerequisite.
     */
    public boolean appSpecificPasswordsEnabled() throws ServiceException {
        if (twoFactorAuthRequired()) {
            return account.isFeatureAppSpecificPasswordsEnabled();
        } else {
            return false;
        }
    }
    private void storeSharedSecret(String secret) throws ServiceException {
        String encrypted = encrypt(secret);
        account.setTwoFactorAuthSecret(encrypted);
    }

    private String loadSharedSecret() throws ServiceException {
        String encryptedSecret = account.getTwoFactorAuthSecret();
        hasStoredSecret = encryptedSecret != null;
        if (encryptedSecret != null) {
            String decrypted = decrypt(account, encryptedSecret);
            String[] parts = decrypted.split("\\|");
            if (parts.length != 2) {
                throw ServiceException.FAILURE("invalid shared secret format", null);
            }
            String secret = parts[0];
            return secret;
        } else {
            return null;
        }
    }

    private static String decrypt(Account account, String encrypted) throws ServiceException {
        return DataSource.decryptData(account.getId(), encrypted);
    }

    private void loadCredentials() throws ServiceException {
        secret = loadSharedSecret();
        scratchCodes = loadScratchCodes();
        appPasswords = loadAppPasswords();
    }

    private List<String> loadScratchCodes() throws ServiceException {
        String encryptedCodes = account.getTwoFactorAuthScratchCodes();
        if (Strings.isNullOrEmpty(encryptedCodes)) {
            hasStoredScratchCodes = false;
            return new ArrayList<String>();
        } else {
            hasStoredScratchCodes = true;
        }
        String commaSeparatedCodes = decrypt(account, encryptedCodes);
        String[] codes = commaSeparatedCodes.split(",");
        List<String> codeList = new ArrayList<String>();
        for (int i = 0; i < codes.length; i++) {
            codeList.add(codes[i]);
        }
        return codeList;
    }

    private void storeScratchCodes(List<String> codes) throws ServiceException {
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

    public List<String> generateNewScratchCodes() throws ServiceException {
        ZimbraLog.account.debug("invalidating current scratch codes");
        CredentialConfig config = getCredentialConfig();
        List<String> newCodes = new CredentialGenerator(config).generateScratchCodes();
        scratchCodes.clear();
        scratchCodes.addAll(newCodes);
        storeScratchCodes();
        return scratchCodes;

    }

    private void storeCredentials(TOTPCredentials credentials) throws ServiceException {
        String secret = String.format("%s|%s", credentials.getSecret(), credentials.getTimestamp());
        storeSharedSecret(secret);
        storeScratchCodes(credentials.getScratchCodes());
    }

    private Encoding getSecretEncoding() throws ServiceException {
        if (encoding == null) {
            try {
                String enc = getGlobalConfig().getTwoFactorAuthSecretEncodingAsString();
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
                String enc = getGlobalConfig().getTwoFactorAuthScratchCodeEncodingAsString();
                this.scratchEncoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.error("scratch code encoding not specified, defaulting to BASE32");
                this.scratchEncoding = Encoding.BASE32;
            }
        }
        return scratchEncoding;
    }

    private Config getGlobalConfig() throws ServiceException {
        return Provisioning.getInstance().getConfig();
    }

    public CredentialConfig getCredentialConfig() throws ServiceException {
        CredentialConfig config = new CredentialConfig()
        .setSecretLength(getGlobalConfig().getTwoFactorAuthSecretLength())
        .setScratchCodeLength(getGlobalConfig().getTwoFactorScratchCodeLength())
        .setEncoding(getSecretEncoding())
        .setScratchCodeEncoding(getScratchCodeEncoding())
        .setNumScratchCodes(account.getCOS().getTwoFactorAuthNumScratchCodes());
        return config;
    }

    public AuthenticatorConfig getAuthenticatorConfig() throws ServiceException {
        AuthenticatorConfig config = new AuthenticatorConfig();
        String algo = Provisioning.getInstance().getConfig().getTwoFactorAuthHashAlgorithmAsString();
        HashAlgorithm algorithm = HashAlgorithm.valueOf(algo);
        config.setHashAlgorithm(algorithm);
        int codeLength = getGlobalConfig().getTwoFactorCodeLength();
        CodeLength numDigits = CodeLength.valueOf(codeLength);
        config.setNumCodeDigits(numDigits);
        config.setWindowSize(getGlobalConfig().getTwoFactorTimeWindowLength() / 1000);
        config.allowedWindowOffset(getGlobalConfig().getTwoFactorTimeWindowOffset());
        return config;
    }

    private boolean checkTOTPCode(String code) throws ServiceException {
        long curTime = System.currentTimeMillis() / 1000;
        AuthenticatorConfig config = getAuthenticatorConfig();
        TOTPAuthenticator auth = new TOTPAuthenticator(config);
        return auth.validateCode(secret, curTime, code, getSecretEncoding());
    }

    public void authenticateTOTP(String code) throws ServiceException {
        if (!checkTOTPCode(code)) {
            ZimbraLog.account.error("invalid TOTP code");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid TOTP code");
        }
    }

    public void authenticate(String code) throws ServiceException {
        if (code == null) {
            ZimbraLog.account.error("two-factor code missing");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "two-factor code missing");
        }
        Boolean codeIsScratchCode = isScratchCode(code);
        if (codeIsScratchCode == null || codeIsScratchCode.equals(false)) {
            if (!checkTOTPCode(code)) {
                boolean success = false;
                if (codeIsScratchCode == null) {
                    //could maybe be a scratch code
                    success = checkScratchCodes(code);
                }
                if (!success) {
                    failedLogin();
                    ZimbraLog.account.error("invalid two-factor code");
                    throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid two-factor code");
                }
            }
        } else {
            authenticateScratchCode(code);
        }
    }

    private Boolean isScratchCode(String code) throws ServiceException {
        int totpLength = getGlobalConfig().getTwoFactorCodeLength();
        int scratchCodeLength = getGlobalConfig().getTwoFactorScratchCodeLength();
        if (totpLength == scratchCodeLength) {
            try {
                Integer.valueOf(code);
                //most likely a TOTP code, but theoretically possible for this to be a scratch code with only digits
                return null;
            } catch (NumberFormatException e) {
                //has alnum characters, so must be a scratch code
                return true;
            }
        } else {
            return code.length() != totpLength;
        }
    }

    public void authenticateScratchCode(String scratchCode) throws ServiceException {
        if (!checkScratchCodes(scratchCode)) {
            failedLogin();
            ZimbraLog.account.error("invalid scratch code");
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid scratch code");
        }
    }

    public void authenticateAppSpecificPassword(String providedPassword) throws ServiceException {
        for (AppSpecificPassword appPassword: appPasswords.values())    {
            if (appPassword.validate(providedPassword)) {
                ZimbraLog.account.debug("logged in with app-specific password");
                appPassword.update();
                return;
            }
        }
        throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "invalid app-specific password");
    }

    private boolean checkScratchCodes(String scratchCode) throws ServiceException {
        for (String code: scratchCodes) {
            if (code.equals(scratchCode)) {
                invalidateScratchCode(code);
                return true;
            }
        }
        return false;
    }

    public List<String> getScratchCodes() {
        return scratchCodes;
    }

    private void invalidateScratchCode(String code) throws ServiceException {
        scratchCodes.remove(code);
        storeScratchCodes();
    }

    public TOTPCredentials generateCredentials() throws ServiceException {
        if (!account.isTwoFactorAuthEnabled()) {
            TOTPCredentials creds = generateNewCredentials();
            storeCredentials(creds);
            return creds;
        } else {
            ZimbraLog.account.info("two-factor authentication already enabled");
            return null;
        }
    }

    public void enableTwoFactorAuth() throws ServiceException {
        account.setTwoFactorAuthEnabled(true);
    }

    private void deleteCredentials() throws ServiceException {
        account.setTwoFactorAuthSecret(null);
        account.setTwoFactorAuthScratchCodes(null);
    }

    public boolean disableTwoFactorAuth(boolean deleteCredentials) throws ServiceException {
        if (account.isFeatureTwoFactorAuthRequired()) {
            throw ServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH();
        } else if (account.isTwoFactorAuthEnabled()) {
            account.setTwoFactorAuthEnabled(false);
            if (deleteCredentials) {
                deleteCredentials();
            }
            revokeAllAppSpecificPasswords();
            return true;
        } else {
            ZimbraLog.account.info("two-factor authentication already disabled");
            return false;
        }
    }

    public AppSpecificPassword generateAppSpecificPassword(String name) throws ServiceException {
        if (!account.isFeatureAppSpecificPasswordsEnabled()) {
            throw ServiceException.FAILURE("app-specific passwords are not enabled", new Throwable());
        }
        if (appPasswords.containsKey(name)) {
            throw ServiceException.FAILURE("app-specific password already exists for the name " + name, new Throwable());
        } else if (appPasswords.size() >= account.getMaxAppSpecificPasswords()) {
            throw ServiceException.FAILURE("app-specific password limit reached", new Throwable());
        }
        AppSpecificPassword password = AppSpecificPassword.generateNew(account, name);
        password.store();
        appPasswords.put(name, password);
        return password;
    }

    public Set<PasswordData> getAppSpecificPasswords() throws ServiceException {
        Set<PasswordData> dataSet = new HashSet<PasswordData>();
        for (AppSpecificPassword appPassword: appPasswords.values()) {
            dataSet.add(appPassword.getPasswordData());
        }
        return dataSet;
    }

    public void revokeAppSpecificPassword(String name) throws ServiceException  {
        if (appPasswords.containsKey(name)) {
            appPasswords.get(name).revoke();
        } else {
            //if a password is not provisioned for this app, log but don't return an error
            ZimbraLog.account.error("no app-specific password provisioned for the name " + name);
        }
    }

    public int getNumAppPasswords() {
        return appPasswords.size();
    }

    private Map<String, AppSpecificPassword> loadAppPasswords() throws ServiceException {
        Map<String, AppSpecificPassword> passMap = new HashMap<String, AppSpecificPassword>();
        String[] passwords = account.getAppSpecificPassword();
        for (int i = 0; i < passwords.length; i++) {
            AppSpecificPassword entry = new AppSpecificPassword(account, passwords[i]);
            if (entry != null) {
                if (entry.isExpired()) {
                    entry.revoke();
                } else {
                    passMap.put(entry.getName(), entry);
                }
            }
        }
        return passMap;
    }

    public void revokeAllAppSpecificPasswords() throws ServiceException {
        for (String name: appPasswords.keySet()) {
            revokeAppSpecificPassword(name);
        }
    }

    public TrustedDeviceToken registerTrustedDevice(Map<String, Object> deviceAttrs) throws ServiceException {
        if (!account.isFeatureTrustedDevicesEnabled()) {
            ZimbraLog.account.warn("attempting to register a trusted device when this feature is not enabled");
            return null;
        }
        TrustedDevice td = new TrustedDevice(account, deviceAttrs);
        ZimbraLog.account.debug("registering new trusted device");
        td.register();
        return td.getToken();
    }

    public List<TrustedDevice> getTrustedDevices() throws ServiceException {
        List<TrustedDevice> trustedDevices = new ArrayList<TrustedDevice>();
        for (String encoded: account.getTwoFactorAuthTrustedDevices()) {
            try {
                TrustedDevice td = new TrustedDevice(account, encoded);
                if (td.isExpired()) {
                    td.revoke();
                }
                trustedDevices.add(td);
            } catch (ServiceException e) {
                ZimbraLog.account.error(e.getMessage());
                account.removeTwoFactorAuthTrustedDevices(encoded);
            }
        }
        return trustedDevices;
    }

    public void revokeTrustedDevice(TrustedDeviceToken token) throws ServiceException {
        ZimbraLog.account.debug("revoking current trusted device");
        TrustedDevice td;
        try {
            td = TrustedDevice.byTrustedToken(account, token);
        } catch (AccountServiceException e) {
            ZimbraLog.account.warn("trying to revoke a trusted auth token with no corresponding device");
            return;
        }
        td.revoke();
    }

    public void revokeAllTrustedDevices() throws ServiceException {
        ZimbraLog.account.debug("revoking all trusted devices");
        for (TrustedDevice td: getTrustedDevices()) {
            td.revoke();
        }
    }

    public void revokeOtherTrustedDevices(TrustedDeviceToken token) throws ServiceException {
        if (token == null) {
            revokeAllTrustedDevices();
        } else {
            ZimbraLog.account.debug("revoking other trusted devices");
            for (TrustedDevice td: getTrustedDevices()) {
                if (!td.getTokenId().equals(token.getId())) {
                    td.revoke();
                }
            }
        }
    }

    public void verifyTrustedDevice(TrustedDeviceToken token, Map<String, Object> attrs) throws ServiceException {
        ZimbraLog.account.debug("verifying trusted device");
        TrustedDevice td = TrustedDevice.byTrustedToken(account, token);
        if (td == null || !td.verify(attrs)) {
            throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(account.getName(), acctNamePassedIn, "trusted device cannot be verified");
        }
    }

    private void failedLogin() throws ServiceException {
        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(Provisioning.getInstance(), account);
        lockoutPolicy.failedSecondFactorLogin();
    }

    public static class TwoFactorPasswordChange extends ChangePasswordListener {
        public static final String LISTENER_NAME = "twofactorpasswordchange";

        @Override
        public void preModify(Account acct, String newPassword, Map context,
                Map<String, Object> attrsToModify) throws ServiceException {
        }

        @Override
        public void postModify(Account acct, String newPassword, Map context) {
            if (acct.isRevokeAppSpecificPasswordsOnPasswordChange()) {
                try {
                    ZimbraLog.account.info("revoking all app-specific passwords due to password change");
                    new TwoFactorManager(acct).revokeAllAppSpecificPasswords();
                } catch (ServiceException e) {
                    ZimbraLog.account.error("could not revoke app-specific passwords on password change", e);
                }
            }
        }
    }
}