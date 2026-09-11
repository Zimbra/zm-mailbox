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

package com.zimbra.cs.account.ldap;

import com.zimbra.common.service.ServiceException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LdapProvisioningCreateDomainExceptionTest {

    @Test
    public void mapCreateDomainServiceExceptionRethrowsPermDenied() {
        ServiceException permDenied = ServiceException.PERM_DENIED("domain denied");
        ServiceException caught = null;

        try {
            LdapProvisioning.mapCreateDomainServiceException("example.com", permDenied);
        } catch (ServiceException e) {
            caught = e;
        }

        assertSame("must rethrow original PERM_DENIED", permDenied, caught);
        assertEquals(ServiceException.PERM_DENIED, caught.getCode());
    }

    @Test
    public void mapCreateDomainServiceExceptionWrapsNonPermDenied() {
        ServiceException invalid = ServiceException.INVALID_REQUEST("bad input", null);

        try {
            throw LdapProvisioning.mapCreateDomainServiceException("example.com", invalid);
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue(e.getMessage().contains("unable to create domain: example.com"));
            assertSame(invalid, e.getCause());
        }
    }
}

