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

import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Functional tests for {@link Alias}.
 *
 * <p>Resolving an alias target ({@link Alias#getTarget}) ultimately calls
 * {@code Provisioning.searchAliasTarget}, which initializes the live-LDAP
 * {@code com.zimbra.cs.ldap.ZLdapFilterFactory} -- a class the in-memory mock harness cannot
 * initialize. So these tests exercise the parts of {@link Alias} reachable under the mock:
 * the entry type, target-id attribute persistence/modification, the dangling-flag cache
 * short-circuit, and the cached-target accessor branches. The dangling flag and cached target
 * are seeded via reflection so the cache short-circuits BEFORE the LDAP search is attempted,
 * exercising the real {@code mIsDangling}/{@code mTarget} branching in {@code getTarget}.</p>
 *
 * <p><b>Why not drive this through {@code SoapProvisioning}/{@code SoapAlias}?</b> That route is
 * not available to a no-network unit test: {@code SoapProvisioning} operates over a live
 * {@code SoapHttpTransport} and throws unless {@code setURI(...)} has been called against a
 * running server, and {@code SoapAlias} is package-private to {@code com.zimbra.cs.account.soap}
 * and is only ever instantiated from a parsed SOAP response -- there is no way to obtain one
 * without a real SOAP endpoint. Note this is a network/LDAP limitation, not a constructor-access
 * one: both {@link com.zimbra.cs.account.MailTarget} and {@link Alias} have public constructors
 * (this test uses {@code new Alias(...)} directly), so direct construction plus reflective cache
 * seeding is the correct approach for exercising {@link Alias} without a server.</p>
 */
public class AliasFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Alias newAlias(String targetId) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (targetId != null) {
            attrs.put(Provisioning.A_zimbraAliasTargetId, targetId);
        }
        return new Alias("alias@zimbra.com", "alias-id-1", attrs, prov);
    }

    /* Forces the alias into the "already searched, target missing" state without touching LDAP. */
    private static void markDangling(Alias alias) throws Exception {
        Field f = Alias.class.getDeclaredField("mIsDangling");
        f.setAccessible(true);
        f.setBoolean(alias, true);
    }

    /* Seeds a resolved target directly so cached-target branches run without an LDAP search. */
    private static void seedTarget(Alias alias, NamedEntry target) throws Exception {
        Field f = Alias.class.getDeclaredField("mTarget");
        f.setAccessible(true);
        f.set(alias, target);
    }

    private Account newAccount(String name, String id) {
        return new Account(name, id, new HashMap<String, Object>(), new HashMap<String, Object>(), prov);
    }

    @Test
    public void getEntryTypeAnyAliasReturnsAlias() throws Exception {
        // Arrange
        Alias alias = newAlias("missing-target-id");

        // Act
        EntryType type = alias.getEntryType();

        // Assert
        assertEquals(EntryType.ALIAS, type);
    }

    @Test
    public void constructorWithTargetIdPersistsTargetIdAttribute() throws Exception {
        // Arrange / Act
        Alias alias = newAlias("target-xyz");

        // Assert -- the alias retains its identity and target-id attribute
        assertEquals("alias@zimbra.com", alias.getName());
        assertEquals("alias-id-1", alias.getId());
        assertEquals("target-xyz", alias.getAttr(Provisioning.A_zimbraAliasTargetId, null));
    }

    @Test
    public void getAttrModifiedTargetIdAttributeReflectsNewValue() throws Exception {
        // Arrange
        Alias alias = newAlias("target-old");

        // Act -- mutate the in-memory attribute map directly (no LDAP)
        Map<String, Object> change = new HashMap<String, Object>();
        change.put(Provisioning.A_zimbraAliasTargetId, "target-new");
        alias.setAttrs(change);

        // Assert
        assertEquals("target-new", alias.getAttr(Provisioning.A_zimbraAliasTargetId, null));
    }

    @Test
    public void getTargetDanglingCachedShortCircuitsToNull() throws Exception {
        // Arrange -- pretend a prior search already established the target is missing
        Alias alias = newAlias("nonexistent-target");
        markDangling(alias);

        // Act -- the dangling short-circuit must return null WITHOUT searching LDAP
        NamedEntry target = alias.getTarget(prov);

        // Assert
        assertNull(target);
    }

    @Test
    public void getTargetTargetAlreadyResolvedReturnsCachedTarget() throws Exception {
        // Arrange -- seed a resolved account as the cached target
        Alias alias = newAlias("acct-1");
        Account acct = newAccount("user@zimbra.com", "acct-1");
        seedTarget(alias, acct);

        // Act -- the mTarget != null branch must return the same instance, no LDAP search
        NamedEntry resolved = alias.getTarget(prov);

        // Assert
        assertSame(acct, resolved);
    }

    @Test
    public void getTargetNameDanglingCachedReturnsNull() throws Exception {
        // Arrange
        Alias alias = newAlias("nonexistent-target");
        markDangling(alias);

        // Act
        String name = alias.getTargetName(prov);

        // Assert
        assertNull(name);
    }

    @Test
    public void getTargetNameTargetResolvedReturnsTargetName() throws Exception {
        // Arrange
        Alias alias = newAlias("acct-1");
        Account acct = newAccount("user@zimbra.com", "acct-1");
        seedTarget(alias, acct);

        // Act
        String name = alias.getTargetName(prov);

        // Assert
        assertEquals("user@zimbra.com", name);
    }

    @Test
    public void getTargetUnicodeNameDanglingCachedReturnsNull() throws Exception {
        // Arrange
        Alias alias = newAlias("nonexistent-target");
        markDangling(alias);

        // Act
        String unicodeName = alias.getTargetUnicodeName(prov);

        // Assert
        assertNull(unicodeName);
    }

    @Test
    public void getTargetUnicodeNameAsciiTargetNameReturnsSameAddress() throws Exception {
        // Arrange -- a plain ASCII address round-trips unchanged through IDN decoding
        Alias alias = newAlias("acct-1");
        Account acct = newAccount("user@zimbra.com", "acct-1");
        seedTarget(alias, acct);

        // Act
        String unicodeName = alias.getTargetUnicodeName(prov);

        // Assert
        assertEquals("user@zimbra.com", unicodeName);
    }

    @Test
    public void getTargetTypeDanglingCachedReturnsNull() throws Exception {
        // Arrange
        Alias alias = newAlias("nonexistent-target");
        markDangling(alias);

        // Act
        TargetType type = alias.getTargetType(prov);

        // Assert -- no resolvable target means no target type
        assertNull(type);
    }

    @Test
    public void getTargetTypeAccountTargetReturnsAccount() throws Exception {
        // Arrange -- a resolved Account target maps to TargetType.account
        Alias alias = newAlias("acct-1");
        Account acct = newAccount("user@zimbra.com", "acct-1");
        seedTarget(alias, acct);

        // Act
        TargetType type = alias.getTargetType(prov);

        // Assert
        assertEquals(TargetType.account, type);
    }
}
