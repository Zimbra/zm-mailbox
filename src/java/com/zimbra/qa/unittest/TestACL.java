package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.CliUtil;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.UserRight;
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
    
    /*
     * for unit testing only
     */
    private class KeyAuthToken extends AuthToken {

        private String mName;
        private String mAccessKey;
        
        KeyAuthToken(String name, String accessKey) {
            mName = name;
            mAccessKey = accessKey;
        }
        
        @Override
        public void encode(HttpClient client, HttpMethod method,
                boolean isAdminReq, String cookieDomain) throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public void encode(HttpState state, boolean isAdminReq, String cookieDomain)
                throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public void encode(HttpServletResponse resp, boolean isAdminReq)
                throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public void encodeAuthResp(Element parent, boolean isAdmin)
                throws ServiceException {
            // TODO Auto-generated method stub

        }

        @Override
        public String getAccountId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getAdminAccountId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getCrumb() throws AuthTokenException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDigest() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getEncoded() throws AuthTokenException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getExpires() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getExternalUserEmail() {
            // TODO Auto-generated method stub
            return mName;
        }

        @Override
        public boolean isAdmin() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDomainAdmin() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isExpired() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isZimbraUser() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ZAuthToken toZAuthToken() throws ServiceException {
            // TODO Auto-generated method stub
            return null;
        }
        
        public String getAccessKey() {
            return mAccessKey;
        }
    }
    
    private static String getEmailAddr(String localPart) {
        return localPart + "@" + DOMAIN_NAME;
    }
    
    private Account guestAccount(String email, String password) {
        return new ACL.GuestAccount(email, password);
    }
    
    private Account keyAccount(String name, String accesKey) {
        AuthToken authToken = new KeyAuthToken(name, accesKey);
        return new ACL.GuestAccount(authToken);
    }
    
    private Account anonAccount() {
        return ACL.ANONYMOUS_ACCT;
    }
    
    // construct a ACE with "pub" grantee type
    private ZimbraACE newPubACE(Right right, boolean deny) throws ServiceException {
        return new ZimbraACE(ACL.GUID_PUBLIC, GranteeType.GT_PUBLIC, right, deny, null);
    }
    
    // construct a ACE with "all" grantee type
    private ZimbraACE newAllACE(Right right, boolean deny) throws ServiceException {
        return new ZimbraACE(ACL.GUID_AUTHUSER, GranteeType.GT_AUTHUSER, right, deny, null);
    }
    
    // construct a ACE with "usr" grantee type
    private ZimbraACE newUsrACE(Account acct, Right right, boolean deny) throws ServiceException {
        return new ZimbraACE(acct.getId(), GranteeType.GT_USER, right, deny, null);
    }
    
    // construct a ACE with "grp" grantee type
    private ZimbraACE newGrpACE(DistributionList dl, Right right, boolean deny) throws ServiceException {
        return new ZimbraACE(dl.getId(), GranteeType.GT_GROUP, right, deny, null);
    }
    
    // construct a ACE with "key" grantee type
    private ZimbraACE newKeyACE(String nameOrEmail, String accessKey, Right right, boolean deny) throws ServiceException {
        return new ZimbraACE(nameOrEmail, GranteeType.GT_KEY, right, deny, accessKey);
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
        if (grantee instanceof ACL.GuestAccount && ((ACL.GuestAccount)grantee).getAccessKey() != null) {
            // string interface always return denied for key grantee unless there is a pub grant
            // skip the test for now, unless we want to pass yet another parameter to this method
            // i.e. - if no pub grant: should always expect false
            //      - if there is a pub grant: should expect the expected
            return;
        }
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
        aces.add(newUsrACE(goodguy, UserRight.RT_viewFreeBusy, false));
        aces.add(newUsrACE(goodguy, UserRight.RT_invite, false));
        aces.add(newUsrACE(badguy, UserRight.RT_viewFreeBusy, true));
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // self should always be allowed
        verify(target, target, UserRight.RT_invite, true);
        
        // admin access using admin privileges
        verify(admin, target, UserRight.RT_invite, true, true);
        
        // admin access NOT using admin privileges
        verify(admin, target, UserRight.RT_invite, false, false);
        
        // specifically allowed
        verify(goodguy, target, UserRight.RT_viewFreeBusy, true);
        verify(goodguy, target, UserRight.RT_invite, true);
        
        // specifically denied
        verify(badguy, target, UserRight.RT_viewFreeBusy, false);
        
        // not specifically allowed or denied, but PUB is allowed
        verify(nobody, target, UserRight.RT_viewFreeBusy, true);
        
        // not specifically allowed or denied
        verify(nobody, target, UserRight.RT_invite, false);
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
        aces.add(newAllACE(UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // zimbra user should be allowed
        verify(zimbra, target, UserRight.RT_viewFreeBusy, true);
        
        // external usr should not be allowed
        verify(guest, target, UserRight.RT_viewFreeBusy, false);
        
        // non granted right should honor callsite default
        verifyDefault(zimbra, target, UserRight.RT_invite);
        verifyDefault(guest, target, UserRight.RT_invite);
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
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // right allowed for PUB
        verify(guest, target, UserRight.RT_viewFreeBusy, true);
        
        // right not in ACL
        verifyDefault(guest, target, UserRight.RT_invite);
    }
    
    /*
     * test key(with an accesskey) grantee
     * Note: GST grantee is not yet implemented for now, the result will be the same as PUB grantee, which is supported)
     */
    public void testGranteeKey() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
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
        Account target = prov.createAccount(getEmailAddr("testGranteeKey-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newKeyACE(GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED, KEY_FOR_GRANTEE_NAME_ALLOWED_KEY_KEY_PROVIDED, UserRight.RT_viewFreeBusy, false));
        aces.add(newKeyACE(GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED, null, UserRight.RT_viewFreeBusy, false));
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED, KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED, UserRight.RT_viewFreeBusy, true));
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_GENERATED, null, UserRight.RT_viewFreeBusy, true));
        Set<ZimbraACE> grantedAces = PermUtil.grantAccess(prov, target, aces);
        
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
        Account deniedKeyGenerated = keyAccount(GRANTEE_NAME_ALLOWED_KEY_KEY_GENERATED, KEY_FOR_GRANTEE_NAME_DENIED_KEY_KEY_GENERATED);
        
        /*
         * test allowed
         */
        verify(allowedKeyProvided, target, UserRight.RT_viewFreeBusy, true);
        verify(allowedKeyGenerated, target, UserRight.RT_viewFreeBusy, true);
        verify(allowedKeyGeneratedWrongAccessKey, target, UserRight.RT_viewFreeBusy, false);
        verify(deniedKeyProvided, target, UserRight.RT_viewFreeBusy, false);
        verify(deniedKeyGenerated, target, UserRight.RT_viewFreeBusy, false);
        
        /*
         * add a pub grant
         */
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        /*
         * verify the effect
         */
        verify(allowedKeyProvided, target, UserRight.RT_viewFreeBusy, true);  // still allowed
        verify(allowedKeyGenerated, target, UserRight.RT_viewFreeBusy, true); // still allowed
        verify(allowedKeyGeneratedWrongAccessKey, target, UserRight.RT_viewFreeBusy, true); // wrong key doesn't matternow, because it will be allowed via the puv grant
        verify(deniedKeyProvided, target, UserRight.RT_viewFreeBusy, false);  // specifically denied should still be denied
        verify(deniedKeyGenerated, target, UserRight.RT_viewFreeBusy, false); // specifically denied should still be denied
        
        /*
         * revoke the denied grants
         */
        aces.clear();
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_PROVIDED, "doesn't matter", UserRight.RT_viewFreeBusy, true));
        aces.add(newKeyACE(GRANTEE_NAME_DENIED_KEY_KEY_GENERATED, null, UserRight.RT_viewFreeBusy, true));
        PermUtil.revokeAccess(prov, target, aces);
        
        /*
         * now everybody should be allowed
         */
        verify(allowedKeyProvided, target, UserRight.RT_viewFreeBusy, true);
        verify(allowedKeyGenerated, target, UserRight.RT_viewFreeBusy, true);
        verify(allowedKeyGeneratedWrongAccessKey, target, UserRight.RT_viewFreeBusy, true);
        verify(deniedKeyProvided, target, UserRight.RT_viewFreeBusy, true);
        verify(deniedKeyGenerated, target, UserRight.RT_viewFreeBusy, true);

        // right not in ACL
        verifyDefault(allowedKeyGenerated, target, UserRight.RT_invite);
    }
    
    public void testGranteeKeyInvalidParams() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account user1 = prov.createAccount(getEmailAddr("testGranteeKeyInvalidParams-user1"), PASSWORD, null);
        
        Account target = prov.createAccount(getEmailAddr("testGranteeKeyInvalidParams-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newKeyACE("good", "abc", UserRight.RT_viewFreeBusy, false));
        aces.add(newKeyACE("bad:aaa:bbb", "xxx:yyy", UserRight.RT_viewFreeBusy, false));  // bad name/accesskey, containing ":"
        aces.add(newUsrACE(user1, UserRight.RT_viewFreeBusy, false));
        
        try {
            Set<ZimbraACE> grantedAces = PermUtil.grantAccess(prov, target, aces);
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.INVALID_REQUEST)) {
                // make sure nothing is granted, including the good ones
                if (PermUtil.getACEs(target, null) == null)
                    return; // good!
            }
        }
        fail();  
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
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // anon grantee
        verify(anon, target, UserRight.RT_viewFreeBusy, true);
        verifyDefault(anon, target, UserRight.RT_invite);
        
        // null grantee
        verify(null, target, UserRight.RT_viewFreeBusy, true);
        verifyDefault(null, target, UserRight.RT_invite);
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
        aces.add(newUsrACE(user1, UserRight.RT_viewFreeBusy, true));
        aces.add(newGrpACE(groupA, UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // group member, but account is specifically denied
        verify(user1, target, UserRight.RT_viewFreeBusy, false);
        
        // group member
        verify(user2, target, UserRight.RT_viewFreeBusy, true);
        
        // not group member
        verify(user3, target, UserRight.RT_viewFreeBusy, false);
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
        aces.add(newGrpACE(G1, UserRight.RT_viewFreeBusy, false));
        aces.add(newGrpACE(G2, UserRight.RT_viewFreeBusy, true));
        aces.add(newGrpACE(G3, UserRight.RT_viewFreeBusy, false));
        aces.add(newGrpACE(G4, UserRight.RT_viewFreeBusy, true));
        aces.add(newGrpACE(G5, UserRight.RT_viewFreeBusy, false));
        aces.add(newGrpACE(G6, UserRight.RT_viewFreeBusy, true));
        PermUtil.grantAccess(prov, target, aces);
        
        verify(user1, target, UserRight.RT_viewFreeBusy, false);
    }
    
    /*
     * test target with no ACL, should return caller default regardless who is the grantee and which right to ask for
     */
    public void testNoACL() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account zimbraUser = prov.createAccount(getEmailAddr("testNoACL-user"), PASSWORD, null);
        Account guest = guestAccount("guest@external.com", "whocares");
        Account anon = anonAccount();
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testNoACL-target"), PASSWORD, null);
        
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
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account zimbraUser = prov.createAccount(getEmailAddr("testDefaultWithNonEmptyACL-user"), PASSWORD, null);
        Account guest = guestAccount("guest@external.com", "whocares");
        Account anon = anonAccount();
        
        Right rightGranted = UserRight.RT_viewFreeBusy;
        Right rightNotGranted = UserRight.RT_invite;
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testDefaultWithNonEmptyACL-target"), PASSWORD, null);
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(zimbraUser, rightGranted, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // verify callsite default is honored for not granted right
        verifyDefault(zimbraUser, target, rightNotGranted);
        verifyDefault(guest, target, rightNotGranted);
        verifyDefault(anon, target, rightNotGranted);
        
        // verify granted right is properly processed
        verify(zimbraUser, target, rightGranted, true);
        verify(guest, target, rightGranted, false);
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
        aces.add(newUsrACE(conflict, UserRight.RT_viewFreeBusy, true));
        aces.add(newUsrACE(conflict, UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
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
        aces.add(newUsrACE(duplicate, UserRight.RT_viewFreeBusy, true));
        aces.add(newUsrACE(duplicate, UserRight.RT_viewFreeBusy, true));
        PermUtil.grantAccess(prov, target, aces);
        
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
        aces.add(newUsrACE(user, UserRight.RT_viewFreeBusy, true));
        aces.add(newGrpACE(group, UserRight.RT_viewFreeBusy, true));
        PermUtil.grantAccess(prov, target, aces);
        
        // verify the grants were added
        Set<ZimbraACE> acl = PermUtil.getACEs(target, null);
        assertEquals(2, acl.size());
        
        // grant some more
        aces.clear();
        aces.add(newPubACE(UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // verify the grants were added
        acl = PermUtil.getACEs(target, null);
        assertEquals(3, acl.size());
        
        // grant some more
        aces.clear();
        aces.add(newAllACE(UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // verify the grants were added
        acl = PermUtil.getACEs(target, null);
        assertEquals(4, acl.size());
        
    }
    
    public void testLoginAsRight() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account user = prov.createAccount(getEmailAddr("testLoginAsRight-user"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testLoginAsRight-target"), PASSWORD, null);
        
        // grant some permissions 
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(user, UserRight.RT_loginAs, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // verify the grant was added
        Set<ZimbraACE> acl = PermUtil.getACEs(target, null);
        assertEquals(1, acl.size());
        verify(user, target, UserRight.RT_loginAs, true);
        
        // verify user can access target's account
        boolean canAccessAccount = mAM.canAccessAccount(user, target);
        assertTrue(canAccessAccount);
        
    }
    
    public void testRevoke() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        
        /*
         * setup grantees
         */
        Account user = prov.createAccount(getEmailAddr("testRevoke-user"), PASSWORD, null);
        
        /*
         * setup targets
         */
        Account target = prov.createAccount(getEmailAddr("testRevoke-target"), PASSWORD, null);
        
        // grant some permissions 
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(user, UserRight.RT_invite, false));
        aces.add(newUsrACE(user, UserRight.RT_viewFreeBusy, false));
        PermUtil.grantAccess(prov, target, aces);
        
        // verify the grant was added
        Set<ZimbraACE> acl = PermUtil.getACEs(target, null);
        assertEquals(2, acl.size());
        verify(user, target, UserRight.RT_invite, true);
        
        // revoke one right
        Set<ZimbraACE> acesToRevoke = new HashSet<ZimbraACE>();
        acesToRevoke.add(newUsrACE(user, UserRight.RT_invite, false));
        PermUtil.revokeAccess(prov, target, acesToRevoke);
        
        // verify the grant was removed
        acl = PermUtil.getACEs(target, null);
        assertEquals(1, acl.size());
        verifyDefault(user, target, UserRight.RT_invite); // callsite default should now apply
        
        // verify the other right is still there
        verify(user, target, UserRight.RT_viewFreeBusy, true);
        
        // revoke the other right
        acesToRevoke = new HashSet<ZimbraACE>();
        acesToRevoke.add(newUsrACE(user, UserRight.RT_viewFreeBusy, false));
        PermUtil.revokeAccess(prov, target, acesToRevoke);
        
        // verify all right are gone
        verifyDefault(user, target, UserRight.RT_invite);
        verifyDefault(user, target, UserRight.RT_viewFreeBusy);
        acl = PermUtil.getACEs(target, null);
        assertNull(acl);

        // revoke non-existing right, make sure we don't crash
        acesToRevoke = new HashSet<ZimbraACE>();
        acesToRevoke.add(newUsrACE(user, UserRight.RT_invite, false));
        PermUtil.revokeAccess(prov, target, acesToRevoke);
    }


    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");

        TestUtil.runTest(TestACL.class);
    }
}
