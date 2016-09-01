package com.zimbra.cs.ephemeral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;

public class InMemoryEphemeralStore extends EphemeralStore {

    private Map<String, Map<String, Object>> storeMap;

    public InMemoryEphemeralStore() {
        storeMap = new HashMap<String, Map<String, Object>>();
        setAttributeEncoder(new ExpirationEncoder());
    }

    @Override
    public EphemeralResult get(String key, EphemeralLocation target)
            throws ServiceException {
        Map<String, Object> map = getSpecifiedMap(target);
        Object value = map.get(key);
        if (value == null) {
            return EphemeralResult.emptyResult(key);
        } else if (value instanceof String) {
            EphemeralKeyValuePair entry = getAttributeEncoder().decode(key, (String) value);
            return new EphemeralResult(entry);
        } else if (value instanceof String[]) {
            String[] values = (String[]) value;
            EphemeralKeyValuePair[] entries = new EphemeralKeyValuePair[values.length];
            for (int i = 0; i < values.length; i++) {
                entries[i] = getAttributeEncoder().decode(key, values[i]);
            }
            return new EphemeralResult(entries);
        } else {
            throw ServiceException.FAILURE("map values can only be of type String or String[]", null);
        }
    }

    @Override
    public void set(EphemeralInput attribute, EphemeralLocation target)
            throws ServiceException {
        String key = attribute.getKey();
        String value = getAttributeEncoder().encode(attribute, target).getSecond();
        Map<String, Object> map = getSpecifiedMap(target);
        map.remove(key);
        StringUtil.addToMultiMap(map, key, value);
    }

    @Override
    public void update(EphemeralInput attribute, EphemeralLocation target)
            throws ServiceException {
        Map<String, Object> map = getSpecifiedMap(target);
        String key = attribute.getKey();
        String value = getAttributeEncoder().encode(attribute, target).getSecond();
        StringUtil.addToMultiMap(map, key, value);
    }

    @Override
    public void delete(String key, EphemeralLocation target)
            throws ServiceException {
        Map<String, Object> map = getSpecifiedMap(target);
        map.remove(key);
    }

    @Override
    public void deleteValue(String key, String valueToDelete, EphemeralLocation target)
            throws ServiceException {
        Map<String, Object> map = getSpecifiedMap(target);
        Object value = map.get(key);
        if (value == null) {
            return;
        }
        String[] values;
        if (value instanceof String) {
            values = new String[]{(String) value};
        } else if (value instanceof String[]) {
            values = (String[]) value;
        } else {
            throw ServiceException.FAILURE("map values can only be of type String or String[]", null);
        }
        List<String> valuesAfterRemoval = new ArrayList<String>();
        for (String v: values) {
            if (!getAttributeEncoder().decode(key, v).getValue().equals(valueToDelete)) {
                valuesAfterRemoval.add(v);
            }
        }
        if (valuesAfterRemoval.isEmpty()) {
            map.remove(key);
        } else if (valuesAfterRemoval.size() == 1) {
            map.put(key, valuesAfterRemoval.get(0));
        } else {
            map.put(key, valuesAfterRemoval.toArray(new String[valuesAfterRemoval.size()]));
        }
    }

    @Override
    public void purgeExpired(String key, EphemeralLocation target)
            throws ServiceException {
        Map<String, Object> map = getSpecifiedMap(target);
        Object value = map.get(key);
        if (value == null) { return; }
        String[] values;
        if (value instanceof String) {
            values = new String[] {(String) value};
        } else if (value instanceof String[]) {
            values = (String[]) value;
        } else {
            throw ServiceException.FAILURE("map values can only be of type String or String[]", null);
        }
        List<String> purged = new LinkedList<String>();
        List<String> remaining = new LinkedList<String>();
        for (String v: values) {
             EphemeralKeyValuePair entry = getAttributeEncoder().decode(key, v);
             if (entry.getExpires() < System.currentTimeMillis()) {
                 purged.add(entry.getValue());
             } else {
                 remaining.add(v);
             }
        }
        if (purged.isEmpty()) {
            return;
        } else if (remaining.isEmpty()) {
            map.remove(key);
        } else if (remaining.size() == 1) {
            map.put(key, remaining.get(0));
        } else {
            map.put(key, remaining.toArray(new String[remaining.size()]));
        }
    }

    @Override
    public boolean hasKey(String keyName, EphemeralLocation target)
            throws ServiceException {
        Map<String, Object> map = getSpecifiedMap(target);
        return map.containsKey(keyName);
    }

    private Map<String, Object> getSpecifiedMap(EphemeralLocation target) {
        String[] locationHierarchy = target.getLocation();
        String storeKey = Joiner.on("|").join(locationHierarchy);
        Map<String, Object> store = storeMap.get(storeKey);
        if (store == null) {
            store = new HashMap<String, Object>();
            storeMap.put(storeKey, store);
        }
        return store;
    }

    public static class Factory implements EphemeralStore.Factory {

        private static InMemoryEphemeralStore instance;

        @Override
        public EphemeralStore getStore() {
            synchronized (Factory.class) {
                if (instance == null) {
                    instance = new InMemoryEphemeralStore();
                }
                return instance;
            }
        }

        @Override
        public void startup() {}

        @Override
        public void shudown() {
            instance = null;
        }
    }
}
