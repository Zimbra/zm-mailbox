package com.zimbra.cs.ephemeral;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
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

    public boolean hasKey(String key, EphemeralLocation location) throws ServiceException {
        backend.open();
        try {
            return backend.hasKey(key, location);
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
                    String className = Provisioning.getInstance().getConfig().getEphemeralStorageClassName();
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
}
