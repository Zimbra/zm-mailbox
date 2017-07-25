package com.zimbra.cs.ephemeral;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Ephemeral store implementation used while attribute migration is in progress.
 * Attribute modifications are applied to both the current and upcoming store.
 *
 * @author iraykin
 */
public class ForwardingEphemeralStore extends EphemeralStore {

    private EphemeralStore currentStore;
    private EphemeralStore futureStore;

    public ForwardingEphemeralStore(EphemeralStore primary, EphemeralStore secondary) {
        this.currentStore = primary;
        this.futureStore = secondary;
    }

    @Override
    public EphemeralResult get(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        //get from the current store
        return currentStore.get(key, location);
    }

    @Override
    public void set(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException {
        //set on both stores
        currentStore.set(attribute, location);
        try {
            futureStore.set(attribute, location);
        } catch (ServiceException e) {
            handleError(e);
        }
    }

    @Override
    public void update(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException {
        //update on both stores
        currentStore.update(attribute, location);
        try {
            futureStore.update(attribute, location);
        } catch(ServiceException e) {
            handleError(e);
        }
    }

    @Override
    public void delete(EphemeralKey key, String value,
            EphemeralLocation location) throws ServiceException {
        //delete from both stores
        currentStore.delete(key, value, location);
        try {
            futureStore.delete(key, value, location);
        } catch (ServiceException e) {
            handleError(e);
        }
    }

    @Override
    public boolean has(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        //only need to check for presence in current store
        return currentStore.has(key, location);
    }

    @Override
    public void purgeExpired(EphemeralKey key, EphemeralLocation location)
            throws ServiceException {
        //purge from both
        currentStore.purgeExpired(key, location);
        try {
            futureStore.purgeExpired(key, location);
        } catch (ServiceException e) {
            handleError(e);
        }
    }

    @Override
    public void deleteData(EphemeralLocation location) throws ServiceException {
        //delete from both
        currentStore.deleteData(location);
        try {
            futureStore.deleteData(location);
        } catch (ServiceException e) {
            handleError(e);
        }
    }

    private void handleError(ServiceException e) {
        //Don't allow failures on destination store to interrupt normal operation,
        //and only store in the logs if debug is enabled. Otherwise we risk flooding
        //the logs with identical error messages in the event that ForwardingEphemeralStore
        //is enabled for a long time and the forwarding store becomes unavailable
        ZimbraLog.ephemeral.debug("error accesssing forwarding ephemeral store", e);
    }

    public static class Factory extends EphemeralStore.Factory {

        private EphemeralStore.Factory currentFactory;
        private EphemeralStore.Factory futureFactory;

        public Factory(EphemeralStore.Factory primaryFactory, EphemeralStore.Factory fallbackFactory) {
            this.currentFactory = primaryFactory;
            this.futureFactory = fallbackFactory;
        }

        @Override
        public EphemeralStore getStore() {
            return new ForwardingEphemeralStore(currentFactory.getStore(), futureFactory.getStore());
        }

        @Override
        public void startup() {
            currentFactory.startup();
            futureFactory.startup();
        }

        @Override
        public void shutdown() {
            currentFactory.shutdown();
            futureFactory.shutdown();
        }

        @Override
        public void test(String url) throws ServiceException {
            currentFactory.test(url);
        }
    }
}
