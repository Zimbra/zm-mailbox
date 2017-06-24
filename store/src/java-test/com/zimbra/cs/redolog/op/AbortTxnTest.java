package com.zimbra.cs.redolog.op;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.MailboxTestUtil;
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

public class AbortTxnTest {
    private AbortTxn op;
    private RedoableOp changeEntry;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() {
        changeEntry = EasyMock.createMockBuilder(CopyItem.class)
                          .withConstructor()
                          .addMockedMethod("getTransactionId")
                          .addMockedMethod("getMailboxId")
                          .createMock();
        EasyMock.expect(changeEntry.getTransactionId())
            .andStubReturn(new TransactionId(1, 2));
        EasyMock.expect(changeEntry.getMailboxId()).andStubReturn(5);

        EasyMock.replay(changeEntry);
    }

    @Test
    public void testDefaultConstructor() {
        op = new AbortTxn();
        Assert.assertNull(op.getTransactionId());
    }

    @Test
    public void testOpConstructor() {
        op = new AbortTxn(changeEntry);
        Assert.assertEquals(5, op.getMailboxId());
        Assert.assertEquals(new TransactionId(1, 2), op.getTransactionId());
        Assert.assertEquals(MailboxOperation.CopyItem, op.getTxnOpCode());
    }

    @Test
    public void serializeDeserialize() throws Exception {
        op = new AbortTxn(changeEntry);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        op.serializeData(new RedoLogOutput(out));

        // reset op
        op = new AbortTxn();
        op.deserializeData(
            new RedoLogInput(new ByteArrayInputStream(out.toByteArray())));
        Assert.assertEquals(MailboxOperation.CopyItem, op.getTxnOpCode());
    }
}
