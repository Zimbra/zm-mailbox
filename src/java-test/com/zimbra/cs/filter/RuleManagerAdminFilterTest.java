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
package com.zimbra.cs.filter;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Unit test for {@link RuleManager} with admin-defined rules.
 */
public final class RuleManagerAdminFilterTest {
    String scriptAdminBefore = "require [\"tag\", \"log\"];\n"
        + "if true {\n"
        + "  tag \"admin-defined-before\";\n"
        + "}";

    String scriptAdminBeforeStop = "require [\"tag\", \"log\"];\n"
        + "if true {\n"
        + "  tag \"admin-defined-before\";\n"
        + "  stop;\n"
        + "}";

    String scriptAdminAfter = "require [\"tag\", \"log\"];\n"
        + "if true {\n"
        + "  tag \"admin-defined-after\";\n"
        + "}";

    String scriptUser = "require [\"tag\", \"log\"];\n"
        + "if true {\n"
        + "  tag \"user-defined\";\n"
        + "}";

    String scriptUserBadRequireName = ""
        + "require [\"badRrequireCommandName\"];\n"
        + "if true {\n"
        + "  tag \"user-defined\";\n"
        + "}\n"
        ;

    String message = "From: do-not-reply@socialcast.com\n"
        + "Reply-To: share@socialcast.com\n"
        + "Subject: test";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void applyAdminRuleBeforeAndAfterUserRuleForIncoming() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.unsetMailAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetMailAdminSieveScriptAfter();

        account.setMailAdminSieveScriptBefore(scriptAdminBefore);
        account.setMailSieveScript(scriptUser);
        account.setMailAdminSieveScriptAfter(scriptAdminAfter);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
            mbox, new ParsedMessage(message.getBytes(), false),
            0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("admin-defined-before", ArrayUtil.getFirstElement(msg.getTags()));
        Assert.assertEquals("user-defined", msg.getTags()[1]);
        Assert.assertEquals("admin-defined-after", msg.getTags()[2]);
    }

    /**
     * Checking backward compatibility: when only the user-defined sieve rule is set,
     * the sieve filter should works as before
     */
    @Test
    public void applyOnlyUserRuleForIncoming() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.unsetMailAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetMailAdminSieveScriptAfter();

        account.setMailSieveScript(scriptUser);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
            mbox, new ParsedMessage(message.getBytes(), false),
            0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("user-defined", ArrayUtil.getFirstElement(msg.getTags()));
    }

    @Test
    public void stopInTheAdminRule() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.unsetMailAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetMailAdminSieveScriptAfter();

        account.setMailAdminSieveScriptBefore(scriptAdminBeforeStop);
        account.setMailSieveScript(scriptUser);
        account.setMailAdminSieveScriptAfter(scriptAdminAfter);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
            mbox, new ParsedMessage(message.getBytes(), false),
            0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("admin-defined-before", ArrayUtil.getFirstElement(msg.getTags()));
    }

    @Test
    public void invalidRequireComand() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.unsetMailAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetMailAdminSieveScriptAfter();

        account.setMailAdminSieveScriptBefore(scriptAdminBeforeStop);
        account.setMailSieveScript(scriptUser);
        account.setMailAdminSieveScriptAfter(scriptUserBadRequireName);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
            mbox, new ParsedMessage(message.getBytes(), false),
            0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
    }
}

