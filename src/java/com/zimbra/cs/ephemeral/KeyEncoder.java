package com.zimbra.cs.ephemeral;

/**
 * Base class for a key encoder; to be composed with a @ValueEncoder
 * to make an @AttributeEncoder
 *
 * @author iraykin
 *
 */
public abstract class KeyEncoder {

    public abstract String encodeKey(EphemeralKey key, EphemeralLocation target);

}
