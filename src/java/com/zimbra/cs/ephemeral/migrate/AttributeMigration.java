package com.zimbra.cs.ephemeral.migrate;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.service.ServiceException;
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

    private void migrateAccount(Account account) {
        MigrationTask migration = new MigrationTask(account, activeConverterMap, callback, deleteOriginal);
        if (async) {
            executor.submit(migration);
        } else {
            migration.migrateAttributes();
        }
    }

    public void migrateAllAccounts() throws ServiceException {
        ZimbraLog.ephemeral.info("beginning migration of attributes %s to ephemeral storage",
                Joiner.on(", ").join(attrsToMigrate));
        for (NamedEntry entry: source.getEntries()) {
            Account acct = (Account) entry;
            migrateAccount(acct);
        }
        if (executor != null) {
            executor.shutdown();
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
            this.store = EphemeralStore.getFactory().getStore();
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
}
