/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DomainCOSMaxAccounts} - the AttributeCallback that rejects
 * duplicate cos entries in zimbraDomainCOSMaxAccounts (a multi-valued "cosId:max" attr).
 */
public class DomainCOSMaxAccountsTest {

    private static final String ATTR = Provisioning.A_zimbraDomainCOSMaxAccounts;

    private Provisioning prov;

    /** Domains created per test, deleted in {@link #tearDown()} even when a test fails. */
    private final List<Domain> createdDomains = new ArrayList<Domain>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        for (Domain d : createdDomains) {
            try {
                prov.deleteDomain(d.getId());
            } catch (Exception ignore) {
                // best-effort cleanup: one failure must not block the rest
            }
        }
        createdDomains.clear();
    }

    private CallbackContext modifyCtx() {
        return new CallbackContext(CallbackContext.Op.MODIFY);
    }

    private Domain newDomain(String name, Map<String, Object> attrs) throws Exception {
        Domain domain = prov.createDomain(name, attrs);
        createdDomains.add(domain);
        return domain;
    }

    @Test
    public void preModifyReplaceWithDistinctCosPasses() throws Exception {
        // Arrange
        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ATTR, new String[] {"cosA:10", "cosB:20"});

        // Act / Assert - distinct cos ids are accepted
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get(ATTR), attrsToModify, null);
        assertTrue("distinct cos values must be accepted", true);
    }

    @Test
    public void preModifyReplaceWithDuplicateCosThrowsInvalidRequest() throws Exception {
        // Arrange
        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ATTR, new String[] {"cosA:10", "cosA:20"});

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, attrsToModify.get(ATTR), attrsToModify, null);
            fail("expected ServiceException for duplicate cos in replace");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("multiple values for the same cos"));
        }
    }

    @Test
    public void preModifyReplaceWithBadFormatThrowsInvalidRequest() throws Exception {
        // Arrange - value missing the ':max' part
        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ATTR, "cosNoColon");

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, attrsToModify.get(ATTR), attrsToModify, null);
            fail("expected ServiceException for invalid format");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("invalid format"));
        }
    }

    @Test
    public void preModifyAddDuplicateOfExistingValueThrowsInvalidRequest() throws Exception {
        // Arrange - domain already has cosA:10; admin adds another cosA value
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(ATTR, "cosA:10");
        Domain domain = newDomain("dcma-add.example.com", domAttrs);

        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("+" + ATTR, "cosA:99");

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), "+" + ATTR, attrsToModify.get("+" + ATTR), attrsToModify, domain);
            fail("expected ServiceException adding a duplicate cos");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("multiple values for the same cos"));
        }
    }

    @Test
    public void preModifyAddNewCosToExistingEntryPasses() throws Exception {
        // Arrange - domain has cosA:10; admin adds cosB:20 (no collision)
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(ATTR, "cosA:10");
        Domain domain = newDomain("dcma-addnew.example.com", domAttrs);

        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("+" + ATTR, "cosB:20");

        // Act / Assert - distinct cos accepted
        callback.preModify(modifyCtx(), "+" + ATTR, attrsToModify.get("+" + ATTR), attrsToModify, domain);
        assertTrue("adding a new distinct cos must be accepted", true);
    }

    @Test
    public void preModifyRemoveThenAddSameCosPasses() throws Exception {
        // Arrange - domain has cosA:10; admin removes cosA and adds cosA:50 in the same modify
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(ATTR, "cosA:10");
        Domain domain = newDomain("dcma-readd.example.com", domAttrs);

        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("-" + ATTR, "cosA:10");
        attrsToModify.put("+" + ATTR, "cosA:50");

        // Act / Assert - the delete pass removes cosA from current, so the add does not collide
        callback.preModify(modifyCtx(), "+" + ATTR, attrsToModify.get("+" + ATTR), attrsToModify, domain);
        assertTrue("remove-then-add of same cos must be accepted", true);
    }

    @Test
    public void preModifyEmptyStringValueInReplaceIsSkipped() throws Exception {
        // Arrange - an empty value should be ignored (continue branch), not crash
        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ATTR, new String[] {"", "cosA:10"});

        // Act / Assert
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get(ATTR), attrsToModify, null);
        assertTrue("empty value must be skipped without error", true);
    }

    @Test
    public void preModifyEntryWithBadExistingValueIsIgnoredOnAdd() throws Exception {
        // Arrange - existing value is malformed (no colon); parse(false) returns null and skips it,
        // so a subsequent add of a different cos succeeds.
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(ATTR, "garbageNoColon");
        Domain domain = newDomain("dcma-badcur.example.com", domAttrs);

        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("+" + ATTR, "cosA:5");

        // Act / Assert
        callback.preModify(modifyCtx(), "+" + ATTR, attrsToModify.get("+" + ATTR), attrsToModify, domain);
        assertTrue("bad existing value must be ignored, add still works", true);
    }

    @Test
    public void postModifyIsNoopDoesNotThrow() throws Exception {
        // Arrange
        DomainCOSMaxAccounts callback = new DomainCOSMaxAccounts();
        Domain entry = newDomain("dcma-post.example.com", new HashMap<String, Object>());

        // Act / Assert - postModify is empty
        callback.postModify(modifyCtx(), ATTR, entry);
        assertNotNull(entry);
    }
}
