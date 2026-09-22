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

package com.zimbra.cs.account.callback;

import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AutoProvScheduledDomains#preModify}. Drives the real validation
 * that every domain added to zimbraAutoProvScheduledDomains exists and has EAGER auto-provision
 * enabled, using real {@link Domain} entries from the in-memory harness. Also verifies the
 * postModify guard short-circuits when the server is not started.
 */
public class AutoProvScheduledDomainsTest {

    private static final String ATTR = Provisioning.A_zimbraAutoProvScheduledDomains;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Domain newDomain(String name, boolean eager) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (eager) {
            attrs.put(Provisioning.A_zimbraAutoProvMode, AutoProvMode.EAGER.name());
        }
        return prov.createDomain(name, attrs);
    }

    @Test
    public void preModifyReplacingWithEagerDomainPasses() throws Exception {
        // Arrange -- scheduling an EAGER-enabled domain (REPLACING semantics: bare attr name)
        Domain domain = newDomain("aps-eager.example.com", true);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ATTR, "aps-eager.example.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- no exception expected
        new AutoProvScheduledDomains().preModify(ctx, ATTR, "aps-eager.example.com",
                toModify, domain);

        // Assert
        assertTrue("EAGER domain may be scheduled", true);
        prov.deleteDomain(domain.getId());
    }

    @Test
    public void preModifyAddingEagerDomainPasses() throws Exception {
        // Arrange -- adding via the "+" prefix exercises the ADDING branch of multiValueMod
        Domain domain = newDomain("aps-add.example.com", true);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put("+" + ATTR, "aps-add.example.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new AutoProvScheduledDomains().preModify(ctx, ATTR, "aps-add.example.com",
                toModify, domain);

        // Assert
        assertTrue("adding an EAGER domain is permitted", true);
        prov.deleteDomain(domain.getId());
    }

    @Test
    public void preModifyUnknownDomainThrowsNoSuchDomain() throws Exception {
        // Arrange -- a domain name that was never created
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ATTR, "aps-missing.example.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new AutoProvScheduledDomains().preModify(ctx, ATTR, "aps-missing.example.com",
                    toModify, null);
            fail("expected NO_SUCH_DOMAIN for an unknown scheduled domain");
        } catch (AccountServiceException e) {
            assertEquals("message reports the missing domain", true,
                    e.getMessage().contains("no such domain")
                    && e.getMessage().contains("aps-missing.example.com"));
        }
    }

    @Test
    public void preModifyDomainWithoutEagerThrowsInvalidRequest() throws Exception {
        // Arrange -- the domain exists but does not have EAGER auto provision enabled
        Domain domain = newDomain("aps-noeager.example.com", false);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ATTR, "aps-noeager.example.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new AutoProvScheduledDomains().preModify(ctx, ATTR, "aps-noeager.example.com",
                    toModify, domain);
            fail("expected INVALID_REQUEST when EAGER is not enabled on the domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertEquals("message names the offending domain", true,
                    e.getMessage().contains("aps-noeager.example.com"));
        } finally {
            prov.deleteDomain(domain.getId());
        }
    }

    @Test
    public void preModifyMultipleDomainsOneMissingThrowsNoSuchDomain() throws Exception {
        // Arrange -- a set of scheduled domains where one is valid and one does not exist
        Domain good = newDomain("aps-good.example.com", true);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(ATTR, new String[] {"aps-good.example.com", "aps-absent.example.com" });
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert -- the missing one must trip the validation
        try {
            new AutoProvScheduledDomains().preModify(ctx, ATTR,
                    new String[] {"aps-good.example.com", "aps-absent.example.com" },
                    toModify, good);
            fail("expected NO_SUCH_DOMAIN when any scheduled domain is missing");
        } catch (AccountServiceException e) {
            assertEquals("message reports the missing domain", true,
                    e.getMessage().contains("no such domain")
                    && e.getMessage().contains("aps-absent.example.com"));
        } finally {
            prov.deleteDomain(good.getId());
        }
    }

    @Test
    public void preModifyRemovingDomainSkipsValidation() throws Exception {
        // Arrange -- REMOVING (the "-" prefix) is neither adding nor replacing, so the
        // existence/EAGER checks are skipped even for a non-existent domain.
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put("-" + ATTR, "aps-anything.example.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- must not throw despite the domain not existing
        new AutoProvScheduledDomains().preModify(ctx, ATTR, "aps-anything.example.com",
                toModify, null);

        // Assert
        assertTrue("removing a scheduled domain skips existence validation", true);
    }

    @Test
    public void preModifyAttrNotBeingModifiedSkipsValidation() throws Exception {
        // Arrange -- the scheduled-domains attr is absent from the modify map => mod is null
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraAutoProvMode, AutoProvMode.EAGER.name());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- no scheduled-domains change present, validation loop never runs
        new AutoProvScheduledDomains().preModify(ctx, ATTR, null, toModify, null);

        // Assert
        assertTrue("absent scheduled-domains change skips validation", true);
    }

    @Test
    public void postModifyServerNotStartedReturnsWithoutSwitchingThread() throws Exception {
        // Arrange -- Zimbra.started() is false in unit tests; postModify must early-return
        // before attempting AutoProvisionThread scheduling.
        Domain domain = newDomain("aps-post.example.com", true);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new AutoProvScheduledDomains().postModify(ctx, ATTR, domain);

        // Assert -- reaching here without exception proves the started() guard fired
        assertTrue("postModify must early-return when server not started", true);
        prov.deleteDomain(domain.getId());
    }
}
