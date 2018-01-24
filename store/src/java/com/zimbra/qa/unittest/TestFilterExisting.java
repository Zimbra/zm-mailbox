/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZFilterAction;
import com.zimbra.client.ZFilterAction.MarkOp;
import com.zimbra.client.ZFilterAction.ZDiscardAction;
import com.zimbra.client.ZFilterAction.ZFileIntoAction;
import com.zimbra.client.ZFilterAction.ZKeepAction;
import com.zimbra.client.ZFilterAction.ZMarkAction;
import com.zimbra.client.ZFilterAction.ZRedirectAction;
import com.zimbra.client.ZFilterAction.ZTagAction;
import com.zimbra.client.ZFilterCondition;
import com.zimbra.client.ZFilterCondition.HeaderOp;
import com.zimbra.client.ZFilterCondition.ZHeaderCondition;
import com.zimbra.client.ZFilterRule;
import com.zimbra.client.ZFilterRules;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZTag;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.Mailbox;

public class TestFilterExisting {

    @Rule
    public TestName testInfo = new TestName();
    private static String USER_NAME = null;
    private static String USER_NAME2 = null;
    private static final String NAME_PREFIX = TestFilterExisting.class.getSimpleName();

    private static final String FOLDER1_NAME = NAME_PREFIX + "-folder1";
    private static final String FOLDER2_NAME = NAME_PREFIX + "-folder2";
    private static final String FOLDER3_NAME = NAME_PREFIX + "-folder3";
    private static final String FOLDER1_PATH = "/" + FOLDER1_NAME;
    private static final String FOLDER2_PATH = "/" + FOLDER2_NAME;
    private static final String FOLDER3_PATH = "/" + FOLDER3_NAME;
    private static final String TAG_NAME = NAME_PREFIX + "-tag";
    private static final String KEEP_RULE_NAME = NAME_PREFIX + " keep";
    private static final String TAG_RULE_NAME = NAME_PREFIX + " tag";
    private static final String FLAG_RULE_NAME = NAME_PREFIX + " flag";
    private static final String MARK_READ_RULE_NAME = NAME_PREFIX + " mark read";
    private static final String FOLDER1_RULE_NAME = NAME_PREFIX + " folder1";
    private static final String FOLDER1_AND_FLAG_RULE_NAME = NAME_PREFIX + " folder1 and flag";
    private static final String FOLDER2_RULE_NAME = NAME_PREFIX + " folder2";
    private static final String FOLDER3_RULE_NAME = NAME_PREFIX + " folder3";
    private static final String DISCARD_RULE_NAME = NAME_PREFIX + " discard";
    private static final String REDIRECT_RULE_NAME = NAME_PREFIX + " redirect";

    private ZFilterRules originalRules;
    private String originalBatchSize;
    private String originalSleepInterval;

    @Before
    public void setUp() throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user";
        USER_NAME2 = prefix + "user2";
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(USER_NAME2);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZMailbox mbox2 = TestUtil.getZMailbox(USER_NAME2);
        originalRules = mbox.getIncomingFilterRules();
        originalBatchSize = TestUtil.getAccountAttr(USER_NAME, Provisioning.A_zimbraFilterBatchSize);
        originalSleepInterval = TestUtil.getAccountAttr(USER_NAME, Provisioning.A_zimbraFilterSleepInterval);
        saveNewRules();

