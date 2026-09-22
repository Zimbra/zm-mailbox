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

package com.zimbra.cs.account.auth;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AuthMechanism}: the {@link AuthMech} string parsing, the
 * {@code newInstance} factory resolving the mechanism off the account's domain (real
 * {@link Account}/{@link Domain} via the MockProvisioning harness), the {@code namePassedIn}
 * context helper, and the {@code ZimbraAuth} encoded-password predicates. No domain-object mocking.
 */
public class AuthMechanismTest {

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        prov.createDomain("example.com", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String name) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createAccount(name, "test123", attrs);
    }

    // ---------- AuthMech.fromString ----------

    @Test
    public void fromStringKnownMechReturnsEnum() throws Exception {
        // Act / Assert
        assertEquals(AuthMech.zimbra, AuthMech.fromString("zimbra"));
        assertEquals(AuthMech.ldap, AuthMech.fromString("ldap"));
        assertEquals(AuthMech.kerberos5, AuthMech.fromString("kerberos5"));
    }

    @Test
    public void fromStringNullReturnsNull() throws Exception {
        // Act / Assert
        assertNull(AuthMech.fromString(null));
    }

    @Test
    public void fromStringUnknownMechThrowsInvalidRequest() throws Exception {
        // Act / Assert
        try {
            AuthMech.fromString("bogus");
            fail("expected INVALID_REQUEST for an unknown mech");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("bogus"));
        }
    }

    // ---------- namePassedIn ----------

    @Test
    public void namePassedInPresentInContextReturnsValue() {
        // Arrange
        Map<String, Object> ctxt = new HashMap<String, Object>();
        ctxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, "user@example.com");

        // Act / Assert
        assertEquals("user@example.com", AuthMechanism.namePassedIn(ctxt));
    }

    @Test
    public void namePassedInAbsentKeyReturnsEmptyString() {
        // Act / Assert — present map but missing key
        assertEquals("", AuthMechanism.namePassedIn(new HashMap<String, Object>()));
    }

    @Test
    public void namePassedInNullContextReturnsEmptyString() {
        // Act / Assert
        assertEquals("", AuthMechanism.namePassedIn(null));
    }

    // ---------- newInstance ----------

    @Test
    public void newInstanceDomainWithNoAuthMechDefaultsToZimbraAuth() throws Exception {
        // Arrange — account in a domain that does not specify an auth mech
        Account acct = createAccount("plain@example.com");

        // Act
        AuthMechanism mech = AuthMechanism.newInstance(acct, null);

        // Assert — the default mechanism is Zimbra auth
        assertEquals(AuthMech.zimbra, mech.getMechanism());
        assertTrue("default mechanism must report isZimbraAuth", mech.isZimbraAuth());
    }

    @Test
    public void newInstanceDomainAuthMechLdapBuildsLdapAuth() throws Exception {
        // Arrange — domain advertises ldap auth
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        prov.createDomain("ldapdom.com", dattrs);
        Account acct = createAccount("u@ldapdom.com");

        // Act
        AuthMechanism mech = AuthMechanism.newInstance(acct, null);

        // Assert
        assertEquals(AuthMech.ldap, mech.getMechanism());
        assertFalse("ldap mechanism is not zimbra auth", mech.isZimbraAuth());
    }

    @Test
    public void newInstanceDomainAuthMechInvalidFallsBackToZimbraAuth() throws Exception {
        // Arrange — domain advertises an unparseable auth mech
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraAuthMech, "totallyBogus");
        prov.createDomain("baddom.com", dattrs);
        Account acct = createAccount("u@baddom.com");

        // Act — invalid mech is caught and falls back
        AuthMechanism mech = AuthMechanism.newInstance(acct, null);

        // Assert
        assertEquals(AuthMech.zimbra, mech.getMechanism());
        assertTrue(mech.isZimbraAuth());
    }

    @Test
    public void newInstanceCustomMechBuildsCustomAuthWithThatMechValue() throws Exception {
        // Arrange — domain advertises a custom handler
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraAuthMech, "custom:sample");
        prov.createDomain("customdom.com", dattrs);
        Account acct = createAccount("u@customdom.com");

        // Act
        AuthMechanism mech = AuthMechanism.newInstance(acct, null);

        // Assert — custom auth keeps the custom mech and is not zimbra auth
        assertEquals(AuthMech.custom, mech.getMechanism());
        assertFalse(mech.isZimbraAuth());
    }

    // ---------- ZimbraAuth predicates ----------

    @Test
    public void zimbraAuthIsEncodedPasswordRecognizesSshaAndSsha512() throws Exception {
        // Arrange
        AuthMechanism.ZimbraAuth za = new AuthMechanism.ZimbraAuth(AuthMech.zimbra);
        String ssha = PasswordUtil.SSHA.generateSSHA("pw", null);
        String ssha512 = PasswordUtil.SSHA512.generateSSHA512("pw", null);

        // Act / Assert
        assertTrue("must recognize an {SSHA} blob", za.isEncodedPassword(ssha));
        assertTrue("must recognize an {SSHA512} blob", za.isEncodedPassword(ssha512));
        assertFalse("a plain string is not an encoded password", za.isEncodedPassword("plainpw"));
    }

    @Test
    public void zimbraAuthIsValidEncodedPasswordMatchesOnlyCorrectPassword() throws Exception {
        // Arrange
        AuthMechanism.ZimbraAuth za = new AuthMechanism.ZimbraAuth(AuthMech.zimbra);
        String encoded = PasswordUtil.SSHA512.generateSSHA512("rightPw", null);

        // Act / Assert
        assertTrue("the correct password must validate", za.isValidEncodedPassword(encoded, "rightPw"));
        assertFalse("a wrong password must not validate", za.isValidEncodedPassword(encoded, "wrongPw"));
    }

    @Test
    public void zimbraAuthCheckPasswordAgingReturnsTrue() throws Exception {
        // Arrange
        AuthMechanism.ZimbraAuth za = new AuthMechanism.ZimbraAuth(AuthMech.zimbra);

        // Act / Assert — Zimbra auth participates in password aging
        assertTrue(za.checkPasswordAging());
    }

    // ---------- newInstance: as-admin auth mech resolution ----------

    @Test
    public void newInstanceAsAdminWithAuthMechAdminUsesAdminMech() throws Exception {
        // Arrange — domain has a distinct admin auth mech
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraAuthMech, AuthMech.zimbra.name());
        dattrs.put(Provisioning.A_zimbraAuthMechAdmin, AuthMech.ldap.name());
        prov.createDomain("adminmech.com", dattrs);
        Account acct = createAccount("u@adminmech.com");
        Map<String, Object> ctxt = new HashMap<String, Object>();
        ctxt.put(AuthContext.AC_AS_ADMIN, Boolean.TRUE);

        // Act
        AuthMechanism mech = AuthMechanism.newInstance(acct, ctxt);

        // Assert — the admin-specific mech (ldap) wins for an as-admin context
        assertEquals(AuthMech.ldap, mech.getMechanism());
        assertFalse(mech.isZimbraAuth());
    }

    @Test
    public void newInstanceAsAdminWithoutAuthMechAdminFallsBackToAuthMech() throws Exception {
        // Arrange — only the general auth mech is set, no admin-specific one
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        prov.createDomain("adminfallback.com", dattrs);
        Account acct = createAccount("u@adminfallback.com");
        Map<String, Object> ctxt = new HashMap<String, Object>();
        ctxt.put(AuthContext.AC_AS_ADMIN, Boolean.TRUE);

        // Act
        AuthMechanism mech = AuthMechanism.newInstance(acct, ctxt);

        // Assert — falls back to the general zimbraAuthMech (ldap)
        assertEquals(AuthMech.ldap, mech.getMechanism());
    }

    // ---------- doTwoFactorAuth ----------

    @Test
    public void doTwoFactorAuthTwoFactorNotRequiredReturnsFalse() throws Exception {
        // Arrange — default factory reports 2FA unavailable/not required
        Account acct = createAccount("tfa@example.com");
        Map<String, Object> ctxt = new HashMap<String, Object>();
        ctxt.put("proto", AuthContext.Protocol.soap);

        // Act
        boolean done = AuthMechanism.doTwoFactorAuth(acct, "pw", ctxt);

        // Assert — no app-specific-password auth attempted
        assertFalse("2FA must not be marked done when not required", done);
    }

    // ---------- doZimbraAuth / ZimbraAuth.doAuth ----------

    @Test
    public void doZimbraAuthValidEncodedPasswordSucceeds() throws Exception {
        // Arrange — account holds a matching SSHA512 password blob
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_userPassword, PasswordUtil.SSHA512.generateSSHA512("rightPw", null));
        Account acct = prov.createAccount("zauth-ok@example.com", "x", attrs);

        // Act / Assert — correct password returns without throwing
        AuthMechanism.doZimbraAuth(null, null, acct, "rightPw", new HashMap<String, Object>());
    }

    @Test
    public void doZimbraAuthMissingUserPasswordThrowsAuthFailed() throws Exception {
        // Arrange — account with no userPassword attribute at all (mock does not store one)
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("zauth-nopw@example.com", "x", attrs);
        assertNull("precondition: no userPassword present", acct.getAttr(Provisioning.A_userPassword));

        // Act / Assert
        try {
            AuthMechanism.doZimbraAuth(null, null, acct, "anything", new HashMap<String, Object>());
            fail("expected AUTH_FAILED for a missing userPassword");
        } catch (AuthFailedServiceException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void doZimbraAuthWrongEncodedPasswordThrowsAuthFailed() throws Exception {
        // Arrange — encoded password present but the supplied password is wrong
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_userPassword, PasswordUtil.SSHA512.generateSSHA512("rightPw", null));
        Account acct = prov.createAccount("zauth-wrong@example.com", "x", attrs);

        // Act / Assert
        try {
            AuthMechanism.doZimbraAuth(null, null, acct, "wrongPw", new HashMap<String, Object>());
            fail("expected AUTH_FAILED for a wrong password");
        } catch (AuthFailedServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("auth"));
        }
    }

    @Test
    public void doZimbraAuthPlaintextStoredPasswordNonLdapEntryThrowsAuthFailed() throws Exception {
        // Arrange — userPassword is not an SSHA blob and account is not an LdapEntry
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_userPassword, "notAnEncodedBlob");
        Account acct = prov.createAccount("zauth-plain@example.com", "x", attrs);

        // Act / Assert — falls through to the final AUTH_FAILED
        try {
            AuthMechanism.doZimbraAuth(null, null, acct, "notAnEncodedBlob", new HashMap<String, Object>());
            fail("expected AUTH_FAILED for a non-encoded, non-LDAP password");
        } catch (AuthFailedServiceException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void zimbraAuthIsZimbraAuthReturnsTrue() throws Exception {
        // Arrange
        AuthMechanism.ZimbraAuth za = new AuthMechanism.ZimbraAuth(AuthMech.zimbra);

        // Act / Assert
        assertTrue(za.isZimbraAuth());
    }

    // ---------- CustomAuth ----------

    @Test
    public void customAuthConstructorWithArgsParsesHandlerNameAndArgs() throws Exception {
        // Arrange — register a recording handler under the parsed name
        final List<String> seenArgs = new ArrayList<String>();
        final boolean[] called = new boolean[] {false };
        ZimbraCustomAuth.register("recHandler", new ZimbraCustomAuth() {
            @Override
            public void authenticate(Account acct, String password, Map<String, Object> context,
                    java.util.List<String> args) {
                called[0] = true;
                if (args != null) {
                    seenArgs.addAll(args);
                }
            }

            @Override
            public boolean checkPasswordAging() {
                return true;
            }
        });
        AuthMechanism.CustomAuth ca = new AuthMechanism.CustomAuth(
                AuthMech.custom, "custom:recHandler argOne \" two three \"");
        Account acct = createAccount("custom-ok@example.com");

        // Act — successful auth runs the handler with parsed args
        ca.doAuth(null, null, acct, "pw", new HashMap<String, Object>());

        // Assert
        assertTrue("handler must have been invoked", called[0]);
        assertTrue("first arg parsed", seenArgs.contains("argOne"));
        assertTrue("quoted whitespace preserved", seenArgs.contains(" two three "));
        assertTrue("checkPasswordAging delegates to handler", ca.checkPasswordAging());
    }

    @Test
    public void customAuthUnknownHandlerDoAuthThrowsAuthFailed() throws Exception {
        // Arrange — handler name that was never registered
        AuthMechanism.CustomAuth ca = new AuthMechanism.CustomAuth(
                AuthMech.custom, "custom:noSuchHandler");
        Account acct = createAccount("custom-missing@example.com");
        Domain domain = prov.getDomain(acct);

        // Act / Assert
        try {
            ca.doAuth(null, domain, acct, "pw", new HashMap<String, Object>());
            fail("expected AUTH_FAILED for an unregistered custom handler");
        } catch (AuthFailedServiceException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void customAuthUnknownHandlerCheckPasswordAgingThrowsFailure() throws Exception {
        // Arrange
        AuthMechanism.CustomAuth ca = new AuthMechanism.CustomAuth(
                AuthMech.custom, "custom:stillMissing");

        // Act / Assert
        try {
            ca.checkPasswordAging();
            fail("expected FAILURE when the custom handler is not found");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void customAuthHandlerThrowsWrapsAsAuthFailedWithMessage() throws Exception {
        // Arrange — a handler that rejects with a non-ServiceException reason
        ZimbraCustomAuth.register("throwHandler", new ZimbraCustomAuth() {
            @Override
            public void authenticate(Account acct, String password, Map<String, Object> context,
                    java.util.List<String> args) throws Exception {
                throw new IllegalStateException("nope");
            }
        });
        AuthMechanism.CustomAuth ca = new AuthMechanism.CustomAuth(
                AuthMech.custom, "custom:throwHandler");
        Account acct = createAccount("custom-throws@example.com");

        // Act / Assert — handler exception is surfaced and its reason included
        try {
            ca.doAuth(null, null, acct, "pw", new HashMap<String, Object>());
            fail("expected AUTH_FAILED when the handler throws");
        } catch (AuthFailedServiceException e) {
            assertTrue("reason must include the handler message", e.getMessage().contains("nope"));
        }
    }

}
