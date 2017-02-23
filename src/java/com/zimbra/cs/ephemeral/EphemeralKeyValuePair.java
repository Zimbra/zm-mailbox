package com.zimbra.cs.ephemeral;


/**
 * Represents a key/value pair returned by AttributeEncoder.decode().
 * This is an intermediate representation of the data.
 * One or more of these are wrapped in an EphemeralResult.
 *
 * @author iraykin
 *
 */
public class EphemeralKeyValuePair {
    protected EphemeralKey key;
    protected String value;

    public EphemeralKeyValuePair(EphemeralKey key, String value) {
        this.key = key;
        this.value = value;
    }

    public EphemeralKey getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
