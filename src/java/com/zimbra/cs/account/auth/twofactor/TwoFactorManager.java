package com.zimbra.cs.account.auth.twofactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AppSpecificPassword;
import com.zimbra.cs.account.AppSpecificPassword.PasswordData;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.CodeLength;
import com.zimbra.cs.account.auth.twofactor.AuthenticatorConfig.HashAlgorithm;
import com.zimbra.cs.account.auth.twofactor.CredentialConfig.Encoding;
import com.zimbra.cs.account.ldap.ChangePasswordListener;

/**
 * This class is the main entry point for two-factor authentication.
 *
 * @author iraykin
 *
 */
public class TwoFactorManager {
	private Account account;
	private String secret;
	private List<String> scratchCodes;
	private Encoding encoding;
	private Encoding scratchEncoding;
	boolean hasStoredSecret;
	boolean hasStoredScratchCodes;
	private Map<String, AppSpecificPassword> appPasswords = new HashMap<String, AppSpecificPassword>();

	public TwoFactorManager(Account account) throws ServiceException {
		this.account = account;
		loadCredentials();
	}

	/* Determine if a second factor is necessary for authenticating this account */
	public static boolean twoFactorAuthRequired(Account account) throws ServiceException {
		boolean isRequired = account.isFeatureTwoFactorAuthRequired();
		boolean isUserEnabled = account.isPrefTwoFactorAuthEnabled();
		return isUserEnabled || isRequired;
	}

	/* Determine if two-factor authentication is properly set up */
	public static boolean twoFactorAuthEnabled(Account account) throws ServiceException {
		if (twoFactorAuthRequired(account)) {
			String secret = account.getTwoFactorAuthSecret();
			return !Strings.isNullOrEmpty(secret);
		} else {
			return false;
		}
	}

	/* Determine if app-specific passwords are enabled for the account.
	 * Two-factor auth is a prerequisite.
	 */
	public static boolean appSpecificPasswordsEnabled(Account account) throws ServiceException {
	    if (twoFactorAuthRequired(account)) {
	        Server server = account.getServer();
	        if (server == null) {
	            return false;
	        } else {
	            return server.isFeatureAppSpecificPasswordsEnabled();
	        }
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
		String commaSeparatedCodes = decrypt(encryptedCodes);
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
		storeSharedSecret(credentials.getSecret());
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

	public void authenticate(String password, String totp) throws ServiceException {
		if (totp == null) {
			ZimbraLog.account.error("TOTP code missing");
			throw AuthFailedServiceException.AUTH_FAILED("TOTP code missing");
		}
		long curTime = System.currentTimeMillis() / 1000;
		AuthenticatorConfig config = getAuthenticatorConfig();
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

	public void authenticateAppSpecificPassword(String providedPassword) throws ServiceException {
		for (AppSpecificPassword appPassword: appPasswords.values())	{
			if (appPassword.validate(providedPassword)) {
				ZimbraLog.account.debug("logged in with app-specific password");
				appPassword.update();
				return;
			}
		}
		throw AuthFailedServiceException.AUTH_FAILED("invalid app-specific password");
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
		if (!account.isPrefTwoFactorAuthEnabled()) {
			account.setPrefTwoFactorAuthEnabled(true);
			TOTPCredentials creds = generateNewCredentials();
			storeCredentials(creds);
			return creds;
		} else {
			ZimbraLog.account.info("two-factor authentication already enabled");
			return null;
		}
	}

	public boolean disableTwoFactorAuth() throws ServiceException {
		return disableTwoFactorAuth(true);
	}

	public boolean disableTwoFactorAuth(boolean deleteCredentials) throws ServiceException {
		if (account.isFeatureTwoFactorAuthRequired()) {
			throw ServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH();
		} else if (account.isPrefTwoFactorAuthEnabled()) {
			account.setPrefTwoFactorAuthEnabled(false);
			if (deleteCredentials) {
				account.setTwoFactorAuthSecret(null);
				account.setTwoFactorAuthScratchCodes(null);
			}
			revokeAllAppSpecificPasswords();
			return true;
		} else {
			ZimbraLog.account.info("two-factor authentication already disabled");
			return false;
		}
	}

	public AppSpecificPassword generateAppSpecificPassword(String name) throws ServiceException {
	    if (!account.getServer().isFeatureAppSpecificPasswordsEnabled()) {
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