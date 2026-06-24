/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.admin.type.DataSourceType;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link Account} exercised against the in-memory
 * {@link MockProvisioning} harness. Covers create/modify/delete workflow,
 * status derivation, address/alias helpers, calendar-resource detection,
 * UC-password encryption, and identity/data-source/signature accessors.
 */
public class AccountFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        // Overwrite-on-duplicate contract: safe to recreate the fixture per-method.
        prov.createAccount("acct@example.com", "secret", new HashMap<String, Object>());
    }

    private Account fixture() throws Exception {
        return prov.get(AccountBy.name, "acct@example.com");
    }

    /*
     * Ensures the well-known default COS exists so {@link Provisioning#getCOS(Account)} can
     * fall back to it by name. The harness does not pre-register it, so we create it on demand
     * (guarded against COS_EXISTS since @BeforeClass runs once per class).
     */
    private Cos ensureDefaultCos() throws Exception {
        Cos cos = prov.get(com.zimbra.common.account.Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
        if (cos == null) {
            cos = prov.createCos(Provisioning.DEFAULT_COS_NAME, new HashMap<String, Object>());
        }
        return cos;
    }

    @Test
    public void createModifyDeleteDisplayNamePersistsAndRetrievable() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "Test User");

        // Act
        Account account = prov.createAccount("workflow@example.com", "secret", attrs);

        // Assert created + persisted
        assertEquals("Test User", account.getDisplayName());
        assertNotNull(account.getId());
        assertEquals(account.getId(), prov.get(AccountBy.name, "workflow@example.com").getId());

        // Act modify via the entity's own modify()
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_displayName, "Updated User");
        account.modify(changes);

        // Assert change persisted on reload
        assertEquals("Updated User",
                prov.get(AccountBy.name, "workflow@example.com").getDisplayName());

        // Act delete via the entity's own deleteAccount()
        account.deleteAccount();

        // Assert gone
        assertNull(prov.get(AccountBy.name, "workflow@example.com"));
    }

    @Test
    public void getEntryTypeAnyAccountReturnsAccount() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert
        assertEquals(Entry.EntryType.ACCOUNT, account.getEntryType());
    }

    @Test
    public void sameAccountSameIdReturnsTrue() throws Exception {
        // Arrange
        Account account = fixture();
        Account reloaded = prov.get(AccountBy.name, "acct@example.com");

        // Act / Assert
        assertTrue("same backing entity must be equal by id", account.sameAccount(reloaded));
    }

    @Test
    public void sameAccountNullReturnsFalse() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert
        assertFalse(account.sameAccount(null));
    }

    @Test
    public void sameAccountDifferentIdReturnsFalse() throws Exception {
        // Arrange
        Account a = fixture();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "different-id-0001");
        Account other = prov.createAccount("other@example.com", "secret", attrs);

        // Act / Assert
        assertFalse(a.sameAccount(other));

        // Cleanup
        other.deleteAccount();
    }

    @Test
    public void isAccountStatusActiveDefaultStatusReturnsTrue() throws Exception {
        // Arrange — createAccount defaults status to active
        Account account = fixture();

        // Act / Assert
        assertTrue(account.isAccountStatusActive());
    }

    @Test
    public void getAccountStatusAdminAccountReturnsRawAccountStatus() throws Exception {
        // Arrange — admin (but not domain admin) short-circuits to the raw status
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "admin-id-0001");
        attrs.put(Provisioning.A_zimbraIsAdminAccount, com.zimbra.common.account.ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        Account admin = prov.createAccount("admin@example.com", "secret", attrs);

        // Act
        String status = admin.getAccountStatus(prov);

        // Assert
        assertEquals(Provisioning.ACCOUNT_STATUS_MAINTENANCE, status);

        // Cleanup
        admin.deleteAccount();
    }

    @Test
    public void getAccountStatusNoDomainReturnsAccountStatus() throws Exception {
        // Arrange — fixture account has no mDomain set in the mock
        Account account = fixture();

        // Act
        String status = account.getAccountStatus(prov);

        // Assert
        assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE, status);
    }

    @Test
    public void isCalendarResourceCalResTypeSetReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "calres-id-0001");
        attrs.put(Provisioning.A_zimbraCalResType, "Location");
        Account account = prov.createAccount("room@example.com", "secret", attrs);

        // Act / Assert
        assertTrue(account.isCalendarResource());

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void isCalendarResourceNoCalResTypeReturnsFalse() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert
        assertFalse(account.isCalendarResource());
    }

    @Test
    public void isAddrOfEntryPrimaryNameReturnsTrue() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert — case-insensitive match on primary name
        assertTrue(account.isAddrOfEntry("ACCT@EXAMPLE.COM"));
    }

    @Test
    public void isAddrOfEntryUnknownAddrReturnsFalse() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert
        assertFalse(account.isAddrOfEntry("stranger@example.com"));
    }

    @Test
    public void getAllAddrsSetWithAliasIncludesNameAndAlias() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "addrs-id-0001");
        attrs.put(Provisioning.A_zimbraMailAlias, "alias@example.com");
        Account account = prov.createAccount("withalias@example.com", "secret", attrs);

        // Act
        Set<String> addrs = account.getAllAddrsSet();

        // Assert
        assertTrue("must contain primary name", addrs.contains("withalias@example.com"));
        assertTrue("must contain alias", addrs.contains("alias@example.com"));

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getAllAddrsAsGroupMemberWithAliasReturnsNameThenAlias() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "gm-id-0001");
        attrs.put(Provisioning.A_zimbraMailAlias, "ga@example.com");
        Account account = prov.createAccount("gmember@example.com", "secret", attrs);

        // Act
        String[] addrs = account.getAllAddrsAsGroupMember();

        // Assert
        assertTrue("first entry is primary name", addrs.length >= 1);
        assertEquals("gmember@example.com", addrs[0]);
        assertTrue("must include alias", Arrays.asList(addrs).contains("ga@example.com"));

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getServerDefaultMailHostResolvesLocalhostServer() throws Exception {
        // Arrange — createAccount defaults zimbraMailHost to the provisioned "localhost" server
        Account account = fixture();

        // Act
        Server server = account.getServer();

        // Assert — the mail host resolves to the registered localhost server
        assertNotNull("account's mail host must resolve to a provisioned server", server);
        assertEquals("localhost", server.getName());
        assertEquals("localhost", account.getServerName());
    }

    @Test
    public void changeUCPasswordThenDecryptRoundTrips() throws Exception {
        // Arrange
        Account account = fixture();

        // Act
        account.changeUCPassword("plainSecret");
        String decrypted = account.getDecryptedUCPassword();

        // Assert
        assertEquals("UC password must round-trip through encryption", "plainSecret", decrypted);
    }

    @Test
    public void changeUCPasswordNullClearsPassword() throws Exception {
        // Arrange
        Account account = fixture();
        account.changeUCPassword("plainSecret");

        // Act
        account.changeUCPassword(null);

        // Assert
        assertNull("clearing UC password yields null decrypted value", account.getDecryptedUCPassword());
    }

    @Test
    public void encrypytUCPasswordThenStaticDecryptRoundTrips() throws Exception {
        // Arrange
        Account account = fixture();

        // Act
        String encrypted = Account.encrypytUCPassword(account.getId(), "topsecret");
        String decrypted = DataSource.decryptData(account.getId(), encrypted);

        // Assert
        assertEquals("topsecret", decrypted);
    }

    @Test
    public void getAllDataSourcesFreshAccountReturnsEmptyList() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert
        assertTrue("a fresh account has no data sources", account.getAllDataSources().isEmpty());
    }

    @Test
    public void getAllIdentitiesFreshAccountReturnsNonNull() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert — default identity is synthesized; list must never be null
        assertNotNull(account.getAllIdentities());
    }

    // ---------- alias workflow ----------

    @Test
    public void addAliasThenGetAllAddrsSetIncludesAlias() throws Exception {
        // Arrange
        Account account = fixture();

        // Act — addAlias delegates to modifyAttrs (+zimbraMailAlias) in the harness
        account.addAlias("aliasadd@example.com");

        // Assert — the alias is now part of the account's address set and recognized
        Set<String> addrs = account.getAllAddrsSet();
        assertTrue("alias must be present after addAlias", addrs.contains("aliasadd@example.com"));
        assertTrue(account.isAddrOfEntry("ALIASADD@EXAMPLE.COM"));
    }

    // ---------- group membership (empty in harness) ----------

    @Test
    public void getAclGroupsFreshAccountReturnsEmptyMembership() throws Exception {
        // Arrange
        Account account = fixture();

        // Act
        Provisioning.GroupMembership membership = account.getAclGroups(false);

        // Assert — the in-memory harness reports no group membership
        assertNotNull(membership);
        assertTrue("fresh account belongs to no ACL groups", membership.groupIds().isEmpty());
    }

    // ---------- identities ----------

    @Test
    public void getDefaultIdentityFreshAccountNamedDefault() throws Exception {
        // Arrange
        Account account = fixture();

        // Act
        Identity def = account.getDefaultIdentity();

        // Assert — synthesized default identity carries the well-known name
        assertNotNull(def);
        assertEquals(com.zimbra.common.account.ProvisioningConstants.DEFAULT_IDENTITY_NAME, def.getName());
    }

    @Test
    public void getAllIdentitiesFreshAccountContainsDefaultIdentity() throws Exception {
        // Arrange
        Account account = fixture();

        // Act
        List<Identity> identities = account.getAllIdentities();

        // Assert
        assertEquals("exactly one synthesized identity in the harness", 1, identities.size());
        assertEquals(com.zimbra.common.account.ProvisioningConstants.DEFAULT_IDENTITY_NAME,
                identities.get(0).getName());
    }

    // ---------- data sources ----------

    @Test
    public void createDataSourceThenGetAllReturnsCreatedSource() throws Exception {
        // Arrange
        Account account = fixture();
        Map<String, Object> attrs = new HashMap<String, Object>();

        // Act
        DataSource ds = account.createDataSource(DataSourceType.imap, "myimap", attrs);

        // Assert — created source is retrievable through the account
        assertNotNull(ds);
        assertEquals("myimap", ds.getName());
        assertEquals(DataSourceType.imap, ds.getType());
        List<DataSource> all = account.getAllDataSources();
        assertEquals(1, all.size());
        assertEquals("myimap", all.get(0).getName());
    }

    // ---------- UC service / username ----------

    @Test
    public void getUCServiceUnsetReturnsNull() throws Exception {
        // Arrange
        Account account = fixture();

        // Act / Assert — no zimbraUCServiceId set
        assertNull(account.getUCService());
    }

    @Test
    public void getUCUsernameUnsetDefaultsToLocalPart() throws Exception {
        // Arrange — no zimbraUCUsername; falls back to the email local part
        Account account = fixture();

        // Act
        String ucUsername = account.getUCUsername();

        // Assert
        assertEquals("acct", ucUsername);
    }

    // ---------- external account / server ----------

    @Test
    public void isAccountExternalTransportMatchesServerReturnsFalse() throws Exception {
        // Arrange — fixture's mail host resolves to the localhost server (serviceHostname=localhost).
        // With no zimbraMailTransport set, Server.mailTransportMatches(null) returns true,
        // so the account is NOT external.
        Account account = fixture();

        // Act — exercises getServer() + mailTransportMatches(null) branch
        boolean external = account.isAccountExternal();

        // Assert — a null transport "matches" any server, so the account is internal
        assertFalse("null transport matches the server, so the account is not external", external);
    }

    @Test
    public void isAccountExternalTransportDoesNotMatchServerReturnsTrue() throws Exception {
        // Arrange — give the account a transport whose host does not match the localhost server
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "external-acct-0001");
        attrs.put(Provisioning.A_zimbraMailTransport, "lmtp:otherhost:7025");
        Account account = prov.createAccount("external@example.com", "secret", attrs);

        // Act — mailTransportMatches returns false because the host differs from serviceHostname
        boolean external = account.isAccountExternal();

        // Assert — a transport that does not match its server reports external
        assertTrue("account whose transport does not match its server is external", external);

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getServerNameNoMailHostReturnsNull() throws Exception {
        // Arrange — account with an explicitly cleared mail host
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "nomailhost-0001");
        attrs.put(Provisioning.A_zimbraMailHost, "");
        Account account = prov.createAccount("nohost@example.com", "secret", attrs);

        // Act / Assert — empty mail host yields a null/absent server name
        assertNull(account.getServerName());

        // Cleanup
        account.deleteAccount();
    }

    // ---------- getAccountStatus domain-status derivation ----------

    @Test
    public void getAccountStatusDomainLockedReturnsLocked() throws Exception {
        // Arrange — active account in a LOCKED domain
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_LOCKED);
        prov.createDomain("locked.dom", dattrs);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "lockdom-acct-0001");
        Account account = prov.createAccount("u@locked.dom", "secret", attrs);

        // Act
        String status = account.getAccountStatus(prov);

        // Assert — active account in a locked domain is effectively locked
        assertEquals(Provisioning.ACCOUNT_STATUS_LOCKED, status);

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getAccountStatusDomainMaintenanceReturnsMaintenance() throws Exception {
        // Arrange — active account in a MAINTENANCE domain
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_MAINTENANCE);
        prov.createDomain("maint.dom", dattrs);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "maintdom-acct-0001");
        Account account = prov.createAccount("u@maint.dom", "secret", attrs);

        // Act
        String status = account.getAccountStatus(prov);

        // Assert
        assertEquals(Provisioning.ACCOUNT_STATUS_MAINTENANCE, status);

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getAccountStatusDomainClosedReturnsClosed() throws Exception {
        // Arrange — active account in a CLOSED domain
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_CLOSED);
        prov.createDomain("closed.dom", dattrs);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "closeddom-acct-0001");
        Account account = prov.createAccount("u@closed.dom", "secret", attrs);

        // Act
        String status = account.getAccountStatus(prov);

        // Assert — closed domain forces closed status
        assertEquals(Provisioning.ACCOUNT_STATUS_CLOSED, status);

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getAccountStatusDomainLockedButAccountClosedKeepsAccountStatus() throws Exception {
        // Arrange — closed account in a locked domain keeps its own (closed) status
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_LOCKED);
        prov.createDomain("locked2.dom", dattrs);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "locked2-acct-0001");
        attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_CLOSED);
        Account account = prov.createAccount("u@locked2.dom", "secret", attrs);

        // Act
        String status = account.getAccountStatus(prov);

        // Assert — account-level closed wins over domain-derived locked
        assertEquals(Provisioning.ACCOUNT_STATUS_CLOSED, status);

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getAccountStatusDomainActiveReturnsAccountStatus() throws Exception {
        // Arrange — active account in an ACTIVE domain
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_ACTIVE);
        prov.createDomain("active.dom", dattrs);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "activedom-acct-0001");
        Account account = prov.createAccount("u@active.dom", "secret", attrs);

        // Act
        String status = account.getAccountStatus(prov);

        // Assert — active domain leaves the account status untouched
        assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE, status);

        // Cleanup
        account.deleteAccount();
    }

    // ---------- defaults ----------

    @Test
    public void setAccountDefaultsPrimaryOnlyAppliesCosDefaults() throws Exception {
        // Arrange — register the default COS so getCOS() can resolve it by name
        ensureDefaultCos();
        Account account = fixture();

        // Act — pull primary defaults from the default COS
        account.setAccountDefaults(false);

        // Assert — the COS resolves and defaults were applied without error
        assertNotNull("default COS must resolve through the harness", account.getCOS());
    }

    @Test
    public void setAccountDefaultsWithSecondaryAppliesDomainDefaults() throws Exception {
        // Arrange — register the default COS, and an account in an existing domain so
        // secondary defaults are pulled
        ensureDefaultCos();
        prov.createDomain("defaults.dom", new HashMap<String, Object>());
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "defaults-acct-0001");
        Account account = prov.createAccount("u@defaults.dom", "secret", attrs);

        // Act — exercises the secondary-defaults (domain) branch
        account.setAccountDefaults(true);

        // Assert
        assertNotNull(account.getCOS());

        // Cleanup
        account.deleteAccount();
    }

    // ---------- auth token validity ----------

    @Test
    public void checkAuthTokenValidityValueMatchingValueReturnsTrue() throws Exception {
        // Arrange — a freshly issued token carries the account's current validity value
        Account account = fixture();
        ZimbraAuthToken token = new ZimbraAuthToken(account);

        // Act — validity-value checking is enabled by config default
        boolean ok = account.checkAuthTokenValidityValue(token);

        // Assert — equal account/token validity values pass the check
        assertTrue(ok);
    }

    // ---------- token cleanup hooks ----------

    @Test
    public void cleanExpiredTokensFreshAccountNoException() throws Exception {
        // Arrange
        Account account = fixture();

        // Act — purgeAuthTokens against an account with no tokens must be a clean no-op
        account.cleanExpiredTokens();

        // Assert — account remains usable afterward
        assertEquals("acct@example.com", account.getName());
    }

    @Test
    public void cleanExpiredJWTokensFreshAccountNoException() throws Exception {
        // Arrange
        Account account = fixture();

        // Act
        account.cleanExpiredJWTokens();

        // Assert
        assertEquals("acct@example.com", account.getName());
    }

    // ---------- strengthened mutation-killing assertions ----------

    @Test
    public void isAccountStatusActiveNonActiveStatusReturnsFalse() throws Exception {
        // Arrange — an account explicitly in MAINTENANCE status is NOT active.
        // Kills BooleanTrueReturnVals on isAccountStatusActive (L238): a forced 'true'
        // would wrongly report a maintenance account as active.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "inactive-status-0001");
        attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        Account account = prov.createAccount("inactive@example.com", "secret", attrs);

        // Act / Assert
        assertFalse("maintenance account must not report active", account.isAccountStatusActive());

        // Also confirm the positive branch on the same code path.
        assertTrue("active fixture account must report active", fixture().isAccountStatusActive());

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getAccountStatusAdminAndDomainAdminDoesNotShortCircuit() throws Exception {
        // Arrange — an account that is BOTH admin and domain admin. The expression
        //   isAdmin = (isAdmin && !isDomainAdmin)  (L273)
        // must evaluate to false, so the method must NOT short-circuit to the raw status
        // and must instead derive the status from the (locked) domain.
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_LOCKED);
        prov.createDomain("admindom.dom", dattrs);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "admin-domadmin-0001");
        attrs.put(Provisioning.A_zimbraIsAdminAccount, com.zimbra.common.account.ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, com.zimbra.common.account.ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        Account acct = prov.createAccount("u@admindom.dom", "secret", attrs);

        // Act
        String status = acct.getAccountStatus(prov);

        // Assert — domain derivation wins (LOCKED), proving the admin short-circuit did NOT fire.
        // If L273 were negated, isAdmin would be true and the raw ACTIVE status would be returned.
        assertEquals(Provisioning.ACCOUNT_STATUS_LOCKED, status);

        // And the pure-admin (non-domain-admin) case still short-circuits to the raw status.
        Map<String, Object> aattrs = new HashMap<String, Object>();
        aattrs.put(Provisioning.A_zimbraId, "pure-admin-0001");
        aattrs.put(Provisioning.A_zimbraIsAdminAccount, com.zimbra.common.account.ProvisioningConstants.TRUE);
        aattrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        Account pureAdmin = prov.createAccount("admin2@admindom.dom", "secret", aattrs);
        assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE, pureAdmin.getAccountStatus(prov));

        // Cleanup
        acct.deleteAccount();
        pureAdmin.deleteAccount();
    }

    @Test
    public void getAllAddrsAsGroupMemberNoAliasesReturnsOnlyName() throws Exception {
        // Arrange — an account with no aliases. The result array must hold exactly the name.
        // Strengthens the capacity-hint arithmetic site (L440) by pinning exact contents/length.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "gm-noalias-0001");
        Account account = prov.createAccount("gmnoalias@example.com", "secret", attrs);

        // Act
        String[] addrs = account.getAllAddrsAsGroupMember();

        // Assert — exactly one element, the primary name.
        assertEquals(1, addrs.length);
        assertEquals("gmnoalias@example.com", addrs[0]);

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void getAllAddrsAsGroupMemberTwoAliasesReturnsNameAndBothAliases() throws Exception {
        // Arrange — name + two distinct aliases => exactly three entries.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "gm-twoalias-0001");
        attrs.put(Provisioning.A_zimbraMailAlias,
                new String[] {"a1@example.com", "a2@example.com"});
        Account account = prov.createAccount("gmtwo@example.com", "secret", attrs);

        // Act
        String[] addrs = account.getAllAddrsAsGroupMember();

        // Assert — name first, then both aliases; exactly three entries total.
        assertEquals(3, addrs.length);
        assertEquals("gmtwo@example.com", addrs[0]);
        List<String> all = Arrays.asList(addrs);
        assertTrue(all.contains("a1@example.com"));
        assertTrue(all.contains("a2@example.com"));

        // Cleanup
        account.deleteAccount();
    }

    @Test
    public void setAccountDefaultsPrimaryOnlyAppliesCosIdDefault() throws Exception {
        // Arrange — default COS exists; the account has no zimbraCOSId of its own.
        Cos cos = ensureDefaultCos();
        Account account = fixture();
        assertNull("fixture must start without an explicit COS id", account.getCOSId());

        // Act — primary-defaults branch (setSecondaryDefaults == false, L420 -> L422).
        account.setAccountDefaults(false);

        // Assert — the COS id was injected into the account's defaults (L415/L416) and is now
        // readable, and the resolved COS matches. Killing the setDefaults(...) call (L422) would
        // leave getCOSId() null.
        assertEquals("COS id must be applied as a default", cos.getId(), account.getCOSId());
        assertEquals(cos.getId(), account.getCOS().getId());
    }

    @Test
    public void setAccountDefaultsWithSecondaryAppliesCosIdAndDomainDefaults() throws Exception {
        // Arrange — default COS plus an account in a domain that carries an account default.
        // zimbraFreebusyExchangeURL is accountCosDomainInherited with no default COS value, so a
        // domain value surfaces only through the secondary-defaults branch (L427 -> L429); a null
        // domain or a removed setDefaults call would not surface it.
        ensureDefaultCos();
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraFreebusyExchangeURL, "https://exch.example.com/ews");
        Domain domain = prov.createDomain("secdefaults.dom", dattrs);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "secdefaults-acct-0001");
        Account account = prov.createAccount("u@secdefaults.dom", "secret", attrs);

        // Act — primary + secondary defaults.
        account.setAccountDefaults(true);

        // Assert — COS id applied (primary) and the domain's value visible (secondary).
        assertNotNull("COS must resolve", account.getCOS());
        assertEquals("COS id applied via defaults", account.getCOS().getId(), account.getCOSId());
        assertEquals("domain account-default must be inherited",
                "https://exch.example.com/ews",
                account.getAttr(Provisioning.A_zimbraFreebusyExchangeURL));

        // Cleanup
        account.deleteAccount();
        prov.deleteDomain(domain.getId());
    }

    @Test
    public void cleanExpiredTokensExpiredAuthTokenIsPurged() throws Exception {
        // Arrange — store an already-expired auth token, confirm it is present.
        // Kills VoidMethodCall on cleanExpiredTokens (L521): removing purgeAuthTokens()
        // would leave the expired token in place.
        Account account = fixture();
        account.addAuthTokens("clean-tok-1", "tokval-1",
                new com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration(1L));
        assertTrue("token must be present before purge", account.hasAuthTokens("clean-tok-1"));

        // Act
        account.cleanExpiredTokens();

        // Assert — the expired token is gone.
        assertFalse("expired auth token must be purged", account.hasAuthTokens("clean-tok-1"));
    }

    @Test
    public void cleanExpiredJWTokensExpiredTokenIsPurged() throws Exception {
        // Arrange — store an already-expired invalid-JWT token.
        // Kills VoidMethodCall on cleanExpiredJWTokens (L525).
        Account account = fixture();
        account.addInvalidJWTokens("clean-jw-1", "jwval-1",
                new com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration(1L));
        assertTrue("JW token must be present before purge", account.hasInvalidJWTokens("clean-jw-1"));

        // Act
        account.cleanExpiredJWTokens();

        // Assert
        assertFalse("expired JW token must be purged", account.hasInvalidJWTokens("clean-jw-1"));
    }

    @Test
    public void checkAuthTokenValidityValueCheckingDisabledMismatchStillPasses() throws Exception {
        // Arrange — disable validity-value checking on the config, then create a token whose
        // validity value differs from the account's. Kills NegateConditionals on L328:
        //   if (!isAuthTokenValidityValueEnabled()) return true;
        // If negated, a disabled config would fall through and compute a (failing) comparison.
        Account account = fixture();
        ZimbraAuthToken token = new ZimbraAuthToken(account); // captures current acct validity
        account.setAuthTokenValidityValue(99);                // now token != account
        boolean savedEnabled = prov.getConfig().isAuthTokenValidityValueEnabled();
        prov.getConfig().setAuthTokenValidityValueEnabled(false);
        try {
            // Act
            boolean ok = account.checkAuthTokenValidityValue(token);
            // Assert — with checking disabled, even a mismatch passes.
            assertTrue("disabled validity check must pass regardless of mismatch", ok);
        } finally {
            prov.getConfig().setAuthTokenValidityValueEnabled(savedEnabled);
        }
    }

    @Test
    public void checkAuthTokenValidityValueEnabledAccountAheadOfTokenReturnsFalse() throws Exception {
        // Arrange — checking enabled, account validity value strictly greater than the token's
        // (acctValue > authTokenValue path), which can never reload and must reject.
        Account account = fixture();
        ZimbraAuthToken token = new ZimbraAuthToken(account); // token validity == current acct value
        account.setAuthTokenValidityValue(account.getAuthTokenValidityValue() + 10); // account ahead
        boolean savedEnabled = prov.getConfig().isAuthTokenValidityValueEnabled();
        prov.getConfig().setAuthTokenValidityValueEnabled(true);
        try {
            // Act
            boolean ok = account.checkAuthTokenValidityValue(token);
            // Assert — a stale token (lower validity than the account) is rejected.
            assertFalse("token behind the account's validity value must be rejected", ok);
        } finally {
            prov.getConfig().setAuthTokenValidityValueEnabled(savedEnabled);
            account.setAuthTokenValidityValue(-1);
        }
    }
}
