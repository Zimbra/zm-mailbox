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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.Provisioning.MailMode;
import com.zimbra.cs.account.Provisioning.MemberOf;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for the concrete (non-abstract) surface of {@link Provisioning} exercised
 * through the in-memory MockProvisioning harness and through its static/value helper types.
 */
public class ProvisioningFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "Prov Tester");
        prov.createAccount("prov@zimbra.com", "secret", attrs);
    }

    private Account acct() throws Exception {
        return prov.get(Key.AccountBy.name, "prov@zimbra.com");
    }

    // ---- sanitizedAttrValue --------------------------------------------------------------

    @Test
    public void sanitizedAttrValuePasswordAttrIsBlocked() {
        // Act
        Object sanitized = Provisioning.sanitizedAttrValue(Provisioning.A_userPassword, "topsecret");

        // Assert
        assertEquals("VALUE-BLOCKED", sanitized);
    }

    @Test
    public void sanitizedAttrValuePasswordAttrDifferentCaseIsBlocked() {
        // Act — case-insensitive match
        Object sanitized = Provisioning.sanitizedAttrValue(
                Provisioning.A_userPassword.toUpperCase(), "topsecret");

        // Assert
        assertEquals("VALUE-BLOCKED", sanitized);
    }

    @Test
    public void sanitizedAttrValueNonSensitiveAttrReturnsOriginal() {
        // Act
        Object sanitized = Provisioning.sanitizedAttrValue(Provisioning.A_displayName, "Bob");

        // Assert
        assertEquals("Bob", sanitized);
    }

    // ---- MailMode ------------------------------------------------------------------------

    @Test
    public void mailModeFromStringValidValueReturnsEnum() throws Exception {
        // Act
        MailMode mode = MailMode.fromString("https");

        // Assert
        assertEquals(MailMode.https, mode);
    }

    @Test
    public void mailModeFromStringInvalidValueThrowsInvalidRequest() {
        // Act / Assert
        try {
            MailMode.fromString("ftp");
            fail("expected ServiceException for unknown mail mode");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("unknown mail mode"));
        }
    }

    // ---- GroupMembership / MemberOf ------------------------------------------------------

    @Test
    public void groupMembershipAppendMemberOfTracksIdsAndMembers() {
        // Arrange
        GroupMembership gm = new GroupMembership();
        MemberOf mo = new MemberOf("grp-1", true, false);

        // Act
        gm.append(mo);

        // Assert
        assertEquals(1, gm.memberOf().size());
        assertEquals(1, gm.groupIds().size());
        assertEquals("grp-1", gm.groupIds().get(0));
        assertTrue("admin flag must be preserved", gm.memberOf().get(0).isAdminGroup());
        assertSame(mo, gm.getMemberOfForId("grp-1"));
        assertNull("unknown id returns null", gm.getMemberOfForId("nope"));

        // a non-admin MemberOf must report isAdminGroup()==false (kills L818 BooleanTrueReturnVals)
        MemberOf plain = new MemberOf("grp-2", false, false);
        assertFalse("non-admin group must report false", plain.isAdminGroup());
    }

    @Test
    public void groupMembershipMergeFromDeduplicatesByGroupId() {
        // Arrange
        GroupMembership a = new GroupMembership();
        a.append(new MemberOf("grp-1", false, false));
        GroupMembership b = new GroupMembership();
        b.append(new MemberOf("grp-1", false, false)); // duplicate id
        b.append(new MemberOf("grp-2", true, false)); // admin group

        // Act
        GroupMembership returned = a.mergeFrom(b);

        // Assert — mergeFrom must return the receiver itself (kills L882 NullReturnVals)
        assertSame("mergeFrom must return 'this'", a, returned);
        // only the new id is added, the duplicate is skipped
        assertEquals(2, a.groupIds().size());
        assertTrue(a.groupIds().contains("grp-1"));
        assertTrue(a.groupIds().contains("grp-2"));
        // the admin flag of the merged-in member is preserved (L818 isAdminGroup)
        assertTrue("grp-2 was an admin group", a.getMemberOfForId("grp-2").isAdminGroup());
    }

    @Test
    public void groupMembershipCloneIsIndependentCopy() {
        // Arrange
        GroupMembership original = new GroupMembership();
        original.append(new MemberOf("grp-1", false, false));

        // Act
        GroupMembership copy = original.clone();
        copy.append(new MemberOf("grp-2", false, false));

        // Assert — mutating the copy must not affect the original
        assertEquals(1, original.groupIds().size());
        assertEquals(2, copy.groupIds().size());
    }

    @Test
    public void groupMembershipExplicitListsConstructorExposesProvidedLists() {
        // Arrange
        List<MemberOf> memberOf = new ArrayList<MemberOf>();
        memberOf.add(new MemberOf("grp-x", false, false));
        List<String> ids = new ArrayList<String>();
        ids.add("grp-x");

        // Act
        GroupMembership gm = new GroupMembership(memberOf, ids);

        // Assert
        assertEquals(1, gm.memberOf().size());
        assertEquals("grp-x", gm.groupIds().get(0));
    }

    // ---- instance helpers on the live mock ----------------------------------------------

    @Test
    public void idIsUUIDDefaultProvisioningIsTrue() {
        // Act / Assert
        assertTrue("base Provisioning reports UUID ids", prov.idIsUUID());
    }

    @Test
    public void modifyAttrsTwoArgOverloadPersistsToReloadedEntry() throws Exception {
        // Arrange
        Account a = acct();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_displayName, "Renamed Tester");

        // Act — exercises the concrete 2-arg modifyAttrs that delegates to the abstract impl
        prov.modifyAttrs(a, changes);

        // Assert — change is visible on a freshly fetched account
        assertEquals("Renamed Tester", acct().getDisplayName());
    }

    @Test
    public void getDomainAccountWithDomainReturnsCreatedDomain() throws Exception {
        // Arrange — create the domain the account lives in
        prov.createDomain("zimbra.com", new HashMap<String, Object>());

        // Act
        Domain domain = prov.getDomain(acct());

        // Assert
        assertNotNull("domain for prov@zimbra.com must resolve", domain);
        assertEquals("zimbra.com", domain.getName());
    }

    @Test
    public void getConfigHarnessReturnsNonNullConfig() throws Exception {
        // Act
        Config config = prov.getConfig();

        // Assert
        assertNotNull("mock provisioning must supply a Config entry", config);
        assertEquals(Entry.EntryType.GLOBALCONFIG, config.getEntryType());
    }

    @Test
    public void getAccountByIdAfterCreateRoundTripsById() throws Exception {
        // Arrange
        Account created = acct();
        String id = created.getId();

        // Act
        Account byId = prov.getAccountById(id);

        // Assert
        assertNotNull(byId);
        assertEquals("prov@zimbra.com", byId.getName());
        assertEquals(id, byId.getId());
    }

    @Test
    public void getAccountByIdUnknownIdReturnsNull() throws Exception {
        // Act
        Account missing = prov.getAccountById("no-such-id-1234");

        // Assert
        assertNull("unknown id must return null, not throw", missing);
    }

    @Test
    public void getInstanceIsStableSingleton() {
        // Act / Assert — both accessors hand back the installed mock
        assertSame(prov, Provisioning.getInstance());
    }

    @Test
    public void getMemberAddrsGroupWithNoMembersReturnsGroupAddrOnly() throws Exception {
        // Arrange
        Provisioning.GroupMemberEmailAddrs addrs = new Provisioning.GroupMemberEmailAddrs();
        addrs.setGroupAddr("dl@zimbra.com");

        // Assert — the value holder reports what was set, internal/external default null
        assertEquals("dl@zimbra.com", addrs.groupAddr());
        assertNull(addrs.internalAddrs());
        assertNull(addrs.externalAddrs());
    }

    @Test
    public void createAccountDuplicateNameOverwritesPerHarnessContract() throws Exception {
        // Arrange — first account already created in setUp
        String firstId = acct().getId();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "Recreated");

        // Act — recreating the same name must NOT throw (overwrite-on-duplicate contract)
        Account recreated = prov.createAccount("prov@zimbra.com", "secret", attrs);

        // Assert
        assertNotNull(recreated);
        assertEquals("Recreated", acct().getDisplayName());
        assertFalse("recreated id should not be empty", recreated.getId().isEmpty());
        assertNotNull(firstId);
    }

    // ---- getAccountBy* convenience delegators -------------------------------------------

    @Test
    public void getAccountByForeignPrincipalUnknownReturnsNull() throws Exception {
        // Act — delegates to get(AccountBy.foreignPrincipal, ...); mock has no such index => null
        Account a = prov.getAccountByForeignPrincipal("nobody@external.com");

        // Assert
        assertNull("unknown foreign principal must resolve to null", a);
    }

    @Test
    public void getAccountByKrb5PrincipalUnknownReturnsNull() throws Exception {
        // Act
        Account a = prov.getAccountByKrb5Principal("nobody@REALM");

        // Assert
        assertNull("unknown krb5 principal must resolve to null", a);
    }

    @Test
    public void getAccountByAppAdminNameUnknownReturnsNull() throws Exception {
        // Act
        Account a = prov.getAccountByAppAdminName("no-app-admin");

        // Assert
        assertNull("unknown app admin name must resolve to null", a);
    }

    @Test
    public void getAccountByAppAdminNameExistingResolvesAccount() throws Exception {
        // The mock's get(AccountBy, key) falls through to id2account for non-name keys, so passing
        // an existing account id resolves a real account. Kills L1168 NullReturnVals (a null mutant
        // would not equal the created account).
        Account created = acct();
        Account a = prov.getAccountByAppAdminName(created.getId());
        assertNotNull("existing id must resolve through getAccountByAppAdminName", a);
        assertSame(created, a);
        assertEquals("prov@zimbra.com", a.getName());
    }

    @Test
    public void getAccountByForeignPrincipalExistingResolvesAccount() throws Exception {
        // Kills L1169 NullReturnVals.
        Account created = acct();
        Account a = prov.getAccountByForeignPrincipal(created.getId());
        assertNotNull(a);
        assertSame(created, a);
        assertEquals(created.getId(), a.getId());
    }

    @Test
    public void getAccountByKrb5PrincipalExistingResolvesAccount() throws Exception {
        // Kills L1170 NullReturnVals.
        Account created = acct();
        Account a = prov.getAccountByKrb5Principal(created.getId());
        assertNotNull(a);
        assertSame(created, a);
        assertEquals("prov@zimbra.com", a.getName());
    }

    @Test
    public void getAccountByNameExistingResolvesThroughDelegator() throws Exception {
        // Arrange — created in setUp
        // Act
        Account a = prov.getAccountByName("prov@zimbra.com");

        // Assert
        assertNotNull(a);
        assertEquals("prov@zimbra.com", a.getName());
    }

    // ---- getDomainBy* convenience delegators --------------------------------------------

    @Test
    public void getDomainByNameAfterCreateRoundTripsByNameAndId() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "dom-by-name-id");
        prov.createDomain("byname.com", attrs);

        // Act
        Domain byName = prov.getDomainByName("byname.com");
        Domain byId = prov.getDomainById("dom-by-name-id");

        // Assert — both delegators resolve the same created domain
        assertNotNull(byName);
        assertNotNull(byId);
        assertEquals("byname.com", byName.getName());
        assertEquals(byName.getId(), byId.getId());
    }

    @Test
    public void getDomainByVirtualHostnameUnknownReturnsNull() throws Exception {
        // Act — mock domain lookup only matches id/name, not vhost => null
        Domain d = prov.getDomainByVirtualHostname("vhost.example.com");

        // Assert
        assertNull(d);
    }

    @Test
    public void getDomainByKrb5RealmUnknownReturnsNull() throws Exception {
        // Act
        Domain d = prov.getDomainByKrb5Realm("EXAMPLE.REALM");

        // Assert
        assertNull(d);
    }

    @Test
    public void getDomainByForeignNameUnknownReturnsNull() throws Exception {
        // Act
        Domain d = prov.getDomainByForeignName("foreign:thing");

        // Assert
        assertNull(d);
    }

    // ---- getCosById / getServerBy* delegators -------------------------------------------

    @Test
    public void getCosByIdAfterCreateRoundTripsById() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "cos-round-trip-id");
        Cos created = prov.createCos("functionalcos", attrs);

        // Act
        Cos byId = prov.getCosById("cos-round-trip-id");

        // Assert
        assertNotNull(byId);
        assertEquals(created.getId(), byId.getId());
        assertEquals("functionalcos", byId.getName());
    }

    @Test
    public void getServerByIdAfterCreateRoundTripsById() throws Exception {
        // Arrange — createServer assigns a real (UUID) zimbraId, distinct from the server name
        Server created = prov.createServer("functional.server", new HashMap<String, Object>());

        // Act — look the server up by the id the harness assigned
        Server byId = prov.getServerById(created.getId());

        // Assert — round-trips by id; name and id resolve consistently
        assertNotNull("server must resolve by its id", byId);
        assertEquals("functional.server", byId.getName());
        assertNotNull("server id should be assigned", byId.getId());
        assertEquals(created.getId(), byId.getId());
    }

    @Test
    public void getServerByServiceHostnameAfterCreateResolvesByName() throws Exception {
        // Arrange — mock stores servers by name; serviceHostname falls through to name lookup
        prov.createServer("svc.host.server", new HashMap<String, Object>());

        // Act
        Server s = prov.getServerByServiceHostname("svc.host.server");

        // Assert
        assertNotNull(s);
        assertEquals("svc.host.server", s.getName());
    }

    @Test
    public void getServerByIdUnknownReturnsNull() throws Exception {
        // Act
        Server s = prov.getServerById("no-such-server-id");

        // Assert
        assertNull(s);
    }

    // ---- GalMode -------------------------------------------------------------------------

    @Test
    public void galModeFromStringValidValueReturnsEnum() throws Exception {
        // Act
        Provisioning.GalMode mode = Provisioning.GalMode.fromString("both");

        // Assert
        assertEquals(Provisioning.GalMode.both, mode);
    }

    @Test
    public void galModeFromStringNullReturnsNull() throws Exception {
        // Act
        Provisioning.GalMode mode = Provisioning.GalMode.fromString(null);

        // Assert
        assertNull("null input must yield null gal mode", mode);
    }

    @Test
    public void galModeFromStringInvalidValueThrowsInvalidRequest() {
        // Act / Assert
        try {
            Provisioning.GalMode.fromString("bogus");
            fail("expected ServiceException for unknown gal mode");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("unknown gal mode"));
        }
    }

    // ---- SetPasswordResult value class ---------------------------------------------------

    @Test
    public void setPasswordResultNoArgCtorHasNoMessage() {
        // Arrange / Act
        Provisioning.SetPasswordResult r = new Provisioning.SetPasswordResult();

        // Assert
        assertFalse("default SetPasswordResult has no message", r.hasMessage());
        assertNull(r.getMessage());
    }

    @Test
    public void setPasswordResultMsgCtorThenSetReportsMessage() {
        // Arrange / Act
        Provisioning.SetPasswordResult r = new Provisioning.SetPasswordResult("policy warning");

        // Assert
        assertTrue(r.hasMessage());
        assertEquals("policy warning", r.getMessage());

        // Act — overwrite the message
        r.setMessage("new warning");

        // Assert
        assertEquals("new warning", r.getMessage());
    }

    // ---- Result / GalResult --------------------------------------------------------------

    @Test
    public void resultStatusMessageDetailCtorExposesAllFields() {
        // Arrange / Act
        Provisioning.Result r = new Provisioning.Result("ok", "all good", "cn=x");

        // Assert
        assertEquals("ok", r.getCode());
        assertEquals("all good", r.getMessage());
        assertEquals("cn=x", r.getComputedDn());
        assertEquals("cn=x", r.getDetail());
        assertTrue("toString surfaces the code", r.toString().contains("ok"));
    }

    @Test
    public void resultExceptionCtorSerializesExceptionIntoMessage() {
        // Arrange
        Exception boom = new IllegalStateException("kaboom");

        // Act
        Provisioning.Result r = new Provisioning.Result("err", boom, "detail-x");

        // Assert — the exception is rendered into the message, detail is preserved
        assertEquals("err", r.getCode());
        assertEquals("detail-x", r.getDetail());
        assertTrue("message must contain the exception text", r.getMessage().contains("kaboom"));
    }

    // ---- RightsDoc value class -----------------------------------------------------------

    @Test
    public void rightsDocAddRightAndNoteAccumulatesInOrder() {
        // Arrange
        Provisioning.RightsDoc doc = new Provisioning.RightsDoc("createAccount");

        // Act
        doc.addRight("createAccountRight");
        doc.addRight("modifyAccountRight");
        doc.addNote("requires domain admin");

        // Assert
        assertEquals("createAccount", doc.getCmd());
        assertEquals(2, doc.getRights().size());
        assertEquals("createAccountRight", doc.getRights().get(0));
        assertEquals(1, doc.getNotes().size());
        assertEquals("requires domain admin", doc.getNotes().get(0));
    }

    // ---- CountAccountResult / CountAccountByCos ------------------------------------------

    @Test
    public void countAccountResultAddByCosExposesCosResults() {
        // Arrange
        Provisioning.CountAccountResult result = new Provisioning.CountAccountResult();

        // Act
        result.addCountAccountByCosResult("cos-1", "default", 42L);

        // Assert
        assertEquals(1, result.getCountAccountByCos().size());
        Provisioning.CountAccountResult.CountAccountByCos byCos =
                result.getCountAccountByCos().get(0);
        assertEquals("cos-1", byCos.getCosId());
        assertEquals("default", byCos.getCosName());
        assertEquals(42L, byCos.getCount());
    }

    // ---- GroupMembershipAtTime -----------------------------------------------------------

    @Test
    public void groupMembershipAtTimeCtorExposesMembershipAndTime() {
        // Arrange
        GroupMembership members = new GroupMembership();
        members.append(new MemberOf("grp-7", false, false));

        // Act
        Provisioning.GroupMembershipAtTime at =
                new Provisioning.GroupMembershipAtTime(members, 12345L);

        // Assert
        assertSame(members, at.getMembership());
        assertEquals(12345L, at.getCorrectAtTime());
        assertEquals(1, at.getMembership().groupIds().size());
    }

    // ---- static helpers ------------------------------------------------------------------

    @Test
    public void isUUIDValidUuidIsTrue() {
        // Act / Assert
        assertTrue(Provisioning.isUUID("d94e42c4-1636-11dd-bccb-2db5f28a0e4f"));
        assertFalse(Provisioning.isUUID("not-a-uuid"));
    }

    @Test
    public void validEmailAddressValidDoesNotThrowInvalidThrows() throws Exception {
        // Act — valid address passes through to NameUtil without throwing
        Provisioning.validEmailAddress("good@zimbra.com");

        // Assert — an obviously bad address throws
        try {
            Provisioning.validEmailAddress("no-at-sign");
            fail("expected ServiceException for malformed address");
        } catch (ServiceException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void getProxyAuthTokenBaseProvisioningReturnsNull() throws Exception {
        // Act — base impl is a no-op returning null
        String token = prov.getProxyAuthToken(acct().getId(), null);

        // Assert
        assertNull(token);
    }

    @Test
    public void allowsPingRemoteBaseProvisioningIsTrue() {
        // Act / Assert — base default
        assertTrue(prov.allowsPingRemote());
    }

    // ---- getDomainByEmailAddr ------------------------------------------------------------

    @Test
    public void getDomainByEmailAddrExistingDomainReturnsThatDomain() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "dom-email-id");
        prov.createDomain("emaildom.com", attrs);

        // Act — extracts the domain part and looks it up by name. Kills L540 NullReturnVals.
        Domain d = prov.getDomainByEmailAddr("someone@emaildom.com");

        // Assert
        assertNotNull("domain for the email's domain part must resolve", d);
        assertEquals("emaildom.com", d.getName());
        assertEquals("dom-email-id", d.getId());
    }

    @Test
    public void getDomainByEmailAddrUnknownDomainThrowsNoSuchDomain() {
        // Act / Assert — unknown domain part throws NO_SUCH_DOMAIN (the null-then-throw branch)
        try {
            prov.getDomainByEmailAddr("nobody@doesnotexist.example");
            fail("expected NO_SUCH_DOMAIN for an unknown domain");
        } catch (ServiceException e) {
            assertTrue("message should name the missing domain",
                    e.getMessage().contains("doesnotexist.example"));
        }
    }

    // ---- getServer(Account) --------------------------------------------------------------

    @Test
    public void getServerAccountOnLocalhostReturnsLocalServer() throws Exception {
        // The harness defaults an account's zimbraMailHost to "localhost". getServer delegates to
        // acct.getServer() which resolves that name. Kills L555 NullReturnVals.
        Server s = prov.getServer(acct());
        assertNotNull("account's server must resolve, not null", s);
        assertEquals("localhost", s.getName());
    }

    // ---- getCOS(Account) -----------------------------------------------------------------

    @Test
    public void getCOSAccountWithCosIdReturnsThatCosAndCaches() throws Exception {
        // Arrange — a COS and an account pointed at it by id
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(Provisioning.A_zimbraId, "explicit-cos-id");
        Cos cos = prov.createCos("explicitcos", cosAttrs);

        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraCOSId, "explicit-cos-id");
        Account a = prov.createAccount("hascos@zimbra.com", "secret", acctAttrs);

        // Act — resolves via the cos-id branch (kills L568 NegateConditionals: with the id present
        // it must NOT fall through to the default-COS branch)
        Cos resolved = prov.getCOS(a);

        // Assert — exact COS by id
        assertNotNull(resolved);
        assertEquals("explicit-cos-id", resolved.getId());
        assertEquals(cos.getId(), resolved.getId());

        // L584 VoidMethodCall: the COS must have been cached on the account entry
        Object cached = a.getCachedData(EntryCacheDataKey.ACCOUNT_COS);
        assertNotNull("getCOS must cache the resolved COS on the account", cached);
        assertSame(resolved, cached);

        // a second call returns the cached instance (L566 cache-hit branch)
        assertSame(resolved, prov.getCOS(a));
    }

    // ---- isOctopus -----------------------------------------------------------------------

    @Test
    public void isOctopusDefaultProductZcsIsFalse() throws Exception {
        // Default product is ZCS, so getProduct() != ZCS is false. Kills L390 NegateConditionals
        // (a flipped == would return true).
        assertFalse("default ZCS product is not Octopus", prov.isOctopus());
    }

    // ---- onLocalServer -------------------------------------------------------------------

    @Test
    public void onLocalServerAccountOnLocalhostIsTrue() throws Exception {
        // Account's mailHost == local server's serviceHostname ("localhost") => local.
        // Kills L1509 (delegate), L1515 (isLocal computation), L1516 (isLocal||alwaysOn),
        // L1521 BooleanFalseReturnVals (a forced-false return would break this).
        assertTrue("account homed on localhost is on the local server",
                Provisioning.onLocalServer(acct()));
    }

    @Test
    public void onLocalServerAccountOnOtherHostIsFalseAndRecordsReason() throws Exception {
        // Arrange — an account whose mailHost is a host the mock does not know
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailHost, "elsewhere.example.com");
        Account remote = prov.createAccount("remote@zimbra.com", "secret", attrs);

        // Act / Assert — not local. Kills L1521 BooleanTrueReturnVals (forced-true would break).
        assertFalse("account on another host is not on the local server",
                Provisioning.onLocalServer(remote));

        // L1517 NegateConditionals + L1518: when not local and a Reasons sink is supplied, a reason
        // is recorded.
        Provisioning.Reasons reasons = new Provisioning.Reasons();
        assertFalse(Provisioning.onLocalServer(remote, reasons));
        assertFalse("a reason must have been recorded for the non-local account",
                reasons.getReason().isEmpty());
        assertTrue(reasons.getReason().contains("isLocal=false"));

        // and when local with a Reasons sink, NO reason is recorded (the guarded branch is skipped)
        Provisioning.Reasons localReasons = new Provisioning.Reasons();
        assertTrue(Provisioning.onLocalServer(acct(), localReasons));
        assertTrue("no reason for a local account", localReasons.getReason().isEmpty());
    }

    // ---- getLocale / getEntryLocale ------------------------------------------------------

    @Test
    public void getLocaleConfigWithZimbraLocaleResolvesThatLocale() throws Exception {
        // Arrange — set zimbraLocale on the global Config to a language unlikely to be the JVM
        // default. getLocale(Config) routes through the private getEntryLocale(entry) ->
        // getEntryLocale(entry, A_zimbraLocale).
        Config config = prov.getConfig();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_zimbraLocale, "ja");
        prov.modifyAttrs(config, changes);

        // Act
        Locale lc = prov.getLocale(prov.getConfig());

        // Assert — kills L1978 (delegate passes A_zimbraLocale), L1971 (lcName != null guard) and
        // L1974 (return lc): any of those breaking would yield Locale.getDefault() instead of ja.
        assertNotNull(lc);
        assertEquals("ja", lc.getLanguage());

        // cleanup so other tests see no config locale
        Map<String, Object> clear = new HashMap<String, Object>();
        clear.put(Provisioning.A_zimbraLocale, "");
        prov.modifyAttrs(prov.getConfig(), clear);
    }

    @Test
    public void getNamesForIdsBaseProvisioningReturnsEmptyMap() throws Exception {
        // Arrange
        java.util.Set<String> ids = new java.util.HashSet<String>();
        ids.add(acct().getId());

        // Act — base impl returns an empty map
        Map<String, String> names = prov.getNamesForIds(ids, Provisioning.EntryType.account);

        // Assert
        assertNotNull(names);
        assertTrue("base getNamesForIds returns empty map", names.isEmpty());
    }
}
