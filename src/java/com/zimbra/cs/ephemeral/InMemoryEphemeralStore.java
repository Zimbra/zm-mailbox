package com.zimbra.cs.ephemeral;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;

public class InMemoryEphemeralStore extends EphemeralStore {

    private Map<String, Multimap<String, String>> storeMap;

    public InMemoryEphemeralStore() {
        storeMap = new HashMap<String, Multimap<String, String>>();
        setAttributeEncoder(new ExpirationEncoder());
    }

    @Override
    public EphemeralResult get(String key, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        Collection<String> values = map.get(key);
        if (values.isEmpty()) {
            return EphemeralResult.emptyResult(key);
        } else {
            EphemeralKeyValuePair[] entries = new EphemeralKeyValuePair[values.size()];
            int i = 0;
            for (String v: values) {
                entries[i] = getAttributeEncoder().decode(key, v);
                i++;
            }
            return new EphemeralResult(entries);
        }
    }

    @Override
    public void set(EphemeralInput attribute, EphemeralLocation target)
            throws ServiceException {
        String key = attribute.getKey();
        String value = getAttributeEncoder().encode(attribute, target).getSecond();
        Multimap<String, String> map = getSpecifiedMap(target);
        map.removeAll(key);
        map.put(key, value);
    }

    @Override
    public void update(EphemeralInput attribute, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        String key = attribute.getKey();
        String value = getAttributeEncoder().encode(attribute, target).getSecond();
        map.put(key, value);
    }

    @Override
    public void delete(String key, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        map.removeAll(key);
    }

    @Override
    public void deleteValue(String key, String valueToDelete, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        Collection<String> values = map.get(key);
        List<String> toDelete = new LinkedList<String>();
        for (String v: values) {
            if (getAttributeEncoder().decode(key, v).getValue().equals(valueToDelete)) {
                toDelete.add(v);
            }
        }
        for (String v: toDelete) {
            map.remove(key, v);
        }
    }

    @Override
    public void purgeExpired(String key, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        Collection<String> values = map.get(key);
        List<String> toDelete = new LinkedList<String>();
        for (String v: values) {
             EphemeralKeyValuePair entry = getAttributeEncoder().decode(key, v);
             if (entry.getExpires() < System.currentTimeMillis()) {
                 toDelete.add(v);
             }
        }
        for (String v: toDelete) {
            map.remove(key, v);
        }
    }

    @Override
    public boolean hasKey(String keyName, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        return map.containsKey(keyName);
    }

    private Multimap<String, String> getSpecifiedMap(EphemeralLocation target) {
        String[] locationHierarchy = target.getLocation();
        String storeKey = Joiner.on("|").join(locationHierarchy);
        Multimap<String, String> store = storeMap.get(storeKey);
        if (store == null) {
            store = ArrayListMultimap.create();
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
        public void shutdown() {
            instance = null;
        }
    }
}
