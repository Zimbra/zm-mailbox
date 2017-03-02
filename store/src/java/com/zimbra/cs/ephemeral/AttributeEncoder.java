package com.zimbra.cs.ephemeral;

import com.zimbra.common.service.ServiceException;


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

    /**
     * Encode an ephemeral input into a String key to be stored in the backend.
     * The output key may be a function of the EphemeralKey and target.
     * @param key
     * @param target
     * @return
     */
    //the value cannot be encoded into the key
    public String encodeKey(EphemeralKey key, EphemeralLocation target) {
        return keyEncoder.encodeKey(key, target);
    }

    /**
     * Encode ephemeral input into a String value to be stored in the backend.
     * The output value may be a function of the EphemeralKey, value, and target.
     * @param input
     * @param target
     * @return String
     */
    public String encodeValue(EphemeralInput input, EphemeralLocation target) {
        return valueEncoder.encodeValue(input, target);
    }

    /**
     *  decode a key/value pair coming from the ephemeral backend into an EphemeralKeyValuePair
     * @param key
     * @param value
     * @return EphemeralKeyValuePair
     * @throws ServiceException if the value cannot be decoded
     */
    public abstract EphemeralKeyValuePair decode(String key, String value) throws ServiceException;
}