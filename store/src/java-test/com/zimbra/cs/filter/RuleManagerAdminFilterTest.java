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

import java.util.Enumeration;
import org.junit.Ignore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Header;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.MailItem;
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
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class RuleManagerAdminFilterTest {
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
        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore(scriptAdminBefore);
        account.setMailSieveScript(scriptUser);
        account.setAdminSieveScriptAfter(scriptAdminAfter);

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
        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

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
        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore(scriptAdminBeforeStop);
        account.setMailSieveScript(scriptUser);
        account.setAdminSieveScriptAfter(scriptAdminAfter);

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
        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        // If bad require control is not found before "stop" action is performed,
        // no error should be occurred.
        account.setAdminSieveScriptBefore(scriptAdminBeforeStop);
        account.setMailSieveScript(scriptUser);
        account.setAdminSieveScriptAfter(scriptUserBadRequireName);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
            mbox, new ParsedMessage(message.getBytes(), false),
            0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("admin-defined-before", ArrayUtil.getFirstElement(msg.getTags()));
    }

    @Test
    public void invalidRequireComand2() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        // If bad require control is found, further script(s) should not be processed as well.
        account.setAdminSieveScriptBefore(scriptAdminBefore);
        account.setMailSieveScript(scriptUserBadRequireName);
        account.setAdminSieveScriptAfter(scriptAdminAfter);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
            mbox, new ParsedMessage(message.getBytes(), false),
            0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("admin-defined-before", ArrayUtil.getFirstElement(msg.getTags()));
    }

    String[] variableScripts = {
            // admin-before
              "require [\"tag\", \"log\", \"variables\"];"
            + "set \"beforevar\" \"foo\";"
            + "tag \"admin-before1-${beforevar}\";",
            // enduser
              "require [\"tag\", \"log\", \"variables\"];"
            + "set \"uservar\" \"bar\";"
            + "tag \"enduser1-${beforevar}\";"
            + "tag \"enduser2-${uservar}\";",
            // admin-after
              "require [\"tag\", \"log\", \"variables\"];"
            + "set \"aftervar\" \"baz\";"
            + "tag \"admin-after1-${beforevar}\";"
            + "tag \"admin-after2-${uservar}\";"
            + "tag \"admin-after3-${aftervar}\";"};

    @Test
    public void resetVariables() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);

        Map<String, Object> attrs = Maps.newHashMap();
        attrs = Maps.newHashMap();
        Provisioning.getInstance().getServer(account).modify(attrs);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore(variableScripts[0]);
        account.setMailSieveScript(variableScripts[1]);
        account.setAdminSieveScriptAfter(variableScripts[2]);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        String[] tags = msg.getTags();
        Assert.assertEquals(6, tags.length);
        Assert.assertEquals("admin-before1-foo", tags[0]);
        Assert.assertEquals("enduser1-", tags[1]);
        Assert.assertEquals("enduser2-bar", tags[2]);
        Assert.assertEquals("admin-after1-", tags[3]);
        Assert.assertEquals("admin-after2-", tags[4]);
        Assert.assertEquals("admin-after3-baz", tags[5]);
    }

/*  @Test
    public void variableAdminOnUserOn() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);

        Map<String, Object> attrs = Maps.newHashMap();
        attrs = Maps.newHashMap();
        Provisioning.getInstance().getServer(account).modify(attrs);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        account.unsetMailAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetMailAdminSieveScriptAfter();

        account.setMailAdminSieveScriptBefore(variableScripts[0]);
        account.setMailSieveScript(variableScripts[1]);
        account.setMailAdminSieveScriptAfter(variableScripts[2]);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        String[] tags = msg.getTags();
        Assert.assertEquals(3, tags.length);
        Assert.assertEquals("before-foo", tags[0]); // ${var} is a valid variable
        Assert.assertEquals("enduser-", tags[1]);   // Variable feature is on but no definition of ${var}
        Assert.assertEquals("after-", tags[2]);     // Variable feature is on but no definition of ${var}
    }

    @Test
    public void variableAdminOnUserOn2() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);

        Map<String, Object> attrs = Maps.newHashMap();
        attrs = Maps.newHashMap();
        Provisioning.getInstance().getServer(account).modify(attrs);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        account.unsetMailAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetMailAdminSieveScriptAfter();

        String enduserScript = "require [\"tag\", \"log\", \"variables\"];"
                + "set \"var\" \"bar\";"
                + "tag \"enduser-${var}\";";
        account.setMailAdminSieveScriptBefore(variableScripts[0]);
        account.setMailSieveScript(enduserScript);
        account.setMailAdminSieveScriptAfter(variableScripts[2]);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        String[] tags = msg.getTags();
        Assert.assertEquals(3, tags.length);
        Assert.assertEquals("before-foo", tags[0]); // "foo" is assigned in admin-defined filter
        Assert.assertEquals("enduser-bar", tags[1]);// "bar" is assigned in user-defined filter
        Assert.assertEquals("after-", tags[2]);     // Variable feature is on but no definition of ${var}
    }*/

    @Test
    public void stopAtAdminBefore() throws Exception {
        String adminBefore = "tag \"before-admin\";"
                           + "stop;";
        String enduser     = "tag \"enduser\";";
        String adminAfter  = "tag \"after\";";

        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore(adminBefore);
        account.setMailSieveScript(enduser);
        account.setAdminSieveScriptAfter(adminAfter);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        String[] tags = msg.getTags();
        Assert.assertEquals(1, tags.length);
        Assert.assertEquals("before-admin", tags[0]);
    }

    @Test
    public void discardAtAdminBefore() throws Exception {
        String adminBefore = "discard;";
        String enduser     = "tag \"enduser\";";
        String adminAfter  = "tag \"after\";";

        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore(adminBefore);
        account.setMailSieveScript(enduser);
        account.setAdminSieveScriptAfter(adminAfter);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
    }

    @Test
    public void fileintoAtAdminBefore() throws Exception {
        String adminBefore = "require \"fileinto\"; fileinto \"foo\";";
        String enduser     = "tag \"enduser\";";
        String adminAfter  = "tag \"after\";";

        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore(adminBefore);
        account.setMailSieveScript(enduser);
        account.setAdminSieveScriptAfter(adminAfter);

        String rawTest = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World";
        RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(rawTest.getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        // message should not be stored in inbox
        Assert.assertNull(
                mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE));

        // message should be stored in foo
        Integer item = mbox
                .getItemIds(null,
                        mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "foo").getId())
                .getIds(MailItem.Type.MESSAGE).get(0);
        Message msg = mbox.getMessageById(null, item);
        Assert.assertEquals("Hello World", msg.getFragment());
        String tags[] = msg.getTags();
        Assert.assertEquals(2, tags.length);
    }

    // Verification for the ZCS-272
