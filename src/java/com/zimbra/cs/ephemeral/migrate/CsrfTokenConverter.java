package com.zimbra.cs.ephemeral.migrate;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.ephemeral.EphemeralKey;

/**
 * Converter used to migrate pre-8.8 CSRF tokens out of LDAP into ephemeral store.
 *
 * @author iraykin
 */

public class CsrfTokenConverter extends MultivaluedAttributeConverter {

    @Override
    public EphemeralInput convert(String attrName, Object ldapValue) {
        String ldapValueStr = (String) ldapValue;
        String[] parts = ldapValueStr.split(":");
        if (parts.length != 3) {
            ZimbraLog.ephemeral.warn("CSRF auth token %s cannot be parsed", ldapValueStr);
            return null;
        }
        String data = parts[0];
        String crumb = parts[1];
        Long expirationMillis;
        try {
            expirationMillis = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("CSRF auth token %s does not have a valid expiration value", ldapValueStr);
            return null;
        }
        EphemeralKey key = new EphemeralKey(attrName, crumb);
        EphemeralInput input = new EphemeralInput(key, data);
        Expiration expiration = new AbsoluteExpiration(expirationMillis);
        input.setExpiration(expiration);
        return input;
    }
}
