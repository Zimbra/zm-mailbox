package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightUtil;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.qa.unittest.TestACL.TestViaGrant;

public class TestACLPrecedence extends TestACL {
    
    
    /*
     * Notions:
     * A - account (not a grantee)
     * G - group (not a grantee)
     * 
     * Grantees:
     * GA - user grantee
     * GG - group grantee
     * 
     * Targets:
     * TA  - account target
     * TCR - calendar resource target
     * TC  - cos target
     * TDL - distribution list target
     * TD  - domain target
     * TS  - server target
     * TCF - config target
     * TG  - global grant target
     * 
     * Rights:
     * P - positive grant
     * N - negative grant
     * R - right
     * 
     * 
     * 
     * Grant:
     * {target-type}-{target}-{grantee-type}-{grantee}-{P or N}-{right}
     * 
     */

    /*
     * Denied over allowed - same account grantee
     * 
     * Grantee:
     *     granted to the same user grantee - GA
     *     one grant allow, one grant deny.
     *     
     * Target:
     *     granted on the same target entry - TA
     * 
     * Right:
     *     R
     *     
     * Expected: 
     *     Denied via grant account-TA-user-GA-N-R
     *     
     * Note:    
     *     This granting is now allowed by SOAP/zmprov.  But can sneak in
     *     by direct ldapmodify
     * 
     */
    public void test1() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account GA = mProv.createAccount(getEmailAddr(testName, "GA"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(GA, USER_RIGHT, ALLOW));
        aces.add(newUsrACE(GA, USER_RIGHT, DENY));
        
        // skip granting code and do direct ldap modify
        Map<String, Object> attrs = new HashMap<String, Object>();
        List<String> values = new ArrayList<String>();
        for (ZimbraACE ace : aces)
            values.add(ace.serialize());
        attrs.put(Provisioning.A_zimbraACE, values);
        mProv.modifyAttrs(TA, attrs);
        
        // verify that both are indeed added
        List<ZimbraACE> acl = RightUtil.getAllACEs(TA);
        assertEquals(2, acl.size());
        
        TestViaGrant via;
        
        // verify that the negative grant is honored
        via = new TestViaGrant(TargetType.account, TA, GranteeType.GT_USER, GA.getName(), USER_RIGHT, NEGATIVE);
        verify(GA, TA, USER_RIGHT, DENY, via);
    }
    

    /*
     * Denied over allowed - same group grantee
     * 
     * Membership:
     *     GG
     *      |
     *      A
     *
     * Grantee:
     *     granted to the same group grantee - GG
     *     one grant allow, one grant deny.
     *     
     * Target:
     *     granted on the same target entry - TA
     *
     * Right:
     *     R
     *
     * Expected: 
     *     Denied via grant account-TA-group-GG-N-R
     *     
     * Note:    
     *     This granting is now allowed by SOAP/zmprov.  But can sneak in
     *     by direct ldapmodify
     * 
     */
    public void test2() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account A = mProv.createAccount(getEmailAddr(testName, "A"), PASSWORD, null);
        DistributionList GG = mProv.createDistributionList(getEmailAddr(testName, "GG"), new HashMap<String, Object>());
        mProv.addMembers(GG, new String[] {A.getName()});
        
