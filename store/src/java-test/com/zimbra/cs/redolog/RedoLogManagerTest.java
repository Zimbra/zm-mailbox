package com.zimbra.cs.redolog;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.logger.FileLogWriter;
import com.zimbra.cs.redolog.op.RedoableOp;
import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class RedoLogManagerTest {
    private RedoLogManager redoLogManager;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        redoLogManager = RedoLogProvider.getInstance().getRedoLogManager();
        redoLogManager.start();
    }

    @After
    public void tearDown() throws Exception {
        redoLogManager.stop();
    }

    @Test
    public void internalState() throws Exception {
        Assert.assertEquals("path != build/test/redo/redo.log",
                            "build/test/redo/redo.log",
                            redoLogManager.getLogFile().getPath());
        Assert.assertEquals("archiveDir != build/test/redo", "build/test/redo",
                            redoLogManager.getArchiveDir().getPath());
        Assert.assertEquals("rolloverDestDir != build/test/redo",
                            "build/test/redo",
                            redoLogManager.getRolloverDestDir().getPath());
        Assert.assertFalse("crashRecovery == true",
                           redoLogManager.getInCrashRecovery());
    }

    @Test
    public void transactionIdIncrements() throws Exception {
        TransactionId id = redoLogManager.getNewTxnId();

        // Get the next transaction ID
        TransactionId nextId = redoLogManager.getNewTxnId();
        Assert.assertEquals(
            "nextId.getCounter() should be one more than id.getCounter()",
            id.getCounter() + 1, nextId.getCounter());
    }

    @Test
    public void rolloverIncrementsLogSequence() throws Exception {
        if (!(redoLogManager.getLogWriter() instanceof FileLogWriter)) {
            /*
            * Rollover is just needed when RedologManager use a FileLogWriter instance as mechanism to Write the redo log,
            * so if RedologManager use a different LogWriter (for example: DbLogWriter) we can skip this test.
            */
            return;
        }

        long currentSequence = redoLogManager.getCurrentLogSequence();
        File previousFile = redoLogManager.forceRollover();
        // No change, since no ops have been logged
        Assert.assertEquals(
            "Log sequence should not change. Nothing logged before rollover.",
            currentSequence, redoLogManager.getCurrentLogSequence());
        Assert.assertNull(
            "No rollover occured, so previous log file should be NULL.",
            previousFile);

        RedoableOp op = EasyMock.createMockBuilder(RedoableOp.class)
                            .withConstructor(MailboxOperation.Preview)
                            .createMock();

        // Run the operation and log.
        op.start(7 /* timestamp */);
        op.log();
        Assert.assertEquals("sequence number should be the same as before.",
                            currentSequence,
                            redoLogManager.getCurrentLogSequence());
        previousFile = redoLogManager.forceRollover();

        Assert.assertEquals(
            "Forced rollover after a log should increment sequence number",
            currentSequence + 1, redoLogManager.getCurrentLogSequence());
        Assert.assertTrue(
            previousFile.getName().contains("seq" + currentSequence + ".log"));
    }
}
