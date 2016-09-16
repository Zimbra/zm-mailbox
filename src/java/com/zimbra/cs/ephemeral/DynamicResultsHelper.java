package com.zimbra.cs.ephemeral;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * Helper class used by InMemoryEphemeralStore and LdapEphemeralStore, or potentially
 * any EphemeralStore implementation that does not support native key expiration or dynamic keys,
 * requiring the expiration and dynamic key data to be encoded in the values.
 *
 * This class handles the task of filtering multivalued results on whether the requested key was dynamic,
 * using the filterValue method, and builds on that to implement handling of the get(), delete(),
 * purgeExpired(), and has() methods of the EphemeralStore interface.
 *
 *
 * @author iraykin
 *
 */
public class DynamicResultsHelper {
    private boolean isDynamic;
    private String dynamicComponent;
    private AttributeEncoder encoder;
    private EphemeralKey key;
    private String encodedKey;
    private EphemeralKeyValuePair kvp;
    private boolean broadKeyMatch = false;

    public DynamicResultsHelper(EphemeralKey key, EphemeralLocation target, AttributeEncoder encoder) {
        this(key, target, encoder, false);
    }

    public DynamicResultsHelper(EphemeralKey key, EphemeralLocation target, AttributeEncoder encoder, boolean broadKeyMatch) {
        this.dynamicComponent = key.getDynamicComponent();
        this.isDynamic = key.isDynamic();
        this.encoder = encoder;
        this.key = key;
        this.encodedKey = encoder.encodeKey(key, target);
        this.broadKeyMatch = broadKeyMatch;
    }

    public boolean filterValue(String encodedValue) {
        kvp = encoder.decode(encodedKey, encodedValue);
        EphemeralKey ephemeralKey = kvp.getKey();
        String dc = ephemeralKey.getDynamicComponent();
        if (isDynamic && (dc != null && dc.equals(dynamicComponent))) {
            // If the requested key is dynamic, return true only if the value has a matching dynamic component
            return true;
        } else if (!isDynamic && !ephemeralKey.isDynamic()) {
            // If the requested key is not dynamic and neither is the value, we're all good
            return true;
        } else if (!isDynamic && ephemeralKey.isDynamic()){
            //If the requested key is not dynamic but the value is, only return True if broadKeyMatch is True
            return broadKeyMatch;
        } else {
            return false;
        }
    }

    public EphemeralKeyValuePair getKeyValuePair() {
        return kvp;
    }

    public EphemeralResult get(Collection<String> values) throws ServiceException {
        if (values.isEmpty()) {
            return EphemeralResult.emptyResult(key);
        } else {
            List<EphemeralKeyValuePair> results = new LinkedList<EphemeralKeyValuePair>();
            for (String v: values) {
                if (filterValue(v)) {
                    results.add(getKeyValuePair());
                }
            }
            return new EphemeralResult(results.toArray(new EphemeralKeyValuePair[results.size()]));
        }
    }

    public List<String> delete(Collection<String> values, String valueToDelete) {
        List<String> toDelete = new LinkedList<String>();
        for (String v: values) {
            if (filterValue(v)) {
                EphemeralKeyValuePair kvp = getKeyValuePair();
                if (kvp.getValue().equals(valueToDelete)) {
                    toDelete.add(v);
                }
            }
        }
        return toDelete;
    }

    public List<String> purge(Collection<String> values) {
        List<String> purged = new LinkedList<String>();
        for (String v: values) {
            if (filterValue(v)) {
                EphemeralKeyValuePair kvp = getKeyValuePair();
                if (kvp instanceof ExpirableEphemeralKeyValuePair) {
                    Long expiration = ((ExpirableEphemeralKeyValuePair) kvp).getExpiration();
                    if (expiration != null && expiration < System.currentTimeMillis()) {
                        purged.add(v);
                    }
                }
            }
        }
        return purged;
    }

    public boolean has(Collection<String> values) {
        if (values == null || values.size() == 0) {
            return false;
        }
        for (String v: values) {
            if (filterValue(v)) {
                return true;
            }
        }
        return false;
    }
}
