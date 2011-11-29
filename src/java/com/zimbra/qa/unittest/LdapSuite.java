/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import org.junit.runner.JUnitCore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.zimbra.common.util.CliUtil;
import com.zimbra.qa.unittest.ldap.TestLdapConnectivity;


public class LdapSuite {
    
    public static class ConsoleListener extends RunListener {
        
        @Override
        public void testRunStarted(Description description) throws Exception {
            System.out.println();
            System.out.println("Started:" + description.getDisplayName());
        }
        
        @Override
        public void testStarted(Description description)  throws Exception {
            System.out.println("    " + description.getDisplayName());
        }
        
        @Override
        public void testFailure(Failure failure)  throws Exception {
            System.out.println("Test: " + failure.getTestHeader());
            // System.out.println(failure.getDescription());
            System.out.println("Message: " + failure.getMessage());
            System.out.println(failure.getTrace());
        }
    }
    
    
    private static void runTests(JUnitCore junit) {
 
    }
    
    private static void addManualTests(JUnitCore junit) {
        
        /*
         * cp -f /Users/pshao/p4/main/ZimbraServer/data/unittest/*.xml /opt/zimbra/conf/rights
         * and
         * uncomment sCoreRightDefFiles.add("rights-unittest.xml"); in RightManager
         */  
        junit.run(TestACAll.class);
        
        /*
         * To setup Velodrome DIT:
         * /Users/pshao/p4/main/Velodrome/ZimbraServer/zimbra-internal/setup-velodrome-dit.pl all  /Users/pshao/p4/main /Users/pshao/temp/dist phoebe.mbp dev
         */
        junit.run(TestProvisioning.class);
        
        /*
         * zmsoap -v -z RunUnitTestsRequest/test=com.zimbra.qa.unittest.TestGalGroupMembers
         */
        // TestGalGroupMembers (has to be run in the server)
        
        // connection test, ahs to be run manually
        junit.run(TestLdapConnectivity.class);
        
        /*
         * merge in tests in TestLdap
         */
        
    }
    
    /*
     * Tests to fix/automate:
     * 
     * TestAccess
     * TestAccessKeyGrant
     * TestACL*
     * 
     * TestGal
     * TestGalGroupMembers
     * TestPreAuthServlet
     * TestSearchDirectory
     */
    
    public static void main(String... args) throws Exception {
        // CliUtil.toolSetup();
        
        JUnitCore junit = new JUnitCore();
        junit.addListener(new ConsoleListener());
        
        runTests(junit);
        
        System.out.println();
        System.out.println("=== Finished ===");
    }
}
