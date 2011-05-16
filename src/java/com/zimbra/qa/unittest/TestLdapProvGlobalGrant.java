package com.zimbra.qa.unittest;

import static org.junit.Assert.assertNotNull;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;

public class TestLdapProvGlobalGrant extends TestLdap {

private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = Provisioning.getInstance();
    }
    
    @Test
    public void getGlobalGrant() throws Exception {
        GlobalGrant globalGrant = prov.getGlobalGrant();
        assertNotNull(globalGrant);
    }
}
