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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ZimbraACE.ExternalGroupInfo;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ZimbraACE}. Builds real ACE objects through the public
 * constructor (which needs no LDAP / RightManager) and verifies grantee resolution,
 * right-modifier predicates, serialization round-trips, deep-copy cloning, validation, and the
 * {@link ExternalGroupInfo} encode/parse helper. Rights are real {@link UserRight} instances.
 */
public class ZimbraACEFunctionalTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    /* Build a concrete user Right without needing RightManager to be initialized. */
    private Right userRight(String name) {
        return new UserRight(name);
    }

    @Test
    public void ctorUserGranteeStoresGranteeTypeAndRight() throws Exception {
        // Arrange
        Right right = userRight("viewFreeBusy");

        // Act
        ZimbraACE ace = new ZimbraACE("id-123", GranteeType.GT_USER, right, null, null);

        // Assert — constructor preserves all supplied fields.
        assertEquals("id-123", ace.getGrantee());
        assertEquals(GranteeType.GT_USER, ace.getGranteeType());
        assertEquals(right, ace.getRight());
        assertNull(ace.getSecret());
    }

    @Test
    public void ctorAuthUserGranteeOverridesGranteeWithWellKnownGuid() throws Exception {
        // Act — GT_AUTHUSER forces the grantee id to the AUTHUSER pseudo-GUID.
        ZimbraACE ace = new ZimbraACE("ignored", GranteeType.GT_AUTHUSER,
                userRight("viewFreeBusy"), null, null);

        // Assert
        assertEquals(GuestAccount.GUID_AUTHUSER, ace.getGrantee());
        assertEquals(GranteeType.GT_AUTHUSER, ace.getGranteeType());
    }

    @Test
    public void ctorPublicGranteeOverridesGranteeWithPublicGuid() throws Exception {
        // Act — GT_PUBLIC forces the grantee id to the PUBLIC pseudo-GUID.
        ZimbraACE ace = new ZimbraACE("ignored", GranteeType.GT_PUBLIC,
                userRight("invite"), null, null);

        // Assert
        assertEquals(GuestAccount.GUID_PUBLIC, ace.getGrantee());
    }

    @Test
    public void rightModifierPredicatesDenyModifierOnlyDenyTrue() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("id-1", GranteeType.GT_USER,
                userRight("viewFreeBusy"), RightModifier.RM_DENY, null);

        // Act / Assert — deny() true; all other modifier predicates false.
        assertTrue(ace.deny());
        assertFalse(ace.canDelegate());
        assertFalse(ace.subDomain());
        assertFalse(ace.disinheritSubGroups());
        assertFalse("denied grant cannot execute-only", ace.canExecuteOnly());
        assertEquals(RightModifier.RM_DENY, ace.getRightModifier());
    }

    @Test
    public void rightModifierPredicatesDelegateModifierOnlyDelegateTrue() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("id-1", GranteeType.GT_USER,
                userRight("viewFreeBusy"), RightModifier.RM_CAN_DELEGATE, null);

        // Act / Assert
        assertTrue(ace.canDelegate());
        assertFalse(ace.deny());
        assertFalse("delegate grant is not execute-only", ace.canExecuteOnly());
    }

    @Test
    public void rightModifierPredicatesNoModifierCanExecuteOnlyTrue() throws Exception {
        // Arrange — a plain grant (no modifier) is execute-only.
        ZimbraACE ace = new ZimbraACE("id-1", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert
        assertTrue(ace.canExecuteOnly());
        assertFalse(ace.deny());
        assertFalse(ace.canDelegate());
        assertFalse(ace.subDomain());
        assertFalse(ace.disinheritSubGroups());
    }

    @Test
    public void rightModifierPredicatesSubDomainAndDisinheritReportCorrectly() throws Exception {
        // Arrange
        ZimbraACE sub = new ZimbraACE("id-1", GranteeType.GT_USER,
                userRight("viewFreeBusy"), RightModifier.RM_SUBDOMAIN, null);
        ZimbraACE dis = new ZimbraACE("id-2", GranteeType.GT_USER,
                userRight("viewFreeBusy"), RightModifier.RM_DISINHERIT_SUB_GROUPS, null);

        // Act / Assert
        assertTrue(sub.subDomain());
        assertTrue(dis.disinheritSubGroups());
    }

    @Test
    public void isGranteeUserIdMatchesExactGrantee() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("user-id-9", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — exact id matches, a different id does not.
        assertTrue(ace.isGrantee("user-id-9"));
        assertFalse(ace.isGrantee("someone-else"));
    }

    @Test
    public void isGranteePublicGranteeMatchesNullAndPublicGuid() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("ignored", GranteeType.GT_PUBLIC,
                userRight("invite"), null, null);

        // Act / Assert — null and the PUBLIC GUID both count as the public grantee.
        assertTrue("null principal is public", ace.isGrantee(null));
        assertTrue(ace.isGrantee(GuestAccount.GUID_PUBLIC));
    }

    @Test
    public void isGranteeAuthUserGranteeMatchesAuthUserGuid() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("ignored", GranteeType.GT_AUTHUSER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert
        assertTrue(ace.isGrantee(GuestAccount.GUID_AUTHUSER));
        assertFalse("auth-user does not match public", ace.isGrantee(GuestAccount.GUID_PUBLIC));
    }

    @Test
    public void serializeUserGrantNoModifierProducesGranteeTypeRightForm() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("abc-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act — serialized form is "{grantee} {granteeType} {right}".
        String s = ace.serialize();

        // Assert
        assertEquals("abc-id usr viewFreeBusy", s);
    }

    @Test
    public void serializeDenyModifierPrependsMinusSign() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("abc-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), RightModifier.RM_DENY, null);

        // Act
        String s = ace.serialize();

        // Assert — the deny modifier '-' precedes the right name.
        assertEquals("abc-id usr -viewFreeBusy", s);
    }

    @Test
    public void serializeGuestGranteeIncludesSecretDelimited() throws Exception {
        // Arrange — guest grantees serialize as {grantee}:{secret}.
        ZimbraACE ace = new ZimbraACE("foo@bar.com", GranteeType.GT_GUEST,
                userRight("viewFreeBusy"), null, "appletree");

        // Act
        String s = ace.serialize();

        // Assert
        assertEquals("foo@bar.com:appletree gst viewFreeBusy", s);
    }

    @Test
    public void setSecretAndSetRightMutateState() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("id-1", GranteeType.GT_KEY,
                userRight("viewFreeBusy"), null, "key1");

        // Act — mutate secret and right.
        ace.setSecret("key2");
        Right newRight = userRight("invite");
        ace.setRight(newRight);

        // Assert
        assertEquals("key2", ace.getSecret());
        assertEquals(newRight, ace.getRight());
    }

    @Test
    public void cloneDeepCopyIndependentButEqualState() throws Exception {
        // Arrange
        ZimbraACE original = new ZimbraACE("id-1", GranteeType.GT_GUEST,
                userRight("viewFreeBusy"), RightModifier.RM_DENY, "secretX");

        // Act
        ZimbraACE copy = original.clone();

        // Assert — clone is a distinct object carrying identical state.
        assertNotNull(copy);
        assertTrue("clone must be a distinct instance", copy != original);
        assertEquals(original.getGrantee(), copy.getGrantee());
        assertEquals(original.getGranteeType(), copy.getGranteeType());
        assertEquals(original.getRight(), copy.getRight());
        assertEquals(original.getRightModifier(), copy.getRightModifier());
        assertEquals(original.getSecret(), copy.getSecret());
        // Mutating the clone must not affect the original.
        copy.setSecret("changed");
        assertEquals("secretX", original.getSecret());
    }

    @Test
    public void validateGuestSecretWithDelimiterThrowsInvalidRequest() throws Exception {
        // Arrange — a guest secret containing the ':' delimiter is illegal.
        ZimbraACE ace = new ZimbraACE("foo@bar.com", GranteeType.GT_GUEST,
                userRight("viewFreeBusy"), null, "bad:secret");

        // Act / Assert
        try {
            ZimbraACE.validate(ace);
            fail("expected INVALID_REQUEST for delimiter in secret");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("cannot contain"));
        }
    }

    @Test
    public void validateUserGranteePassesRegardlessOfContent() throws Exception {
        // Arrange — validation only constrains guest/key grantees.
        ZimbraACE ace = new ZimbraACE("any:thing", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — no exception for non guest/key grantee types.
        ZimbraACE.validate(ace);
    }

    @Test
    public void externalGroupInfoEncodeThenParseRoundTrips() throws Exception {
        // Act — encode joins domain and group with ':'; parse splits them back.
        String encoded = ZimbraACE.ExternalGroupInfo.encodeIfExtGroupNameMissingDomain(
                "domain-id", "groupName");
        ExternalGroupInfo info = ZimbraACE.ExternalGroupInfo.parse(encoded);

        // Assert
        assertEquals("domain-id:groupName", encoded);
        assertEquals("groupName", info.getExternalGroupName());
    }

    @Test
    public void externalGroupInfoParseBadGranteeThrowsParseError() throws Exception {
        // Act / Assert — missing the ':' separator is a parse error.
        try {
            ZimbraACE.ExternalGroupInfo.parse("noColonHere");
            fail("expected PARSE_ERROR for malformed external group grantee");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid external group grantee"));
        }
    }

    @Test
    public void toStringUserGrantContainsSerializedForm() throws Exception {
        // Arrange
        ZimbraACE ace = new ZimbraACE("abc-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act
        String dump = ace.toString();

        // Assert — non-verbose dump embeds the serialized ACE.
        assertTrue(dump.contains("abc-id usr viewFreeBusy"));
    }

    // ------------------------------------------------------------------
    // LDAP-style deserialization constructor: ZimbraACE(String, RightManager, ...)
    // ------------------------------------------------------------------

    private static final String UUID_A = "11111111-2222-3333-4444-555555555555";

    @Test
    public void ldapCtorUserGrantParsesGranteeTypeAndRight() throws Exception {
        // Arrange — a real RightManager (loaded from packaged rights files, no LDAP).
        RightManager rm = RightManager.getInstance();

        // Act — deserialize "{uuid} usr viewFreeBusy".
        ZimbraACE ace = new ZimbraACE(UUID_A + " usr viewFreeBusy", rm,
                TargetType.account, "target@example.com");

        // Assert — grantee, type and target metadata round-trip.
        assertEquals(UUID_A, ace.getGrantee());
        assertEquals(GranteeType.GT_USER, ace.getGranteeType());
        assertEquals("viewFreeBusy", ace.getRight().getName());
        assertEquals(TargetType.account, ace.getTargetType());
        assertEquals("target@example.com", ace.getTargetName());
        assertNull("plain right has no modifier", ace.getRightModifier());
    }

    @Test
    public void ldapCtorDenyModifierParsesNegativeRight() throws Exception {
        // Arrange
        RightManager rm = RightManager.getInstance();

        // Act — leading '-' marks a deny grant.
        ZimbraACE ace = new ZimbraACE(UUID_A + " grp -viewFreeBusy", rm,
                TargetType.account, "tgt");

        // Assert — modifier is DENY and the right name strips the sign.
        assertTrue(ace.deny());
        assertEquals(RightModifier.RM_DENY, ace.getRightModifier());
        assertEquals("viewFreeBusy", ace.getRight().getName());
        assertEquals(GranteeType.GT_GROUP, ace.getGranteeType());
    }

    @Test
    public void ldapCtorGuestGrantWithSecretSplitsGranteeAndSecret() throws Exception {
        // Arrange
        RightManager rm = RightManager.getInstance();

        // Act — guest serialized as "{email}:{secret} gst {right}".
        ZimbraACE ace = new ZimbraACE("foo@bar.com:appletree gst viewFreeBusy", rm,
                TargetType.account, "tgt");

        // Assert — grantee and secret separated on the ':' delimiter.
        assertEquals(GranteeType.GT_GUEST, ace.getGranteeType());
        assertEquals("foo@bar.com", ace.getGrantee());
        assertEquals("appletree", ace.getSecret());
    }

    @Test
    public void ldapCtorGuestGrantNoSecretLeavesSecretNull() throws Exception {
        // Arrange
        RightManager rm = RightManager.getInstance();

        // Act — guest grantee with no secret part.
        ZimbraACE ace = new ZimbraACE("foo@bar.com gst viewFreeBusy", rm,
                TargetType.account, "tgt");

        // Assert
        assertEquals("foo@bar.com", ace.getGrantee());
        assertNull(ace.getSecret());
    }

    @Test
    public void ldapCtorExtDomainGrantStoresDomainName() throws Exception {
        // Arrange
        RightManager rm = RightManager.getInstance();

        // Act — external domain grantee stores the domain name verbatim.
        ZimbraACE ace = new ZimbraACE("partner.com edom viewFreeBusy", rm,
                TargetType.account, "tgt");

        // Assert
        assertEquals(GranteeType.GT_EXT_DOMAIN, ace.getGranteeType());
        assertEquals("partner.com", ace.getGrantee());
    }

    @Test
    public void ldapCtorExtGroupGrantKeepsCombinedGrantee() throws Exception {
        // Arrange
        RightManager rm = RightManager.getInstance();
        String combined = UUID_A + ":group@external.com";

        // Act — external group keeps {domainId}:{groupName} undivided.
        ZimbraACE ace = new ZimbraACE(combined + " egp viewFreeBusy", rm,
                TargetType.account, "tgt");

        // Assert
        assertEquals(GranteeType.GT_EXT_GROUP, ace.getGranteeType());
        assertEquals(combined, ace.getGrantee());
    }

    @Test
    public void ldapCtorNonUuidUserGranteeThrowsParseError() throws Exception {
        // Arrange — usr/grp/dom grantees must be UUIDs.
        RightManager rm = RightManager.getInstance();

        // Act / Assert
        try {
            new ZimbraACE("not-a-uuid usr viewFreeBusy", rm, TargetType.account, "tgt");
            fail("expected PARSE_ERROR for non-UUID user grantee");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("is not a UUID"));
        }
    }

    @Test
    public void ldapCtorSingleTokenThrowsParseError() throws Exception {
        // Arrange — an ACE without spaces cannot be split into 3 parts.
        RightManager rm = RightManager.getInstance();

        // Act / Assert — getParts finds no delimiter.
        try {
            new ZimbraACE("oneword", rm, TargetType.account, "tgt");
            fail("expected PARSE_ERROR for ACE without delimiters");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("bad ACE"));
        }
    }

    @Test
    public void ldapCtorOnlyOneDelimiterThrowsParseError() throws Exception {
        // Arrange — only a single space => second lastIndexOf fails.
        RightManager rm = RightManager.getInstance();

        // Act / Assert
        try {
            new ZimbraACE("usr viewFreeBusy", rm, TargetType.account, "tgt");
            fail("expected PARSE_ERROR for ACE with one delimiter");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("bad ACE"));
        }
    }

    @Test
    public void externalGroupInfoEncodeWhenNameAlreadyHasDomainReturnsUnchanged() throws Exception {
        // Act — if the external group name already contains ':' the domain is not re-prepended.
        String encoded = ZimbraACE.ExternalGroupInfo.encodeIfExtGroupNameMissingDomain(
                "domain-id", "other-domain:groupName");

        // Assert — the already-qualified value is returned verbatim.
        assertEquals("other-domain:groupName", encoded);
    }

    // ------------------------------------------------------------------
    // matchesGrantee / matches — exercised through the package-private entry point
    // ------------------------------------------------------------------

    @Test
    public void matchesGranteePublicGranteeMatchesAnyTarget() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.createAccount("ace-pub@example.com", "test123",
                new HashMap<String, Object>());
        ZimbraACE ace = new ZimbraACE("ignored", GranteeType.GT_PUBLIC,
                userRight("invite"), null, null);

        // Act / Assert — public grants always match (even a null target).
        assertTrue(ace.matchesGrantee(acct, true));
        assertTrue("public matches null target", ace.matchesGrantee(null, true));
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void matchesGranteeAuthUserMatchesRealAccountButNotGuest() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.createAccount("ace-auth@example.com", "test123",
                new HashMap<String, Object>());
        ZimbraACE ace = new ZimbraACE("ignored", GranteeType.GT_AUTHUSER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — a real account is an authenticated user; a guest is not.
        assertTrue(ace.matchesGrantee(acct, true));
        assertFalse(ace.matchesGrantee(new GuestAccount("guest@x.com", "pw"), true));
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void matchesGranteeUserGranteeMatchesById() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "match-user-id");
        Account acct = prov.createAccount("ace-user@example.com", "test123", attrs);
        ZimbraACE ace = new ZimbraACE("match-user-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — user grantee matches the target whose id equals the grantee id.
        assertTrue(ace.matchesGrantee(acct, true));
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void matchesGranteeUserGranteeDifferentIdDoesNotMatch() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "actual-id");
        Account acct = prov.createAccount("ace-user2@example.com", "test123", attrs);
        ZimbraACE ace = new ZimbraACE("some-other-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert
        assertFalse(ace.matchesGrantee(acct, true));
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void matchesGranteeDomainGranteeMatchesByDomainId() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.createDomain("ace-dom.example", new HashMap<String, Object>());
        Account acct = prov.createAccount("u@ace-dom.example", "test123",
                new HashMap<String, Object>());
        ZimbraACE ace = new ZimbraACE(domain.getId(), GranteeType.GT_DOMAIN,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — domain grantee matches when the target's domain id equals the grantee.
        assertTrue(ace.matchesGrantee(acct, true));
        prov.deleteAccount(acct.getId());
        prov.deleteDomain(domain.getId());
    }

    @Test
    public void matchesGranteeEmailGranteeAccountMatchesByNameIgnoreCase() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.createAccount("ace-email@example.com", "test123",
                new HashMap<String, Object>());
        ZimbraACE ace = new ZimbraACE("ACE-EMAIL@EXAMPLE.COM", GranteeType.GT_EMAIL,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — email grantee matches an Account by case-insensitive name.
        assertTrue(ace.matchesGrantee(acct, true));
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void matchesGranteeExtDomainGranteeMatchesGuestDomain() throws Exception {
        // Arrange — guest with domain x.com should match an edom grantee for x.com.
        GuestAccount guest = new GuestAccount("user@x.com", "pw");
        ZimbraACE ace = new ZimbraACE("x.com", GranteeType.GT_EXT_DOMAIN,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert
        assertTrue(ace.matchesGrantee(guest, true));
        ZimbraACE other = new ZimbraACE("y.com", GranteeType.GT_EXT_DOMAIN,
                userRight("viewFreeBusy"), null, null);
        assertFalse(other.matchesGrantee(guest, true));
    }

    @Test
    public void matchesGranteeGuestGranteeMatchesGuestCredentials() throws Exception {
        // Arrange — guest grant with matching email + password.
        GuestAccount guest = new GuestAccount("g@x.com", "secretpw");
        ZimbraACE ace = new ZimbraACE("g@x.com", GranteeType.GT_GUEST,
                userRight("viewFreeBusy"), null, "secretpw");

        // Act / Assert — matching guest credentials match; a non-guest target does not.
        assertTrue(ace.matchesGrantee(guest, true));
        Account plain = Provisioning.getInstance().createAccount("ace-plain@example.com",
                "test123", new HashMap<String, Object>());
        assertFalse(ace.matchesGrantee(plain, true));
        Provisioning.getInstance().deleteAccount(plain.getId());
    }

    // ------------------------------------------------------------------
    // getGranteeDisplayName — resolves names through Provisioning
    // ------------------------------------------------------------------

    @Test
    public void getGranteeDisplayNameUserGranteeReturnsAccountName() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "display-user-id");
        Account acct = prov.createAccount("display@example.com", "test123", attrs);
        ZimbraACE ace = new ZimbraACE("display-user-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — display name resolves to the account name.
        assertEquals("display@example.com", ace.getGranteeDisplayName());
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void getGranteeDisplayNameUnknownUserReturnsNull() throws Exception {
        // Arrange — no account exists with this id.
        ZimbraACE ace = new ZimbraACE("no-such-user-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert — unresolved user grantee yields null.
        assertNull(ace.getGranteeDisplayName());
    }

    @Test
    public void getGranteeDisplayNameDomainGranteeReturnsDomainName() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.createDomain("display-dom.example", new HashMap<String, Object>());
        ZimbraACE ace = new ZimbraACE(domain.getId(), GranteeType.GT_DOMAIN,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert
        assertEquals("display-dom.example", ace.getGranteeDisplayName());
        prov.deleteDomain(domain.getId());
    }

    @Test
    public void getGranteeDisplayNameExtGroupGranteeReturnsExternalGroupName() throws Exception {
        // Arrange — ext group display name is the parsed external group part.
        ZimbraACE ace = new ZimbraACE("domId:theGroup", GranteeType.GT_EXT_GROUP,
                userRight("viewFreeBusy"), null, null);

        // Act / Assert
        assertEquals("theGroup", ace.getGranteeDisplayName());
    }

    @Test
    public void getGranteeDisplayNameGuestGranteeReturnsGranteeVerbatim() throws Exception {
        // Arrange — guest/key/ext-domain just return the grantee string.
        ZimbraACE ace = new ZimbraACE("guest@x.com", GranteeType.GT_GUEST,
                userRight("viewFreeBusy"), null, "pw");

        // Act / Assert
        assertEquals("guest@x.com", ace.getGranteeDisplayName());
    }

    @Test
    public void getGranteeDisplayNamePublicGranteeReturnsNull() throws Exception {
        // Arrange — public/authuser grantees have no display name.
        ZimbraACE ace = new ZimbraACE("ignored", GranteeType.GT_PUBLIC,
                userRight("invite"), null, null);

        // Act / Assert
        assertNull(ace.getGranteeDisplayName());
    }

    @Test
    public void dumpVerboseIncludesGranteeAndRightDetails() throws Exception {
        // Arrange
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "dump-user-id");
        Account acct = prov.createAccount("dump@example.com", "test123", attrs);
        ZimbraACE ace = new ZimbraACE("dump-user-id", GranteeType.GT_USER,
                userRight("viewFreeBusy"), null, null);

        // Act — verbose dump labels every component.
        String verbose = ace.dump(true);

        // Assert
        assertTrue(verbose.contains("grantee name=dump@example.com"));
        assertTrue(verbose.contains("grantee id=dump-user-id"));
        assertTrue(verbose.contains("grantee type=usr"));
        assertTrue(verbose.contains("right=viewFreeBusy"));
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void validateGuestGranteeContainsDelimiterThrowsInvalidRequest() throws Exception {
        // Arrange — a guest grantee containing the ':' delimiter is illegal.
        ZimbraACE ace = new ZimbraACE("foo:bar@x.com", GranteeType.GT_GUEST,
                userRight("viewFreeBusy"), null, "pw");

        // Act / Assert
        try {
            ZimbraACE.validate(ace);
            fail("expected INVALID_REQUEST for delimiter in grantee");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("grantee cannot contain"));
        }
    }
}
