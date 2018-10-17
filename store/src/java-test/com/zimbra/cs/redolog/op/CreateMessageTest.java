package com.zimbra.cs.redolog.op;

import com.google.common.base.Charsets;
import org.junit.Ignore;
import com.google.common.io.CharStreams;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessageDataSource;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class CreateMessageTest {
    private CreateMessage op;
    private Mailbox mbox;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        Account account = Provisioning.getInstance().createAccount(
            "test@zimbra.com", "secret", new HashMap<String, Object>());
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        ParsedMessage pm = MailboxTestUtil.generateMessage("test");
        final long msgSize = pm.getRawInputStream().available();

        op = new CreateMessage(mbox.getId() /* mailboxId */, "rcpt@example.com",
                               false /* shared */, "message digest",
                               msgSize /* msgSize */, 6 /* folderId */,
                               true /* noICal */, 0 /* flags */,
                               new String[] {"tag"});
        op.setMessageBodyInfo(new ParsedMessageDataSource(pm), msgSize);
        op.setMessageId(-1);
        op.setConvId(-1);
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void startSetsTimestamp() {
        Assert.assertEquals("receivedDate != -1 before start",
                            -1, op.mReceivedDate);
        op.start(7);
        Assert.assertEquals("receivedDate != 7", 7, op.mReceivedDate);
        Assert.assertEquals("timestamp != 7", 7, op.getTimestamp());
    }

    @Test
    public void serializeDeserialize() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        op.serializeData(new RedoLogOutput(out));

        // reset op
        op = new CreateMessage();
        op.deserializeData(
            new RedoLogInput(new ByteArrayInputStream(out.toByteArray())));
        Assert.assertEquals("rcpt@example.com", op.getRcptEmail());
        Assert.assertEquals(6, op.getFolderId());
        Assert.assertEquals(0, op.getFlags());
        Assert.assertEquals(new String[] {"tag"}, op.getTags());
        Assert.assertEquals(":streamed:", op.getPath());
        Assert.assertEquals("Input stream is not empty", "",
                            CharStreams.toString(new InputStreamReader(
                                op.getAdditionalDataStream(), Charsets.UTF_8)));
    }

    @Test
    public void redo() throws Exception {
        op.redo();

        // Look in the mailbox and see if the message is there.
        Message msg =
            mbox.getMessageById(op.getOperationContext(), mbox.getLastItemId());
        Assert.assertEquals("subject != test", "test", msg.getSubject());
        Assert.assertEquals("sender != bob@example.com",
                            "Bob Evans <bob@example.com>", msg.getSender());
        Assert.assertEquals("folderId != 6", 6, msg.getFolderId());
    }
}
