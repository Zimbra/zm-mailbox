/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.util.BuildInfo.Version;


public class TestBuildInfo extends TestCase {

    public void invalid(String version) throws Exception {
        
        try {
            new Version(version);
        } catch (ServiceException e) {
            System.out.println("good:" + e.getMessage());
            return; // this is good
        }
        fail(); //this is bad
    }
    
    public void testInvalid() throws Exception {
        invalid("a.0.0");
        invalid("6.0.0_GB");
        invalid("5.0.12.1");
    }
    
    
    public void testValid() throws Exception {
        Version ver;
        
        ver = new Version("6");
        ver = new Version("6_M1");
        ver = new Version("6_BETA1");
        ver = new Version("6_RC1");
        ver = new Version("6_GA");
        ver = new Version("6.0");
        ver = new Version("6.0_M1");
        ver = new Version("6.0_BETA1");
        ver = new Version("6.0_RC1");
        ver = new Version("6.0_GA");
        ver = new Version("6.0.0");
        ver = new Version("6.0.0_M1");
        ver = new Version("6.0.0_BETA1");
        ver = new Version("6.0.0_RC1");
        ver = new Version("6.0.0_GA");
        ver = new Version("6.0.0_GA1");
    }
    
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
    
    public void testRealReleases() throws Exception {
        // all real releases we've made or will make
        String[] versions = new String[] {
            // "3.0.M1",   this format is not not supported, we should not have any customers upgrading from this version
            "3.0.0_M2",
            "3.0.0_M3",
            "3.0.0_M4",
            "3.0.0_GA",
            "3.0.1_GA",
            "3.1.0_GA",
            "3.1.1_GA",
            "3.1.2_GA",
            "3.1.3_GA",
            "3.1.4_GA",
            "3.2.0_M1",
            "3.2.0_M2",
            "4.0.0_RC1",
            "4.0.0_GA",
            "4.0.1_GA",
            "4.0.2_GA",
            "4.0.3_GA",
            "4.0.4_GA",
            "4.0.5_GA",
            "4.1.0_BETA1",
            "4.5.0_BETA1",
            "4.5.0_BETA2",
            "4.5.0_RC1",
            "4.5.0_RC2",
            "4.5.0_GA",
            "4.5.1_GA",
            "4.5.2_GA",
            "4.5.3_GA",
            "4.5.4_GA",
            "4.5.5_GA",
            "4.5.6_GA",
            "4.5.7_GA",
            "4.5.8_GA",
            "4.5.9_GA",
            "4.5.10_GA",
            "4.5.11_GA",
            "5.0.0_BETA1",
            "5.0.0_BETA2",
            "5.0.0_BETA3",
            "5.0.0_BETA4",
            "5.0.0_RC1",
            "5.0.0_RC2",
            "5.0.0_RC3",
            "5.0.0_GA",
            "5.0.1_GA",
            "5.0.2_GA",
            "5.0.3_GA",
            "5.0.4_GA",
            "5.0.5_GA",
            "5.0.6_GA",
            "5.0.7_GA",
            "5.0.8_GA",
            "5.0.9_GA",
            "5.0.10_GA",
            "5.0.11_GA",
            "5.0.12_GA",
            "6.0.0_BETA1",
            "6.0.0_BETA2",
            "6.0.0_RC1",
            "6.0.0_RC2",
            "6.0.0_GA"
        };
        
        for (int i=0; i<versions.length-1; i++) {
            // System.out.println();
            assertTrue(Version.compare(versions[i], versions[i]) == 0);
            for (int j=i+1; j<versions.length; j++) {
                // System.out.println(versions[j] + "    " + versions[i]);
                assertTrue(Version.compare(versions[j], versions[i]) > 0);
            }
        }
    }
    
    /*
     * test some odd cases
     */
    public void testVersionWithRelease() throws Exception {
        assertTrue(Version.compare("6.0.0_GA", "6.0.0") > 0);
        assertTrue(Version.compare("6.0.0", "6.0.0_GA") < 0);
        assertTrue(Version.compare("6.0.0_RC1", "6.0.0_RC") > 0);
        assertTrue(Version.compare("6.0.0_RC", "6.0.0_RC1") < 0);
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
