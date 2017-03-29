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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.localconfig.LC;
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
import com.zimbra.cs.ephemeral.EphemeralResult;
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
    private int numThreads;
    private CSVReports csvReports = null;
    private MigrationCallback callback;
    // exceptionWhileMigrating is static for convenient access from threads.
    // Note that there is an implicit assumption that only one AttributMigration is active at any one time
    private static Exception exceptionWhileMigrating = null;
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
        if ((null == numThreads) || (1 == numThreads)) {
            this.numThreads = 1;
        } else {
            this.numThreads = numThreads;
        }
        exceptionWhileMigrating = null; // reset for later unit tests
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
        ZimbraLog.ephemeral.info("beginning migration of attributes %s to ephemeral storage",
                Joiner.on(", ").join(attrsToMigrate));
        EphemeralStore store = callback.getStore();
        if (store != null) {
            getMigrationFlag(store).set();
            migrationHelper.flushCache();
        }
    }

    @VisibleForTesting
    public void endMigration() throws ServiceException {
        EphemeralStore store = callback.getStore();
        if (store != null) {
            getMigrationFlag(store).unset();
            migrationHelper.flushCache();
        }
        ZimbraLog.ephemeral.info("migration of attributes %s to ephemeral storage completed",
                Joiner.on(", ").join(attrsToMigrate));
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
                            queue, activeConverterMap, csvReports, callback, deleteOriginal);
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
                        queue, activeConverterMap, csvReports, callback, deleteOriginal);
                worker.run();
            }
            if (exceptionWhileMigrating != null) {
                csvReports.zimbraLogFinalSummary(false);
                throw ServiceException.FAILURE("Failure during migration", exceptionWhileMigrating);
            }
            endMigration();
            csvReports.zimbraLogFinalSummary(true);
        } finally {
            closeReports();
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
        public abstract boolean setEphemeralData(EphemeralInput input, EphemeralLocation location, String origName, Object origValue) throws ServiceException;

        /**
         * Delete original attribute values
         */
        public abstract void deleteOriginal(Entry entry, String attrName, Object value, AttributeConverter converter) throws ServiceException;

        public abstract EphemeralStore getStore();

        /** @return Whether CSV reports should be created */
        public boolean disableCreatingReports();
    }

    /**
     * Callback that stores the EphemeralInput in the EphemeralStore
     */
    static class ZimbraMigrationCallback implements MigrationCallback {
        private final EphemeralStore store;
        private LdapProvisioning prov = null;

        public ZimbraMigrationCallback() throws ServiceException {
            EphemeralStore.Factory factory = EphemeralStore.getFactory();
            if (factory instanceof LdapEphemeralStore.Factory) {
                throw ServiceException.FAILURE("migration to LdapEphemeralStore is not supported", null);
            }
            String url = Provisioning.getInstance().getConfig().getEphemeralBackendURL();
            factory.test(url);
            EphemeralStore store = factory.getStore();
            if (store instanceof FallbackEphemeralStore) {
                this.store = ((FallbackEphemeralStore) store).getPrimaryStore();
            } else{
                this.store = store;
            }
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
        public boolean setEphemeralData(EphemeralInput input, EphemeralLocation location, String origName, Object origValue) throws ServiceException {
            ZimbraLog.ephemeral.debug("migrating '%s' value '%s'", origName, String.valueOf(origValue));
            EphemeralKey key = input.getEphemeralKey();
            EphemeralResult existing = store.get(key, location);
            if (existing == null || existing.isEmpty()) {
                store.update(input, location);
                return true;
            } else {
                ZimbraLog.ephemeral.debug("%s already has value '%s' in %s, skipping", key, existing.getValue(), store.getClass().getSimpleName());
                return false;
            }
        }

        @Override
        public void deleteOriginal(Entry entry, String attrName, Object value, AttributeConverter converter)
                throws ServiceException {
            ZimbraLog.ephemeral.debug("deleting original value for attribute '%s': '%s'", attrName, value);
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put("-" + attrName, value);
            if (prov != null) {
                prov.modifyEphemeralAttrsInLdap(entry, attrs);
            }
        }

        @Override
        public EphemeralStore getStore() {
            return store;
        }

        @Override
        public boolean disableCreatingReports() {
            return false;
        }
    }

    static class DryRunMigrationCallback implements MigrationCallback {
        private static final PrintStream console = System.out;

        @Override
        public boolean setEphemeralData(EphemeralInput input,
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
            return true;
        }

        @Override
        public void deleteOriginal(Entry entry, String attrName, Object value,
                AttributeConverter converter) throws ServiceException {
        }

        @Override
        public EphemeralStore getStore() {
            return null;
        }

        @Override
        public boolean disableCreatingReports() {
            return true;
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
        protected boolean deleteOriginal;
        protected Entry entry;
        protected CSVReports csvReports = null;
        protected long start;

        public MigrationTask(Entry entry, Map<String, AttributeConverter> converters,
                MigrationCallback callback, boolean deleteOriginal) {
            this(entry, converters, null, callback, deleteOriginal);
        }

        public MigrationTask(Entry entry, Map<String, AttributeConverter> converters,
                CSVReports csvReports, MigrationCallback callback, boolean deleteOriginal) {
            this.entry = entry;
            this.converters = converters;
            this.callback = callback;
            this.deleteOriginal = deleteOriginal;
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
                    if (callback.setEphemeralData(input, location, attrName, origValue)) {
                        attrsMigrated++;
                    }
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
                if (deleteOriginal) {
                    try {
                        callback.deleteOriginal(entry, attrName, origValue, converters.get(attrName));
                    } catch (ServiceException e) {
                        csvReports.log(entry.getLabel(), start, false, attrsMigrated, String.format(
                                "error deleting original LDAP value '%s' of attribute '%s' with callback '%s'",
                                origValue, attrName, callback.getClass().getName()));
                        ZimbraLog.ephemeral.error(
                                "error deleting original LDAP value '%s' of attribute '%s' with callback '%s'",
                                origValue, attrName, callback.getClass().getName());
                    }
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
        private final boolean deleteOriginal;

        public MigrationWorker(ConsumableQueue<NamedEntry> entries, Map<String, AttributeConverter> converters,
                CSVReports csvReports, MigrationCallback callback, boolean deleteOriginal) {
            this.entries = entries;
            this.converters = converters;
            this.csvReports = csvReports;
            this.callback = callback;
            this. deleteOriginal = deleteOriginal;
        }

        @Override
        public void run() {
            ZimbraLog.ephemeral.debug("Starting Thread %s", Thread.currentThread().getName());
            while (exceptionWhileMigrating == null) {
                Entry entry = entries.poll();
                if (null == entry) {
                    break;  // All entries processed.  We're done
                }
                MigrationTask migration = new MigrationTask(entry, converters, csvReports, callback, deleteOriginal);
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
            clearConfigCacheOnAllServers(true);
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
                        ZimbraLog.ephemeral.debug("sent FlushCache request to server %s", server.getServiceHostname());

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
