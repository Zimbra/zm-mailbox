package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;

import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;

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
}
