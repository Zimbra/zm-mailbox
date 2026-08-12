package com.zimbra.cs.account.auth;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ZCS-19498: Tests for configurable fallback auth routing in AuthMechanism.newInstance().
 * Verifies that IDP_ROPC correctly routes to MFA for EAS and to fallback for other protocols,
 * while ensuring standard and non-ROPC custom mechanisms remain unaffected.
 */
public class AuthMechanismFallbackRoutingTest {

    private Provisioning prov;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initProvisioning();
        prov = Provisioning.getInstance();
    }

    private Account createAccountWithAuthMech(String domainName, String authMech) throws Exception {
        Map<String, Object> domainAttrs = new HashMap<>();
        domainAttrs.put(Provisioning.A_zimbraAuthMech, authMech);
        prov.createDomain(domainName, domainAttrs);
        Map<String, Object> acctAttrs = new HashMap<>();
        prov.createAccount("testuser@" + domainName, "password", acctAttrs);
        return prov.get(Key.AccountBy.name, "testuser@" + domainName);
    }

    private Map<String, Object> createEASContext() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put(AuthContext.AC_PROTOCOL, AuthContext.Protocol.zsync);
        ctx.put(AuthContext.AC_SUB_PROTOCOL, AuthContext.SubProtocol.eas);
        return ctx;
    }

    @Test
    public void testRopcRoutingLogic() throws Exception {
        // Test Case: fallback:ad custom:idp-ropc ...
        Account account = createAccountWithAuthMech("ropc-test.com", "fallback:ad custom:idp-ropc arg1");

        // 1. EAS Protocol -> Must route to CustomAuth (MFA)
        assertEquals("EAS must route to custom auth", AuthMech.custom,
                AuthMechanism.newInstance(account, createEASContext()).getMechanism());

        // 2. Non-EAS Protocol -> Must route to Fallback (AD)
        assertEquals("Non-EAS must route to fallback (AD)", AuthMech.ad,
                AuthMechanism.newInstance(account, new HashMap<>()).getMechanism());

        // 3. Null Context -> Must handle gracefully and route to Fallback
        assertEquals("Null context must route to fallback", AuthMech.ad,
                AuthMechanism.newInstance(account, null).getMechanism());
    }

    @Test
    public void testPassThroughMechanisms() throws Exception {
        // 1. Standard mechanism (zimbra) -> Should bypass custom logic
        Account zimbraAcct = createAccountWithAuthMech("std-zimbra.com", "zimbra");
        assertEquals("Standard zimbra must route directly", AuthMech.zimbra,
                AuthMechanism.newInstance(zimbraAcct, null).getMechanism());

        // 2. Non-ROPC custom mechanism -> Should bypass fallback logic
        Account customAcct = createAccountWithAuthMech("std-custom.com", "custom:saml handler1");
        assertEquals("Non-ROPC custom must route to CustomAuth", AuthMech.custom,
                AuthMechanism.newInstance(customAcct, new HashMap<>()).getMechanism());
    }

    @Test
    public void testInvalidAndMalformedConfigs() throws Exception {
        Map<String, Object> ctx = new HashMap<>();
        // 1. No fallback prefix for ROPC + Non-EAS -> silently returns default ZimbraAuth
        Account noFallback = createAccountWithAuthMech("err-nofb.com", "custom:idp-ropc arg1");
        assertEquals("No fallback configured must return default zimbra", AuthMech.zimbra,
                AuthMechanism.newInstance(noFallback, ctx).getMechanism());

        // 2. Invalid fallback type (custom) -> resolveFallbackMech throws AUTH_FAILED
        Account badFallback = createAccountWithAuthMech("err-badfb.com",
                "fallback:custom custom:idp-ropc");
        try {
            AuthMechanism.newInstance(badFallback, ctx);
            fail("Invalid fallback type 'custom' must throw");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("authentication failed"));
        }
        // 3. Malformed prefix (no space after fallback:ad) -> returns default ZimbraAuth
        Account malformed = createAccountWithAuthMech("err-malformed.com", "fallback:ad");
        assertEquals("Malformed config must return default zimbra", AuthMech.zimbra,
                AuthMechanism.newInstance(malformed, ctx).getMechanism());
    }
}
