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

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.ZAttrProvisioning.DomainStatus;
import com.zimbra.common.account.ZAttrProvisioning.DomainType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link Domain} — entry type, real modify/delete workflows through the
 * in-memory provisioning harness, status predicates (suspended/shutdown), domain-type/local
 * detection, rename detection, unicode name derivation, account defaults, and the unsupported
 * GAL-search-base path.
 */
public class DomainTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        provisioning = Provisioning.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        Domain d = provisioning.get(DomainBy.name, "dtest.example.com");
        if (d != null) {
            provisioning.deleteDomain(d.getId());
        }
    }

    private Domain createDomain(Map<String, Object> attrs) throws ServiceException {
        return provisioning.createDomain("dtest.example.com", attrs);
    }

    // ---------- entry type ----------

    @Test
    public void getEntryTypeAlwaysIsDomain() throws Exception {
        // Arrange
        Domain d = createDomain(new HashMap<String, Object>());

        // Act / Assert
        assertEquals("entry type is DOMAIN", EntryType.DOMAIN, d.getEntryType());
    }

    // ---------- unicode name ----------

    @Test
    public void getUnicodeNameAsciiDomainReturnsSameName() throws Exception {
        // Arrange
        Domain d = createDomain(new HashMap<String, Object>());

        // Act / Assert
        assertEquals("ascii domain unicode name equals its name",
                "dtest.example.com", d.getUnicodeName());
    }

    // ---------- modify workflow (real persistence) ----------

    @Test
    public void modifySetDescriptionPersistsAndReloadable() throws Exception {
        // Arrange
        Domain d = createDomain(new HashMap<String, Object>());
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_description, "Marketing domain");

        // Act
        d.modify(changes);

        // Assert — reload by id, don't trust the in-memory ref alone
        Domain reloaded = provisioning.get(DomainBy.id, d.getId());
        assertNotNull("domain still present after modify", reloaded);
        assertEquals("description persisted on the entry",
                "Marketing domain", reloaded.getAttr(Provisioning.A_description));
    }

    // ---------- delete workflow ----------

    @Test
    public void deleteDomainExistingDomainRemovesFromProvisioning() throws Exception {
        // Arrange
        Domain d = createDomain(new HashMap<String, Object>());
        String id = d.getId();
        assertNotNull("domain created", provisioning.get(DomainBy.id, id));

        // Act — deleteDomain ignores its arg and uses getId() internally
        d.deleteDomain(id);

        // Assert
        assertNull("domain gone after delete", provisioning.get(DomainBy.id, id));
    }

    // ---------- isSuspended ----------

    @Test
    public void isSuspendedStatusSuspendedReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, DomainStatus.suspended.toString());
        Domain d = createDomain(attrs);

        // Act / Assert
        assertTrue("suspended status => isSuspended true", d.isSuspended());
    }

    @Test
    public void isSuspendedStatusActiveReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, DomainStatus.active.toString());
        Domain d = createDomain(attrs);

        // Act / Assert
        assertFalse("active status => not suspended", d.isSuspended());
    }

    @Test
    public void isSuspendedNoStatusReturnsFalse() throws Exception {
        // Arrange — no status attr set
        Domain d = createDomain(new HashMap<String, Object>());

        // Act / Assert
        assertFalse("absent status => not suspended", d.isSuspended());
    }

    // ---------- isShutdown ----------

    @Test
    public void isShutdownStatusShutdownReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, DomainStatus.shutdown.toString());
        Domain d = createDomain(attrs);

        // Act / Assert
        assertTrue("shutdown status => isShutdown true", d.isShutdown());
    }

    @Test
    public void isShutdownStatusActiveReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, DomainStatus.active.toString());
        Domain d = createDomain(attrs);

        // Act / Assert
        assertFalse("active status => not shutdown", d.isShutdown());
    }

    // ---------- isLocal ----------

    @Test
    public void isLocalTypeLocalReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, DomainType.local.toString());
        Domain d = createDomain(attrs);

        // Act / Assert
        assertTrue("local domain type => isLocal true", d.isLocal());
    }

    @Test
    public void isLocalTypeAliasReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, DomainType.alias.toString());
        Domain d = createDomain(attrs);

        // Act / Assert
        assertFalse("alias domain type => isLocal false", d.isLocal());
    }

    // ---------- beingRenamed ----------

    @Test
    public void beingRenamedRenameInfoSetReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainRenameInfo, "RENAME_INPROGRESS");
        Domain d = createDomain(attrs);

        // Act / Assert
        assertTrue("non-empty rename info => beingRenamed true", d.beingRenamed());
    }

    @Test
    public void beingRenamedNoRenameInfoReturnsFalse() throws Exception {
        // Arrange
        Domain d = createDomain(new HashMap<String, Object>());

        // Act / Assert
        assertFalse("absent rename info => not being renamed", d.beingRenamed());
    }

    // ---------- account defaults ----------

    @Test
    public void getAccountDefaultsFreshDomainReturnsNonNullMap() throws Exception {
        // Arrange
        Domain d = createDomain(new HashMap<String, Object>());

        // Act
        Map<String, Object> defaults = d.getAccountDefaults();

        // Assert
        assertNotNull("account defaults map is initialized (never null)", defaults);
    }

    // ---------- getGalSearchBase ----------

    @Test
    public void getGalSearchBaseAnySpecThrowsUnsupported() throws Exception {
        // Arrange
        Domain d = createDomain(new HashMap<String, Object>());

        // Act / Assert
        try {
            d.getGalSearchBase("DOMAIN");
            fail("getGalSearchBase is unsupported on the base Domain");
        } catch (ServiceException e) {
            assertTrue("message states it is unsupported", e.getMessage().contains("unsupported"));
        }
    }
}
