package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralStoreTest.TestLocation;
import com.zimbra.cs.ephemeral.LdapEphemeralStore.AbstractLdapHelper;


/**
 * Tests the LdapEphemeralStore backend by verifying the LDAP attribute map
 * that would be sent to Provisioning.modifyAttrs() by the ZimbraLdapHelper.
 *
 * @author iraykin
 *
 */
public class LdapEphemeralStoreTest {
    private EphemeralStore store;
    private MockLdapHelper helper;

    @Before
    public void setUp() throws Exception {
        helper = new MockLdapHelper();
        store = new LdapEphemeralStore(helper);
    }

    @Test
    public void testGet() throws Exception {
        EphemeralKey key = new EphemeralKey("foo");
        store.set(new EphemeralInput(key, "bar"), new TestLocation());
        EphemeralResult result = store.get(key, new TestLocation());
        assertEquals("bar", result.getValue());
    }

    @Test
    public void testSet() throws Exception {
        EphemeralKey key = new EphemeralKey("foo");
        store.set(new EphemeralInput(new EphemeralKey("foo"), "bar"), new TestLocation());
        Map<String, Object> expected = makeMap("foo", "bar");
        verifyAttrMap(expected);
    }

    @Test
    public void testUpdate() throws Exception {
        store.update(new EphemeralInput(new EphemeralKey("foo"), "bar"), new TestLocation());
        Map<String, Object> expected = makeMap("+foo", "bar");
        verifyAttrMap(expected);
    }

    @Test
    public void testDelete() throws Exception {
        //delete one value
        EphemeralLocation location = new TestLocation();
        store.set(new EphemeralInput(new EphemeralKey("foo"), "bar"), location);
        helper.reset();
        store.delete(new EphemeralKey("foo"), "bar", location);
        Map<String, Object> expected = makeMap("-foo", "bar");
        verifyAttrMap(expected);
        helper.reset();

        //delete one value from several
        store.set(new EphemeralInput(new EphemeralKey("foo"), "bar"), location);
        store.update(new EphemeralInput(new EphemeralKey("foo"), "baz"), location);
        helper.reset();
        store.delete(new EphemeralKey("foo"), "bar", location);
        expected = makeMap("-foo", "bar");
        verifyAttrMap(expected);
    }

    @Test
    public void testExpiry() throws Exception {
        EphemeralLocation location = new TestLocation();
        EphemeralInput input = new EphemeralInput(new EphemeralKey("foo"), "bar");
        input.setExpiration(new AbsoluteExpiration(1000L));
        store.set(input, location);
        input = new EphemeralInput(new EphemeralKey("foo", "1"), "bar");
        input.setExpiration(new AbsoluteExpiration(1000L));
        store.set(input, location);
        input = new EphemeralInput(new EphemeralKey("foo", "2"), "bar");
        input.setExpiration(new AbsoluteExpiration(1000L));
        store.set(input, location);
        helper.reset();
        Thread.sleep(1500);
        store.purgeExpired(new EphemeralKey("foo"), location);
        Map<String, Object> expected = makeMap("-foo", "bar||1000", "bar|1|1000", "bar|2|1000");
        verifyAttrMap(expected);
    }

    @Test
    public void testHasKey() throws Exception {
        EphemeralLocation target = new TestLocation();
        store.set(new EphemeralInput(new EphemeralKey("foo"), "bar"), target);
        assertTrue(store.has(new EphemeralKey("foo"), target));
    }

    private Map<String, Object> makeMap(String key, String... values) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String v: values) {
            StringUtil.addToMultiMap(map, key, v);
        }
        return map;
    }

    private void verifyAttrMap(Map<String, Object> expected) throws Exception {
        for (Map.Entry<String, Object> entry: expected.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                assertEquals(value, helper.getAttrs().get(key));
            } else if (value instanceof String[]) {
                String[] values = (String[]) value;
                String[] helperValues = (String[]) helper.getAttrs().get(key);
                assertEquals(values.length, helperValues.length);
                for (int i = 0; i < values.length; i++) {
                    assertEquals(values[i], helperValues[i]);
                }
            }
        }
    }

    /**
     * helper class that stores data in an in-memory ephemeral store,
     * while keeping track of the attribute modification map that would
     * otherwise be used to modify LDAP values with Provisioning.modifyAttrs()
     */
    static class MockLdapHelper extends AbstractLdapHelper {
        private InMemoryEphemeralStore store;
        private EphemeralLocation location;
        private AttributeEncoder encoder;

        public MockLdapHelper() {
            store = new InMemoryEphemeralStore();
            store.setAttributeEncoder(new DummyAttributeEncoder());
            encoder = new DynamicExpirationEncoder();
        }

        public Map<String, Object> getAttrs() {
            return attrs;
        }

        @Override
        void setLocation(EphemeralLocation location) throws ServiceException {
            this.location = location;
        }

        @Override
        String getAttr(String key) throws ServiceException {
            return store.get(new EphemeralKey(key), location).getValue();
        }

        @Override
        String[] getMultiAttr(String key) throws ServiceException {
            return store.get(new EphemeralKey("foo"), location).getValues();
        }

        private String[] objectToStringArray(Object o) throws ServiceException {
            String[] arr;
            if (o instanceof String) {
                arr = new String[] {(String) o};
            } else if (o instanceof String[]) {
                arr = (String[]) o;
            } else {
                throw ServiceException.FAILURE("unexpected value type", null);
            }
            return arr;
        }

        @Override
        void executeChange() throws ServiceException {
            for (Map.Entry<String, Object> kv: attrs.entrySet()) {
                String key = kv.getKey();
                String[] values = objectToStringArray(kv.getValue());
                if ((key.startsWith("+") || !key.startsWith("-")) && values.length > 0) {
                    String firstValue = values[0];
                    EphemeralInput input = new EphemeralInput(new EphemeralKey("foo"), firstValue);
                    if (key.startsWith("+")) {
                        store.update(input, location);
                    } else {
                        store.set(input, location);
                    }
                    // if more than one value is provided, it has to be an update
                    if (values.length > 1) {
                        for (int i = 1; i < values.length; i++) {
                            input = new EphemeralInput(new EphemeralKey("foo"), values[i]);
                            store.update(input, location);
                        }
                    }
                } else {
                    for (String v: values) {
                        EphemeralKeyValuePair kvPair = encoder.decode(key.substring(1), v);
                        store.delete(kvPair.getKey(), kvPair.getValue(), location);
                    }
                }
            }
        // don't reset attrs, since we care about what the internal attribute change map looks like
        }
    }

    /**
     * encoder that doesn't do anything with the expiration; needed for inner InMemoryEphemeralStore
     * used by MockLdapHelper, so that the encoded value can be extracted from the memory store
     * without being decoded prematurely
     */
    static class DummyAttributeEncoder extends AttributeEncoder {

        public DummyAttributeEncoder() {
            setKeyEncoder(new StaticKeyEncoder());
            setValueEncoder(new ValueEncoder() {

                @Override
                public String encodeValue(EphemeralInput input, EphemeralLocation target) {
                    return String.valueOf(input.getValue());
                }
            });
        }

        @Override
        public EphemeralKeyValuePair decode(String key, String value) throws ServiceException {
            return new EphemeralKeyValuePair(new EphemeralKey(key), value);
        }
    }
}
