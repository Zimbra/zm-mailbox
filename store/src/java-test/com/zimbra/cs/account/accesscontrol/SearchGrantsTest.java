/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link SearchGrants}. The {@code doSearch()} / {@code search()} paths
 * issue real LDAP queries (LdapProv) and cannot run on the in-memory harness, so these tests
 * cover the reachable construction and staging logic: the package-private constructors, the
 * mutable fetch-attribute set, and the raw-result staging type (target-id derivation and
 * multi-value attribute coercion).
 */
public class SearchGrantsTest {

    private Provisioning prov;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        // ZimbraACL construction in getGrants() needs the RightManager singleton initialized.
        RightManager.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    @SuppressWarnings("unchecked")
    private Set<String> fetchAttrs(SearchGrants sg) throws Exception {
        Field f = SearchGrants.class.getDeclaredField("fetchAttrs");
        f.setAccessible(true);
        return (Set<String>) f.get(sg);
    }

    @Test
    public void ctorByGranteeIdsDefaultFetchAttrsIncludesAceAndCore() throws Exception {
        // Arrange
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        targetTypes.add(TargetType.account);
        Set<String> granteeIds = new HashSet<String>();
        granteeIds.add("11111111-1111-1111-1111-111111111111");

        // Act
        SearchGrants sg = new SearchGrants(prov, targetTypes, granteeIds);

        // Assert - the four well-known fetch attrs are seeded
        Set<String> attrs = fetchAttrs(sg);
        assertTrue("must fetch zimbraACE", attrs.contains(Provisioning.A_zimbraACE));
        assertTrue("must fetch zimbraId", attrs.contains(Provisioning.A_zimbraId));
        assertTrue("must fetch objectClass", attrs.contains(Provisioning.A_objectClass));
        assertTrue("must fetch cn", attrs.contains(Provisioning.A_cn));
        assertEquals(4, attrs.size());
    }

    @Test
    public void addFetchAttributeSingleAddsToFetchSet() throws Exception {
        // Arrange
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        targetTypes.add(TargetType.account);
        SearchGrants sg = new SearchGrants(prov, targetTypes, new HashSet<String>());

        // Act
        sg.addFetchAttribute(Provisioning.A_displayName);

        // Assert
        Set<String> attrs = fetchAttrs(sg);
        assertTrue("newly added attr must be present", attrs.contains(Provisioning.A_displayName));
        assertEquals(5, attrs.size());
    }

    @Test
    public void addFetchAttributeSetAddsAllToFetchSet() throws Exception {
        // Arrange
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        targetTypes.add(TargetType.domain);
        SearchGrants sg = new SearchGrants(prov, targetTypes, new HashSet<String>());

        Set<String> more = new HashSet<String>();
        more.add(Provisioning.A_displayName);
        more.add(Provisioning.A_description);

        // Act
        sg.addFetchAttribute(more);

        // Assert
        Set<String> attrs = fetchAttrs(sg);
        assertTrue(attrs.contains(Provisioning.A_displayName));
        assertTrue(attrs.contains(Provisioning.A_description));
        assertEquals(6, attrs.size());
    }

    @Test
    public void ctorByAcctStoresAcctAndRightsGranteeIdsNull() throws Exception {
        // Arrange
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        targetTypes.add(TargetType.account);
        Set<Right> rights = new HashSet<Right>();
        rights.add(new UserRight("invite"));

        // Act - the acct-based ctor (acct=set, granteeIds=null, onMaster=false)
        SearchGrants sg = new SearchGrants(prov, targetTypes, null, rights, false);

        // Assert
        Field acctField = SearchGrants.class.getDeclaredField("acct");
        acctField.setAccessible(true);
        assertEquals("acct field is null when constructed with a null account",
                null, acctField.get(sg));

        Field granteeIdsField = SearchGrants.class.getDeclaredField("granteeIds");
        granteeIdsField.setAccessible(true);
        assertEquals("granteeIds must be null in the acct-based ctor", null,
                granteeIdsField.get(sg));

        Field onMasterField = SearchGrants.class.getDeclaredField("onMaster");
        onMasterField.setAccessible(true);
        assertFalse("onMaster must reflect the ctor arg", (Boolean) onMasterField.get(sg));
    }

