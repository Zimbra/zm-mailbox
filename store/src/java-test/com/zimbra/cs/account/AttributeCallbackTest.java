/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback.MultiValueMod;
import com.zimbra.cs.account.AttributeCallback.SingleValueMod;
import com.zimbra.cs.account.callback.CallbackContext;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for the protected helpers of {@link AttributeCallback}. A concrete subclass
 * ({@link TestCallback}) exposes the protected mod-analysis helpers and records pre/postModify
 * invocations so callback firing can be asserted. The {@code newValuesToBe} helper is exercised
 * against a real {@link Account} from the in-memory harness so {@code getMultiAttrSet} reflects
 * actual persisted multi-valued state.
 */
public class AttributeCallbackTest {

    /** Real concrete callback that exposes the protected helpers and records invocations. */
    private static class TestCallback extends AttributeCallback {
        private int preCount;

        private int postCount;

        private String lastAttr;

        @Override
        public void preModify(CallbackContext context, String attrName, Object attrValue,
                Map attrsToModify, Entry entry) throws ServiceException {
            preCount++;
            lastAttr = attrName;
        }

        @Override
        public void postModify(CallbackContext context, String attrName, Entry entry) {
            postCount++;
            lastAttr = attrName;
        }

        SingleValueMod single(String attrName, Object value) throws ServiceException {
            return singleValueMod(attrName, value);
        }

        SingleValueMod single(Map attrs, String attrName) throws ServiceException {
            return singleValueMod(attrs, attrName);
        }

        MultiValueMod multi(Map attrs, String attrName) throws ServiceException {
            return multiValueMod(attrs, attrName);
        }

        List<String> multiValue(Object value) throws ServiceException {
            return getMultiValue(value);
        }

        Set<String> multiValueSet(Object value) throws ServiceException {
            return getMultiValueSet(value);
        }

        Set<String> newValues(MultiValueMod mod, Entry entry, String attrName) {
            return newValuesToBe(mod, entry, attrName);
        }
    }

