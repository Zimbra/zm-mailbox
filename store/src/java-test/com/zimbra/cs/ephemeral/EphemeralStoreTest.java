package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.RelativeExpiration;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class EphemeralStoreTest {

    private EphemeralStore store;

    @Before
    public void setUp() throws Exception {
        EphemeralStore.setFactory(InMemoryEphemeralStore.Factory.class);
        store = EphemeralStore.getFactory().getStore();
    }

    @After
    public void tearDown() throws Exception {
        EphemeralStore.getFactory().shutdown();
    }

    @Test
    public void testSet() throws Exception {
        EphemeralKey key = new EphemeralKey("foo");
        EphemeralLocation target = new TestLocation();
        store.set(new EphemeralInput(key, "bar"), target);
        assertEquals("bar", store.get(key, target).getValue());
        store.set(new EphemeralInput(key, "baz"), target);
        assertEquals("baz", store.get(key, target).getValue());
    }

    @Test
    public void testUpdate() throws Exception {
        EphemeralKey staticKey = new EphemeralKey("foo");
        EphemeralKey dynamicKey = new EphemeralKey("foo", "1");
        EphemeralLocation target = new TestLocation();
        store.update(new EphemeralInput(staticKey, "bar"), target);
        store.update(new EphemeralInput(staticKey, "baz"), target);
        EphemeralResult result = store.get(staticKey, target);
        String[] values = result.getValues();
        assertEquals("bar", values[0]);
        assertEquals("baz", values[1]);

        //test dynamic values
        store.update(new EphemeralInput(dynamicKey, "dynamic"), target);
        result = store.get(staticKey, target);
        values = result.getValues();
        assertEquals(2, values.length);
        assertEquals("bar", values[0]);
        assertEquals("baz", values[1]);

        result = store.get(dynamicKey, target);
        values = result.getValues();
        assertEquals(1, values.length);
        assertEquals("dynamic", values[0]);
    }

    @Test
    public void testDelete() throws Exception {
        EphemeralLocation target = new TestLocation();
        EphemeralKey staticKey = new EphemeralKey("foo");
        EphemeralKey dynamicKey1 = new EphemeralKey("foo", "1");
        EphemeralKey dynamicKey2 = new EphemeralKey("foo", "2");

        store.set(new EphemeralInput(staticKey, "bar"), target);
        store.update(new EphemeralInput(staticKey, "baz"), target);
        store.update(new EphemeralInput(dynamicKey1, "dynamic1"), target);
        store.update(new EphemeralInput(dynamicKey2, "dynamic2"), target);
        store.delete(staticKey, "bar", target);
        EphemeralResult result = store.get(staticKey, target);
        assertEquals(1, result.getValues().length);
        assertEquals("baz", result.getValue());
        store.delete(staticKey, "baz", target);
        result = store.get(staticKey, target);
        assertTrue(result.isEmpty());

        store.delete(dynamicKey1, "dynamic1", target);
        result = store.get(dynamicKey1, target);
        assertTrue(result.isEmpty());
        result = store.get(dynamicKey2, target);
        assertEquals(1, result.getValues().length);
        assertEquals("dynamic2", result.getValue());

        store.delete(dynamicKey2, "dynamic2", target);
        result = store.get(dynamicKey2, target);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExpiry() throws Exception {
        EphemeralLocation target = new TestLocation();
        EphemeralKey staticKey = new EphemeralKey("foo");
        EphemeralKey dynamicKey = new EphemeralKey("foo","1");
        EphemeralInput input1 = new EphemeralInput(staticKey, "bar");
        input1.setExpiration(new RelativeExpiration(1L, TimeUnit.SECONDS));
        store.set(input1, target);
        EphemeralInput input2 = new EphemeralInput(dynamicKey, "bar");
        input2.setExpiration(new RelativeExpiration(1L, TimeUnit.SECONDS));
        store.update(input2, target);
        //sanity check
        assertEquals("bar", store.get(staticKey, target).getValue());
        assertEquals("bar", store.get(dynamicKey, target).getValue());
        Thread.sleep(1500);
        //purging without specifying a dynamic component will purge all keys with dynamic components
        store.purgeExpired(staticKey, target);
        assertTrue(store.get(staticKey, target).isEmpty());
        assertTrue(store.get(dynamicKey, target).isEmpty());

        store.set(input1, target);
        store.update(input2, target);
        Thread.sleep(1500);
        //purging with a dynamic component will purge only keys that match that dynamic component
        store.purgeExpired(dynamicKey, target);
        assertTrue(store.get(dynamicKey, target).isEmpty());
        assertEquals("bar", store.get(staticKey, target).getValue());
    }

    @Test
    public void testDeleteData() throws Exception {
        EphemeralLocation location1 = new TestLocation("location1");
        EphemeralLocation location2 = new TestLocation("location2");
        EphemeralKey key1 = new EphemeralKey("attr1");
        EphemeralInput input1 = new EphemeralInput(key1, "value1");
        EphemeralKey key2 = new EphemeralKey("attr2");
        EphemeralInput input2 = new EphemeralInput(key2, "value2");
        store.set(input1, location1);
        store.set(input2, location2);
        store.deleteData(location1);
        assertTrue(store.get(key1, location1).isEmpty());
        assertEquals(store.get(key2, location2).getValue(), "value2");
    }

    @Test
    public void testHas() throws Exception {
        EphemeralLocation target = new TestLocation();

        EphemeralKey staticKey = new EphemeralKey("foo");
        EphemeralKey dynamicKey1 = new EphemeralKey("foo", "1");
        EphemeralKey dynamicKey2 = new EphemeralKey("foo", "2");

        store.set(new EphemeralInput(staticKey, "bar"), target);
        store.update(new EphemeralInput(dynamicKey1, "bar"), target);
        store.update(new EphemeralInput(dynamicKey2, "bar"), target);

        assertTrue(store.has(staticKey, target));
        assertTrue(store.has(dynamicKey1, target));
        assertTrue(store.has(dynamicKey2, target));
        assertFalse(store.has(new EphemeralKey("foo", "3"), target));

        //delete one of the dynamic keys
        store.delete(dynamicKey1, "bar", target);
        assertFalse(store.has(dynamicKey1, target));
        assertTrue(store.has(dynamicKey2, target));
    }

    @Test
    public void testDataTypes() throws Exception {
        EphemeralLocation target = new TestLocation();
        Integer intValue = 1;
        Boolean boolValue = true;
        Long longValue = 10000000000L;
        store.set(new EphemeralInput(new EphemeralKey("integer"), intValue), target);
        store.set(new EphemeralInput(new EphemeralKey("boolean"), boolValue), target);
        store.set(new EphemeralInput(new EphemeralKey("long"), longValue), target);
        assertEquals(intValue, store.get(new EphemeralKey("integer"), target).getIntValue());
        assertEquals(boolValue, store.get(new EphemeralKey("boolean"), target).getBoolValue());
        assertEquals(longValue, store.get(new EphemeralKey("long"), target).getLongValue());
    }

    @Test
    public void testIncorrectDataTypes() throws Exception {
        EphemeralKey key = new EphemeralKey("foo");
        EphemeralLocation target = new TestLocation();
        store.set(new EphemeralInput(key, "bar"), target);
        store.update(new EphemeralInput(key, "1"), target);
        store.update(new EphemeralInput(key, "true"), target);
        EphemeralResult result = store.get(key, target);

        assertEquals((Integer) null, result.getIntValue());
        assertEquals(new Integer(1), result.getIntValue(1));
        assertArrayEquals(new Integer[] {null, 1, null}, result.getIntValues());
        assertArrayEquals(new Integer[] {0, 1, 0}, result.getIntValues(0));

        assertEquals((Boolean) null, store.get(key, target).getBoolValue());
        assertEquals(true, store.get(key, target).getBoolValue(true));
        assertArrayEquals(new Boolean[] {null, null, true}, result.getBoolValues());
        assertArrayEquals(new Boolean[] {false, false, true}, result.getBoolValues(false));


        assertEquals((Long) null, store.get(key, target).getLongValue());
        assertEquals(new Long(1), store.get(key, target).getLongValue(1L));
        assertArrayEquals(new Long[] {null, 1L, null}, result.getLongValues());
        assertArrayEquals(new Long[] {0L, 1L, 0L}, result.getLongValues(0L));
    }

    @Test
    public void testDifferentLocation() throws Exception {
        EphemeralKey key = new EphemeralKey("foo");
        EphemeralLocation target = new TestLocation();
        store.set(new EphemeralInput(key, "bar"), target);

        EphemeralLocation differentLocation = new EphemeralLocation() {

            @Override
            public String[] getLocation() { return new String[] { "different" }; }
        };

        assertTrue(store.get(key, differentLocation).isEmpty());
    }

    @Test
    public void testDefaults() throws Exception {
        EphemeralKey key = new EphemeralKey("foo");
        EphemeralResult result = new EphemeralResult(key, (String) null);
        assertEquals(null, result.getValue());
        assertEquals("bar", result.getValue("bar"));
        assertEquals(null, result.getIntValue());
        assertEquals((Integer) 1, result.getIntValue(1));
        assertEquals(null, result.getBoolValue());
        assertEquals(true, result.getBoolValue(true));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAttrs() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.createAccount("user1", "test123", new HashMap<String, Object>());
        Date date = new Date();
        acct.setLastLogonTimestamp(date);
        Map<String, Object> attrs = acct.getAttrs(false, true);
        assertEquals(LdapDateUtil.toGeneralizedTime(date), attrs.get(Provisioning.A_zimbraLastLogonTimestamp));
    }

    @Test
    public void testFallbackEphemeralStore() throws Exception {

        EphemeralStore primary = new InMemoryEphemeralStore();
        EphemeralStore secondary = new InMemoryEphemeralStore();
        EphemeralStore store = new FallbackEphemeralStore(primary, secondary);

        EphemeralLocation target = new TestLocation();
        EphemeralKey key1 = new EphemeralKey("foo1");
        EphemeralKey key2 = new EphemeralKey("foo2");
        EphemeralInput input1 = new EphemeralInput(key1, "bar1");
        input1.setExpiration(new AbsoluteExpiration(1000L));
        EphemeralInput input2 = new EphemeralInput(key2, "bar2");
        input2.setExpiration(new AbsoluteExpiration(1000L));

        //add a value via the fallback store
        primary.set(input1, target);
        //make sure it went to the primary store
        assertEquals("bar1", primary.get(key1, target).getValue());
        //but not the secondary store
        assertTrue(secondary.get(key1, target).isEmpty());

        //add a value to the secondary store
        secondary.set(input2, target);

        //make sure get() and has() work for values in both primary and secondary stores
        assertTrue(store.has(key1, target));
        assertTrue(store.has(key2, target));
        assertEquals("bar1", store.get(key1, target).getValue());
        assertEquals("bar2", store.get(key2, target).getValue());

        //deleting via fallback store should delete from both primary and secondary
        store.delete(key1, "bar1", target);
        assertFalse(primary.has(key1, target));
        store.delete(key2, "bar2", target);
        assertFalse(secondary.has(key2, target));

        //purging should purge from both primary and secondary as well
        primary.set(input1, target);
        secondary.set(input1, target);
        store.purgeExpired(key1, target);
        assertFalse(primary.has(key1, target));
        assertFalse(secondary.has(key1, target));
    }

    static class TestLocation extends EphemeralLocation {

        private String locationName;

        public TestLocation(String locationName) {
            this.locationName = locationName;
        }

        public TestLocation() {
            this("test");
        }

        @Override
        public String[] getLocation() {
            return new String[] { locationName };
        }
    }
}
