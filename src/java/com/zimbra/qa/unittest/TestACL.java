package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

import junit.framework.TestCase;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.service.AuthProvider;

public abstract class TestACL extends TestCase {
    
    // private static final AclAccessManager mAM = new AclAccessManager();
    protected static final AccessManager mAM = AccessManager.getInstance();
    protected static final Provisioning mProv = Provisioning.getInstance();
    protected static final String TEST_ID = TestProvisioningUtil.genTestId();
    protected static final String DOMAIN_NAME = TestProvisioningUtil.baseDomainName("test-ACL", TEST_ID);
    protected static final String PASSWORD = "test123";
    
    static {
        try {
            // create a domain
            Domain domain = mProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail();
        }
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
        return testCaseName + "-" + localPartPostfix + "@" + DOMAIN_NAME;
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
    
    // construct a ACE with "pub" grantee type
    protected ZimbraACE newPubACE(Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(ACL.GUID_PUBLIC, GranteeType.GT_PUBLIC, right, allowDeny.deny(), null);
    }
    
    // construct a ACE with "all" grantee type
    protected ZimbraACE newAllACE(Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(ACL.GUID_AUTHUSER, GranteeType.GT_AUTHUSER, right, allowDeny.deny(), null);
    }
    
    // construct a ACE with "usr" grantee type
    protected ZimbraACE newUsrACE(Account acct, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(acct.getId(), GranteeType.GT_USER, right, allowDeny.deny(), null);
    }
    
    // construct a ACE with "grp" grantee type
    protected ZimbraACE newGrpACE(DistributionList dl, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(dl.getId(), GranteeType.GT_GROUP, right, allowDeny.deny(), null);
    }
    
    // construct a ACE with "key" grantee type
    protected ZimbraACE newKeyACE(String nameOrEmail, String accessKey, Right right, AllowOrDeny allowDeny) throws ServiceException {
        return new ZimbraACE(nameOrEmail, GranteeType.GT_KEY, right, allowDeny.deny(), accessKey);
    }

    /*
     * verify we always get the expected result, regardless what the default value is
     * This test does NOT use the admin privileges 
     * 
     * This is for testing target entry with some ACL.
     */
    protected void verify(Account grantee, Entry target, Right right, AllowOrDeny expected) throws Exception {
        verify(grantee, target, right, AS_USER, ALLOW, expected);
        verify(grantee, target, right, AS_USER, DENY, expected);
    }
    
    /*
     * verify we always get the expected result, regardless what the default value is
     * 
     * This is for testing target entry with some ACL.
     */
    protected void verify(Account grantee, Entry target, Right right, AsAdmin asAdmin, AllowOrDeny expected) throws Exception {
        // 1. pass allow as the default value, result should not be affected by the default value
        verify(grantee, target, right, asAdmin, ALLOW, expected);
        
        // 2. pass deny as the default value, result should not be affected by the default value
        verify(grantee, target, right, asAdmin, DENY, expected);
    }
    
    /*
     * verify that the result IS the default value
     * 
     * This is for testing target entry without any ACL.
     */
    protected void verifyDefault(Account grantee, Entry target, Right right) throws Exception {
        AsAdmin asAdmin = AS_USER; // TODO: test admin case
        
        // 1. pass true as the default value, result should be true
        verify(grantee, target, right, asAdmin, ALLOW, ALLOW);
            
        // 2. pass false as the default value, result should be false
        verify(grantee, target, right, asAdmin, DENY, DENY);
    }
    
    /*
     * verify expected result
     */
    protected void verify(Account grantee, Entry target, Right right, AsAdmin asAdmin, AllowOrDeny defaultValue, AllowOrDeny expected) throws Exception {
        boolean result;
        
        // Account interface
        result = mAM.canPerform(grantee==null?null:grantee, target, right, asAdmin.yes(), defaultValue.allow());
        assertEquals(expected.allow(), result);
        
        // AuthToken interface
        result = mAM.canPerform(grantee==null?null:AuthProvider.getAuthToken(grantee), target, right, asAdmin.yes(), defaultValue.allow());
        assertEquals(expected.allow(), result);
        
        // String interface
        result = mAM.canPerform(grantee==null?null:grantee.getName(), target, right, asAdmin.yes(), defaultValue.allow());
        if (grantee instanceof ACL.GuestAccount && ((ACL.GuestAccount)grantee).getAccessKey() != null) {
            // string interface always return denied for key grantee unless there is a pub grant
            // skip the test for now, unless we want to pass yet another parameter to this method
            // i.e. - if no pub grant: should always expect false
            //      - if there is a pub grant: should expect the expected
            return;
        }
        assertEquals(expected.allow(), result);
    }
    
       
    /*
     * utility methods to grant/revoke right
     * 
     * To simulate how grants are done in the real server/zmprov, we first call TargetType.lookupTarget to 
     * "look for" the taret, then use the returned entry instead of giving the target entry passed in 
     * directly to PermUtil.
     * 
     */
    protected Set<ZimbraACE> grantRight(TargetType targetType, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        String targetId = (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
        Entry targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.id, targetId);
        return PermUtil.grantRight(mProv, targetEntry, aces);
    }
        
    protected Set<ZimbraACE> revokeRight(TargetType targetType, Entry target, Set<ZimbraACE> aces) throws ServiceException {
        // call TargetType.lookupTarget instead of passing the target entry directly for two reasons:
        // 1. to simulate how grants are done in the real server/zmprov
        // 2. convert DistributionList to AclGroup
        String targetId = (target instanceof NamedEntry)? ((NamedEntry)target).getId() : null;
        Entry targetEntry = TargetType.lookupTarget(mProv, targetType, TargetBy.id, targetId);
        return PermUtil.revokeRight(mProv, targetEntry, aces);
    }

   
}