        /*
         * setup targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(GG, USER_RIGHT, ALLOW));
        aces.add(newGrpACE(GG, USER_RIGHT, DENY));
        
        // skip granting code and do direct ldap modify
        Map<String, Object> attrs = new HashMap<String, Object>();
        List<String> values = new ArrayList<String>();
        for (ZimbraACE ace : aces)
            values.add(ace.serialize());
        attrs.put(Provisioning.A_zimbraACE, values);
        mProv.modifyAttrs(TA, attrs);
        
        // verify that both are indeed added
        List<ZimbraACE> acl = RightUtil.getAllACEs(TA);
        assertEquals(2, acl.size());
        
        TestViaGrant via;
        
        // verify that the negative grant is honored
        via = new TestViaGrant(TargetType.account, TA, GranteeType.GT_GROUP, GG.getName(), USER_RIGHT, NEGATIVE);
        verify(A, TA, USER_RIGHT, DENY, via);
    }
    
    /*
     * Denied over allowed - same distance group grantee
     *
     * Membership:
     *     GG1       GG2
     *      |          |
     *      A          A
     *
     * Grantee:
     *     granted to two different groups - GG1 and GG2, both with the same distance to the user U
     *     allow GG1, deny GG2.
     *     
     * Target:
     *     granted on the same target entry - TA
     * 
     * Right:
     *     R
     * 
     * Expected: 
     *     Denied via grant account-TA-group-GG2-N-R
     *     
     * Note:    
     *     This granting is now allowed by SOAP/zmprov.  But can sneak in
     *     by direct ldapmodify
     * 
     */
    public void test3() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account A = mProv.createAccount(getEmailAddr(testName, "A"), PASSWORD, null);
        DistributionList GG1 = mProv.createDistributionList(getEmailAddr(testName, "GG1"), new HashMap<String, Object>());
        DistributionList GG2 = mProv.createDistributionList(getEmailAddr(testName, "GG2"), new HashMap<String, Object>());
        mProv.addMembers(GG1, new String[] {A.getName()});
        mProv.addMembers(GG2, new String[] {A.getName()});
        
        
        /*
         * setup targets
         */
        Account TA = mProv.createAccount(getEmailAddr(testName, "TA"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(GG1, USER_RIGHT, ALLOW));
        aces.add(newGrpACE(GG2, USER_RIGHT, DENY));
        grantRight(TargetType.account, TA, aces);
        
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, TA, GranteeType.GT_GROUP, GG1.getName(), USER_RIGHT, NEGATIVE);
        via.addCanAlsoVia(new TestViaGrant(TargetType.account, TA, GranteeType.GT_GROUP, GG2.getName(), USER_RIGHT, NEGATIVE));
        verify(A, TA, USER_RIGHT, DENY, via);
    }
    
    
    /*
     * Denied over allowed - same distance group grantee
     *
     * Membership:
     *          G1(A)                      G4(D)
     *          / \                        / \
     *         A  G2(D)                  A  G5(A)
     *             / \                        / \
     *            A  G3(A)                   A  G6(D)
     *                 |                          |
     *                 A                          A
     * 
     *
     * Grantee:
     *     GG1(allow), GG2(deny), GG3(allow), GG4(deny), GG5(allow), GG6(deny)
     *     
     * Target:
     *     granted on the same target entry - TA
     * 
     * Right:
     *     R
     * 
     * Expected: 
     *     Denied via grant account-TA-group-GG2-N-R or account-TA-group-GG4-N-R or account-TA-group-GG6-N-R
     * 
     */
    public void test4() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account A = mProv.createAccount(getEmailAddr(testName, "A"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList GG1 = mProv.createDistributionList(getEmailAddr(testName, "GG1"), new HashMap<String, Object>());
        DistributionList GG2 = mProv.createDistributionList(getEmailAddr(testName, "GG2"), new HashMap<String, Object>());
        DistributionList GG3 = mProv.createDistributionList(getEmailAddr(testName, "GG3"), new HashMap<String, Object>());
        DistributionList GG4 = mProv.createDistributionList(getEmailAddr(testName, "GG4"), new HashMap<String, Object>());
        DistributionList GG5 = mProv.createDistributionList(getEmailAddr(testName, "GG5"), new HashMap<String, Object>());
        DistributionList GG6 = mProv.createDistributionList(getEmailAddr(testName, "GG6"), new HashMap<String, Object>());

        mProv.addMembers(GG1, new String[] {A.getName(), GG2.getName()});
        mProv.addMembers(GG2, new String[] {A.getName(), GG3.getName()});
        mProv.addMembers(GG3, new String[] {A.getName()});
        mProv.addMembers(GG4, new String[] {A.getName(), GG5.getName()});
        mProv.addMembers(GG5, new String[] {A.getName(), GG6.getName()});
        mProv.addMembers(GG6, new String[] {A.getName()});
        
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(GG1, USER_RIGHT, ALLOW));
        aces.add(newGrpACE(GG2, USER_RIGHT, DENY));
        aces.add(newGrpACE(GG3, USER_RIGHT, ALLOW));
        aces.add(newGrpACE(GG4, USER_RIGHT, DENY));
        aces.add(newGrpACE(GG5, USER_RIGHT, ALLOW));
        aces.add(newGrpACE(GG6, USER_RIGHT, DENY));
        grantRight(TargetType.account, target, aces);
        
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, GG2.getName(), USER_RIGHT, NEGATIVE);
        via.addCanAlsoVia(new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, GG4.getName(), USER_RIGHT, NEGATIVE));
        via.addCanAlsoVia(new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, GG6.getName(), USER_RIGHT, NEGATIVE));
        verify(A, target, USER_RIGHT, DENY, via);
    }
    
    
    /*
     Combining Target Scope and Grantee Scope: Grantee Relativity takes Precedence over Target Relativity
       For example, for this target hierarchy:
           domain D
               group G1 (allow right R to group GC)
                   group G2 (deny right R to group GB)
                       group G3 (deny right R to group GA)
                           user account U   
                       
       And this grantee hierarchy:
           group GA
               group GB
                   group GC
                       (admin) account A
                   
       Then A is *allowed* for right R on target account U, because GC is more specific to A than GA and GB.
       Even if on the target side, grant on G3(grant to GA) and G2(grant to GB) is more specific than the 
       grant on G1(grant to GC).          
       
       the above is no longer true
                   
     */
    public void test5() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList GA = mProv.createDistributionList(getEmailAddr(testName, "GA"), new HashMap<String, Object>());
        DistributionList GB = mProv.createDistributionList(getEmailAddr(testName, "GB"), new HashMap<String, Object>());
        DistributionList GC = mProv.createDistributionList(getEmailAddr(testName, "GC"), new HashMap<String, Object>());

        mProv.addMembers(GA, new String[] {GB.getName()});
        mProv.addMembers(GB, new String[] {GC.getName()});
        mProv.addMembers(GC, new String[] {grantee.getName()});
        
        
        /*
         * setup targets
         */
        TestViaGrant via;
        
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        
        DistributionList G1 = mProv.createDistributionList(getEmailAddr(testName, "G1"), new HashMap<String, Object>());
        DistributionList G2 = mProv.createDistributionList(getEmailAddr(testName, "G2"), new HashMap<String, Object>());
        DistributionList G3 = mProv.createDistributionList(getEmailAddr(testName, "G3"), new HashMap<String, Object>());
        
        mProv.addMembers(G1, new String[] {G2.getName()});
        mProv.addMembers(G2, new String[] {G3.getName()});
        mProv.addMembers(G3, new String[] {target.getName()});
        
        Right right = UserRight.R_viewFreeBusy;
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(GC, right, ALLOW));
        grantRight(TargetType.distributionlist, G1, aces);
        
        aces.clear();
        aces.add(newGrpACE(GB, right, DENY));
        grantRight(TargetType.distributionlist, G2, aces);
        
        aces.clear();
        aces.add(newGrpACE(GA, right, DENY));
        grantRight(TargetType.distributionlist, G3, aces);
        
        // the right should be allowed via the grant on G1, granted to group GC 
        /*
        via = new TestViaGrant(TargetType.distributionlist, G1, GranteeType.GT_GROUP, GC.getName(), right, POSITIVE);
        verify(grantee, target, right, ALLOW, via);
        */
        
        via = new TestViaGrant(TargetType.distributionlist, G2, GranteeType.GT_GROUP, GB.getName(), right, NEGATIVE);
        via.addCanAlsoVia(new TestViaGrant(TargetType.distributionlist, G3, GranteeType.GT_GROUP, GA.getName(), right, NEGATIVE));
        verify(grantee, target, right, DENY, via);
        
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLPrecedence.class);
    }
}
