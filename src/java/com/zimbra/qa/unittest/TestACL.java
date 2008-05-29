package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.service.AuthProvider;

public class TestACL extends TestCase {
    // private static final AclAccessManager mAM = new AclAccessManager();
    private static final AccessManager mAM = AccessManager.getInstance();
    private static final String TEST_ID = TestProvisioningUtil.genTestId();
    private static final String DOMAIN_NAME = TestProvisioningUtil.baseDomainName("test-ACL", TEST_ID);
    private static final String PASSWORD = "test123";
    
    static {
        Provisioning prov = Provisioning.getInstance();
        try {
            // create a domain
            Domain domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    private static String getEmailAddr(String localPart) {
        return localPart + "@" + DOMAIN_NAME;
    }
    
    private Account guestAccount(String email, String password) {
        return new ACL.GuestAccount(email, password);
    }
    
    private Account anonAccount() {
        return ACL.ANONYMOUS_ACCT;
    }
    
    /*
     * verify we always get the expected result, regardless what the default value is
     * This test does NOT use the admin privileges 
     * 
     * This is for testing target entry with some ACL.
     */
    private void verify(Account grantee, Account target, Right right, boolean expected) throws Exception {
        verify(grantee, target, right, false, true, expected);
    }
    
    /*
     * verify we always get the expected result, regardless what the default value is
     * 
     * This is for testing target entry with some ACL.
     */
    private void verify(Account grantee, Account target, Right right, boolean asAdmin, boolean expected) throws Exception {
        // 1. pass true as the default value, result should not be affected by the default value
        verify(grantee, target, right, asAdmin, true, expected);
        
        // 2. pass false as the default value, result should not be affected by the default value
        verify(grantee, target, right, asAdmin, false, expected);
    }
    
    /*
     * verify that the result IS the default value
     * 
     * This is for testing target entry without any ACL.
     */
    private void verifyDefault(Account grantee, Account target, Right right) throws Exception {
        boolean asAdmin = false; // TODO: test admin case
        
        // 1. pass true as the default value, result should be true
        verify(grantee, target, right, asAdmin, true, true);
            
        // 2. pass false as the default value, result should be false
        verify(grantee, target, right, asAdmin, false, false);
    }
    
    /*
     * verify expected result
     */
    private void verify(Account grantee, Account target, Right right, boolean asAdmin, boolean defaultValue, boolean expected) throws Exception {
        boolean result;
        
        // Account interface
        result = mAM.canPerform(grantee==null?null:grantee, target, right, asAdmin, defaultValue);
        assertEquals(expected, result);
        
        // AuthToken interface
        result = mAM.canPerform(grantee==null?null:AuthProvider.getAuthToken(grantee), target, right, asAdmin, defaultValue);
        assertEquals(expected, result);
        
        // String interface
        result = mAM.canPerform(grantee==null?null:grantee.getName(), target, right, asAdmin, defaultValue);
        assertEquals(expected, result);
    }
    
    /*
     * test Zimbra user grantee
     */
    public void testGranteeUser() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account goodguy = prov.createAccount(getEmailAddr("testGranteeUser-goodguy"), PASSWORD, null);
        Account badguy = prov.createAccount(getEmailAddr("testGranteeUser-badguy"), PASSWORD, null);
        Account nobody = prov.createAccount(getEmailAddr("testGranteeUser-nobody"), PASSWORD, null);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, Provisioning.TRUE);
        Account admin = prov.createAccount(getEmailAddr("testGranteeUser-admin"), PASSWORD, attrs);
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGranteeUser-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(goodguy, Right.RT_viewFreeBusy, false));
        aces.add(new ZimbraACE(goodguy, Right.RT_invite, false));
        aces.add(new ZimbraACE(badguy, Right.RT_viewFreeBusy, true));
        aces.add(new ZimbraACE(ACL.ANONYMOUS_ACCT, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // self should always be allowed
        verify(target, target, Right.RT_invite, true);
        
        // admin access using admin privileges
        verify(admin, target, Right.RT_invite, true, true);
        
        // admin access NOT using admin privileges
        verify(admin, target, Right.RT_invite, false, false);
        
        // specifically allowed
        verify(goodguy, target, Right.RT_viewFreeBusy, true);
        verify(goodguy, target, Right.RT_invite, true);
        
        // specifically denied
        verify(badguy, target, Right.RT_viewFreeBusy, false);
        
        // not specifically allowed or denied, but PUB is allowed
        verify(nobody, target, Right.RT_viewFreeBusy, true);
        
        // not specifically allowed or denied
        verify(nobody, target, Right.RT_invite, false);
    }
    
    /*
     * test all(all authed Zimbra users) grantee
     */
    public void testGranteeAllAuthUser() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account guest = guestAccount("guest@external.com", "whocares");
        Account zimbra = prov.createAccount(getEmailAddr("testGranteeAllAuthUser-zimbra"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGranteeAllAuthUser-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(ACL.GUID_AUTHUSER, GranteeType.GT_AUTHUSER, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // zimbra user should be allowed
        verify(zimbra, target, Right.RT_viewFreeBusy, true);
        
        // external usr should not be allowed
        verify(guest, target, Right.RT_viewFreeBusy, false);
        
        // no one should be allowed for a non-granted right
        verify(zimbra, target, Right.RT_invite, false);
        verify(guest, target, Right.RT_invite, false);
    }
    
    /*
     * test guest(with a non-Zimbra email address, and a password) grantee
     * Note: GST grantee is not yet implemented for now, the result will be the same as PUB grantee, which is supported)
     */
    public void testGranteeGuest() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account guest = guestAccount("guest@external.com", "whocares");
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGranteeGuest-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(ACL.ANONYMOUS_ACCT, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // right allowed for PUB
        verify(guest, target, Right.RT_viewFreeBusy, true);
        
        // right not in ACL
        verify(guest, target, Right.RT_invite, false);
    }
    
    /*
     * test anonymous(without any identity) grantee
     */
    public void testGranteeAnon() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account anon = anonAccount();
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGranteeAnon-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(ACL.ANONYMOUS_ACCT, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // anon grantee
        verify(anon, target, Right.RT_viewFreeBusy, true);
        verify(anon, target, Right.RT_invite, false);
        
        // null grantee
        verify(null, target, Right.RT_viewFreeBusy, true);
        verify(null, target, Right.RT_invite, false);
    }
    
    public void testGranteeGroupSimple() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account user1 = prov.createAccount(getEmailAddr("testGranteeGroupSimple-user1"), PASSWORD, null);
        Account user2 = prov.createAccount(getEmailAddr("testGranteeGroupSimple-user2"), PASSWORD, null);
        Account user3 = prov.createAccount(getEmailAddr("testGranteeGroupSimple-user3"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList groupA = prov.createDistributionList(getEmailAddr("testGranteeGroupSimple-groupA"), new HashMap<String, Object>());
        prov.addMembers(groupA, new String[] {user1.getName(), user2.getName()});
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGroup-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(user1, Right.RT_viewFreeBusy, true));
        aces.add(new ZimbraACE(groupA, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // group member, but account is specifically denied
        verify(user1, target, Right.RT_viewFreeBusy, false);
        
        // group member
        verify(user2, target, Right.RT_viewFreeBusy, true);
        
        // not group member
        verify(user3, target, Right.RT_viewFreeBusy, false);
    }
    
    /*
     * Test this insane membership: user1 should be DENIED via G6(D)
     * 
     *          G1(A)                      G4(D)
     *          / \                        / \
     *      user1  G2(D)                usr1  G5(A)
     *             / \                        / \
     *         user1  G3(A)               user1  G6(D)
     *                 |                          |
     *               user1                      user1
     * 
     */
    public void testGranteeGroup() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account user1 = prov.createAccount(getEmailAddr("testGranteeGroup-user1"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList G1 = prov.createDistributionList(getEmailAddr("testGranteeGroup-G1"), new HashMap<String, Object>());
        DistributionList G2 = prov.createDistributionList(getEmailAddr("testGranteeGroup-G2"), new HashMap<String, Object>());
        DistributionList G3 = prov.createDistributionList(getEmailAddr("testGranteeGroup-G3"), new HashMap<String, Object>());
        DistributionList G4 = prov.createDistributionList(getEmailAddr("testGranteeGroup-G4"), new HashMap<String, Object>());
        DistributionList G5 = prov.createDistributionList(getEmailAddr("testGranteeGroup-G5"), new HashMap<String, Object>());
        DistributionList G6 = prov.createDistributionList(getEmailAddr("testGranteeGroup-G6"), new HashMap<String, Object>());

        prov.addMembers(G1, new String[] {user1.getName(), G2.getName()});
        prov.addMembers(G2, new String[] {user1.getName(), G3.getName()});
        prov.addMembers(G3, new String[] {user1.getName()});
        prov.addMembers(G4, new String[] {user1.getName(), G5.getName()});
        prov.addMembers(G5, new String[] {user1.getName(), G6.getName()});
        prov.addMembers(G6, new String[] {user1.getName()});
        
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGranteeGroup-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(G1, Right.RT_viewFreeBusy, false));
        aces.add(new ZimbraACE(G2, Right.RT_viewFreeBusy, true));
        aces.add(new ZimbraACE(G3, Right.RT_viewFreeBusy, false));
        aces.add(new ZimbraACE(G4, Right.RT_viewFreeBusy, true));
        aces.add(new ZimbraACE(G5, Right.RT_viewFreeBusy, false));
        aces.add(new ZimbraACE(G6, Right.RT_viewFreeBusy, true));
        PermUtil.grantAccess(target, aces);
        
        verify(user1, target, Right.RT_viewFreeBusy, false);
    }
    
    /*
     * test target with no ACL, should return caller default regardless who is the grantee and which right to ask for
     */
    public void testNoACL() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account zimbraUser = prov.createAccount(getEmailAddr("zimbra-user"), PASSWORD, null);
        Account guest = guestAccount("guest@external.com", "whocares");
        Account anon = anonAccount();
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testNoACL-target"), PASSWORD, null);
        
        for (Right right : RightManager.getInstance().getAllRights().values()) {
            verifyDefault(zimbraUser, target, right);
            verifyDefault(guest, target, right);
            verifyDefault(anon, target, right);
        }
    }
    
    public void testGrantConflict() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account conflict = prov.createAccount(getEmailAddr("testGrantConflict-conflict"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGrantConflict-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(conflict, Right.RT_viewFreeBusy, true));
        aces.add(new ZimbraACE(conflict, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // verify that only one is added 
        Set<ZimbraACE> acl = PermUtil.getACEs(target, null);
        assertEquals(1, acl.size());
    }
    
    public void testGrantDuplicate() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account duplicate = prov.createAccount(getEmailAddr("testGrantDuplicate-duplicate"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGrantDuplicate-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(duplicate, Right.RT_viewFreeBusy, true));
        aces.add(new ZimbraACE(duplicate, Right.RT_viewFreeBusy, true));
        PermUtil.grantAccess(target, aces);
        
        // verify that only one is added 
        Set<ZimbraACE> acl = PermUtil.getACEs(target, null);
        assertEquals(1, acl.size());
    }
    
    public void testGrant() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account user = prov.createAccount(getEmailAddr("testGrant-user"), PASSWORD, null);
        DistributionList group = prov.createDistributionList(getEmailAddr("testGrant-group"), new HashMap<String, Object>());
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testGrant-target"), PASSWORD, null);
        
        // grant some permissions 
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(user, Right.RT_viewFreeBusy, true));
        aces.add(new ZimbraACE(group, Right.RT_viewFreeBusy, true));
        PermUtil.grantAccess(target, aces);
        
        // verify the grants were added
        Set<ZimbraACE> acl = PermUtil.getACEs(target, null);
        assertEquals(2, acl.size());
        
        // grant some more
        aces.clear();
        aces.add(new ZimbraACE(ACL.ANONYMOUS_ACCT, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // verify the grants were added
        acl = PermUtil.getACEs(target, null);
        assertEquals(3, acl.size());
        
        // grant some more
        aces.clear();
        aces.add(new ZimbraACE(ACL.GUID_AUTHUSER, GranteeType.GT_AUTHUSER, Right.RT_viewFreeBusy, false));
        PermUtil.grantAccess(target, aces);
        
        // verify the grants were added
        acl = PermUtil.getACEs(target, null);
        assertEquals(4, acl.size());
        
    }

    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");

        TestUtil.runTest(new TestSuite(TestACL.class));
    }
}
