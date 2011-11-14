package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.Key.GranteeBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Group.GroupOwner;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.account.message.CreateDistributionListRequest;
import com.zimbra.soap.account.message.CreateDistributionListResponse;
import com.zimbra.soap.account.message.DistributionListActionRequest;
import com.zimbra.soap.account.message.DistributionListActionResponse;
import com.zimbra.soap.account.message.GetAccountMembershipRequest;
import com.zimbra.soap.account.message.GetAccountMembershipResponse;
import com.zimbra.soap.account.message.GetDistributionListRequest;
import com.zimbra.soap.account.message.GetDistributionListResponse;
import com.zimbra.soap.account.message.SubscribeDistributionListRequest;
import com.zimbra.soap.account.message.SubscribeDistributionListResponse;
import com.zimbra.soap.account.type.DistributionListAction;
import com.zimbra.soap.account.type.DistributionListInfo;
import com.zimbra.soap.account.type.DistributionListOwnerSelector;
import com.zimbra.soap.account.type.DistributionListSubscribeOp;
import com.zimbra.soap.account.type.DistributionListSubscribeStatus;
import com.zimbra.soap.account.type.DistributionListAction.Operation;
import com.zimbra.soap.admin.type.DLInfo;
import com.zimbra.soap.base.DistributionListOwnerInfoInterface;
import com.zimbra.soap.type.DistributionListOwnerBy;
import com.zimbra.soap.type.DistributionListOwnerType;
import com.zimbra.soap.type.DistributionListSelector;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.TargetBy;

public class TestProvDelegatedDL {
    private static boolean verbose = false;
    
    private static String TEST_ID = TestProvisioningUtil.genTestId();
    private static String DOMAIN_NAME = TestProvisioningUtil.baseDomainName("test-delegated-dl", TEST_ID);
    private static String ADMIN = TestUtil.getAddress("admin", DOMAIN_NAME);
    private static String USER_CREATOR = TestUtil.getAddress("creator", DOMAIN_NAME);
    private static String USER_OWNER = TestUtil.getAddress("owner", DOMAIN_NAME);
    private static String USER_NOT_OWNER = TestUtil.getAddress("not-owner", DOMAIN_NAME);
    private static String DL_NAME = TestUtil.getAddress("dl", DOMAIN_NAME);
    private static String PASSWORD = "test123";
        
    private static Provisioning prov;
    private static HttpDebugListener soapDebugListener = new DebugListener();
    
    private static class DebugListener implements HttpDebugListener {

        @Override
        public void receiveSoapMessage(PostMethod postMethod, Element envelope) {
            if (!verbose) {
                return;
            }
            
            System.out.println();
            System.out.println("=== Response ===");
            System.out.println(envelope.prettyPrint());
        }

        @Override
        public void sendSoapMessage(PostMethod postMethod, Element envelope) {
            if (!verbose) {
                return;
            }
            
            System.out.println();
            System.out.println("=== Request ===");
            System.out.println(envelope.prettyPrint());
        }
    }

    private static SoapTransport authUser(String acctName) throws Exception {
        com.zimbra.soap.account.type.Account acct = 
            new com.zimbra.soap.account.type.Account(
                    com.zimbra.soap.account.type.Account.By.NAME, acctName);
        
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setHttpDebugListener(soapDebugListener);
        
        AuthRequest req = new AuthRequest(acct, PASSWORD);
        AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        return transport;
    }
    
    private SoapTransport authAdmin() throws Exception {
        
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        transport.setHttpDebugListener(soapDebugListener);
        
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(ADMIN, PASSWORD);
        com.zimbra.soap.admin.message.AuthResponse resp = invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        return transport;
    }
    
    private static <T> T invokeJaxb(SoapTransport transport, Object jaxbObject)
    throws ServiceException, IOException {
        Element req = JaxbUtil.jaxbToElement(jaxbObject);
        Element res = transport.invoke(req);
        return (T) JaxbUtil.elementToJaxb(res);
    }
    
