/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link AccountComparator}, comparing real {@link Account} domain objects
 * built through the in-memory MockProvisioning harness across all three compare modes.
 */
public class AccountComparatorTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account account(String name, String id, Map<String, Object> attrs) {
        return new Account(name, id, attrs, new HashMap<String, Object>(), prov);
    }

    @Test
    public void compareByIdDefaultFieldOrdersByZimbraId() throws Exception {
        // Arrange -- field 0 (the default) compares by id. Account.getId() reads the
        // zimbraId attribute (ZAttrAccount override), not the constructor id arg, so the
        // attribute must be set for the comparison to be meaningful.
        AccountComparator cmp = new AccountComparator();
        Map<String, Object> attrsA = new HashMap<String, Object>();
        attrsA.put(Provisioning.A_zimbraId, "aaa-id");
        Account a = account("a@zimbra.com", "aaa-id", attrsA);
        Map<String, Object> attrsB = new HashMap<String, Object>();
        attrsB.put(Provisioning.A_zimbraId, "bbb-id");
        Account b = account("b@zimbra.com", "bbb-id", attrsB);

        // Act
        int result = cmp.compare(a, b);

        // Assert -- "aaa-id" sorts before "bbb-id"
        assertTrue("aaa-id should sort before bbb-id", result < 0);
        assertTrue(cmp.compare(b, a) > 0);
        assertEquals(0, cmp.compare(a, a));
    }

    @Test
    public void compareExplicitConstructorFieldComparesByDomain() throws Exception {
        // Arrange -- field 1 compares by domain name
        AccountComparator cmp = new AccountComparator(1, null);
        Account a = account("u@alpha.com", "id-a", new HashMap<String, Object>());
        Account b = account("u@beta.com", "id-b", new HashMap<String, Object>());

        // Act
        int result = cmp.compare(a, b);

        // Assert -- "alpha.com" sorts before "beta.com"
        assertTrue("alpha should sort before beta", result < 0);
    }

    @Test
    public void compareSetCompareByDomainOrdersByDomainName() throws Exception {
        // Arrange
        AccountComparator cmp = new AccountComparator();
        cmp.setCompareByDomain();
        Account a = account("u@zebra.com", "id-a", new HashMap<String, Object>());
        Account b = account("u@apple.com", "id-b", new HashMap<String, Object>());

        // Act
        int result = cmp.compare(a, b);

        // Assert -- "zebra.com" sorts after "apple.com"
        assertTrue("zebra should sort after apple", result > 0);
    }

    @Test
    public void compareSetCompareByAttributeOrdersByNamedAttribute() throws Exception {
        // Arrange -- field 2 compares by an arbitrary attribute value
        AccountComparator cmp = new AccountComparator();
        cmp.setCompareByAttribute(Provisioning.A_displayName);

        Map<String, Object> attrsA = new HashMap<String, Object>();
        attrsA.put(Provisioning.A_displayName, "Aaron");
        Account a = account("a@zimbra.com", "id-a", attrsA);

        Map<String, Object> attrsB = new HashMap<String, Object>();
        attrsB.put(Provisioning.A_displayName, "Zoe");
        Account b = account("b@zimbra.com", "id-b", attrsB);

        // Act
        int result = cmp.compare(a, b);

        // Assert -- "Aaron" sorts before "Zoe"
        assertTrue("Aaron should sort before Zoe", result < 0);
    }

    @Test
    public void compareSetCompareByIDResetsToIdComparison() throws Exception {
        // Arrange -- switch to attribute mode, then back to id mode
        AccountComparator cmp = new AccountComparator();
        cmp.setCompareByAttribute(Provisioning.A_displayName);
        cmp.setCompareByID();

        // Account.getId() reads the zimbraId attribute, so set it explicitly.
        Map<String, Object> attrsA = new HashMap<String, Object>();
        attrsA.put(Provisioning.A_zimbraId, "id-aaa");
        Account a = account("a@zimbra.com", "id-aaa", attrsA);
        Map<String, Object> attrsB = new HashMap<String, Object>();
        attrsB.put(Provisioning.A_zimbraId, "id-bbb");
        Account b = account("b@zimbra.com", "id-bbb", attrsB);

        // Act
        int result = cmp.compare(a, b);

        // Assert -- comparison is now by id again
        assertTrue("id-aaa should sort before id-bbb", result < 0);
    }

    @Test
    public void compareNonAccountObjectsReturnsZero() throws Exception {
        // Arrange
        AccountComparator cmp = new AccountComparator();

        // Act -- neither operand is an Account
        int result = cmp.compare("not-an-account", new Object());

        // Assert
        assertEquals(0, result);
    }

    @Test
    public void compareOneNonAccountObjectReturnsZero() throws Exception {
        // Arrange
        AccountComparator cmp = new AccountComparator();
        Account a = account("a@zimbra.com", "id-a", new HashMap<String, Object>());

        // Act -- only one operand is an Account
        int result = cmp.compare(a, "not-an-account");

        // Assert
        assertEquals(0, result);
    }

    @Test
    public void compareAttributeMissingSwallowsExceptionAndReturnsZero() throws Exception {
        // Arrange -- compare by an attribute that neither account has, triggering an NPE that
        // the comparator catches and treats as "equal"
        AccountComparator cmp = new AccountComparator();
        cmp.setCompareByAttribute("zimbraNoSuchAttributeXYZ");
        Account a = account("a@zimbra.com", "id-a", new HashMap<String, Object>());
        Account b = account("b@zimbra.com", "id-b", new HashMap<String, Object>());

        // Act
        int result = cmp.compare(a, b);

        // Assert -- exception path yields 0
        assertEquals(0, result);
    }

    @Test
    public void compareUnknownFieldReturnsZeroViaDefaultBranch() throws Exception {
        // Arrange -- field value outside the known switch cases hits the default branch
        AccountComparator cmp = new AccountComparator(99, null);
        Account a = account("a@zimbra.com", "id-a", new HashMap<String, Object>());
        Account b = account("b@zimbra.com", "id-b", new HashMap<String, Object>());

        // Act
        int result = cmp.compare(a, b);

        // Assert
        assertEquals(0, result);
    }
}
