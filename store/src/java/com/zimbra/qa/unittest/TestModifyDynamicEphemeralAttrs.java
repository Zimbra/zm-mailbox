package com.zimbra.qa.unittest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class TestModifyDynamicEphemeralAttrs {

    private static final String acctName = TestModifyDynamicEphemeralAttrs.class.getSimpleName();
    private static Account acct;
    private static Provisioning prov = Provisioning.getInstance();

    @BeforeClass
    public static void init() throws Exception {
        TestUtil.deleteAccount(acctName);
        acct = TestUtil.createAccount(acctName);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        TestUtil.deleteAccount(acct.getName());
    }

    @Test
    public void testModifyDynamicAttrsViaModifyAttrs() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        // test that if the provided value is invalid, it won't be added
        attrs.put(Provisioning.A_zimbraAuthTokens, "123|badtoken");
        prov.modifyAttrs(acct, attrs);
        assertFalse(acct.hasAuthTokens("123"));

        // test that we can inject an LDAP-formatted auth token value
        Long millis = System.currentTimeMillis() + 1000 * 60 * 60;
        attrs.put(Provisioning.A_zimbraAuthTokens, String.format("123|%d|test", millis));
        prov.modifyAttrs(acct, attrs);
        assertTrue(acct.hasAuthTokens("123"));

        // test that we can add a second value using the '+' prefix
        attrs.clear();
        attrs.put("+" + Provisioning.A_zimbraAuthTokens, String.format("456|%d|test", millis));
        prov.modifyAttrs(acct, attrs);
        assertTrue(acct.hasAuthTokens("123"));
        assertTrue(acct.hasAuthTokens("456"));

        // test that we can delete a single value
        attrs.clear();
        attrs.put("-" + Provisioning.A_zimbraAuthTokens, String.format("123|%d|test", millis));
        prov.modifyAttrs(acct, attrs);
        assertFalse(acct.hasAuthTokens("123"));
        assertTrue(acct.hasAuthTokens("456"));
    }
}