        // Speed up the test.
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraFilterSleepInterval, "0");
    }

    /**
     * Tests {@link RuleManager#getRuleByName}.
     */
    @Test
    public void testGetRule() throws Exception {
        String rule1 = "# Rule 1\r\nabc\r\n";
        String rule2 = "# Rule 2\r\ndef\r\n";
        String script = rule1 + rule2;
        Assert.assertEquals(rule1, RuleManager.getRuleByName(script, "Rule 1").getSecond());
        Assert.assertEquals(rule2, RuleManager.getRuleByName(script, "Rule 2").getSecond());
    }
    @Test
    public void testKeep() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " test keep";
        String id = TestUtil.addMessage(mbox, subject);
        String query = "in:inbox subject:\"" + subject + "\"";
        String[] ruleNames = new String[] { KEEP_RULE_NAME };

        // Test keep in inbox by id.
        Set<String> affectedIds = runRules(ruleNames, id, null);
        Assert.assertEquals(0, affectedIds.size());
        TestUtil.getMessage(mbox, query);

        // Test keep in inbox by query.
        affectedIds = runRules(ruleNames, null, "in:inbox");
        Assert.assertEquals(0, affectedIds.size());
        TestUtil.getMessage(mbox, query);

        // Move message to folder1.
        ZFolder folder1 = TestUtil.createFolder(mbox, FOLDER1_PATH);
        mbox.moveMessage(id, folder1.getId());

        // Test keep in folder1 by id.
        affectedIds = runRules(ruleNames, id, null);
        Assert.assertEquals(0, affectedIds.size());
        query = "in:" + FOLDER1_NAME + " subject:\"" + subject + "\"";
        TestUtil.getMessage(mbox, query);

        // Test keep in folder1 by query.
        affectedIds = runRules(ruleNames, null, "in:" + FOLDER1_NAME);
        Assert.assertEquals(0, affectedIds.size());
        TestUtil.getMessage(mbox, query);
    }
    @Test
    public void testFileInto() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.createFolder(mbox, FOLDER1_PATH);
        TestUtil.createFolder(mbox, FOLDER2_PATH);

        String subject = NAME_PREFIX + " test keep, fileinto folder1 and folder2";
        String id = TestUtil.addMessage(mbox, subject);
        String[] ruleNames = new String[] { FOLDER1_RULE_NAME };

        // Test file into folder1 by id.
        Set<String> affectedIds = runRules(ruleNames, id, null);
        Assert.assertEquals(1, affectedIds.size());
        Assert.assertTrue(affectedIds.contains(id));
        assertMoved("inbox", FOLDER1_NAME, subject);

        // Test file into folder2 by query.
        ZMessage msg = TestUtil.getMessage(mbox, "in:" + FOLDER1_NAME + " subject:\"" + subject + "\"");
        id = msg.getId();
        ruleNames = new String[] { FOLDER2_RULE_NAME };
        affectedIds = runRules(ruleNames, null, "in:" + FOLDER1_NAME);
        Assert.assertEquals(1, affectedIds.size());
        Assert.assertTrue(affectedIds.contains(id));
        assertMoved(FOLDER1_NAME, FOLDER2_NAME, subject);

        // Move message back to inbox.
        msg = TestUtil.getMessage(mbox, "in:" + FOLDER2_NAME + " subject:\"" + subject + "\"");
        id = msg.getId();
        mbox.moveMessage(id, Integer.toString(Mailbox.ID_FOLDER_INBOX));

        // Test keep and file into folder1 and folder2.
        ruleNames = new String[] { KEEP_RULE_NAME, FOLDER1_RULE_NAME, FOLDER2_RULE_NAME };
        affectedIds = runRules(ruleNames, id, null);
        Assert.assertEquals(1, affectedIds.size());
        Assert.assertTrue(affectedIds.contains(id));
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        TestUtil.getMessage(mbox, "in:" + FOLDER1_NAME + " subject:\"" + subject + "\"");
        TestUtil.getMessage(mbox, "in:" + FOLDER2_NAME + " subject:\"" + subject + "\"");
    }
    @Test
    public void testTag() throws Exception {
        // Add message
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZTag tag = mbox.createTag(TAG_NAME, null);
        String subject = NAME_PREFIX + " test tag";
        String id = TestUtil.addMessage(mbox, subject);
        ZMessage msg = mbox.getMessageById(id);

        // Run flag rule and make sure the message is not tagged.
        Set<String> affectedIds = runRules(new String[] { FLAG_RULE_NAME }, id, null);
        Assert.assertEquals(0, affectedIds.size());
        Assert.assertFalse(msg.hasTags());

        // Run tag rule and make sure the message is tagged.
        affectedIds = runRules(new String[] { TAG_RULE_NAME }, id, null);
        Assert.assertEquals(1, affectedIds.size());
        Assert.assertTrue(affectedIds.contains(id));
        mbox.noOp();
        msg = mbox.getMessageById(id);
        Assert.assertEquals(tag.getId(), msg.getTagIds());
    }
    @Test
    public void testFlag() throws Exception {
        // Add message
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " test flag";
        String id = TestUtil.addMessage(mbox, subject);
        ZMessage msg = mbox.getMessageById(id);

        // Run tag rule and make sure the message is not tagged.
        Set<String> affectedIds = runRules(new String[] { TAG_RULE_NAME }, id, null);
        Assert.assertEquals(0, affectedIds.size());
        Assert.assertFalse(msg.isFlagged());

        // Run flag rule and make sure the message is flagged.
        affectedIds = runRules(new String[] { FLAG_RULE_NAME }, id, null);
        mbox.noOp();
        msg = mbox.getMessageById(id);
        Assert.assertTrue(msg.isFlagged());
    }
    @Test
    public void testMarkRead() throws Exception {
        // Add message
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " test mark read";
        String id = TestUtil.addMessage(mbox, subject);
        mbox.markMessageRead(id, false);
        ZMessage msg = mbox.getMessageById(id);
        Assert.assertTrue(msg.isUnread());

        // Run mark unread rule and validate.
        Set<String> affectedIds = runRules(new String[] { MARK_READ_RULE_NAME }, id, null);
        Assert.assertEquals(1, affectedIds.size());
        mbox.noOp();
        Assert.assertFalse(msg.isUnread());
    }
    @Test
    public void testDiscard() throws Exception {
        // Add message
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " test keep, fileinto folder1, and discard";
        String id = TestUtil.addMessage(mbox, subject);

        // Run the keep and discard rules, and make sure the message was kept.
        Set<String> affectedIds = runRules(new String[] { KEEP_RULE_NAME, DISCARD_RULE_NAME }, id, null);
        Assert.assertEquals(0, affectedIds.size());
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + subject + "\"");

        // Run the discard and fileinto rules, and make sure the message was filed.
        affectedIds = runRules(new String[] { DISCARD_RULE_NAME, FOLDER1_RULE_NAME }, id, null);
        assertMoved("inbox", FOLDER1_NAME, subject);

        // Run the discard rule by itself and make sure the message was deleted.
        String query = "in:" + FOLDER1_NAME + " subject:\"" + subject + "\"";
        ZMessage msg = TestUtil.getMessage(mbox, query);
        affectedIds = runRules(new String[] { DISCARD_RULE_NAME }, null, query);
        Assert.assertEquals(1, affectedIds.size());
        Assert.assertTrue(affectedIds.contains(msg.getId()));
        List<ZMessage> messages = TestUtil.search(mbox, query);
        Assert.assertEquals(0, messages.size());
    }
    @Test
    public void testRedirect() throws Exception {
        // Add message
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " test redirect";
        String id = TestUtil.addMessage(mbox, subject);
        String query = "in:inbox subject:\"" + subject + "\"";

        // Run the keep and discard rules, and make sure the message was kept.
        Set<String> affectedIds = runRules(new String[] { REDIRECT_RULE_NAME }, id, null);
        Assert.assertEquals(0, affectedIds.size());
        TestUtil.getMessage(mbox, query);

        // Make sure that the redirect action was ignored, and the message
        // does not exist in user2's mailbox.
        ZMailbox mbox2 = TestUtil.getZMailbox(USER_NAME2);
        Assert.assertEquals(0, TestUtil.search(mbox2, "in: inbox subject:\"" + subject + "\"").size());
    }

    private class RunRule implements Runnable {

        private final String mRuleName;
        private final String mIdList;
        private Exception mError;

        private RunRule(String ruleName, String idList) {
            mRuleName = ruleName;
            mIdList = idList;
        }

        @Override
        public void run() {
            try {
                runRules(new String[] { mRuleName }, mIdList, null);
            } catch (Exception e) {
                mError = e;
            }
        }

        private Exception getError() {
            return mError;
        }
    }

    /**
     * Simultaneously flags and discards the same set of messages (bug 41609).
     */
    @Test
    public void testSimultaneous() throws Exception {
        // Add messages.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<String> msgIds = new ArrayList<String>();
        for (int i = 1; i <= 10; i++) {
            msgIds.add(TestUtil.addMessage(mbox, NAME_PREFIX + " discard flag " + i));
        }
        String idList = StringUtil.join(",", msgIds);

        // Start two threads that simultaneously process the same set of messages.
        RunRule runDiscard = new RunRule(DISCARD_RULE_NAME, idList);
        RunRule runFlag = new RunRule(FLAG_RULE_NAME, idList);
        Thread discardThread = new Thread(runDiscard);
        Thread flagThread = new Thread(runFlag);
        discardThread.start();
        flagThread.start();
        discardThread.join();
        flagThread.join();

        // Make sure there were no errors.
        Exception e = runDiscard.getError();
        if (e != null) {
            Assert.fail(e.toString());
        }
        e = runFlag.getError();
        if (e != null) {
            Assert.fail(e.toString());
        }
    }

    /**
     * Confirms that filing into the same folder doesn't result in a duplicate copy
     * of the message (bug 42051).
     */
    @Test
    public void testFileIntoSameFolder() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder3 = TestUtil.createFolder(mbox, FOLDER3_PATH);
        String subject = NAME_PREFIX + " test folder3";
        String id = TestUtil.addMessage(mbox, subject, folder3.getId(), null);

        // Run the folder3 rule, and make sure one message matches instead of two.
        Set<String> affectedIds = runRules(new String[] { FOLDER3_RULE_NAME }, id, null);
        Assert.assertEquals(0, affectedIds.size());
        TestUtil.getMessage(mbox, "subject:\"" + subject + "\"");
    }

    /**
     * Confirms that a flag rule runs when a message is filed into the same
     * folder (bug 44588).
     */
    @Test
    public void testFileIntoSameFolderAndFlag() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder1 = TestUtil.createFolder(mbox, FOLDER1_PATH);
        String subject = NAME_PREFIX + " test folder1 and flag";
        String id = TestUtil.addMessage(mbox, subject, folder1.getId(), null);
        ZMessage msg = mbox.getMessageById(id);
        Assert.assertTrue(StringUtil.isNullOrEmpty(msg.getFlags()));

        // Run the rule, make sure the message was flagged but not moved.
        Set<String> affectedIds = runRules(new String[] { FOLDER1_AND_FLAG_RULE_NAME }, id, null);
        Assert.assertEquals(1, affectedIds.size());
        msg = TestUtil.getMessage(mbox, "subject:\"" + subject + "\"");
        Assert.assertEquals("f", msg.getFlags());
    }

    /**
     * Confirms that we're enforcing {@code zimbraFilterBatchSize}.
     */
    @Test
    public void testBatchSize() throws Exception {
        TestUtil.getAccount(USER_NAME).setFilterBatchSize(1);

        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " testBatchSize flag";
        String msg1Id = TestUtil.addMessage(mbox, subject + " 1");
        String msg2Id = TestUtil.addMessage(mbox, subject + " 2");
        try {
            runRules(new String[] { FLAG_RULE_NAME }, null, "subject: \'" + subject + "\'");
            Assert.fail("Batch size was not enforced");
        } catch (SoapFaultException e) {
            String msg = e.getMessage();
            Assert.assertTrue(msg.contains("2 messages"));
            Assert.assertTrue(msg.contains("limit of 1"));
        }

        // Make sure the rule was not executed.
        ZMessage msg = mbox.getMessageById(msg1Id);
        if (msg.hasFlags()) {
            Assert.assertFalse(msg.getFlags().contains("f"));
        }
        msg = mbox.getMessageById(msg2Id);
        if (msg.hasFlags()) {
            Assert.assertFalse(msg.getFlags().contains("f"));
        }
    }

    /**
     * Confirms that {@code zimbraFilterSleepInterval slows down
     * {@code ApplyFilterRules}.
     */
    @Test
    public void testSleepInterval() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " testSleepInterval";
        TestUtil.addMessage(mbox, subject + " 1");
        TestUtil.addMessage(mbox, subject + " 2");

        TestUtil.getAccount(USER_NAME).setFilterSleepInterval("500ms");
        long startTime = System.currentTimeMillis();
        runRules(new String[] { KEEP_RULE_NAME }, null, "subject: \'" + subject + "\'");
        Assert.assertTrue(System.currentTimeMillis() - startTime > 500);
    }

    private void assertMoved(String sourceFolderName, String destFolderName, String subject)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, "in:" + sourceFolderName + " subject:\"" + subject + "\"");
        Assert.assertEquals(0, messages.size());
        messages = TestUtil.search(mbox, "in:" + destFolderName + " subject:\"" + subject + "\"");
        Assert.assertEquals(1, messages.size());
    }

    /**
     * Runs filter rules on existing messages.
     *
     * @param ruleNames names of rules to execute
     * @param idList comma-separated list of message id's, or <tt>null</tt> if
     * the query should be used
     * @param query a search query, or <tt>null</tt> if the id list should be used
     * @return the affected message id's
     */
    private Set<String> runRules(String[] ruleNames, String idList, String query)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        // Assemble request.
        Element request = new XMLElement(MailConstants.APPLY_FILTER_RULES_REQUEST);
        Element rulesEl = request.addElement(MailConstants.E_FILTER_RULES);
        for (String ruleName : ruleNames) {
            rulesEl.addElement(MailConstants.E_FILTER_RULE).addAttribute(MailConstants.A_NAME, ruleName);
        }
        if (idList != null) {
            request.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_IDS, idList);
        }
        if (query != null) {
            request.addElement(MailConstants.E_QUERY).setText(query);
        }

        // Invoke and parse response.
        Element response = mbox.invoke(request);
        Set<String> affectedIds = new TreeSet<String>();
        Element msgEl = response.getOptionalElement(MailConstants.E_MSG);
        if (msgEl != null) {
            for (String id : msgEl.getAttribute(MailConstants.A_IDS).split(",")) {
                affectedIds.add(id);
            }
        }
        return affectedIds;
    }

    private void saveNewRules() throws Exception {
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();

        // if subject contains "tag", tag
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "tag"));
        actions.add(new ZTagAction(TAG_NAME));
        rules.add(new ZFilterRule(TAG_RULE_NAME, true, false, conditions, actions));

        // if subject contains "flag", flag
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "flag"));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule(FLAG_RULE_NAME, true, false, conditions, actions));

        // if subject contains "mark read", mark read
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "mark read"));
        actions.add(new ZMarkAction(MarkOp.READ));
        rules.add(new ZFilterRule(MARK_READ_RULE_NAME, true, false, conditions, actions));

        // if subject contains "keep", keep
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "keep"));
        actions.add(new ZKeepAction());
        rules.add(new ZFilterRule(KEEP_RULE_NAME, true, false, conditions, actions));

        // if subject contains "folder1", file into folder1
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "folder1"));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        rules.add(new ZFilterRule(FOLDER1_RULE_NAME, true, false, conditions, actions));

        // if the subject contains "folder1 and flag", file into folder1 and flag
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "folder1 and flag"));
        actions.add(new ZFileIntoAction(FOLDER1_PATH));
        actions.add(new ZMarkAction(MarkOp.FLAGGED));
        rules.add(new ZFilterRule(FOLDER1_AND_FLAG_RULE_NAME, true, false, conditions, actions));

        // if subject contains "folder2", file into folder2
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "folder2"));
        actions.add(new ZFileIntoAction(FOLDER2_PATH));
        rules.add(new ZFilterRule(FOLDER2_RULE_NAME, true, false, conditions, actions));

        // if subject contains "folder3", file into folder3.  This one uses the
        // folder name without the leading slash.
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "folder3"));
        actions.add(new ZFileIntoAction(FOLDER3_NAME));
        rules.add(new ZFilterRule(FOLDER3_RULE_NAME, true, false, conditions, actions));

        // if subject contains "discard", discard
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "discard"));
        actions.add(new ZDiscardAction());
        rules.add(new ZFilterRule(DISCARD_RULE_NAME, true, false, conditions, actions));

        // If subject contains "redirect", redirect to user2.  This rule should
        // be ignored when applied to existing messages.
        conditions = new ArrayList<ZFilterCondition>();
        actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "redirect"));
        actions.add(new ZRedirectAction(TestUtil.getAddress(USER_NAME2)));
        rules.add(new ZFilterRule(REDIRECT_RULE_NAME, true, false, conditions, actions));

        // Save rules
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        mbox.saveIncomingFilterRules(new ZFilterRules(rules));
    }

    @After
    public void tearDown() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        mbox.saveIncomingFilterRules(originalRules);
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraFilterBatchSize, originalBatchSize);
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraFilterSleepInterval, originalSleepInterval);
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(USER_NAME2);
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFilterExisting.class);
    }
}
