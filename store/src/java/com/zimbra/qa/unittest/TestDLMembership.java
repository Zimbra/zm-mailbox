/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.prov.soap.SoapTest;
import com.zimbra.soap.account.message.GetDistributionListMembersRequest;
import com.zimbra.soap.account.message.GetDistributionListMembersResponse;
import com.zimbra.soap.admin.message.AddAccountAliasRequest;
import com.zimbra.soap.admin.message.AddAccountAliasResponse;
import com.zimbra.soap.admin.message.AddDistributionListMemberRequest;
import com.zimbra.soap.admin.message.AddDistributionListMemberResponse;
import com.zimbra.soap.admin.message.GetAccountMembershipRequest;
import com.zimbra.soap.admin.message.GetAccountMembershipResponse;
import com.zimbra.soap.admin.message.RemoveDistributionListMemberRequest;
import com.zimbra.soap.admin.message.RemoveDistributionListMemberResponse;
import com.zimbra.soap.admin.type.DLInfo;
import com.zimbra.soap.type.AccountSelector;

public class TestDLMembership extends TestCase {
    private static String TEST_USER = "testuser1";
    private static String TEST_USER2 = "testuser2";
    private static String TEST_ALIAS = "testalias1";
    private static String TEST_DL = "testdl1";
    private static String TEST_DL2 = "estdl1";

    private static Account testUser;
    private static Account testUser2;
    private static DistributionList testDL;
    private static DistributionList testDL2;

