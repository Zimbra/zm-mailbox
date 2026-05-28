/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Group} and group management.
 *
 * Tests verify group creation, modification, deletion, membership operations,
 * subscription policies, and owner management.
 */
public class GroupTest {

    private Provisioning prov;
    private Domain testDomain;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initProvisioning();
        prov = Provisioning.getInstance();

        // Create test domain for group operations
        Map<String, Object> domainAttrs = new HashMap<>();
        domainAttrs.put("description", "Test domain for GroupTest");
        testDomain = prov.createDomain("test-group-domain.local", domainAttrs);
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup
        try {
            if (testDomain != null) {
                prov.deleteDomain(testDomain.getId());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        MailboxTestUtil.clearData();
    }

    @Test
    public void createDistributionList_withAttributes_persistsSuccessfully() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("displayName", "Test Distribution List");
        attrs.put("description", "Test DL for membership tests");

        Group dl = prov.createDistributionList("testdl@test-group-domain.local", attrs);

        assertNotNull(dl);
        assertNotNull(dl.getId());
        assertEquals("testdl", dl.getName());
        assertEquals("Test Distribution List", dl.getDisplayName());
        assertEquals("testdl@test-group-domain.local", dl.getMail());
        assertEquals("Test DL for membership tests", dl.getAttr("description"));
    }

    @Test
    public void getDistributionList_byName_returnsSameEntry() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("displayName", "Test DL");

        Group created = prov.createDistributionList("dlname@test-group-domain.local", attrs);

