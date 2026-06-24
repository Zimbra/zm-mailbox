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
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link RightBearer}. Uses the in-memory MockProvisioning harness to
 * build real {@link Account} domain objects, and exercises the static grantee-validation and
 * grantee-matching logic plus the RightBearer wrapper for a global admin.
 */
public class RightBearerTest {

    private Provisioning prov;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String name, Map<String, Object> attrs) throws Exception {
        return prov.createAccount(name, "test123", attrs);
    }

    @Test
    public void isValidGranteeForAdminRightsDelegatedAdminUserIsValid() throws Exception {
        // Arrange - a delegated (but not global) admin account
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "FALSE");
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        Account acct = createAccount("delegated@example.com", attrs);

        // Act
        boolean valid = RightBearer.isValidGranteeForAdminRights(GranteeType.GT_USER, acct);

        // Assert
        assertTrue("delegated admin (not global admin) is a valid admin-rights grantee", valid);
    }

    @Test
    public void isValidGranteeForAdminRightsGlobalAdminUserIsNotValid() throws Exception {
        // Arrange - a global admin (system admins cannot RECEIVE grants)
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        Account acct = createAccount("globaladmin@example.com", attrs);

        // Act
        boolean valid = RightBearer.isValidGranteeForAdminRights(GranteeType.GT_USER, acct);

        // Assert
        assertFalse("global admin cannot be granted admin rights", valid);
    }

    @Test
    public void isValidGranteeForAdminRightsPlainUserIsNotValid() throws Exception {
        // Arrange - a normal account, neither admin nor delegated admin
        Account acct = createAccount("plain@example.com", new HashMap<String, Object>());

        // Act
        boolean valid = RightBearer.isValidGranteeForAdminRights(GranteeType.GT_USER, acct);

        // Assert
        assertFalse("a plain user cannot be an admin-rights grantee", valid);
    }

    @Test
    public void isValidGranteeForAdminRightsAdminGroupIsValid() throws Exception {
        // Arrange - reuse an account as a NamedEntry carrying the admin-group flag
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, "TRUE");
        Account groupLike = createAccount("admingroup@example.com", attrs);

        // Act
        boolean valid = RightBearer.isValidGranteeForAdminRights(GranteeType.GT_GROUP, groupLike);

        // Assert
        assertTrue("a group with zimbraIsAdminGroup=TRUE is a valid grantee", valid);
    }

    @Test
    public void isValidGranteeForAdminRightsNonAdminGroupIsNotValid() throws Exception {
        // Arrange
        Account groupLike = createAccount("plaingroup@example.com", new HashMap<String, Object>());

        // Act
        boolean valid = RightBearer.isValidGranteeForAdminRights(GranteeType.GT_GROUP, groupLike);

        // Assert
        assertFalse("a group without the admin flag is not a valid grantee", valid);
    }

    @Test
    public void isValidGranteeForAdminRightsExternalGroupIsAlwaysValid() throws Exception {
        // Arrange
        Account acct = createAccount("extgrp@example.com", new HashMap<String, Object>());

        // Act
        boolean valid = RightBearer.isValidGranteeForAdminRights(GranteeType.GT_EXT_GROUP, acct);

        // Assert
        assertTrue("external group grantees are always valid", valid);
    }

    @Test
    public void isValidGranteeForAdminRightsGuestGranteeTypeIsNotValid() throws Exception {
        // Arrange
        Account acct = createAccount("guesty@example.com", new HashMap<String, Object>());

        // Act
        boolean valid = RightBearer.isValidGranteeForAdminRights(GranteeType.GT_GUEST, acct);

        // Assert
        assertFalse("guest grantee type is not valid for admin rights", valid);
    }

    @Test
    public void newRightBearerGlobalAdminAccountWrapsAsGlobalAdminWithIdAndName() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account acct = createAccount("gadmin@example.com", attrs);

        // Act
        RightBearer rb = RightBearer.newRightBearer(acct);

        // Assert - GlobalAdmin path; id/name flow through from the wrapped entry
        assertTrue("global admin must wrap as GlobalAdmin", rb instanceof RightBearer.GlobalAdmin);
        assertEquals(acct.getId(), rb.getId());
        assertEquals(acct.getName(), rb.getName());
    }

    @Test
    public void matchesGranteeGranteeIdInAceGranteeSetReturnsTrue() throws Exception {
        // Arrange - build a Grantee whose id set we control via reflection so we don't depend
        // on group-membership lookups (unsupported on the mock for accounts).
        Account acct = createAccount("matchme@example.com", new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(acct, acct.getId());

        ZimbraACE ace = new ZimbraACE(acct.getId(), GranteeType.GT_USER,
                new UserRight("invite"), null, null);

        // Act
        boolean matched = RightBearer.matchesGrantee(grantee, ace);

        // Assert
        assertTrue("ace whose grantee id is in the grantee id-set must match", matched);
    }

    @Test
    public void matchesGranteeGranteeIdNotInAceGranteeSetReturnsFalse() throws Exception {
        // Arrange
        Account acct = createAccount("nomatch@example.com", new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(acct, acct.getId());

        ZimbraACE ace = new ZimbraACE("11111111-1111-1111-1111-111111111111",
                GranteeType.GT_USER, new UserRight("invite"), null, null);

        // Act
        boolean matched = RightBearer.matchesGrantee(grantee, ace);

        // Assert
        assertFalse("ace with an unrelated grantee id must not match", matched);
    }

    @Test
    public void matchesGranteeExternalGroupAceForGroupGranteeThrowsNotImplemented() throws Exception {
        // Arrange - a group (not account) grantee against an external-group ACE
        Account acct = createAccount("groupgrantee@example.com", new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(acct, "some-other-id");
        setGranteeType(grantee, GranteeType.GT_GROUP);   // force isAccount()==false

        ZimbraACE ace = new ZimbraACE("dom-id:extgroupname", GranteeType.GT_EXT_GROUP,
                new UserRight("invite"), null, null);

        // Act / Assert
        try {
            RightBearer.matchesGrantee(grantee, ace);
            fail("expected FAILURE for ext-group ACE against a group grantee");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("not yet implemented"));
        }
    }

    // ---- helpers: build a Grantee without triggering unsupported group-membership lookups ----

    @SuppressWarnings("unchecked")
    private RightBearer.Grantee newGranteeWithIds(Account acct, String... ids) throws Exception {
        java.lang.reflect.Constructor<RightBearer.Grantee> ctor =
                (java.lang.reflect.Constructor<RightBearer.Grantee>)
                        RightBearer.Grantee.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        // instantiate without invoking the heavy ctor logic via reflection on fields
        RightBearer.Grantee grantee = (RightBearer.Grantee)
                sun.reflect.ReflectionFactory.getReflectionFactory()
                        .newConstructorForSerialization(RightBearer.Grantee.class,
                                Object.class.getDeclaredConstructor())
                        .newInstance();
        // populate the fields matchesGrantee depends on
        setField(grantee, "mRightBearer", acct);
        setField(grantee, "mGranteeType", GranteeType.GT_USER);
        Set<String> idSet = new HashSet<String>();
        for (String id : ids) {
            idSet.add(id);
        }
        setField(grantee, "mIdAndGroupIds", idSet);
        return grantee;
    }

    private void setGranteeType(RightBearer.Grantee grantee, GranteeType type) throws Exception {
        setField(grantee, "mGranteeType", type);
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        // mRightBearer is declared on the parent RightBearer class while mGranteeType and
        // mIdAndGroupIds are declared on Grantee, so walk the hierarchy to locate the field.
        Class<?> cls = RightBearer.Grantee.class;
        java.lang.reflect.Field f = null;
        while (cls != null) {
            try {
                f = cls.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        if (f == null) {
            throw new NoSuchFieldException(name);
        }
        f.setAccessible(true);
        f.set(obj, value);
    }
}