    private TestCallback cb;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        cb = new TestCallback();
        prov = Provisioning.getInstance();
    }

    @Test
    public void preAndPostModifyInvokedRecordsCallbackFiring() throws Exception {
        // Act — drive the abstract contract directly
        cb.preModify(new CallbackContext(CallbackContext.Op.MODIFY), "displayName", "Bob",
                new HashMap(), null);
        cb.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "displayName", null);

        // Assert
        assertEquals("preModify should fire once", 1, cb.preCount);
        assertEquals("postModify should fire once", 1, cb.postCount);
        assertEquals("displayName", cb.lastAttr);
    }

    @Test
    public void singleValueModStringValueReportsSetting() throws Exception {
        // Act
        SingleValueMod svm = cb.single("displayName", "Bob");

        // Assert
        assertTrue("non-empty string => SETTING", svm.setting());
        assertFalse(svm.unsetting());
        assertEquals("Bob", svm.value());
    }

    @Test
    public void singleValueModNullValueReportsUnsetting() throws Exception {
        // Act
        SingleValueMod svm = cb.single("displayName", null);

        // Assert
        assertTrue("null => UNSETTING", svm.unsetting());
        assertFalse(svm.setting());
    }

    @Test
    public void singleValueModEmptyStringReportsUnsetting() throws Exception {
        // Act
        SingleValueMod svm = cb.single("displayName", "");

        // Assert
        assertTrue("empty string => UNSETTING", svm.unsetting());
    }

    @Test
    public void singleValueModNonStringValueThrowsInvalidRequest() throws Exception {
        // Act / Assert
        try {
            cb.single("displayName", Integer.valueOf(5));
            fail("expected ServiceException for non-String single value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("single-valued"));
        }
    }

    @Test
    public void singleValueModFromMapMinusPrefixReportsUnsetting() throws Exception {
        // Arrange — "-attr" wins immediately
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("-displayName", "anything");

        // Act
        SingleValueMod svm = cb.single(attrs, "displayName");

        // Assert
        assertTrue("-attr present => UNSETTING", svm.unsetting());
    }

    @Test
    public void singleValueModFromMapPlusPrefixStringArrayReportsSetting() throws Exception {
        // Arrange — value supplied via +attr as a single-element array
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("+displayName", new String[] {"Alice"});

        // Act
        SingleValueMod svm = cb.single(attrs, "displayName");

        // Assert
        assertTrue("single-element array => SETTING", svm.setting());
        assertEquals("Alice", svm.value());
    }

    @Test
    public void singleValueModFromMapMultiElementArrayThrowsInvalidRequest() throws Exception {
        // Arrange — multi-element array for a single-valued attr is invalid
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("displayName", new String[] {"a", "b"});

        // Act / Assert
        try {
            cb.single(attrs, "displayName");
            fail("expected ServiceException for multi-element single-valued attr");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("single-valued"));
        }
    }

    @Test
    public void singleValueModFromMapAbsentReportsUnsetting() throws Exception {
        // Act — attribute not present at all
        SingleValueMod svm = cb.single(new HashMap(), "displayName");

        // Assert
        assertTrue("absent attr => UNSETTING", svm.unsetting());
    }

    @Test
    public void multiValueModPlainValueReportsReplacing() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "a@x.com");

        // Act
        MultiValueMod mvm = cb.multi(attrs, Provisioning.A_zimbraMailAlias);

        // Assert
        assertTrue("plain value => REPLACING", mvm.replacing());
        assertEquals(1, mvm.values().size());
        assertTrue(mvm.values().contains("a@x.com"));
    }

    @Test
    public void multiValueModPlusPrefixReportsAdding() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("+" + Provisioning.A_zimbraMailAlias, new String[] {"x@x.com", "y@x.com"});

        // Act
        MultiValueMod mvm = cb.multi(attrs, Provisioning.A_zimbraMailAlias);

        // Assert
        assertTrue("+attr => ADDING", mvm.adding());
        assertEquals(2, mvm.valuesSet().size());
    }

    @Test
    public void multiValueModMinusPrefixReportsRemoving() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("-" + Provisioning.A_zimbraMailAlias, "old@x.com");

        // Act
        MultiValueMod mvm = cb.multi(attrs, Provisioning.A_zimbraMailAlias);

        // Assert
        assertTrue("-attr => REMOVING", mvm.removing());
        assertTrue(mvm.values().contains("old@x.com"));
    }

    @Test
    public void multiValueModEmptyStringValueReportsDeleting() throws Exception {
        // Arrange — empty string value means delete all
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "");

        // Act
        MultiValueMod mvm = cb.multi(attrs, Provisioning.A_zimbraMailAlias);

        // Assert
        assertTrue("empty string => DELETING", mvm.deleting());
        assertTrue("DELETING carries no values", mvm.values().isEmpty());
    }

    @Test
    public void multiValueModAttrNotPresentReturnsNull() throws Exception {
        // Act — attribute completely absent
        MultiValueMod mvm = cb.multi(new HashMap(), Provisioning.A_zimbraMailAlias);

        // Assert
        assertNull("absent attr yields null mod", mvm);
    }

    @Test
    public void getMultiValueCollectionReturnsAllStringified() throws Exception {
        // Act
        List<String> list = cb.multiValue(Arrays.asList("a", "b", "c"));

        // Assert
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    public void getMultiValueUnsupportedTypeThrowsInvalidRequest() throws Exception {
        // Act / Assert
        try {
            cb.multiValue(Integer.valueOf(3));
            fail("expected ServiceException for unsupported value type");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("String"));
        }
    }

    @Test
    public void getMultiValueSetStringArrayDedupesIntoSet() throws Exception {
        // Act
        Set<String> set = cb.multiValueSet(new String[] {"a", "a", "b"});

        // Assert
        assertEquals("duplicates collapse in a set", 2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
    }

    @Test
    public void newValuesToBeAddingOnExistingEntryUnionsWithCurrentValues() throws Exception {
        // Arrange — real account already holding one alias value
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "existing@x.com");
        Account acct = prov.createAccount("mvbase@zimbra.com", "secret", attrs);

        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put("+" + Provisioning.A_zimbraMailAlias, "new@x.com");
        MultiValueMod mvm = cb.multi(mod, Provisioning.A_zimbraMailAlias);

        // Act
        Set<String> result = cb.newValues(mvm, acct, Provisioning.A_zimbraMailAlias);

        // Assert — union of existing + added
        assertTrue("existing value retained", result.contains("existing@x.com"));
        assertTrue("added value present", result.contains("new@x.com"));
        assertEquals(2, result.size());
    }

    @Test
    public void newValuesToBeNullEntryAndNullModReturnsEmptySet() throws Exception {
        // Act — create-time with no mod
        Set<String> result = cb.newValues(null, null, Provisioning.A_zimbraMailAlias);

        // Assert
        assertTrue("null entry + null mod => empty set", result.isEmpty());
    }

    @Test
    public void newValuesToBeDeletingOnEntryReturnsEmptySet() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "x@x.com");
        Account acct = prov.createAccount("mvdel@zimbra.com", "secret", attrs);

        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(Provisioning.A_zimbraMailAlias, "");   // delete all
        MultiValueMod mvm = cb.multi(mod, Provisioning.A_zimbraMailAlias);

        // Act
        Set<String> result = cb.newValues(mvm, acct, Provisioning.A_zimbraMailAlias);

        // Assert
        assertTrue("DELETING => resulting set is empty", result.isEmpty());
    }
}