    private static Group createGroupAndAddOwner(String groupName) throws Exception {
        return createGroupAndAddOwner(groupName, null);
    }
    
    private static Group createGroupAndAddOwner(String groupName, List<KeyValuePair> attrs) 
    throws Exception {
        boolean dynamic = false;
        
        Group group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNull(group);
        
        /*
        Account owner = prov.get(AccountBy.name, USER_OWNER);
        assertNotNull(owner);
        
        Group group = prov.createDelegatedGroup(groupName, attrs, owner);
        assertNotNull(group);
        */
        
        SoapTransport transport = authUser(USER_CREATOR);
        
        CreateDistributionListRequest req = new CreateDistributionListRequest(
                groupName, attrs, dynamic);
        CreateDistributionListResponse resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, groupName);
        assertNotNull(group);
        assertEquals(groupName, group.getName());
        assertNotNull(group.getAttr(Provisioning.A_zimbraMailHost));
        
        /*
         * USER_CREATOR is automatically an owner now.
         */
        
        // add USER_OWNER as an owner
        DistributionListAction action = new DistributionListAction(Operation.addOwner);
        DistributionListActionRequest actionReq = new DistributionListActionRequest(
                DistributionListSelector.fromName(groupName), action);
        
        action.setOwner(new DistributionListOwnerSelector(DistributionListOwnerType.usr, 
                DistributionListOwnerBy.name, USER_OWNER));
        DistributionListActionResponse actionResp = invokeJaxb(transport, actionReq);
        
        // remove USER_CREATOR from the owner list
        action = new DistributionListAction(Operation.removeOwner);
        actionReq = new DistributionListActionRequest(
                DistributionListSelector.fromName(groupName), action);
        
        action.setOwner(new DistributionListOwnerSelector(DistributionListOwnerType.usr, 
                DistributionListOwnerBy.name, USER_CREATOR));
        actionResp = invokeJaxb(transport, actionReq);
        
