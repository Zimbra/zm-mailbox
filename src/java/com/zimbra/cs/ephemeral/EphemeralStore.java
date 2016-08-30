package com.zimbra.cs.ephemeral;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;

/** Singleton representing the entry point into ephemeral storage.
 * Routes requests to the specified @EphemeralBackend.
 * @author iraykin
 *
 */
public class EphemeralStore {
    private static EphemeralStore singleton;
    private EphemeralBackend backend;

    private EphemeralStore(EphemeralBackend backend) {
        this.backend = backend;
    }

    public EphemeralResult get(String key, EphemeralLocation location)
            throws ServiceException {
        backend.open();
        try {
            return backend.get(key, location);
        } finally {
            backend.close();
        }
    }

    public void set(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException {
        backend.open();
        try {
            backend.set(attribute, location);
        } finally {
            backend.close();
        }
    }

    public void update(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException {
        backend.open();
        try {
            backend.update(attribute, location);
        } finally {
            backend.close();
        }
    }

    public void delete(String key, EphemeralLocation location) throws ServiceException {
        backend.open();
        try {
            backend.delete(key, location);
        } finally {
            backend.close();
        }
    }

    public void deleteValue(String key, String value, EphemeralLocation location)
            throws ServiceException {
        backend.open();
        try {
            backend.deleteValue(key, value, location);
        } finally {
            backend.close();
        }
    }

    public void purgeExpired(String key, EphemeralLocation location)
            throws ServiceException {
        backend.open();
        try {
            backend.purgeExpired(key, location);
        } finally {
            backend.close();
        }
    }

    public static EphemeralStore getInstance() throws ServiceException {
        if (singleton == null) {
            synchronized (EphemeralStore.class) {
                if (singleton == null) {
                    EphemeralBackend backend;
                    String className = LC.zimbra_class_ephemeral_store_backend.value();
                    if (className != null && !className.equals("")) {
                        Class<?> klass = null;
                        try {
                            try {
                                klass = Class.forName(className);
                            } catch (ClassNotFoundException cnfe) {
                                klass = ExtensionUtil.findClass(className);
                            }
                            backend = (EphemeralBackend) klass.newInstance();
                        } catch (Exception e) {
                            //TODO: use default backend
                            throw ServiceException.FAILURE("no EphemeralBackend specified", null);

                        }
                    } else {
                        //TODO: use default backend
                        throw ServiceException.FAILURE("no EphemeralBackend specified", null);
                    }
                    ZimbraLog.ephemeral.info("Using EphemeralBackend %s", backend.getClass().getCanonicalName());
                    singleton = new EphemeralStore(backend);
                }
            }
        }
        return singleton;
    }

    /**
     * This abstract class represents a hierarchical specification of the
     * location of ephemeral attributes.
     *
     * The only method, getLocation(), returns an array of Strings representing
     * a hierarchy under which the key/value pair is stored. It is up to
     * the @EphemeralBackend implementation to decide how to use this,
     * be it accessing the appropriate database, constructing a key hierarchy,
     * or something else.
     *
     * @author iraykin
     *
     */
    public static abstract class Location {
        public abstract String[] getLocation();
    }
}