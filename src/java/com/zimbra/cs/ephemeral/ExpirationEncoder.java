package com.zimbra.cs.ephemeral;


/**
 * A class that encodes the expiration of an attribute in the value itself,
 * for use with backends that don't support automatic key expiry.
 * It should be noted that the expiration input is relative,
 * while the stored value is milliseconds since the epoch.
 * @author iraykin
 *
 */
public class ExpirationEncoder extends AttributeEncoder {

    @Override
    public EphemeralKeyValuePair decode(String key, String value) {
        // does this value have an expiry associated with it?
        int i = value.lastIndexOf("|");
        Long expires = null;
        if (i > 0) {
            try {
                expires = Long.valueOf(value.substring(i+1));
                value = value.substring(0, i);
            } catch (NumberFormatException e) {
                // value stays as the original
            }
        }
        return new EphemeralKeyValuePair(key, value, expires);
    }

    @Override
    public String encodeKey(EphemeralInput input, EphemeralLocation target) {
        return input.getKey();
    }

    @Override
    public String encodeValue(EphemeralInput input, EphemeralLocation target) {
        Object value = input.getValue();
        Long expires = input.getExpiration();
        String encoded;
        if (expires != null && expires > 0L) {
            encoded = String.format("%s|%s", String.valueOf(value), String.valueOf(expires));
        } else {
            encoded = String.valueOf(value);
        }
        return encoded;
    }
}
