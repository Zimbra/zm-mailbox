package com.zimbra.cs.redolog.op;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.RedoCommitCallback;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.redolog.TransactionId;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CommitTxnTest {
    private CommitTxn op;
    private RedoCommitCallback callback;
    private RedoableOp changeEntry;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() {
        callback = EasyMock.createStrictMock(RedoCommitCallback.class);
        changeEntry = EasyMock.createMockBuilder(CopyItem.class)
                          .withConstructor()
                          .addMockedMethod("getTransactionId")
                          .addMockedMethod("getMailboxId")
                          .addMockedMethod("getCommitCallback")
                          .createMock();
        EasyMock.expect(changeEntry.getTransactionId())
            .andStubReturn(new TransactionId(1, 2));
        EasyMock.expect(changeEntry.getMailboxId()).andStubReturn(5);
        EasyMock.expect(changeEntry.getCommitCallback())
            .andStubReturn(callback);

        EasyMock.replay(changeEntry);
    }

    @Test
    public void testDefaultConstructor() {
        op = new CommitTxn();
        Assert.assertNull(op.getTransactionId());
    }

    @Test
    public void testOpConstructor() {
        op = new CommitTxn(changeEntry);
        Assert.assertEquals(5, op.getMailboxId());
        Assert.assertEquals(new TransactionId(1, 2), op.getTransactionId());
        Assert.assertEquals(MailboxOperation.CopyItem, op.getTxnOpCode());
        Assert.assertEquals(callback, op.getCallback());
    }

    @Test
    public void serializeDeserialize() throws Exception {
        op = new CommitTxn(changeEntry);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        op.serializeData(new RedoLogOutput(out));

        // reset op
        op = new CommitTxn();
        op.deserializeData(
            new RedoLogInput(new ByteArrayInputStream(out.toByteArray())));
        Assert.assertEquals("opcode should be CopyItem after deserialize.",
                            MailboxOperation.CopyItem, op.getTxnOpCode());
    }
}
