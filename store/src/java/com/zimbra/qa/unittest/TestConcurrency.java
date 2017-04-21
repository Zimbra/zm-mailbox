/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcGetMsgRequest;
import com.zimbra.cs.client.soap.LmcMsgActionRequest;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;

/**
 * Tests concurrent operations on the same mailbox.
 *
 * @author bburtin
 */
public class TestConcurrency {

    @Rule
    public TestName testInfo = new TestName();

    public static final String TAG_PREFIX = "TestConcurrency";
    public static final String FOLDER_NAME = "TestConcurrency";

    Account mAccount;
    Mailbox mMbox;

    @Before
    public void setUp() throws Exception {
        mAccount = TestUtil.getAccount("user1");
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);

        // Clean up tags, in case the last run didn't exit cleanly
        cleanUp();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        // Delete tags
        List<Tag> tagList = mMbox.getTagList(null);
        if (tagList != null) {
            Iterator<Tag> i = tagList.iterator();
            while (i.hasNext()) {
                Tag tag = i.next();
                if (tag.getName().startsWith(TAG_PREFIX)) {
                    mMbox.delete(null, tag.getId(), tag.getType());
                }
            }
        }

        // Move items from temp folder back to inbox
        Folder folder = TestUtil.getFolderByPath(mMbox, FOLDER_NAME);
        if (folder != null) {
            List<MailItem> ids = mMbox.getItemList(null, MailItem.Type.MESSAGE, folder.getId());
            Iterator<MailItem> i = ids.iterator();
            while (i.hasNext()) {
                Message message = (Message) i.next();
                mMbox.move(null, message.getId(), MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);
            }
            mMbox.delete(null, folder.getId(), folder.getType());
        }
    }


    @Test
    public void testRead()
    throws Exception {
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new ReadMessagesThread(5), "ReadMessagesThread-" + i);
        }

        runThreads(threads);
    }

    @Test
    public void testTag()
    throws Exception {
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            Tag tag = mMbox.createTag(null, TAG_PREFIX + i + 1, (byte) 0);
            threads[i] = new Thread(new TagMessagesThread(tag, 5), "TagMessagesThread-" + i);
        }

        runThreads(threads);
    }

    @Test
    public void testReadAndTag()
    throws Exception {
        int numThreads = 6;
        Thread[] threads = new Thread[numThreads];
        int tagNum = 1;

        for (int i = 0; i < numThreads; i += 2) {
            Tag tag = mMbox.createTag(null, TAG_PREFIX + tagNum, (byte) 0);
            threads[i] = new Thread(new ReadMessagesThread(5), "ReadMessagesThread-" + i);
            threads[i + 1] = new Thread(new TagMessagesThread(tag, 5), "TagMessagesThread-" + i);
            tagNum++;
        }

        runThreads(threads);
    }

    @Test
    public void testReadAndMove() throws Exception {
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];

        // Create thread for moving messages
        Folder folder = mMbox.createFolder(null, FOLDER_NAME, Mailbox.ID_FOLDER_USER_ROOT, new Folder.FolderOptions());
        threads[0] = new Thread(new MoveMessagesThread(folder, 5));

        // Create threads for reading messages
        for (int i = 1; i < numThreads; i++) {
            threads[i] = new Thread(new ReadMessagesThread(5));
        }

        runThreads(threads);

    }

    private void runThreads(Thread[] threads)
    throws Exception {
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        while (true) {
            boolean allDone = true;
            for (int i = 0; i < threads.length; i++) {
                if (threads[i].isAlive()) {
                    allDone = false;
                    break;
                }
            }

            if (allDone) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private class ReadMessagesThread implements Runnable {

        int mNumToRead;

        ReadMessagesThread(int numToRead) {
            mNumToRead = numToRead;
        }

        @Override
        public void run() {
            ZimbraLog.test.debug("Starting ReadMessagesThread");
            int numRead = 0;

            try {
                LmcSession session = TestUtil.getSoapSession("user1");
                // Search for messages containing "roland", so we don't run into problems
                // with Asian character sets
                List<Integer> ids = TestUtil.search(mMbox, "in:inbox roland", MailItem.Type.MESSAGE);
                assertTrue("Search returned " + ids.size() + " messages.  Expected at least " + mNumToRead,
                    ids.size() >= mNumToRead);

                for (Integer id : ids) {
                    LmcGetMsgRequest req = new LmcGetMsgRequest();
                    req.setMsgToGet(id.toString());
                    req.setSession(session);
                    req.invoke(TestUtil.getSoapUrl());
                    numRead++;
                    if (numRead == mNumToRead) {
                        break;
                    }
                }
            } catch (Exception e) {
                ZimbraLog.test.error("Error in ReadMessagesThread", e);
            }

            ZimbraLog.test.debug("ReadMessagesThread read " + numRead + " messages");
        }
    }

    private class TagMessagesThread implements Runnable {

        Tag mTag;
        int mNumToTag;

        TagMessagesThread(Tag tag, int numToTag) {
            mTag = tag;
            mNumToTag = numToTag;
        }

        @Override
        public void run() {
            ZimbraLog.test.debug("Starting TagMessagesThread");
            int numTagged = 0;

            try {
                LmcSession session = TestUtil.getSoapSession("user1");
                // Search for messages containing "roland", so we don't run into problems
                // with Asian character sets
                List<Integer> ids = TestUtil.search(mMbox, "in:inbox", MailItem.Type.MESSAGE);

                for (Integer id : ids) {
                    LmcMsgActionRequest req = new LmcMsgActionRequest();
                    req.setOp("tag");
                    req.setMsgList(id.toString());
                    req.setTag(Integer.toString(mTag.getId()));
                    req.setSession(session);
                    req.invoke(TestUtil.getSoapUrl());
                    numTagged++;
                    if (numTagged == mNumToTag) {
                        break;
                    }
                }
            } catch (Exception e) {
                ZimbraLog.test.error("Error in TagMessagesThread", e);
            }

            ZimbraLog.test.debug("TagMessagesThread tagged " + numTagged + " messages");
        }
    }

    private class MoveMessagesThread implements Runnable {

        Folder mFolder;
        int mNumToMove;

        MoveMessagesThread(Folder folder, int numToMove) {
            mFolder = folder;
            mNumToMove = numToMove;
        }

        @Override
        public void run() {
            ZimbraLog.test.debug("Starting MoveMessagesThread");
            int numMoved = 0;

            try {
                LmcSession session = TestUtil.getSoapSession("user1");
                List<Integer> ids = TestUtil.search(mMbox, "in:inbox", MailItem.Type.MESSAGE);

                for (Integer id : ids) {
                    LmcMsgActionRequest req = new LmcMsgActionRequest();
                    req.setOp("move");
                    req.setMsgList(id.toString());
                    req.setFolder(Integer.toString(mFolder.getId()));
                    req.setSession(session);
                    req.invoke(TestUtil.getSoapUrl());
                    numMoved++;
                    if (numMoved == mNumToMove) {
                        break;
                    }
                }
            } catch (Exception e) {
                ZimbraLog.test.error("Error in MoveMessagesThread", e);
            }

            ZimbraLog.test.debug("MoveMessagesThread moved " + numMoved + " messages");
        }
    }
}
