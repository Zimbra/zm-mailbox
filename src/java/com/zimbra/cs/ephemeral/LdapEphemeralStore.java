package com.zimbra.cs.ephemeral;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class LdapEphemeralStore extends EphemeralStore {

    private AttributeEncoder attributeEncoder;

    public LdapEphemeralStore() {
        attributeEncoder = new ExpirationEncoder();
    }

    @Override
    public EphemeralResult get(String key, EphemeralLocation target)
            throws ServiceException {
        Entry entry = ldapLocationToEntry(target);
        String[] values = entry.getMultiAttr(key);
        EphemeralKeyValuePair[] parsed = new EphemeralKeyValuePair[values.length];
        for (int i = 0; i < values.length; i++) {
            parsed[i] = attributeEncoder.decode(key, values[i]);
        }
        return new EphemeralResult(parsed);
    }

    @Override
    public void set(EphemeralInput input, EphemeralLocation location)
            throws ServiceException {
        Entry entry = ldapLocationToEntry(location);
        Pair<String, String> ldapData = attributeEncoder.encode(input, location);
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(ldapData.getFirst(), ldapData.getSecond());
        Provisioning.getInstance().modifyAttrs(entry, attrs);
    }

    @Override
    public void update(EphemeralInput input, EphemeralLocation location)
            throws ServiceException {
        Entry entry = ldapLocationToEntry(location);
        Pair<String, String> ldapData = attributeEncoder.encode(input, location);
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + ldapData.getFirst(), ldapData.getSecond());
        Provisioning.getInstance().modifyAttrs(entry, attrs);

    }

    @Override
    public void delete(String key, EphemeralLocation location)
            throws ServiceException {
        Entry entry = ldapLocationToEntry(location);
        String[] vals = entry.getMultiAttr(key);
        deleteInternal(entry, key, Arrays.asList(vals));
    }

    @Override
    public void deleteValue(String key, String valueToDelete, EphemeralLocation target)
            throws ServiceException {
        Entry entry = ldapLocationToEntry(target);
        // have to take into account that values may have expiration encoded in
        List<String> toDelete = new LinkedList<String>();
        for (String val: entry.getMultiAttr(key)) {
            String parsedValue = attributeEncoder.decode(key, val).getValue();
            if (parsedValue.equals(valueToDelete)) {
                toDelete.add(val);
            }
        }
        deleteInternal(entry, key, toDelete);
    }

    @Override
    public void purgeExpired(String key, EphemeralLocation location)
            throws ServiceException {
        Entry entry = ldapLocationToEntry(location);
        String[] values = entry.getMultiAttr(key);
        List<String> purged = new LinkedList<String>();
        for (String ldapValue: values) {
            EphemeralKeyValuePair parsed = attributeEncoder.decode(key, ldapValue);
            Long expiry  = parsed.getExpires();
            if (expiry != null && System.currentTimeMillis() > expiry) {
                String value = parsed.getValue();
                ZimbraLog.ephemeral.info("purging ephemeral value %s for key %s", key, value);
                purged.add(value);
            }
        }
        deleteInternal(entry, key, purged);
    }

    @Override
    public boolean hasKey(String keyName, EphemeralLocation location)
            throws ServiceException {
        Entry entry = ldapLocationToEntry(location);
        return !Strings.isNullOrEmpty(entry.getAttr(keyName));
    }

    private void deleteInternal(Entry entry, String key, List<String> values) throws ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        for (String val: values) {
            StringUtil.addToMultiMap(attrs, "-" + key, val);
        }
        Provisioning.getInstance().modifyAttrs(entry, attrs);
    }

    private Entry ldapLocationToEntry(EphemeralLocation location) throws ServiceException {
        if (!(location instanceof LdapEntryLocation)) {
            throw ServiceException.FAILURE("LdapEphemeralBackend can only be used with LdapEntryLocation", null);
        }
        return ((LdapEntryLocation) location).getEntry();
    }

    public static class Factory implements EphemeralStore.Factory {

        @Override
        public EphemeralStore getStore() {
            return new LdapEphemeralStore();
        }

        @Override
        public void startup() {}

        @Override
        public void shudown() {}
    }
}
