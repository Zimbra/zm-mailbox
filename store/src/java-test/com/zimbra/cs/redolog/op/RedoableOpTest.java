package com.zimbra.cs.redolog.op;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.redolog.TransactionId;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;

public class RedoableOpTest extends EasyMockSupport {
    private RedoLogManager mgr;
    private RedoableOp op;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() {
        mgr = createStrictMock(RedoLogManager.class);
        op = createMockBuilder(RedoableOp.class)
                 .withConstructor(MailboxOperation.CopyItem, mgr)
                 .addMockedMethod("toString")
                 .createMock();
    }

    @Test
    public void constructor() {
        Assert.assertNull(op.getTransactionId());
        Assert.assertEquals(RedoableOp.UNKNOWN_ID, op.getMailboxId());
        Assert.assertTrue(op.isStartMarker());
        Assert.assertFalse(op.isEndMarker());
        Assert.assertFalse(op.isDeleteOp());
    }

    @Test
    public void startLogCommit() throws ServiceException {
        final TransactionId txnId = new TransactionId(1, 2);
        EasyMock.expect(mgr.getNewTxnId()).andReturn(txnId);
        mgr.log(op, false);
        mgr.commit(op);
        replayAll();

        op.start(7);
        Assert.assertEquals(txnId, op.getTransactionId());
        Assert.assertEquals(7, op.getTimestamp());
        op.log(false);
        op.setSerializedByteArray(new byte[3]);
        op.commit();
        Assert.assertNull("Commit clears byte array.",
                          op.mSerializedByteArrayVector);
        verifyAll();
    }

    @Test
    public void inactiveOp() throws ServiceException {
        replayAll();
        op.commit();
        op.abort();
        // no calls to mock expected, op is not active until log()
        verifyAll();
    }

    @Test
    public void serialize() throws Exception {
        op = createMockBuilder(RedoableOp.class)
                 .withConstructor(MailboxOperation.CopyItem, mgr)
                 .addMockedMethod("getTransactionId")
                 .createMock();

        EasyMock.expect(op.getTransactionId())
            .andReturn(new TransactionId(1, 2));
        op.serializeData(EasyMock.anyObject(RedoLogOutput.class));
        replayAll();
        InputStream out = op.getInputStream();
        Assert.assertNotNull("getInputStream sets up internal vector.",
                             op.mSerializedByteArrayVector);
        Assert.assertEquals("available bytes != 46", 46, out.available());
        byte[] bytes = new byte[RedoableOp.REDO_MAGIC.length()];
        out.read(bytes);
        Assert.assertEquals("REDO_MAGIC missing in serialize.",
                            RedoableOp.REDO_MAGIC, new String(bytes));
        verifyAll();
    }

    @Test
    public void chainedCommit() throws ServiceException {
        mgr.log(op, false);
        mgr.commit(op);

        RedoableOp subOp = createMock(RedoableOp.class);
        subOp.commit();
        replayAll();
        op.addChainedOp(subOp);

        op.log(false);
        op.commit();
        verifyAll();
    }

    @Test
    public void chainedAbort() {
        mgr.log(op, false);
        mgr.abort(op);

        RedoableOp subOp = createMock(RedoableOp.class);
        subOp.abort();
        replayAll();
        op.addChainedOp(subOp);

        op.log(false);
        op.abort();
        verifyAll();
    }

    @Test
    public void checkSubclasses() throws Exception {
        Assert.assertTrue("Some RedoableOp subclasses are incomplete.  "
                              + "Hint: Make sure the subclass defines a default"
                              + " constructor.",
                          RedoableOp.checkSubclasses());
        MailboxTestUtil.clearData();
    }
}
