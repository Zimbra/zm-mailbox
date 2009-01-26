package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.CliUtil;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightChecker;
import com.zimbra.cs.account.accesscontrol.RightChecker.AllowedAttrs;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.RightUtil;
import com.zimbra.cs.account.accesscontrol.RoleAccessManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.service.AuthProvider;


public abstract class TestACL extends TestCase {
    
    protected static boolean CHECK_LIMIT = false;  // todo: remove this and all limit related code after all tests pass. 
    
    // private static final AclAccessManager mAM = new AclAccessManager();
    protected static final AccessManager mAM = AccessManager.getInstance();
    protected static final Provisioning mProv = Provisioning.getInstance();
    protected static final String TEST_ID = TestProvisioningUtil.genTestId();
    protected static final String DOMAIN_NAME = TestProvisioningUtil.baseDomainName("test-ACL", TEST_ID);
    protected static final String PASSWORD = "test123";
    
    // user right
    protected static final Right USER_RIGHT = UserRight.R_viewFreeBusy;
    
    // account right
    protected static Right ADMIN_RIGHT_ACCOUNT;
    protected static Right ADMIN_RIGHT_CALENDAR_RESOURCE;
    protected static Right ADMIN_RIGHT_CONFIG;
    protected static Right ADMIN_RIGHT_COS;
    protected static Right ADMIN_RIGHT_DISTRIBUTION_LIST;
    protected static Right ADMIN_RIGHT_DOMAIN;
    protected static Right ADMIN_RIGHT_GLOBALGRANT;
    protected static Right ADMIN_RIGHT_SERVER;
    protected static Right ADMIN_RIGHT_ZIMLET;
    
