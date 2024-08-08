/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.*;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;

import static org.junit.Assert.*;

public class TestLdapProvAttrCallback extends LdapTest {

    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName(), null);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }

    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }

    private Account createAccount(String localPart, Map<String, Object> attrs)
    throws Exception {
        return provUtil.createAccount(localPart, domain, attrs);
    }

    private void deleteAccount(Account acct) throws Exception {
        provUtil.deleteAccount(acct);
    }

    private DistributionList createDistributionList(String localpart) throws Exception {
        return createDistributionList(localpart, null);
    }

    private DistributionList createDistributionList(String localPart, Map<String, Object> attrs)
    throws Exception {
        return provUtil.createDistributionList(localPart, domain, attrs);
    }

    private DynamicGroup createDynamicGroup(String localPart, Map<String, Object> attrs)
    throws Exception {
        return provUtil.createDynamicGroup(localPart, domain, attrs);
    }

    private DynamicGroup createDynamicGroup(String localPart)
    throws Exception {
        return createDynamicGroup(localPart, null);
    }

    private void deleteDynamicGroup(DynamicGroup group) throws Exception {
        provUtil.deleteDynamicGroup(group);
    }

    private DynamicGroup refresh(DynamicGroup group) throws Exception {
        prov.flushCache(CacheEntryType.group, null);
        group = (DynamicGroup) prov.getGroup(Key.DistributionListBy.id, group.getId());
        assertNotNull(group);
        return group;
    }

    private Domain createDomain(String subDomainSegment, Map<String, Object> attrs)
    throws Exception {
        return provUtil.createDomain(subDomainSegment + "." + baseDomainName(), attrs);
    }

    @Test
    public void accountStatus() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart("account"));
        Account acct = createAccount(ACCT_NAME_LOCALPART);

        String ALIAS_LOCALPART_1 = Names.makeAliasNameLocalPart(genAcctNameLocalPart("alias-1"));
        String ALIAS_NAME_1 = TestUtil.getAddress(ALIAS_LOCALPART_1, domain.getName()).toLowerCase();
        String ALIAS_LOCALPART_2 = Names.makeAliasNameLocalPart(genAcctNameLocalPart("alias-2"));
        String ALIAS_NAME_2 = TestUtil.getAddress(ALIAS_LOCALPART_2, domain.getName()).toLowerCase();

        prov.addAlias(acct, ALIAS_NAME_1);
        prov.addAlias(acct, ALIAS_NAME_2);

        String DL_NAME_LOCALPART = Names.makeDLNameLocalPart("accountStatus-dl");
        DistributionList dl = createDistributionList(DL_NAME_LOCALPART);

        dl.addMembers(new String[] {acct.getName(), ALIAS_NAME_1, ALIAS_NAME_2} );

        prov.flushCache(CacheEntryType.account, null);
        prov.flushCache(CacheEntryType.group, null);

        Set<String> allMembers = dl.getAllMembersSet();
        assertEquals(3, allMembers.size());
        assertTrue(allMembers.contains(acct.getName()));
        assertTrue(allMembers.contains(ALIAS_NAME_1));
        assertTrue(allMembers.contains(ALIAS_NAME_2));

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, ZAttrProvisioning.AccountStatus.closed.name());
        prov.modifyAttrs(acct, attrs);

        prov.flushCache(CacheEntryType.account, null);
        prov.flushCache(CacheEntryType.group, null);

        dl = prov.get(Key.DistributionListBy.id, dl.getId());

        allMembers = dl.getAllMembersSet();
        assertEquals(0, allMembers.size());

        deleteAccount(acct);
    }

    private void verifyIsNotACLGroup(DynamicGroup group, String expectedMemberURL) {
        assertEquals(ProvisioningConstants.FALSE, group.getAttr(Provisioning.A_zimbraIsACLGroup));
        assertEquals(expectedMemberURL, group.getAttr(Provisioning.A_memberURL));
    }

    @Test
    public void zimbraIsACLGroupAndMemberURLCreate() throws Exception {
        String SOME_URL = "blah";

        Map<String, Object> attrs = Maps.newHashMap();
        boolean caughtException;
        DynamicGroup group;

        // 1. specify memberURL and set zimbraIsACLGroup to false -> OK
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup(genGroupNameLocalPart("1"), attrs);
        verifyIsNotACLGroup(group, SOME_URL);
        deleteDynamicGroup(group);

        // 2. specify memberURL and set zimbraIsACLGroup to true -> FAIL
        caughtException = false;
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        try {
            group = createDynamicGroup(genGroupNameLocalPart("2"), attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);


        // 3. specify memberURL without setting zimbraIsACLGroup -> FAIL
        caughtException = false;
        attrs.clear();
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        try {
            group = createDynamicGroup(genGroupNameLocalPart("3"), attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
    }

    @Test
    public void zimbraIsACLGroupAndMemberURLModify() throws Exception {
        String SOME_URL = "blah";
        String SOME_URL_2 = "blah blah";

        Map<String, Object> attrs = Maps.newHashMap();
        boolean caughtException;
        DynamicGroup group;

        group = createDynamicGroup(genGroupNameLocalPart("3"));
        attrs.clear();
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        caughtException = false;
        try {
            prov.modifyAttrs(group, attrs, true);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
        deleteDynamicGroup(group);

        group = createDynamicGroup(genGroupNameLocalPart("4"));
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        caughtException = false;
        try {
            prov.modifyAttrs(group, attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
        deleteDynamicGroup(group);

        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup(genGroupNameLocalPart("8"), attrs);
        attrs.clear();
        attrs.put(Provisioning.A_memberURL, SOME_URL_2);
        prov.modifyAttrs(group, attrs, true);
        verifyIsNotACLGroup(group, SOME_URL_2);
        deleteDynamicGroup(group);

        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        group = createDynamicGroup(genGroupNameLocalPart("9"), attrs);
        attrs.clear();
        attrs.put(Provisioning.A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_memberURL, SOME_URL);
        caughtException = false;
        try {
            prov.modifyAttrs(group, attrs, true);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            } else {
                throw e;
            }
        }
        assertTrue(caughtException);
        deleteDynamicGroup(group);
    }

    @Test
    public void authMech() throws Exception {
        // good mech
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraAuthMech, "custom");
        Domain testDomain = createDomain("authMech", attrs);

        // bad mech
        attrs.clear();
        attrs.put(Provisioning.A_zimbraAuthMech, "bad");
        boolean caughtException = false;
        try {
            prov.modifyAttrs(testDomain, attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
    }

}
