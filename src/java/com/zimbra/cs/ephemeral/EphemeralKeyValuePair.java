package com.zimbra.cs.ephemeral;


/** Represents an key/value pair, as well as an optional expiration time.
 * This class is the output of @AttributeEncoder.decode(), which makes it an
 * intermediate representation of the data. One or more of these are wrapped
 * in an @EphemeralResult.
 *
 * @author iraykin
 *
 */
class EphemeralKeyValuePair {
    private String key;
    private String value;
    private Long expires;

    public EphemeralKeyValuePair(String key, String value) {
        this(key, value, null);
    }

    public EphemeralKeyValuePair(String key, String value, Long expires) {
        this.key = key;
        this.value = value;
        this.expires = expires;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Long getExpires() {
        return expires;
    }
}
