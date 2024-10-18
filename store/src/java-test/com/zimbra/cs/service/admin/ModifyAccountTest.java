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
package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

public class ModifyAccountTest {

    private Map<String, Object> attrs = new HashMap<>();
    private ModifyAccount modifyAccount = new ModifyAccount();

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
    }

    @Test
    public void testValidateMailAttachmentMaxSize_WithinLimit() {
        attrs.put(Provisioning.A_zimbraMailAttachmentMaxSize, "10000");
        try {
            modifyAccount.validateMailAttachmentMaxSize(attrs);
        } catch (ServiceException e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testValidateMailAttachmentMaxSize_ExceedingLimit() {
        attrs.put(Provisioning.A_zimbraMailAttachmentMaxSize, "100000000");
        try {
            modifyAccount.validateMailAttachmentMaxSize(attrs);
            fail("Exception should be thrown");
        } catch (ServiceException e) {
            assertEquals("account.INVALID_ATTR_VALUE", e.getCode());
            assertTrue(e.getMessage().contains("larger than max allowed"));
        }
    }

    @Test
    public void testValidateMailAttachmentMaxSize_InvalidLong() {
        attrs.put(Provisioning.A_zimbraMailAttachmentMaxSize, "19999abc");
        try {
            modifyAccount.validateMailAttachmentMaxSize(attrs);
            fail("Exception should be thrown");
        } catch (ServiceException e) {
            assertEquals("account.INVALID_ATTR_VALUE", e.getCode());
            assertTrue(e.getMessage().contains("must be a valid long"));
        }
    }
}