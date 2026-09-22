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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.SearchAccountsOptions.IncludeType;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link SearchAccountsOptions}, including its interaction with the
 * inherited {@link SearchDirectoryOptions} type/return-attr state and a real {@link Domain}
 * created through the MockProvisioning harness.
 */
public class SearchAccountsOptionsTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    @Test
    public void constructorDefaultIncludesAccountsAndCalendarResources() throws Exception {
        // Act
        SearchAccountsOptions options = new SearchAccountsOptions();

        // Assert -- default include type and the corresponding object types
        assertEquals(IncludeType.ACCOUNTS_AND_CALENDAR_RESOURCES, options.getIncludeType());
        Set<ObjectType> types = options.getTypes();
        assertTrue(types.contains(ObjectType.accounts));
        assertTrue(types.contains(ObjectType.resources));
        assertEquals(2, types.size());
    }

    @Test
    public void constructorWithReturnAttrsStoresReturnAttrsAndDefaultType() throws Exception {
        // Arrange
        String[] returnAttrs = new String[] {Provisioning.A_mail, Provisioning.A_displayName};

        // Act
        SearchAccountsOptions options = new SearchAccountsOptions(returnAttrs);

        // Assert
        assertArrayEquals(returnAttrs, options.getReturnAttrs());
        assertEquals(IncludeType.ACCOUNTS_AND_CALENDAR_RESOURCES, options.getIncludeType());
    }

    @Test
    public void constructorWithDomainSetsDomainAndDefaultType() throws Exception {
        // Arrange -- a real domain via the harness
        Domain domain = prov.createDomain("search-domain.com", new HashMap<String, Object>());

        // Act
        SearchAccountsOptions options = new SearchAccountsOptions(domain);

        // Assert
        assertEquals(domain.getId(), options.getDomain().getId());
        assertEquals(IncludeType.ACCOUNTS_AND_CALENDAR_RESOURCES, options.getIncludeType());
    }

    @Test
    public void constructorWithDomainAndReturnAttrsSetsBoth() throws Exception {
        // Arrange
        Domain domain = prov.createDomain("search-domain2.com", new HashMap<String, Object>());
        String[] returnAttrs = new String[] {Provisioning.A_mail};

        // Act
        SearchAccountsOptions options = new SearchAccountsOptions(domain, returnAttrs);

        // Assert
        assertEquals(domain.getId(), options.getDomain().getId());
        assertArrayEquals(returnAttrs, options.getReturnAttrs());
        assertEquals(IncludeType.ACCOUNTS_AND_CALENDAR_RESOURCES, options.getIncludeType());
    }

    @Test
    public void setIncludeTypeAccountsOnlySetsSingleAccountType() throws Exception {
        // Arrange
        SearchAccountsOptions options = new SearchAccountsOptions();

        // Act
        options.setIncludeType(IncludeType.ACCOUNTS_ONLY);

        // Assert
        assertEquals(IncludeType.ACCOUNTS_ONLY, options.getIncludeType());
        Set<ObjectType> types = options.getTypes();
        assertEquals(1, types.size());
        assertTrue(types.contains(ObjectType.accounts));
        assertFalse(types.contains(ObjectType.resources));
    }

    @Test
    public void setIncludeTypeCalendarResourcesOnlySetsSingleResourceType() throws Exception {
        // Arrange
        SearchAccountsOptions options = new SearchAccountsOptions();

        // Act
        options.setIncludeType(IncludeType.CALENDAR_RESOURCES_ONLY);

        // Assert
        assertEquals(IncludeType.CALENDAR_RESOURCES_ONLY, options.getIncludeType());
        Set<ObjectType> types = options.getTypes();
        assertEquals(1, types.size());
        assertTrue(types.contains(ObjectType.resources));
        assertFalse(types.contains(ObjectType.accounts));
    }

    @Test
    public void setIncludeTypeNonSystemAccountsOnlyRecordsTypeButLeavesTypesUnchanged() throws Exception {
        // Arrange -- start from the default include type
        SearchAccountsOptions options = new SearchAccountsOptions();
        Set<ObjectType> before = options.getTypes();
        int beforeSize = before.size();

        // Act -- NON_SYSTEM_ACCOUNTS_ONLY has no switch branch, so types are not reset
        options.setIncludeType(IncludeType.NON_SYSTEM_ACCOUNTS_ONLY);

        // Assert -- include type updated, object types left as they were
        assertEquals(IncludeType.NON_SYSTEM_ACCOUNTS_ONLY, options.getIncludeType());
        assertEquals(beforeSize, options.getTypes().size());
    }

    @Test
    public void setIncludeTypeSwitchBetweenTypesUpdatesTypesEachTime() throws Exception {
        // Arrange
        SearchAccountsOptions options = new SearchAccountsOptions();

        // Act / Assert -- a full workflow of transitions
        options.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        assertEquals(1, options.getTypes().size());

        options.setIncludeType(IncludeType.ACCOUNTS_AND_CALENDAR_RESOURCES);
        assertEquals(2, options.getTypes().size());

        options.setIncludeType(IncludeType.CALENDAR_RESOURCES_ONLY);
        assertTrue(options.getTypes().contains(ObjectType.resources));
        assertFalse(options.getTypes().contains(ObjectType.accounts));
    }

    @Test
    public void setTypesStringArgThrowsServiceFailure() throws Exception {
        // Arrange
        SearchAccountsOptions options = new SearchAccountsOptions();

        // Act / Assert -- this overload is forbidden in favor of setIncludeType
        try {
            options.setTypes("accounts");
            fail("expected ServiceException for setTypes(String)");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("use setIncludeType instead"));
        }
    }

    @Test
    public void setTypesVarargsArgThrowsServiceFailure() throws Exception {
        // Arrange
        SearchAccountsOptions options = new SearchAccountsOptions();

        // Act / Assert
        try {
            options.setTypes(ObjectType.accounts, ObjectType.resources);
            fail("expected ServiceException for setTypes(ObjectType...)");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("use setIncludeType instead"));
        }
    }

    @Test
    public void getDomainNeverSetReturnsNull() throws Exception {
        // Arrange
        SearchAccountsOptions options = new SearchAccountsOptions();

        // Act / Assert -- domain only set by the domain-taking constructors
        assertNull(options.getDomain());
    }

    @Test
    public void includeTypeValueOfRoundTripsAllConstants() {
        // Act / Assert -- enum is reachable without LDAP
        assertEquals(IncludeType.ACCOUNTS_ONLY, IncludeType.valueOf("ACCOUNTS_ONLY"));
        assertEquals(4, IncludeType.values().length);
    }
}
