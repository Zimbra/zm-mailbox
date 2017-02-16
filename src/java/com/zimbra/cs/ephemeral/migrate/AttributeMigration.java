package com.zimbra.cs.ephemeral.migrate;

import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.FallbackEphemeralStore;
import com.zimbra.cs.ephemeral.LdapEntryLocation;
import com.zimbra.cs.ephemeral.LdapEphemeralStore;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.soap.admin.type.CacheEntryType;

/**
 * Class handling migrating attributes to ephemeral storage
 *
 * @author iraykin
 *
 */
public class AttributeMigration {
    private EntrySource source;
    private final Collection<String> attrsToMigrate;
    private boolean deleteOriginal = true;
    private boolean async = true;
    private MigrationCallback callback;
    private static ExecutorService executor = null;
    private static ServiceException exceptionWhileMigrating = null;
    private static MigrationHelper migrationHelper;
    static {
        setMigrationHelper(new ZimbraMigrationHelper());
    }

    //map of all available converters
    private static Map<String, AttributeConverter> converterMap = new HashMap<String, AttributeConverter>();
    static {
        registerConverter(Provisioning.A_zimbraAuthTokens, new AuthTokenConverter());
        registerConverter(Provisioning.A_zimbraCsrfTokenData, new CsrfTokenConverter());
        registerConverter(Provisioning.A_zimbraLastLogonTimestamp, new StringAttributeConverter());
    }

    //map of converters to be used in a particular migration
    private Map<String, AttributeConverter> activeConverterMap;

    public AttributeMigration(Collection<String> attrsToMigrate, Integer numThreads) throws ServiceException {
        this(attrsToMigrate, null, null, numThreads);
    }

    /**
     * @param attrsToMigrate - collection of attribute names to migrate
     * @param source - EntrySource implementation
     * @param callback - MigrationCallback implementation that handles generated EphemeralInputs
     * @param numThreads - number of threads to use during migration. If null, the migration happens synchronously.
     * @throws ServiceException
     */
    public AttributeMigration(Collection<String> attrsToMigrate, EntrySource source,  MigrationCallback callback, Integer numThreads) throws ServiceException {
        this.attrsToMigrate = attrsToMigrate;
        initConverters();
        setSource(source);
        setCallback(callback);
        if (numThreads != null) {
            executor = newFixedThreadPool(numThreads, new ThreadFactoryBuilder().setNameFormat("MigrateEphemeralAttrs-%d").setDaemon(false).build());
        } else {
            async = false;
        }
    }

    public static void registerConverter(String attribute, AttributeConverter converter) {
        ZimbraLog.ephemeral.debug("registering converter '%s' for attribute '%s'",
                converter.getClass().getName(), attribute);
        converterMap.put(attribute, converter);
    }

    public void setSource(EntrySource source) {
        this.source = source;
    }

    public void setCallback(MigrationCallback callback) {
        this.callback = callback;
    }

    public void setDeleteOriginal(boolean deleteOriginal) {
        this.deleteOriginal = deleteOriginal;
    }

    private void initConverters() throws ServiceException {
        activeConverterMap = new HashMap<String, AttributeConverter>();
        for (String attr: attrsToMigrate) {
            AttributeConverter converter = converterMap.get(attr);
            if (converter == null) {
                throw ServiceException.FAILURE(
                        String.format("no AttributeConverter registered for attribute %s; migration not possible", attr), null);
            } else {
                activeConverterMap.put(attr,  converter);
            }
        }
    }

    private void migrateAccount(Account account) throws ServiceException {
        MigrationTask migration = new MigrationTask(account, activeConverterMap, callback, deleteOriginal);
        migration.migrateAttributes();
    }

    private void migrateAccountAsync(Account account) throws ServiceException {
        MigrationTask migration = new MigrationTask(account, activeConverterMap, callback, deleteOriginal);
        executor.submit(migration);
    }

    public static void setMigrationHelper(MigrationHelper helper) {
        migrationHelper = helper;
    }

