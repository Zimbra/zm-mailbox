package com.zimbra.cs.ephemeral;

/**
 * The key portion of an ephemeral key/value pair.
 * A wrapper around a string key, with an optional "dynamic" component.
 * It is up to the EphemeralBackend implementation to decide
 * what to do with each part of the key.
 *
 * @author iraykin
 *
 */
public class EphemeralKey {

    private String key;
    private String dynamicComponent;

    public EphemeralKey(String key) {
        this(key, null);
    }

    public EphemeralKey(String key, String dynamicComponent) {
        this.key = key;
        this.dynamicComponent = dynamicComponent;
    }

    public String getKey() {
        return key;
    }

    public String getDynamicComponent() {
        return dynamicComponent;
    }

    public boolean isDynamic() {
        return dynamicComponent != null;
    }

    @Override
    public String toString() {
        if (isDynamic()) {
            return String.format("Ephemeral key [%s <%s>]", key, dynamicComponent);
        } else {
            return String.format("Ephemeral key [%s]", key);
        }
    }
}
