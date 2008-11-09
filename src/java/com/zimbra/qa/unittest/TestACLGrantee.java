package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RoleAccessManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;

public class TestACLGrantee extends TestACL {
    
    /*
     * ======================
     * ======================
     *     Grantee Tests
     * ======================
     * ======================
     */
    
    /*
     * test Zimbra user grantee
     */
    public void testGranteeUser() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account goodguy = mProv.createAccount(getEmailAddr(testName, "goodguy"), PASSWORD, null);
        Account badguy = mProv.createAccount(getEmailAddr(testName, "badguy"), PASSWORD, null);
        Account nobody = mProv.createAccount(getEmailAddr(testName, "nobody"), PASSWORD, null);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, Provisioning.TRUE);
        Account admin = mProv.createAccount(getEmailAddr(testName, "admin"), PASSWORD, attrs);
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(goodguy, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newUsrACE(goodguy, UserRight.RT_invite, ALLOW));
        aces.add(newUsrACE(badguy, UserRight.RT_viewFreeBusy, DENY));
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        // self should always be allowed
        verify(target, target, UserRight.RT_invite, ALLOW, null);
        
        // admin access using admin privileges
        if (AccessManager.getInstance() instanceof RoleAccessManager) // *all* decisions are based on ACL, admins don't have special rights 
            verify(admin, target, UserRight.RT_invite, AS_ADMIN, DENY, null);
        else
            verify(admin, target, UserRight.RT_invite, AS_ADMIN, ALLOW, null);
        
        // admin access NOT using admin privileges
        verify(admin, target, UserRight.RT_invite, AS_USER, DENY, null);
        
        TestViaGrant via;
        
        // specifically allowed
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, goodguy.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(goodguy, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, goodguy.getName(), UserRight.RT_invite, POSITIVE);
        verify(goodguy, target, UserRight.RT_invite, ALLOW, via);
        
        // specifically denied
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, badguy.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        verify(badguy, target, UserRight.RT_viewFreeBusy, DENY, via);
        
        // not specifically allowed or denied, but PUB is allowed
        verify(nobody, target, UserRight.RT_viewFreeBusy, ALLOW, null);
        
        // not specifically allowed or denied
        verify(nobody, target, UserRight.RT_invite, DENY, null);
    }
    
    /*
     * test all(all authed Zimbra users) grantee
     */
    public void testGranteeAllAuthUser() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account guest = guestAccount("guest@external.com", "whocares");
        Account zimbra = mProv.createAccount(getEmailAddr(testName, "zimbra"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newAllACE(UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        TestViaGrant via;
        
        // zimbra user should be allowed
        via = new AuthUserViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(zimbra, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        // external usr should not be allowed
        verify(guest, target, UserRight.RT_viewFreeBusy, DENY, null);
        
        // non granted right should honor callsite default
        verifyDefault(zimbra, target, UserRight.RT_invite);
        verifyDefault(guest, target, UserRight.RT_invite);
    }
    
    /*
     * test guest(with a non-Zimbra email address, and a password) grantee
     * Note: GST grantee is not yet implemented for now, the result will be the same as PUB grantee, which is supported)
     */
    public void testGranteeGuest() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account guest = guestAccount("guest@external.com", "whocares");
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        TestViaGrant via;
        
        // right allowed for PUB
        via = new PubViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(guest, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        // right not in ACL
        verifyDefault(guest, target, UserRight.RT_invite);
    }
    
    /*
     * test key(with an accesskey) grantee
     * Note: GST grantee is not yet implemented for now, the result will be the same as PUB grantee, which is supported)
     */
    public void testGranteeKey() throws Exception {
        String testName = getName();
        
        final String GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED          = "allowedKeyProvided with space";
        final String KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED  = "allowed my access key";
        final String GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED         = "allowedKeyGenerated@external.com";
              String KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED = null;  // will know after the grant
        
        final String GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED           = "deniedKeyProvided with space";
        final String KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED   = "denied my access key";
        final String GRANTEE_NAME_DENIED_KEY_KEY_GENERATED          = "deniedKeyGenerated@external.com";
              String KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_GENERATED  = null;  // will know after the grant
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newKeyACE(GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED, KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newKeyACE(GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED, null, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED, KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED, UserRight.RT_viewFreeBusy, DENY));
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_GENERATED, null, UserRight.RT_viewFreeBusy, DENY));
        Set<ZimbraACE> grantedAces = grantRight(TargetType.account, target, aces);
        
        /*
         * get generated accesskey to build our test accounts
         */
        for (ZimbraACE ace : grantedAces) {
            if (ace.getGrantee().equals(GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED))
                KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED = ace.getSecret();
            if (ace.getGrantee().equals(GRANTEE_NAME_DENIED_KEY_KEY_GENERATED))
                KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_GENERATED = ace.getSecret();
        }
        
        /*
         * setup grantees
         */
        Account allowedKeyProvided = keyAccount(GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED, KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED);
        Account allowedKeyGenerated = keyAccount(GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED, KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED);
        Account allowedKeyGeneratedWrongAccessKey = keyAccount(GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED, KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED+"bogus");
        Account deniedKeyProvided = keyAccount(GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED, KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED);
        Account deniedKeyGenerated = keyAccount(GRANTEE_NAME_DENIED_KEY_KEY_GENERATED, KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_GENERATED);
        
        TestViaGrant via;
        
        /*
         * test allowed
         */
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, allowedKeyProvided.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyProvided, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, allowedKeyGenerated.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyGenerated, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        verify(allowedKeyGeneratedWrongAccessKey, target, UserRight.RT_viewFreeBusy, DENY, null);
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, deniedKeyProvided.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        verify(deniedKeyProvided, target, UserRight.RT_viewFreeBusy, DENY, via);
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, deniedKeyGenerated.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        verify(deniedKeyGenerated, target, UserRight.RT_viewFreeBusy, DENY, via);
        
        /*
         * add a pub grant
         */
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        /*
         * verify the effect
         */
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, allowedKeyProvided.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyProvided, target, UserRight.RT_viewFreeBusy, ALLOW, via);  // still allowed
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, allowedKeyGenerated.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyGenerated, target, UserRight.RT_viewFreeBusy, ALLOW, via); // still allowed
        
        via = new PubViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyGeneratedWrongAccessKey, target, UserRight.RT_viewFreeBusy, ALLOW, via); // wrong key doesn't matter now, because it will be allowed via the pub grant
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, deniedKeyProvided.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        verify(deniedKeyProvided, target, UserRight.RT_viewFreeBusy, DENY, via);  // specifically denied should still be denied
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, deniedKeyGenerated.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        verify(deniedKeyGenerated, target, UserRight.RT_viewFreeBusy, DENY, via); // specifically denied should still be denied
        
        
        /*
         * revoke the denied grants
         */
        aces.clear();
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED, "doesn't matter", UserRight.RT_viewFreeBusy, DENY));
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_GENERATED, null, UserRight.RT_viewFreeBusy, DENY));
        revokeRight(TargetType.account, target, aces);
        
        /*
         * now everybody should be allowed
         */
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, allowedKeyProvided.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyProvided, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_KEY, allowedKeyGenerated.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyGenerated, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        via = new PubViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(allowedKeyGeneratedWrongAccessKey, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        via = new PubViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(deniedKeyProvided, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        via = new PubViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(deniedKeyGenerated, target, UserRight.RT_viewFreeBusy, ALLOW, via);

        // right not in ACL
        verifyDefault(allowedKeyGenerated, target, UserRight.RT_invite);
    }
    
    public void testGranteeKeyInvalidParams() throws Exception {
        String testName = getName();
        
        Account user1 = mProv.createAccount(getEmailAddr(testName, "user1"), PASSWORD, null);
        
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newKeyACE("good", "abc", UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newKeyACE("bad:aaa:bbb", "xxx:yyy", UserRight.RT_viewFreeBusy, ALLOW));  // bad name/accesskey, containing ":"
        aces.add(newUsrACE(user1, UserRight.RT_viewFreeBusy, ALLOW));
        
        try {
            Set<ZimbraACE> grantedAces = grantRight(TargetType.account, target, aces);
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.INVALID_REQUEST)) {
                // make sure nothing is granted, including the good ones
                if (PermUtil.getAllACEs(target) == null)
                    return; // good!
            }
        }
        fail();  
    }
    
    /*
     * test anonymous(without any identity) grantee
     */
    public void testGranteeAnon() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account anon = anonAccount();
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        TestViaGrant via;
        
        // anon grantee
        via = new PubViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(anon, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        verifyDefault(anon, target, UserRight.RT_invite);
        
        // null grantee
        via = new PubViaGrant(TargetType.account, target, UserRight.RT_viewFreeBusy, POSITIVE);
        verify(null, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        verifyDefault(null, target, UserRight.RT_invite);
    }
    
    public void testGranteeGroupSimple() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account user1 = mProv.createAccount(getEmailAddr(testName, "user1"), PASSWORD, null);
        Account user2 = mProv.createAccount(getEmailAddr(testName, "user2"), PASSWORD, null);
        Account user3 = mProv.createAccount(getEmailAddr(testName, "user3"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList groupA = mProv.createDistributionList(getEmailAddr(testName, "groupA"), new HashMap<String, Object>());
        mProv.addMembers(groupA, new String[] {user1.getName(), user2.getName()});
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr("testGroup-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(user1, UserRight.RT_viewFreeBusy, DENY));
        aces.add(newGrpACE(groupA, UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        TestViaGrant via;
        
        // group member, but account is specifically denied
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, user1.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        verify(user1, target, UserRight.RT_viewFreeBusy, DENY, via);
        
        // group member
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, groupA.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(user2, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        // not group member
        verify(user3, target, UserRight.RT_viewFreeBusy, DENY, null);
    }
    
    /*
     * Test this membership: user1 should be DENIED via G2(D) or G4(D), G6(D) - they all have the same distance to user1.
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
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account user1 = mProv.createAccount(getEmailAddr(testName, "user1"), PASSWORD, null);
        
        /*
         * setup groups
         */
        DistributionList G1 = mProv.createDistributionList(getEmailAddr(testName, "G1"), new HashMap<String, Object>());
        DistributionList G2 = mProv.createDistributionList(getEmailAddr(testName, "G2"), new HashMap<String, Object>());
        DistributionList G3 = mProv.createDistributionList(getEmailAddr(testName, "G3"), new HashMap<String, Object>());
        DistributionList G4 = mProv.createDistributionList(getEmailAddr(testName, "G4"), new HashMap<String, Object>());
        DistributionList G5 = mProv.createDistributionList(getEmailAddr(testName, "G5"), new HashMap<String, Object>());
        DistributionList G6 = mProv.createDistributionList(getEmailAddr(testName, "G6"), new HashMap<String, Object>());

        mProv.addMembers(G1, new String[] {user1.getName(), G2.getName()});
        mProv.addMembers(G2, new String[] {user1.getName(), G3.getName()});
        mProv.addMembers(G3, new String[] {user1.getName()});
        mProv.addMembers(G4, new String[] {user1.getName(), G5.getName()});
        mProv.addMembers(G5, new String[] {user1.getName(), G6.getName()});
        mProv.addMembers(G6, new String[] {user1.getName()});
        
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(G1, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newGrpACE(G2, UserRight.RT_viewFreeBusy, DENY));
        aces.add(newGrpACE(G3, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newGrpACE(G4, UserRight.RT_viewFreeBusy, DENY));
        aces.add(newGrpACE(G5, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newGrpACE(G6, UserRight.RT_viewFreeBusy, DENY));
        grantRight(TargetType.account, target, aces);
        
        TestViaGrant via;
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, G2.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        via.addCanAlsoVia(new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, G4.getName(), UserRight.RT_viewFreeBusy, NEGATIVE));
        via.addCanAlsoVia(new TestViaGrant(TargetType.account, target, GranteeType.GT_GROUP, G6.getName(), UserRight.RT_viewFreeBusy, NEGATIVE));
        verify(user1, target, UserRight.RT_viewFreeBusy, DENY, via);
    }
    
    /*
     * test target with no ACL, should return caller default regardless who is the grantee and which right to ask for
     */
    public void testNoACL() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account zimbraUser = mProv.createAccount(getEmailAddr(testName, "user"), PASSWORD, null);
        Account guest = guestAccount("guest@external.com", "whocares");
        Account anon = anonAccount();
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        
        for (Right right : RightManager.getInstance().getAllUserRights().values()) {
            verifyDefault(zimbraUser, target, right);
            verifyDefault(guest, target, right);
            verifyDefault(anon, target, right);
        }
    }
    
    /*
     * test target with no ACL for the requested right but does have ACL for some other rights.
     * should return caller default for the right that does not have any ACL (bug 30241), 
     * should allow/disallow for the rights according to the ACE.
     */
    public void testDefaultWithNonEmptyACL() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account zimbraUser = mProv.createAccount(getEmailAddr(testName, "user"), PASSWORD, null);
        Account guest = guestAccount("guest@external.com", "whocares");
        Account anon = anonAccount();
        
        Right rightGranted = UserRight.RT_viewFreeBusy;
        Right rightNotGranted = UserRight.RT_invite;
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(zimbraUser, rightGranted, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        TestViaGrant via;
        
        // verify callsite default is honored for not granted right
        verifyDefault(zimbraUser, target, rightNotGranted);
        verifyDefault(guest, target, rightNotGranted);
        verifyDefault(anon, target, rightNotGranted);
        
        // verify granted right is properly processed
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, zimbraUser.getName(), rightGranted, POSITIVE);
        verify(zimbraUser, target, rightGranted, ALLOW, via);
        
        verify(guest, target, rightGranted, DENY, null);
    }    
    
    public void testGrantConflict() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newUsrACE(grantee, UserRight.RT_viewFreeBusy, DENY));
        grantRight(TargetType.account, target, aces);
        
        // verify that only one is added 
        Set<ZimbraACE> acl = PermUtil.getAllACEs(target);
        assertEquals(1, acl.size());
    }
    
    public void testGrantConflictDirectLdapModify() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account grantee = mProv.createAccount(getEmailAddr(testName, "grantee"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, UserRight.RT_viewFreeBusy, ALLOW));
        aces.add(newUsrACE(grantee, UserRight.RT_viewFreeBusy, DENY));
        
        // skip granting code and do direct ldap modify
        Map<String, Object> attrs = new HashMap<String, Object>();
        List<String> values = new ArrayList<String>();
        for (ZimbraACE ace : aces)
            values.add(ace.serialize());
        attrs.put(Provisioning.A_zimbraACE, values);
        mProv.modifyAttrs(target, attrs);
        
        // verify that both are indeed added
        Set<ZimbraACE> acl = PermUtil.getAllACEs(target);
        assertEquals(2, acl.size());
        
        TestViaGrant via;
        
        // verify that the negative grant is honored
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, grantee.getName(), UserRight.RT_viewFreeBusy, NEGATIVE);
        verify(grantee, target, UserRight.RT_viewFreeBusy, DENY, via);
    }
    
    public void testGrantDuplicate() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account duplicate = mProv.createAccount(getEmailAddr(testName, "duplicate"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(duplicate, UserRight.RT_viewFreeBusy, DENY));
        aces.add(newUsrACE(duplicate, UserRight.RT_viewFreeBusy, DENY));
        grantRight(TargetType.account, target, aces);
        
        // verify that only one is added 
        Set<ZimbraACE> acl = PermUtil.getAllACEs(target);
        assertEquals(1, acl.size());
    }
    
    public void testGrant() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account user = mProv.createAccount(getEmailAddr(testName, "user"), PASSWORD, null);
        DistributionList group = mProv.createDistributionList(getEmailAddr(testName, "group"), new HashMap<String, Object>());
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        
        // grant some permissions 
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(user, UserRight.RT_viewFreeBusy, DENY));
        aces.add(newGrpACE(group, UserRight.RT_viewFreeBusy, DENY));
        grantRight(TargetType.account, target, aces);
        
        // verify the grants were added
        Set<ZimbraACE> acl = PermUtil.getAllACEs(target);
        assertEquals(2, acl.size());
        
        // grant some more
        aces.clear();
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        // verify the grants were added
        acl = PermUtil.getAllACEs(target);
        assertEquals(3, acl.size());
        
        // grant some more
        aces.clear();
        aces.add(newAllACE(UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        // verify the grants were added
        acl = PermUtil.getAllACEs(target);
        assertEquals(4, acl.size());
        
    }
    
    public void testLoginAsRight() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account user = mProv.createAccount(getEmailAddr(testName, "user"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        
        TestViaGrant via;
        
        // grant some permissions 
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(user, UserRight.RT_loginAs, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        // verify the grant was added
        Set<ZimbraACE> acl = PermUtil.getAllACEs(target);
        assertEquals(1, acl.size());
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, user.getName(), UserRight.RT_loginAs, POSITIVE);
        verify(user, target, UserRight.RT_loginAs, ALLOW, via);
        
        // verify user can access target's account
        boolean canAccessAccount = mAM.canAccessAccount(user, target);
        assertTrue(canAccessAccount);
        
    }
    
    public void testRevoke() throws Exception {
        String testName = getName();
        
        /*
         * setup grantees
         */
        Account user = mProv.createAccount(getEmailAddr(testName, "user"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = mProv.createAccount(getEmailAddr(testName, "target"), PASSWORD, null);
        
        TestViaGrant via;
        
        // grant some permissions 
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(user, UserRight.RT_invite, ALLOW));
        aces.add(newUsrACE(user, UserRight.RT_viewFreeBusy, ALLOW));
        grantRight(TargetType.account, target, aces);
        
        // verify the grant was added
        Set<ZimbraACE> acl = PermUtil.getAllACEs(target);
        assertEquals(2, acl.size());
        
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, user.getName(), UserRight.RT_invite, POSITIVE);
        verify(user, target, UserRight.RT_invite, ALLOW, via);
        
        // revoke one right
        Set<ZimbraACE> acesToRevoke = new HashSet<ZimbraACE>();
        acesToRevoke.add(newUsrACE(user, UserRight.RT_invite, ALLOW));
        revokeRight(TargetType.account, target, acesToRevoke);
        
        // verify the grant was removed
        acl = PermUtil.getAllACEs(target);
        assertEquals(1, acl.size());
        verifyDefault(user, target, UserRight.RT_invite); // callsite default should now apply
        
        // verify the other right is still there
        via = new TestViaGrant(TargetType.account, target, GranteeType.GT_USER, user.getName(), UserRight.RT_viewFreeBusy, POSITIVE);
        verify(user, target, UserRight.RT_viewFreeBusy, ALLOW, via);
        
        // revoke the other right
        acesToRevoke = new HashSet<ZimbraACE>();
        acesToRevoke.add(newUsrACE(user, UserRight.RT_viewFreeBusy, ALLOW));
        revokeRight(TargetType.account, target, acesToRevoke);
        
        // verify all right are gone
        verifyDefault(user, target, UserRight.RT_invite);
        verifyDefault(user, target, UserRight.RT_viewFreeBusy);
        acl = PermUtil.getAllACEs(target);
        assertNull(acl);

        // revoke non-existing right, make sure we don't crash
        acesToRevoke = new HashSet<ZimbraACE>();
        acesToRevoke.add(newUsrACE(user, UserRight.RT_invite, ALLOW));
        revokeRight(TargetType.account, target, acesToRevoke);
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        TestUtil.runTest(TestACLGrantee.class);
    }
}
