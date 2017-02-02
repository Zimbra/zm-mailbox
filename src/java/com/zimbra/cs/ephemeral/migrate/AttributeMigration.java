package com.zimbra.cs.ephemeral.migrate;

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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.LdapEntryLocation;
import com.zimbra.cs.ephemeral.LdapEphemeralStore;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;

/**
 * Class handling migrating attributes to ephemeral storage
 *
 * @author iraykin
 *
 */
public class AttributeMigration {
    private EntrySource source;
    private Collection<String> attrsToMigrate;
    private boolean deleteOriginal = true;
    private boolean async = true;
    private MigrationCallback callback;
    private static ExecutorService executor = null;
    private static ServiceException exceptionWhileMigrating = null;

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
        ZimbraLog.ephemeral.debug("registering converter %s for attribute %s", converter.getClass().getName(), attribute);
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

    public void migrateAllAccounts() throws ServiceException {
        ZimbraLog.ephemeral.info("beginning migration of attributes %s to ephemeral storage",
                Joiner.on(", ").join(attrsToMigrate));
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
        private List<String> accounts = new ArrayList<String>();

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
    }

    /**
     * Callback that stores the EphemeralInput in the EphemeralStore
     */
    static class ZimbraMigrationCallback implements MigrationCallback {
        private EphemeralStore store;
        private boolean destinationIsLdap;
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
            Provisioning prov = Provisioning.getInstance();
            if (prov instanceof LdapProvisioning) {
                this.prov = (LdapProvisioning) prov;
            } else {
                ZimbraLog.ephemeral.warn("LdapProvisioning required to delete attributes from LDAP after migration to ephemeral storage; %s provided. "
                        + "Old values will not be removed", prov.getClass().getName());
            }

        }

        @Override
        public void setEphemeralData(EphemeralInput input, EphemeralLocation location, String origName, Object origValue) throws ServiceException {
            ZimbraLog.ephemeral.debug("migrating %s value %s", origName, String.valueOf(origValue));
            store.update(input, location);
        }

        @Override
        public void deleteOriginal(Entry entry, String attrName, Object value, AttributeConverter converter)
                throws ServiceException {
            // don't delete LDAP value if we are using LdapEphemeralStore and the attribute is single-valued,
            // since the value was already overwritten in the setEphemeralData step
            if (!(destinationIsLdap && !converter.isMultivalued())) {
                ZimbraLog.ephemeral.debug("deleting original value for attribute %s: %s", attrName, value);
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put("-" + attrName, value);
                if (prov != null) {
                    prov.modifyEphemeralAttrsInLdap(entry, attrs);
                }
            }
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
                ZimbraLog.ephemeral.warn("multivalued attribute converter expects String or String[], got type %s for attribute '%s'", obj.getClass().getName(), attr);
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
            if (entry instanceof NamedEntry) {
                ZimbraLog.ephemeral.debug("migrating attributes to ephemeral storage for account %s", ((NamedEntry) entry).getId());
            }
            List<Pair<EphemeralInput, Object>> inputs = new LinkedList<Pair<EphemeralInput, Object>>();

            boolean hasDataToMigrate = false;
            for (Map.Entry<String, AttributeConverter> entry: converters.entrySet()) {
                String attrName = entry.getKey();
                AttributeConverter converter = entry.getValue();
                List<Pair<EphemeralInput, Object>> migratedData = migrateAttr(attrName, converter);
                if (!migratedData.isEmpty()) {
                    hasDataToMigrate = true;
                    inputs.addAll(migratedData);
                }
            }
            if (!hasDataToMigrate) {
                ZimbraLog.ephemeral.info("no ephemeral data to migrate for account %s", ((NamedEntry) entry).getId());
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
                        ZimbraLog.ephemeral.error("error deleting original LDAP value %s of attribute %s with callback %s",
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
}
