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
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.client.ZFilterAction;
import com.zimbra.client.ZFilterCondition;
import com.zimbra.client.ZFilterRule;
import com.zimbra.client.ZFilterRules;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZFilterAction.ZRFCCompliantNotifyAction;
import com.zimbra.client.ZFilterCondition.HeaderOp;
import com.zimbra.client.ZFilterCondition.ZHeaderCondition;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

import junit.framework.TestCase;

public class TestFilterRFCCompliantNotify extends TestCase {
    private static String USER_NAME = "user1";
    private static String REMOTE_USER_NAME = "user2";
    private static String NAME_PREFIX = "TestFilter";

    private ZMailbox mMbox;
    private ZFilterRules mOriginalIncomingRules;
    private boolean mAvailableRFCCompliantNotify;

    public void testNotifyAction()
    throws Exception {
        if (!mAvailableRFCCompliantNotify) {
            fail("Unable to test because 'zimbraSieveNotifyActionRFCCompliant' is set to FALSE");
            return;
        }

        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZFilterCondition.ZTrueCondition());

        String notifyMsg = "This is the notification email alert";
        StringBuilder mailto = new StringBuilder("mailto:")
                .append(TestUtil.getAddress(REMOTE_USER_NAME))
                .append("?body=")
                .append(notifyMsg)
                .append("&Importance=High&X-Priority=1");
        String subject = NAME_PREFIX + " testRFCCompliantNotifyAction";

        // add an action to notify user2
        actions.add(new ZFilterAction.ZRFCCompliantNotifyAction(
                TestUtil.getAddress(USER_NAME), subject, mailto.toString()));
        rules.add(new ZFilterRule("testRFCCompliantNotifyAction", true, false, conditions, actions));
        saveIncomingRules(mMbox, new ZFilterRules(rules));

        String body = "Hi, How are you today?";
        String msg = new MessageBuilder().withFrom(REMOTE_USER_NAME).withSubject(subject).withBody(body).create();
        // send msg to user1
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, REMOTE_USER_NAME, msg);
        // check msg got filed into user1's mailbox
        TestUtil.getMessage(mMbox, "in:inbox subject:\"" + subject + "\"");
        // check notification msg from user1 in user2's mailbox, it should have the same subject
        ZMailbox zMailbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        List<ZMessage> msgs = TestUtil.waitForMessages(zMailbox, "in:inbox subject:\"" + subject + "\"", 1, 120000);
        ZMessage zMessage =  msgs.get(0);
        //        ZMessage zMessage = TestUtil.waitForMessage(zMailbox, "in:inbox subject:\"" + subject + "\"");
        ZMessage.ZMimePart mimeStructure = zMessage.getMimeStructure();
        String bodyContent = mimeStructure.getContent();

        // check body text of the notification msg
        assertTrue(bodyContent.contains(notifyMsg));

        // check headers of the notification msg
        String content = TestUtil.getContent(zMailbox, zMessage.getId());
        assertTrue(content.contains("From: " + USER_NAME));
        assertTrue(content.contains("To: " + REMOTE_USER_NAME));
        assertTrue(content.contains("Subject: " + subject));
        assertTrue(content.contains("Auto-Submitted: auto-notified;"));
        assertTrue(content.contains("X-Zimbra-Forwarded: " + USER_NAME));
    }

    @Override
    public void setUp() throws Exception {
        cleanUp();

        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailSieveScript, "");
        attrs.put(Provisioning.A_zimbraMailOutgoingSieveScript, "");
        Provisioning.getInstance().modifyAttrs(account, attrs);

        mMbox = TestUtil.getZMailbox(USER_NAME);

        // Create mountpoint for testMountpoint()
        ZMailbox remoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);

        mOriginalIncomingRules = mMbox.getIncomingFilterRules();
        saveIncomingRules(mMbox, getTestIncomingRules());

        mAvailableRFCCompliantNotify  = account.isSieveNotifyActionRFCCompliant();
    }

    /**
     * Saves the given incoming filter rules.  Then gets them from the server and confirms that
     * the element tree matches.
     */
    private void saveIncomingRules(ZMailbox mbox, ZFilterRules rules) throws Exception {
        mbox.saveIncomingFilterRules(rules);
        ZFilterRules result = mbox.getIncomingFilterRules(true);
        TestUtil.assertEquals(rules.dump(), result.dump());
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(REMOTE_USER_NAME, NAME_PREFIX);

        // Clean up messages created by testBase64Subject()
        // bug 36705 for (ZMessage msg : TestUtil.search(mMbox, "villanueva")) {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        for (ZMessage msg : TestUtil.search(mbox, "cortes de luz")) {
            mbox.deleteMessage(msg.getId());
        }
    }

    private ZFilterRules getTestIncomingRules()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();

        // if subject contains "folder1", file into folder1 and tag with tag1
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, NAME_PREFIX));
        actions.add(new ZRFCCompliantNotifyAction(USER_NAME, "", "", "Notify You've got a mail!",
                "mailto:" + REMOTE_USER_NAME + "?body=This is the notification email alert&Importance=High&X-Priority=1"));
        rules.add(new ZFilterRule("testRFCCompliantNotify", true, false, conditions, actions));

        return new ZFilterRules(rules);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFilterRFCCompliantNotify.class);
    }
}

