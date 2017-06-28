package com.zimbra.cs.redolog.op;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.redolog.TransactionId;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashSet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CheckpointTest {
    private Checkpoint op;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void testDefaultConstructor() {
        op = new Checkpoint();
        Assert.assertEquals(0, op.getNumActiveTxns());
        Assert.assertNull(op.getTransactionId());
    }

    @Test
    public void testSetConstructor() {
        LinkedHashSet<TransactionId> txns = new LinkedHashSet<TransactionId>();
        txns.add(new TransactionId(1, 2));
        txns.add(new TransactionId(3, 4));

        op = new Checkpoint(txns);
        Assert.assertEquals(new TransactionId(), op.getTransactionId());
        Assert.assertEquals("expected 2 active transactions.",
                            2, op.getNumActiveTxns());
        Assert.assertEquals("Transactions don't match.",
                            txns, op.getActiveTxns());
    }

    @Test
    public void serializeDeserialize() throws Exception {
        LinkedHashSet<TransactionId> txns = new LinkedHashSet<TransactionId>();
        txns.add(new TransactionId(1, 2));
        txns.add(new TransactionId(3, 4));

        op = new Checkpoint(txns);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        op.serializeData(new RedoLogOutput(out));

        // reset op
        op = new Checkpoint();
        op.deserializeData(
            new RedoLogInput(new ByteArrayInputStream(out.toByteArray())));
        Assert.assertEquals("Transactions don't match after deserialize.",
                            txns, op.getActiveTxns());
    }
}
