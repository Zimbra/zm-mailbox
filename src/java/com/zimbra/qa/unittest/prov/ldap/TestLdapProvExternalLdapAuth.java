package com.zimbra.qa.unittest.prov.ldap;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;
import com.zimbra.qa.unittest.prov.Names;

public class TestLdapProvExternalLdapAuth extends LdapTest {

    private static LdapProvTestUtil provUtil;
    private static LdapProv prov;
    private static Domain domain;
    private static LdapConnType testConnType = LdapConnType.PLAIN;  // LDAPS  STARTTLS
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) throws Exception {
        return provUtil.createAccount(localPart, domain, attrs);
    }
    
    private String getAccountDN(Account acct) throws Exception {
        if (acct instanceof com.zimbra.cs.account.ldap.legacy.entry.LdapAccount) {
            return ((com.zimbra.cs.account.ldap.legacy.entry.LdapAccount) acct).getDN();
        } else if (acct instanceof com.zimbra.cs.account.ldap.entry.LdapAccount) {
            return ((com.zimbra.cs.account.ldap.entry.LdapAccount) acct).getDN();
        }
        
        fail();
        return null;  // make the compiler happy
    }
    
    private String getLdapURL() {
        if (LdapConnType.LDAPS == testConnType) {
            return "ldaps://" + LC.zimbra_server_hostname.value() + ":636";
        } else {
            return "ldap://" + LC.zimbra_server_hostname.value() + ":389";
        }
    }
    
    private String getWantStartTLS() {
        if (LdapConnType.STARTTLS == testConnType) {
            return ProvisioningConstants.TRUE;
        } else { 
            return null;
        }
    }
    
    @Test
    public void checkAuthConfigBySearch() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart("checkAuthConfigBySearch");
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        String ACCT_DN = getAccountDN(acct);
        String PASSWORD = "test123";
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        attrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        
        Provisioning.Result result;
        String expectedComputedSearchFilter;
        
        // %n = username with @ (or without, if no @ was specified)
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
        result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
        assertEquals(Check.STATUS_OK, result.getCode());
        expectedComputedSearchFilter = "(zimbraMailDeliveryAddress=" + acct.getName() + ")";
        assertEquals(expectedComputedSearchFilter, result.getComputedDn());
        
        // %u = username with @ removed
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(uid=%u)");
        result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
        assertEquals(Check.STATUS_OK, result.getCode());
        expectedComputedSearchFilter = "(uid=" + ACCT_NAME_LOCALPART + ")";
        assertEquals(expectedComputedSearchFilter.toLowerCase(), result.getComputedDn().toLowerCase());
        
        // %d = domain as foo.com
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(mail=%u@%d)");
        result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
        assertEquals(Check.STATUS_OK, result.getCode());
        expectedComputedSearchFilter = "(mail=" + acct.getName() + ")";
        assertEquals(expectedComputedSearchFilter, result.getComputedDn());
        
        // %D = domain as dc=foo,dc=com
        /* Nope: this is not valid, cannot search by DN
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(dn=%u,ou=people,%D)");
        result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
        assertEquals(Check.STATUS_OK, result.getCode());
        expectedComputedSearchFilter = "(dn=" + ACCT_DN + ")";
        assertEquals(expectedComputedSearchFilter, result.getComputedDn());
        */
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void checkAuthConfigByBindDNTemplate() throws Exception {
        // TODO: doesn't work with special chars, even in the legacy implementation.
        // String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("checkAuthConfigByBindDNTemplate");
        String ACCT_NAME_LOCALPART = "checkAuthConfigByBindDNTemplate";
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        String ACCT_DN = getAccountDN(acct);
        String PASSWORD = "test123";
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        attrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        
        Provisioning.Result result;
        
        // %D = domain as dc=foo,dc=com
        attrs.put(Provisioning.A_zimbraAuthLdapBindDn, "uid=%u,ou=people,%D");
        result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
        assertEquals(Check.STATUS_OK, result.getCode());
        // expectedComputedSearchFilter = "(zimbraMailDeliveryAddress=" + acct.getName() + ")";
        assertEquals(ACCT_DN, result.getComputedDn());
        
        provUtil.deleteAccount(acct);
    }

    @Test
    public void checkAuthConfigFailures() throws Exception {
        String ACCT_NAME_LOCALPART = "checkAuthConfigFailures";
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        String ACCT_DN = getAccountDN(acct);
        String PASSWORD = "test123";
        
        Provisioning.Result result;
        
        Map<String, Object> attrs = Maps.newHashMap();
        
        try {
            SKIP_IF_IN_MEM_LDAP_SERVER("external auth STATUS_UNKNOWN_HOST status is not supported by InMemoryDirectoryServer");
            
            attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
            attrs.put(Provisioning.A_zimbraAuthLdapURL, "ldap://" + "bogus" + ":389");
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
            result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
            assertEquals(Check.STATUS_UNKNOWN_HOST, result.getCode());
        } catch (SkippedForInMemLdapServer e) {
        }
        
        try {
            SKIP_IF_IN_MEM_LDAP_SERVER("external auth STATUS_CONNECTION_REFUSED status is not supported by InMemoryDirectoryServer");
            
            attrs.clear();
            attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
            attrs.put(Provisioning.A_zimbraAuthLdapURL, "ldap://" + LC.zimbra_server_hostname.value() + ":38900");
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
            result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
            assertEquals(Check.STATUS_CONNECTION_REFUSED, result.getCode());
        } catch (SkippedForInMemLdapServer e) {
        }
        
        try {
            SKIP_IF_IN_MEM_LDAP_SERVER("external auth STATUS_COMMUNICATION_FAILURE status is not supported by InMemoryDirectoryServer");
            
            attrs.clear();
            attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
            attrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
            attrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, LdapConstants.LDAP_TRUE);
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
            result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
            // assertEquals(Check.STATUS_SSL_HANDSHAKE_FAILURE, result.getCode());  // if TLS is enabled in sladp.conf
            assertEquals(Check.STATUS_COMMUNICATION_FAILURE, result.getCode());     // if TLS is not enabled in sladp.conf
        } catch (SkippedForInMemLdapServer e) {
        }
        
        try {
            SKIP_IF_IN_MEM_LDAP_SERVER("external auth STATUS_AUTH_FAILED status is not supported by InMemoryDirectoryServer");
            attrs.clear();
            attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
            attrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, "bogus");
            attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
            attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
            result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
            assertEquals(Check.STATUS_AUTH_FAILED, result.getCode());
        } catch (SkippedForInMemLdapServer e) {
        }
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        attrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
        result = prov.checkAuthConfig(attrs, acct.getName(), "bogus");
        assertEquals(Check.STATUS_AUTH_FAILED, result.getCode());
        
        // TODO, how to test this?
        // STATUS_AUTH_NOT_SUPPORTED
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        attrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBase, "dc=bogus");
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");
        result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
        assertEquals(Check.STATUS_NAME_NOT_FOUND, result.getCode());
        
        attrs.clear();
        attrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        attrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        attrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n"); // missing the closing paren
        result = prov.checkAuthConfig(attrs, acct.getName(), PASSWORD);
        assertEquals(Check.STATUS_INVALID_SEARCH_FILTER, result.getCode());
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void externalLdapAuthByDNOnAccount() throws Exception {
        LdapProv ldapProv = (LdapProv) prov;
        
        String DOMAIN_NAME = Names.makeDomainName(
                "externalLdapAuthByDNOnAccount.".toLowerCase() + baseDomainName());
        
        String authMech = AuthMech.ldap.name();
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        domainAttrs.put(Provisioning.A_zimbraAuthMech, authMech);
        domainAttrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, getWantStartTLS());
        
        Domain domain = provUtil.createDomain(DOMAIN_NAME, domainAttrs);
        
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart("externalLdapAuthByDNOnAccount");
        Account acct = provUtil.createAccount(ACCT_NAME_LOCALPART, domain);
        
        String ACCT_DN = getAccountDN(acct);
        
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraAuthLdapExternalDn, ACCT_DN);
        ldapProv.modifyAttrs(acct, acctAttrs);
        
        prov.authAccount(acct, "test123", AuthContext.Protocol.test);
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void externalLdapAuthBySearch() throws Exception {
        LdapProv ldapProv = (LdapProv) prov;
        
        String DOMAIN_NAME = Names.makeDomainName(
                "externalLdapAuthBySearch.".toLowerCase() + baseDomainName());
        
        String authMech = AuthMech.ldap.name();
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        domainAttrs.put(Provisioning.A_zimbraAuthMech, authMech);
        domainAttrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, getWantStartTLS());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapSearchFilter, "(zimbraMailDeliveryAddress=%n)");

        Domain domain = provUtil.createDomain(DOMAIN_NAME, domainAttrs);
        
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart("externalLdapAuthByDNOnAccount");
        Account acct = provUtil.createAccount(ACCT_NAME_LOCALPART, domain);
        
        prov.authAccount(acct, "test123", AuthContext.Protocol.test);
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void externalLdapAuthByBindDNtemplate() throws Exception {
        LdapProv ldapProv = (LdapProv) prov;
        
        String DOMAIN_NAME = Names.makeDomainName(
                "externalLdapAuthByBindDNtemplate.".toLowerCase() + baseDomainName());
        
        String authMech = AuthMech.ldap.name();
        Map<String, Object> domainAttrs = new HashMap<String, Object>();
        domainAttrs.put(Provisioning.A_zimbraAuthMech, authMech);
        domainAttrs.put(Provisioning.A_zimbraAuthLdapURL, getLdapURL());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapStartTlsEnabled, getWantStartTLS());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapSearchBindPassword, LC.zimbra_ldap_password.value());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapSearchBindDn, LC.zimbra_ldap_userdn.value());
        domainAttrs.put(Provisioning.A_zimbraAuthLdapBindDn, "uid=%u,ou=people,%D");
        
        Domain domain = provUtil.createDomain(DOMAIN_NAME, domainAttrs);
        
        // TODO: doesn't work with special chars, even in the legacy implementation.
        // String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("checkAuthConfigByBindDNTemplate");
        // String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("externalLdapAuthByDNOnAccount");
        String ACCT_NAME_LOCALPART = "externalLdapAuthByDNOnAccount";
        Account acct = provUtil.createAccount(ACCT_NAME_LOCALPART, domain);
        
        prov.authAccount(acct, "test123", AuthContext.Protocol.test);
        
        provUtil.deleteAccount(acct);
    }
    
    @Test
    public void zimbraAuthNonSSHA() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart("zimbraAuthNonSSHA");
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        
        String PASSWORD = InMemoryLdapServer.Password.genNonSSHAPassword("not-ssha-blah");
        
        // modify userPassword via modifyAccount, not changePassword
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_userPassword, PASSWORD);
        prov.modifyAttrs(acct, acctAttrs);
        
        // good password
        prov.authAccount(acct, PASSWORD, AuthContext.Protocol.test);
        
        
        // bad password
        boolean caughtException = false;
        try {
            prov.authAccount(acct, PASSWORD + "not", AuthContext.Protocol.test);
        } catch (AccountServiceException e) {
            if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        provUtil.deleteAccount(acct);
    }
    
}
