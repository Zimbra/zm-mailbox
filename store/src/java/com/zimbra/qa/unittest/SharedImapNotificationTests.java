package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.SocketException;
import java.util.Map;

import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.Atom;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;

public abstract class SharedImapNotificationTests extends ImapTestBase {

    protected abstract void runOp(MailboxOperation op, ZMailbox mbox, ZFolder folder) throws Exception;

    @Test
    public void testNotificationsActiveFolder() throws Exception {
        String folderName = "testNotificationsActiveFolder-folder";
        String subject1 = "testNotificationsActiveFolder-msg1";
        String subject2 = "testNotificationsActiveFolder-msg2";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject1, folder.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());

        MailboxOperation addMessage = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

            }

            @Override
            protected String checkResult() throws Exception {
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
                if(mdMap.size() != 2) {
                    return String.format("Size of map returned by fetch should be 2. Getting %d", mdMap.size());
                }
                return null;
            }
        };

        runOp(addMessage, zmbox, folder);
    }

    @Test
    public void testNotificationsEmptyActiveFolder() throws Exception {
        String folderName = "testNotificationsEmptyActiveFolder-folder";
        String subject1 = "testNotificationsEmptyActiveFolder-msg";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName);

        MailboxOperation addMessage = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                TestUtil.addMessage(zmbox, subject1, folder.getId(), null);

            }

            @Override
            protected String checkResult() throws Exception {
                try {
                    Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
                    if(mdMap.size() != 1) {
                        return String.format("Size of map returned by fetch should be 1. Getting %d", mdMap.size());
                    }
                } catch (CommandFailedException cfe) {
                    return cfe.getError();
                }
                return null;
            }
        };

        runOp(addMessage, zmbox, folder);
    }

    @Test
    public void testNotificationsCachedFolder() throws Exception {
        String folderName1 = "testNotificationsCachedFolder-folder1";
        String folderName2 = "testNotificationsCachedFolder-folder2";

        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder1 = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        TestUtil.addMessage(zmbox, subject1, folder1.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        connection.select(folderName2);

        MailboxOperation addMessage = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                TestUtil.addMessage(zmbox, subject2, folder1.getId(), null);

            }

            @Override
            protected String checkResult() throws Exception {
                connection.select(folderName1);
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
                if(mdMap.size() != 2) {
                    return String.format("Size of map returned by fetch afer reselecting cached folder should be 2. Getting %d", mdMap.size());
                }
                return null;
            }
        };

        runOp(addMessage, zmbox, folder1);
    }

    @Test
    public void testDeleteMessageNotificationActiveFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        String msgId = TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 2, mdMap.size());

        MailboxOperation deleteMessage = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.deleteMessage(msgId);
            }

            @Override
            protected String checkResult() throws Exception {
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
                assertEquals("Size of map returned by fetch 2", 2, mdMap.size());
                MessageData md = mdMap.get(1L);
                //verify that the deleted message has a NIL response
                Envelope envelope = md.getEnvelope();
                if(envelope == null) {
                    return "Envelope should not be NULL";
                }
                if(envelope.getSubject() != null) {
                    return "Envelope::subject should be NULL";
                }
                BodyStructure bs = md.getBodyStructure();
                if(bs.getSize() != 0) {
                    return "BodyStructure::geSize should return 0";
                }
                //verify that the second message is correct
                md = mdMap.get(2L);
                if(!subject2.equals(md.getEnvelope().getSubject())) {
                    return String.format("Subject should be %s. Getting %d", subject2, md.getEnvelope().getSubject());
                }
                connection.expunge();
                mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
                if(mdMap.size() != 1) {
                    return String.format("Size of map returned by fetch should be 1. Getting %d", mdMap.size());
                }
                return null;
            }
        };

        runOp(deleteMessage, zmbox, folder);
    }

    @Test
    public void testDeleteMessageNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        String msgId = TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 2, mdMap.size());

        connection.select(folderName2);

        MailboxOperation deleteMessage = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.deleteMessage(msgId);
            }

            @Override
            protected String checkResult() throws Exception {
                connection.select(folderName1);
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
                //expunged messages are not returned as NIL responses if folder is re-selected
                if(mdMap.size() != 1) {
                    return String.format("Size of map returned by fetch should be 1. Getting %d", mdMap.size());
                }
                return null;
            }
        };

        runOp(deleteMessage, zmbox, folder);
    }

    @Test
    public void testDeleteTagNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String tagName = "TestRemoteImapNotifications-tag";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect();
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);

        Flags flags = info.getPermanentFlags();
        assertTrue("folder info should list tag", flags.contains(new Atom(tagName)));
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        Flags msgFlags = mdMap.get(1L).getFlags();
        assertTrue("message should be flagged with tag", msgFlags.contains(new Atom(tagName)));

        MailboxOperation deleteTag = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.deleteTag(tag.getId());
            }

            @Override
            protected String checkResult() throws Exception {
                MailboxInfo info = connection.select(folderName);
                Flags flags = info.getPermanentFlags();
                if(flags.contains(new Atom(tagName))) {
                    return String.format("folder info should not list tag %s", tagName);
                }
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
                Flags msgFlags = mdMap.get(1L).getFlags();
                if(msgFlags.contains(new Atom(tagName))) {
                    return String.format("message should not be flagged with %s", tagName);
                }
                return null;
            }
        };

        runOp(deleteTag, zmbox, folder);
    }

    @Test
    public void testDeleteTagNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 ="TestRemoteImapNotifications-folder2";
        String tagName = "TestRemoteImapNotifications-tag";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect();
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName1);

        Flags flags = info.getPermanentFlags();
        assertTrue("folder info should list tag", flags.contains(new Atom(tagName)));
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        Flags msgFlags = mdMap.get(1L).getFlags();
        assertTrue("message should be flagged with tag", msgFlags.contains(new Atom(tagName)));

        connection.select(folderName2);

        MailboxOperation deleteTag = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.deleteTag(tag.getId());
            }

            @Override
            protected String checkResult() throws Exception {
                MailboxInfo info = connection.select(folderName1);
                Flags flags = info.getPermanentFlags();
                if(flags.contains(new Atom(tagName))) {
                    return String.format("folder info should not list tag %s", tagName);
                }
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
                Flags msgFlags = mdMap.get(1L).getFlags();
                if(msgFlags.contains(new Atom(tagName))) {
                    return String.format("message should not be flagged with %s", tagName);
                }
                return null;
            }
        };

        runOp(deleteTag, zmbox, folder);
    }

    @Test
    public void testDeleteFolderNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        MailboxOperation deleteFolder = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.deleteFolder(folder.getId());
            }

            @Override
            protected String checkResult() throws Exception {
                try {
                    connection.fetch("1:*", "(ENVELOPE BODY)");
                    return "should not be able to connect; connection should be closed";
                } catch (SocketException | CommandFailedException e) {} // connection is force closed
                return null;
            }
        };

        runOp(deleteFolder, zmbox, folder);
    }

    @Test
    public void testDeleteFolderNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        connection.select(folderName2);

        MailboxOperation deleteFolder = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.deleteFolder(folder.getId());
            }

            @Override
            protected String checkResult() throws Exception {
                try {
                    connection.list("", "*");
                } catch (CommandFailedException e) {
                    return "should be able to connect after deleting a cached folder";
                }
                return null;
            }
        };

        runOp(deleteFolder, zmbox, folder);
    }

    @Test
    public void testRenameTagNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String tagName = "TestRemoteImapNotifications-tag";
        String newTagName = "TestRemoteImapNotifications-tag2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect();
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        MailboxOperation renameTag = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.renameTag(tag.getId(), newTagName);
            }

            @Override
            protected String checkResult() throws Exception {
                MailboxInfo info = connection.select(folderName);
                Flags flags = info.getPermanentFlags();
                if(flags.contains(new Atom(tagName))) {
                    return String.format("Permanent flags should not contain %s", tagName);
                }
                if(!flags.contains(new Atom(newTagName))) {
                    return String.format("Permanent flags should contain %s", newTagName);
                }
                return null;
            }
        };

        runOp(renameTag, zmbox, folder);
    }

    @Test
    public void testRenameTagNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder1";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String tagName = "TestRemoteImapNotifications-tag";
        String newTagName = "TestRemoteImapNotifications-tag2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect();
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName1);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        info = connection.select(folderName2);

        MailboxOperation renameTag = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.renameTag(tag.getId(), newTagName);
            }

            @Override
            protected String checkResult() throws Exception {
                MailboxInfo info = connection.select(folderName1);
                Flags flags = info.getPermanentFlags();
                if(flags.contains(new Atom(tagName))) {
                    return String.format("Flags should NOT contain %s", tagName);
                }
                if(!flags.contains(new Atom(newTagName))) {
                    return String.format("Flags should contain %s", newTagName);
                }
                return null;
            }
        };

        runOp(renameTag, zmbox, folder);
    }

    @Test
    public void testDeleteMessageBySubjectNotifications() throws Exception {
        String folderName = "testDeleteMessageBySubjectNotifications-folder";
        String subject1 = "testDeleteMessageBySubjectNotifications-msg1";
        String subject2 = "testDeleteMessageBySubjectNotifications-msg2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 1", 2, mdMap.size());

        MailboxOperation deleteBySubject = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                TestUtil.deleteMessages(zmbox, "subject: " + subject2);
            }

            @Override
            protected String checkResult() throws Exception {
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE)");
                mdMap.entrySet().removeIf(e ->  e.getValue().getEnvelope() == null || e.getValue().getEnvelope().getSubject() == null);
                if(mdMap.size() != 1) {
                    return String.format("Size of map returned by fetch should be 1. Getting %d", mdMap.size());
                }
                return null;
            }
        };

        runOp(deleteBySubject, zmbox, folder);
    }


    @Test
    public void testModifyItemNotificationActiveFolder() throws Exception {
        String folderName = "testNotificationsActiveFolder-folder";
        String subject = "testNotificationsActiveFolder-msg1";
        String tagName = "testNotificationsActiveFolder-tag";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        String msgId = TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        connection = connect();
        connection.login(PASS);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());
        //sanity check - make sure the tag is not already set on the message
        Flags flags = mdMap.get(1L).getFlags();
        assertFalse(flags.contains(new Atom(tag.getName())));

        MailboxOperation tagMessage = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.tagMessage(msgId, tag.getId(), true);
            }

            @Override
            protected String checkResult() throws Exception {
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
                if(mdMap.size() != 1) {
                    return String.format("Size of map returned by fetch should be 1. Getting %d", mdMap.size());
                }

                Flags flags = mdMap.get(1L).getFlags();
                if(!flags.contains(new Atom(tag.getName()))) {
                    return String.format("Flags should contain %s", tag.getName());
                }
                return null;
            }
        };

        runOp(tagMessage, zmbox, folder);
    }

    @Test
    public void testModifyItemNotificationCachedFolder() throws Exception {
        String folderName1 = "testNotificationsActiveFolder-folder1";
        String folderName2 = "testNotificationsActiveFolder-folder2";
        String subject = "testNotificationsActiveFolder-msg1";
        String tagName = "testNotificationsActiveFolder-tag";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        String msgId = TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect();
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());
        //sanity check - make sure the tag is not already set on the message
        Flags flags = mdMap.get(1L).getFlags();
        assertFalse(flags.contains(new Atom(tag.getName())));

        connection.select(folderName2);

        MailboxOperation tagMessage = new MailboxOperation() {
            @Override
            protected void run(ZMailbox zmbox) throws Exception {
                zmbox.tagMessage(msgId, tag.getId(), true);
            }

            @Override
            protected String checkResult() throws Exception {
                connection.select(folderName1);
                Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
                if(mdMap.size() != 1) {
                    return String.format("Size of map returned by fetch should be 1. Found: %d", mdMap.size());
                }
                Flags flags = mdMap.get(1L).getFlags();
                if(!flags.contains(new Atom(tag.getName()))) {
                    return String.format("Flags should contain %s", tag.getName());
                }
                return null;
            }
        };

        runOp(tagMessage, zmbox, folder);
    }

    /**
     * This class encapsulates some mailbox operation run from an IMAP server.
     * It is used to test IMAP notifications via response headers and waitsets.
     */
    protected static abstract class MailboxOperation {
        protected abstract void run(ZMailbox zmbox) throws Exception;
        protected abstract String checkResult() throws Exception;
    }

}
