package com.zimbra.cs.ephemeral;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;

/**
 * Attribute encoder specific to LdapEphemeralBackend.
 * @author iraykin
 *
 */
public class LdapAttributeEncoder extends DynamicExpirationEncoder {

    public LdapAttributeEncoder() {
        setKeyEncoder(new StaticKeyEncoder());
        setValueEncoder(new LdapValueEncoder());
    }

    @Override
    public ExpirableEphemeralKeyValuePair decode(String key, String value) {
        if (key.equalsIgnoreCase(Provisioning.A_zimbraAuthTokens)) {
            return decodeAuthToken(value);
        } else if (key.equalsIgnoreCase(Provisioning.A_zimbraCsrfTokenData)) {
            return decodeCsrfToken(value);
        } else {
            return super.decode(key, value);
        }
    }

    private ExpirableEphemeralKeyValuePair decodeAuthToken(String value) {
        String[] parts = value.split("\\|");
        if (parts.length != 3) {
            ZimbraLog.ephemeral.warn("LDAP auth token %s cannot be parsed", value);
            return null;
        }
        String token = parts[0];
        Long expirationMillis;
        try {
            expirationMillis = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("LDAP auth token %s does not have a valid expiration value", value);
            return null;
        }
        String serverVersion = parts[2];
        EphemeralKey key = new EphemeralKey(Provisioning.A_zimbraAuthTokens, token);
        return new ExpirableEphemeralKeyValuePair(key, serverVersion, expirationMillis);
    }

    private ExpirableEphemeralKeyValuePair decodeCsrfToken(String value) {
        String[] parts = value.split(":");
        if (parts.length != 3) {
            ZimbraLog.ephemeral.warn("CSRF auth token %s cannot be parsed", value);
            return null;
        }
        String data = parts[0];
        String crumb = parts[1];
        Long expirationMillis;
        try {
            expirationMillis = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("CSRF auth token %s does not have a valid expiration value", value);
            return null;
        }
        EphemeralKey key = new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, crumb);
        return new ExpirableEphemeralKeyValuePair(key, data, expirationMillis);
    }
}
