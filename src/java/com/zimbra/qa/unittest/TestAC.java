package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.GranteeFlag;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand.ACE;
import com.zimbra.cs.account.accesscontrol.RightCommand.Grants;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.mailbox.ACL;

public class TestAC extends TestCase {
    
    protected static final AccessManager sAM = AccessManager.getInstance();
    protected static final Provisioning sProv = Provisioning.getInstance();
    protected static final String TEST_ID = TestProvisioningUtil.genTestId();
    protected static final String BASE_DOMAIN_NAME = TestProvisioningUtil.baseDomainName("test-ACL", TEST_ID);
    protected static final String PASSWORD = "test123";
    
    protected static final Right USER_RIGHT = User.R_loginAs;
    protected static final Right ADMIN_ACCOUNT_RIGHT = Admin.R_renameAccount;
    
    Account mGlobalAdminAcct;
    private int mSequence = 1;
    
    
    private String nextSeq() {
        return "" + mSequence++;
    }
    
    private String genDomainName() {
        return nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genCosName() {
        return "cos-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genServerName() {
        return "server-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genXMPPComponentName() {
        return "xmpp-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private String genZimletName() {
        return "zimlet-" + nextSeq() + "." + BASE_DOMAIN_NAME;
    }
    
    private Domain createDomain() throws Exception {
        return sProv.createDomain(genDomainName(), new HashMap<String, Object>());
    }
    
    private Account createAccount(String localpart, Domain domain, Map<String, Object> attrs) throws Exception {
        if (domain == null)
            domain = createDomain();
         
        String email = localpart + "@" + domain.getName();
        return sProv.createAccount(email, PASSWORD, attrs);
    }
    
    private Account createUserAccount(String localpart, Domain domain) throws Exception {
        return createAccount(localpart, domain, null);
    }
    
    private Account createDelegatedAdminAccount(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        return createAccount(localpart, domain, attrs);
    }
    
    protected Account createGuestAccount(String email, String password) {
        return new ACL.GuestAccount(email, password);
    }
    
    protected Account createKeyAccount(String name, String accesKey) {
        AuthToken authToken = new TestACAccessKey.KeyAuthToken(name, accesKey);
        return new ACL.GuestAccount(authToken);
    }
    
    protected Account anonAccount() {
        return ACL.ANONYMOUS_ACCT;
    }
    
    private CalendarResource createCalendarResource(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, localpart);
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        
        String email = localpart + "@" + domain.getName();
        return sProv.createCalendarResource(email, PASSWORD, attrs);
    }
    
    private Cos createCos() throws Exception {
        return sProv.createCos(genCosName(), null);
    }
    
    protected DistributionList createGroup(String localpart, Domain domain, Map<String, Object> attrs) throws Exception {
        if (domain == null)
            domain = createDomain();
         
        String email = localpart + "@" + domain.getName();
        return sProv.createDistributionList(email, attrs);
    }
    
    private DistributionList createUserGroup(String localpart, Domain domain) throws Exception {
        return createGroup(localpart, domain, new HashMap<String, Object>());
    }
    
