package com.zimbra.cs.ephemeral;

import com.zimbra.common.util.Pair;

/** An abstract helper class responsible for converting EphemeralBackend inputs
 * into key/value String pairs, needed for each EphemeralBackend implementation, and vice-versa.
 *
 * @author iraykin
 */
public abstract class AttributeEncoder {

    // encode ephemeral attribute data into a key/value String pair
    public abstract Pair<String, String> encode(EphemeralInput attribute, EphemeralLocation target);

    // decode a key/value pair coming from the ephemeral backend into an EphemeralKeyValuePair
    public abstract EphemeralKeyValuePair decode(String key, String value);
}