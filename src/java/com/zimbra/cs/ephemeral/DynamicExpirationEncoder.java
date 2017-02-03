package com.zimbra.cs.ephemeral;

import com.zimbra.common.service.ServiceException;


/**
 * AttributeEncoder to be used with backends that do not support
 * dynamic keys or native key expiration, like @LdapEphemeralStore
 *
 * @author iraykin
 *
 */
public class DynamicExpirationEncoder extends AttributeEncoder {

    public DynamicExpirationEncoder() {
        setKeyEncoder(new StaticKeyEncoder());
        setValueEncoder(new DynamicExpirationValueEncoder());
    }

    @Override
    public ExpirableEphemeralKeyValuePair decode(String key, String value) throws ServiceException {
        EphemeralKey ephemeralKey;
        String decodedValue;
        String dynamicKeyPart;
        Long expires = null;
        int pipeIndex = value.lastIndexOf("|");
        if (pipeIndex == -1) {
            // nothing is encoded in the value
            ephemeralKey = new EphemeralKey(key);
            decodedValue = value;
        } else if (value.endsWith("|")) {
            // expecting value to be of the form "[value]|[dynamic key part]|"
            //dynamic but no expiry
            String sub = value.substring(0, value.length() - 1);
            int i = sub.lastIndexOf("|");
            if (i > 0) {
                decodedValue = sub.substring(0, i);
                dynamicKeyPart = sub.substring(i+1);
                ephemeralKey = new EphemeralKey(key, dynamicKeyPart);
            } else {
                //could be just a value that happens to end with a pipe
                decodedValue = value;
                ephemeralKey = new EphemeralKey(key);
            }
        } else {
            // expecting value to be of the form "[value]|[possibly empty dynamic key part]|[expiration]
            String sub = value.substring(0, pipeIndex);
            String expiresStr = value.substring(pipeIndex+1);
            if (sub.endsWith("|")) {
                // value is of the form "[value]||[expiration]", so no dynamic key part
                ephemeralKey = new EphemeralKey(key);
                decodedValue = sub.substring(0, sub.length() - 1);
            } else {
                int prevPipeIndex = sub.lastIndexOf("|");
                if (prevPipeIndex > 0) {
                    dynamicKeyPart = sub.substring(prevPipeIndex + 1);
                    ephemeralKey = new EphemeralKey(key, dynamicKeyPart);
                    decodedValue = sub.substring(0, prevPipeIndex);
                } else {
                    // value is of the form "[something]|[something]", which means
                    // it's not actually encoded
                    ephemeralKey = new EphemeralKey(key);
                    decodedValue = value;
                    expiresStr = null;
                }
            }
            if (expiresStr != null) {
                try {
                    expires = Long.valueOf(expiresStr);
                } catch (NumberFormatException e) {
                    decodedValue = value;
                    ephemeralKey = new EphemeralKey(key);
                }
            }
        }
        // does this value have an expiry associated with it?
        return new ExpirableEphemeralKeyValuePair(ephemeralKey, decodedValue, expires);
    }
}
