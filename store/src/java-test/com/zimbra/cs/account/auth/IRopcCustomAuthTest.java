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

package com.zimbra.cs.account.auth;

import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IRopcCustomAuthTest {
    private IRopcCustomAuth ropcCustomAuth;

    private Account mockAccount;

    private Map<String, Object> mockContext;

    @Before
    public void setUp() throws Exception {
        ropcCustomAuth = new IRopcCustomAuth();
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("user1@example.zimbra.com", "password", new HashMap<String, Object>());
        mockAccount = Provisioning.getInstance().get(Key.AccountBy.name, "user1@example.zimbra.com");
        mockContext = new HashMap<String, Object>();
        mockContext.put("protocol", "zsync");
    }

    @Test
    public void testCLassIsInstantiated() {
        assertNotNull("IRopcCustomAuthInstance should not be null", ropcCustomAuth);
    }

    @Test
    public void testExecuteRopcAuthReturnsTrue() {
        assertTrue("Should return true", ropcCustomAuth.executeRopcAuth());
    }

    @Test
    public void testAuthnetocateValidCallDoesnotThrowException() {
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, null);
        } catch (Exception e) {
            fail("Auth should not throw a exception");
        }
    }

    @Test
    public void testCeckPasswordAgingReturnsFalse() {
        assertFalse("Password aging should return false", ropcCustomAuth.checkPasswordAging());
    }
}
