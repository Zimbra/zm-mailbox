package com.zimbra.cs.ephemeral;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;

public class FallbackEphemeralStore extends EphemeralStore {

    private EphemeralStore primary;
    private EphemeralStore secondary;

    public FallbackEphemeralStore(EphemeralStore primary, EphemeralStore secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public EphemeralResult get(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        EphemeralResult result = primary.get(key, location);
        if (result == null || result.isEmpty()) {
            return secondary.get(key, location);
        } else {
            return result;
        }
    }

    @Override
    public void set(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException {
        primary.set(attribute, location);
    }

    @Override
    public void update(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException {
        primary.update(attribute, location);
    }

    @Override
    public void delete(EphemeralKey key, String value,
            EphemeralLocation location) throws ServiceException {
        primary.delete(key, value, location);
        secondary.delete(key, value, location);
    }

    @Override
    public boolean has(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        return primary.has(key, location) || secondary.has(key, location);
    }

    @Override
    public void purgeExpired(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        primary.purgeExpired(key, location);
        secondary.purgeExpired(key, location);
    }

    @Override
    public void deleteData(EphemeralLocation location) throws ServiceException {
        primary.deleteData(location);
        secondary.deleteData(location);
    }

    @VisibleForTesting
    public EphemeralStore getPrimaryStore() {
        return primary;
    }

    @VisibleForTesting
    public EphemeralStore getSecondaryStore() {
        return secondary;
    }

    public static class Factory implements EphemeralStore.Factory {

        private EphemeralStore.Factory primaryFactory;
        private EphemeralStore.Factory fallbackFactory;

        public Factory(EphemeralStore.Factory primaryFactory, EphemeralStore.Factory fallbackFactory) {
            this.primaryFactory = primaryFactory;
            this.fallbackFactory = fallbackFactory;
        }

        @Override
        public EphemeralStore getStore() {
            return new FallbackEphemeralStore(primaryFactory.getStore(), fallbackFactory.getStore());
        }

        @Override
        public void startup() {
            primaryFactory.startup();
            fallbackFactory.startup();
        }

        @Override
        public void shutdown() {
            primaryFactory.shutdown();
            fallbackFactory.shutdown();
        }

        @Override
        public void test(String url) throws ServiceException {
            primaryFactory.test(url);
        }
    }
}
