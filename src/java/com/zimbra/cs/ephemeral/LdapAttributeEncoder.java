package com.zimbra.cs.ephemeral;

import com.zimbra.common.service.ServiceException;
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
    public ExpirableEphemeralKeyValuePair decode(String key, String value) throws ServiceException {
        if (key.equalsIgnoreCase(Provisioning.A_zimbraAuthTokens)) {
            return decodeAuthToken(value);
        } else if (key.equalsIgnoreCase(Provisioning.A_zimbraCsrfTokenData)) {
            return decodeCsrfToken(value);
        } else {
            return super.decode(key, value);
        }
    }

    private ExpirableEphemeralKeyValuePair decodeAuthToken(String value) throws ServiceException {
        String[] parts = value.split("\\|");
        if (parts.length != 3) {
            throw ServiceException.PARSE_ERROR(String.format("LDAP auth token %s cannot be parsed", value), null);
        }
        String token = parts[0];
        Long expirationMillis;
        try {
            expirationMillis = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw ServiceException.PARSE_ERROR(String.format("LDAP auth token %s does not have a valid expiration value", value), e);
        }
        String serverVersion = parts[2];
        EphemeralKey key = new EphemeralKey(Provisioning.A_zimbraAuthTokens, token);
        return new ExpirableEphemeralKeyValuePair(key, serverVersion, expirationMillis);
    }

    private ExpirableEphemeralKeyValuePair decodeCsrfToken(String value) throws ServiceException {
        String[] parts = value.split(":");
        if (parts.length != 3) {
            throw ServiceException.PARSE_ERROR(String.format("CSRF token %s cannot be parsed", value), null);
        }
        String data = parts[0];
        String crumb = parts[1];
        Long expirationMillis;
        try {
            expirationMillis = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw ServiceException.PARSE_ERROR(String.format("LDAP CSRF token %s does not have a valid expiration value", value), e);
        }
        EphemeralKey key = new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, crumb);
        return new ExpirableEphemeralKeyValuePair(key, data, expirationMillis);
    }
}
