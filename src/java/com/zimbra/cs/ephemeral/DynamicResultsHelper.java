package com.zimbra.cs.ephemeral;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

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
    private List<String> valuesToDelete = new ArrayList<String>(); //values that cannot be decoded
    private DeletionCallback deletionCallback;

    public DynamicResultsHelper(EphemeralKey key, EphemeralLocation target, AttributeEncoder encoder, DeletionCallback deletionCallback) {
        this(key, target, encoder, deletionCallback, false);
    }

    public DynamicResultsHelper(EphemeralKey key, EphemeralLocation target, AttributeEncoder encoder, DeletionCallback deletionCallback, boolean broadKeyMatch) {
        this.dynamicComponent = key.getDynamicComponent();
        this.isDynamic = key.isDynamic();
        this.encoder = encoder;
        this.key = key;
        this.encodedKey = encoder.encodeKey(key, target);
        this.broadKeyMatch = broadKeyMatch;
        this.deletionCallback = deletionCallback;
    }

    public boolean filterValue(String encodedValue) throws ServiceException {
        try {
            kvp = encoder.decode(encodedKey, encodedValue);
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.PARSE_ERROR)) {
                //cannot decode value, flag for deletion
                valuesToDelete.add(encodedValue);
                return false;
            } else {
                throw e;
            }
        }
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
            deleteBadValues();
            return new EphemeralResult(results.toArray(new EphemeralKeyValuePair[results.size()]));
        }
    }

    public List<String> delete(Collection<String> values, String valueToDelete) throws ServiceException {
        List<String> toDelete = new LinkedList<String>();
        for (String v: values) {
            if (filterValue(v)) {
                EphemeralKeyValuePair kvp = getKeyValuePair();
                if (kvp.getValue().equals(valueToDelete)) {
                    toDelete.add(v);
                }
            }
        }
        deleteBadValues();
        return toDelete;
    }

    public List<String> purge(Collection<String> values) throws ServiceException {
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
        deleteBadValues();
        return purged;
    }

    public boolean has(Collection<String> values) throws ServiceException {
        if (values == null || values.size() == 0) {
            return false;
        }
        for (String v: values) {
            if (filterValue(v)) {
                return true;
            }
        }
        deleteBadValues();
        return false;
    }

    private void deleteBadValues() {
        if (!valuesToDelete.isEmpty()) {
            try {
                if (deletionCallback != null) {
                    ZimbraLog.ephemeral.debug("deleting unparseable values %s for key %s", Joiner.on(", ").join(valuesToDelete), encodedKey);
                    deletionCallback.delete(encodedKey, valuesToDelete);
                }
            } catch (ServiceException e) {
                // don't let failures here get in the way
                ZimbraLog.ephemeral.error("error deleting unparseable ephemeral values", e);
            } finally {
                valuesToDelete.clear();
            }
        }
    }

    static interface DeletionCallback {
        void delete(String encodedKey, List<String> values) throws ServiceException;
    }
}
