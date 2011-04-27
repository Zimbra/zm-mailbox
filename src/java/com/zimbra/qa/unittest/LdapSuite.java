package com.zimbra.qa.unittest;

import org.junit.runner.JUnitCore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.zimbra.common.util.CliUtil;


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
        junit.run(TestAccountLockout.class);
        junit.run(TestACPermissionCache.class);
        junit.run(TestACUserRights.class);
        junit.run(TestAlias.class);
        junit.run(TestAttr.class);
        junit.run(TestBuildInfo.class);
        junit.run(TestCos.class);
        junit.run(TestIDN.class);
        junit.run(TestLdapBinary.class);
        junit.run(TestLdapUtil.class);
        junit.run(TestProvCallback.class);
        junit.run(TestProvGroup.class);
        junit.run(TestProvValidator.class);
        junit.run(TestSearchCalendarResources.class);
        junit.run(TestSearchGal.class);
    }
    
    private static void addManualTests(JUnitCore junit) {
        
        /*
         * cp -f /Users/pshao/p4/main/ZimbraServer/data/unittest/*.xml /opt/zimbra/conf/rights
         * and
         * uncomment sCoreRightDefFiles.add("rights-unittest.xml"); in RightManager
         */  
        junit.run(TestACBasic.class);
        
        /*
         * To setup Velodrome DIT:
         * /Users/pshao/p4/main/Velodrome/ZimbraServer/zimbra-internal/setup-velodrome-dit.pl all  /Users/pshao/p4/main /Users/pshao/temp/dist phoebe.mbp dev
         */
        junit.run(TestProvisioning.class);
        
        /*
         * zmsoap -v -z RunUnitTestsRequest/test=com.zimbra.qa.unittest.TestGalGroupMembers
         */
        // TestGalGroupMembers (has to be run in the server)
        
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
     * TestDomainStatus
     * TestGal
     * TestGalGroupMembers
     * TestPreAuthServlet
     * TestSearchDirectory
     * TestShareInfo
     * TestZimbraId
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
