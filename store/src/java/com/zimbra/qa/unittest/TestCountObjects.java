/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015, 2016 Synacor, Inc.
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
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.qa.unittest.prov.soap.SoapTest;
import com.zimbra.soap.admin.message.CountObjectsRequest;
import com.zimbra.soap.admin.message.CountObjectsResponse;
import com.zimbra.soap.admin.type.CountObjectsType;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.DomainSelector.DomainBy;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.TargetBy;

public class TestCountObjects extends TestCase {
    private final Provisioning mProv = Provisioning.getInstance();
    private static String PASSWORD = "test123";
    private static String DOMAIN_ADMIN_USER = "domaintestadmin";
    private static String ROGUE_ADMIN_USER = "roguetestadmin";
    private static String DELEGATED_ADMIN_USER = "delegatedtestadmin";
    private final String DOMAIN_NAME = "testcountobjects.com";
    private final String ANOTHER_DOMAIN_NAME = "onlyrelated.com";
    private String DOMAIN_ADMIN_USER_EMAIL;
    private String DELEGATED_ADMIN_USER_EMAIL;
    private String ROGUE_ADMIN_USER_EMAIL;
    private final ArrayList<String> testAccountIDs = new ArrayList<String>();
    private final ArrayList<String> testDLIDs = new ArrayList<String>();
    private final ArrayList<String> testDomainIDs = new ArrayList<String>();
    private final ArrayList<String> testCOSIDs = new ArrayList<String>();

    @Override
    @Before
    public void setUp() throws Exception {

        // create domain
        Map<String, Object> attrs = new HashMap<String, Object>();
        Domain domain = mProv.createDomain(DOMAIN_NAME, attrs);
        assertNotNull(domain);
        testDomainIDs.add(domain.getId());

        attrs = new HashMap<String, Object>();
        Domain anotherDomain = mProv.createDomain(ANOTHER_DOMAIN_NAME, attrs);
        assertNotNull(anotherDomain);
        testDomainIDs.add(anotherDomain.getId());

        // create some accounts
        for (int i = 0; i < 10; i++) {
            Map<String, Object> acctAttrs = new HashMap<String, Object>();
            Account acct = mProv.createAccount("count-test-acc" + i + "@"
                    + DOMAIN_NAME, PASSWORD, acctAttrs);
            assertNotNull(acct);
            testAccountIDs.add(acct.getId());
            mProv.addAlias(acct, "count-test-alias" + i + "@" + DOMAIN_NAME);
        }

        // create some DLs
        for (int i = 0; i < 10; i++) {
            Map<String, Object> acctAttrs = new HashMap<String, Object>();
            DistributionList dl = mProv.createDistributionList("count-test-DL"
                    + i + "@" + DOMAIN_NAME, acctAttrs);
            assertNotNull(dl);
            testDLIDs.add(dl.getId());
        }

        // create some COSes
        for (int i = 0; i < 10; i++) {
            Map<String, Object> cosAttrs = new HashMap<String, Object>();
            Cos cos = mProv.createCos("count-test-cos" + i + "-" + DOMAIN_NAME,
                    cosAttrs);
            assertNotNull(cos);
            testCOSIDs.add(cos.getId());
        }

        // create domain admin account
        DOMAIN_ADMIN_USER_EMAIL = DOMAIN_ADMIN_USER + "@" + DOMAIN_NAME;
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount,
                ProvisioningConstants.TRUE);
        Account acct = mProv.createAccount(DOMAIN_ADMIN_USER_EMAIL, PASSWORD,
                attrs);
        assertNotNull(acct);
        mProv.grantRight(TargetType.domain.getCode(), TargetBy.name,
                DOMAIN_NAME,
                com.zimbra.cs.account.accesscontrol.GranteeType.GT_USER
                        .getCode(), GranteeBy.name, DOMAIN_ADMIN_USER_EMAIL,
                null, RightConsts.RT_domainAdminRights, null);
        testAccountIDs.add(acct.getId());