/*  @Test
    public void deleteHeaderInAdminBefore() throws Exception {
        String adminBefore = "require [\"editheader\",\"log\"];\n"
                           + "deleteheader :matches \"X-Test-Header\" \"Ran*\";\n";

        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore(adminBefore);

        String rawTest = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "X-Test-Header: Random\n"
                       + "\n"
                       + "Hello World";
        RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(rawTest.getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);

        Integer itemId = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
        Message message = mbox.getMessageById(null, itemId);
        boolean headerDeleted = true;
        for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
            Header temp = enumeration.nextElement();
            if ("X-Test-Header".equals(temp.getName())) {
                headerDeleted = false;
                break;
            }
        }
        Assert.assertTrue(headerDeleted);
    }

    // Verification for the ZCS-272
    @Test
    public void deleteHeaderInUser() throws Exception {
        String endUser = "require [\"editheader\",\"log\"];\n"
                       + "deleteheader :matches \"X-Test-Header\" \"Ran*\";\n";

        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setMailSieveScript(endUser);

        String rawTest = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "X-Test-Header: Random\n"
                       + "\n"
                       + "Hello World";
        RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(rawTest.getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);

        Integer itemId = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
        Message message = mbox.getMessageById(null, itemId);
        boolean headerDeleted = true;
        for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
            Header temp = enumeration.nextElement();
            if ("X-Test-Header".equals(temp.getName())) {
                headerDeleted = false;
                break;
            }
        }
        Assert.assertTrue(headerDeleted);
    }

    // Verification for the ZCS-272
    @Test
    public void deleteHeaderInAdminAfter() throws Exception {
        String adminAfter = "require [\"editheader\",\"log\"];\n"
                          + "deleteheader :matches \"X-Test-Header\" \"Ran*\";\n";

        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptAfter(adminAfter);

        String rawTest = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "X-Test-Header: Random\n"
                       + "\n"
                       + "Hello World";
        RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(rawTest.getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);

        Integer itemId = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
        Message message = mbox.getMessageById(null, itemId);
        boolean headerDeleted = true;
        for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
            Header temp = enumeration.nextElement();
            if ("X-Test-Header".equals(temp.getName())) {
                headerDeleted = false;
                break;
            }
        }
        Assert.assertTrue(headerDeleted);
    }
*/

    /* Verification for the ZCS-611
     */
    @Test
    public void requireText() throws Exception {
        String adminAfter = "require [\"log\", \"fileinto\"];\n"
                + "require \"tag\";\n"
                + "if  header :contains [\"Subject\"] \"require abc def\" {\n"
                + "  tag \"--require--\";"
                + "  tag \"123require789\";\n"
                + "}";

        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptAfter(adminAfter);

        RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage("Subject: require abc def\n".getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);

        Integer itemId = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
        Message message = mbox.getMessageById(null, itemId);
        String[] tags = message.getTags();
        Assert.assertTrue(tags != null);
        Assert.assertEquals(2, tags.length);
        Assert.assertEquals("--require--", tags[0]);
        Assert.assertEquals("123require789", tags[1]);
    }

    @Test
    public void noFilters() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
    }

    @Test
    public void discardOnlyAtUser() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setMailSieveScript("discard;");

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(0, ids.size());
    }

    @Test
    public void discardOnlyAtAdminBefore() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptBefore("discard;");

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(0, ids.size());
    }

    @Test
    public void discardOnlyAtAdminAfter() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        RuleManager.clearCachedRules(account);

        account.unsetAdminSieveScriptBefore();
        account.unsetMailSieveScript();
        account.unsetAdminSieveScriptAfter();

        account.setAdminSieveScriptAfter("discard;");

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(message.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(0, ids.size());
    }
}

