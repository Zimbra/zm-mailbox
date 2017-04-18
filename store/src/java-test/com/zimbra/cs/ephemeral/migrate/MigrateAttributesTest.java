package com.zimbra.cs.ephemeral.migrate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralResult;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.Factory;
import com.zimbra.cs.ephemeral.FallbackEphemeralStore;
import com.zimbra.cs.ephemeral.InMemoryEphemeralStore;
import com.zimbra.cs.ephemeral.LdapEntryLocation;
import com.zimbra.cs.ephemeral.LdapEphemeralStore;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.EntrySource;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationCallback;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationFlag;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationHelper;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.MigrationTask;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration.ZimbraMigrationFlag;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class MigrateAttributesTest {

    private static Map<String, AttributeConverter> converters = new HashMap<String, AttributeConverter>();
    static {
        converters.put(Provisioning.A_zimbraAuthTokens, new AuthTokenConverter());
        converters.put(Provisioning.A_zimbraCsrfTokenData, new CsrfTokenConverter());
        converters.put(Provisioning.A_zimbraLastLogonTimestamp, new StringAttributeConverter());
    }

    private static String authToken1;
    private static String authToken2;
    private static String csrfToken1;
    private static String csrfToken2;
    private static String lastLogon;
    private static Account acct;

    @BeforeClass
    public static void setUp() throws Exception {
        MailboxTestUtil.initServer();
        AttributeMigration.setMigrationHelper(new TestMigrationHelper());
        Provisioning prov = Provisioning.getInstance();
        acct = prov.createAccount("user1", "test123", new HashMap<String, Object>());
        Map<String, Object> attrs = new HashMap<String, Object>();
        authToken1 = String.format("%d|%d|%s", 1234, 100000L, "server_1");
        authToken2 = String.format("%d|%d|%s", 5678, 100000L, "server_2");
        attrs.put("+" + Provisioning.A_zimbraAuthTokens, new String[] {authToken1, authToken2});
        csrfToken1 = String.format("%s:%s:%d", "data1", "crumb1", 100000L);
        csrfToken2 = String.format("%s:%s:%d", "data2", "crumb2", 100000L);
        attrs.put("+" + Provisioning.A_zimbraCsrfTokenData, new String[] {csrfToken1, csrfToken2});
        lastLogon = "currentdate";
        attrs.put("+" + Provisioning.A_zimbraLastLogonTimestamp, lastLogon);
        prov.modifyAttrs(acct, attrs);
    }

    /*
     * Test the individual converters
     */
    @Test
    public void testConverters() {
        EphemeralInput input = runConverter(Provisioning.A_zimbraAuthTokens, new AuthTokenConverter());
        verifyAuthTokenEphemeralInput(input, "1234", "server_1", 100000L);

        input = runConverter(Provisioning.A_zimbraCsrfTokenData, new CsrfTokenConverter());
        verifyCsrfTokenEphemeralInput(input, "crumb1", "data1", 100000L);

        input = runConverter(Provisioning.A_zimbraLastLogonTimestamp, new StringAttributeConverter());
        verifyLastLogonTimestampEphemeralInput(input, "currentdate");
    }

    private EphemeralInput runConverter(String attrName, AttributeConverter converter) {
        String value = acct.getAttr(attrName, false, true);
        return converter.convert(attrName, value);
    }

    /*
     * Test MigrationTask for auth tokens
     */
    @Test
    public void testAuthTokenMigrationTask() throws ServiceException {
        List<EphemeralInput> results = new LinkedList<EphemeralInput>();
        Map<String, AttributeConverter> converters = new HashMap<String, AttributeConverter>();
        converters.put(Provisioning.A_zimbraAuthTokens, new AuthTokenConverter());
        Multimap<String, Object> deletedAttrs = LinkedListMultimap.create();
        MigrationTask task = new MigrationTask(acct, converters, new DummyMigrationCallback(results, deletedAttrs), true);
        task.migrateAttributes();
        assertEquals(2, results.size());
        verifyAuthTokenEphemeralInput(results.get(0), "1234", "server_1", 100000L);
        verifyAuthTokenEphemeralInput(results.get(1), "5678", "server_2", 100000L);
        Collection<Object> deleted = deletedAttrs.asMap().get(Provisioning.A_zimbraAuthTokens);
        assertEquals(2, deleted.size());
        assertTrue(deleted.contains(authToken1));
        assertTrue(deleted.contains(authToken2));
    }

    /*
     * Test MigrationTask for CSRF tokens
     */
    @Test
    public void testCsrfTokenMigrationTask() throws ServiceException {
        List<EphemeralInput> results = new LinkedList<EphemeralInput>();
        Map<String, AttributeConverter> converters = new HashMap<String, AttributeConverter>();
        converters.put(Provisioning.A_zimbraCsrfTokenData, new CsrfTokenConverter());
        Multimap<String, Object> deletedAttrs = LinkedListMultimap.create();
        MigrationTask task = new MigrationTask(acct, converters, new DummyMigrationCallback(results, deletedAttrs), true);
        task.migrateAttributes();
        assertEquals(2, results.size());
        verifyCsrfTokenEphemeralInput(results.get(0), "crumb1", "data1", 100000L);
        verifyCsrfTokenEphemeralInput(results.get(1), "crumb2", "data2", 100000L);
        Collection<Object> deleted = deletedAttrs.asMap().get(Provisioning.A_zimbraCsrfTokenData);
        assertEquals(2, deleted.size());
        assertTrue(deleted.contains(csrfToken1));
        assertTrue(deleted.contains(csrfToken2));
    }

    /*
     * Test MigrationTask for last login timestamp
     */
    @Test
    public void testLastLogonTimestampMigrationTask() throws ServiceException {
        List<EphemeralInput> results = new LinkedList<EphemeralInput>();
        Map<String, AttributeConverter> converters = new HashMap<String, AttributeConverter>();
        converters.put(Provisioning.A_zimbraLastLogonTimestamp, new StringAttributeConverter());
        Multimap<String, Object> deletedAttrs = LinkedListMultimap.create();
        MigrationTask task = new MigrationTask(acct, converters, new DummyMigrationCallback(results, deletedAttrs), true);
        task.migrateAttributes();
        assertEquals(1, results.size());
        verifyLastLogonTimestampEphemeralInput(results.get(0), "currentdate");
        Collection<Object> deleted = deletedAttrs.asMap().get(Provisioning.A_zimbraLastLogonTimestamp);
        assertEquals(1, deleted.size());
        assertTrue(deleted.contains("currentdate"));
    }


    /*
     * Test end-to-end AttributeMigration
     */
    @Test
    public void testAttributeMigration() throws Exception {
        EphemeralStore destination = EphemeralStore.getFactory().getStore();
        EntrySource source = new DummyEntrySource(acct);
        Multimap<String, Object> deletedAttrs = LinkedListMultimap.create();

        List<String> attrsToMigrate = Arrays.asList(new String[] {
                Provisioning.A_zimbraAuthTokens,
                Provisioning.A_zimbraCsrfTokenData,
                Provisioning.A_zimbraLastLogonTimestamp});


        //DummyMigrationCallback will store attributes in InMemoryEphemeralStore, and track deletions in deletedAttrs map
        MigrationCallback callback = new DummyMigrationCallback(destination, deletedAttrs);
        AttributeMigration migration = new AttributeMigration(attrsToMigrate, source, callback, null);

        //disable running in separate thread
        //run migration
        migration.migrateAllAccounts();
        EphemeralLocation location = new LdapEntryLocation(acct);
        EphemeralResult result = destination.get(new EphemeralKey(Provisioning.A_zimbraAuthTokens, "1234"), location);
        assertEquals("server_1", result.getValue());
        result = destination.get(new EphemeralKey(Provisioning.A_zimbraAuthTokens, "5678"), location);
        assertEquals("server_2", result.getValue());
        result = destination.get(new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, "crumb1"), location);
        assertEquals("data1", result.getValue());
        result = destination.get(new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, "crumb2"), location);
        assertEquals("data2", result.getValue());
        result = destination.get(new EphemeralKey(Provisioning.A_zimbraLastLogonTimestamp), location);
        assertEquals("currentdate", result.getValue());
        Collection<Object> deleted = deletedAttrs.get(Provisioning.A_zimbraAuthTokens);
        assertTrue(deleted.contains(authToken1));
        assertTrue(deleted.contains(authToken2));
        deleted = deletedAttrs.get(Provisioning.A_zimbraCsrfTokenData);
        assertTrue(deleted.contains(csrfToken1));
        assertTrue(deleted.contains(csrfToken2));
        deleted = deletedAttrs.get(Provisioning.A_zimbraLastLogonTimestamp);
        assertTrue(deleted.contains(lastLogon));
    }

    @Test
    public void testErrorDuringMigration() throws Exception {
        List<EphemeralInput> results = new LinkedList<EphemeralInput>();
        EntrySource source = new DummyEntrySource(acct, acct, acct);
        Multimap<String, Object> deletedAttrs = LinkedListMultimap.create();

        List<String> attrsToMigrate = Arrays.asList(new String[] {
                Provisioning.A_zimbraAuthTokens,
                Provisioning.A_zimbraCsrfTokenData,
                Provisioning.A_zimbraLastLogonTimestamp});


        DummyMigrationCallback callback = new DummyMigrationCallback(results, deletedAttrs);
        callback.throwErrorDuringMigration = true;
        AttributeMigration migration = new AttributeMigration(attrsToMigrate, source, callback, null);
        try {
            migration.migrateAllAccounts();
            fail("synchronous migration should throw an exception");
        } catch (ServiceException e) {
            //make sure the root exception got thrown
            assertTrue(e.getMessage().contains("Failure during migration"));
        }
        assertEquals(0, results.size()); //make sure nothing got migrated
        migration = new AttributeMigration(attrsToMigrate, source, callback, 3);

        try {
            migration.migrateAllAccounts();
            fail("async migration should throw an exception");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Failure during migration"));
        }
        assertEquals(0, results.size());
    }

    @Test
    public void testMigrateAlreadyMigratedAccount() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        //create a new account that will not have any data to migrate
        Account acct = prov.createAccount("user2", "test123", new HashMap<String, Object>());
        EntrySource source = new DummyEntrySource(acct);
        Multimap<String, Object> deletedAttrs = LinkedListMultimap.create();
        List<EphemeralInput> results = new LinkedList<EphemeralInput>();
        List<String> attrsToMigrate = Arrays.asList(new String[] {
                Provisioning.A_zimbraAuthTokens,
                Provisioning.A_zimbraCsrfTokenData,
                Provisioning.A_zimbraLastLogonTimestamp});

        DummyMigrationCallback callback = new DummyMigrationCallback(results, deletedAttrs);
        callback.throwErrorDuringMigration = false;
        AttributeMigration migration = new AttributeMigration(attrsToMigrate, source, callback, null);
        migration.migrateAllAccounts();
        assertTrue(results.isEmpty());
    }

    @Test
    public void testFallbackEphemeralStoreWhenMigrating() throws Exception {
        EphemeralStore destination = EphemeralStore.getFactory().getStore();
        EntrySource source = new DummyEntrySource(acct);
        Multimap<String, Object> deletedAttrs = LinkedListMultimap.create();

        List<String> attrsToMigrate = Arrays.asList(new String[] {
                Provisioning.A_zimbraAuthTokens,
                Provisioning.A_zimbraCsrfTokenData,
                Provisioning.A_zimbraLastLogonTimestamp});


        //DummyMigrationCallback will store attributes in InMemoryEphemeralStore, and track deletions in deletedAttrs map
        MigrationCallback callback = new DummyMigrationCallback(destination, deletedAttrs);
        AttributeMigration migration = new AttributeMigration(attrsToMigrate, source, callback, null);
        migration.beginMigration();
        //set to in-memory backend because fallback won't be enabled with default LDAP backend
        EphemeralStore.setFactory(InMemoryEphemeralStore.Factory.class);
        Factory factory = EphemeralStore.getFactory();
        EphemeralStore store = factory.getStore();
        //in-memory backend will be wrapped in a FallbackEphemeralStore, with LDAP as the fallback
        assertTrue(store instanceof FallbackEphemeralStore);
        FallbackEphemeralStore fallbackStore = (FallbackEphemeralStore) store;
        assertTrue(fallbackStore.getPrimaryStore() instanceof InMemoryEphemeralStore);
        assertTrue(fallbackStore.getSecondaryStore() instanceof LdapEphemeralStore);
        migration.endMigration();
        EphemeralStore.setFactory(InMemoryEphemeralStore.Factory.class);
        //when migration is finished, fallback won't be enabled anymore
        factory = EphemeralStore.getFactory();
        store = factory.getStore();
        assertTrue(store instanceof InMemoryEphemeralStore);
    }

    private void verifyAuthTokenEphemeralInput(EphemeralInput input, String token, String serverVersion, Long expiration) {
        EphemeralKey key = input.getEphemeralKey();
        assertEquals(Provisioning.A_zimbraAuthTokens, key.getKey());
        assertEquals(token, key.getDynamicComponent());
        assertEquals(serverVersion, input.getValue());
        assertEquals(expiration, input.getExpiration());
    }

    private void verifyCsrfTokenEphemeralInput(EphemeralInput input, String crumb, String data, Long expiration) {
        EphemeralKey key = input.getEphemeralKey();
        assertEquals(Provisioning.A_zimbraCsrfTokenData, key.getKey());
        assertEquals(crumb, key.getDynamicComponent());
        assertEquals(data, input.getValue());
        assertEquals(expiration, input.getExpiration());
    }

    private void verifyLastLogonTimestampEphemeralInput(EphemeralInput input, String expected) {
        EphemeralKey key = input.getEphemeralKey();
        assertEquals(Provisioning.A_zimbraLastLogonTimestamp, key.getKey());
        assertTrue(key.getDynamicComponent() == null);
        assertEquals("currentdate", input.getValue());
    }

    public static class DummyMigrationCallback implements AttributeMigration.MigrationCallback {
        private List<EphemeralInput> trackedInputs;
        private final Multimap<String, Object> deletedValues;
        private EphemeralStore store = null;
        private boolean throwErrorDuringMigration = false;

        // for end-to-end testing with InMemoryEphemeralStore
        DummyMigrationCallback(EphemeralStore store, Multimap<String, Object> deletedValues) {
            this.deletedValues = deletedValues;
            this.store = store;
        }

        //for testing outputs of AttributeConverters
        DummyMigrationCallback(List<EphemeralInput> inputs, Multimap<String, Object> deletedValues) {
            this.trackedInputs = inputs;
            this.deletedValues = deletedValues;
        }

        @Override
        public boolean setEphemeralData(EphemeralInput input,
                EphemeralLocation location, String origKey, Object origValue) throws ServiceException {
            if (throwErrorDuringMigration) {
                throw ServiceException.FAILURE("error during migration", null);
            } else {
                if (trackedInputs != null) {
                    trackedInputs.add(input);
                }
                if (store != null) {
                    store.update(input, location);
                }
                return true;
            }
        }

        @Override
        public void deleteOriginal(Entry entry, String attrName, Object value,
                AttributeConverter converter) throws ServiceException {
            deletedValues.put(attrName, value);
        }

        @Override
        public EphemeralStore getStore() {
            return store;
        }

        @Override
        public boolean disableCreatingReports() {
            return true;
        }
    }

    public class DummyEntrySource implements EntrySource {
        List<NamedEntry> entries;
        public DummyEntrySource(Account... accts) {
            entries = new ArrayList<NamedEntry>(accts.length);
            entries.addAll(Arrays.asList(accts));
        }
        @Override
        public List<NamedEntry> getEntries() throws ServiceException {
            return entries;
        }
    }

    public static class TestMigrationHelper implements MigrationHelper {

        private static MigrationFlag flag = null;
        private static Factory fallbackFactory = new LdapEphemeralStore.Factory();

        @Override
        public MigrationFlag getMigrationFlag(EphemeralStore store) {
            if (flag == null) {
                flag = new ZimbraMigrationFlag(store);
            }
            return flag;
        }

        @Override
        public Factory getFallbackFactory() {
            return fallbackFactory;
        }

        @Override
        public void flushCache() {}
    }

}
