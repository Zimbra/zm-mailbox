/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.soap.mail.message.CreateTagRequest;
import com.zimbra.soap.mail.message.CreateTagResponse;
import com.zimbra.soap.mail.message.MsgActionRequest;
import com.zimbra.soap.mail.message.MsgActionResponse;
import com.zimbra.soap.mail.type.ActionResult;
import com.zimbra.soap.mail.type.ActionSelector;
import com.zimbra.soap.mail.type.TagInfo;
import com.zimbra.soap.mail.type.TagSpec;

/**
 * @author bburtin
 */
public class TestTags {
    @Rule
    public TestName testInfo = new TestName();

    protected static String USER = null;
    private DbConnection mConn;
    private Mailbox mMbox;
    private Account mAccount;
    private String remoteUser;

    private static String TAG_PREFIX = "TestTags";
    private static String MSG_SUBJECT = "Test tags";

    private Message mMessage1;
    private Message mMessage2;
    private Message mMessage3;
    private Message mMessage4;
    private Conversation mConv;
    private Tag[] mTags = new Tag[0];

    /**
     * Creates the message used for tag tests
     */
    @Before
    public void setUp() throws Exception {
        ZimbraLog.test.debug("TestTags.setUp()");
        String testId = String.format("%s-%s-%d", this.getClass().getSimpleName(), testInfo.getMethodName(), (int)Math.abs(Math.random()*100));
        USER = String.format("%s-user", testId).toLowerCase();
        remoteUser = "test.tags.user@" + TestUtil.getDomain();

        //Always clean up before running tests
        cleanUp();

        mAccount = TestUtil.createAccount(USER);
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
        mConn = DbPool.getConnection();
        mMessage1 = TestUtil.addMessage(mMbox, MSG_SUBJECT + " 1");
        mMessage2 = TestUtil.addMessage(mMbox, MSG_SUBJECT + " 2");
        mMessage3 = TestUtil.addMessage(mMbox, MSG_SUBJECT + " 3");
        mMessage4 = TestUtil.addMessage(mMbox, MSG_SUBJECT + " 4");

        mConv = mMbox.getConversationById(null, mMessage1.getConversationId());
        refresh();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAddTag() throws Exception {
        CreateTagRequest createTagReq = new CreateTagRequest();
        TagSpec tag = new TagSpec("tag150146840461812563");
        tag.setColor((byte) 5);
        createTagReq.setTag(tag);
        ZMailbox zMbox = TestUtil.getZMailbox(USER);
        CreateTagResponse createResp = zMbox.invokeJaxb(createTagReq);
        assertNotNull("CreateTagResponse should not be null", createResp);
        TagInfo createdTag = createResp.getTag();
        assertNotNull("CreateTagResponse/tag should not be null", createdTag);
        assertNotNull("Created tag should have an ID", createdTag.getId());
        assertNotNull("Created tag should have a color", createdTag.getColor());
        assertTrue("Color of created tag should be 5", createdTag.getColor() == (byte)5);

        //use "update" and "t" element
        ActionSelector msgAction = ActionSelector.createForIdsAndOperation(Integer.toString(mMessage1.getId()), MailConstants.OP_UPDATE);
        msgAction.setTags(createdTag.getId());
        MsgActionRequest msgActionReq = new MsgActionRequest(msgAction);
        MsgActionResponse msgActionResp = zMbox.invokeJaxb(msgActionReq);
        assertNotNull("MsgActionResponse should not be null", msgActionResp);
        ActionResult res = msgActionResp.getAction();
        assertNotNull("MsgActionResponse/action should not be null", res);

        //use "tag" and "tag" element
        msgAction = ActionSelector.createForIdsAndOperation(Integer.toString(mMessage3.getId()), MailConstants.OP_TAG);
        msgAction.setTag(Integer.parseInt(createdTag.getId()));
        msgActionReq = new MsgActionRequest(msgAction);
        msgActionResp = zMbox.invokeJaxb(msgActionReq);
        assertNotNull("MsgActionResponse should not be null", msgActionResp);
        res = msgActionResp.getAction();
        assertNotNull("MsgActionResponse/action should not be null", res);
    }
    @Test
    public void testManyTags()
    throws Exception {
        // xxx bburtin: Don't run this test as part of the regular test suite,
        // since it takes almost 20 seconds to run.
        boolean runTest = false;
        TestUtil.assumeTrue("Don't run this test as part of the regular test suite", runTest);

        int numPrepares = ZimbraPerf.getPrepareCount();

        // Create the maximum number of tags, based on the number that already exist
        // in the mailbox
        int numTags = 256 - mMbox.getTagList(null).size();
        assertTrue("Can't create any new tags", numTags != 0);

        // Create tags
        mTags = new Tag[numTags];
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.createTag(null, TAG_PREFIX + (i + 1), (byte) 0);
        }
        refresh();

        // Assign each tag to M1
        for (int i = 0; i < mTags.length; i++) {
            mMbox.alterTag(null, mMessage1.getId(), mMessage1.getType(), mTags[i].getName(), true, null);
            refresh();
        }

        numPrepares = ZimbraPerf.getPrepareCount() - numPrepares;
        ZimbraLog.test.debug("testManyTags generated %d SQL statements.", numPrepares);
    }