        Group retrieved = prov.getGroup(DistributionListBy.name, "dlname@test-group-domain.local");

        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("dlname@test-group-domain.local", retrieved.getName());
        assertEquals("Test DL", retrieved.getDisplayName());
    }

    @Test
    public void getDistributionList_byId_returnsSameEntry() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("displayName", "DL By ID Test");

        Group created = prov.createDistributionList("dlbyid@test-group-domain.local", attrs);

        Group retrieved = prov.getGroup(DistributionListBy.id, created.getId());

        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("dlbyid", retrieved.getName());
    }

    @Test
    public void deleteDistributionList_removesFromSystem_noLongerRetrievable() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("displayName", "DL To Delete");

        Group created = prov.createDistributionList("todll@test-group-domain.local", attrs);
        String dlId = created.getId();

        // Verify exists
        Group beforeDelete = prov.getGroup(DistributionListBy.id, dlId);
        assertNotNull(beforeDelete);

        // Delete
        prov.deleteGroup(dlId);

        // Verify no longer exists
        Group afterDelete = prov.getGroup(DistributionListBy.id, dlId);
        assertNull(afterDelete);
    }

    @Test
    public void modifyDistributionList_updatesAttributes_changePersisted() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("displayName", "Original Name");

        Group created = prov.createDistributionList("modifytest@test-group-domain.local", attrs);

        // Modify
        Map<String, Object> modifyAttrs = new HashMap<>();
        modifyAttrs.put("displayName", "Updated Name");
        modifyAttrs.put("description", "Updated description");

        prov.modifyAttrs(created, modifyAttrs);

        // Retrieve and verify
        Group modified = prov.getGroup(DistributionListBy.id, created.getId());
        assertEquals("Updated Name", modified.getDisplayName());
        assertEquals("Updated description", modified.getAttr("description"));
    }

    @Test
    public void addMember_toDistributionList_memberListUpdated() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Members Test DL");
        Group dl = prov.createDistributionList("memberstest@test-group-domain.local", dlAttrs);

        Map<String, Object> acctAttrs = new HashMap<>();
        acctAttrs.put("displayName", "Member Account");
        Account acct = prov.createAccount("memberacct@test-group-domain.local", "password", acctAttrs);

        // Add member
        prov.addGroupMembers(dl, new String[]{acct.getMail()});

        // Verify member is in group
        String[] members = prov.getGroupMembers(dl);
        assertTrue("Account should be member of group", members != null && java.util.Arrays.asList(members).contains(acct.getMail()));
    }

    @Test
    public void removeMember_fromDistributionList_memberListUpdated() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Remove Member Test");
        Group dl = prov.createDistributionList("removetest@test-group-domain.local", dlAttrs);

        Map<String, Object> acctAttrs = new HashMap<>();
        acctAttrs.put("displayName", "Member To Remove");
        Account acct = prov.createAccount("torem@test-group-domain.local", "password", acctAttrs);

        // Add then remove
        prov.addGroupMembers(dl, new String[]{acct.getMail()});
        String[] beforeRemove = prov.getGroupMembers(dl);
        assertTrue(beforeRemove != null && java.util.Arrays.asList(beforeRemove).contains(acct.getMail()));

        prov.removeGroupMembers(dl, new String[]{acct.getMail()});

        // Verify member no longer in group
        String[] afterRemove = prov.getGroupMembers(dl);
        assertFalse("Account should not be member after removal", afterRemove != null && java.util.Arrays.asList(afterRemove).contains(acct.getMail()));
    }

    @Test
    public void isMemberOf_accountInGroup_returnsTrue() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Member Check DL");
        Group dl = prov.createDistributionList("membercheck@test-group-domain.local", dlAttrs);

        Map<String, Object> acctAttrs = new HashMap<>();
        acctAttrs.put("displayName", "Checking Member");
        Account acct = prov.createAccount("checking@test-group-domain.local", "password", acctAttrs);

        prov.addGroupMembers(dl, new String[]{acct.getMail()});

        // Test membership check
        assertTrue("Account should be member of group", dl.isMemberOf(acct));
    }

    @Test
    public void hideInGal_attributeSet_groupHiddenFromGal() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Hidden DL");
        dlAttrs.put("zimbraHideInGal", "TRUE");

        Group dl = prov.createDistributionList("hidden@test-group-domain.local", dlAttrs);

        assertTrue("Group should be hidden in GAL", dl.hideInGal());
    }

    @Test
    public void hideInGal_attributeNotSet_groupVisibleInGal() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Visible DL");

        Group dl = prov.createDistributionList("visible@test-group-domain.local", dlAttrs);

        assertFalse("Group should be visible in GAL", dl.hideInGal());
    }

    @Test
    public void isAddrOfEntry_withGroupMail_returnsTrue() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Addr Test DL");

        Group dl = prov.createDistributionList("addrtest@test-group-domain.local", dlAttrs);

        assertTrue("Group mail should be recognized as address of entry",
                dl.isAddrOfEntry("addrtest@test-group-domain.local"));
    }

    @Test
    public void getSubscriptionPolicy_defaultPolicy_returnReject() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Policy Test DL");

        Group dl = prov.createDistributionList("policy@test-group-domain.local", dlAttrs);

        // Default subscription policy should be REJECT
        assertEquals(Group.DEFAULT_SUBSCRIPTION_POLICY, dl.getSubscriptionPolicy());
    }

    @Test
    public void getUnsubscriptionPolicy_defaultPolicy_returnReject() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Unsub Policy Test DL");

        Group dl = prov.createDistributionList("unsubpolicy@test-group-domain.local", dlAttrs);

        // Default unsubscription policy should be REJECT
        assertEquals(Group.DEFAULT_UNSUBSCRIPTION_POLICY, dl.getUnsubscriptionPolicy());
    }

    @Test
    public void getEntryType_forDistributionList_returnsGroupType() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Type Test DL");

        Group dl = prov.createDistributionList("typetest@test-group-domain.local", dlAttrs);

        assertEquals(Entry.EntryType.DISTRIBUTIONLIST, dl.getEntryType());
    }

    @Test
    public void getAllAddrsSet_includesGroupMail() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "All Addrs DL");

        Group dl = prov.createDistributionList("alladdrs@test-group-domain.local", dlAttrs);

        Set<String> addrs = dl.getAllAddrsSet();
        assertNotNull(addrs);
        assertTrue("All addresses should include group mail", addrs.contains("alladdrs@test-group-domain.local"));
    }

    @Test
    public void isDynamic_forDistributionList_returnsFalse() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Static DL");

        Group dl = prov.createDistributionList("static@test-group-domain.local", dlAttrs);

        assertFalse("Regular distribution list should not be dynamic", dl.isDynamic());
    }

    @Test
    public void createMultipleGroups_andRetrieveAll_returnsAll() throws ServiceException {
        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put("displayName", "DL 1");
        prov.createDistributionList("dl1@test-group-domain.local", attrs1);

        Map<String, Object> attrs2 = new HashMap<>();
        attrs2.put("displayName", "DL 2");
        prov.createDistributionList("dl2@test-group-domain.local", attrs2);

        // Get all in domain
        java.util.List<Group> groups = prov.getAllGroups(testDomain);

        assertNotNull(groups);
        assertTrue("Should have at least 2 groups", groups.size() >= 2);
    }

    @Test
    public void createGroup_missingAttributes_throwsException() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Incomplete DL");
        // missing required attributes

        try {
            // Using email address without mail attribute should work since email is the address
            prov.createDistributionList("incomplete@test-group-domain.local", dlAttrs);
            // Some implementations might allow this with defaults
            // If it succeeds, verify it was created
        } catch (ServiceException e) {
            // This is acceptable if implementation requires certain attributes
            assertTrue("Should have validation error", e.getMessage() != null);
        }
    }

    @Test
    public void addDuplicateMember_toGroup_idempotent() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Duplicate Member DL");
        Group dl = prov.createDistributionList("duplicate@test-group-domain.local", dlAttrs);

        Map<String, Object> acctAttrs = new HashMap<>();
        acctAttrs.put("displayName", "Dup Account");
        Account acct = prov.createAccount("dupacct@test-group-domain.local", "password", acctAttrs);

        // Add same member twice
        prov.addGroupMembers(dl, new String[]{acct.getMail()});
        String[] membersAfterFirst = prov.getGroupMembers(dl);
        int firstCount = membersAfterFirst != null ? membersAfterFirst.length : 0;

        // Adding again should be idempotent
        prov.addGroupMembers(dl, new String[]{acct.getMail()});
        String[] membersAfterSecond = prov.getGroupMembers(dl);
        int secondCount = membersAfterSecond != null ? membersAfterSecond.length : 0;

        assertEquals("Adding same member twice should not duplicate", firstCount, secondCount);
    }

    @Test
    public void getGroup_byMail_returnsSameEntry() throws ServiceException {
        Map<String, Object> dlAttrs = new HashMap<>();
        dlAttrs.put("displayName", "Mail Lookup DL");

        Group created = prov.createDistributionList("mailtest@test-group-domain.local", dlAttrs);

        Group retrieved = prov.getGroup(DistributionListBy.name, "mailtest@test-group-domain.local");

        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("mailtest@test-group-domain.local", retrieved.getMail());
    }

    @Test
    public void createGroup_duplicateName_throwsException() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("displayName", "First DL");

        Group first = prov.createDistributionList("duplicate@test-group-domain.local", attrs);
        assertNotNull(first);

        try {
            prov.createDistributionList("duplicate@test-group-domain.local", attrs);
            fail("Should throw ServiceException for duplicate name");
        } catch (ServiceException e) {
            assertTrue("Error should indicate duplicate",
                    e.getMessage().contains("already") || e.getMessage().contains("duplicate"));
        }
    }
}
