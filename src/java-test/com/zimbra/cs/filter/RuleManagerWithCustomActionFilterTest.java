/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.filter;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.*;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.commands.AbstractActionCommand;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Unit test for {@link com.zimbra.cs.filter.RuleManager}
 * used with CustomActionFilter extension
 */
public final class RuleManagerWithCustomActionFilterTest {

    private static SieveFactory original_sf;

    @BeforeClass
    public static void init() throws Exception {

        // keep original sieve factory
        original_sf = RuleManager.getSieveFactory();

        ExtensionTestUtil.init();

        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());

        // make sure the behavior before registering custom actions
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        AbstractActionCommand ext =
                (AbstractActionCommand) ExtensionUtil.getExtension("discard");
        Assert.assertNull(ext);

        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if socialcast { discard; }");
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox, new ParsedMessage(
                "From: do-not-reply@socialcast.com\nReply-To: share@socialcast.com\nSubject: test".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(0, ids.size());

        // register custom action extensions
        ExtensionTestUtil.registerExtension("com.zimbra.extensions.DummyCustomDiscard");
        ExtensionTestUtil.registerExtension("com.zimbra.extensions.DummyCustomTag");
        ExtensionUtil.initAll();


    }

    @AfterClass
    public static void cleanUp() throws Exception{

        // set original ones
        JsieveConfigMapHandler.registerCommand("discard", "com.zimbra.cs.filter.jsieve.Discard");
        JsieveConfigMapHandler.registerCommand("tag", "com.zimbra.cs.filter.jsieve.Tag");

        // set original sieve factory back
        Method method = RuleManager.class.getDeclaredMethod("createSieveFactory");
        method.setAccessible(true);

        Field field =  RuleManager.class.getDeclaredField("SIEVE_FACTORY");
        field.setAccessible(true);

        field.set(RuleManager.class, original_sf);

        // inactivate custom action extension for just in case
        //ZimbraExtension discard_ext = ExtensionUtil.getExtension("discard");
        //discard_ext.destroy();

    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void tagAndCustomDiscard() throws Exception {

        // register custom action extension
        //ExtensionTestUtil.registerExtension("com.zimbra.extensions.DummyCustomDiscard");
        //ExtensionUtil.initAll();

        JsieveConfigMapHandler.registerCommand("discard", "com.zimbra.extensions.DummyCustomDiscard");
        JsieveConfigMapHandler.registerCommand("tag", "com.zimbra.cs.filter.jsieve.Tag");

        // recreate sieve factory
        Method method = RuleManager.class.getDeclaredMethod("createSieveFactory");
        method.setAccessible(true);

        Field field =  RuleManager.class.getDeclaredField("SIEVE_FACTORY");
        field.setAccessible(true);

        field.set(RuleManager.class, method.invoke(RuleManager.class));

        // make sure the registrations
        AbstractActionCommand ext =
                                    (AbstractActionCommand) ExtensionUtil.getExtension("discard");
        Assert.assertNotNull(ext);


        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if socialcast { tag \"socialcast\"; discard; }");
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox, new ParsedMessage(
                "From: do-not-reply@socialcast.com\nReply-To: share@socialcast.com\nSubject: test".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("Inbox", mbox.getFolderById(null, msg.getFolderId()).getName());
        Assert.assertArrayEquals(new String[] { "socialcast" , "priority" }, msg.getTags());

    }

    @Test
    public void customDicardAndCustomTag() throws Exception {

        // register custom action extensions
        //ExtensionTestUtil.registerExtension("com.zimbra.extensions.DummyCustomDiscard");
        //ExtensionTestUtil.registerExtension("com.zimbra.extensions.DummyCustomTag");
        //ExtensionUtil.initAll();

        JsieveConfigMapHandler.registerCommand("discard", "com.zimbra.extensions.DummyCustomDiscard");
        JsieveConfigMapHandler.registerCommand("tag", "com.zimbra.extensions.DummyCustomTag");

        // recreate sieve factory
        Method method = RuleManager.class.getDeclaredMethod("createSieveFactory");
        method.setAccessible(true);

        Field field =  RuleManager.class.getDeclaredField("SIEVE_FACTORY");
        field.setAccessible(true);

        field.set(RuleManager.class, method.invoke(RuleManager.class));

        // make sure the registrations
        AbstractActionCommand discard_ext =
                (AbstractActionCommand) ExtensionUtil.getExtension("discard");
        Assert.assertNotNull(discard_ext);

        AbstractActionCommand tag_ext =
                (AbstractActionCommand) ExtensionUtil.getExtension("tag");
        Assert.assertNotNull(tag_ext);


        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if header :contains [\"Subject\"] [\"Zimbra\"] { tag \"socialcast\"; discard; }");
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox, new ParsedMessage(
                "From: do-not-reply@socialcast.com\nReply-To: share@socialcast.com\nSubject: Zimbra".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertArrayEquals(new String[] { "zimbra" , "priority" }, msg.getTags());

        // inactivate custom tag action extension for just in case this test would be executed
        // before tagAndCustomDiscard test above
        //ZimbraExtension tag_ext2 = ExtensionUtil.getExtension("tag");
        //tag_ext2.destroy();

    }

}
