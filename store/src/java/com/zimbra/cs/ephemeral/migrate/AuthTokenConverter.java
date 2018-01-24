package com.zimbra.cs.ephemeral.migrate;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.ephemeral.EphemeralKey;

/**
 * Converter used to migrate pre-8.8 auth tokens out of LDAP into ephemeral store.
 *
 * @author iraykin
 *
 */
public class AuthTokenConverter extends MultivaluedAttributeConverter {

    @Override
    public EphemeralInput convert(String attrName, Object ldapValue) {
        String ldapValueStr = (String) ldapValue;
        String[] parts = ldapValueStr.split("\\|");
        if (parts.length != 3) {
            ZimbraLog.ephemeral.warn("LDAP auth token %s cannot be parsed", ldapValueStr);
            return null;
        }
        String token = parts[0];
        Long expirationMillis;
        try {
            expirationMillis = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("LDAP auth token %s does not have a valid expiration value", ldapValueStr);
            return null;
        }
        String serverVersion = parts[2];
        EphemeralKey key = new EphemeralKey(attrName, token);
        EphemeralInput input = new EphemeralInput(key, serverVersion);
        Expiration expiration = new AbsoluteExpiration(expirationMillis);
        input.setExpiration(expiration);
        return input;
    }
}