        return group;
    }
    
    @BeforeClass
    public static void init() throws Exception {
        // init rights
        RightManager.getInstance();
        
        // use soap provisioning
        TestUtil.cliSetup();
        
        prov = Provisioning.getInstance();
        
        Domain domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
        
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, ProvisioningConstants.TRUE);
        Account admin = prov.createAccount(ADMIN, PASSWORD, adminAttrs);
        assertNotNull(admin);
        
        Account creator = prov.createAccount(USER_CREATOR, PASSWORD, new HashMap<String, Object>());
        assertNotNull(creator);
        
        Account owner = prov.createAccount(USER_OWNER, PASSWORD, new HashMap<String, Object>());
        assertNotNull(owner);
        
        Account notOwner = prov.createAccount(USER_NOT_OWNER, PASSWORD, new HashMap<String, Object>());
        assertNotNull(notOwner);
        
        prov.grantRight(TargetType.domain.getCode(), TargetBy.name, domain.getName(), 
                GranteeType.GT_USER.getCode(), GranteeBy.name, creator.getName(), null, 
                User.R_createDistList.getName(), null);
        
        // create a DL for get/action tests
        List<KeyValuePair> attrs = Lists.newArrayList(new KeyValuePair(
                Provisioning.A_zimbraDistributionListSubscriptionPolicy, 
                ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name()));
        Group group = createGroupAndAddOwner(DL_NAME, attrs);
    }
    
    // @AfterClass
    public static void cleanup() throws Exception {
        Account admin = prov.get(AccountBy.name, ADMIN);
        prov.deleteAccount(admin.getId());
        
        Account creator = prov.get(AccountBy.name, USER_CREATOR);
        prov.deleteAccount(creator.getId());
        
        Account owner = prov.get(AccountBy.name, USER_OWNER);
        prov.deleteAccount(owner.getId());
        
        Account notOwner = prov.get(AccountBy.name, USER_NOT_OWNER);
        prov.deleteAccount(notOwner.getId());
        
        Domain domain = prov.get(DomainBy.name, DOMAIN_NAME);
        prov.deleteDomain(domain.getId());
    }
    
    private String getAddress(String localpart) {
        return TestUtil.getAddress(localpart, DOMAIN_NAME);
    }
    
    
    @Test
    /*
     * Test the owners element in zimbraAdmin:GetDistributionList
     */
    public void getDistributionListAdmin() throws Exception {
        SoapTransport transport = authAdmin();
        
        com.zimbra.soap.admin.message.GetDistributionListRequest req = 
            new com.zimbra.soap.admin.message.GetDistributionListRequest(
                    com.zimbra.soap.admin.type.DistributionListSelector.fromName(DL_NAME));
        
        com.zimbra.soap.admin.message.GetDistributionListResponse resp = invokeJaxb(transport, req);
        com.zimbra.soap.admin.type.DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertNotNull(group);
        assertEquals(group.getId(), dlId);

        /*
        System.out.println("\nAttrs:");
        List<com.zimbra.soap.admin.type.Attr> attrs = dlInfo.getAttrList();
        for (com.zimbra.soap.admin.type.Attr attr : attrs) {
            System.out.println(attr.getN() + ", " + attr.getValue());
        }
        */
        
        List<? extends DistributionListOwnerInfoInterface> dlOwners = dlInfo.getOwners();
        assertEquals(1, dlOwners.size());
        for (DistributionListOwnerInfoInterface owner : dlOwners) {
            DistributionListOwnerType type = owner.getType();
            String id = owner.getId();
            String name = owner.getName();
            
            assertEquals(DistributionListOwnerType.usr, type);
            assertEquals(USER_OWNER, name);
        }
    }
    
    @Test
    public void createDistributionListPermDenied() throws Exception {
        String dlName = getAddress("dl-test-create-perm-denied");
        boolean dynamic = true;
        
        SoapTransport transport = authUser(USER_OWNER);
        
        CreateDistributionListRequest req = new CreateDistributionListRequest(
                dlName, null, dynamic);
        
        boolean caughtPermDenied = false;
        try {
            CreateDistributionListResponse resp = invokeJaxb(transport, req);
        } catch (SoapFaultException e) {
            String code = e.getCode();
            if (ServiceException.PERM_DENIED.equals(code)) {
                caughtPermDenied = true;
            }
        }
        
        assertTrue(caughtPermDenied);
    }
    
    @Test
    public void createDistributionList() throws Exception {
        String dlName = getAddress("dl-test-create");
        boolean dynamic = true;
        
        SoapTransport transport = authUser(USER_CREATOR);
        
        CreateDistributionListRequest req = new CreateDistributionListRequest(
                dlName, null, dynamic);
        
        List<KeyValuePair> attrsCreate = Lists.newArrayList(new KeyValuePair(
                Provisioning.A_zimbraDistributionListSubscriptionPolicy, 
                ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name()));
        
        req.setKeyValuePairs(attrsCreate);
        CreateDistributionListResponse resp = invokeJaxb(transport, req);
        DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        
        Group group = prov.getGroup(Key.DistributionListBy.name, dlName);
        assertNotNull(group);
        assertEquals(group.getId(), dlId);
        
        List<? extends DistributionListOwnerInfoInterface> dlOwners = dlInfo.getOwners();
        assertEquals(1, dlOwners.size());
        for (DistributionListOwnerInfoInterface owner : dlOwners) {
            DistributionListOwnerType type = owner.getType();
            String id = owner.getId();
            String name = owner.getName();
            
            assertEquals(DistributionListOwnerType.usr, type);
            assertEquals(USER_CREATOR, name);
        }
        
        boolean seenExpectedMail = false;
        boolean seenExpectedSubsPolicy = false;
        List<? extends KeyValuePair> attrs = dlInfo.getAttrList();
        for (KeyValuePair attr : attrs) {
            String name = attr.getKey();
            String value = attr.getValue();
            if (Provisioning.A_mail.equals(name)) {
                assertEquals(group.getName(), value);
                seenExpectedMail = true;
            }
            
            if (Provisioning.A_zimbraDistributionListSubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name(), value);
                seenExpectedSubsPolicy = true;
            }
        }
        assertTrue(seenExpectedMail);
        assertTrue(seenExpectedSubsPolicy);
    }
    
    @Test
    public void getDistributionList() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        GetDistributionListRequest req = new GetDistributionListRequest(
                DistributionListSelector.fromName(DL_NAME), Boolean.TRUE);
        
        GetDistributionListResponse resp = invokeJaxb(transport, req);
        assertTrue(resp.isOwner());
        assertFalse(resp.isMember());
        
        DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertNotNull(group);
        assertEquals(group.getId(), dlId);

        boolean seenMail = false;
        boolean seenSubsPolicy = false;
        boolean seenUnsubsPolicy = false;
        List<? extends KeyValuePair> attrs = dlInfo.getAttrList();
        for (KeyValuePair attr : attrs) {
            String name = attr.getKey();
            String value = attr.getValue();
            if (Provisioning.A_mail.equals(name)) {
                assertEquals(group.getName(), value);
                seenMail = true;
            }
            if (Provisioning.A_zimbraDistributionListSubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListSubscriptionPolicy.ACCEPT.name(), value);
                seenSubsPolicy = true;
            }
            if (Provisioning.A_zimbraDistributionListUnsubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListUnsubscriptionPolicy.REJECT.name(), value);
                seenUnsubsPolicy = true;
            }
        }
        assertTrue(seenMail);
        assertTrue(seenSubsPolicy);
        assertTrue(seenUnsubsPolicy);
        
        List<? extends DistributionListOwnerInfoInterface> dlOwners = dlInfo.getOwners();
        assertEquals(1, dlOwners.size());
        for (DistributionListOwnerInfoInterface owner : dlOwners) {
            DistributionListOwnerType type = owner.getType();
            String id = owner.getId();
            String name = owner.getName();
            
            assertEquals(DistributionListOwnerType.usr, type);
            assertEquals(USER_OWNER, name);
        }
    }

    @Test
    public void distributionListActionAddRemoveMembers() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        // addMembers
        DistributionListAction action = new DistributionListAction(Operation.addMembers);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        String MEMBER1 = "member1@test.com";
        String MEMBER2 = "member2@test.com";
        action.setMember(MEMBER1);
        action.setMember(MEMBER2);
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        Set<String> members = group.getAllMembersSet();
        assertEquals(2, members.size());
        assertTrue(members.contains(MEMBER1));
        assertTrue(members.contains(MEMBER2));
        
        // removeMembers
        action = new DistributionListAction(Operation.removeMembers);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        action.setMember(MEMBER1);
        action.setMember(MEMBER2);
        resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        members = group.getAllMembersSet();
        assertEquals(0, members.size());
    }

    @Test
    public void distributionListActionAddRemoveAlias() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        // addAlias
        DistributionListAction action = new DistributionListAction(Operation.addAlias);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        String ALIAS = getAddress("alias");
        action.setAlias(ALIAS);
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        // get the DL by alias
        Group group = prov.getGroup(Key.DistributionListBy.name, ALIAS);
        assertEquals(DL_NAME, group.getName());
        
        // removeAlias
        action = new DistributionListAction(Operation.removeAlias);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        action.setAlias(ALIAS);
        resp = invokeJaxb(transport, req);
        
        // get the DL by alias, should no longer be found
        group = prov.getGroup(Key.DistributionListBy.name, ALIAS);
        assertNull(group);
    }
    
    @Test
    public void distributionListActionAddRemoveOwner() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        // addOwner
        DistributionListAction action = new DistributionListAction(Operation.addOwner);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        action.setOwner(new DistributionListOwnerSelector(DistributionListOwnerType.usr, 
                DistributionListOwnerBy.name, USER_NOT_OWNER));
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        List<GroupOwner> owners = Group.GroupOwner.getOwners(group, true); 
        assertEquals(2, owners.size());
        boolean seenUserOwner = false;
        boolean seenUserNotOwner = false;
        for (GroupOwner owner : owners) {
            if (owner.getType() == GranteeType.GT_USER) {
                if (USER_OWNER.equals(owner.getName())) {
                    seenUserOwner = true;
                }
                if (USER_NOT_OWNER.equals(owner.getName())) {
                    seenUserNotOwner = true;
                }
            }
        }
        assertTrue(seenUserOwner);
        assertTrue(seenUserNotOwner);
        
        // removeOwner
        action = new DistributionListAction(Operation.removeOwner);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        action.setOwner(new DistributionListOwnerSelector(DistributionListOwnerType.usr, 
                DistributionListOwnerBy.name, USER_NOT_OWNER));
        resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        owners = Group.GroupOwner.getOwners(group, true); 
        assertEquals(1, owners.size());
        seenUserOwner = false;
        seenUserNotOwner = false;
        for (GroupOwner owner : owners) {
            if (owner.getType() == GranteeType.GT_USER) {
                if (USER_OWNER.equals(owner.getName())) {
                    seenUserOwner = true;
                }
                if (USER_NOT_OWNER.equals(owner.getName())) {
                    seenUserNotOwner = true;
                }
            }
        }
        assertTrue(seenUserOwner);
        assertFalse(seenUserNotOwner);
    }
    
    @Test
    public void distributionListActionModify() throws Exception {
        String ATTR = Provisioning.A_description;
        String VALUE = "test description";
        
        SoapTransport transport = authUser(USER_OWNER);
        
        DistributionListAction action = new DistributionListAction(Operation.modify);
        
        List<KeyValuePair> attrs = Lists.newArrayList(new KeyValuePair(ATTR, VALUE));
        action.setKeyValuePairs(attrs);
        
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertEquals(VALUE, group.getAttr(ATTR));
    }
    
    @Test
    public void distributionListActionRename() throws Exception {
        SoapTransport transport = authUser(USER_OWNER);
        
        DistributionListAction action = new DistributionListAction(Operation.rename);
        String DL_NEW_NAME = getAddress("dl-new-name");
        action.setNewName(DL_NEW_NAME);
        
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NAME), action);
        
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        Group group = prov.getGroup(Key.DistributionListBy.name, DL_NEW_NAME);
        assertEquals(DL_NEW_NAME, group.getName());
        
        // rename it back
        action = new DistributionListAction(Operation.rename);
        action.setNewName(DL_NAME);
        req = new DistributionListActionRequest(
                DistributionListSelector.fromName(DL_NEW_NAME), action);
        resp = invokeJaxb(transport, req);
        group = prov.getGroup(Key.DistributionListBy.name, DL_NAME);
        assertEquals(DL_NAME, group.getName());
    }
    
    @Test
    public void distributionListActionDelete() throws Exception {
        // create a group for the delete test
        String NAME = getAddress("to-be-deleted");
        Group group = createGroupAndAddOwner(NAME);
        
        SoapTransport transport = authUser(USER_OWNER);
        
        DistributionListAction action = new DistributionListAction(Operation.delete);
        DistributionListActionRequest req = new DistributionListActionRequest(
                DistributionListSelector.fromName(NAME), action);
        
        DistributionListActionResponse resp = invokeJaxb(transport, req);
        
        group = prov.getGroup(Key.DistributionListBy.name, NAME);
        assertNull(null);
    }
    
    @Test
    public void subscribeDistributionList() throws Exception {
        SoapTransport transport = authUser(USER_NOT_OWNER);
        
        // subscribe
        SubscribeDistributionListRequest req = new SubscribeDistributionListRequest(
                DistributionListSelector.fromName(DL_NAME),
                DistributionListSubscribeOp.subscribe);
        
        SubscribeDistributionListResponse resp = invokeJaxb(transport, req);
        assertEquals(DistributionListSubscribeStatus.subscribed, resp.getStatus());
        
        // unsubscribe
        req = new SubscribeDistributionListRequest(
                DistributionListSelector.fromName(DL_NAME),
                DistributionListSubscribeOp.unsubscribe);
        
        boolean caughtPermDenied = false;
        try {
            resp = invokeJaxb(transport, req);
        } catch (ServiceException e) {
            String code = e.getCode();
            if (ServiceException.PERM_DENIED.equals(code)) {
                caughtPermDenied = true;
            }
        }
        assertTrue(caughtPermDenied);
    }
    
    /*
    @Test
    public void getDistributionListInfo() throws Exception {
        String NAME = getAddress("get-dl-info-test");
        Group group = createGroupAndAddOwner(NAME);
        
        SoapTransport transport = authUser(USER_NOT_OWNER);
        
        GetDistributionListInfoRequest req = new GetDistributionListInfoRequest(
                DistributionListSelector.fromName(NAME));
        
        GetDistributionListInfoResponse resp = invokeJaxb(transport, req);
        
        assertFalse(resp.isOwner());
        assertFalse(resp.isMember());
        
        DistributionListInfo dlInfo = resp.getDl();
        
        String dlId = dlInfo.getId();
        assertEquals(group.getId(), dlId);

        boolean seenSubsPolicy = false;
        boolean seenUnsubsPolicy = false;
        List<? extends Attr> attrs = dlInfo.getAttrList();
        for (Attr attr : attrs) {
            String name = attr.getName();
            String value = attr.getValue();
            if (Provisioning.A_zimbraDistributionListSubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListSubscriptionPolicy.REJECT.name(), value);
                seenSubsPolicy = true;
            }
            if (Provisioning.A_zimbraDistributionListUnsubscriptionPolicy.equals(name)) {
                assertEquals(ZAttrProvisioning.DistributionListUnsubscriptionPolicy.REJECT.name(), value);
                seenUnsubsPolicy = true;
            }
        }
        assertTrue(seenSubsPolicy);
        assertTrue(seenUnsubsPolicy);
    }
    */
    
    @Test
    public void getAccountMembership() throws Exception {
        String GROUP_NAME = getAddress("getAccountMembership".toLowerCase());
        Group group = createGroupAndAddOwner(GROUP_NAME);
        
        // add a member
        prov.addGroupMembers(group, new String[]{USER_NOT_OWNER});
        
        SoapTransport transport = authUser(USER_NOT_OWNER);
        GetAccountMembershipRequest req = new GetAccountMembershipRequest(Boolean.TRUE);
        GetAccountMembershipResponse resp = invokeJaxb(transport, req);
        
        boolean seenGroup = false;
        List<DLInfo> groups = resp.getDlList();
        for (DLInfo dlInfo : groups) {
            String id = dlInfo.getId();
            String name = dlInfo.getName();
            
            if (group.getId().equals(id) && name.equals(GROUP_NAME)) {
                seenGroup = true;
            }
        }
        assertTrue(seenGroup);
        
        // rename group
        String GROUP_NEW_NAME = getAddress("getAccountMembership-new".toLowerCase());
        prov.renameGroup(group.getId(), GROUP_NEW_NAME);
        
        // get membership again, should show the new name
        resp = invokeJaxb(transport, req);
        seenGroup = false;
        groups = resp.getDlList();
        for (DLInfo dlInfo : groups) {
            String id = dlInfo.getId();
            String name = dlInfo.getName();
            
            if (group.getId().equals(id) && name.equals(GROUP_NEW_NAME)) {
                seenGroup = true;
            }
        }
        assertTrue(seenGroup);
    }
    
}
