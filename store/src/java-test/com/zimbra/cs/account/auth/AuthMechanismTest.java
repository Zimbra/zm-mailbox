package com.zimbra.cs.account.auth;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.ZimbraTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AuthMechanism} — focuses on the pure-logic methods
 * that do not require LDAP/DB, plus Mockito-based tests for the factory.
 *
 * Pure tests (no mocks): namePassedIn()
 * MockProvisioning tests: newInstance() with zimbraAuthMech variations
 * Mockito tests: newInstance() when Provisioning is stubbed
 */
public class AuthMechanismTest {

    @Before
    public void setUp() {
        ZimbraTestUtil.installMockProvisioning();
    }

    @After
    public void tearDown() {
        ZimbraTestUtil.restoreProvisioning();
    }

    // =========================================================================
    // namePassedIn — pure static utility, no dependencies
    // =========================================================================

    @Test
    public void namePassedIn_presentInContext_returnsValue() {
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, "user@example.com");
        assertEquals("user@example.com", AuthMechanism.namePassedIn(ctx));
    }

    @Test
    public void namePassedIn_missingFromContext_returnsEmptyString() {
        Map<String, Object> ctx = new HashMap<String, Object>();
        assertEquals("", AuthMechanism.namePassedIn(ctx));
    }

    @Test
    public void namePassedIn_nullContext_returnsEmptyString() {
        assertEquals("", AuthMechanism.namePassedIn(null));
    }

    @Test
    public void namePassedIn_nullValue_returnsEmptyString() {
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, null);
        assertEquals("", AuthMechanism.namePassedIn(ctx));
    }

    // =========================================================================
    // AuthMech enum — fromString
    // =========================================================================

    @Test
    public void authMech_fromString_zimbraReturnsZimbra() throws Exception {
        assertEquals(AuthMechanism.AuthMech.zimbra,
                AuthMechanism.AuthMech.fromString("zimbra"));
    }

    @Test
    public void authMech_fromString_ldapReturnsLdap() throws Exception {
        assertEquals(AuthMechanism.AuthMech.ldap,
                AuthMechanism.AuthMech.fromString("ldap"));
    }

    @Test
    public void authMech_fromString_adReturnsAd() throws Exception {
        assertEquals(AuthMechanism.AuthMech.ad,
                AuthMechanism.AuthMech.fromString("ad"));
    }

    @Test
    public void authMech_fromString_kerberos5ReturnsKerberos5() throws Exception {
        assertEquals(AuthMechanism.AuthMech.kerberos5,
                AuthMechanism.AuthMech.fromString("kerberos5"));
    }

    @Test
    public void authMech_fromString_customReturnsCustom() throws Exception {
        assertEquals(AuthMechanism.AuthMech.custom,
                AuthMechanism.AuthMech.fromString("custom:myhandler"));
    }

    @Test(expected = com.zimbra.common.service.ServiceException.class)
    public void authMech_fromString_unknownThrowsServiceException() throws Exception {
        AuthMechanism.AuthMech.fromString("unknown_mech");
    }

    // =========================================================================
    // newInstance — uses Mockito stubs
    // =========================================================================

    @Test
    public void newInstance_zimbraAuthMech_returnsZimbraAuthInstance() throws Exception {
        Provisioning mockProv = ZimbraTestUtil.installMockitoProvisioning();

        Account acct = Mockito.mock(Account.class);
        Domain  domain = Mockito.mock(Domain.class);

        Mockito.when(mockProv.getDomain(acct)).thenReturn(domain);
        Mockito.when(domain.getAttr(Provisioning.A_zimbraAuthMech)).thenReturn("zimbra");

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(AuthContext.AC_AS_ADMIN, Boolean.FALSE);

        AuthMechanism mech = AuthMechanism.newInstance(acct, ctx);

        assertNotNull(mech);
        assertTrue("expected ZimbraAuth (isZimbraAuth)", mech.isZimbraAuth());
        assertEquals(AuthMechanism.AuthMech.zimbra, mech.getMechanism());
    }

    @Test
    public void newInstance_ldapAuthMech_returnsLdapAuthInstance() throws Exception {
        Provisioning mockProv = ZimbraTestUtil.installMockitoProvisioning();

        Account acct   = Mockito.mock(Account.class);
        Domain  domain = Mockito.mock(Domain.class);

        Mockito.when(mockProv.getDomain(acct)).thenReturn(domain);
        Mockito.when(domain.getAttr(Provisioning.A_zimbraAuthMech)).thenReturn("ldap");

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(AuthContext.AC_AS_ADMIN, Boolean.FALSE);

        AuthMechanism mech = AuthMechanism.newInstance(acct, ctx);

        assertNotNull(mech);
        assertEquals(AuthMechanism.AuthMech.ldap, mech.getMechanism());
    }

    @Test
    public void newInstance_nullDomain_fallsBackToZimbraAuth() throws Exception {
        Provisioning mockProv = ZimbraTestUtil.installMockitoProvisioning();

        Account acct = Mockito.mock(Account.class);
        Mockito.when(mockProv.getDomain(acct)).thenReturn(null);

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(AuthContext.AC_AS_ADMIN, Boolean.FALSE);

        AuthMechanism mech = AuthMechanism.newInstance(acct, ctx);

        assertNotNull(mech);
        // With null domain, zimbraAuthMech lookup returns null → defaults to zimbra
        assertTrue(mech.isZimbraAuth());
    }

    @Test
    public void newInstance_adminContext_usesAdminAuthMech() throws Exception {
        Provisioning mockProv = ZimbraTestUtil.installMockitoProvisioning();

        Account acct   = Mockito.mock(Account.class);
        Domain  domain = Mockito.mock(Domain.class);

        Mockito.when(mockProv.getDomain(acct)).thenReturn(domain);
        // zimbraAuthMechAdmin not set — falls back to zimbraAuthMech
        Mockito.when(domain.getAttr(Provisioning.A_zimbraAuthMechAdmin)).thenReturn(null);
        Mockito.when(domain.getAttr(Provisioning.A_zimbraAuthMech)).thenReturn("zimbra");

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(AuthContext.AC_AS_ADMIN, Boolean.TRUE);

        AuthMechanism mech = AuthMechanism.newInstance(acct, ctx);

        assertNotNull(mech);
        assertTrue(mech.isZimbraAuth());
    }

    // =========================================================================
    // getMechanism / isZimbraAuth on base class
    // =========================================================================

    @Test
    public void newInstance_getMechanism_matchesRequestedMech() throws Exception {
        Provisioning mockProv = ZimbraTestUtil.installMockitoProvisioning();

        Account acct   = Mockito.mock(Account.class);
        Domain  domain = Mockito.mock(Domain.class);

        Mockito.when(mockProv.getDomain(acct)).thenReturn(domain);
        Mockito.when(domain.getAttr(Provisioning.A_zimbraAuthMech)).thenReturn("kerberos5");

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(AuthContext.AC_AS_ADMIN, Boolean.FALSE);

        AuthMechanism mech = AuthMechanism.newInstance(acct, ctx);
        assertEquals(AuthMechanism.AuthMech.kerberos5, mech.getMechanism());
    }
}
