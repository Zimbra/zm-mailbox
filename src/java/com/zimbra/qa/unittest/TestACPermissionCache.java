package com.zimbra.qa.unittest;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.soap.type.TargetBy;

public class TestACPermissionCache extends TestAC {

    protected static final AccessManager accessMgr = AccessManager.getInstance();
    
    private static final Right A_USER_RIGHT = TestAC.USER_RIGHT;
    private static final Right A_USER_RIGHT_DISTRIBUTION_LIST = TestAC.USER_RIGHT_DISTRIBUTION_LIST;
    private static final Right A_CACHEABLE_ADMIN_RIGHT = Admin.R_adminLoginAs;  // the only cached admin right
    
    public void tearDown() throws Exception {
        deleteAllEntries();
    }
    
    static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }
    
    private void grantRight(TargetType targetType, Entry target, 
            GranteeType granteeType, NamedEntry grantee, Right right) 
    throws ServiceException {
        grantRight(targetType, target, granteeType, grantee, null, right);
    }
    
    private void revokeRight(TargetType targetType, Entry target, 
            GranteeType granteeType, NamedEntry grantee, Right right) 
    throws ServiceException {
        RightCommand.revokeRight(
                mProv, getGlobalAdminAcct(),
                targetType.getCode(), TargetBy.name, target.getLabel(),
                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), 
                right.getName(), null);
    }
    
    // takes a secret
    private void grantRight(TargetType targetType, Entry target, 
            GranteeType granteeType, NamedEntry grantee, String secret, Right right) 
    throws ServiceException {
        RightCommand.grantRight(
                mProv, getGlobalAdminAcct(),
                targetType.getCode(), TargetBy.name, target.getLabel(),
                granteeType.getCode(), Key.GranteeBy.name, grantee.getName(), secret,
                right.getName(), null);
    }
    
    // grant target
    private static final String GRANTTARGET_USER_ACCT = "granttarget-user-acct";
    private static final String GRANTTARGET_USER_GROUP = "granttarget-user-group";
    
    // subgroup of a group grant target
    private static final String SUBGROUP_OF_GRANTTARGET_USER_GROUP = "subgroup-of-granttarget-user-group";
    
    // actual target
    private static final String TARGET_USER_ACCT = "target-user-acct";
    private static final String TARGET_USER_GROUP = "target-user-group";
    
    // grantee user account
    private static final String GRANTEE_USER_ACCT = "grantee-user-acct";
    private static final String GRANTEE_USER_GROUP = "grantee-user-group";
    private static final String GRANTEE_ADMIN_ACCT = "grantee-admin-acct";
    private static final String GRANTEE_ADMIN_GROUP = "grantee-admin-group";
    private static final String GRANTEE_GUEST_ACCT = "grantee-guest-acct";
    private static final String GRANTEE_GUEST_ACCT_PASSWORD = "grantee-guest-acct-password";
    
    /*
     * =================
     * target side test
     * =================
     */
    @Test
    public void testGuestAccount() throws Exception {
        Right right = A_USER_RIGHT;
        
        Domain domain = createDomain();
        Account grantTarget = createUserAccount(GRANTTARGET_USER_ACCT, domain);
        Account target = grantTarget;
        Account grantee = createGuestAccount(GRANTEE_GUEST_ACCT, GRANTEE_GUEST_ACCT_PASSWORD);
        Account notGrantee = createGuestAccount(GRANTEE_USER_ACCT + "not", GRANTEE_GUEST_ACCT_PASSWORD);
        
        boolean allow;
        
        grantRight(TargetType.account, grantTarget, GranteeType.GT_GUEST, grantee, GRANTEE_GUEST_ACCT_PASSWORD, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
        allow = accessMgr.canDo(notGrantee, target, right, false, null);
        assertFalse(allow); 
    }
    
    @Test
    public void testGrantChangeOnTarget() throws Exception {
        Right right = A_USER_RIGHT;
        
        Domain domain = createDomain();
        Account grantTarget = createUserAccount(GRANTTARGET_USER_ACCT, domain);
        Account target = grantTarget;
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        boolean allow;
        
        grantRight(TargetType.account, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        revokeRight(TargetType.account, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        grantRight(TargetType.account, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void testGrantChangeOnDirectlyInheritedDistributionList() throws Exception {
        Right right = A_USER_RIGHT_DISTRIBUTION_LIST;
        
        Domain domain = createDomain();
        DistributionList grantTarget = createUserDistributionList(GRANTTARGET_USER_GROUP, domain);
        DistributionList target = createUserDistributionList(TARGET_USER_GROUP, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        mProv.addMembers(grantTarget, new String[]{target.getName()});
        
        boolean allow;
        
        grantRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        revokeRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        grantRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void testGrantChangeOnIndirectlyInheritedDistributionList() throws Exception {
        Right right = A_USER_RIGHT_DISTRIBUTION_LIST;
        
        Domain domain = createDomain();
        DistributionList grantTarget = createUserDistributionList(GRANTTARGET_USER_GROUP, domain);
        DistributionList subGroup = createUserDistributionList(SUBGROUP_OF_GRANTTARGET_USER_GROUP, domain);
        DistributionList target = createUserDistributionList(TARGET_USER_GROUP, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        mProv.addMembers(grantTarget, new String[]{subGroup.getName()});
        mProv.addMembers(subGroup, new String[]{target.getName()});
        
        boolean allow;
        
        grantRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        revokeRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        grantRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void testGrantChangeOnDomain() throws Exception {
        Right right = A_USER_RIGHT;
        
        Domain domain = createDomain();
        Domain grantTarget = domain;
        Account target = createUserAccount(TARGET_USER_ACCT, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);

        boolean allow;
        
        grantRight(TargetType.domain, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        revokeRight(TargetType.domain, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        grantRight(TargetType.domain, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void testGrantChangeOnGlobalGrant() throws Exception {
        Right right = A_USER_RIGHT;
        
        Domain domain = createDomain();
        GlobalGrant grantTarget = mProv.getGlobalGrant();
        Account target = createUserAccount(TARGET_USER_ACCT, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);

        boolean allow;
        
        grantRight(TargetType.global, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        revokeRight(TargetType.global, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        grantRight(TargetType.global, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void testDirectGroupMembershipChanged() throws Exception {
        Right right = A_USER_RIGHT_DISTRIBUTION_LIST;
        
        Domain domain = createDomain();
        DistributionList grantTarget = createUserDistributionList(GRANTTARGET_USER_GROUP, domain);
        DistributionList target = createUserDistributionList(TARGET_USER_GROUP, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        mProv.addMembers(grantTarget, new String[]{target.getName()});
        
        boolean allow;
        
        grantRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        mProv.removeMembers(grantTarget, new String[]{target.getName()});
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        mProv.addMembers(grantTarget, new String[]{target.getName()});
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void testIndirectGroupMembershipChanged() throws Exception {
        Right right = A_USER_RIGHT_DISTRIBUTION_LIST;
        
        Domain domain = createDomain();
        DistributionList grantTarget = createUserDistributionList(GRANTTARGET_USER_GROUP, domain);
        DistributionList subGroup = createUserDistributionList(SUBGROUP_OF_GRANTTARGET_USER_GROUP, domain);
        DistributionList target = createUserDistributionList(TARGET_USER_GROUP, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        mProv.addMembers(grantTarget, new String[]{subGroup.getName()});
        mProv.addMembers(subGroup, new String[]{target.getName()});
        
        boolean allow;
        
        grantRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        // this test won't work because although the permission cache is cleared,
        // the upward groups are still cached on the account, it has been the 
        // behavior predates the permission cache enhancement 
        // mProv.removeMembers(grantTarget, new String[]{subGroup.getName()});
        // allow = accessMgr.canDo(grantee, target, right, false, null);
        // assertFalse(allow);
        
        // this works
        mProv.removeMembers(subGroup, new String[]{target.getName()});
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        mProv.addMembers(subGroup, new String[]{target.getName()});
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void testDomainOfTargetChanged() throws Exception {
        Right right = A_USER_RIGHT;
        
        Domain domain = createDomain();
        Domain grantTarget = domain;
        Account target = createUserAccount(TARGET_USER_ACCT, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        boolean allow;
        
        grantRight(TargetType.domain, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        Domain newDomain = createDomain();
        String id = target.getId();
        String oldName = target.getName();
        String newName = getEmailLocalpart(target.getName()) + "@" + newDomain.getName();
        mProv.renameAccount(id, newName);
        target = mProv.get(Key.AccountBy.id, id);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertFalse(allow); 
        
        mProv.renameAccount(id, oldName);
        target = mProv.get(Key.AccountBy.id, id);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow); 
    }
    
    @Test
    public void GrantTargetDeleted() throws Exception {
        Right right = A_USER_RIGHT_DISTRIBUTION_LIST;
        
        Domain domain = createDomain();
        DistributionList grantTarget = createUserDistributionList(GRANTTARGET_USER_GROUP, domain);
        DistributionList subGroup = createUserDistributionList(SUBGROUP_OF_GRANTTARGET_USER_GROUP, domain);
        DistributionList target = createUserDistributionList(TARGET_USER_GROUP, domain);
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        mProv.addMembers(grantTarget, new String[]{subGroup.getName()});
        mProv.addMembers(subGroup, new String[]{target.getName()});
        
        boolean allow;
        
        grantRight(TargetType.dl, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, false, null);
        assertTrue(allow);  
        
        // this test won't work because although the permission cache is cleared,
        // the upward groups are still cached on the account, it has been the 
        // behavior predates the permission cache enhancement 
        // mProv.deleteDistributionList(grantTarget.getId());
        // allow = accessMgr.canDo(grantee, target, right, false, null);
        // assertFalse(allow); 
    }

    
    /*
     * =================
     * grantee side test
     * =================
     */
    @Test
    public void testGranteeGroupMembershipChanged() throws Exception {  
        Right right = A_USER_RIGHT;
        
        Domain domain = createDomain();
        Account grantTarget = createUserAccount(GRANTTARGET_USER_ACCT, domain);
        Account target = grantTarget;
        DistributionList grantee = createUserDistributionList(GRANTEE_USER_GROUP, domain);
        Account account =  createUserAccount(GRANTEE_USER_ACCT, domain);
        
        mProv.addMembers(grantee, new String[]{account.getName()});
        
        boolean allow;
        
        grantRight(TargetType.account, grantTarget, GranteeType.GT_GROUP, grantee, right);
        allow = accessMgr.canDo(account, target, right, false, null);
        assertTrue(allow);  
        
        mProv.removeMembers(grantee, new String[]{account.getName()});
        allow = accessMgr.canDo(account, target, right, false, null);
        assertFalse(allow); 
    }
    
    @Test
    public void testGranteeAdminFlagChanged() throws Exception {
        Right right = A_CACHEABLE_ADMIN_RIGHT;
        
        Domain domain = createDomain();
        Account grantTarget = createUserAccount(GRANTTARGET_USER_ACCT, domain);
        Account target = grantTarget;
        Account grantee = createDelegatedAdminAccount(GRANTEE_ADMIN_ACCT, domain);
        
        boolean allow;
        
        grantRight(TargetType.account, grantTarget, GranteeType.GT_USER, grantee, right);
        allow = accessMgr.canDo(grantee, target, right, true, null);
        assertTrue(allow);  
        
        grantee.setIsDelegatedAdminAccount(false);
        try {
            allow = accessMgr.canDo(grantee, target, right, true, null); 
        } catch (ServiceException e) {
            if (ServiceException.PERM_DENIED.equals(e.getCode()))
                allow = false;
        }
        assertFalse(allow); 
        
        grantee.setIsDelegatedAdminAccount(true);
        allow = accessMgr.canDo(grantee, target, right, true, null);
        assertTrue(allow); 
    }

    
    /*
    private void doTestPerf(boolean cache, boolean expectedResult, Account grantee, Entry target, Right right) throws Exception {
        int numIters = 10000000;
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numIters; i++) {
            if (!cache)
                PermCacheManager.getInstance().invalidateCache();
            boolean allow = accessMgr.canDo(grantee, target, right, false, null);
            assertEquals(expectedResult, allow); 
            
            if ((i + 1) % 1000000 == 0)
                System.out.println("Cache " + cache + " " + (i + 1));
        }
        long endTime = System.currentTimeMillis();
        
        long elapsedTimeSecs = (endTime - startTime)/1000;
        
        System.out.println("Cache " + cache + " => elapsed time = " + elapsedTimeSecs + " secs");
    }

    
    public void testPerf() throws Exception {
        Right right = A_USER_RIGHT;
        
        Domain domain = createDomain();
        Account grantTarget = createUserAccount(GRANTTARGET_USER_ACCT, domain);
        Account target = grantTarget;
        Account grantee = createUserAccount(GRANTEE_USER_ACCT, domain);
        
        // grantRight(TargetType.account, grantTarget, GranteeType.GT_USER, grantee, right);
        
        boolean expectedResult = false;
        // doTestPerf(true, expectedResult, grantee, target, right);
        // doTestPerf(false, expectedResult, grantee, target, right);
    }
    */
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");
        
        TestUtil.runTest(TestACPermissionCache.class);
    }
    
}
