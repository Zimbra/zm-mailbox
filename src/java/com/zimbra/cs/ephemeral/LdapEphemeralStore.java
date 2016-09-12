package com.zimbra.cs.ephemeral;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class LdapEphemeralStore extends EphemeralStore {

    private AbstractLdapHelper helper;

    public LdapEphemeralStore() {
        this(new ZimbraLdapHelper());
    }

    public LdapEphemeralStore(AbstractLdapHelper helper) {
        this.helper = helper;
        setAttributeEncoder(new ExpirationEncoder());
    }

    @Override
    public EphemeralResult get(String key, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        String[] values = helper.getMultiAttr(key);
        EphemeralKeyValuePair[] parsed = new EphemeralKeyValuePair[values.length];
        for (int i = 0; i < values.length; i++) {
            parsed[i] = getAttributeEncoder().decode(key, values[i]);
        }
        return new EphemeralResult(parsed);
    }

    @Override
    public void set(EphemeralInput input, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        AttributeEncoder encoder = getAttributeEncoder();
        String key = encoder.encodeKey(input, location);
        String value = encoder.encodeValue(input, location);
        helper.addChange(key, value);
        helper.executeChange();
    }

    @Override
    public void update(EphemeralInput input, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        AttributeEncoder encoder = getAttributeEncoder();
        String key = encoder.encodeKey(input, location);
        String value = encoder.encodeValue(input, location);
        helper.addChange("+" + key, value);
        helper.executeChange();
    }

    @Override
    public void delete(String key, String valueToDelete, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        // have to take into account that values may have expiration encoded in
        List<String> toDelete = new LinkedList<String>();
        for (String val: helper.getMultiAttr(key)) {
            String parsedValue = getAttributeEncoder().decode(key, val).getValue();
            if (parsedValue.equals(valueToDelete)) {
                toDelete.add(val);
            }
        }
        deleteInternal(helper, key, toDelete);
    }

    @Override
    public void purgeExpired(String key, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        String[] values = helper.getMultiAttr(key);
        List<String> purged = new LinkedList<String>();
        AttributeEncoder encoder = getAttributeEncoder();
        for (String ldapValue: values) {
            EphemeralKeyValuePair entry = encoder.decode(key, ldapValue);
            if (entry instanceof ExpirableEphemeralKeyValuePair) {
                Long expiry  = ((ExpirableEphemeralKeyValuePair) entry).getExpiration();
                if (expiry != null && System.currentTimeMillis() > expiry) {
                    String value = entry.getValue();
                    ZimbraLog.ephemeral.info("purging ephemeral value '%s' for key '%s'", key, value);
                    purged.add(ldapValue);
                }
            }
        }
        deleteInternal(helper, key, purged);
    }

    @Override
    public boolean has(String key, String value, EphemeralLocation location)
            throws ServiceException {
        String[] values = helper.getMultiAttr(key);
        if (values == null || values.length == 0) {
            return false;
        }
        Set<String> valueSet = new HashSet<String>(Arrays.asList(values));
        return valueSet.contains(value);
    }

    private void deleteInternal(AbstractLdapHelper helper, String key, List<String> values) throws ServiceException {
        for (String val: values) {
            helper.addChange("-" + key, val);
        }
        helper.executeChange();
    }

    public static class Factory implements EphemeralStore.Factory {

        @Override
        public EphemeralStore getStore() {
            return new LdapEphemeralStore();
        }

        @Override
        public void startup() {}

        @Override
        public void shutdown() {}
    }

    /**
     * Abstract helper class that accumulates an LDAP-style attribute map.
     * Subclasses specify what to do with this map, given an @EphemeralLocation,
     * as well as fetching values from this location.
     */
    @VisibleForTesting
    static abstract class AbstractLdapHelper {
        protected Map<String, Object> attrs;

        public AbstractLdapHelper() {
            attrs = new HashMap<String, Object>();
        }

        void addChange(String attr, String value) {
            StringUtil.addToMultiMap(attrs, attr, value);
        }

        void reset() {
            attrs.clear();
        }

        abstract void setLocation(EphemeralLocation location) throws ServiceException;

        abstract String getAttr(String key) throws ServiceException;

        abstract String[] getMultiAttr(String key) throws ServiceException;

        abstract void executeChange() throws ServiceException;
    }

    /**
     * Implementation of AbstractHelper that passes the attribute map to Provisioning.modifyAttrs(),
     * routing attribute changes back to the existing LDAP system.
     */
    private static class ZimbraLdapHelper extends AbstractLdapHelper {
        private Entry entry;

        @Override
        void setLocation(EphemeralLocation location) throws ServiceException {
            entry = locationToEntry(location);
        }

        @Override
        String getAttr(String key) throws ServiceException {
            return entry.getAttr(key);
        }

        @Override
        String[] getMultiAttr(String key) throws ServiceException {
            return entry.getMultiAttr(key);
        }

        @Override
        void executeChange() throws ServiceException {
            Provisioning.getInstance().modifyAttrs(entry, attrs);
            reset();
        }

        protected Entry locationToEntry(EphemeralLocation location) throws ServiceException {
            if (!(location instanceof LdapEntryLocation)) {
                throw ServiceException.FAILURE("LdapEphemeralBackend can only be used with LdapEntryLocation", null);
            }
            return ((LdapEntryLocation) location).getEntry();
        }
    }
}