        // create delegated admin account
        DELEGATED_ADMIN_USER_EMAIL = DELEGATED_ADMIN_USER + "@" + DOMAIN_NAME;
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount,
          ProvisioningConstants.TRUE);
        acct = mProv.createAccount(DELEGATED_ADMIN_USER_EMAIL, PASSWORD,
          attrs);
        assertNotNull(acct);
        mProv.grantRight(TargetType.domain.getCode(), TargetBy.name,
          ANOTHER_DOMAIN_NAME,
          com.zimbra.cs.account.accesscontrol.GranteeType.GT_USER
            .getCode(), GranteeBy.name, DELEGATED_ADMIN_USER_EMAIL,
          null, RightConsts.RT_domainAdminRights, null);
        testAccountIDs.add(acct.getId());

        // create delegated admin account that does not have a permission to
        // count accounts, but has permission to count distribution lists
        ROGUE_ADMIN_USER_EMAIL = ROGUE_ADMIN_USER + "@" + DOMAIN_NAME;
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount,
                ProvisioningConstants.TRUE);
        acct = mProv.createAccount(ROGUE_ADMIN_USER_EMAIL, PASSWORD, attrs);
        assertNotNull(acct);
        mProv.grantRight(TargetType.domain.getCode(), TargetBy.name,
                DOMAIN_NAME,
                com.zimbra.cs.account.accesscontrol.GranteeType.GT_USER
                        .getCode(), GranteeBy.name, ROGUE_ADMIN_USER_EMAIL,
                null, RightConsts.RT_countDistributionList, null);
        testAccountIDs.add(acct.getId());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        for (String ID : testAccountIDs) {
            mProv.deleteAccount(ID);
        }
        for (String ID : testDLIDs) {
            mProv.deleteDistributionList(ID);
        }
        for (String ID : testCOSIDs) {
            mProv.deleteCos(ID);
        }
        for (String ID : testDomainIDs) {
            mProv.deleteDomain(ID);
        }
    }

    @Test
    public void testDomainAdmin() throws Exception {
        try {
            SoapTransport transport = SoapTest.authAdmin(DOMAIN_ADMIN_USER_EMAIL,
                    PASSWORD);
            // count domains
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                        CountObjectsType.domain);
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                fail("should not be able to count domains");
            } catch (SoapFaultException e) {
                assertEquals("should not be able to count domains",
                        ServiceException.PERM_DENIED, e.getCode());
            }

            // count COSes
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                        CountObjectsType.cos);
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                fail("should not be able to count COSes");
            } catch (SoapFaultException e) {
                assertEquals("should not be able to count COSes",
                        ServiceException.PERM_DENIED, e.getCode());
            }

            // count user accounts
            CountObjectsRequest req = new CountObjectsRequest(
                    CountObjectsType.userAccount);
            CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one account", resp.getNum() > 0);
            assertEquals("object type in response should be 'userAccount'", "userAccount",resp.getType());

            // count accounts
            req = new CountObjectsRequest(CountObjectsType.account);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one account", resp.getNum() > 0);
            assertEquals("object type in response should be 'account'", "account",resp.getType());

            // count distribution lists
            req = new CountObjectsRequest(CountObjectsType.dl);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one distribution list",
                    resp.getNum() > 0);
            assertEquals("object type in response should be 'dl'", "dl",resp.getType());

            // count aliases without a domain filter
            req = new CountObjectsRequest(CountObjectsType.alias);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one alias", resp.getNum() > 0);
            assertEquals("object type in response should be 'alias'", "alias",resp.getType());

            // count user accounts with domain filter
            req = new CountObjectsRequest(CountObjectsType.userAccount);
            req.addDomain(new DomainSelector(DomainBy.name, DOMAIN_NAME));
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one userAccount",
                    resp.getNum() > 0);
            assertEquals("object type in response should be 'userAccount'", "userAccount",resp.getType());

            // count accounts with domain filter
            req = new CountObjectsRequest(CountObjectsType.account, new DomainSelector(DomainBy.name, DOMAIN_NAME));
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one account", resp.getNum() > 0);
            assertEquals("object type in response should be 'account'", "account",resp.getType());

            // count DLs with domain filter
            req = new CountObjectsRequest(CountObjectsType.dl, new DomainSelector(DomainBy.name, DOMAIN_NAME));
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one distribution list",
                    resp.getNum() > 0);
            assertEquals("object type in response should be 'dl'", "dl",resp.getType());

            // count aliases with domain filter
            req = new CountObjectsRequest(CountObjectsType.alias, new DomainSelector(DomainBy.name, DOMAIN_NAME));
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one alias", resp.getNum() > 0);
            assertEquals("object type in response should be 'alias'", "alias",resp.getType());

        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testOnlyRelated() throws Exception
    {
        try {
            SoapTransport transport = SoapTest.authAdmin(DELEGATED_ADMIN_USER_EMAIL,
              PASSWORD);
            // count only related domains
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                  CountObjectsType.domain);
                req.setOnlyRelated(true);
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);

                assertTrue("should have exactly one account", resp.getNum() == 1);
                assertEquals("object type in response should be 'domain'", "domain",resp.getType());
            } catch (SoapFaultException e) {
                fail("should not be fail");
            }

            // count domain without necessary right
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                  CountObjectsType.domain);
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                fail("should not be able to count domains");
            } catch (SoapFaultException e) {
                assertEquals(ServiceException.PERM_DENIED, e.getCode());
            }
            // count domain with countDomain right
            try {
                mProv.grantRight(TargetType.global.getCode(), null,
                  null,
                  com.zimbra.cs.account.accesscontrol.GranteeType.GT_USER
                    .getCode(), GranteeBy.name, DELEGATED_ADMIN_USER_EMAIL,
                  null, RightConsts.RT_countDomain, null);

                CountObjectsRequest req = new CountObjectsRequest(
                  CountObjectsType.domain);
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);

                assertTrue("should have at least 2 domain", resp.getNum() > 1);
                assertEquals("object type in response should be 'domain'", "domain",resp.getType());
            } catch (SoapFaultException e) {
                fail("should not be fail");
            }
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testRogueAdmin() throws Exception {
        try {
            SoapTransport transport = SoapTest.authAdmin(ROGUE_ADMIN_USER_EMAIL,
                    PASSWORD);
            // count domains
            try {
                CountObjectsRequest req = new CountObjectsRequest(CountObjectsType.domain);
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                fail("should not be able to count domains");
            } catch (SoapFaultException e) {
                assertEquals(ServiceException.PERM_DENIED, e.getCode());
            }

            // count user accounts
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                        CountObjectsType.userAccount, new DomainSelector(DomainBy.name, DOMAIN_NAME));
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                fail("should not be able to count accounts");
            } catch (SoapFaultException e) {
                assertEquals(ServiceException.PERM_DENIED, e.getCode());
            }

            // count accounts
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                        CountObjectsType.account, new DomainSelector(DomainBy.name, DOMAIN_NAME));
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                fail("should not be able to count accounts");
            } catch (SoapFaultException e) {
                assertEquals(ServiceException.PERM_DENIED, e.getCode());
            }

            // count distribution lists
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                        CountObjectsType.dl);
                req.addDomain(new DomainSelector(DomainBy.name, DOMAIN_NAME));
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                assertTrue("should have at least one distribution list",
                        resp.getNum() > 0);
            } catch (ServiceException e) {
                fail("should not be throwing exception here "
                        + e.getLocalizedMessage());
            }

            // count aliases
            try {
                CountObjectsRequest req = new CountObjectsRequest(
                        CountObjectsType.alias, new DomainSelector(DomainBy.name, DOMAIN_NAME));
                CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
                fail("should not be able to count aliases");
            } catch (SoapFaultException e) {
                assertEquals(ServiceException.PERM_DENIED, e.getCode());
            }
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testSuperAdmin() {
        try {
            SoapTransport transport = TestUtil.getAdminSoapTransport();
            // count domains
            CountObjectsRequest req = new CountObjectsRequest(
                    CountObjectsType.domain);
            CountObjectsResponse resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one domain", resp.getNum() > 0);

            // count user accounts
            req = new CountObjectsRequest(CountObjectsType.userAccount);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one userAccount",
                    resp.getNum() > 0);

            // count accounts
            req = new CountObjectsRequest(CountObjectsType.account);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one account", resp.getNum() > 0);

            // count DLs
            req = new CountObjectsRequest(CountObjectsType.dl);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one distribution list",
                    resp.getNum() > 0);

            // count aliases
            req = new CountObjectsRequest(CountObjectsType.alias);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one alias", resp.getNum() > 0);

            // count COSes
            req = new CountObjectsRequest(CountObjectsType.cos);
            resp = SoapTest.invokeJaxb(transport, req);
            assertTrue("should have at least one COS", resp.getNum() > 0);
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }
}
