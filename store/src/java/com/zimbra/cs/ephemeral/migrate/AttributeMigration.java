package com.zimbra.cs.ephemeral.migrate;

import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.concurrent.Executors.newCachedThreadPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.zimbra.cs.pubsub.PubSubService;
import com.zimbra.cs.pubsub.message.FlushCacheMsg;
import com.zimbra.soap.admin.type.CacheSelector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.BackendType;
import com.zimbra.cs.ephemeral.LdapEntryLocation;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo.Status;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.admin.FlushCache;
import com.zimbra.soap.admin.type.CacheEntryType;

/**
 * Class handling migrating attributes to ephemeral storage
 *
 * @author iraykin
 *
 */
public class AttributeMigration {
    private ShutdownHook hook;
    private String destUrl;
    private EntrySource source;
    private final Collection<String> attrsToMigrate;
    private int numThreads;
    private CSVReports csvReports = null;
    private static MigrationCallback callback;
    // exceptionWhileMigrating is static for convenient access from threads.
    // Note that there is an implicit assumption that only one AttributMigration is active at any one time
    private static Exception exceptionWhileMigrating = null;

    //map of all available converters
    private static Map<String, AttributeConverter> converterMap = new HashMap<String, AttributeConverter>();
    static {
        registerConverter(Provisioning.A_zimbraAuthTokens, new AuthTokenConverter());
        registerConverter(Provisioning.A_zimbraInvalidJWTokens, new AuthTokenConverter());
        registerConverter(Provisioning.A_zimbraCsrfTokenData, new CsrfTokenConverter());
        registerConverter(Provisioning.A_zimbraLastLogonTimestamp, new StringAttributeConverter());
    }

    //map of converters to be used in a particular migration
    private Map<String, AttributeConverter> activeConverterMap;

    /**
     * @param destUrl - ephemeral storage url to which data is transferred
     * @param attrsToMigrate - collection of attribute names to migrate
     * @param numThreads - number of threads to use during migration. If null, the migration happens synchronously.
     * @throws ServiceException
     */
    public AttributeMigration(String destUrl, Collection<String> attrsToMigrate, Integer numThreads) throws ServiceException {
        this(destUrl, attrsToMigrate, null, numThreads);
    }

    /**
     * @param destUrl - ephemeral storage url to which data is transferred
     * @param attrsToMigrate - collection of attribute names to migrate
     * @param source - EntrySource implementation
     * @param callback - MigrationCallback implementation that handles generated EphemeralInputs
     * @param numThreads - number of threads to use during migration. If null, the migration happens synchronously.
     * @throws ServiceException
     */
    public AttributeMigration(String destUrl, Collection<String> attrsToMigrate, EntrySource source, Integer numThreads) throws ServiceException {
        this.destUrl = destUrl;
        this.attrsToMigrate = attrsToMigrate;
        initConverters();
        setSource(source);
        if ((null == numThreads) || (1 == numThreads)) {
            this.numThreads = 1;
        } else {
            this.numThreads = numThreads;
        }
        exceptionWhileMigrating = null; // reset for later unit tests
        this.hook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(this.hook);
    }

    public static void registerConverter(String attribute, AttributeConverter converter) {
        ZimbraLog.ephemeral.debug("registering converter '%s' for attribute '%s'",
                converter.getClass().getName(), attribute);
        converterMap.put(attribute, converter);
    }

    public static AttributeConverter getConverter(String attrName) {
        return converterMap.get(attrName);
    }

    public void setSource(EntrySource source) {
        this.source = source;
    }

    public static void setCallback(MigrationCallback callback) {
        AttributeMigration.callback = callback;
    }

    private void initConverters() throws ServiceException {
        activeConverterMap = new HashMap<String, AttributeConverter>();
        for (String attr: attrsToMigrate) {
            AttributeConverter converter = converterMap.get(attr);
            if (converter == null) {
                throw new InvalidAttributeException(attr);
            } else {
                activeConverterMap.put(attr,  converter);
            }
        }
    }

    public final static class CSVReports {
        private static String[] csvHeaders =
            {"Name", "Start", "Elapsed ms", "Succeeded", "Number of attributes", "Info"};
        private static final SimpleDateFormat CSV_SUFFIX_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        final boolean dummyReport;
        private Report fullReport = null;
        private Report errorReport = null;

        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        /** use this for a null report */
        public CSVReports(boolean dummyMode) {
            dummyReport = dummyMode;
            if (dummyReport) {
                return;
            }
            String suffix = CSV_SUFFIX_DATE_FORMAT.format(new Date(System.currentTimeMillis()));
            fullReport = new Report(String.format("%s%szmmigrateattrs-%s.csv",
                        LC.zimbra_tmp_directory.value(), File.separator, suffix));
            errorReport = new Report(String.format("%s%szmmigrateattrs-errors-%s.csv",
                        LC.zimbra_tmp_directory.value(), File.separator, suffix));
        }

