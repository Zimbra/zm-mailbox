package com.zimbra.cs.ephemeral;

import com.zimbra.common.util.Pair;

/** A class that encodes the expiration of an attribute in the value itself,
 * for use with backends that don't support automatic key expiry.
 * It should be noted that the expiration input is relative,
 * while the stored value is milliseconds since the epoch.
 * @author iraykin
 *
 */
public class ExpirationEncoder extends AttributeEncoder {

    @Override
    public Pair<String, String> encode(EphemeralInput input, EphemeralLocation target) {
        String key = input.getKey();
        Object value = input.getValue();
        Long expires = input.getExpiration();
        if (expires != null && expires > 0L) {
            value = String.format("%s|%s", String.valueOf(value), String.valueOf(expires));
        }
        // dynamic key aren't supported, so the isDynamic() flag isn't used here
        return new Pair<String, String>(key, String.valueOf(value));
    }

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
}
