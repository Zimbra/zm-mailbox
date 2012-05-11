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

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.*;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.qa.unittest.TestUtil;

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
        Cleanup.deleteAll(baseDomainName());
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

    /* ================================================
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

    @Test
    public void memberViaSlapdOverlay() throws Exception {
        DynamicGroup group = createDynamicGroup(genGroupNameLocalPart());

        // TODO
    }


}
