package com.zimbra.qa.unittest;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.ldap.LdapUtilCommon;

public class TestLdapUtil {

    @Test
    public void testAuthDN() {
        
        assertEquals("schemers@example.zimbra.com", 
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", null));
        
        assertEquals("schemers@example.zimbra.com",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", ""));
        
        assertEquals("WTF",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", "WTF"));
        
        assertEquals("schemers@example.zimbra.com",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", "%n"));
        
        assertEquals("schemers",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", "%u"));
        
        assertEquals("example.zimbra.com",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", "%d"));
        
        assertEquals("dc=example,dc=zimbra,dc=com",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", "%D"));
        
        assertEquals("uid=schemers,ou=people,dc=example,dc=zimbra,dc=com",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", "uid=%u,ou=people,%D"));
        
        assertEquals("n(schemers@example.zimbra.com)u(schemers)d(example.zimbra.com)D(dc=example,dc=zimbra,dc=com)(%)",
                LdapUtilCommon.computeAuthDn("schemers@example.zimbra.com", "n(%n)u(%u)d(%d)D(%D)(%%)"));
    }
}
