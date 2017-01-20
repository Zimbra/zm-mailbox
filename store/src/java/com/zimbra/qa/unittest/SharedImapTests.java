package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.MessagingException;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.ResponseHandler;

/**
 * Definitions of tests used from {@Link TestLocalImapShared} and {@Link TestRemoteImapShared}
 */
public abstract class SharedImapTests {
    private static final String USER = "SharedImapTests-user";
    private static final String PASS = "test123";
    private Account acc = null;
    private Server imapServer = null;
    private ImapConnection connection;
    private static final SoapProvisioning sp = new SoapProvisioning();
    private static boolean mIMAPDisplayMailFoldersOnly;


    public void sharedSetUp() throws ServiceException, IOException  {
        imapServer = Provisioning.getInstance().getLocalServer();
        mIMAPDisplayMailFoldersOnly = imapServer.isImapDisplayMailFoldersOnly();
        imapServer.setImapDisplayMailFoldersOnly(false);
        sharedCleanup();
        acc = TestUtil.createAccount(USER);
        Provisioning.getInstance().setPassword(acc, PASS);
    }

    private void sharedCleanup() throws ServiceException {
        if(TestUtil.accountExists(USER)) {
            TestUtil.deleteAccount(USER);
        }
    }

    public void sharedTearDown() throws ServiceException  {
        sharedCleanup();
        if (imapServer != null) {
            imapServer.setImapDisplayMailFoldersOnly(mIMAPDisplayMailFoldersOnly);
        }
    }

    private ImapConnection connect(Server server) throws IOException {
        ImapConfig config = new ImapConfig(server.getServiceHostname());
        config.setPort(server.getImapBindPort());
        config.setAuthenticationId(USER);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection conn = new ImapConnection(config);
        conn.connect();
        return conn;
    }

    @Test
    public void testListFolderContents() throws IOException, ServiceException, MessagingException {
        String folderName = "SharedImapTests-testOpenFolder";
        String subject = "SharedImapTests-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        MessageData md = mdMap.values().iterator().next();
        assertNotNull("MessageData", md);
        Envelope env = md.getEnvelope();
        assertNotNull("Envelope", env);
        assertEquals("Subject from envelope", subject, env.getSubject());
        assertNotNull("Internal date", md.getInternalDate());
        BodyStructure bs = md.getBodyStructure();
        assertNotNull("Body Structure", bs);
        if (bs.isMultipart()) {
            BodyStructure[] parts = bs.getParts();
            for (BodyStructure part : parts) {
                assertNotNull(part.getType());
                assertNotNull(part.getSubtype());
            }
        } else {
            assertNotNull("Body structure type", bs.getType());
            assertNotNull("Body structure sub-type", bs.getSubtype());
        }
        Body[] body = md.getBodySections();
        assertNotNull("body", body);
        assertEquals(1, body.length);
    }

    @Test
    public void testIdleNotification() throws IOException, ServiceException, MessagingException {
        final ImapConnection connection1 = connect(imapServer);
        connection1.login(PASS);
        connection1.select("INBOX");
        ImapConnection connection2 = connect(imapServer);
        connection2.login(PASS);
        try {
            Flags flags = Flags.fromSpec("afs");
            Date date = new Date(System.currentTimeMillis());
            String subject = "SharedImapTest-testIdleNotification";
            ZMailbox zmbox = TestUtil.getZMailbox(USER);
            TestUtil.addMessage(zmbox, subject, "1", null);
            Literal msg = TestImap.message(1000);
            final AtomicBoolean gotExists = new AtomicBoolean(false);
            final AtomicBoolean gotRecent = new AtomicBoolean(false);
            final CountDownLatch doneSignal = new CountDownLatch(1);

            // Kick off an IDLE command - which will be processed in another thread until we call stopIdle()
            connection1.idle(new ResponseHandler() {
                @Override
                public void handleResponse(ImapResponse res) {
                    if ("* 1 EXISTS".equals(res.toString())) {
                        gotExists.set(true);
                    }
                    if ("* 1 RECENT".equals(res.toString())) {
                        gotRecent.set(true);
                    }
                    if (gotExists.get() && gotRecent.get()) {
                        doneSignal.countDown();
                    }
                }
            });
            assertTrue("Connection is not idling when it should be", connection1.isIdling());

            try {
                AppendResult res = connection2.append("INBOX", flags, date, msg);
                assertNotNull(res);
            } finally {
                msg.dispose();
            }

            try {
                doneSignal.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                Assert.fail("Wait interrupted. ");
            }
            assertTrue("Connection is not idling when it should be", connection1.isIdling());
            connection1.stopIdle();
            assertTrue("Connection is idling when it should NOT be", !connection1.isIdling());
            MailboxInfo mboxInfo = connection1.getMailboxInfo();
            assertEquals("Connection was not notified of correct number of existing items", 1, mboxInfo.getExists());
            assertEquals("Connection was not notified of correct number of recent items", 1, mboxInfo.getRecent());
        } finally {
            connection1.close();
            connection2.close();
        }
    }
}
