package com.zimbra.qa.unittest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.ldap.LdapProv;

public class TestACDiscoverRights extends TestLdap {
    private static LdapProv prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = LdapProv.getInst();
        domain = TestLdapProvDomain.createDomain(prov, baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return baseDomainName(TestACDiscoverRights.class);
    }
    
    @Test
    public void discoverRights() throws Exception {
        // Account acct1 = Test
    }
}
