package com.zimbra.qa.unittest;

import static org.junit.Assert.assertNotNull;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;

public class TestLdapProvGlobalGrant {

private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        TestLdap.manualInit();
        
        prov = Provisioning.getInstance();
    }
    
    @Test
    public void getGlobalGrant() throws Exception {
        GlobalGrant globalGrant = prov.getGlobalGrant();
        assertNotNull(globalGrant);
    }
}
