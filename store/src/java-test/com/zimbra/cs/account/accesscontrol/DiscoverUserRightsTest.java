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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DiscoverUserRights}. The {@code handle()} flow drives a real
 * {@link SearchGrants} which requires an LDAP-backed provisioning to search grants, so it is not
 * reachable under the in-memory mock. These tests therefore exercise the package-private
 * constructor's reachable logic: it rejects an empty right set with a {@code FAILURE}
 * {@link ServiceException}, defensively copies the supplied right set (so later mutation of the
 * caller's set does not leak in), and records the credentials/onMaster flag. Fields are read by
 * reflection to verify the captured state. Real {@link Account} entries and {@code User.R_loginAs}
 * (loaded by {@link RightManager}) come from the {@link MailboxTestUtil#initServer()} harness.
 */
public class DiscoverUserRightsTest {

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        RightManager.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String email) throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createAccount(email, "test123", attrs);
    }

    @SuppressWarnings("unchecked")
    private Set<Right> readRights(DiscoverUserRights dur) throws Exception {
        Field f = DiscoverUserRights.class.getDeclaredField("rights");
        f.setAccessible(true);
        return (Set<Right>) f.get(dur);
    }

    private Account readAcct(DiscoverUserRights dur) throws Exception {
        Field f = DiscoverUserRights.class.getDeclaredField("acct");
        f.setAccessible(true);
        return (Account) f.get(dur);
    }

    private boolean readOnMaster(DiscoverUserRights dur) throws Exception {
        Field f = DiscoverUserRights.class.getDeclaredField("onMaster");
        f.setAccessible(true);
        return f.getBoolean(dur);
    }

    @Test
    public void constructorEmptyRightsThrowsFailure() throws Exception {
        // Arrange
        Account acct = createAccount("dur-empty@example.com");
        Set<Right> empty = new HashSet<>();

        // Act / Assert — guard rejects an empty right set
        try {
            new DiscoverUserRights(acct, empty, false);
            fail("expected ServiceException for empty rights");
        } catch (ServiceException e) {
            assertTrue("no right specified message",
                    e.getMessage().toLowerCase().contains("no right"));
        }
    }

    @Test
    public void constructorSingleRightCapturesCredentialsAndRight() throws Exception {
        // Arrange
        Account acct = createAccount("dur-single@example.com");
        Set<Right> rights = new HashSet<>();
        rights.add(User.R_loginAs);

        // Act
        DiscoverUserRights dur = new DiscoverUserRights(acct, rights, false);

        // Assert — credentials and right captured
        assertEquals(acct.getId(), readAcct(dur).getId());
        assertTrue("loginAs retained", readRights(dur).contains(User.R_loginAs));
        assertEquals(1, readRights(dur).size());
    }

    @Test
    public void constructorCopiesRightSetCallerMutationDoesNotLeak() throws Exception {
        // Arrange
        Account acct = createAccount("dur-copy@example.com");
        Set<Right> rights = new HashSet<>();
        rights.add(User.R_loginAs);

        // Act — ctor does Sets.newHashSet(rights); mutate the caller's set afterwards
        DiscoverUserRights dur = new DiscoverUserRights(acct, rights, false);
        rights.clear();

        // Assert — internal copy is independent of the caller's set
        assertNotSame("must be a defensive copy", rights, readRights(dur));
        assertEquals("internal copy unaffected by caller clear", 1, readRights(dur).size());
        assertTrue(readRights(dur).contains(User.R_loginAs));
    }

    @Test
    public void constructorOnMasterTrueRecordsFlag() throws Exception {
        // Arrange
        Account acct = createAccount("dur-master@example.com");
        Set<Right> rights = new HashSet<>();
        rights.add(User.R_loginAs);

        // Act
        DiscoverUserRights dur = new DiscoverUserRights(acct, rights, true);

        // Assert
        assertTrue("onMaster captured", readOnMaster(dur));
    }

    @Test
    public void constructorOnMasterFalseRecordsFlag() throws Exception {
        // Arrange
        Account acct = createAccount("dur-replica@example.com");
        Set<Right> rights = new HashSet<>();
        rights.add(User.R_loginAs);

        // Act
        DiscoverUserRights dur = new DiscoverUserRights(acct, rights, false);

        // Assert
        assertFalse("onMaster=false captured", readOnMaster(dur));
    }

    @Test
    public void constructorMultipleRightsAllRetained() throws Exception {
        // Arrange — two distinct user rights from the loaded right manager
        Account acct = createAccount("dur-multi@example.com");
        Set<Right> rights = new HashSet<>();
        rights.add(User.R_loginAs);
        rights.add(User.R_sendAs);

        // Act
        DiscoverUserRights dur = new DiscoverUserRights(acct, rights, false);

        // Assert — both rights captured in the internal copy
        assertEquals(2, readRights(dur).size());
        assertTrue(readRights(dur).contains(User.R_loginAs));
        assertTrue(readRights(dur).contains(User.R_sendAs));
    }
}
