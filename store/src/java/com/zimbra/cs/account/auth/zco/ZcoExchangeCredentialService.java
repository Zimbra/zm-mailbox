package com.zimbra.cs.account.auth.zco;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.memcached.MemcachedConnector;

public final class ZcoExchangeCredentialService {

    public static final String CREDENTIAL_PREFIX = "zcx1.";
    public static final String AUDIENCE = "zco";
    public static final long CREDENTIAL_LIFETIME_MS = 5 * 60 * 1000L; // 5 min TTL
    public static final long AUTHN_REQUEST_LIFETIME_MS = 10 * 60 * 1000L; // 10 min TTL

    private static final String CREDENTIAL_KEY_PREFIX = "cred:";
    private static final String AUTHN_KEY_PREFIX = "authn:";
    private static final String RECORD_VERSION = "v1";

    private static final Pattern HANDLE = Pattern.compile("^zcx1\\.[A-Za-z0-9_-]{43}$");
    private static final Pattern B64URL_43 = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Pattern NONCE = Pattern.compile("^[A-Za-z0-9_-]{16}$");
    private static final Pattern SAML_ID = Pattern.compile("^[A-Za-z0-9_.:-]{1,256}$");

    private static final ZcoExchangeCredentialService INSTANCE =
            new ZcoExchangeCredentialService(new DefaultStoreProvider());

    private final SecureRandom random = new SecureRandom();
    private final Supplier<ZcoCredentialStore> storeProvider;

    ZcoExchangeCredentialService(Supplier<ZcoCredentialStore> storeProvider) {
        this.storeProvider = storeProvider;
    }

    public static ZcoExchangeCredentialService getInstance() {
        return INSTANCE;
    }

    public void recordAuthnRequest(String nonce, String authnRequestId) throws ServiceException {
        if (nonce == null || !NONCE.matcher(nonce).matches()
                || authnRequestId == null || !SAML_ID.matcher(authnRequestId).matches()) {
            throw ServiceException.INVALID_REQUEST("invalid ZCO AuthnRequest record", null);
        }
        if (!storeProvider.get().put(AUTHN_KEY_PREFIX + nonce, authnRequestId, AUTHN_REQUEST_LIFETIME_MS)) {
            throw ServiceException.FAILURE("ZCO exchange store unavailable", null);
        }
    }

    public boolean consumeAuthnRequest(String nonce, String inResponseTo) {
        if (nonce == null || !NONCE.matcher(nonce).matches() || inResponseTo == null || inResponseTo.isEmpty()) {
            return false;
        }
        final ZcoCredentialStore store = storeProvider.get();
        final String key = AUTHN_KEY_PREFIX + nonce;
        final String recorded = store.get(key);
        if (recorded == null || !constantTimeEquals(recorded, inResponseTo)) {
            return false;
        }
        return store.delete(key);
    }

    public String issueCredential(Account account, String challenge) throws ServiceException {
        if (account == null || challenge == null || !B64URL_43.matcher(challenge).matches()) {
            throw ServiceException.INVALID_REQUEST("invalid ZCO challenge", null);
        }
        final byte[] raw = new byte[32];
        random.nextBytes(raw);
        final String handle = CREDENTIAL_PREFIX + b64url(raw);

        final long expiresAt = System.currentTimeMillis() + CREDENTIAL_LIFETIME_MS;
        final String record = String.join("|", RECORD_VERSION, account.getId(), challenge, AUDIENCE,
                Long.toString(expiresAt));
        if (!storeProvider.get().put(CREDENTIAL_KEY_PREFIX + sha256Hex(handle), record, CREDENTIAL_LIFETIME_MS)) {
            throw ServiceException.FAILURE("ZCO exchange store unavailable", null);
        }
        return handle;
    }

    public Account redeem(String handle, String verifier, Account requested) throws ServiceException {
        if (handle == null || !HANDLE.matcher(handle).matches()
                || verifier == null || !B64URL_43.matcher(verifier).matches()) {
            throw rejected("malformed", null);
        }
        final ZcoCredentialStore store = storeProvider.get();
        final String key = CREDENTIAL_KEY_PREFIX + sha256Hex(handle);

        final String[] rec = parse(store.get(key));
        if (rec == null) {
            throw rejected("unknown-or-expired", key);
        }
        final String accountId = rec[1];
        final String challenge = rec[2];
        if (!AUDIENCE.equals(rec[3]) || System.currentTimeMillis() > Long.parseLong(rec[4])) {
            store.delete(key);
            throw rejected("audience-or-expiry", key);
        }

        final String computed = b64url(sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
        if (!constantTimeEquals(computed, challenge)) {
            store.delete(key);
            throw rejected("verifier-mismatch", key);
        }
        if (requested == null || !accountId.equalsIgnoreCase(requested.getId())) {
            store.delete(key);
            throw rejected("account-mismatch", key);
        }
        if (!store.delete(key)) {
            throw rejected("lost-race", key);
        }

        final Account account = Provisioning.getInstance().get(Key.AccountBy.id, accountId);
        if (account == null || !account.isAccountStatusActive()) {
            throw rejected("account-not-active", key);
        }
        if (!browserAuthConfigured(account)) {
            throw rejected("browser-auth-not-configured", key);
        }
        return account;
    }

    private static boolean browserAuthConfigured(Account account) throws ServiceException {
        final Domain domain = Provisioning.getInstance().getDomain(account);
        if (domain == null) {
            return false;
        }
        final String loginUrl = domain.getWebClientLoginURL();
        final String[] ssoUrl = domain.getSamlSSOURL();
        return loginUrl != null && !loginUrl.isEmpty() && ssoUrl != null && ssoUrl.length > 0;
    }

    private static String[] parse(String record) {
        if (record == null) {
            return null;
        }
        final String[] f = record.split("\\|", -1);
        if (f.length != 5 || !RECORD_VERSION.equals(f[0])) {
            return null;
        }
        try {
            Long.parseLong(f[4]);
        } catch (NumberFormatException e) {
            return null;
        }
        return f;
    }

    private static AuthFailedServiceException rejected(String reason, String key) {
        ZimbraLog.security.warn("ZCO exchange rejected: reason=%s key=%s", reason,
                key == null ? "-" : key.substring(key.indexOf(':') + 1, key.indexOf(':') + 9));
        return AuthFailedServiceException.AUTH_FAILED("ZCO exchange failed");
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] sha256(byte[] in) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(in);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256Hex(String s) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : sha256(s.getBytes(StandardCharsets.US_ASCII))) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String b64url(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static final class DefaultStoreProvider implements Supplier<ZcoCredentialStore> {
        private final ZcoCredentialStore memcached = new MemcachedZcoCredentialStore();
        private final ZcoCredentialStore inMemory = new InMemoryZcoCredentialStore();
        private volatile boolean warned;

        @Override
        public ZcoCredentialStore get() {
            if (MemcachedConnector.isConnected()) {
                return memcached;
            }
            if (!warned) {
                warned = true;
                ZimbraLog.security.warn("ZCO exchange: memcached is not connected, using the in-memory store. "
                        + "Single use is only guaranteed on a single node.");
            }
            return inMemory;
        }
    }
}