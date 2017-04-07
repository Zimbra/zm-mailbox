package com.zimbra.cs.ephemeral;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationFlag;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.cs.util.Zimbra;

/**
 * Abstract class representing ephemeral storage.
 *
 * @author iraykin
 *
 */
public abstract class EphemeralStore {

    /** Use this interface to identify ZimbraExtensions that provide EphemeralStores */
    public static interface Extension {
        String getStoreId();
    }

    private static Map<String, String> factories = new HashMap<String, String>();
    private static Factory factory;
    protected AttributeEncoder encoder;
    static {
        factories.put("ldap", LdapEphemeralStore.Factory.class.getName());
    }

    /**
     * Get the value for a key. If key does not exist, returns an empty
     * EphemeralResult instance.
     *
     * @param key
     * @param location
     * @return
     * @throws ServiceException
     */
    public abstract EphemeralResult get(EphemeralKey key, EphemeralLocation location)
            throws ServiceException;

    /**
     * Set a value for a key if the key does not exist, or overwrites
     * otherwise. If the key is multi-valued, all original values are deleted.
     *
     * @param input
     * @param location
     * @throws ServiceException
     */
    public abstract void set(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException;

    /**
     * Set a value for a key if the key does not exist, or add another value
     * otherwise. If this value already exists, the expiration is updated if one
     * is provided.
     *
     * @param input
     * @param location
     * @throws ServiceException
     */
    public abstract void update(EphemeralInput attribute, EphemeralLocation location)
            throws ServiceException;

    /**
     * Delete specified value for a key. If the value does not exist, do
     * nothing.
     *
     * @param key
     * @param value
     * @param location
     * @throws ServiceException
     */
    public abstract void delete(EphemeralKey key, String value, EphemeralLocation location)
            throws ServiceException;


    /**
     * Check whether the specified key exists in the target location.
     *
     * @param key
     * @param value
     * @param location
     * @return
     * @throws ServiceException
     */
    public abstract boolean has(EphemeralKey key, EphemeralLocation location)
            throws ServiceException;

    /**
     * Delete keys that have passed their expiration. If the backend natively
     * supports key expiry, this may do nothing.
     *
     *
     * @param key
     * @param location
     * @throws ServiceException
     */
    public abstract void purgeExpired(EphemeralKey key, EphemeralLocation location)
            throws ServiceException;

    public static void registerFactory(String prefix, String klass) {
        if (factories.containsKey(prefix)) {
            ZimbraLog.ephemeral.warn("Replacing ephemeral factory class '%s' registered for '%s' with '%s'",
                    factories.get(prefix), prefix, klass);
        } else {
            ZimbraLog.ephemeral.info("Registering ephemeral factory class '%s' for prefix '%s'", klass, prefix);
        }
        factories.put(prefix,  klass);
    }

    private static void handleFailure(FailureMode onFailure, String message, Throwable t) {
        switch(onFailure) {
        case halt:
            if (t == null) {
                Zimbra.halt(message);
            } else {
                Zimbra.halt(message, t);
            }
            break;
        case safe:
            if (t == null) {
                ZimbraLog.ephemeral.debug(message);
            } else {
                ZimbraLog.ephemeral.debug(message, t);
            }
            break;
        }
    }

    private static final void setFactory(String factoryClassName, FailureMode onFailure) {
        if (factoryClassName == null) {
            handleFailure(onFailure, "no EphemeralStore specified", null);
            return;
        }
        Class<? extends Factory> factoryClass = null;
        try {
            factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
        } catch (ClassNotFoundException cnfe) {
            try {
                factoryClass = ExtensionUtil.findClass(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException cnfe2) {
                handleFailure(onFailure, String.format("Unable to find EphemeralStore factory %s", factoryClassName), cnfe2);
                return;
            }
        }
        setFactory(factoryClass, onFailure);
    }

    public static final void clearFactory() {
        if (factory != null) {
            factory.shutdown();
        }
        factory = null;
    }

    @VisibleForTesting
    public static final void setFactory(Class<? extends Factory> factoryClass) {
        setFactory(factoryClass, FailureMode.halt);
    }

    public static final void setFactory(Class<? extends Factory> factoryClass, FailureMode onFailure) {
        try {
            boolean useFallback = false;
            Factory fac = factoryClass.newInstance();
            if (!(fac instanceof LdapEphemeralStore.Factory)) {
                EphemeralStore store = fac.getStore();
                MigrationFlag flag = AttributeMigration.getMigrationFlag(store);
                try {
                    useFallback = flag.isSet();
                } catch (ServiceException e) {
                    ZimbraLog.ephemeral.warn("unable to determine if migration is in progress", e);
                }
            }
            if (useFallback) {
                factory = new FallbackEphemeralStore.Factory(fac, AttributeMigration.getFallbackFactory());
            } else {
                factory = fac;
            }
            factory.startup();
            ZimbraLog.ephemeral.debug("using ephemeral store factory %s", factoryClass.getDeclaringClass().getSimpleName());
        } catch (InstantiationException | IllegalAccessException e) {
            handleFailure(onFailure, String.format("unable to initialize EphemeralsStore factory %s", factoryClass.getDeclaringClass().getSimpleName()), e);
        }
    }

    public void setAttributeEncoder(AttributeEncoder encoder) {
        this.encoder = encoder;
    }

    protected String encodeKey(EphemeralInput input, EphemeralLocation target) {
        return encoder.encodeKey(input.getEphemeralKey(), target);
    }

    protected String encodeKey(EphemeralKey key, EphemeralLocation target) {
        return encoder.encodeKey(key, target);
    }

    protected String encodeValue(EphemeralInput input, EphemeralLocation target) {
        return encoder.encodeValue(input, target);
    }

    protected EphemeralKeyValuePair decode(String key, String value) throws ServiceException {
        return encoder.decode(key, value);
    }

    public static Factory getFactory(FailureMode onFailure) throws ServiceException {
        if (factory == null) {
            String factoryClass = null;
            String url = Provisioning.getInstance().getConfig().getEphemeralBackendURL();
            if (url != null) {
                String[] tokens = url.split(":");
                if (tokens != null && tokens.length > 0) {
                    factoryClass = factories.get(tokens[0]);
                }
            } else {
                factoryClass = factories.get("ldap");
            }
            setFactory(factoryClass, onFailure);
        }
        return factory;
    }

    public static Factory getFactory() throws ServiceException {
        return getFactory(FailureMode.halt);
    }

    public static Factory getFactory(String backendName) {
        String factoryClassName = factories.get(backendName);
        if (factoryClassName == null) {
            return null;
        }
        Class<? extends Factory> factoryClass = null;
        try {
            factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
        } catch (ClassNotFoundException cnfe) {
            try {
                factoryClass = ExtensionUtil.findClass(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException cnfe2) {
                return null;
            }
        }
        try {
            Factory factory = factoryClass.newInstance();
            factory.startup();
            return factory;
        } catch (InstantiationException | IllegalAccessException e) {
            ZimbraLog.ephemeral.error("unable to instantiate factory %s",factoryClassName, e);
            return null;
        }
    }

    private static Options OPTIONS = new Options();
    static {
        OPTIONS.addOption("u", "test-url", true, "test whether can connect to this URL.  Exit non-zero if cannot.");
        OPTIONS.addOption("d", "debug", false, "Enable debug logging");
        OPTIONS.addOption("h", "help", false, "Display this help message");
    }

    private static void usage() {
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zmjava com.zimbra.cs.ephemeral.EphemeralStore [options]", null, OPTIONS, 2, 2, null);
            System.exit(0);
    }

    public static class EphemeralStoreMatcher implements ExtensionUtil.ExtensionMatcher {
        private final String storeId;
        public EphemeralStoreMatcher(String storeId) {
            this.storeId = storeId;
        }
        @Override
        public boolean matches(ZimbraExtension ext) {
            if (ext instanceof EphemeralStore.Extension) {
                return storeId.equals(((EphemeralStore.Extension) ext).getStoreId());
            }
            return false;
        }
    }

    public static boolean canConnectToURL(String url) {
        String[] tokens = url.split(":");
        if (tokens == null || tokens.length <= 0) {
            ZimbraLog.ephemeral.error("'%s' is an invalid URL for %s", url, Provisioning.A_zimbraEphemeralBackendURL);
            return false;
        }
        String backend = tokens[0];
        if (backend.equalsIgnoreCase("ldap")) {
            return true;
        }
        ExtensionUtil.initAllMatching(new EphemeralStoreMatcher(backend));
        Factory theFactory = EphemeralStore.getFactory(backend);
        if (theFactory == null) {
            ZimbraLog.ephemeral.error("no factory found for backend for URL '%s'", url);
            return false;
        }
        try {
            theFactory.test(url);
        } catch (ServiceException e) {
            ZimbraLog.ephemeral.error("cannot set '%s' to '%s' (%s)", Provisioning.A_zimbraEphemeralBackendURL,
                    url, e.getMessage());
            return false;
        }
        ZimbraLog.ephemeral.debug("Successfully connected to URL '%s'.  Valid value for '%s'",
            url, Provisioning.A_zimbraEphemeralBackendURL);
        return true;
    }


    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(OPTIONS, args);
        if (cl.hasOption('h') || !cl.hasOption('u')) {
            usage();
            return;
        }
        if (cl.hasOption('d')) {
            ZimbraLog.ephemeral.setLevel(Level.debug);
        } else {
            // suppress noise
            ZimbraLog.ephemeral.setLevel(Level.error);
            ZimbraLog.extensions.setLevel(Level.error);
        }
        String url = cl.getOptionValue('u');
        if (canConnectToURL(url)) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    public static interface Factory {

        EphemeralStore getStore();
        void startup();
        void shutdown();

        /**
         * Validate the given URL for the EphemeralStore implementation.
         * This method should throw an ServiceException if the backend cannot function with this URL.
         *
         * @param url
         * @throws ServiceException
         */
        void test(String url) throws ServiceException;
    }

    public static enum FailureMode {
        halt, safe;
    }
}
