package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.GranteeFlag;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightChecker;
import com.zimbra.cs.account.accesscontrol.RightManager;
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
    
    protected static Right USER_RIGHT;
    protected static Right ADMIN_RIGHT_ACCOUNT;
    protected static Right ADMIN_RIGHT_CALENDAR_RESOURCE;
    protected static Right ADMIN_RIGHT_CONFIG;
    protected static Right ADMIN_RIGHT_COS;
    protected static Right ADMIN_RIGHT_DISTRIBUTION_LIST;
    protected static Right ADMIN_RIGHT_DOMAIN;
    protected static Right ADMIN_RIGHT_GLOBALGRANT;
    protected static Right ADMIN_RIGHT_SERVER;
    protected static Right ADMIN_RIGHT_XMPP_COMPONENT;
    protected static Right ADMIN_RIGHT_ZIMLET;
    private static List<Right> sRights;
    
    private Account mGlobalAdminAcct;
    private int mSequence = 1;
    
    
    List<NamedEntry> mDeleteEntries = new ArrayList<NamedEntry>();
    
    // add domains in a seperate list, so they are deleted, after all domained 
    // entries are deleted, or else will get domain not empty exception
    // TODO: need to handle subdomains - those needed to be deleted before parent domains or
    //       else won't get deleted.  For now just go in LDAP and delete the test root directly.
    List<NamedEntry> mDeleteDomains = new ArrayList<NamedEntry>();
    
    static {
        try {
            // setup rights
            USER_RIGHT                    = getRight("test-user"); // User.R_loginAs;
            ADMIN_RIGHT_ACCOUNT           = getRight("test-preset-account");
            ADMIN_RIGHT_CALENDAR_RESOURCE = getRight("test-preset-calendarresource");
            ADMIN_RIGHT_CONFIG            = getRight("test-preset-globalconfig");
            ADMIN_RIGHT_COS               = getRight("test-preset-cos");
            ADMIN_RIGHT_DISTRIBUTION_LIST = getRight("test-preset-distributionlist");
            ADMIN_RIGHT_DOMAIN            = getRight("test-preset-domain");
            ADMIN_RIGHT_GLOBALGRANT       = getRight("test-preset-globalgrant");
            ADMIN_RIGHT_SERVER            = getRight("test-preset-server");
            ADMIN_RIGHT_XMPP_COMPONENT    = getRight("test-preset-xmppcomponent");
            ADMIN_RIGHT_ZIMLET            = getRight("test-preset-zimlet");
            
            sRights = new ArrayList<Right>();
            sRights.add(USER_RIGHT);
            sRights.add(ADMIN_RIGHT_ACCOUNT);
            sRights.add(ADMIN_RIGHT_CALENDAR_RESOURCE);
            sRights.add(ADMIN_RIGHT_CONFIG);
            sRights.add(ADMIN_RIGHT_COS);
            sRights.add(ADMIN_RIGHT_DISTRIBUTION_LIST);
            sRights.add(ADMIN_RIGHT_DOMAIN);
            sRights.add(ADMIN_RIGHT_GLOBALGRANT);
            sRights.add(ADMIN_RIGHT_SERVER);
            sRights.add(ADMIN_RIGHT_XMPP_COMPONENT);
            sRights.add(ADMIN_RIGHT_ZIMLET);
            
        
        } catch (ServiceException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    static Right getRight(String right) throws ServiceException {
        return RightManager.getInstance().getRight(right);
    }
    
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
        Domain domain = sProv.createDomain(genDomainName(), new HashMap<String, Object>());
        mDeleteDomains.add(domain);
        return domain;
    }
    
    private Account createAccount(String localpart, Domain domain, Map<String, Object> attrs) throws Exception {
        if (domain == null)
            domain = createDomain();
         
        String email = localpart + "@" + domain.getName();
        Account acct = sProv.createAccount(email, PASSWORD, attrs);
        mDeleteEntries.add(acct);
        return acct;
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
        CalendarResource cr = sProv.createCalendarResource(email, PASSWORD, attrs);
        mDeleteEntries.add(cr);
        return cr;
    }
    
    private Cos createCos() throws Exception {
        Cos cos = sProv.createCos(genCosName(), null);
        mDeleteEntries.add(cos);
        return cos;
    }
    
    protected DistributionList createGroup(String localpart, Domain domain, Map<String, Object> attrs) throws Exception {
        if (domain == null)
            domain = createDomain();
         
        String email = localpart + "@" + domain.getName();
        DistributionList dl = sProv.createDistributionList(email, attrs);
        mDeleteEntries.add(dl);
        return dl;
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
        Server server = sProv.createServer(genServerName(), new HashMap<String, Object>());
        mDeleteEntries.add(server);
        return server;
    }
    
    /*
    private Server createXMPPComponent() throws Exception {
        return sProv.createXMPPComponent(genXMPPComponentName(), null);
    }
    */
    
    private Zimlet createZimlet() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraZimletVersion, "1.0");
        Zimlet zimlet = sProv.createZimlet(genZimletName(), attrs);
        mDeleteEntries.add(zimlet);
        return zimlet;
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
        else if (entry instanceof DistributionList)
            sProv.deleteDistributionList(entry.getId());
        else if (entry instanceof Domain)
            sProv.deleteDomain(entry.getId());
        else if (entry instanceof Server)
            sProv.deleteServer(entry.getId());
        else if (entry instanceof Zimlet)
            sProv.deleteZimlet(entry.getName());
        else
            throw new Exception("not yet implemented");
            
    }
    
    // delete all non-domained entries
    // for domained entries, it is faster to go in LDAP and just delete the domain root
    private void deleteAllEntries() throws Exception {
        for (NamedEntry entry : mDeleteEntries)
            deleteEntry(entry);
        mDeleteEntries.clear();
        
        for (NamedEntry entry : mDeleteDomains)
            deleteEntry(entry);
        mDeleteDomains.clear();
    }
    
    private void revokeAllGrantsOnGlobalGrant() throws Exception {
        
        Grants grants = RightCommand.getGrants(sProv,
                TargetType.global.getCode(), null, null, 
                null, null, null, false);
            
        for (RightCommand.ACE ace : grants.getACEs()) {
            RightCommand.revokeRight(sProv,
                getGlobalAdminAcct(),
                ace.targetType(), TargetBy.id, ace.targetId(),
                ace.granteeType(), GranteeBy.id, ace.granteeId(),
                ace.right(), ace.rightModifier());
        }
    }
    
    private Account getGlobalAdminAcct() throws Exception {
        if (mGlobalAdminAcct == null)
            mGlobalAdminAcct = sProv.get(AccountBy.name, TestUtil.getAddress("admin"));
        return mGlobalAdminAcct;
    }
    
    private boolean asAdmin(Account acct) {
        // for now return true if the account is an admin account
        // TODO: test cases when the account is an admin account but is not using the admin privelege
        return (acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false) ||
                acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false));
    }
    
    private boolean isRightGrantableOnTargetType(Right right, TargetType targetType) throws Exception {
        if (targetType == TargetType.dl && !RightChecker.allowGroupTarget(right))
            return false;
        
        if (right.isUserRight()) {
            return targetType == TargetType.account ||
                   targetType == TargetType.calresource ||
                   targetType == TargetType.dl ||
                   targetType == TargetType.domain ||
                   targetType == TargetType.global;
        } else {
            TargetType rightTarget = right.getTargetType();
            switch (rightTarget) {
            case account:
                return targetType == TargetType.account ||
                       targetType == TargetType.dl ||
                       targetType == TargetType.domain ||
                       targetType == TargetType.global;
            case calresource:
                return targetType == TargetType.calresource ||
                       targetType == TargetType.dl ||
                       targetType == TargetType.domain ||
                       targetType == TargetType.global;
            case cos:
                return targetType == TargetType.cos ||
                       targetType == TargetType.global;
            case dl:
                return targetType == TargetType.dl ||
                       targetType == TargetType.domain ||
                       targetType == TargetType.global;
            case domain:
                return targetType == TargetType.domain ||
                       targetType == TargetType.global;
            case server:
                return targetType == TargetType.server ||
                       targetType == TargetType.global;
            case xmppcomponent:
                return targetType == TargetType.xmppcomponent ||
                       targetType == TargetType.global;
            case zimlet:
                return targetType == TargetType.zimlet ||
                       targetType == TargetType.global;
            case config:
                return targetType == TargetType.config ||
                       targetType == TargetType.global;
            case global:
                return targetType == TargetType.global;
            default:
                fail();
                return false; // just to get the compiler happy
            }
        }
    }
    
    private void doTest(String note, TargetType grantedOnTargetType, GranteeType granteeType, Right right) throws Exception {
        System.out.println("testing (" + note + "): " + 
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
        List<Account> allowedAccts = new ArrayList<Account>();
        List<Account> deniedAccts = new ArrayList<Account>();
        NamedEntry grantee = null;
        String granteeName = null;
        String secret = null;
        
        switch (granteeType) {
        case GT_USER:
            if (isUserRight) {
                grantee = createUserAccount("grantee-user-acct", domain);
                allowedAccts.add((Account)grantee);
                deniedAccts.add(createUserAccount("denied-user-acct", domain));
            } else {
                grantee = createDelegatedAdminAccount("grantee-da-acct", domain);
                allowedAccts.add((Account)grantee);
                deniedAccts.add(createDelegatedAdminAccount("denied-da-acct", domain));
            }
            granteeName = grantee.getName();

            break;
        case GT_GROUP:
            if (isUserRight) {
                grantee = createUserGroup("grantee-user-group", domain);
                Account allowedAcct = createUserAccount("allowed-user-acct", domain);
                allowedAccts.add(allowedAcct);
                sProv.addMembers((DistributionList)grantee, new String[]{allowedAcct.getName()});
                deniedAccts.add(createUserAccount("denied-user-acct", domain));
            } else {
                grantee = createAdminGroup("grantee-admin-group", domain);
                Account allowedAcct = createDelegatedAdminAccount("allowed-da-acct", domain);
                allowedAccts.add(allowedAcct);
                sProv.addMembers((DistributionList)grantee, new String[]{allowedAcct.getName()});
                deniedAccts.add(createDelegatedAdminAccount("denied-da-acct", domain));
            }
            granteeName = grantee.getName();
            break;
        case GT_AUTHUSER:
            if (isUserRight) {
                allowedAccts.add(createUserAccount("allowed-user-acct", domain));
                deniedAccts.add(createGuestAccount("not-my-guest@external.com", "test123"));
            } else {
                deniedAccts.add(createDelegatedAdminAccount("denied-da-acct", domain));
            }
            break;
        case GT_DOMAIN:
            grantee = createDomain();
            if (isUserRight) {
                allowedAccts.add(createUserAccount("allowed-user-acct", (Domain)grantee));
                Domain notGrantee = createDomain();
                deniedAccts.add(createUserAccount("denied-user-acct", notGrantee));
            } else {
                deniedAccts.add(createDelegatedAdminAccount("denied-da-acct", (Domain)grantee));
                // TODO: TEST R_crossDomainAdmin
            }
            granteeName = grantee.getName();
            break;
        case GT_GUEST:
            granteeName = "be-my-guest@guest.com";  // an email address
            secret = "test123"; // password
            if (isUserRight) {
                allowedAccts.add(createGuestAccount(granteeName, secret));
                deniedAccts.add(createGuestAccount("not-my-guest@external.com", "bad"));
            } else {
                deniedAccts.add(createDelegatedAdminAccount("denied-da-acct", domain));
                deniedAccts.add(createGuestAccount(granteeName, secret));
            }
            break;
        case GT_KEY:
            granteeName = "be-my-guest"; // a display name
            secret = "test123"; // access key
            if (isUserRight) {
                allowedAccts.add(createKeyAccount(granteeName, secret));
                deniedAccts.add(createKeyAccount("not-my-guest", "bad"));
            } else {
                deniedAccts.add(createDelegatedAdminAccount("denied-da-acct", domain));
                deniedAccts.add(createKeyAccount(granteeName, secret));
            }
            break;
        case GT_PUBLIC:
            if (isUserRight) {
                allowedAccts.add(anonAccount());
            } else {
                deniedAccts.add(anonAccount());
            }
            break;
        default:
            fail();
        }
        
        //
        // 3. setup expectations for the granting action
        //
        boolean expectInvalidRequest = false;
        if (isUserRight) {
            expectInvalidRequest = !isRightGrantableOnTargetType(right, grantedOnTargetType);
            
        } else {
            // is admin right
            if (!granteeType.allowedForAdminRights())
                expectInvalidRequest = true;
            
            if (!expectInvalidRequest) {
                if (granteeType == GranteeType.GT_DOMAIN && right != Admin.R_crossDomainAdmin)
                    expectInvalidRequest = true;
            }
            
            if (!expectInvalidRequest)
                expectInvalidRequest = !isRightGrantableOnTargetType(right, grantedOnTargetType);
        }
            
        //
        // 4. setup target on which the right is to be granted
        //
        Entry grantedOnTarget = null;
        String targetName = null;
        boolean gotInvalidrequestException = false;
        
        switch (grantedOnTargetType) {
        case account:
            grantedOnTarget = createUserAccount("target-acct", domain);
            targetName = ((Account)grantedOnTarget).getName();
            break;
        case calresource:
            grantedOnTarget = createCalendarResource("target-cr", domain);
            targetName = ((CalendarResource)grantedOnTarget).getName();
            break;
        case cos:
            grantedOnTarget = createCos();
            targetName = ((Cos)grantedOnTarget).getName();
            break;
        case dl:
            grantedOnTarget = createUserGroup("target-group", domain);
            targetName = ((DistributionList)grantedOnTarget).getName();
            break;
        case domain:
            grantedOnTarget = domain;
            targetName = domain.getName();
            break;
        case server:
            grantedOnTarget = createServer();
            targetName = ((Server)grantedOnTarget).getName();
            break;
        case xmppcomponent:
            cleanup(); // skip for now
            return;
        case zimlet:
            grantedOnTarget = createZimlet();
            targetName = ((Zimlet)grantedOnTarget).getName();
            break;
        case config:
            grantedOnTarget = getConfig();
            break;
        case global:
            grantedOnTarget = getGlobalGrant();
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
        // 5. verify the grant
        //
        assertEquals(expectInvalidRequest, gotInvalidrequestException);

        //
        // 6. setup target object
        //
        Entry goodTarget = null;
        Entry badTarget = null;
        TargetType targetTypeOfRight = right.getTargetType();
        switch (targetTypeOfRight) {
        case account:
            if (grantedOnTargetType == TargetType.account) {
                goodTarget = grantedOnTarget;
                badTarget = createUserAccount("bad-target-acct", domain);
                
            } else if (grantedOnTargetType == TargetType.calresource) {
                if (isUserRight) {
                    goodTarget = grantedOnTarget;
                    badTarget = createCalendarResource("bad-target-cr", domain);
                } else {
                    badTarget = grantedOnTarget;
                }
                
            } else if (grantedOnTargetType == TargetType.dl) {
                if (RightChecker.allowGroupTarget(right)) {
                    goodTarget = createUserAccount("target-acct", domain);
                    sProv.addMembers((DistributionList)grantedOnTarget, new String[]{((Account)goodTarget).getName()});
                } else {
                    badTarget = createUserAccount("target-acct", domain);
                    sProv.addMembers((DistributionList)grantedOnTarget, new String[]{((Account)badTarget).getName()});
                }
                
            } else if (grantedOnTargetType == TargetType.domain) {
                goodTarget = createUserAccount("target-acct", domain);
                
                Domain anyDomain = createDomain();
                badTarget = createUserAccount("target-acct", anyDomain);
                
            } else if (grantedOnTargetType == TargetType.global) {
                Domain anyDomain = createDomain();
                goodTarget = createUserAccount("target-acct", anyDomain);
                
            } else {
                badTarget = grantedOnTarget;
            }

            break;    
        case calresource:
            if (grantedOnTargetType == TargetType.calresource) {
                goodTarget = grantedOnTarget;
                badTarget = createCalendarResource("bad-target-cr", domain);
                
            } else if (grantedOnTargetType == TargetType.dl) {
                if (RightChecker.allowGroupTarget(right)) {
                    goodTarget = createCalendarResource("target-cr", domain);
                    sProv.addMembers((DistributionList)grantedOnTarget, new String[]{((Account)goodTarget).getName()});
                } else {
                    badTarget = createCalendarResource("target-cr", domain);
                    sProv.addMembers((DistributionList)grantedOnTarget, new String[]{((Account)badTarget).getName()});
                }
                
            } else if (grantedOnTargetType == TargetType.domain) {
                goodTarget = createCalendarResource("target-cr", domain);
                
                Domain anyDomain = createDomain();
                badTarget = createUserAccount("target-acct", anyDomain);
                
            } else if (grantedOnTargetType == TargetType.global) {
                Domain anyDomain = createDomain();
                goodTarget = createCalendarResource("target-cr", anyDomain);
            } else {
                badTarget = grantedOnTarget;
            }
            break;    
        case cos:
            if (grantedOnTargetType == TargetType.cos)
                goodTarget = grantedOnTarget;
            else if (grantedOnTargetType == TargetType.global)
                goodTarget = createCos();
            
            if (goodTarget == null)
                badTarget = grantedOnTarget;
            break;
        case dl:
            if (grantedOnTargetType == TargetType.dl) {
                goodTarget = grantedOnTarget;
                badTarget = createUserGroup("bad-target-dl", domain);
            } else if (grantedOnTargetType == TargetType.domain) {
                goodTarget = createUserGroup("target-dl", domain);
                
                Domain anyDomain = createDomain();
                badTarget = createUserGroup("bad-target-dl", anyDomain);
                
            } else if (grantedOnTargetType == TargetType.global) {
                Domain anyDomain = createDomain();
                goodTarget = createUserGroup("target-dl", anyDomain);
            } else {
                badTarget = grantedOnTarget;
            }
            break;  
        case domain:
            if (grantedOnTargetType == TargetType.domain) {
                goodTarget = grantedOnTarget;
                badTarget = createDomain();
            } else if (grantedOnTargetType == TargetType.global) {
                goodTarget = createDomain();
            } else {
                badTarget = grantedOnTarget;
            }
            break;
        case server:
            if (grantedOnTargetType == TargetType.server) {
                goodTarget = grantedOnTarget;
                badTarget = createServer();
            } else if (grantedOnTargetType == TargetType.global) {
                goodTarget = createServer();
            } else {
                badTarget = grantedOnTarget;
            }
            break;
        case xmppcomponent:
            cleanup(); // skip for now
            return;
        case zimlet:
            // zimlet is trouble, need to reload it or else the grant is not on the object
            // ldapProvisioning.getZimlet does not return a cached entry so our grantedOnTarget
            // object does not have the grant
            sProv.reload(grantedOnTarget);
            
            if (grantedOnTargetType == TargetType.zimlet) {
                goodTarget = grantedOnTarget;
                badTarget = createZimlet();
            } else if (grantedOnTargetType == TargetType.global) {
                goodTarget = createZimlet();
            } else {
                badTarget = grantedOnTarget;
            }
            break;
        case config:
            if (grantedOnTargetType == TargetType.config)
                goodTarget = grantedOnTarget;
            else if (grantedOnTargetType == TargetType.global)
                goodTarget = getConfig();
            else
                badTarget = grantedOnTarget;
            break;
        case global:
            if (grantedOnTargetType == TargetType.global)
                goodTarget = getGlobalGrant();
            else
                badTarget = grantedOnTarget;
            break;
        default:
            fail();
        }
        

        //
        // 7. check permission
        //
        boolean allow;
        if (goodTarget != null) {
            for (Account allowedAcct : allowedAccts) {
                allow = sAM.canDo(allowedAcct, goodTarget, right, asAdmin(allowedAcct), null);
                assertTrue(allow);  
            }
            for (Account deniedAcct : deniedAccts) {
                allow = false;
                try {
                    allow = sAM.canDo(deniedAcct, goodTarget, right, asAdmin(deniedAcct), null);
                } catch (ServiceException e) {
                    if (!ServiceException.PERM_DENIED.equals(e.getCode()))
                        fail();
                }
                assertFalse(allow);  
            }
        }
        if (badTarget != null) {
            for (Account allowedAcct : allowedAccts) {
                allow = sAM.canDo(allowedAcct, badTarget, right, asAdmin(allowedAcct), null);
                assertFalse(allow);  
            }
            for (Account deniedAcct : deniedAccts) {
                allow = false;
                try {
                    allow = sAM.canDo(deniedAcct, badTarget, right, asAdmin(deniedAcct), null);
                } catch (ServiceException e) {
                    if (!ServiceException.PERM_DENIED.equals(e.getCode()))
                        fail();
                }
                assertFalse(allow); 
            }
        }
        

        //
        // finally, clean up
        //
        cleanup();
    }
    
    private void cleanup() throws Exception {
        // remove all grants on global grant so it will not interfere with later tests
        revokeAllGrantsOnGlobalGrant();
        
        deleteAllEntries();
    }
    
    /*
     * test basic target-grantee-right combos
     */
    public void testBasic() throws Exception {

        // full test
        /*
        int totalTests = TargetType.values().length * GranteeType.values().length * sRights.size();
        int curTest = 1;
        for (TargetType targetType : TargetType.values()) {
            for (GranteeType granteeType : GranteeType.values()) {
                for (Right right : sRights) {
                    doTest((curTest++) + "/" + totalTests, targetType, granteeType, right);
                }
            }
        }
        */
        
        /*
         *  account 
         *  calresource
         *  cos
         *  dl
         *  domain
         *  server
         *  xmppcomponent
         *  zimlet
         *  config
         *  global
         */
        // test a particular grant target
        int totalTests = GranteeType.values().length * sRights.size();
        int curTest = 1;
        TargetType targetType = TargetType.zimlet;
        for (GranteeType granteeType : GranteeType.values()) {
            for (Right right : sRights) {
                doTest((curTest++) + "/" + totalTests, targetType, granteeType, right);
            }
        }

        
        // test a particular grant target and grantee type and right
        // doTest("1/1", TargetType.dl, GranteeType.GT_USER, ADMIN_RIGHT_ACCOUNT);
    }
    

    /*
     *  Note: do *not* copy it to /Users/pshao/p4/main/ZimbraServer/conf
     *  that could accidently generate a RightDef.java with our test rights.
     *  
     *  cp /Users/pshao/p4/main/ZimbraServer/data/unittest/*.xml /opt/zimbra/conf/rights
     *  and
     *  uncomment sCoreRightDefFiles.add("rights-unittest.xml"); in RightManager
     *  
     *  REMEMBER: to comment it out before checking in
     */
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");
        
        TestUtil.runTest(TestAC.class);
        
        /*
        TestAC test = new TestAC();
        test.revokeAllGrantsOnGlobalGrant();
        */
    }

}
