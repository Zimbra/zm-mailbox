package com.zimbra.qa.unittest;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class TestLdapProvGlobalConfig {

    private static Provisioning prov;
    
    @BeforeClass
    public static void init() {
        prov = Provisioning.getInstance();
    }
    
    @Test
    public void getGlobalConfig() throws Exception {
        Config config = prov.getConfig();
        assertNotNull(config != null);
    }

}