    @Override
    @Before
    public void setUp() throws Exception {
        cleanup();
        //create a user with an alias
        testUser = TestUtil.createAccount(TEST_USER);
        testUser2 = TestUtil.createAccount(TEST_USER2);
        //create DL
        testDL = TestUtil.createDistributionList(TEST_DL);
        testDL2 = TestUtil.createDistributionList(TEST_DL2);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private void cleanup() throws Exception {
        TestUtil.deleteDistributionList(TEST_DL);
        TestUtil.deleteAccount(TEST_USER);
        TestUtil.deleteDistributionList(TEST_DL2);
        TestUtil.deleteAccount(TEST_USER2);
    }
    @Test
    public void testAddMemberByName() {
        SoapTransport transport;
        try {
            transport = TestUtil.getAdminSoapTransport();
            //add a member by account name
            AddDistributionListMemberResponse addDLMemberResp = SoapTest.invokeJaxb(transport, new AddDistributionListMemberRequest(testDL.getId(),
                    Collections.singleton(testUser.getName())));
            assertNotNull("AddDistributionListMemberResponse cannot be null", addDLMemberResp);

            //verify that account is a member of the DL
            ArrayList<DistributionList> result = new ArrayList<DistributionList>();
            GetAccountMembershipResponse resp = SoapTest.invokeJaxb(transport,
                new GetAccountMembershipRequest(AccountSelector.fromName(TEST_USER)));
            assertNotNull("GetAccountMembershipRequest cannot be null", resp);
            List<DLInfo> dlInfoList = resp.getDlList();
            assertTrue("Account is not a member of any DLs",dlInfoList.size()>0);
            assertEquals("Account should be a member of the test DL only", dlInfoList.get(0).getName(),testDL.getName());

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetDistributionListMembers() {
        SoapTransport transport;
        try {
            transport = TestUtil.getAdminSoapTransport();
            AddDistributionListMemberResponse addDLMemberResp = SoapTest.invokeJaxb(transport, new AddDistributionListMemberRequest(testDL.getId(),
                    Collections.singleton(testUser.getName())));
            AddDistributionListMemberResponse addDLMemberResp2 = SoapTest.invokeJaxb(transport, new AddDistributionListMemberRequest(testDL2.getId(),
                Collections.singleton(testUser2.getName())));
            assertNotNull("AddDistributionListMemberResponse cannot be null", addDLMemberResp);

            //Verify GetDistributionListMembersRequest returns correct members.
            SoapTransport transportAccount = TestUtil.authUser(testUser.getName(), TestUtil.DEFAULT_PASSWORD);
            GetDistributionListMembersResponse resp = SoapTest.invokeJaxb(transportAccount,
                new GetDistributionListMembersRequest(0, 0, testDL2.getName()));
            List<String> dlInfoList = resp.getDlMembers();
            assertFalse("Unexepcted member present",dlInfoList.contains(testUser.getName()));
            assertTrue("DL member not present", dlInfoList.contains(testUser2.getName()));

            resp = SoapTest.invokeJaxb(transportAccount,
                new GetDistributionListMembersRequest(0, 0, testDL.getName()));
            dlInfoList = resp.getDlMembers();
            assertFalse("Unexepcted member present",dlInfoList.contains(testUser2.getName()));
            assertTrue("DL member not present", dlInfoList.contains(testUser.getName()));
         } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testAddMemberByAlias() {
        SoapTransport transport;
        try {
            transport = TestUtil.getAdminSoapTransport();
            //add an alias to the account
            AddAccountAliasResponse addAliasResp = SoapTest.invokeJaxb(transport,new AddAccountAliasRequest(testUser.getId(),TestUtil.getAddress(TEST_ALIAS), false));
            assertNotNull("AddAccountAliasResponse cannot be null", addAliasResp);
            Account acct = Provisioning.getInstance().getAccount(testUser.getId());
            assertNotNull(acct);
            assertNotNull("account's aliases are null", acct.getAliases());
            assertEquals("account has no aliases", acct.getAliases().length,1);

            //add a member by alias
            AddDistributionListMemberResponse addDLMemberResp = SoapTest.invokeJaxb(transport, new AddDistributionListMemberRequest(testDL.getId(),
                    Collections.singleton(TestUtil.getAddress(TEST_ALIAS))));
            assertNotNull("AddDistributionListMemberResponse cannot be null", addDLMemberResp);

            //verify that account is a member of the DL
            GetAccountMembershipResponse resp = SoapTest.invokeJaxb(transport,
                new GetAccountMembershipRequest(AccountSelector.fromName(TEST_USER)));
            assertNotNull("GetAccountMembershipRequest cannot be null", resp);
            List<DLInfo> dlInfoList = resp.getDlList();
            assertTrue("Account is not a member of any DLs",dlInfoList.size()>0);
            assertEquals("Account should be a member of the test DL only", dlInfoList.get(0).getName(),testDL.getName());

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testRemoveMemberByAlias() {
        SoapTransport transport;
        try {
            transport = TestUtil.getAdminSoapTransport();
            //add an alias to the account
            AddAccountAliasResponse addAliasResp = SoapTest.invokeJaxb(transport,new AddAccountAliasRequest(testUser.getId(),TestUtil.getAddress(TEST_ALIAS), false));
            assertNotNull("AddAccountAliasResponse cannot be null", addAliasResp);
            Account acct = Provisioning.getInstance().getAccount(testUser.getId());
            assertNotNull(acct);
            assertNotNull("account's aliases are null", acct.getAliases());
            assertEquals("account has no aliases", acct.getAliases().length,1);

            //add account to DL by alias
            AddDistributionListMemberResponse addDLMemberResp = SoapTest.invokeJaxb(transport, new AddDistributionListMemberRequest(testDL.getId(),
                    Collections.singleton(TestUtil.getAddress(TEST_ALIAS))));
            assertNotNull("AddDistributionListMemberResponse cannot be null", addDLMemberResp);

            //verify that account is a member of the DL
            ArrayList<DistributionList> result = new ArrayList<DistributionList>();
            GetAccountMembershipResponse resp = SoapTest.invokeJaxb(transport,
                new GetAccountMembershipRequest(AccountSelector.fromName(TEST_USER)));
            assertNotNull("GetAccountMembershipRequest cannot be null", resp);
            List<DLInfo> dlInfoList = resp.getDlList();
            assertTrue("Account is not a member of any DLs",dlInfoList.size()>0);
            assertEquals("Account should be a member of the test DL only", dlInfoList.get(0).getName(),testDL.getName());

            //remove the account's alias from the DL
            RemoveDistributionListMemberResponse rdlmresp = SoapTest.invokeJaxb(transport,
                    new RemoveDistributionListMemberRequest(testDL.getId(), Arrays.asList(new String[] {TestUtil.getAddress(TEST_ALIAS)})));
            assertNotNull("RemoveDistributionListMemberRequest cannot be null", rdlmresp);

            //verify that account is NOT a member of the DL anymore
            result = new ArrayList<DistributionList>();
            resp = SoapTest.invokeJaxb(transport,
                new GetAccountMembershipRequest(AccountSelector.fromName(TEST_USER)));
            assertNotNull("GetAccountMembershipRequest cannot be null", resp);
            dlInfoList = resp.getDlList();
            assertTrue("Account should not be a member of any DLs",dlInfoList.size() == 0);

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testRemoveMemberByName() {
        SoapTransport transport;
        try {
            transport = TestUtil.getAdminSoapTransport();
            //add an alias to the account
            AddAccountAliasResponse addAliasResp = SoapTest.invokeJaxb(transport,new AddAccountAliasRequest(testUser.getId(),TestUtil.getAddress(TEST_ALIAS),false));
            assertNotNull("AddAccountAliasResponse cannot be null", addAliasResp);
            Account acct = Provisioning.getInstance().getAccount(testUser.getId());
            assertNotNull(acct);
            assertNotNull("account's aliases are null", acct.getAliases());
            assertEquals("account has no aliases", acct.getAliases().length,1);

            //add account to DL by alias
            AddDistributionListMemberResponse addDLMemberResp = SoapTest.invokeJaxb(transport, new AddDistributionListMemberRequest(testDL.getId(),
                    Collections.singleton(TestUtil.getAddress(TEST_ALIAS))));
            assertNotNull("AddDistributionListMemberResponse cannot be null", addDLMemberResp);

            //verify that account is a member of the DL
            ArrayList<DistributionList> result = new ArrayList<DistributionList>();
            GetAccountMembershipResponse resp = SoapTest.invokeJaxb(transport,
                new GetAccountMembershipRequest(AccountSelector.fromName(TEST_USER)));
            assertNotNull("GetAccountMembershipRequest cannot be null", resp);
            List<DLInfo> dlInfoList = resp.getDlList();
            assertTrue("Account is not a member of any DLs",dlInfoList.size()>0);
            assertEquals("Account should be a member of the test DL only", dlInfoList.get(0).getName(),testDL.getName());

            //remove the account's alias from the DL
            RemoveDistributionListMemberResponse rdlmresp = SoapTest.invokeJaxb(transport,
                    new RemoveDistributionListMemberRequest(testDL.getId(), null, Arrays.asList(new String[] {TestUtil.getAddress(TEST_USER)})));
            assertNotNull("RemoveDistributionListMemberRequest cannot be null", rdlmresp);

            //verify that account is NOT a member of the DL anymore
            result = new ArrayList<DistributionList>();
            resp = SoapTest.invokeJaxb(transport,
                new GetAccountMembershipRequest(AccountSelector.fromName(TEST_USER)));
            assertNotNull("GetAccountMembershipRequest cannot be null", resp);
            dlInfoList = resp.getDlList();
            assertTrue("Account should not be a member of any DLs",dlInfoList.size() == 0);

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

}
