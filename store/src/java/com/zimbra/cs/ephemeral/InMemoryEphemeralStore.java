package com.zimbra.cs.ephemeral;

import java.util.Collection;
import java.util.HashMap;
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
        setAttributeEncoder(new DynamicExpirationEncoder());
    }

    @Override
    public EphemeralResult get(EphemeralKey key, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        String encodedKey = encodeKey(key, target);
        Collection<String> values = map.get(encodedKey);
        DynamicResultsHelper helper = new DynamicResultsHelper(key, target, encoder, null);
        return helper.get(values);
    }

    @Override
    public void set(EphemeralInput attribute, EphemeralLocation target)
            throws ServiceException {
        String encodedKey = encodeKey(attribute, target);
        String encodedValue = encodeValue(attribute, target);
        Multimap<String, String> map = getSpecifiedMap(target);
        map.removeAll(encodedKey);
        map.put(encodedKey, encodedValue);
    }

    @Override
    public void update(EphemeralInput attribute, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        String encodedKey = encodeKey(attribute, target);
        String encodedValue = encodeValue(attribute, target);
        map.put(encodedKey, encodedValue);
    }

    @Override
    public void delete(EphemeralKey key, String valueToDelete, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        String encodedKey = encodeKey(key, target);
        Collection<String> values = map.get(encodedKey);
        DynamicResultsHelper helper = new DynamicResultsHelper(key, target, encoder, null);
        List<String> toDelete = helper.delete(values, valueToDelete);
        for (String v: toDelete) {
            map.remove(encodedKey, v);
        }
    }

    @Override
    public void purgeExpired(EphemeralKey key, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        String encodedKey = encodeKey(key, target);
        Collection<String> values = map.get(encodedKey);
        DynamicResultsHelper helper = new DynamicResultsHelper(key, target, encoder, null, true);
        List<String> purged = helper.purge(values);
        for (String v: purged) {
            map.remove(encodedKey, v);
        }
    }

    @Override
    public void deleteData(EphemeralLocation location) throws ServiceException {
        String[] locationHierarchy = location.getLocation();
        String storeKey = Joiner.on("|").join(locationHierarchy);;
        storeMap.remove(storeKey);
    }


    @Override
    public boolean has(EphemeralKey key, EphemeralLocation target)
            throws ServiceException {
        Multimap<String, String> map = getSpecifiedMap(target);
        String encodedKey = encodeKey(key, target);
        Collection<String> values = map.get(encodedKey);
        DynamicResultsHelper helper = new DynamicResultsHelper(key, target, encoder, null);
        return helper.has(values);
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

        @Override
        public void test(String url) throws ServiceException {}
    }
}
