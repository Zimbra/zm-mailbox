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

package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FcmConfigRestrictionTest {

    private FcmConfigRestriction callback;

    private Config config;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        callback = new FcmConfigRestriction();
        config = new Config(new HashMap<String, Object>(), Provisioning.getInstance());
    }

    @Test
    public void preModifyAllowsGlobalConfigEntryWhenNotCreatingDomain() throws Exception {
        CallbackContext ctx = new CallbackContext(CallbackContext.Op.MODIFY);

        callback.preModify(ctx, "zimbraFcmEnabled", "TRUE", new HashMap<String, Object>(), config);
    }

    @Test
    public void preModifyRejectsNonConfigEntry() throws Exception {
        CallbackContext ctx = new CallbackContext(CallbackContext.Op.MODIFY);

        try {
            callback.preModify(ctx, "zimbraFcmEnabled", "TRUE", new HashMap<String, Object>(), null);
            fail("expected PERM_DENIED for non-config entry");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertEquals("permission denied: 'zimbraFcmEnabled' can only be configured at the global config level.",
                    e.getMessage());
        }
    }

    @Test
    public void preModifyRejectsDomainCreateContextEvenForConfigEntry() throws Exception {
        CallbackContext ctx = new CallbackContext(CallbackContext.Op.CREATE);
        ctx.setCreatingEntryType(Domain.class);

        try {
            callback.preModify(ctx, "zimbraFcmEnabled", "TRUE", new HashMap<String, Object>(), config);
            fail("expected PERM_DENIED for domain create context");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertEquals("permission denied: 'zimbraFcmEnabled' can only be configured at the global config level.",
                    e.getMessage());
        }
    }

    @Test
    public void postModifyIsNoOp() {
        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY), "zimbraFcmEnabled", config);
    }
}