    static {
        
        System.out.println();
        System.out.println("AccessManager: " + mAM.getClass().getName());
        System.out.println();
        
        try {
            // create a domain
            Domain domain = mProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
            
            // setup rights
            ADMIN_RIGHT_ACCOUNT           = getRight("test-preset-account");
            ADMIN_RIGHT_CALENDAR_RESOURCE = getRight("test-preset-calendarresource");
            ADMIN_RIGHT_CONFIG            = getRight("test-preset-globalconfig");
            ADMIN_RIGHT_COS               = getRight("test-preset-cos");
            ADMIN_RIGHT_DISTRIBUTION_LIST = getRight("test-preset-distributionlist");
            ADMIN_RIGHT_DOMAIN            = getRight("test-preset-domain");
            ADMIN_RIGHT_GLOBALGRANT       = getRight("test-preset-globalgrant");
            ADMIN_RIGHT_SERVER            = getRight("test-preset-server");
            ADMIN_RIGHT_ZIMLET            = getRight("test-preset-zimlet");
            
        } catch (ServiceException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    String getTestName() {
        return getName().substring(4);
    }
    
    /*
     * for testing key grantees
     */
    protected class KeyAuthToken extends AuthToken {

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
        public void encode(HttpServletResponse resp, boolean isAdminReq, boolean secureCookie)
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
    
    
    /*
     * ======================
     * ======================
     *     Util Methods
     * ======================
     * ======================
     */
    
    protected static String getEmailAddr(String localPart) {
        return localPart + "@" + DOMAIN_NAME;
    }
    
    protected static String getEmailAddr(String testCaseName, String localPartPostfix) {
        if (testCaseName == null)
            return localPartPostfix + "@" + DOMAIN_NAME;
        else
            return testCaseName + "-" + localPartPostfix + "@" + DOMAIN_NAME;
    }
    
    protected static String getSubDomainName(String testCaseName) {
        return testCaseName + "." + DOMAIN_NAME;
    }
    
    protected Account guestAccount(String email, String password) {
        return new ACL.GuestAccount(email, password);
    }
    
    protected Account keyAccount(String name, String accesKey) {
        AuthToken authToken = new KeyAuthToken(name, accesKey);
        return new ACL.GuestAccount(authToken);
    }
    
    protected Account anonAccount() {
        return ACL.ANONYMOUS_ACCT;
    }
    
    protected Account createAdminAccount(String email)throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, Provisioning.TRUE);
        return mProv.createAccount(email, PASSWORD, attrs);
    }
    
    /*
     * convenient notions so callsites don't have to deal with many various boolean values 
     * when passing args to the utility methods, also callsites code are more readable.
     */
    protected static enum AllowOrDeny {
        ALLOW(true),
        DENY(false);
        
        boolean mAllow;
        
        AllowOrDeny(boolean allow) {
            mAllow = allow;
        }
        
        boolean deny() {
            return !mAllow;
        }
        
        boolean allow() {
            return mAllow;
        }
        
        // TODO add support for RM_CAN_DELEGATE
        RightModifier toRightModifier() {
            if (deny())
                return RightModifier.RM_DENY;
            else
                return null;
        }
    }
    
    // shorthand notion so we don't have to refer to AllowOrDeny from callsites
    protected static final AllowOrDeny ALLOW = AllowOrDeny.ALLOW;
    protected static final AllowOrDeny DENY = AllowOrDeny.DENY;
    
    protected static enum AsAdmin {
        AS_ADMIN(true),
        AS_USER(false);
        
        boolean mAsAdmin;
        
        AsAdmin(boolean asAdmin) {
            mAsAdmin = asAdmin;
        }
        
        boolean yes()  {
            return mAsAdmin;
        }
    }
    // shorthand notion so we don't have to refer to AllowOrDeny from callsites
    protected static final AsAdmin AS_ADMIN = AsAdmin.AS_ADMIN;
    protected static final AsAdmin AS_USER = AsAdmin.AS_USER;
    
    
    protected static enum LimitOrNoLimit {
        LIMIT(true),
        NOLIMIT(false),
        NULLLIMIT(false); // for tests in that limit doesn't matter
        
        boolean mLimit;
        
        LimitOrNoLimit(boolean limit) {
            mLimit = limit;
        }
        
        boolean limit() {
            // master key to turn of limit checking
            if (!CHECK_LIMIT)
                return false;
            
            // should never be called for NULLLIMIT
            if (this == NULLLIMIT)
                fail();
            
            return mLimit;
        }
    }
    protected static final LimitOrNoLimit LIMIT = LimitOrNoLimit.LIMIT;
    protected static final LimitOrNoLimit NOLIMIT = LimitOrNoLimit.NOLIMIT;
    protected static final LimitOrNoLimit NULLLIMIT = LimitOrNoLimit.NULLLIMIT;
    
    protected static enum GetOrSet {
        GET(true),
        SET(false);
        
        boolean mGet;
        
        GetOrSet(boolean get) {
            mGet = get;
        }
        
        boolean isGet() {
            return mGet;
        }
    }
    protected static final GetOrSet GET = GetOrSet.GET;
    protected static final GetOrSet SET = GetOrSet.SET;
    
    // construct a ACE with "pub" grantee type
    protected ZimbraACE newPubACE(Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(ACL.GUID_PUBLIC, GranteeType.GT_PUBLIC, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "all" authuser grantee type
    protected ZimbraACE newAllACE(Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(ACL.GUID_AUTHUSER, GranteeType.GT_AUTHUSER, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "usr" grantee type
    protected ZimbraACE newUsrACE(Account acct, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(acct.getId(), GranteeType.GT_USER, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "grp" grantee type
    protected ZimbraACE newGrpACE(DistributionList dl, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(dl.getId(), GranteeType.GT_GROUP, right, allowDeny.toRightModifier(), null);
    }
    
    // construct a ACE with "key" grantee type
    protected ZimbraACE newKeyACE(String nameOrEmail, String accessKey, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(nameOrEmail, GranteeType.GT_KEY, right, allowDeny.toRightModifier(), accessKey);
    }
    
    Set<ZimbraACE> makeUsrGrant(Account grantee, Right right, AllowOrDeny alloworDeny) throws ServiceException {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newUsrACE(grantee, right, alloworDeny));
        return aces;
    }
    
    Set<ZimbraACE> makeGrpGrant(DistributionList grantee, Right right, AllowOrDeny alloworDeny) throws ServiceException {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(newGrpACE(grantee, right, alloworDeny));
        return aces;
    }
    
    
    // another shorthand so we don't have to remember true/false.
    static final boolean POSITIVE = false;
    static final boolean NEGATIVE = true;
    
    static class TestViaGrant extends ViaGrant {
        String mTargetType;
        String mTargetName;
        String mGranteeType;
        String mGranteeName;
        String mRight;
        boolean mIsNegativeGrant;
        
        Set<TestViaGrant> mCanAlsoVia;
        
        TestViaGrant(TargetType targetType,
                     Entry target,
                     GranteeType granteeType,
                     String granteeName,
                     Right right,
                     boolean isNegativeGrant) {
            mTargetType = targetType.getCode();
            mTargetName = target.getLabel();
            mGranteeType = granteeType.getCode();
            mGranteeName = granteeName;
            mRight = right.getName();
            mIsNegativeGrant = isNegativeGrant;
        }
        
        public String getTargetType() { 
            return mTargetType;
        } 
        
        public String getTargetName() {
            return mTargetName;
        }
        
        public String getGranteeType() {
            return mGranteeType;
        }
        
        public String getGranteeName() {
            return mGranteeName;
        }
        
        public String getRight() {
            return mRight;
        }
        
        public boolean isNegativeGrant() {
            return mIsNegativeGrant;
        }
        
        public void addCanAlsoVia(TestViaGrant canAlsoVia) {
            if (mCanAlsoVia == null)
                mCanAlsoVia = new HashSet<TestViaGrant>();
            mCanAlsoVia.add(canAlsoVia);
        }
        
        public void verify(ViaGrant actual) {
            try {
                assertEquals(getTargetType(),   actual.getTargetType());
                assertEquals(getTargetName(),   actual.getTargetName());
                assertEquals(getGranteeType(),  actual.getGranteeType());
                assertEquals(getGranteeName(),  actual.getGranteeName());
                assertEquals(getRight(),        actual.getRight());
                assertEquals(isNegativeGrant(), actual.isNegativeGrant());
            } catch (AssertionFailedError e) {
                if (mCanAlsoVia == null)
                    throw e;
                
                // see if any canAlsoVia matches
                for (TestViaGrant canAlsoVia : mCanAlsoVia) {
                    try {
                        canAlsoVia.verify(actual);
                        // good, at least one of the canAlsoVia matches
                        return;
                    } catch (AssertionFailedError     eAlso) {
                        // ignore, see if next one matches
                    }
                }
                // if we get here, none of the canAlsoVia matches
                // throw the assertion exception on the main via
                throw e;
            }
        }
    }
    
    static class AuthUserViaGrant extends TestViaGrant {
        AuthUserViaGrant(TargetType targetType,
                         Entry target,
                         Right right,
                         boolean isNegativeGrant) {
            super(targetType, target, GranteeType.GT_AUTHUSER, null, right, isNegativeGrant);
        }
    }
    
    static class PubViaGrant extends TestViaGrant {
        PubViaGrant(TargetType targetType,
                    Entry target,
                    Right right,
                    boolean isNegativeGrant) {
            super(targetType, target, GranteeType.GT_PUBLIC, null, right, isNegativeGrant);
        }
    }

    static class TodoViaGrant extends ViaGrant {

    }

    /*
     * verify we always get the expected result, regardless what the default value is
     * This test does NOT use the admin privileges 
     * 
     * This is for testing target entry with some ACL.
     */
    protected void verify(Account grantee, Entry target, Right right, AllowOrDeny expected, ViaGrant expectedVia) throws Exception {
        verify(grantee, target, right, AS_USER, ALLOW, expected, expectedVia);
        verify(grantee, target, right, AS_USER, DENY, expected, expectedVia);
    }
    
    /*
     * verify we always get the expected result, regardless what the default value is
     * 
     * This is for testing target entry with some ACL.
     */
    protected void verify(Account grantee, Entry target, Right right, AsAdmin asAdmin, AllowOrDeny expected, ViaGrant expectedVia) throws Exception {
        // 1. pass allow as the default value, result should not be affected by the default value
        verify(grantee, target, right, asAdmin, ALLOW, expected, expectedVia);
        
        // 2. pass deny as the default value, result should not be affected by the default value
        verify(grantee, target, right, asAdmin, DENY, expected, expectedVia);
    }
    
    /*
     * verify that the result IS the default value
     * 
     * This is for testing target entry without any ACL.
     */
    protected void verifyDefault(Account grantee, Entry target, Right right) throws Exception {
        AsAdmin asAdmin = AS_USER; // TODO: test admin case
        
        // 1. pass true as the default value, result should be true
        verify(grantee, target, right, asAdmin, ALLOW, ALLOW, null);
            
        // 2. pass false as the default value, result should be false
        verify(grantee, target, right, asAdmin, DENY, DENY, null);
    }
    
    void assertEquals(ViaGrant expected, ViaGrant actual) {
        
        if (expected == null && actual == null)
            return;
        
        if (!(AccessManager.getInstance() instanceof RoleAccessManager))
            return;
        
        if (expected instanceof TodoViaGrant)
            return; // TODO
        
        ((TestViaGrant)expected).verify(actual);
    }
    
    /*
     * verify expected result
     */
    protected void verify(Account grantee, Entry target, Right right, AsAdmin asAdmin, AllowOrDeny defaultValue, AllowOrDeny expected, ViaGrant expectedVia) throws Exception {
        boolean result;
        
        // Account interface
        ViaGrant via = (expectedVia==null)?null:new ViaGrant();
        result = mAM.canDo(grantee==null?null:grantee, target, right, asAdmin.yes(), defaultValue.allow(), via);
        assertEquals(expected.allow(), result);
        assertEquals(expectedVia, via);
        
        // AuthToken interface
        via = (expectedVia==null)?null:new ViaGrant();
        result = mAM.canDo(grantee==null?null:AuthProvider.getAuthToken(grantee), target, right, asAdmin.yes(), defaultValue.allow(), via);
        assertEquals(expected.allow(), result);
        assertEquals(expectedVia, via);
        
        // String interface
        via = (expectedVia==null)?null:new ViaGrant();
        result = mAM.canDo(grantee==null?null:grantee.getName(), target, right, asAdmin.yes(), defaultValue.allow(), via);
        if (grantee instanceof ACL.GuestAccount && ((ACL.GuestAccount)grantee).getAccessKey() != null) {
            // string interface always return denied for key grantee unless there is a pub grant
            // skip the test for now, unless we want to pass yet another parameter to this method
            // i.e. - if no pub grant: should always expect false
            //      - if there is a pub grant: should expect the expected
            return;
        }
        assertEquals(expected.allow(), result);
        assertEquals(expectedVia, via);
    }
    
    void assertEquals(Set<String> expected, Set<String> actual) {
        assertEquals(expected.size(), actual.size());
        for (String s: expected)
            assertTrue(actual.contains(s));
    }
    
    void assertEquals(AllowedAttrs expected, AllowedAttrs actual) {
        assertEquals(expected.getResult(), actual.getResult());
        if (actual.getResult() == AllowedAttrs.Result.ALLOW_SOME) {
            assertEquals(expected.getAllowed(), actual.getAllowed());
        }
    }
    
    protected void verify(Account grantee, Entry target, GetOrSet getOrSet, AllowedAttrs expected) {
        try {
            // call RightChecker directly instead of mAM, we want to verify the interim result.
            AllowedAttrs allowedAttrs = getOrSet.isGet() ? 
                                            RightChecker.canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_GET_ATTRS, false):
                                            RightChecker.canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_SET_ATTRS, false);
            // System.out.println("========== Test result ==========\n" + allowedAttrs.dump());
            assertEquals(expected, allowedAttrs);
        } catch (ServiceException e) {
            fail();
        }
    }
        
    protected void verify(Account grantee, Entry target, Right right, Map<String, Object> attrs, AllowOrDeny expected) throws ServiceException {
        boolean actual = mAM.canPerform(grantee, target, right, false, attrs, null);
        assertEquals(expected.allow(), actual);
    }
    
       
    /*
     * utility methods to grant/revoke right
     * 
     * To simulate how grants are done in the real server/zmprov, we first call TargetType.lookupTarget to 
     * "look for" the taret, then use the returned entry instead of giving the target entry passed in 
     * directly to RightUtil.
     * 
     */
    protected List<ZimbraACE> grantRight(TargetType targetType, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        Entry targetEntry;
        if (target instanceof Zimlet) {
            // must be by name
            String targetName = ((Zimlet)target).getName();
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.name, targetName);
        } else {
            String targetId = (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.id, targetId);
        }
        return RightUtil.grantRight(mProv, targetEntry, aces);
    }
        
    protected List<ZimbraACE> revokeRight(TargetType targetType, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        // call TargetType.lookupTarget instead of passing the target entry directly for two reasons:
        // 1. to simulate how grants are done in the real server/zmprov
        // 2. convert DistributionList to AclGroup
        Entry targetEntry;
        if (target instanceof Zimlet) {
            // must be by name
            String targetName = ((Zimlet)target).getName();
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.name, targetName);
        } else {
        String targetId = (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
            targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.id, targetId);
        }
        return RightUtil.revokeRight(mProv, targetEntry, aces);
    }
    
    static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }
    
    
/*
  <key name="zimbra_class_accessmanager">
    <value>com.zimbra.cs.account.accesscontrol.RoleAccessManager</value>
  </key>
*/
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
        
        // run all ACL tests
        TestUtil.runTest(TestACLGrantee.class);
        TestUtil.runTest(TestACLTarget.class);
        TestUtil.runTest(TestACLPrecedence.class);
        
        if (mAM instanceof RoleAccessManager) {
            TestUtil.runTest(TestACLAttrRight.class);
            TestUtil.runTest(TestACLRight.class);
        }
    }

}