    public static MigrationFlag getMigrationFlag(EphemeralStore store) {
        return migrationHelper.getMigrationFlag(store);
    }

    public static EphemeralStore.Factory getFallbackFactory() {
        return migrationHelper.getFallbackFactory();
    }

    @VisibleForTesting
    public void beginMigration() throws ServiceException {
        ZimbraLog.ephemeral.info("beginning migration of attributes %s to ephemeral storage for all accounts",
                Joiner.on(", ").join(attrsToMigrate));
        EphemeralStore store = callback.getStore();
        if (store != null) {
            getMigrationFlag(store).set();
            EphemeralStore.clearFactory();
            migrationHelper.flushCache();
        }
    }

    @VisibleForTesting
    public void endMigration() throws ServiceException {
        EphemeralStore store = callback.getStore();
        if (store != null) {
            getMigrationFlag(store).unset();
            EphemeralStore.clearFactory();
            migrationHelper.flushCache();
        }
    }

    public void migrateAllAccounts() throws ServiceException {
        ZimbraLog.ephemeral.info("beginning migration of attributes %s to ephemeral storage",
                Joiner.on(", ").join(attrsToMigrate));
        beginMigration();
        if (async) {
            for (NamedEntry entry: source.getEntries()) {
                try {
                    migrateAccountAsync((Account) entry);
                } catch (RejectedExecutionException e) {
                    //executor has been shut down due to error in migration process
                    break;
                }
            }
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {}
            if (exceptionWhileMigrating != null) {
                throw exceptionWhileMigrating;
            }
        } else {
            for (NamedEntry entry: source.getEntries()) {
                migrateAccount((Account) entry);
            }
        }
        ZimbraLog.ephemeral.info("migration of attributes %s to ephemeral storage completed",
                Joiner.on(", ").join(attrsToMigrate));
        endMigration();
    }

    static interface EntrySource {
        public abstract List<NamedEntry> getEntries() throws ServiceException;
    }

    static class AllAccountsSource implements EntrySource {

