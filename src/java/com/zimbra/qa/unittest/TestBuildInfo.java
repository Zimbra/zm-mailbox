package com.zimbra.qa.unittest;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.util.BuildInfo.Version;


public class TestBuildInfo extends TestCase {

    public void testVersion() throws Exception {
        Version v1 = new Version("5.0.10");
        Version v2 = new Version("5.0.9");
        Version v3 = new Version("5.0.10");
        Version v4 = new Version("5.0");
        Version future = new Version(Version.FUTURE);
        
        assertTrue(v1.compare(v2) > 0);
        assertTrue(v1.compare(v3) == 0);
        assertTrue(v2.compare(v1) < 0);
        assertTrue(v1.compare(v4) > 0);
        
        assertTrue(v1.compare(future) < 0);
        assertTrue(future.compare(v1) > 0);
        assertTrue(future.compare(future) == 0);
        
        assertTrue(Version.compare("5.0.10", "5.0.9") > 0);
        assertTrue(Version.compare("5.0.9", "5.0.10") < 0);
        assertTrue(Version.compare("5.0.10", "5.0.10") == 0);
    }
    
    public void testInvalid() throws Exception {
        try {
            new Version("a.0.0");
        } catch (NumberFormatException e) {
            return; // this is good
        }
        fail(); //this is bad
    }
    
    public void testInVersion() throws Exception {
        AttributeManager am = AttributeManager.getInstance();
        
        assertTrue(am.inVersion("zimbraId", "0")); 
        assertTrue(am.inVersion("zimbraId", "5.0.10"));
        
        assertFalse(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.0.9")); 
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.0.10")); 
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.0.11")); 
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "5.5")); 
        assertTrue(am.inVersion("zimbraZimletDomainAvailableZimlets", "6")); 
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception  {
        TestUtil.runTest(TestBuildInfo.class);
    }

}
