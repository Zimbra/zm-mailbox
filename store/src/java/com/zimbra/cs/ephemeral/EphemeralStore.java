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
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo.Status;
import com.zimbra.cs.ephemeral.migrate.ZimbraMigrationInfo;
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

    /**
     * Delete all ephemeral data for the specified EphemeralLocation
     *
     * @param location
     * @throws ServiceException
     */
    public abstract void deleteData(EphemeralLocation location)
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
        String className = factoryClass.getDeclaringClass().getSimpleName();
        try {
            boolean useForwarding = false;
            Factory fac = factoryClass.newInstance();
            try {
                MigrationInfo info = AttributeMigration.getMigrationInfo();
                Status curStatus = info.getStatus();
                if (!Strings.isNullOrEmpty(info.getURL()) && (curStatus == Status.COMPLETED || curStatus == Status.IN_PROGRESS)) {
                    useForwarding = true;
                }

            } catch (ServiceException e) {
                ZimbraLog.ephemeral.warn("unable to retrieve migration info", e);
            }
            if (useForwarding) {
                try {
                    Factory forwardingFactory = getNewFactory(BackendType.migration);
                    factory = new ForwardingEphemeralStore.Factory(fac, forwardingFactory);
                    ZimbraLog.ephemeral.debug("using forwarding ephemeral store; %s forwarding to %s", className, forwardingFactory.getClass().getName());
                } catch (ServiceException e) {
                    ZimbraLog.ephemeral.warn("unable to initialize forwarding migration factory, using %s", className, e);
                    factory = fac;
                }
            } else {
                factory = fac;
                ZimbraLog.ephemeral.debug("using ephemeral store factory %s", className);
            }
            factory.startup();
        } catch (InstantiationException | IllegalAccessException e) {
            handleFailure(onFailure, String.format("unable to initialize EphemeralStore factory %s", className), e);
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

    private static String getFactoryClassName(String backendName, boolean disableAutoload) throws ServiceException {
        String factoryClassName = factories.get(backendName);
        if (factoryClassName == null && !disableAutoload) {
            //perhaps the extension hasn't been loaded - try to find it and check again
            Level savedEphemLogLevel = ZimbraLog.ephemeral.getLevel();
            Level savedExtenLogLevel = ZimbraLog.extensions.getLevel();
            // cut down on noise unless enabled debug; this is called from CLI utilities so messages go to the console
            if (!ZimbraLog.ephemeral.isDebugEnabled()) {
                ZimbraLog.extensions.setLevel(Level.error);
                ZimbraLog.ephemeral.setLevel(Level.error);
            }
            try {
                ExtensionUtil.initEphemeralBackendExtension(backendName);
            } finally {
                ZimbraLog.extensions.setLevel(savedExtenLogLevel);
                ZimbraLog.ephemeral.setLevel(savedEphemLogLevel);
            }
            factoryClassName = factories.get(backendName);
        }
        return factoryClassName;
    }

    public static Factory getNewFactory(BackendType backendType) throws ServiceException {
        String factoryClassName = null;
        String url = Factory.getBackendURL(backendType);
        if (url != null) {
            String[] tokens = url.split(":");
            if (tokens != null && tokens.length > 0) {
                factoryClassName = getFactoryClassName(tokens[0], false);
            }
        } else {
            throw ServiceException.FAILURE(String.format("no ephemeral backend URL specified for backend type '%s'", backendType.toString()), null);
        }
        if (factoryClassName == null) {
            throw ServiceException.FAILURE(String.format("no EphemeralStore.Factory class specified for backend type '%s'", backendType.toString()), null);
        }
        Class<? extends Factory> factoryClass = null;
        try {
            factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
        } catch (ClassNotFoundException cnfe) {
            try {
                factoryClass = ExtensionUtil.findClass(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException cnfe2) {
                throw ServiceException.FAILURE(String.format("Unable to find EphemeralStore factory %s", factoryClassName), cnfe2);
            }
        }
        try {
            Factory newFactory = factoryClass.newInstance();
            newFactory.setBackendType(backendType);
            newFactory.startup();
            return newFactory;
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE(String.format("unable to initialize EphemeralsStore factory %s", factoryClass.getDeclaringClass().getSimpleName()), e);
        }
    }

    public static Factory getFactory(FailureMode onFailure) throws ServiceException {
        if (factory == null) {
            String factoryClass = null;
            String url = Provisioning.getInstance().getConfig().getEphemeralBackendURL();
            if (url != null) {
                String[] tokens = url.split(":");
                if (tokens != null && tokens.length > 0) {
                    factoryClass = getFactoryClassName(tokens[0], false);
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
        return getFactory(backendName, false);
    }

    public static Factory getFactory(String backendName, boolean disableAutoload) {
        String factoryClassName;
        try {
            factoryClassName = getFactoryClassName(backendName, disableAutoload);
        } catch (ServiceException e) {
            //this method shouldn't throw an exception
            return null;
        }
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

    public static abstract class Factory {

        private static String getBackendURL(BackendType type) throws ServiceException {
            Config config = Provisioning.getInstance().getConfig();
            String url;
            switch (type) {
            case previous:
                url = config.getPreviousEphemeralBackendURL();
                break;
            case migration:
                url = new ZimbraMigrationInfo().getURL();
                break;
            case primary:
            default:
                url = config.getEphemeralBackendURL();
                break;
            }
            return url;
        }

        private BackendType backendType = BackendType.primary;

        public void setBackendType(BackendType type) {
            backendType = type;
        }

        public BackendType getBackendType() {
            return backendType;
        }

        protected String getURL() throws ServiceException {
            return getBackendURL(backendType);
        }

        public EphemeralStore getNewStore() throws ServiceException {
            throw ServiceException.UNSUPPORTED();
        }
        public abstract EphemeralStore getStore();
        public abstract void startup();
        public abstract void shutdown();

        /**
         * Validate the given URL for the EphemeralStore implementation.
         * This method should throw an ServiceException if the backend cannot function with this URL.
         *
         * @param url
         * @throws ServiceException
         */
        public abstract void test(String url) throws ServiceException;
    }

    public static enum FailureMode {
        halt, safe;
    }

    public static enum BackendType {
        primary, previous, migration;
    }
}