        @Override
        public List<NamedEntry> getEntries() throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            SearchDirectoryOptions options = new SearchDirectoryOptions();
            ZLdapFilter filter = ZLdapFilterFactory.getInstance().allAccounts();
            options.setFilter(filter);
            options.setTypes(ObjectType.accounts);
            List<NamedEntry> accounts = prov.searchDirectory(options);
            return accounts;
        }
    }

    static class SomeAccountsSource implements EntrySource {
        private final List<String> accounts = new ArrayList<String>();

        public SomeAccountsSource(String[] acctValues) {
            accounts.addAll(Arrays.asList(acctValues));
        }

        @Override
        public List<NamedEntry> getEntries() throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            List<NamedEntry> entries = new ArrayList<NamedEntry>();
            for (String acctValue: accounts) {
                Account acct = prov.getAccount(acctValue);
                if (acct == null) {
                    ZimbraLog.ephemeral.error("no such account: %s", acctValue);
                    continue;
                } else {
                    entries.add(acct);
                }
            }
            return entries;
        }
    }

    /**
     * Callbacks to be invoked on EphemeralInputs generated by AttributeConverter.convert() calls
     */
    static interface MigrationCallback {
        /**
         * handle EphemeralInput instances generated from LDAP attributes
         */
        public abstract void setEphemeralData(EphemeralInput input, EphemeralLocation location, String origName, Object origValue) throws ServiceException;

        /**
         * Delete original attribute values
         */
        public abstract void deleteOriginal(Entry entry, String attrName, Object value, AttributeConverter converter) throws ServiceException;

        public abstract EphemeralStore getStore();
    }

    /**
     * Callback that stores the EphemeralInput in the EphemeralStore
     */
    static class ZimbraMigrationCallback implements MigrationCallback {
        private final EphemeralStore store;
        private final boolean destinationIsLdap;
        private LdapProvisioning prov = null;

        public ZimbraMigrationCallback() throws ServiceException {
            EphemeralStore.Factory factory = EphemeralStore.getFactory();
            if (factory instanceof LdapEphemeralStore.Factory) {
                throw ServiceException.FAILURE("migration to LdapEphemeralStore is not supported", null);
            }
            String url = Provisioning.getInstance().getConfig().getEphemeralBackendURL();
            factory.test(url);
            this.store = factory.getStore();
            this.destinationIsLdap = (store instanceof LdapEphemeralStore);
            Provisioning myProv = Provisioning.getInstance();
            if (myProv instanceof LdapProvisioning) {
                this.prov = (LdapProvisioning) myProv;
            } else {
                ZimbraLog.ephemeral.warn(
                    "LdapProvisioning required to delete attributes from LDAP after migration to ephemeral storage;"
                    + "'%s' provided. Old values will not be removed", myProv.getClass().getName());
            }

        }

        @Override
        public void setEphemeralData(EphemeralInput input, EphemeralLocation location, String origName, Object origValue) throws ServiceException {
            ZimbraLog.ephemeral.debug("migrating '%s' value '%s'", origName, String.valueOf(origValue));
            store.update(input, location);
        }

        @Override
        public void deleteOriginal(Entry entry, String attrName, Object value, AttributeConverter converter)
                throws ServiceException {
            // don't delete LDAP value if we are using LdapEphemeralStore and the attribute is single-valued,
            // since the value was already overwritten in the setEphemeralData step
            if (!(destinationIsLdap && !converter.isMultivalued())) {
                ZimbraLog.ephemeral.debug("deleting original value for attribute '%s': '%s'", attrName, value);
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put("-" + attrName, value);
                if (prov != null) {
                    prov.modifyEphemeralAttrsInLdap(entry, attrs);
                }
            }
        }

        @Override
        public EphemeralStore getStore() {
            return store;
        }
    }

    static class DryRunMigrationCallback implements MigrationCallback {
        private static final PrintStream console = System.out;

        @Override
        public void setEphemeralData(EphemeralInput input,
                EphemeralLocation location, String origName, Object origValue) throws ServiceException {
            EphemeralKey key = input.getEphemeralKey();
            console.println(String.format("\n%s", origName));
            console.println(String.format(Strings.repeat("-", origName.length())));
            console.println(String.format("original value:     %s", origValue));
            console.println(String.format("ephemeral value:    %s", input.getValue()));
            if (key.getDynamicComponent() != null) {
                console.println(String.format("dynamic component:  %s", key.getDynamicComponent()));
            }
            if (input.getExpiration() != null) {
                console.println(String.format("expiration:         %s", input.getExpiration()));
            }
            String locationStr = Joiner.on(" | ").join(location.getLocation());
            console.println(String.format("ephemeral location: [%s]", locationStr));
        }

        @Override
        public void deleteOriginal(Entry entry, String attrName, Object value,
                AttributeConverter converter) throws ServiceException {
        }

        @Override
        public EphemeralStore getStore() {
            return null;
        }
    }

    /**
     * Class representing the migration of attributes on a single LDAP Entry
     *
     * @author iraykin
     *
     */
    public static class MigrationTask implements Runnable {

        protected Map<String, AttributeConverter> converters;
        protected MigrationCallback callback;
        protected boolean deleteOriginal;
        protected Entry entry;

        public MigrationTask(Entry entry, Map<String, AttributeConverter> converters, MigrationCallback callback, boolean deleteOriginal) {
            this.entry = entry;
            this.converters = converters;
            this.callback = callback;
            this.deleteOriginal = deleteOriginal;
        }

        /**
         * Return the ephemeral location for this migration
         * @return
         */
        public EphemeralLocation getEphemeralLocation() {
            return new LdapEntryLocation(entry);
        }

        private List<Pair<EphemeralInput, Object>> migrateMultivaluedAttr(String attr, Object obj, AttributeConverter converter) {
            String[] values;
            if (obj instanceof String) {
                values = new String[] {(String) obj };
            } else if (obj instanceof String[]) {
                values = (String[]) obj;
            } else {
                ZimbraLog.ephemeral.warn("multivalued attribute converter expects String or String[],"
                        + " got type '%s' for attribute '%s'", obj.getClass().getName(), attr);
                return Collections.emptyList();
            }
            List<Pair<EphemeralInput, Object>> inputs = new LinkedList<Pair<EphemeralInput, Object>>();
            for (String v: values) {
                EphemeralInput input = converter.convert(attr, v);
                if (input != null) {
                    inputs.add(new Pair<EphemeralInput, Object>(input, v));
                }
            }
            return inputs;
        }

        private List<Pair<EphemeralInput, Object>> migrateAttr(String attr, AttributeConverter converter) {
            boolean multiValued = converter.isMultivalued();
            Object obj = multiValued ? entry.getMultiAttr(attr, false, true) : entry.getAttr(attr, false, true);
            if (obj == null || (obj instanceof String[] && ((String[]) obj).length == 0)) {
                return Collections.emptyList();
            }
            List<Pair<EphemeralInput, Object>> inputs = new LinkedList<Pair<EphemeralInput, Object>>();
            if (multiValued) {
                inputs.addAll(migrateMultivaluedAttr(attr, obj, converter));
            } else {
                EphemeralInput input = converter.convert(attr, obj);
                if (input != null) {
                    inputs.add(new Pair<EphemeralInput, Object>(input, obj));
                }
            }
            return inputs;
        }

        @VisibleForTesting
        public void migrateAttributes() throws ServiceException {
            ZimbraLog.ephemeral.debug("migrating attributes to ephemeral storage for account '%s'", entry.getLabel());
            List<Pair<EphemeralInput, Object>> inputs = new LinkedList<Pair<EphemeralInput, Object>>();

            boolean hasDataToMigrate = false;
            for (Map.Entry<String, AttributeConverter> converterEntry: converters.entrySet()) {
                String attrName = converterEntry.getKey();
                AttributeConverter converter = converterEntry.getValue();
                List<Pair<EphemeralInput, Object>> migratedData = migrateAttr(attrName, converter);
                if (!migratedData.isEmpty()) {
                    hasDataToMigrate = true;
                    inputs.addAll(migratedData);
                }
            }
            if (!hasDataToMigrate) {
                ZimbraLog.ephemeral.info("no ephemeral data to migrate for account '%s'", entry.getLabel());
                return;
            }
            EphemeralLocation location = getEphemeralLocation();
            for (Pair<EphemeralInput, Object> pair: inputs) {
                EphemeralInput input = pair.getFirst();
                String attrName = input.getEphemeralKey().getKey();
                Object origValue = pair.getSecond();
                try {
                    if (Thread.interrupted()) {
                        //another thread encountered an error during migration
                        return;
                    }
                    callback.setEphemeralData(input, location, attrName, origValue);
                } catch (ServiceException e) {
                    // if an exception is encountered, shut down all the migration threads and store
                    // the error so that it can be re-raised at the end
                    ZimbraLog.ephemeral.error("error encountered during migration; stopping migration process");
                    if (executor != null) {
                        exceptionWhileMigrating = e;
                        executor.shutdownNow();
                    }
                    throw e;
                }
                if (deleteOriginal) {
                    try {
                        callback.deleteOriginal(entry, attrName, origValue, converters.get(attrName));
                    } catch (ServiceException e) {
                        ZimbraLog.ephemeral.error(
                                "error deleting original LDAP value '%s' of attribute '%s' with callback '%s'",
                                origValue, attrName, callback.getClass().getName());
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                migrateAttributes();
            } catch (ServiceException e) {
                // async migration will re-raise the exception after all threads have been shut down
            }
        }
    }

    public static abstract class MigrationFlag {

        protected EphemeralStore store;

        public MigrationFlag(EphemeralStore store) {
            this.store = store;
        }

        public abstract void set() throws ServiceException;
        public abstract void unset() throws ServiceException;
        public abstract boolean isSet() throws ServiceException;
    }

    public static class ZimbraMigrationFlag extends MigrationFlag {

        private static final String migrationKey = "zimbraMigrationInProgress";
        private EphemeralKey key;
        private EphemeralLocation location;
        public ZimbraMigrationFlag(EphemeralStore store) {
            super(store);
            this.key = new EphemeralKey(migrationKey);
            this.location = new EphemeralLocation() {

                @Override
                public String[] getLocation() {
                    return new String[] { EntryType.GLOBALCONFIG.toString() };
                }
            };
        }

        private EphemeralStore getFlagStore() {
            //store will usually be SSDBEphemeralStore, but it's possible for this to be
            //FallbackEphemeralStore, in which case we want to isolate the primary store
            //to avoid acting on LdapEphemeralStore with the custom EphemeralLocation instance above.
            EphemeralStore flagStore;
            if (store instanceof FallbackEphemeralStore) {
                flagStore = ((FallbackEphemeralStore) store).getPrimaryStore();
            } else {
                flagStore = store;
            }
            return flagStore;
        }

        @Override
        public void set() throws ServiceException {
            EphemeralInput input = new EphemeralInput(key, ProvisioningConstants.TRUE);
            getFlagStore().set(input, location);
        }

        @Override
        public void unset() throws ServiceException {
            getFlagStore().delete(key, ProvisioningConstants.TRUE, location);
        }

        @Override
        public boolean isSet() throws ServiceException {
            return getFlagStore().has(key, location);
        }
    }

    /**
     * Class that returns instances of MigrationFlag and EphemeralStore.Factory
     * used during ephemeral data migration.
     *
     */
    public static interface MigrationHelper {
        MigrationFlag getMigrationFlag(EphemeralStore store);
        EphemeralStore.Factory getFallbackFactory();
        void flushCache();
    }

    /**
     * Default implementation; unit tests use another implementation.
     */
    public static class ZimbraMigrationHelper implements MigrationHelper {

        @Override
        public MigrationFlag getMigrationFlag(EphemeralStore store) {
            return new ZimbraMigrationFlag(store);
        }

        @Override
        public EphemeralStore.Factory getFallbackFactory() {
            return new LdapEphemeralStore.Factory();
        }

        @Override
        public void flushCache() {
            clearConfigCacheOnAllServers(false);
        }
    }

    public static void clearConfigCacheOnAllServers(boolean includeLocal) {
        ExecutorService executor = newCachedThreadPool(newDaemonThreadFactory("ClearEphemeralConfigCache"));
        List<Server> servers = null;
        try {
            servers = Provisioning.getInstance().getAllMailClientServers();
        } catch (ServiceException e) {
            ZimbraLog.account.warn("cannot fetch list of servers");
            return;
        }
        for (Server server: servers) {
            try {
                if (server.isLocalServer() && !includeLocal) {
                    // don't need to flush cache on this server
                    continue;
                }
            } catch (ServiceException e2) {
                ZimbraLog.ephemeral.warn("error determining if server %s is local server", server.getServiceHostname());
            }
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    SoapProvisioning soapProv = new SoapProvisioning();
                    try {
                        String adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI, true);
                        soapProv.soapSetURI(adminUrl);
                    } catch (ServiceException e1) {
                        ZimbraLog.ephemeral.warn("could not get admin URL for server %s during ephemeral backend change", e1);
                        return;
                    }

                    try {
                        soapProv.soapZimbraAdminAuthenticate();
                        soapProv.flushCache(CacheEntryType.config, null);
                        ZimbraLog.ephemeral.info("sent FlushCache request to server %s", server.getServiceHostname());

                    } catch (ServiceException e) {
                        ZimbraLog.ephemeral.warn("cannot send FlushCache request to server %s", server.getServiceHostname(), e);
                    }
                }

            });

        }
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}
    }
}
