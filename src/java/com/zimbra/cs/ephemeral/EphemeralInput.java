package com.zimbra.cs.ephemeral;

import java.util.concurrent.TimeUnit;

/**
 * Class representing the key/value input for ephemeral storage.
 *
 * Aside from the required key/value strings, two options can be specified:
 * 1) A value, in milliseconds, of how long the key/value pair should last.
 *    Depending on whether the @EphemeralStore natively supports key expiration,
 *    this will either be encoded in the value to be used by @EphemeralStore.purgeExpired(),
 *    or leveraged by the backend's own key expiration mechanism.
 * 2) Whether the key/value pair is dynamic. If the @EphemeralStore supports
 *    dynamic keys (for example, if it is schemaless) this should be used
 *    as a hint to the implementation to encode the key and value into a single key.
 *    This is beneficial for use cases where we need to check whether a multi-valued key
 *    contains a specific value.
 *
 * @author iraykin
 *
 */
public class EphemeralInput {
    private String key;
    private Object value;
    private Expiration expiration;
    private boolean dynamic;

    public EphemeralInput(String key, String value) {
        this(key, value, null, false);
    }

    public EphemeralInput(String key, Integer value) {
        this(key, value, null, false);
    }

    public EphemeralInput(String key, Long value) {
        this(key, value, null, false);
    }

    public EphemeralInput(String key, Boolean value) {
        this(key, value, null, false);
    }

    public EphemeralInput(String key, String value, Expiration expiration, boolean dynamic) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
        this.dynamic = dynamic;
    }

    public EphemeralInput(String key, Integer value, Expiration expiration, boolean dynamic) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
        this.dynamic = dynamic;
    }

    public EphemeralInput(String key, Long value, Expiration expiration, boolean dynamic) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
        this.dynamic = dynamic;
    }

    public EphemeralInput(String key, Boolean value, Expiration expiration, boolean dynamic) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
        this.dynamic = dynamic;
    }

    public String getKey() {
        return key;
    }
    public Object getValue() {
        return value;
    }

    public void setExpiration(Expiration expiration) {
        this.expiration = expiration;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Long getExpiration() {
        return expiration == null ? null : expiration.getMillis();
    }

    public boolean isDynamic() {
        return dynamic;
    }


    /**
     * Class representing the relative expiration time of a key/value pair
     *
     */
    public static class Expiration {
        private Long expiresIn;
        private TimeUnit unit;

        public Expiration(Long expiresIn, TimeUnit unit) {
            this.expiresIn = expiresIn;
            this.unit = unit;
        }

        public Long getMillis() {
            return System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(expiresIn, unit);
        }
    }
}
