package com.zimbra.cs.ephemeral;


/** An abstract helper class responsible for converting EphemeralBackend inputs
 * into key/value String pairs, needed for each EphemeralBackend implementation, and vice-versa.
 *
 * @author iraykin
 */
public abstract class AttributeEncoder {

    //encode inputs into the key to be used by the backend
    public abstract String encodeKey(EphemeralInput attribute, EphemeralLocation target);

    //encode inputs into the value to be used by the backend
    public abstract String encodeValue(EphemeralInput attribute, EphemeralLocation target);

    // decode a key/value pair coming from the ephemeral backend into an EphemeralKeyValuePair
    public abstract EphemeralKeyValuePair decode(String key, String value);
}