        public void log(String acctName, long start, boolean success, int attrNum, String... rest) {
            if (dummyReport) {
                return;
            }
            List<String> line = Lists.newArrayList();
            line.add(acctName);
            line.add(DATE_FORMAT.format(new Date(start)));
            long now = System.currentTimeMillis();
            line.add(Long.toString(now - start));
            line.add(success ? "Y" : "N");
            line.add(Integer.toString(attrNum));
            for (String str : rest) {
                line.add(str);
            }
            fullReport.printRecord(line);
            if (!success) {
                errorReport.printRecord(line);
            }
        }

        public void zimbraLogFinalSummary(boolean completed) {
            if (dummyReport) {
                return;
            }
            if (null != fullReport.csvPrinter) {
                if (completed) {
                    ZimbraLog.ephemeral.info("See full report               : '%s'", fullReport.name);
                } else {
                    ZimbraLog.ephemeral.info("See PARTIAL report (migration abandoned) : '%s'", fullReport.name);
                }
            }
            if (null != errorReport.csvPrinter) {
                ZimbraLog.ephemeral.info("See report summary for errors : '%s'", errorReport.name);
            }
        }

        public void close() {
            if (dummyReport) {
                return;
            }
            fullReport.close();
            errorReport.close();
        }

        public void flush() {
            if (dummyReport) {
                return;
            }
            fullReport.flush();
            errorReport.flush();
        }

        private class Report {
            final String name;
            CSVPrinter csvPrinter = null;
            OutputStream outputStream = null;
            boolean broken = false;
            Report (String fname) {
                name = fname;
            }

            void printRecord(List<String> line) {
                if (!open()) {
                    broken = true;
                    return;
                }
                try {
                    synchronized(this) {
                        csvPrinter.printRecord(line);
                    }
                } catch (IOException e) {
                    ZimbraLog.ephemeral.error("Problem writing record '%s' to file '%s'", line, name, e);
                }
            }

            boolean open() {
                if (broken) {
                    return false;
                }
                if (null != csvPrinter) {
                    return true;
                }
                try {
                    synchronized(this) {
                        if (null != csvPrinter) {
                            return true;
                        }
                        outputStream = new FileOutputStream(name);
                        csvPrinter = CSVFormat.DEFAULT.withHeader(csvHeaders).print(
                                new OutputStreamWriter(outputStream, "UTF-8"));
                    }
                    Path path = Paths.get(name);
                    try {
                        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
                    } catch (IOException e) {
                        ZimbraLog.ephemeral.error("Failed to set CSV file '%s' permissions", name, e);
                    }
                    return true;
                } catch (IOException e) {
                    ZimbraLog.ephemeral.error("Failed to create CSV file '%s'.", name, e);
                    return false;
                }
            }

            void close() {
                if (csvPrinter != null) {
                    try {
                        csvPrinter.close();
                    } catch (IOException e) {
                        ZimbraLog.ephemeral.info("Problem closing CSVPrinter for %s", name,  e);
                    }
                    Path path = Paths.get(name);
                    try {
                        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("r--------"));
                    } catch (IOException e) {
                        ZimbraLog.ephemeral.error("Failed to set CSV file '%s' permissions", name, e);
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        ZimbraLog.ephemeral.error("Problem closing Stream for CSVPrinter for file %s", name, e);
                    }
                }
            }

