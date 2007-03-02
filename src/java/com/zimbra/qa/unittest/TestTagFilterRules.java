/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.zclient.ZFilterAction;
import com.zimbra.cs.zclient.ZFilterCondition;
import com.zimbra.cs.zclient.ZFilterRule;
import com.zimbra.cs.zclient.ZFilterRules;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.cs.zclient.ZFilterAction.ZTagAction;
import com.zimbra.cs.zclient.ZFilterCondition.HeaderOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZHeaderCondition;

public class TestTagFilterRules
extends TestCase
{
    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = "TestTagFilterRules";
    private static final String TAG_NAME = NAME_PREFIX;
    private static final String NEW_TAG_NAME = TAG_NAME + "2";
    
    private ZMailbox mMbox;
    private ZTag mTag;
    private ZFilterRules mOriginalRules;
    
    public void setUp()
    throws Exception {
        cleanUp();
        
        mMbox = TestUtil.getZMailbox(USER_NAME);
        mTag = mMbox.createTag(TAG_NAME, ZTag.Color.purple);
        mOriginalRules = mMbox.getFilterRules();
        mMbox.saveFilterRules(getRules());
    }

    /**
     * Verifies that filter rules are updated when a tag is renamed.
     */
    public void testRenameTag()
    throws Exception {
        // Send message 
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(USER_NAME);
        String subject = NAME_PREFIX + " testRenameTag 1";
        TestUtil.insertMessageLmtp(1, subject, recipient, sender);
        
        // Confirm that the original tag was applied
        ZMessage msg = TestUtil.getMessage(mMbox, "tag:" + TAG_NAME);
        TestUtil.verifyTag(mMbox, msg, TAG_NAME);
        
        // Rename tag
        mMbox.renameTag(mTag.getId(), NEW_TAG_NAME);

        // Confirm filter rules were updated
        ZTagAction action = (ZTagAction) mMbox.getFilterRules(true).getRules().get(0).getActions().get(0);
        assertEquals("Tag name didn't change", NEW_TAG_NAME, action.getTagName());
        
        // Send another message
        subject = NAME_PREFIX + " testRenameTag 2";
        TestUtil.insertMessageLmtp(2, subject, recipient, sender);
        
        // Confirm that the new tag name is now applied to both messages
        List<ZMessage> messages = TestUtil.search(mMbox, "tag:" + NEW_TAG_NAME);
        assertEquals("Incorrect number of tagged messages", 2, messages.size());
        for (ZMessage msg2 : messages) {
            TestUtil.verifyTag(mMbox, msg2, NEW_TAG_NAME);
        }
    }
    
    public void tearDown()
    throws Exception {
        mMbox.saveFilterRules(mOriginalRules);
        cleanUp();
    }
    
    public void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    private ZFilterRules getRules()
    throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        // if subject contains "TestTagFilterRules", tag TestTagFilterRules
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, NAME_PREFIX));
        actions.add(new ZTagAction(TAG_NAME));
        rules.add(new ZFilterRule(TAG_NAME, true, false, conditions, actions));
        return new ZFilterRules(rules);
    }

    public static void main(String[] args)
    throws Exception {
        CliUtil.toolSetup();
        TestUtil.runTest(new TestSuite(TestTagFilterRules.class), null);
    }
}
