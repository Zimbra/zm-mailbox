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
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
 * Functional tests for {@link GranteeType}: code/JAXB round-trips, flag-derived predicates,
 * and grantee lookup against the in-memory MockProvisioning harness.
 */
public class GranteeTypeTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    @Test
    public void fromCodeKnownCodeReturnsMatchingEnum() throws Exception {
        // Act
        GranteeType gt = GranteeType.fromCode("usr");

        // Assert
        assertSame(GranteeType.GT_USER, gt);
        assertEquals("usr", gt.getCode());
    }

    @Test
    public void fromCodeAllCodesRoundTripToCode() throws Exception {
        // Arrange / Act / Assert — every code resolves back to itself
        for (GranteeType gt : GranteeType.values()) {
            assertSame("code " + gt.getCode() + " must round-trip",
                    gt, GranteeType.fromCode(gt.getCode()));
        }
    }

    @Test
    public void fromCodeUnknownCodeThrowsParseError() {
        try {
            GranteeType.fromCode("no-such-code");
            fail("expected ServiceException for invalid grantee code");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("invalid grantee type"));
        }
    }

    @Test
    public void toJaxbAndFromJaxbRoundTripReturnsSameEnum() {
        // Arrange
        GranteeType original = GranteeType.GT_GROUP;

        // Act
        com.zimbra.soap.type.GranteeType jaxb = original.toJaxb();
        GranteeType back = GranteeType.fromJaxb(jaxb);

        // Assert
        assertEquals(com.zimbra.soap.type.GranteeType.grp, jaxb);
        assertSame(original, back);
    }

    @Test
    public void fromJaxbEveryValueResolves() {
        for (GranteeType gt : GranteeType.values()) {
            assertSame(gt, GranteeType.fromJaxb(gt.toJaxb()));
        }
    }

    @Test
    public void allowedForAdminRightsUserVsPublicReflectsAdminFlag() {
        // GT_USER carries F_ADMIN, GT_PUBLIC does not
        assertTrue(GranteeType.GT_USER.allowedForAdminRights());
        assertFalse(GranteeType.GT_PUBLIC.allowedForAdminRights());
    }

    @Test
    public void allowSecretGuestVsUserReflectsSecretFlag() {
        assertTrue(GranteeType.GT_GUEST.allowSecret());
        assertTrue(GranteeType.GT_KEY.allowSecret());
        assertFalse(GranteeType.GT_USER.allowSecret());
    }

    @Test
    public void isZimbraEntryUserVsGuestReflectsZimbraEntryFlag() {
        assertTrue(GranteeType.GT_USER.isZimbraEntry());
        assertTrue(GranteeType.GT_DOMAIN.isZimbraEntry());
        assertFalse(GranteeType.GT_GUEST.isZimbraEntry());
    }

    @Test
    public void needsGranteeIdentityAuthuserAndPublicReturnFalseOthersTrue() {
        assertFalse(GranteeType.GT_AUTHUSER.needsGranteeIdentity());
        assertFalse(GranteeType.GT_PUBLIC.needsGranteeIdentity());
        assertTrue(GranteeType.GT_USER.needsGranteeIdentity());
        assertTrue(GranteeType.GT_GUEST.needsGranteeIdentity());
    }

    @Test
    public void hasFlagsIndividualFlagMatchesGuest() {
        assertTrue("guest is an individual", GranteeType.GT_GUEST.hasFlags(GranteeFlag.F_INDIVIDUAL));
        assertFalse("group is not an individual", GranteeType.GT_GROUP.hasFlags(GranteeFlag.F_INDIVIDUAL));
    }

    @Test
    public void lookupGranteeExistingUserReturnsAccount() throws Exception {
        // Arrange — create a real account in the harness
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("lookupuser@example.com", "pw", attrs);

        // Act
        NamedEntry found = GranteeType.lookupGrantee(prov, GranteeType.GT_USER,
                GranteeBy.name, "lookupuser@example.com", true);

        // Assert
        assertNotNull(found);
        assertEquals(acct.getId(), found.getId());

        // Cleanup
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void lookupGranteeMissingUserMustFindThrowsNoSuchAccount() {
        try {
            GranteeType.lookupGrantee(prov, GranteeType.GT_USER, GranteeBy.name,
                    "ghost@example.com", true);
            fail("expected NO_SUCH_ACCOUNT");
        } catch (ServiceException e) {
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_ACCOUNT, e.getCode());
        }
    }

    @Test
    public void lookupGranteeGuestTypeReturnsGuestAccountWithoutLookup() throws Exception {
        // Act — guest path never touches provisioning
        NamedEntry found = GranteeType.lookupGrantee(prov, GranteeType.GT_GUEST,
                GranteeBy.name, "guest@elsewhere.com", true);

        // Assert
        assertNotNull(found);
        assertTrue(found instanceof GuestAccount);
        assertEquals("guest@elsewhere.com", found.getName());
    }

    @Test
    public void lookupGranteeInvalidTypeForLookupThrowsInvalidRequest() {
        try {
            GranteeType.lookupGrantee(prov, GranteeType.GT_PUBLIC, GranteeBy.name,
                    "anything", true);
            fail("expected INVALID_REQUEST for non-lookupable grantee type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void determineGranteeTypeNonEmailTypeReturnedUnchanged() throws Exception {
        // Act
        GranteeType resolved = GranteeType.determineGranteeType(GranteeType.GT_USER,
                GranteeBy.name, "whoever@example.com", "example.com");

        // Assert — non-email types pass straight through
        assertSame(GranteeType.GT_USER, resolved);
    }

    @Test
    public void determineGranteeTypeEmailMatchingAccountResolvesToUser() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("realuser@example.com", "pw", attrs);

        // Act
        GranteeType resolved = GranteeType.determineGranteeType(GranteeType.GT_EMAIL,
                GranteeBy.name, "realuser@example.com", "example.com");

        // Assert
        assertSame(GranteeType.GT_USER, resolved);

        // Cleanup
        prov.deleteAccount(acct.getId());
    }

    // ---- appended functional tests covering previously-uncovered branches ----

    @Test
    public void fromJaxbUnrecognisedValueThrowsIllegalArgument() {
        // Arrange — null is not mapped to any GranteeType
        try {
            // Act
            GranteeType.fromJaxb(null);
            fail("expected IllegalArgumentException for unrecognised JAXB grantee type");
        } catch (IllegalArgumentException e) {
            // Assert
            assertTrue(e.getMessage().contains("Unrecognised GranteeType"));
        }
    }

    @Test
    public void lookupGranteeMissingGroupMustFindThrowsNoSuchDistributionList() {
        // Arrange — getGroupBasic returns null in the harness; GT_GROUP path must throw when mustFind
        try {
            // Act
            GranteeType.lookupGrantee(prov, GranteeType.GT_GROUP, GranteeBy.name,
                    "nogroup@example.com", true);
            fail("expected NO_SUCH_DISTRIBUTION_LIST");
        } catch (ServiceException e) {
            // Assert
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_DISTRIBUTION_LIST,
                    e.getCode());
        }
    }

    @Test
    public void lookupGranteeMissingGroupNotMustFindReturnsNull() throws Exception {
        // Act — without mustFind the GT_GROUP path returns the (null) lookup result
        NamedEntry found = GranteeType.lookupGrantee(prov, GranteeType.GT_GROUP,
                GranteeBy.name, "nogroup@example.com", false);

        // Assert
        assertNull("missing group without mustFind must be null", found);
    }

    @Test
    public void lookupGranteeExistingDomainReturnsDomain() throws Exception {
        // Arrange — create a real domain in the harness
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        com.zimbra.cs.account.Domain dom = prov.createDomain("granteedom.example.com", attrs);

        // Act
        NamedEntry found = GranteeType.lookupGrantee(prov, GranteeType.GT_DOMAIN,
                GranteeBy.name, "granteedom.example.com", true);

        // Assert
        assertNotNull(found);
        assertEquals(dom.getId(), found.getId());
    }

    @Test
    public void lookupGranteeMissingDomainMustFindThrowsNoSuchDomain() {
        // Act / Assert — GT_DOMAIN path throws when the domain is absent and mustFind is set
        try {
            GranteeType.lookupGrantee(prov, GranteeType.GT_DOMAIN, GranteeBy.name,
                    "ghostdom.example.com", true);
            fail("expected NO_SUCH_DOMAIN");
        } catch (ServiceException e) {
            assertTrue("message must indicate the missing domain",
                    e.getMessage().contains("no such domain"));
        }
    }

    @Test
    public void lookupGranteeEmailMatchingAccountReturnsAccount() throws Exception {
        // Arrange — GT_EMAIL first resolves an internal account
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("emailgrantee@example.com", "pw", attrs);

        // Act
        NamedEntry found = GranteeType.lookupGrantee(prov, GranteeType.GT_EMAIL,
                GranteeBy.name, "emailgrantee@example.com", true);

        // Assert — the internal account is returned, not a guest
        assertNotNull(found);
        assertEquals(acct.getId(), found.getId());
        assertFalse("an internal account must not resolve to a guest", found instanceof GuestAccount);

        // Cleanup
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void lookupGranteeEmailNoMatchFallsBackToGuest() throws Exception {
        // Act — no account, no group, no external group => guest fallback
        NamedEntry found = GranteeType.lookupGrantee(prov, GranteeType.GT_EMAIL,
                GranteeBy.name, "stranger@nowhere.com", true);

        // Assert
        assertNotNull(found);
        assertTrue("unknown email must fall back to a guest", found instanceof GuestAccount);
        assertEquals("stranger@nowhere.com", found.getName());
    }

    @Test
    public void lookupGranteeTwoArgOverloadDelegatesWithMustFind() {
        // Act / Assert — the 4-arg overload defaults mustFind=true, so a missing user throws
        try {
            GranteeType.lookupGrantee(prov, GranteeType.GT_USER, GranteeBy.name,
                    "ghost2@example.com");
            fail("expected NO_SUCH_ACCOUNT from mustFind=true overload");
        } catch (ServiceException e) {
            assertEquals(com.zimbra.cs.account.AccountServiceException.NO_SUCH_ACCOUNT, e.getCode());
        }
    }

    @Test
    public void lookupGranteeSelectorOverloadResolvesAccount() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("selectoruser@example.com", "pw", attrs);

        com.zimbra.soap.admin.type.GranteeSelector selector =
                new com.zimbra.soap.admin.type.GranteeSelector(
                        com.zimbra.soap.type.GranteeType.usr, GranteeBy.name,
                        "selectoruser@example.com");

        // Act — selector overload defaults mustFind=true
        NamedEntry found = GranteeType.lookupGrantee(prov, selector);

        // Assert
        assertNotNull(found);
        assertEquals(acct.getId(), found.getId());

        // Cleanup
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void lookupGranteeSelectorOverloadMustFindFlagReturnsNullWhenAbsent() throws Exception {
        // Arrange — selector for a missing user with mustFind=false
        com.zimbra.soap.admin.type.GranteeSelector selector =
                new com.zimbra.soap.admin.type.GranteeSelector(
                        com.zimbra.soap.type.GranteeType.usr, GranteeBy.name,
                        "missingselector@example.com");

        // Act
        NamedEntry found = GranteeType.lookupGrantee(prov, selector, false);

        // Assert
        assertNull("missing user via selector without mustFind must be null", found);
    }

    @Test
    public void determineGranteeTypeEmailNoMatchResolvesToGuest() throws Exception {
        // Act — an email that matches nothing resolves to guest
        GranteeType resolved = GranteeType.determineGranteeType(GranteeType.GT_EMAIL,
                GranteeBy.name, "unknownperson@nowhere.com", "nowhere.com");

        // Assert
        assertSame(GranteeType.GT_GUEST, resolved);
    }
}
