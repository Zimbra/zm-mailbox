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

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LdapProvisioningCreateDomainExceptionTest {

    private static Provisioning provisioning;

    private final List<String> createdDomainIds = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        for (String id : createdDomainIds) {
            Domain domain = provisioning.get(DomainBy.id, id);
            if (domain != null) {
                provisioning.deleteDomain(id);
            }
        }
        createdDomainIds.clear();
    }

    private String domainName(String label) {
        return label + "-" + System.currentTimeMillis() + "-" + System.nanoTime() + ".example.com";
    }

    @Test
    public void createDomainWithoutRestrictedMailRecallAttributeCreatesDomain() throws Exception {
        String name = domainName("create");

        Domain created = provisioning.createDomain(name, new HashMap<>());
        createdDomainIds.add(created.getId());

        assertNotNull(created);
        assertEquals(name, created.getName());
    }

    @Test
    public void createDomainWithMailRecallEnabledFalseDoesNotThrowPermDenied() throws Exception {
        String name = domainName("false");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraFeatureMailRecallEnabled, ProvisioningConstants.FALSE);

        Domain created = provisioning.createDomain(name, attrs);
        createdDomainIds.add(created.getId());

        assertNotNull(created);
        assertEquals(name, created.getName());
    }
}

