package com.zimbra.cs.ephemeral;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ephemeral.DynamicResultsHelper.DeletionCallback;

public class LdapEphemeralStore extends EphemeralStore {

    private AbstractLdapHelper helper;
    private DeletionCallback callback;

    public LdapEphemeralStore() {
        this(new ZimbraLdapHelper());
    }

    public LdapEphemeralStore(AbstractLdapHelper helper) {
        this.helper = helper;
        this.callback = new LdapDeletionCallback();
        setAttributeEncoder(new LdapAttributeEncoder());
    }

    @Override
    public EphemeralResult get(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        String[] values = helper.getMultiAttr(key.getKey());
        DynamicResultsHelper iteratorHelper = new DynamicResultsHelper(key, location, encoder, callback);
        return iteratorHelper.get(Arrays.asList(values));
    }

    @Override
    public void set(EphemeralInput input, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        String key = encodeKey(input, location);
        String value = encodeValue(input, location);
        helper.addChange(key, value);
        helper.executeChange();
    }

    @Override
    public void update(EphemeralInput input, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        String key = encodeKey(input, location);
        String value = encodeValue(input, location);
        helper.addChange("+" + key, value);
        helper.executeChange();
    }

    @Override
    public void delete(EphemeralKey key, String valueToDelete, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        String encodedKey = encodeKey(key, location);
        // have to take into account that values may have expiration encoded in
        DynamicResultsHelper iteratorHelper = new DynamicResultsHelper(key, location, encoder, callback);
        String[] values = helper.getMultiAttr(key.getKey());
        List<String> toDelete = iteratorHelper.delete(Arrays.asList(values), valueToDelete);
        deleteInternal(helper, encodedKey, toDelete);
    }

    @Override
    public void purgeExpired(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        String encodedKey = encodeKey(key, location);
        String[] values = helper.getMultiAttr(key.getKey());
        DynamicResultsHelper iteratorHelper = new DynamicResultsHelper(key, location, encoder, callback, true);
        List<String> purged = iteratorHelper.purge(Arrays.asList(values));
        deleteInternal(helper, encodedKey, purged);
    }

    @Override
    public boolean has(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        helper.setLocation(location);
        DynamicResultsHelper iteratorHelper = new DynamicResultsHelper(key, location, encoder, callback);
        String[] values = helper.getMultiAttr(key.getKey());
        return iteratorHelper.has(Arrays.asList(values));
    }

    private void deleteInternal(AbstractLdapHelper helper, String key, List<String> values) throws ServiceException {
        for (String val: values) {
            helper.addChange("-" + key, val);
        }
        helper.executeChange();
    }

    @Override
    public void deleteData(EphemeralLocation location) throws ServiceException {
        // LDAP backend does not need to support deleting ephemeral data; deletion
        // is handled by LdapProvisioning
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

        @Override
        public void test(String url) throws ServiceException {}
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
            return entry.getAttr(key, true, true);
        }

        @Override
        String[] getMultiAttr(String key) throws ServiceException {
            return entry.getMultiAttr(key, true, true);
        }

        @Override
        void executeChange() throws ServiceException {
            if (attrs.isEmpty()) {
                return;
            }
            Provisioning prov = Provisioning.getInstance();
            if (prov instanceof LdapProvisioning) {
                ((LdapProvisioning) prov).modifyEphemeralAttrsInLdap(entry, attrs);
            } else {
                throw ServiceException.FAILURE(
                        String.format("LdapEphemeralStore can only be used with LdapProvisioning: '%s' provided",
                                prov.getClass().getName()), null);
            }
            reset();
        }

        protected Entry locationToEntry(EphemeralLocation location) throws ServiceException {
            if (!(location instanceof LdapEntryLocation)) {
                throw ServiceException.FAILURE("LdapEphemeralBackend can only be used with LdapEntryLocation", null);
            }
            return ((LdapEntryLocation) location).getEntry();
        }
    }

    private class LdapDeletionCallback implements DeletionCallback {

        @Override
        public void delete(String encodedKey, List<String> values)
                throws ServiceException {
            deleteInternal(helper, encodedKey, values);
        }
    }
}
