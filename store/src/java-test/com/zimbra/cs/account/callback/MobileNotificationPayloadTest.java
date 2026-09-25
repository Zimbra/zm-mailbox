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
import java.util.HashMap;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MobileNotificationPayloadTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        provisioning = Provisioning.getInstance();
    }

    @Test
    public void preModifyAllowsGlobalConfigOnly() throws ServiceException {
        MobileNotificationPayload callback = new MobileNotificationPayload();

        callback.preModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", "CONFIGURABLE",
                new HashMap<>(),
                createConfigEntry());
    }

    @Test
    public void preModifyRejectsDomainCreateContext() {
        expectDenied(new CallbackContext(CallbackContext.Op.CREATE), createConfigEntry(),
                Domain.class);
    }

    @Test
    public void preModifyRejectsAccountCreateContext() {
        expectDenied(new CallbackContext(CallbackContext.Op.CREATE), createConfigEntry(),
                Account.class);
    }

    @Test
    public void preModifyRejectsCosCreateContext() {
        expectDenied(new CallbackContext(CallbackContext.Op.CREATE), createConfigEntry(),
                Cos.class);
    }

    @Test
    public void preModifyRejectsDomainEntryUpdate() {
        expectDenied(new CallbackContext(CallbackContext.Op.MODIFY), createDomainEntry(), null);
    }

    @Test
    public void preModifyRejectsAccountEntryUpdate() {
        expectDenied(new CallbackContext(CallbackContext.Op.MODIFY), createAccountEntry(), null);
    }

    @Test
    public void preModifyRejectsCosEntryUpdate() {
        expectDenied(new CallbackContext(CallbackContext.Op.MODIFY), createCosEntry(), null);
    }

    @Test
    public void postModifySucceedsForConfigEntry() {
        MobileNotificationPayload callback = new MobileNotificationPayload();

        // Should not throw an exception
        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", createConfigEntry());
    }

    @Test
    public void postModifyFlushesOnlyOncePerContext() {
        MobileNotificationPayload callback = new MobileNotificationPayload();
        CallbackContext context = new CallbackContext(CallbackContext.Op.MODIFY);

        // Should not throw on first call
        callback.postModify(context, "zimbraPushNotificationPayloadMode", createConfigEntry());

        // Should not throw on second call (idempotent due to context.isDoneAndSetIfNot)
        callback.postModify(context, "zimbraPushNotificationPayloadMode", createConfigEntry());
    }

    @Test
    public void postModifyIgnoresNonConfigEntries() {
        MobileNotificationPayload callback = new MobileNotificationPayload();

        // Should not throw for account entry
        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", createAccountEntry());

        // Should not throw for domain entry
        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", createDomainEntry());

        // Should not throw for COS entry
        callback.postModify(new CallbackContext(CallbackContext.Op.MODIFY),
                "zimbraPushNotificationPayloadMode", createCosEntry());
    }

    private void expectDenied(CallbackContext context, Entry entry,
            Class<? extends Entry> creatingEntryType) {
        if (creatingEntryType != null) {
            context.setCreatingEntryType(creatingEntryType);
        }

        MobileNotificationPayload callback = new MobileNotificationPayload();
        try {
            callback.preModify(context, "zimbraPushNotificationPayloadMode", "CONFIGURABLE",
                    new HashMap<>(), entry);
            fail("expected ServiceException denying non-global-config updates");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("global config level"));
        }
    }

    private Config createConfigEntry() {
        return new Config(new HashMap<>(), provisioning);
    }

    private Domain createDomainEntry() {
        return new Domain("example.com", "domain-id", new HashMap<>(),
                new HashMap<>(), provisioning);
    }

    private Account createAccountEntry() {
        return new Account("user@example.com", "account-id", new HashMap<>(),
                new HashMap<>(), provisioning);
    }

    private Cos createCosEntry() {
        return new Cos("default", "cos-id", new HashMap<>(), provisioning);
    }
}