            void flush() {
                if (csvPrinter != null) {
                    try {
                        synchronized(this) {
                            csvPrinter.flush();
                        }
                    } catch (IOException e) {
                        ZimbraLog.ephemeral.debug("Problem flushing csvPrinter for %s", name, e.getMessage());
                    }
                }
            }
        }
    }

    private void closeReports() {
        if (null != csvReports) {
            csvReports.close();
        }
    }

    public static MigrationInfo getMigrationInfo() throws ServiceException {
        return MigrationInfo.getFactory().getInfo();
    }

    @VisibleForTesting
    public void beginMigration() throws ServiceException {
        if (callback == null) {
            throw ServiceException.FAILURE("no MigrationCallback specified", null);
        }
        MigrationInfo info = getMigrationInfo();
        Status curStatus = info.getStatus();
        if (curStatus == Status.IN_PROGRESS) {
            throw ServiceException.FAILURE(String.format("Cannot begin migration; a migration to %s is in progress (started %s).\n"
                    + "If this is in error, please reset the migration status by running zmmigrateattrs -c", info.getURL(), info.getDateStr("MM/dd/yyyy HH:mm:ss")), null);
        }
        ZimbraLog.ephemeral.info("beginning migration of attributes %s to ephemeral storage",
                Joiner.on(", ").join(attrsToMigrate));
        info.beginMigration();
        callback.flushCache();
    }

    @VisibleForTesting
    public void endMigration() throws ServiceException {
        getMigrationInfo().endMigration();
        ZimbraLog.ephemeral.info("migration of attributes %s to ephemeral storage completed",
                Joiner.on(", ").join(attrsToMigrate));
        ZimbraLog.ephemeral.info("Note: This utility does not delete the migrated ephemeral data from LDAP.");
        ZimbraLog.ephemeral.info("      Once testing shows that the migration was successful `zimbraEphemeralBackendUrl` needs to be updated to point to the new backend.");
        ZimbraLog.ephemeral.info(String.format("      Example: zmprov mcf zimbraEphemeralBackendUrl %s", this.destUrl));

        callback.flushCache();
    }

    /**
     * A lightweight partial implementation of a Queue that can only be consumed, not added to.
     * Could have used a LinkedList but that seems overkill
     */
    private static class ConsumableQueue<T> {
        private final List<T> queue;
        private int headIndex;

        public ConsumableQueue(List<T> entries) {
            queue = Collections.unmodifiableList(entries);
            headIndex = 0;
        }

        public int size() {
            return queue.size() - headIndex;
        }

        public boolean isEmpty() {
            return size() <= 0;
        }

        /**
         * Retrieves and removes the head of this queue, or returns {@code null} if this queue is empty.
         * @return the head of this queue, or {@code null} if this queue is empty
         */
        public synchronized T poll() {
            if (isEmpty()) {
                return null;
            }
            T entry = queue.get(headIndex);
            headIndex++;
            return entry;
        }
    }

    public void migrateAllAccounts() throws ServiceException {
        try {
            beginMigration();
            csvReports = new CSVReports(callback.disableCreatingReports());
            this.hook.setCSVReports(csvReports);
            ConsumableQueue<NamedEntry> queue = new ConsumableQueue<NamedEntry>(source.getEntries());
            int numEntries = queue.size();
            if ((numThreads > 1) && (numEntries > 1)) {
                if (numThreads > numEntries) {
                    ZimbraLog.ephemeral.info("Only using %d threads - 1 for each account", numEntries);
                    numThreads = numEntries;
                }
                Thread[] threads = new Thread[numThreads];
                for (int i = 0; i < threads.length; i++) {
                    MigrationWorker worker = new MigrationWorker(
                            queue, activeConverterMap, csvReports, callback);
                    threads[i] = new Thread(worker, String.format("MigrateEphemeralAttrs-%d", i));
                }
                for (Thread thread : threads) {
                    thread.start();
                }
                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                    }
                }
            } else {
                MigrationWorker worker = new MigrationWorker(
                        queue, activeConverterMap, csvReports, callback);
                worker.run();
            }
            if (exceptionWhileMigrating != null) {
                this.hook.setFinished(false);
                this.hook.setException(exceptionWhileMigrating);
                getMigrationInfo().migrationFailed();
                callback.flushCache();
                throw ServiceException.FAILURE("Failure during migration", exceptionWhileMigrating);
            } else {
                this.hook.setFinished(true);
                endMigration();
            }
        } finally {
            closeReports();
        }
    }

    public static interface EntrySource {
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
        public void setEphemeralData(EphemeralInput input, EphemeralLocation location, String origName, Object origValue) throws ServiceException;

        public EphemeralStore getStore();

        /** @return Whether CSV reports should be created */
        public boolean disableCreatingReports();

        /**
         * Flush the config cache
         */
        public void flushCache();
    }

    /**
     * Callback that stores the EphemeralInput in the EphemeralStore specified by the provided URL
     */
    static class ZimbraMigrationCallback implements MigrationCallback {
        private final String destURL;
        private final EphemeralStore store;
        private LdapProvisioning prov = null;

        public ZimbraMigrationCallback(String destinationURL) throws ServiceException {
            //Update migration info first so that EphemeralStore.Factory.getBackendURL() can access it.
            //We don't have to flush the cache here, since this only needs to be visible to this process
            MigrationInfo info = getMigrationInfo();
            info.setURL(destinationURL);
            info.save();
            String backend = destinationURL.split(":")[0];
            EphemeralStore.Factory factory = EphemeralStore.getFactory(backend);
            if (factory == null) {
                throw ServiceException.FAILURE(String.format("no ephemeral store found for URL '%s'", destinationURL), null);
            }
            factory.test(destinationURL);
            factory.setBackendType(BackendType.migration);
            destURL = destinationURL;
            store = factory.getStore();
            ZimbraLog.ephemeral.debug("migrating to ephemeral backend at %s", destURL);
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
        public EphemeralStore getStore() {
            return store;
        }

        @Override
        public boolean disableCreatingReports() {
            return false;
        }

        @Override
        public void flushCache() {
            clearConfigCacheOnAllServers(true, false);
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
        public EphemeralStore getStore() {
            return null;
        }

        @Override
        public boolean disableCreatingReports() {
            return true;
        }

        @Override
        public void flushCache() {
            //we don't need to actually flush the cache if doing a dry run
        }
    }

    /**
     * Class representing the migration of attributes on a single LDAP Entry
     *
     * @author iraykin
     *
     */
    public static class MigrationTask {

        protected Map<String, AttributeConverter> converters;
        protected MigrationCallback callback;
        protected Entry entry;
        protected CSVReports csvReports = null;
        protected long start;

        public MigrationTask(Entry entry, Map<String, AttributeConverter> converters,
                MigrationCallback callback) {
            this(entry, converters, null, callback);
        }

        public MigrationTask(Entry entry, Map<String, AttributeConverter> converters,
                CSVReports csvReports, MigrationCallback callback) {
            this.entry = entry;
            this.converters = converters;
            this.callback = callback;
            if (null == csvReports) {
                this.csvReports = new CSVReports(true); /* dummy reports */
            } else {
                this.csvReports = csvReports;
            }
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
            start = System.currentTimeMillis();
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
                csvReports.log(entry.getLabel(), start, true, 0, "no ephemeral data to migrate");
                ZimbraLog.ephemeral.info("no ephemeral data to migrate for account '%s'", entry.getLabel());
                return;
            }
            EphemeralLocation location = getEphemeralLocation();
            int attrsMigrated = 0;
            for (Pair<EphemeralInput, Object> pair: inputs) {
                EphemeralInput input = pair.getFirst();
                String attrName = input.getEphemeralKey().getKey();
                Object origValue = pair.getSecond();
                try {
                    if ((Thread.interrupted()) || (null != exceptionWhileMigrating)) {
                        //another thread encountered an error during migration
                        csvReports.log(entry.getLabel(), start, false, attrsMigrated,
                                "migration stopped before completing");
                        return;
                    }
                    callback.setEphemeralData(input, location, attrName, origValue);
                    attrsMigrated++;
                } catch (Exception e) {
                    // if an exception is encountered, shut down all the migration threads and store
                    // the error so that it can be re-raised at the end
                    csvReports.log(entry.getLabel(), start, false, attrsMigrated,
                            String.format("error encountered during migration; stopping migration process - %s",
                                    e.getMessage()));
                    ZimbraLog.ephemeral.error("error encountered during migration; stopping migration process", e);
                    if (null == exceptionWhileMigrating) {
                        exceptionWhileMigrating = e;
                    }
                    throw e;
                }
            }
            csvReports.log(entry.getLabel(), start, true, attrsMigrated, "migration completed");
        }
    }

    /**
     * Responsible for consuming some of the shared queue of entries in co-operation with other workers
     */
    public static class MigrationWorker implements Runnable {
        private final ConsumableQueue<NamedEntry> entries;
        private final Map<String, AttributeConverter> converters;
        private final CSVReports csvReports;
        private final MigrationCallback callback;

        public MigrationWorker(ConsumableQueue<NamedEntry> entries, Map<String, AttributeConverter> converters,
                CSVReports csvReports, MigrationCallback callback) {
            this.entries = entries;
            this.converters = converters;
            this.csvReports = csvReports;
            this.callback = callback;
        }

        @Override
        public void run() {
            ZimbraLog.ephemeral.debug("Starting Thread %s", Thread.currentThread().getName());
            while (exceptionWhileMigrating == null) {
                Entry entry = entries.poll();
                if (null == entry) {
                    break;  // All entries processed.  We're done
                }
                MigrationTask migration = new MigrationTask(entry, converters, csvReports, callback);
                try {
                    migration.migrateAttributes();
                } catch (Exception e) {
                    // async migration will re-raise the exception after all threads have been shut down
                } finally {
                    csvReports.flush();
                }
            }
            ZimbraLog.ephemeral.debug("Finishing Thread %s", Thread.currentThread().getName());
        }
    }

    public static void clearConfigCacheOnAllServers(boolean includeLocal, boolean registerTokenInPrevStore) {
        CacheSelector selector = new CacheSelector(true, CacheEntryType.config.name());
        PubSubService.getInstance().publish(PubSubService.BROADCAST, new FlushCacheMsg(selector));
    }
}
