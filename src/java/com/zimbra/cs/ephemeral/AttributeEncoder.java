package com.zimbra.cs.ephemeral;


/** An abstract helper class responsible for converting EphemeralBackend inputs
 * into key/value String pairs, needed for each EphemeralBackend implementation, and vice-versa.
 * The encoding is delegated to @KeyEncoder and @ValueEncoder implementations.
 * Subclasses set the key and value encoders, and implement the decode() method to convert
 * the stored key/value strings into EphemeralKeyValuePair instances.
 *
 * @author iraykin
 */
public abstract class AttributeEncoder {

    protected KeyEncoder keyEncoder;
    protected ValueEncoder valueEncoder;

    protected void setKeyEncoder(KeyEncoder encoder) {
        this.keyEncoder = encoder;
    }

    protected void setValueEncoder(ValueEncoder encoder) {
        this.valueEncoder = encoder;
    }

    //final keys can only be a function of the EphemeralKey and target;
    //the value cannot be encoded into the key
    public String encodeKey(EphemeralKey key, EphemeralLocation target) {
        return keyEncoder.encodeKey(key, target);
    }

    //final values may be a function of the EphemeralKey, value, and target
    public String encodeValue(EphemeralInput input, EphemeralLocation target) {
        return valueEncoder.encodeValue(input, target);
    }

    // decode a key/value pair coming from the ephemeral backend into an EphemeralKeyValuePair
    public abstract EphemeralKeyValuePair decode(String key, String value);
}