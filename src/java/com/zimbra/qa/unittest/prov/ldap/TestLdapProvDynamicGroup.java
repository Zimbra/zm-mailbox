/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.ldap;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.*;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.GroupMemberEmailAddrs;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.Provisioning.MemberOf;
import com.zimbra.cs.account.ldap.entry.LdapAccount;
import com.zimbra.cs.account.ldap.entry.LdapDynamicGroup;
import com.zimbra.cs.gal.GalGroupMembers;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Verify;

public class TestLdapProvDynamicGroup extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    private static Sequencer seq;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName(), null);
        seq = new Sequencer();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        // Cleanup.deleteAll(baseDomainName());  // this makes slapd crazy for some reason
    }

    private DynamicGroup createDynamicGroup(String localPart) throws Exception {
        return provUtil.createDynamicGroup(localPart, domain);
    }

    private DynamicGroup createDynamicGroup(String localPart, Map<String, Object> attrs)
    throws Exception {
        return provUtil.createDynamicGroup(localPart, domain, attrs);
    }

    private void deleteDynamicGroup(DynamicGroup group) throws Exception {
        provUtil.deleteDynamicGroup(group);
    }

    /*
     * basic creation test
     */
    @Test
    public void createDynamicGroup() throws Exception {
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart());
        assertEquals(true, group.isIsACLGroup());

        // make sure the group has a home server
        Server homeServer = group.getServer();
        assertNotNull(homeServer);

        deleteDynamicGroup(group);
    }

    /*
     * deletion test
     */
    @Test
    public void deleteDynamicGroup() throws Exception {
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart());
        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);

        // add a member
        prov.addGroupMembers(group, new String[]{acct.getName()});

        assertTrue(acct.getMultiAttrSet(Provisioning.A_zimbraMemberOf).contains(group.getId()));

        // delete the group
        deleteDynamicGroup(group);

        // reget the acct
        acct = prov.get(AccountBy.id, acct.getId());
        assertFalse(acct.getMultiAttrSet(Provisioning.A_zimbraMemberOf).contains(group.getId()));
    }

    /*
     * rename test
     */
    @Test
    public void renameDynamicGroup() throws Exception {
        String origLocalpart = genGroupNameLocalPart();
        Group group = createDynamicGroup(origLocalpart);

        String groupId = group.getId();

        String newLocalpart = origLocalpart + "-new";
        String newName = TestUtil.getAddress(newLocalpart, domain.getName());

        prov.renameGroup(groupId, newName);

        group = prov.getGroup(DistributionListBy.name, newName);
        assertEquals(groupId, group.getId());
    }


    /*
     * ================================================
     * Testing zimbraIsACLGroup and memeberURL settings
     * ================================================
     */

    /*
     * On create: if memeberURL is specified, zimbraIsACLGroup must also be specified
     * an set to FALSE.
     */
    @Test
    public void zimbraIsACLGroup_and_memeberURL_1() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_memberURL, "blah");
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart(seq), attrs);
        assertEquals(false, group.isIsACLGroup());

        String errCode = null;
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_memberURL, "blah");
        try {
            createDynamicGroup(genGroupNameLocalPart(seq), attrs);
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);

        errCode = null;
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_memberURL, "blah");
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        try {
            createDynamicGroup(genGroupNameLocalPart(seq), attrs);
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);
    }

    /*
     * On create: if memeberURL is not specified, zimbraIsACLGroup must be either not
     * specified, or set to TRUE.
     */
    @Test
    public void zimbraIsACLGroup_and_memeberURL_2() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();

        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart(seq), attrs);
        assertEquals(true, group.isIsACLGroup());

        String errCode = null;
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        try {
            createDynamicGroup(genGroupNameLocalPart(seq), attrs);
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);
    }

    /*
     * On modify: zimbraIsACLGroup is not mutable, regardless whetyher memberURL
     * is specified.
     */
    @Test
    public void zimbraIsACLGroup_and_memeberURL_3() throws Exception {
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart());
        Map<String, Object> attrs;

        String errCode = null;
        try {
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
            prov.modifyAttrs(group, attrs, true);
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);

        errCode = null;
        try {
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
            attrs.put(Provisioning.A_memberURL, "blah");
            prov.modifyAttrs(group, attrs, true);
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);
    }

    /*
     * On modify: memeberURL cannot be modified if zimbraIsACLGroup is TRUE.
     */
    @Test
    public void zimbraIsACLGroup_and_memeberURL_4() throws Exception {
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart());
        Map<String, Object> attrs;

        String errCode = null;
        try {
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_memberURL, "blah");
            prov.modifyAttrs(group, attrs, true);
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);
    }

    /*
     * On modify: memeberURL can be modified if zimbraIsACLGroup is FALSE.
     */
    @Test
    public void zimbraIsACLGroup_and_memeberURL_5() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_memberURL, "foo");
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart(seq), attrs);
        assertEquals(false, group.isIsACLGroup());

        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_memberURL, "bar");
        prov.modifyAttrs(group, attrs, true);
        assertEquals("bar", group.getMemberURL());
    }

    /*
     * Test member attr, which is populated by OpenLDAP dyngroup overlay
     */
    @Test
    public void memberAttrViaSlapdOverlay() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(SkipTestReason.DYNAMIC_GROUP_OVERLAY);

        Group group = createDynamicGroup(genGroupNameLocalPart());

        Account acct1 = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        Account acct2 = provUtil.createAccount(genAcctNameLocalPart("2"), domain);

        prov.addGroupMembers(group, new String[]{
                acct1.getName(), acct2.getName()});

        group = prov.getGroup(DistributionListBy.id, group.getId());
        String[] memberDNs = group.getMultiAttr(Provisioning.A_member);

        Set<String> expected = Sets.newHashSet(
                ((LdapAccount)acct1).getDN(),
                ((LdapAccount)acct2).getDN(),
                "cn=external," + ((LdapDynamicGroup)group).getDN()); // LdapProvisioning.DYNAMIC_GROUP_STATIC_UNIT_NAME

        Verify.verifyEquals(expected, memberDNs);
    }

    /*
     * Test add members
     */
    @Test
    public void addMembersBasic() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());

        Account acct1 = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        Account acct2 = provUtil.createAccount(genAcctNameLocalPart("2"), domain);
        String extAddr1 = "user1@external.com";
        String extAddr2 = "user2@external.com";

        prov.addGroupMembers(group, new String[]{
                acct1.getName(), acct2.getName(), extAddr1, extAddr2});

        String[] members = prov.getGroupMembers(group);

        Set<String> expected = Sets.newHashSet(
                acct1.getName(), acct2.getName(), extAddr1, extAddr2);

        Verify.verifyEquals(expected, members);
    }

    /*
     * Test add invalid email address as member
     */
    @Test
    public void addMembersInvalidAddr() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());

        String addr = "bogus";

        String errCode = null;
        try {
            prov.addGroupMembers(group, new String[]{addr});
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);
    }

    /*
     * Test add existing members
     */
    @Test
    public void addMemberExisting() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());

        Account acct1 = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        Account acct2 = provUtil.createAccount(genAcctNameLocalPart("2"), domain);
        String extAddr1 = "user1@external.com";
        String extAddr2 = "user2@external.com";

        Set<String> expected = Sets.newHashSet(
                acct1.getName(), acct2.getName(), extAddr1, extAddr2);

        prov.addGroupMembers(group, new String[]{
                acct1.getName(), acct2.getName(), extAddr1, extAddr2});
        String[] members = prov.getGroupMembers(group);
        Verify.verifyEquals(expected, members);

        // add again, should get back the same members
        prov.addGroupMembers(group, new String[]{
                acct1.getName(), acct2.getName(), extAddr1, extAddr2});
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(expected, members);
    }

    /*
     * Test add internal non-account address as members - not allowed
     */
    @Test
    public void addMembersInternalAddress() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());

        String otherDynGroupLocalPart = genGroupNameLocalPart("other-dynamic");
        Group otherDynamicGroup = createDynamicGroup(otherDynGroupLocalPart);
        Group otherStaticGroup = provUtil.createDistributionList(genGroupNameLocalPart("other-static"), domain);

        String errCode = null;
        try {
            prov.addGroupMembers(group, new String[]{otherDynamicGroup.getName()});
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);

        errCode = null;
        try {
            prov.addGroupMembers(group, new String[]{otherStaticGroup.getName()});
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);

        // dynamic unit of a dynamic group
        errCode = null;
        try {
            String addr = otherDynGroupLocalPart + ".__internal__@" + domain.getName();
            prov.addGroupMembers(group, new String[]{addr});
        } catch (ServiceException e) {
            errCode = e.getCode();
        }
        assertEquals(ServiceException.INVALID_REQUEST, errCode);
    }

    /*
     * Test remove members
     */
    @Test
    public void removeMembersBasic() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());

        Account acct1 = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        Account acct2 = provUtil.createAccount(genAcctNameLocalPart("2"), domain);
        String extAddr1 = "user1@external.com";
        String extAddr2 = "user2@external.com";

        prov.addGroupMembers(group, new String[]{
                acct1.getName(), acct2.getName(), extAddr1, extAddr2});

        String[] members = prov.getGroupMembers(group);

        Set<String> expected = Sets.newHashSet(
                acct1.getName(), acct2.getName(), extAddr1, extAddr2);
        Verify.verifyEquals(expected, members);

        prov.removeGroupMembers(group, new String[]{acct1.getName(), extAddr1});
        members = prov.getGroupMembers(group);
        expected = Sets.newHashSet(acct2.getName(), extAddr2);
        Verify.verifyEquals(expected, members);

        // remove non-existing members, should not throw any exception
        prov.removeGroupMembers(group, new String[]{group.getName()});
        prov.removeGroupMembers(group, new String[]{"bogus"});
    }

    /*
     * Test Provisioning.getGroupMembership
     */
    @Test
    public void getGroupMembership() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());
        Group otherDynamicGroup = createDynamicGroup(genGroupNameLocalPart("other-dynamic"));
        Group otherStaticGroup = provUtil.createDistributionList(genGroupNameLocalPart("other-static"), domain);

        Account acct = provUtil.createAccount(genAcctNameLocalPart(), domain);

        String externalAddr = "user@external.com";
        GuestAccount guestAcct = new GuestAccount(externalAddr, null);

        prov.addGroupMembers(group, new String[]{acct.getName(), externalAddr});
        prov.addGroupMembers(otherDynamicGroup, new String[]{acct.getName(), externalAddr});
        prov.addGroupMembers(otherStaticGroup, new String[]{acct.getName(), externalAddr});

        Set<String> expectedMemberOf = Sets.newHashSet(
                Verify.makeResultStr(group.getId(), Boolean.FALSE),
                Verify.makeResultStr(otherDynamicGroup.getId(), Boolean.FALSE),
                Verify.makeResultStr(otherStaticGroup.getId(), Boolean.FALSE));

        Set<String> expectedIds = Sets.newHashSet(
                Verify.makeResultStr(group.getId()),
                Verify.makeResultStr(otherDynamicGroup.getId()),
                Verify.makeResultStr(otherStaticGroup.getId()));


        /*
         * verify membership for acct
         */
        GroupMembership membership = prov.getGroupMembership(acct, false);
        List<MemberOf> memberOfList = membership.memberOf();
        List<String> groupIdList = membership.groupIds();

        Set<String> actualMemberOf = Sets.newHashSet();
        for (MemberOf memberOf : memberOfList) {
            actualMemberOf.add(Verify.makeResultStr(memberOf.getId(), memberOf.isAdminGroup()));
        }

        Set<String> actualIds = Sets.newHashSet();
        for (String id : groupIdList) {
            actualIds.add(Verify.makeResultStr(id));
        }

        Verify.verifyEquals(expectedMemberOf, actualMemberOf);
        Verify.verifyEquals(expectedIds, actualIds);

        /*
         * verify membership for guest acct
         */
        membership = prov.getGroupMembership(guestAcct, false);
        memberOfList = membership.memberOf();
        groupIdList = membership.groupIds();

        actualMemberOf = Sets.newHashSet();
        for (MemberOf memberOf : memberOfList) {
            actualMemberOf.add(Verify.makeResultStr(memberOf.getId(), memberOf.isAdminGroup()));
        }

        actualIds = Sets.newHashSet();
        for (String id : groupIdList) {
            actualIds.add(Verify.makeResultStr(id));
        }

        Verify.verifyEquals(expectedMemberOf, actualMemberOf);
        Verify.verifyEquals(expectedIds, actualIds);
    }

    /*
     * Test groups members show up correctly in GAL search
     */
    @Test
    public void galSearch() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(SkipTestReason.DN_SUBTREE_MATCH_FILTER);

        Group group = createDynamicGroup(genGroupNameLocalPart());

        Account acct1 = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        Account acct2 = provUtil.createAccount(genAcctNameLocalPart("2"), domain);
        String externalAddr = "user@external.com";

        prov.addGroupMembers(group, new String[]{acct1.getName(), externalAddr});

        Set<String> galGroupMembers = GalGroupMembers.getGroupMembers(group.getName(), acct2);

        Set<String> expected = Sets.newHashSet(acct1.getName(), externalAddr);
        Verify.verifyEquals(expected, galGroupMembers);
    }


    /*
     * Test external address is removed from all dynamic group static units if
     * new groups or group aliases are created/renamed with the same address.
     */
    @Test
    public void externalAddrRemovedOnInternalAddrCreationCreation() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());
        String groupId = group.getId();

        String externalAddr1 = genGroupNameLocalPart("1") + "@" + domain.getName();
        String externalAddr2 = genGroupNameLocalPart("2") + "@" + domain.getName();
        String externalAddr3 = genGroupNameLocalPart("3") + "@" + domain.getName();
        String externalAddr4 = genGroupNameLocalPart("4") + "@" + domain.getName();
        String externalAddr5 = genGroupNameLocalPart("5") + "@" + domain.getName();
        String externalAddr6 = genGroupNameLocalPart("6") + "@" + domain.getName();
        String externalAddr7 = genGroupNameLocalPart("7") + "@" + domain.getName();
        String externalAddr8 = genGroupNameLocalPart("8") + "@" + domain.getName();
        String externalAddr9 = genGroupNameLocalPart("9") + "@" + domain.getName();

        prov.addGroupMembers(group, new String[]{
                externalAddr1, externalAddr2, externalAddr3,
                externalAddr4, externalAddr5, externalAddr6,
                externalAddr7, externalAddr8, externalAddr9});
        String[] members = prov.getGroupMembers(group);

        Verify.verifyEquals(
                Sets.newHashSet(externalAddr1, externalAddr2, externalAddr3, externalAddr4,
                        externalAddr5, externalAddr6, externalAddr7, externalAddr8, externalAddr9),
                members);

        //
        // create a static group
        //
        Group newGroup = provUtil.createGroup(externalAddr1, false);
        // re-get the group, the group instance we got hold of has been removed from cache
        // when member of it was removed, but members are still cached on out instance
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr2, externalAddr3, externalAddr4,
                        externalAddr5, externalAddr6, externalAddr7, externalAddr8, externalAddr9),
                members);

        // add an alias to the new group
        prov.addGroupAlias(newGroup, externalAddr2);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr3, externalAddr4,
                        externalAddr5, externalAddr6, externalAddr7, externalAddr8, externalAddr9),
                members);

        // rename the group
        prov.renameGroup(newGroup.getId(), externalAddr3);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr4,
                        externalAddr5, externalAddr6, externalAddr7, externalAddr8, externalAddr9),
                members);

        //
        // create a dynamic group
        //
        newGroup = provUtil.createGroup(externalAddr4, true);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr5, externalAddr6, externalAddr7, externalAddr8, externalAddr9),
                members);

        // add an alias to the new group
        prov.addGroupAlias(newGroup, externalAddr5);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr6, externalAddr7, externalAddr8, externalAddr9),
                members);

        // rename the group
        prov.renameGroup(newGroup.getId(), externalAddr6);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr7, externalAddr8, externalAddr9),
                members);

        //
        // create an account
        //
        Account newAcct = provUtil.createAccount(externalAddr7);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr8, externalAddr9),
                members);

        // add an alias to the new account
        prov.addAlias(newAcct, externalAddr8);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr9),
                members);

        // rename the account
        prov.renameAccount(newAcct.getId(), externalAddr9);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        Verify.verifyEquals(
                Collections.<String>emptySet(),
                members);
    }

    /*
     * renaming an account should not affect its membership in a dynamic group
     */
    @Test
    public void renameAccount() throws Exception {
        Group group = createDynamicGroup(genGroupNameLocalPart());
        String groupId = group.getId();

        Account acct = provUtil.createAccount(genAcctNameLocalPart("1"), domain);
        String newAddr = TestUtil.getAddress(genAcctNameLocalPart("2"), domain.getName());

        prov.addGroupMembers(group, new String[]{acct.getName(), newAddr});
        String[] members = prov.getGroupMembers(group);
        // ensure groups contains the account
        Verify.verifyEquals(
                Sets.newHashSet(acct.getName(), newAddr),
                members);
        GroupMembership membership = prov.getGroupMembership(acct, false);
        List<String> groupIdList = membership.groupIds();
        // ensure the account is a member of the group
        assertTrue(groupIdList.contains(groupId));

        //
        // rename the account, then make sure the account is still in the group
        //
        prov.renameAccount(acct.getId(), newAddr);
        group = prov.getGroupBasic(DistributionListBy.id, groupId);
        members = prov.getGroupMembers(group);
        // ensure groups contains the account
        Verify.verifyEquals(
                Sets.newHashSet(newAddr),
                members);
        membership = prov.getGroupMembership(acct, false);
        groupIdList = membership.groupIds();
        // ensure the account is a member of the group
        assertTrue(groupIdList.contains(groupId));
    }

    private void testGetMemberAddrs(boolean dynamic) throws Exception {
        /*
         * group has both internal and external members
         */
        Group group = provUtil.createGroup(genGroupNameLocalPart(seq), domain, dynamic);

        Account acct1 = provUtil.createAccount(genAcctNameLocalPart(seq), domain);
        Account acct2 = provUtil.createAccount(genAcctNameLocalPart(seq), domain);
        String externalAddr1 = "user1@external.com";
        String externalAddr2 = "user2@external.com";

        prov.addGroupMembers(group, new String[]{
                acct1.getName(), acct2.getName(), externalAddr1, externalAddr2});
        GroupMemberEmailAddrs addrs = prov.getMemberAddrs(group);
        assertNull(addrs.groupAddr());
        Verify.verifyEquals(
                Sets.newHashSet(acct1.getName(), acct2.getName()),
                addrs.internalAddrs());
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr1, externalAddr2),
                addrs.externalAddrs());

        /*
         * group has only internal members
         */
        group = provUtil.createGroup(genGroupNameLocalPart(seq), domain, dynamic);
        prov.addGroupMembers(group, new String[]{
                acct1.getName(), acct2.getName()});
        addrs = prov.getMemberAddrs(group);
        assertEquals(group.getName(), addrs.groupAddr());
        assertNull(addrs.internalAddrs());
        assertNull(addrs.externalAddrs());

        /*
         * group has only external members
         */
        group = provUtil.createGroup(genGroupNameLocalPart(seq), domain, dynamic);
        prov.addGroupMembers(group, new String[]{
                externalAddr1, externalAddr2});
        addrs = prov.getMemberAddrs(group);
        assertNull(addrs.groupAddr());
        assertNull(addrs.internalAddrs());
        Verify.verifyEquals(
                Sets.newHashSet(externalAddr1, externalAddr2),
                addrs.externalAddrs());

        /*
         * group has no member
         */
        group = provUtil.createGroup(genGroupNameLocalPart(seq), domain, dynamic);
        addrs = prov.getMemberAddrs(group);
        assertEquals(group.getName(), addrs.groupAddr());
        assertNull(addrs.internalAddrs());
        assertNull(addrs.externalAddrs());
    }


    /*
     * Test dispatching group members into internal/external piles
     */
    @Test
    public void getMemberAddrs() throws Exception {
        testGetMemberAddrs(true);
        testGetMemberAddrs(false);
    }

}
