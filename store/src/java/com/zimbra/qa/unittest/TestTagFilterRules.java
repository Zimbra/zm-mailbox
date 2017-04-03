/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZFilterAction;
import com.zimbra.client.ZFilterAction.ZTagAction;
import com.zimbra.client.ZFilterCondition;
import com.zimbra.client.ZFilterCondition.HeaderOp;
import com.zimbra.client.ZFilterCondition.ZHeaderCondition;
import com.zimbra.client.ZFilterRule;
import com.zimbra.client.ZFilterRules;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZTag;

public class TestTagFilterRules {
    private static final String USER_NAME = TestTagFilterRules.class.getSimpleName();
    private static final String TAG_NAME = USER_NAME;
    private static final String TAG2_NAME = USER_NAME+ "2";
    private static final String SUBJECT_PREFIX = USER_NAME;
    private static final String NEW_TAG_NAME = TAG_NAME + " new";

    private ZMailbox mMbox;
    private ZTag mTag;
    private ZTag mTag2;

    @Before
    public void setUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.createAccount(USER_NAME);
        mMbox = TestUtil.getZMailbox(USER_NAME);
        mTag = mMbox.createTag(TAG_NAME, ZTag.Color.purple);
        mTag2 = mMbox.createTag(TAG2_NAME, ZTag.Color.green);
        mMbox.saveIncomingFilterRules(getRules());
    }

    /**
     * Verifies that filter rules are updated when a tag is renamed.
     */
    @Test
    public void testRenameTag()
    throws Exception {
        // Send message
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(USER_NAME);
        String subject = SUBJECT_PREFIX + "testRenameTag 1";
        TestUtil.addMessageLmtp(subject, recipient, sender);

        // Confirm that the original tag was applied
        ZMessage msg = TestUtil.getMessage(mMbox, "tag:" + TAG_NAME);
        TestUtil.verifyTag(mMbox, msg, TAG_NAME);

        // Rename tag
        mMbox.renameTag(mTag.getId(), NEW_TAG_NAME);

        // Confirm filter rules were updated
        ZTagAction action = (ZTagAction) mMbox.getIncomingFilterRules(true).getRules().get(0).getActions().get(0);
        assertEquals("Tag name didn't change", NEW_TAG_NAME, action.getTagName());

        // Send another message
        subject = SUBJECT_PREFIX + " testRenameTag 2";
        TestUtil.addMessageLmtp(subject, recipient, sender);

        // Confirm that the new tag name is now applied to both messages
        List<ZMessage> messages = TestUtil.search(mMbox, "tag:\"" + NEW_TAG_NAME + "\"");
        assertEquals("Incorrect number of tagged messages", 2, messages.size());
        for (ZMessage msg2 : messages) {
            TestUtil.verifyTag(mMbox, msg2, NEW_TAG_NAME);
        }
    }

    /**
     * Verifies that filter rules are disabled when a tag is deleted.
     */
    @Test
    public void testDeleteTag()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        mbox.deleteTag(mTag.getId());

        // Deliver message that used to match the tag rule and make sure
        // that the message is not tagged.
        String subject = SUBJECT_PREFIX + " testDeleteTag";
        TestUtil.addMessageLmtp(subject, USER_NAME, USER_NAME);
        ZMessage msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        assertEquals(mTag2.getId(), msg.getTagIds());

        // Confirm that the first rule was disabled and the second was not.
        ZFilterRule rule1 = TestUtil.getFilterRule(mbox, TAG_NAME);
        assertFalse(rule1.isActive());
        ZFilterRule rule2 = TestUtil.getFilterRule(mbox, TAG2_NAME);
        assertTrue(rule2.isActive());
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    private ZFilterRules getRules()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        // if subject contains "TestTagFilterRules", tag TestTagFilterRules
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, SUBJECT_PREFIX));
        actions.add(new ZTagAction(TAG_NAME));
        rules.add(new ZFilterRule(TAG_NAME, true, false, conditions, actions));

        // if subject contains "TestTagFilterRules", tag TestTagFilterRules2
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, SUBJECT_PREFIX));
        actions.add(new ZTagAction(TAG2_NAME));
        rules.add(new ZFilterRule(TAG2_NAME, true, false, conditions, actions));

        return new ZFilterRules(rules);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestTagFilterRules.class);
    }
}