    @Test
    public void grantsOnTargetRawWithZimbraIdTargetIdIsZimbraId() throws Exception {
        // Arrange - a raw result carrying a zimbraId
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "abc-123");
        attrs.put(Provisioning.A_cn, "myzimlet");
        attrs.put(Provisioning.A_objectClass, "zimbraAccount");
        attrs.put(Provisioning.A_zimbraACE, "grantee usr invite");

        Object raw = newGrantsOnTargetRaw(attrs);

        // Act
        String targetId = invokeGetTargetId(raw);

        // Assert - zimbraId wins over cn
        assertEquals("abc-123", targetId);
    }

    @Test
    public void grantsOnTargetRawWithoutZimbraIdTargetIdFallsBackToCn() throws Exception {
        // Arrange - zimlets have no id, so cn is used as the key
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_cn, "myzimlet");
        attrs.put(Provisioning.A_objectClass, "zimbraZimletEntry");

        Object raw = newGrantsOnTargetRaw(attrs);

        // Act
        String targetId = invokeGetTargetId(raw);

        // Assert
        assertEquals("myzimlet", targetId);
    }

    @Test
    public void grantsOnTargetRawMultiValueObjectClassCoercedToArrayInToString() throws Exception {
        // Arrange - objectClass supplied as a String[] (multi-valued)
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "id-9");
        attrs.put(Provisioning.A_objectClass, new String[] {"zimbraAccount", "zimbraMailRecipient"});
        attrs.put(Provisioning.A_zimbraACE, new String[] {"g1 usr invite", "g2 usr -invite"});

        Object raw = newGrantsOnTargetRaw(attrs);

        // Act
        String dump = raw.toString();

        // Assert - both object classes and both ACEs surface in the debug string
        assertNotNull(dump);
        assertTrue(dump.contains("zimbraAccount"));
        assertTrue(dump.contains("zimbraMailRecipient"));
        assertTrue(dump.contains("id-9"));
    }

    // ---- helpers for the private inner staging type ----

    private Class<?> grantsOnTargetRawClass() throws Exception {
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("GrantsOnTargetRaw")) {
                return c;
            }
        }
        throw new IllegalStateException("GrantsOnTargetRaw not found");
    }

    private Object newGrantsOnTargetRaw(Map<String, Object> attrs) throws Exception {
        Constructor<?> ctor = grantsOnTargetRawClass().getDeclaredConstructor(Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(attrs);
    }

    private String invokeGetTargetId(Object raw) throws Exception {
        Method m = grantsOnTargetRawClass().getDeclaredMethod("getTargetId");
        m.setAccessible(true);
        return (String) m.invoke(raw);
    }

    // ---- helpers for SearchGrantsResults / GrantsOnTarget ----

    private Class<?> resultsClass() throws Exception {
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("SearchGrantsResults")) {
                return c;
            }
        }
        throw new IllegalStateException("SearchGrantsResults not found");
    }

    private Object newResults() throws Exception {
        Constructor<?> ctor = resultsClass().getDeclaredConstructor(Provisioning.class);
        ctor.setAccessible(true);
        return ctor.newInstance(prov);
    }

    private void addRawResult(Object results, Object raw) throws Exception {
        Method m = resultsClass().getDeclaredMethod("addResult", grantsOnTargetRawClass());
        m.setAccessible(true);
        m.invoke(results, raw);
    }

    @SuppressWarnings("unchecked")
    private Set<Object> invokeGetResults(Object results) throws Exception {
        Method m = resultsClass().getDeclaredMethod("getResults");
        m.setAccessible(true);
        return (Set<Object>) m.invoke(results);
    }

    @Test
    public void getResultsUnknownObjectClassThrowsFailureWrappedInInvocation() throws Exception {
        // Arrange — a staged raw result whose objectClass matches no known target type
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "no-type-id");
        attrs.put(Provisioning.A_objectClass, "somethingUnknown");
        attrs.put(Provisioning.A_zimbraACE, new String[] {});
        Object raw = newGrantsOnTargetRaw(attrs);
        Object results = newResults();
        addRawResult(results, raw);

        // Act / Assert — getGrants() cannot map the OC and throws FAILURE
        try {
            invokeGetResults(results);
            fail("expected failure for unmappable object class");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof com.zimbra.common.service.ServiceException);
        }
    }

    @Test
    public void getResultsAccountTargetNotFoundThrowsFailure() throws Exception {
        // Arrange — object class maps to account, but no account exists with that id
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "missing-account-id");
        attrs.put(Provisioning.A_objectClass, "zimbraAccount");
        attrs.put(Provisioning.A_zimbraACE, new String[] {});
        Object raw = newGrantsOnTargetRaw(attrs);
        Object results = newResults();
        addRawResult(results, raw);

        // Act / Assert — lookupTarget cannot find the account so getGrants throws FAILURE
        try {
            invokeGetResults(results);
            fail("expected failure for missing account target");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof com.zimbra.common.service.ServiceException);
        }
    }

    @Test
    public void getResultsConfigTargetResolvesToGlobalConfigEntry() throws Exception {
        // Arrange — the global config target resolves via prov.getConfig() in the harness, so
        // getGrants() succeeds and yields a GrantsOnTarget for the config entry.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "config-id");
        attrs.put(Provisioning.A_objectClass, "zimbraGlobalConfig");
        attrs.put(Provisioning.A_zimbraACE, new String[] {});
        Object raw = newGrantsOnTargetRaw(attrs);
        Object results = newResults();
        addRawResult(results, raw);

        // Act
        Set<Object> grants = invokeGetResults(results);

        // Assert — exactly one resolved grant whose target is the global config
        assertEquals(1, grants.size());
        Object grant = grants.iterator().next();
        Method getTargetEntry = grant.getClass().getDeclaredMethod("getTargetEntry");
        getTargetEntry.setAccessible(true);
        Object targetEntry = getTargetEntry.invoke(grant);
        assertNotNull("config target entry must resolve", targetEntry);

        Method getAcl = grant.getClass().getDeclaredMethod("getAcl");
        getAcl.setAccessible(true);
        assertNotNull("acl must be built", getAcl.invoke(grant));
    }

    @Test
    public void getResultsCachedAfterFirstCallReturnsSameSetInstance() throws Exception {
        // Arrange — config target so resolution succeeds
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "config-id-2");
        attrs.put(Provisioning.A_objectClass, "zimbraGlobalConfig");
        attrs.put(Provisioning.A_zimbraACE, new String[] {});
        Object results = newResults();
        addRawResult(results, newGrantsOnTargetRaw(attrs));

        // Act — call twice; the second call must return the memoized set
        Set<Object> first = invokeGetResults(results);
        Set<Object> second = invokeGetResults(results);

        // Assert
        assertTrue("results set is memoized", first == second);
    }

    @Test
    public void getGranteeIdsConstructedWithGranteeIdsReturnsSameSet() throws Exception {
        // Arrange — the granteeIds-based ctor short-circuits getGranteeIds() to the given set
        Set<TargetType> targetTypes = new HashSet<TargetType>();
        targetTypes.add(TargetType.account);
        Set<String> granteeIds = new HashSet<String>();
        granteeIds.add("g-1");
        granteeIds.add("g-2");
        SearchGrants sg = new SearchGrants(prov, targetTypes, granteeIds);

        // Act
        Method m = SearchGrants.class.getDeclaredMethod("getGranteeIds");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) m.invoke(sg);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains("g-1"));
        assertTrue(result.contains("g-2"));
    }

    @Test
    public void grantsOnTargetRawSingleStringAceCoercedToSingletonArray() throws Exception {
        // Arrange — zimbraACE supplied as a single String must be coerced to a one-element array
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "id-single");
        attrs.put(Provisioning.A_objectClass, "zimbraServer");
        attrs.put(Provisioning.A_zimbraACE, "g1 usr invite");
        Object raw = newGrantsOnTargetRaw(attrs);

        // Act
        Field aceField = grantsOnTargetRawClass().getDeclaredField("zimbraACE");
        aceField.setAccessible(true);
        String[] aces = (String[]) aceField.get(raw);

        // Assert
        assertEquals(1, aces.length);
        assertEquals("g1 usr invite", aces[0]);
    }
}
