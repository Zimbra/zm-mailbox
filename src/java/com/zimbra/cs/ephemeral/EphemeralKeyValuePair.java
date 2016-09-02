package com.zimbra.cs.ephemeral;


/**
 * Represents a key/value pair returned by @AttributeEncoder.decode().
 * This is an intermediate representation of the data.
 * One or more of these are wrapped in an @EphemeralResult.
 *
 * @author iraykin
 *
 */
class EphemeralKeyValuePair {
    protected String key;
    protected String value;

    public EphemeralKeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
