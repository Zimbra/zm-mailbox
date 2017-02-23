package com.zimbra.cs.ephemeral;

import java.util.concurrent.TimeUnit;

/**
 * Class representing the key/value input for ephemeral storage.
 *
 * Aside from the required key/value strings, two options can be specified:
 * 1) A value, in milliseconds, of how long the key/value pair should last.
 *    Depending on whether the EphemeralStore natively supports key expiration,
 *    this will either be encoded in the value to be used by EphemeralStore.purgeExpired(),
 *    or leveraged by the backend's own key expiration mechanism.
 * 2) Whether the key/value pair is dynamic. If the EphemeralStore supports
 *    dynamic keys (for example, if it is schemaless) this should be used
 *    as a hint to the implementation to encode the key and value into a single key.
 *    This is beneficial for use cases where we need to check whether a multivalued key
 *    contains a specific value.
 *
 * @author iraykin
 *
 */
public class EphemeralInput {
    private EphemeralKey key;
    private Object value;
    private Expiration expiration;

    public EphemeralInput(EphemeralKey key, String value) {
        this(key, value, null);
    }

    public EphemeralInput(EphemeralKey key, Integer value) {
        this(key, value, null);
    }

    public EphemeralInput(EphemeralKey key, Long value) {
        this(key, value, null);
    }

    public EphemeralInput(EphemeralKey key, Boolean value) {
        this(key, value, null);
    }

    public EphemeralInput(EphemeralKey key, String value, Expiration expiration) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
    }

    public EphemeralInput(EphemeralKey key, Integer value, Expiration expiration) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
    }

    public EphemeralInput(EphemeralKey key, Long value, Expiration expiration) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
    }

    public EphemeralInput(EphemeralKey key, Boolean value, Expiration expiration) {
        this.key = key;
        this.value = value;
        this.expiration = expiration;
    }

    public EphemeralKey getEphemeralKey() {
        return key;
    }
    public Object getValue() {
        return value;
    }

    public void setExpiration(Expiration expiration) {
        this.expiration = expiration;
    }

    /**
     * Returns the expiration of the key/value pair in milliseconds since epoch,
     * or null if the attribute does not expire.
     */
    public Long getExpiration() {
        return expiration == null ? null : expiration.getMillis();
    }

    public Long getRelativeExpiration() {
        return expiration == null ? null : expiration.getRelativeMillis();
    }

    public boolean isDynamic() {
        return key.isDynamic();
    }


    public static abstract class Expiration {
        public abstract long getMillis();
        public long getRelativeMillis() {
            return getMillis() - System.currentTimeMillis();
        }
    }

    /**
     * Class representing the relative expiration time of a key/value pair
     */
    public static class RelativeExpiration extends Expiration {
        protected Long expiresIn;
        protected TimeUnit unit;

        public RelativeExpiration(Long expiresIn, TimeUnit unit) {
            this.expiresIn = expiresIn;
            this.unit = unit;
        }

        @Override
        public long getMillis() {
            return System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(expiresIn, unit);
        }
    }

    /**
     * Class representing the expiration time of a key/value pair in milliseconds since the epoch
     */
    public static class AbsoluteExpiration extends Expiration {
        private long millis;

        public AbsoluteExpiration(long millis) {
            this.millis = millis;
        }

        @Override
        public long getMillis() {
            return millis;
        }

    }
}
