/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link CrossDomain}: the cross-domain-admin grant validation invariants
 * ({@code validateCrossDomainAdminGrant}), the {@code checkCrossDomain} short-circuit when the
 * target has no domain, and the cross-domain-admin ACE scan ({@code checkCrossDomainAdminRight})
 * against a real {@link Domain} with no ACL. RightManager is initialized in @BeforeClass so the
 * generated {@code Admin.R_crossDomainAdmin} right is populated; entities come from the harness.
 */
public class CrossDomainTest {

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        // Loads the right-definition XML; populates Admin.R_crossDomainAdmin (else it is null).
        RightManager.getInstance();
    }

    private Domain createDomain(String name) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createDomain(name, attrs);
    }

    // ---------- validateCrossDomainAdminGrant ----------

    @Test
    public void validateCrossDomainAdminGrantCrossDomainRightWithDomainGranteeReturnsTrue() throws Exception {
        // Arrange — the right that is legitimately granted to a domain grantee
        assertNotNull("RightManager must have populated the crossDomainAdmin right",
                Admin.R_crossDomainAdmin);

        // Act
        boolean isCrossDomain = CrossDomain.validateCrossDomainAdminGrant(
                Admin.R_crossDomainAdmin, GranteeType.GT_DOMAIN);

        // Assert
        assertTrue("crossDomainAdmin + domain grantee is the cross-domain case", isCrossDomain);
    }

    @Test
    public void validateCrossDomainAdminGrantCrossDomainRightWithNonDomainGranteeThrows() throws Exception {
        // Act / Assert — crossDomainAdmin must be granted to a domain, not e.g. a user
        try {
            CrossDomain.validateCrossDomainAdminGrant(Admin.R_crossDomainAdmin, GranteeType.GT_USER);
            fail("expected INVALID_REQUEST: crossDomainAdmin grantee must be a domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("must be a domain"));
        }
    }

    @Test
    public void validateCrossDomainAdminGrantOtherRightWithDomainGranteeThrows() throws Exception {
        // Arrange — any non-crossDomain admin right
        AdminRight other = Admin.R_renameAccount;
        assertNotNull("RightManager must populate R_renameAccount", other);

        // Act / Assert — a domain grantee is only legal for crossDomainAdmin
        try {
            CrossDomain.validateCrossDomainAdminGrant(other, GranteeType.GT_DOMAIN);
            fail("expected INVALID_REQUEST: non-crossDomain right cannot have a domain grantee");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("cannot be a domain"));
        }
    }

    @Test
    public void validateCrossDomainAdminGrantOtherRightWithNonDomainGranteeReturnsFalse() throws Exception {
        // Arrange
        AdminRight other = Admin.R_renameAccount;

        // Act — legal ordinary grant: returns false (not the cross-domain case), no throw
        boolean isCrossDomain = CrossDomain.validateCrossDomainAdminGrant(other, GranteeType.GT_USER);

        // Assert
        assertFalse("an ordinary right to an ordinary grantee is not cross-domain", isCrossDomain);
    }

    // ---------- checkCrossDomain ----------

    @Test
    public void checkCrossDomainNullTargetDomainReturnsTrue() throws Exception {
        // Act — when the target has no domain the check lets it through
        boolean ok = CrossDomain.checkCrossDomain(prov, null, null, null);

        // Assert
        assertTrue("a null target domain is allowed through", ok);
    }

    // ---------- checkCrossDomainAdminRight ----------

    @Test
    public void checkCrossDomainAdminRightTargetNotDomainThrowsFailure() throws Exception {
        // Arrange — a non-domain target entry
        Entry notADomain = prov.getConfig();
        Domain granteeDomain = createDomain("grantee-cd.com");

        // Act / Assert
        try {
            CrossDomain.checkCrossDomainAdminRight(prov, granteeDomain, notADomain, false);
            fail("expected FAILURE when target is not a domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void checkCrossDomainAdminRightDomainTargetWithNoAclReturnsFalse() throws Exception {
        // Arrange — a freshly created target domain has no cross-domain ACE
        Domain granteeDomain = createDomain("grantee2-cd.com");
        Domain targetDomain = createDomain("target-cd.com");

        // Act
        Boolean result = CrossDomain.checkCrossDomainAdminRight(prov, granteeDomain, targetDomain, false);

        // Assert — no ACE granting crossDomainAdmin => FALSE
        assertEquals(Boolean.FALSE, result);
    }
}