    @Test
    public void testRemoveTag() throws Exception {
        // Create tags
        mTags = new Tag[4];
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.createTag(null, TAG_PREFIX + (i + 1), (byte)0);
        }
        refresh();

        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTags[0].getName(), true, null);
        // tag:TestTags1 -> (M1)
        Set<Integer> ids = search("tag:" + mTags[0].getName(), MailItem.Type.MESSAGE);
        assertEquals("1: result size", 1, ids.size());
        assertTrue("1: no message 1", ids.contains(new Integer(mMessage2.getId())));

        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTags[0].getName(), false, null);

        // tag:TestTags1 -> none
        ids = search("tag:" + mTags[0].getName(), MailItem.Type.MESSAGE);
        assertEquals("0: result size", 0, ids.size());
    }

    @Test
    public void testNonExistentTagSearch()
    throws Exception {
        Set<Integer> ids = search("tag:nonexistent", MailItem.Type.MESSAGE);
        assertEquals("search for tag:nonexistent result size", 0, ids.size());
    }

    @Test
    public void testTagSearch()
    throws Exception {
        // Create tags
        mTags = new Tag[4];
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.createTag(null, TAG_PREFIX + (i + 1), (byte)0);
        }
        refresh();

        // First assign T1 to the entire conversation, then remove it from M2-M4
        mMbox.alterTag(null, mConv.getId(), mConv.getType(), mTags[0].getName(), true, null);
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTags[0].getName(), false, null);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTags[0].getName(), false, null);
        mMbox.alterTag(null, mMessage4.getId(), mMessage4.getType(), mTags[0].getName(), false, null);

        // Assign tags:
        //   M1: T1
        //   M2: T2
        //   M3: T2, T3
        //   M4: no tags
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTags[1].getName(), true, null);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTags[1].getName(), true, null);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTags[2].getName(), true, null);
        refresh();

        // tag:TestTags1 -> (M1)
        Set<Integer> ids = search("tag:" + mTags[0].getName(), MailItem.Type.MESSAGE);
        assertEquals("1: result size", 1, ids.size());
        assertTrue("1: no message 1", ids.contains(new Integer(mMessage1.getId())));

        // tag:TestTags1 or tag:TestTags2 -> (M1,M2,M3)
        ids = search("tag:" + mTags[0].getName() + " or tag:" + mTags[1].getName(), MailItem.Type.MESSAGE);
        assertEquals("2a: result size", 3, ids.size());
        assertTrue("2a: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("2a: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("2a: no message 3", ids.contains(new Integer(mMessage3.getId())));

        // tag:TestTags2 tag:TestTags3 -> (M3)
        ids = search("tag:" + mTags[1].getName() + " tag:" + mTags[2].getName(), MailItem.Type.MESSAGE);
        assertEquals("2b: result size", 1, ids.size());
        assertTrue("2b: no message 3", ids.contains(new Integer(mMessage3.getId())));

        // not tag:TestTags1 -> (M2,M3,M4,...)
        ids = search("not tag:" + mTags[0].getName(), MailItem.Type.MESSAGE);
        assertFalse("3: message 1 found", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("3: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("3: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("3: no message 4", ids.contains(new Integer(mMessage4.getId())));

        // not tag:TestTags2 not tag:TestTags3 -> (M1,M4,...)
        ids = search("not tag:" + mTags[1].getName() + " not tag:" + mTags[2].getName(), MailItem.Type.MESSAGE);
        assertTrue("4: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("4: contains message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("4: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("4: no message 4", ids.contains(new Integer(mMessage4.getId())));

        // tag:TestTags2 not tag:TestTags3 -> (M2)
        ids = search("tag:" + mTags[1].getName() + " not tag:" + mTags[2].getName(), MailItem.Type.MESSAGE);
        assertFalse("5: message 1 found", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("5: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("5: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("5: contains message 4", ids.contains(new Integer(mMessage4.getId())));

        // tag:TestTags4 -> ()
        ids = search("tag:" + mTags[3].getName(), MailItem.Type.MESSAGE);
        assertEquals("6: search should have returned no results", 0, ids.size());
    }

    /**
     * Bug 79576 was seeing extra (wrong) hits from shared folder when click on a tag.
     */
    @Test
    public void testRemoteTagSearch()
    throws Exception {
        Account remoteAcct = TestUtil.createAccount(remoteUser);
        remoteAcct = TestUtil.getAccount(remoteUser);
        Mailbox remoteMbox = MailboxManager.getInstance().getMailboxByAccount(remoteAcct);
        remoteMbox.grantAccess(null, Mailbox.ID_FOLDER_INBOX, mAccount.getId(),
                ACL.GRANTEE_USER,(short) (ACL.RIGHT_READ | ACL.RIGHT_WRITE | ACL.RIGHT_INSERT), null);
        mMbox.createMountpoint(null, Mailbox.ID_FOLDER_USER_ROOT, "remoteInbox", remoteAcct.getId(),
                Mailbox.ID_FOLDER_INBOX, null, MailItem.Type.MESSAGE, Flag.ID_CHECKED, (byte) 2, false);
        Message remoteMsg1 = TestUtil.addMessage(remoteMbox, MSG_SUBJECT + " in shared inbox tagged with shared TAG");
        Message remoteMsg2 = TestUtil.addMessage(remoteMbox, MSG_SUBJECT + " in shared inbox tagged remOnly");
        Tag[] remoteTags = new Tag[2];
        remoteTags[0] = remoteMbox.createTag(null, TAG_PREFIX + 1, (byte)0);
        remoteTags[1] = remoteMbox.createTag(null, TAG_PREFIX + "remOnly", (byte)0);
        mTags = new Tag[2];
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.createTag(null, TAG_PREFIX + (i + 1), (byte)0);
        }
        refresh();
        remoteMbox.alterTag(null, remoteMsg1.getId(), remoteMsg1.getType(), remoteTags[0].getName(), true, null);
        remoteMbox.alterTag(null, remoteMsg2.getId(), remoteMsg2.getType(), remoteTags[1].getName(), true, null);
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTags[0].getName(), true, null);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTags[1].getName(), true, null);
        Folder sharedFolder = mMbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, "remoteInbox");
        sharedFolder.getId();
        List<ZimbraHit> hits = TestUtil.searchForHits(mMbox,
                String.format("tag:\"%s\" (inid:%d OR is:local)", mTags[0].getName(), sharedFolder.getId()),
                MailItem.Type.MESSAGE);
        assertEquals("Search for tag present in both local and remote mboxes.  Number of hits returned", 2, hits.size());
        boolean gotLocalHit = false;
        boolean gotRemoteHit = false;
        ZimbraLog.test.setLevel(Level.trace);
        for (ZimbraHit hit : hits) {
            ZimbraLog.test.info("HIT %s", hit.getParsedItemID());
            ItemId parsedId = hit.getParsedItemID();
            if (parsedId.belongsTo(mAccount) && hit.getItemId() == mMessage2.getId()) {
                gotLocalHit = true;
            }
            if (parsedId.belongsTo(remoteAcct) && hit.getItemId() == remoteMsg1.getId()) {
                gotRemoteHit = true;
            }
        }
        assertTrue("1st search should return one local hit", gotLocalHit);
        assertTrue("1st search should return one remote hit", gotRemoteHit);

        Set<Integer> ids = search(
                String.format("tag:\"%s\" (inid:%d OR is:local)", mTags[1].getName(), sharedFolder.getId()),
                MailItem.Type.MESSAGE);
        assertEquals("Search for tag not present in remote mbox. Number of ids returned", 1, ids.size());
        assertTrue("2nd search should contain message 3", ids.contains(new Integer(mMessage3.getId())));
    }

    @Test
    public void testFlagSearch() throws Exception {
        // First assign T1 to the entire conversation, then remove it from M2-M4
        mMbox.alterTag(null, mConv.getId(), mConv.getType(), Flag.FlagInfo.REPLIED, true, null);
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), Flag.FlagInfo.REPLIED, false, null);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), Flag.FlagInfo.REPLIED, false, null);
        mMbox.alterTag(null, mMessage4.getId(), mMessage4.getType(), Flag.FlagInfo.REPLIED, false, null);

        // Assign tags:
        //   M1: replied
        //   M2: flagged
        //   M3: flagged, forwarded
        //   M4: no flags
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), Flag.FlagInfo.FLAGGED, true, null);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), Flag.FlagInfo.FLAGGED, true, null);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), Flag.FlagInfo.FORWARDED, true, null);
        refresh();

        // is:replied -> (M1,...)
        Set<Integer> ids = search("is:replied", MailItem.Type.MESSAGE);
        assertTrue("1: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("1: message 2 found", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("1: message 3 found", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("1: message 4 found", ids.contains(new Integer(mMessage4.getId())));

        // is:flagged is:forwarded -> (M3,...)
        ids = search("is:flagged is:forwarded", MailItem.Type.MESSAGE);
        assertFalse("2a: message 1 found", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("2a: message 2 found", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("2a: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("2a: message 4 found", ids.contains(new Integer(mMessage4.getId())));

        // is:replied or is:flagged -> (M2,M3,...)
        ids = search("is:replied or is:flagged", MailItem.Type.MESSAGE);
        assertTrue("2b: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("2b: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("2b: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("2b: message 4 found", ids.contains(new Integer(mMessage4.getId())));


        // not is:replied -> (M2,M3,M4,...)
        ids = search("not is:replied", MailItem.Type.MESSAGE);
        assertFalse("3: contains message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("3: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("3: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("3: no message 4", ids.contains(new Integer(mMessage4.getId())));

        // not is:flagged not is:forwarded -> (M1,M4,...)
        ids = search("not is:flagged not is:forwarded", MailItem.Type.MESSAGE);
        assertTrue("4: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("4: contains message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("4: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("4: no message 4", ids.contains(new Integer(mMessage4.getId())));

        // is:flagged not is:forwarded -> (M2)
        ids = search("is:flagged not is:forwarded", MailItem.Type.MESSAGE);
        assertFalse("5: contains message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("5: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("5: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("5: contains message 4", ids.contains(new Integer(mMessage4.getId())));

        // tag:\Deleted -> ()
        // Cannot search for tag:\\Deleted with the old-style Mailbox.search(String query...) API
        // need to update test code to use the new API Mailbox.search(SearchParams...) API
        // and specify SearchParams.setIncludeTagDeleted(true) on the params
        //
        //        ids = search("tag:\\Deleted", MailItem.TYPE_MESSAGE);
        //        assertFalse("6: contains message 1", ids.contains(new Integer(mMessage1.getId())));
        //        assertFalse("6: contains message 2", ids.contains(new Integer(mMessage2.getId())));
        //        assertFalse("6: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        //        assertFalse("6: contains message 4", ids.contains(new Integer(mMessage4.getId())));
    }

    @Test
    public void testSearchUnreadAsTag() throws Exception {
        boolean unseenSearchSucceeded = false;
        try {
            search("tag:\\Unseen", MailItem.Type.MESSAGE);
            unseenSearchSucceeded = true;
        } catch (ServiceException e) {
            assertEquals("Unexpected exception type", MailServiceException.INVALID_NAME, e.getCode());
        }
        assertFalse("tag:\\Unseen search should not have succeeded", unseenSearchSucceeded);

        Set<Integer> isUnreadIds = search("is:unread", MailItem.Type.MESSAGE);
        Set<Integer> tagUnreadIds = search("tag:\\Unread", MailItem.Type.MESSAGE);
        if (!(isUnreadIds.containsAll(tagUnreadIds))) {
            fail("Mismatch in search results.  is:unread returned (" +
                StringUtil.join(",", isUnreadIds) + "), tag:\\Unread returned (" +
                StringUtil.join(",", tagUnreadIds) + ")");
        }
    }

    private Set<Integer> search(String query, MailItem.Type type) throws Exception {
        List<Integer> ids = TestUtil.search(mMbox, query, type);
        return new HashSet<Integer>(ids);
    }

    private void refresh()
    throws Exception {
        if (mMessage1 != null) {
            mMessage1 = mMbox.getMessageById(null, mMessage1.getId());
        }
        if (mMessage2 != null) {
            mMessage2 = mMbox.getMessageById(null, mMessage2.getId());
        }
        if (mMessage3 != null) {
            mMessage3 = mMbox.getMessageById(null, mMessage3.getId());
        }
        if (mMessage4 != null) {
            mMessage4 = mMbox.getMessageById(null, mMessage4.getId());
        }
        if (mConv != null) {
            mConv = mMbox.getConversationById(null, mConv.getId());
        }
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.getTagById(null, mTags[i].getId());
        }
    }

    @After
    public void tearDown() throws Exception {
        ZimbraLog.test.debug("TestTags.tearDown()");

        cleanUp();

        DbPool.quietClose(mConn);
    }

    private void cleanUp() throws Exception {
        try {
            TestUtil.deleteAccountIfExists(remoteUser);
            TestUtil.deleteAccountIfExists(USER);
        } catch (Exception e) {
        }
    }

}
