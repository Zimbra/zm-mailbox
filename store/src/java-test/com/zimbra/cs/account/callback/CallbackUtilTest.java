/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.util.Zimbra;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(JUnitParamsRunner.class)
public class CallbackUtilTest {

    private static Object[] validTestData() {
        return new Object[] {
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 7,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 5,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 9,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 10,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 7,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 5,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 9,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 10,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 7,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 5,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 9,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 10,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)}
        };
    }

    private static Object[] invalidTestData() {
        return new Object[] {
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 6,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorCodeLength, 8,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 8,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 6,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)},
                new Object[] {Provisioning.A_zimbraTwoFactorScratchCodeLength, 8,
                        createAttrsMap(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6, 8)}
        };
    }

    private static Map<String, Integer> createAttrsMap(String attrName, int value1, int value2) {
        Map<String, Integer> attrs = new HashMap<>();
        switch (attrName) {
            case Provisioning.A_zimbraTwoFactorCodeLength:
                attrs.put(Provisioning.A_zimbraTwoFactorScratchCodeLength, value1);
                attrs.put(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, value2);
                break;
            case Provisioning.A_zimbraTwoFactorAuthEmailCodeLength:
                attrs.put(Provisioning.A_zimbraTwoFactorCodeLength, value1);
                attrs.put(Provisioning.A_zimbraTwoFactorScratchCodeLength, value2);
                break;
            case Provisioning.A_zimbraTwoFactorScratchCodeLength:
                attrs.put(Provisioning.A_zimbraTwoFactorCodeLength, value1);
                attrs.put(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength, value2);
                break;
        }
        return attrs;
    }

    @BeforeClass
    public static void init() throws Exception {
        // Full server init (provisioning + DB pool + MailboxManager) so that
        // getSortedMailboxIdList(), which calls MailboxManager.getInstance(),
        // is reachable under the in-memory harness. This is a superset of
        // initProvisioning(), so all provisioning-only tests remain valid.
        MailboxTestUtil.initServer();
    }

    @Test
    @Parameters(method = "validTestData")
    public void validateAttributeValueTest(String attrName, int attrValue, Map<String, Integer> attrs)
            throws ServiceException {
        CallbackUtil.validateTwoFactorAuthAttributeValue(attrName, attrValue, attrs, 10);
    }

    @Test(expected = ServiceException.class)
    @Parameters(method = "invalidTestData")
    public void validateAttributeValueThrowsExceptionTest(String attrName, int attrValue, Map<String, Integer> attrs)
            throws ServiceException {
        CallbackUtil.validateTwoFactorAuthAttributeValue(attrName, attrValue, attrs, 10);
    }

    @Test
    public void validateTwoFactorAuthAttributeValueAboveMaxThrowsInvalidRequest() {
        // Arrange
        Map<String, Integer> attrs = createAttrsMap(Provisioning.A_zimbraTwoFactorCodeLength, 6, 8);

        // Act / Assert - value greater than maxCodeLength is rejected before any other check
        try {
            CallbackUtil.validateTwoFactorAuthAttributeValue(
                    Provisioning.A_zimbraTwoFactorCodeLength, 11, attrs, 10);
            fail("expected ServiceException for value above max code length");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("cannot set above 10"));
        }
    }

    @Test
    public void validateTwoFactorAuthAttributeValueNullAttrsThrowsInvalidRequest() {
        // Act / Assert - null attrs map is rejected
        try {
            CallbackUtil.validateTwoFactorAuthAttributeValue(
                    Provisioning.A_zimbraTwoFactorCodeLength, 5, null, 10);
            fail("expected ServiceException for null attrs map");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("cannot be null or size less than 2"));
        }
    }

    @Test
    public void validateTwoFactorAuthAttributeValueTooFewAttrsThrowsInvalidRequest() {
        // Arrange - only a single sibling attribute provided
        Map<String, Integer> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraTwoFactorScratchCodeLength, 6);

        // Act / Assert
        try {
            CallbackUtil.validateTwoFactorAuthAttributeValue(
                    Provisioning.A_zimbraTwoFactorCodeLength, 5, attrs, 10);
            fail("expected ServiceException for attrs size less than 2");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("size less than 2"));
        }
    }

    @Test
    public void verificationBeforeStartingThreadAttrMismatchReturnsNull() {
        // Arrange - current attribute differs from the expected one this callback handles
        Server result = CallbackUtil.verificationBeforeStartingThread(
                "zimbraExpectedAttr", "zimbraOtherAttr", null, "testOp");

        // Assert - callback bails out early with null
        assertNull("mismatched attribute must short-circuit to null", result);
    }

    @Test
    public void verificationBeforeStartingThreadZimbraNotStartedReturnsNull() {
        // Arrange - attr matches, but Zimbra is not started in the unit harness
        Server result = CallbackUtil.verificationBeforeStartingThread(
                "zimbraSameAttr", "zimbraSameAttr", null, "testOp");

        // Assert - not-started guard returns null
        assertNull("not-started Zimbra must short-circuit to null", result);
    }

    @Test
    public void getTimeIntervalUnknownAttrReturnsProvidedDefault() {
        // Arrange / Act - unknown attr falls back to the previous value via Server.getTimeInterval
        long prev = 5000L;
        long result = CallbackUtil.getTimeInterval("zimbraNoSuchIntervalAttr", prev);

        // Assert - default is returned for an attr with no configured value
        assertEquals(prev, result);
    }

    @Test
    public void logStartupUnknownAttrReturnsTrue() {
        // Act - getAttr returns null for an unset attr, but logStartup still succeeds (no exception)
        boolean result = CallbackUtil.logStartup("zimbraNoSuchAttr");

        // Assert
        assertTrue("logStartup returns true when the local server is reachable", result);
    }

    @Test
    public void isLocalServerLocalServerInstanceReturnsTrue() throws ServiceException {
        // Arrange - the mock's local server compared against itself
        Server local = Provisioning.getInstance().getLocalServer();

        // Act
        boolean result = CallbackUtil.isLocalServer(local);

        // Assert
        assertTrue("the local server must be recognized as local", result);
    }

    /**
     * A server whose id differs from the local server's id must NOT be recognized as local.
     * Kills L113 ({@code return server.getId().equals(local.getId())} replaced with
     * {@code return true}): this case must observably return false.
     */
    @Test
    public void isLocalServerDifferentServerReturnsFalse() throws ServiceException {
        // Arrange — a server with an id guaranteed not to match the local server's id.
        // Server.getId() resolves to the A_zimbraId attribute (ZAttrServer override), not the
        // constructor id arg, so the id must be supplied via the attrs map.
        Map<String, Object> otherAttrs = new HashMap<String, Object>();
        otherAttrs.put(Provisioning.A_zimbraId, "callbackutil-other-server-id");
        Server other = new Server("callbackutil-other-server", "callbackutil-other-server-id",
                otherAttrs, new HashMap<String, Object>(),
                Provisioning.getInstance());

        // Act
        boolean result = CallbackUtil.isLocalServer(other);

        // Assert
        assertFalse("a non-local server must not be recognized as local", result);
    }

    // ---- appended functional tests covering previously-uncovered branches ----

    // Toggle the package-private Zimbra.sInited flag so the started() guard can be exercised.
    private static void setZimbraStarted(boolean started) throws Exception {
        Field f = Zimbra.class.getDeclaredField("sInited");
        f.setAccessible(true);
        f.setBoolean(null, started);
    }

    @Test
    public void getSortedMailboxIdListReturnsNonNullAscendingList() throws Exception {
        // Act — list of all mailbox ids currently registered in the harness
        List<Integer> ids = CallbackUtil.getSortedMailboxIdList();

        // Assert — non-null and in non-descending order (order-independent of how many
        // other tests have created mailboxes by the time this runs)
        assertNotNull(ids);
        for (int i = 1; i < ids.size(); i++) {
            assertTrue("getSortedMailboxIdList must return ids in ascending order",
                    ids.get(i - 1).intValue() <= ids.get(i).intValue());
        }
    }

    /**
     * Creates several mailboxes (keyed by random UUID account ids, so the underlying
     * {@code MailboxManager.getMailboxIds()} array is effectively unordered) and asserts the
     * returned list is BOTH complete and strictly sorted.
     *
     * <p>Kills L108 ({@code return list} -> empty list: the list must contain every created id) and
     * L107 ({@code Collections.sort(list)} removed: the result must be sorted ascending and equal
     * to an independently-sorted copy of the raw manager ids).
     */
    @Test
    public void getSortedMailboxIdListWithMailboxesIsSortedAndComplete() throws Exception {
        // Arrange — materialize a handful of mailboxes
        Provisioning p = Provisioning.getInstance();
        java.util.List<Integer> created = new java.util.ArrayList<Integer>();
        for (int i = 0; i < 5; i++) {
            Account a = p.createAccount("sortmbox-" + java.util.UUID.randomUUID() + "@example.com",
                    "test123", new HashMap<String, Object>());
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(a);
            created.add(Integer.valueOf(mbox.getId()));
        }

        // Act
        List<Integer> ids = CallbackUtil.getSortedMailboxIdList();

        // Assert — non-empty and contains every id we just created (kills empty-return mutation)
        assertFalse("populated harness must yield a non-empty id list", ids.isEmpty());
        assertTrue("returned list must contain every created mailbox id",
                ids.containsAll(created));

        // Assert — strictly the sorted form of the raw manager ids (kills removed-sort mutation)
        int[] raw = MailboxManager.getInstance().getMailboxIds();
        java.util.List<Integer> expected = new java.util.ArrayList<Integer>();
        for (int r : raw) {
            expected.add(Integer.valueOf(r));
        }
        java.util.Collections.sort(expected);
        assertEquals("getSortedMailboxIdList must equal the sorted manager id list", expected, ids);
        for (int i = 1; i < ids.size(); i++) {
            assertTrue("ids must be in ascending order",
                    ids.get(i - 1).intValue() <= ids.get(i).intValue());
        }
    }

    @Test
    public void verificationBeforeStartingThreadStartedWithMailboxServiceReturnsLocalServer() throws Exception {
        // Arrange — mark Zimbra started and ensure the local server advertises the mailbox service
        Server local = Provisioning.getInstance().getLocalServer();
        Map<String, Object> saved = new HashMap<String, Object>(local.getAttrs());
        Map<String, Object> mutated = new HashMap<String, Object>(local.getAttrs());
        mutated.put(Provisioning.A_zimbraServiceEnabled, new String[] {"mailbox"});
        local.setAttrs(mutated);
        setZimbraStarted(true);
        try {
            // Act — matching attr, started, mailbox service present, non-Server entry
            Server result = CallbackUtil.verificationBeforeStartingThread(
                    "zimbraSameAttr", "zimbraSameAttr", null, "testOp");

            // Assert — the local server is returned
            assertNotNull("started + mailbox service must yield the local server", result);
            assertSame(local, result);
        } finally {
            // Cleanup — restore global state for other tests
            setZimbraStarted(false);
            local.setAttrs(saved);
        }
    }

    @Test
    public void verificationBeforeStartingThreadStartedWithoutMailboxServiceReturnsNull() throws Exception {
        // Arrange — started but the local server does NOT advertise the mailbox service
        Server local = Provisioning.getInstance().getLocalServer();
        Map<String, Object> saved = new HashMap<String, Object>(local.getAttrs());
        Map<String, Object> mutated = new HashMap<String, Object>(local.getAttrs());
        mutated.put(Provisioning.A_zimbraServiceEnabled, new String[] {"ldap"});
        local.setAttrs(mutated);
        setZimbraStarted(true);
        try {
            // Act
            Server result = CallbackUtil.verificationBeforeStartingThread(
                    "zimbraSameAttr", "zimbraSameAttr", null, "testOp");

            // Assert — no mailbox service short-circuits to null
            assertNull("missing mailbox service must short-circuit to null", result);
        } finally {
            setZimbraStarted(false);
            local.setAttrs(saved);
        }
    }

    @Test
    public void verificationBeforeStartingThreadServerEntryMatchingLocalReturnsLocalServer() throws Exception {
        // Arrange — started, mailbox service, and the entry IS the local server (same reference)
        Server local = Provisioning.getInstance().getLocalServer();
        Map<String, Object> saved = new HashMap<String, Object>(local.getAttrs());
        Map<String, Object> mutated = new HashMap<String, Object>(local.getAttrs());
        mutated.put(Provisioning.A_zimbraServiceEnabled, new String[] {"mailbox"});
        local.setAttrs(mutated);
        setZimbraStarted(true);
        try {
            // Act — passing the local server itself takes the matching-id branch
            Server result = CallbackUtil.verificationBeforeStartingThread(
                    "zimbraSameAttr", "zimbraSameAttr", local, "testOp");

            // Assert
            assertNotNull(result);
            assertSame(local, result);
        } finally {
            setZimbraStarted(false);
            local.setAttrs(saved);
        }
    }

    @Test
    public void verificationBeforeStartingThreadDifferentServerEntryReturnsNull() throws Exception {
        // Arrange — started, mailbox service, but the entry is a DIFFERENT server (id mismatch)
        Server local = Provisioning.getInstance().getLocalServer();
        Map<String, Object> saved = new HashMap<String, Object>(local.getAttrs());
        Map<String, Object> mutated = new HashMap<String, Object>(local.getAttrs());
        mutated.put(Provisioning.A_zimbraServiceEnabled, new String[] {"mailbox"});
        local.setAttrs(mutated);
        setZimbraStarted(true);
        try {
            Server other = new Server("other-server", "other-server-id",
                    new HashMap<String, Object>(), new HashMap<String, Object>(),
                    Provisioning.getInstance());

            // Act — wrong server short-circuits to null
            Server result = CallbackUtil.verificationBeforeStartingThread(
                    "zimbraSameAttr", "zimbraSameAttr", other, "testOp");

            // Assert
            assertNull("a non-local server entry must short-circuit to null", result);
        } finally {
            setZimbraStarted(false);
            local.setAttrs(saved);
        }
    }
}