    private DistributionList createAdminGroup(String localpart, Domain domain) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
        return createGroup(localpart, domain, attrs);
    }
    
    
    private Server createServer() throws Exception {
        return sProv.createServer(genServerName(), new HashMap<String, Object>());
    }
    
    /*
    private Server createXMPPComponent() throws Exception {
        return sProv.createXMPPComponent(genXMPPComponentName(), null);
    }
    */
    
    private Zimlet createZimlet() throws Exception {
        return sProv.createZimlet(genZimletName(), new HashMap<String, Object>());
    }
    
    private Config getConfig() throws Exception {
        return sProv.getConfig();
    }
    
    private GlobalGrant getGlobalGrant() throws Exception {
        return sProv.getGlobalGrant();
    }
    
    private void deleteEntry(NamedEntry entry) throws Exception {
        if (entry instanceof Account)
            sProv.deleteAccount(entry.getId());
        else if (entry instanceof CalendarResource)
            sProv.deleteCalendarResource(entry.getId());
        else if (entry instanceof Cos)
            sProv.deleteCos(entry.getId());
        else if (entry instanceof Server)
            sProv.deleteServer(entry.getId());
        else
            throw new Exception("not yet implemented");
            
    }
    
    private void revokeAllGrantsOnGlobalGrant() throws Exception {
        
        Grants grants = RightCommand.getGrants(sProv,
                TargetType.global.getCode(), null, null, 
                null, null, null, false);
            
        for (RightCommand.ACE ace : grants.getACEs()) {
            RightCommand.revokeRight(sProv,
                getGlobalAdminAcct(),
                ace.targetType(), TargetBy.name, ace.targetName(),
                ace.granteeType(), GranteeBy.name, ace.granteeName(),
                ace.right(), ace.rightModifier());
        }
    }
    
    private Account getGlobalAdminAcct() throws Exception {
        if (mGlobalAdminAcct == null)
            mGlobalAdminAcct = sProv.get(AccountBy.name, TestUtil.getAddress("admin"));
        return mGlobalAdminAcct;
    }
    
    private void accountTargetTest(TargetType grantedOnTargetType, GranteeType granteeType, Right right) throws Exception {
        System.out.println("accountTargetTest: " + 
                "grant target=" + grantedOnTargetType.getCode() + 
                ", grantee type=" + granteeType.getCode() +
                ", right=" + right.getName());
        
        //
        // 1. some basic preparation
        //
        Domain domain = createDomain();
        boolean isUserRight = right.isUserRight();
        
        //
        // 2. setup grantee
        //
        NamedEntry grantee = null;
        String granteeName = null;
        String secret = null;
        
        switch (granteeType) {
        case GT_USER:
            if (isUserRight)
                grantee = createUserAccount("grantee-acct", domain);
            else
                grantee = createDelegatedAdminAccount("grantee-acct", domain);
            granteeName = grantee.getName();
            break;
        case GT_GROUP:
            if (isUserRight)
                grantee = createUserGroup("grantee-group", domain);
            else
                grantee = createAdminGroup("grantee-group", domain);
            granteeName = grantee.getName();
            break;
        case GT_AUTHUSER:
            break;
        case GT_DOMAIN:
            grantee = createDomain();
            granteeName = grantee.getName();
            break;
        case GT_GUEST:
            granteeName = "be-my-guest@guest.com";  // an email address
            secret = "test123"; // password
            break;
        case GT_KEY:
            granteeName = "be-my-guest"; // a display name
            secret = "test123"; // access key
            break;
        case GT_PUBLIC:
            break;
        default:
            fail();
        }
        
        //
        // 3. setup expectations for the granting action
        //
        boolean expectInvalidrequest = false;
        if (isUserRight) {
            if (grantedOnTargetType == TargetType.cos ||
                grantedOnTargetType == TargetType.server ||
                grantedOnTargetType == TargetType.xmppcomponent ||
                grantedOnTargetType == TargetType.zimlet ||
                grantedOnTargetType == TargetType.config)
                expectInvalidrequest = true;
        } else {
            // is admin right
            if (!granteeType.allowedForAdminRights())
                expectInvalidrequest = true;
            
            if (granteeType == GranteeType.GT_DOMAIN && right != Admin.R_crossDomainAdmin)
                expectInvalidrequest = true;
            
            if (grantedOnTargetType == TargetType.calresource ||
                grantedOnTargetType == TargetType.cos ||
                grantedOnTargetType == TargetType.server ||
                grantedOnTargetType == TargetType.xmppcomponent ||
                grantedOnTargetType == TargetType.zimlet ||
                grantedOnTargetType == TargetType.config)
                expectInvalidrequest = true;
        }
            
        //
        // 4. setup granting target
        //
        NamedEntry deleteMe = null;
        String targetName = null;
        boolean gotInvalidrequestException = false;
        
        switch (grantedOnTargetType) {
        case account:
            Account targetAccount = createUserAccount("target-acct", domain);
            targetName = targetAccount.getName();
            break;
        case calresource:
            CalendarResource targetCR = createCalendarResource("target-cr", domain);
            targetName = targetCR.getName();
            break;
        case cos:
            Cos targetCos = createCos();
            deleteMe = targetCos;
            targetName = targetCos.getName();
            break;
        case dl:
            DistributionList targetDL = createUserGroup("target-group", domain);
            targetName = targetDL.getName();
            break;
        case domain:
            targetName = domain.getName();
            break;
        case server:
            Server targetServer = createServer();
            deleteMe = targetServer;
            targetName = targetServer.getName();
            break;
        case xmppcomponent:
            return; // skip for now
        case zimlet:
            Zimlet targetZimlet = createZimlet();
            deleteMe = targetZimlet;
            targetName = targetZimlet.getName();
            break;
        case config:
            Config targetConfig = getConfig();
            break;
        case global:
            GlobalGrant targetGlobalGrant = getGlobalGrant();
            break;
        default:
            fail();
        }
            
        try {    
            // TODO: in a different test, test granting by a different authed account: 
            //       global admin, delegated admin, user
            // 
            Account grantingAccount = getGlobalAdminAcct();
            
            RightCommand.grantRight(
                    sProv, 
                    grantingAccount,
                    grantedOnTargetType.getCode(), TargetBy.name, targetName,
                    granteeType.getCode(), GranteeBy.name, granteeName, secret,
                    right.getName(), null);
            
        } catch (ServiceException e) {
            gotInvalidrequestException = ServiceException.INVALID_REQUEST.equals(e.getCode());
            
            // e.printStackTrace();
        }
        
        //
        // 5. verify
        //
        assertEquals(expectInvalidrequest, gotInvalidrequestException);

        //
        // 6. check the grant
        //
        // boolean allow = sAM.canDo(zsc.getAuthToken(), entry, right, false); todo
        
        //
        // finally, clean up
        //
        if (deleteMe != null)
            deleteEntry(deleteMe);
        
        // remvoe all grants on global grant so it will not interfere with later tests
        revokeAllGrantsOnGlobalGrant();
    }
    
    public void testAccountTarget() throws Exception {
        for (TargetType targetType : TargetType.values()) {
            for (GranteeType granteeType : GranteeType.values()) {
                accountTargetTest(targetType, granteeType, USER_RIGHT);
                accountTargetTest(targetType, granteeType, ADMIN_ACCOUNT_RIGHT);
            }
        }
    }
    

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");
        
        // TestUtil.runTest(TestACL.class);
        
        TestAC test = new TestAC();
        test.revokeAllGrantsOnGlobalGrant();
    }

}
