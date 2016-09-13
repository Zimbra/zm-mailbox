package com.zimbra.cs.ephemeral;

/**
 * Value encoder for use with ephemeral backends that don't support native
 * key expiration or dynamic keys, like LDAP. Both the expiration and
 * dynamic key component is encoded in the value as [value]|[dynamic key part]|[expiration]
 *
 * @author iraykin
 *
 */
public class DynamicExpirationValueEncoder extends ValueEncoder {

    @Override
    public String encodeValue(EphemeralInput input, EphemeralLocation target) {
        Object value = input.getValue();
        Long expires = input.getExpiration();
        EphemeralKey ephemeralKey = input.getEphemeralKey();
        String encoded;
        if (expires != null && expires > 0L) {
            String dynamicPart = ephemeralKey.isDynamic() ? ephemeralKey.getDynamicComponent() : "";
            encoded = String.format("%s|%s|%s", String.valueOf(value), dynamicPart, String.valueOf(expires));
        } else {
            if (ephemeralKey.isDynamic()) {
                encoded = String.format("%s|%s|", String.valueOf(value), ephemeralKey.getDynamicComponent());
            } else {
                encoded = String.valueOf(value);
            }
        }
        return encoded;
    }

}
