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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.admin.type.CacheEntryType;
import java.util.HashMap;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MobileNotificationPayloadTest {

    private static class TestableMobileNotificationPayload extends MobileNotificationPayload {
        private CacheEntryType flushedType;

        private Provisioning.CacheEntry[] flushedEntries;

        private int flushCount;

        @Override
        protected void flushCache(CacheEntryType type, Provisioning.CacheEntry[] entries)
                throws ServiceException {
            flushCount++;
            flushedType = type;
            flushedEntries = entries;
        }
    }

    private static Provisioning provisioning;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        provisioning = Provisioning.getInstance();
    }

    @Test
    public void preModifyAllowsGlobalConfigOnly() throws Exception {
        TestableMobileNotificationPayload callback = new TestableMobileNotificationPayload();

        callback.preModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", "CONFIGURABLE",
                new HashMap<String, Object>(),
                createConfigEntry());
    }

    @Test
    public void preModifyRejectsDomainCreateContext() throws Exception {
        expectDenied(new CallbackContext(CallbackContext.Op.CREATE), createConfigEntry(),
                Domain.class);
    }

    @Test
    public void preModifyRejectsAccountCreateContext() throws Exception {
        expectDenied(new CallbackContext(CallbackContext.Op.CREATE), createConfigEntry(),
                Account.class);
    }

    @Test
    public void preModifyRejectsCosCreateContext() throws Exception {
        expectDenied(new CallbackContext(CallbackContext.Op.CREATE), createConfigEntry(),
                Cos.class);
    }

    @Test
    public void preModifyRejectsDomainEntryUpdate() throws Exception {
        expectDenied(new CallbackContext(CallbackContext.Op.MODIFY), createDomainEntry(), null);
    }

    @Test
    public void preModifyRejectsAccountEntryUpdate() throws Exception {
        expectDenied(new CallbackContext(CallbackContext.Op.MODIFY), createAccountEntry(), null);
    }

    @Test
    public void preModifyRejectsCosEntryUpdate() throws Exception {
        expectDenied(new CallbackContext(CallbackContext.Op.MODIFY), createCosEntry(), null);
    }

    @Test
    public void postModifyFlushesConfigCache() throws Exception {
        TestableMobileNotificationPayload callback = new TestableMobileNotificationPayload();

        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", createConfigEntry());

        assertEquals(CacheEntryType.config, callback.flushedType);
        assertNull(callback.flushedEntries);
        assertEquals(1, callback.flushCount);
    }

    @Test
    public void postModifyFlushesOnlyOncePerContext() throws Exception {
        TestableMobileNotificationPayload callback = new TestableMobileNotificationPayload();
        CallbackContext context = new CallbackContext(CallbackContext.Op.MODIFY);

        callback.postModify(context, "zimbraPushNotificationPayloadMode", createConfigEntry());
        callback.postModify(context, "zimbraPushNotificationPayloadMode", createConfigEntry());

        assertEquals(1, callback.flushCount);
    }

    @Test
    public void postModifyIgnoresNonConfigEntries() throws Exception {
        TestableMobileNotificationPayload callback = new TestableMobileNotificationPayload();

        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", createAccountEntry());

        assertNull(callback.flushedType);
        assertNull(callback.flushedEntries);
    }

    private void expectDenied(CallbackContext context, Entry entry,
            Class<? extends Entry> creatingEntryType) throws Exception {
        if (creatingEntryType != null) {
            context.setCreatingEntryType(creatingEntryType);
        }

        TestableMobileNotificationPayload callback = new TestableMobileNotificationPayload();
        try {
            callback.preModify(context, "zimbraPushNotificationPayloadMode", "CONFIGURABLE",
                    new HashMap<String, Object>(), entry);
            fail("expected ServiceException denying non-global-config updates");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("global config level"));
        }
    }

    private Config createConfigEntry() {
        return new Config(new HashMap<String, Object>(), provisioning);
    }

    private Domain createDomainEntry() {
        return new Domain("example.com", "domain-id", new HashMap<String, Object>(),
                new HashMap<String, Object>(), provisioning);
    }

    private Account createAccountEntry() {
        return new Account("user@example.com", "account-id", new HashMap<String, Object>(),
                new HashMap<String, Object>(), provisioning);
    }

    private Cos createCosEntry() {
        return new Cos("default", "cos-id", new HashMap<String, Object>(), provisioning);
    }
}
