package com.zimbra.cs.redolog.logger;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.CopyItem;
import com.zimbra.cs.redolog.op.RedoableOp;
import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class FileLogReaderTest {
    private FileLogReader logReader;
    private FileLogWriter logWriter;
    private File logfile;

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        logfile = folder.newFile("logfile");
        RedoLogManager mockRedoLogManager =
            EasyMock.createNiceMock(RedoLogManager.class);
        logReader = new FileLogReader(logfile);
        logWriter = new FileLogWriter(mockRedoLogManager, logfile,
                                      0 /* no fsync thread */);
    }

    private void writeOp(TransactionId id) throws IOException {
        logWriter.open();
        RedoableOp op = EasyMock.createMockBuilder(CopyItem.class)
                            .withConstructor()
                            .addMockedMethod("getTransactionId")
                            .createMock();
        EasyMock.expect(op.getTransactionId()).andStubReturn(id);

        EasyMock.replay(op);
        logWriter.log(op, op.getInputStream(), true /* synchronous */);
        logWriter.close();
    }

    @Test
    public void openReadClose() throws Exception {
        writeOp(new TransactionId(7, 3));

        logReader.open();
        Assert.assertEquals("Read file to unexpected position",
                            FileHeader.HEADER_LEN, logReader.position());
        RedoableOp op = logReader.getNextOp();
        Assert.assertEquals(FileHeader.HEADER_LEN,
                            logReader.getLastOpStartOffset());
        Assert.assertEquals("mismateched transactionid",
                            op.getTransactionId(), new TransactionId(7, 3));
        Assert.assertNull("More ops in file.", logReader.getNextOp());
        logReader.close();
    }

    @Test
    public void skipsJunkInFile() throws Exception {
        // open and close with logwriter to get a header.
        logWriter.open();
        logWriter.close();

        // seek to end of file and write junk
        RandomAccessFile raf = new RandomAccessFile(logfile, "rw");
        raf.seek(raf.length());
        raf.writeChars("This is junk in the file");
        raf.close();

        // Write using logWriter
        writeOp(new TransactionId(7, 3));

        // seek to end of file and write junk
        raf = new RandomAccessFile(logfile, "rw");
        raf.seek(raf.length());
        raf.writeChars("This is other junk in the file");
        raf.close();
        // Write using logWriter
        writeOp(new TransactionId(8, 4));

        logReader.open();
        Assert.assertEquals(FileHeader.HEADER_LEN, logReader.position());
        RedoableOp op = logReader.getNextOp();
        Assert.assertEquals("Should skip 48 bytes of junk",
                            FileHeader.HEADER_LEN + 48,
                            logReader.getLastOpStartOffset());
        Assert.assertEquals("TransactionId mismatch",
                            op.getTransactionId(), new TransactionId(7, 3));

        op = logReader.getNextOp();
        Assert.assertEquals("TransactionId mismatch",
                            op.getTransactionId(), new TransactionId(8, 4));

        Assert.assertNull("More ops in file.", logReader.getNextOp());
    }

    @Test(expected = IOException.class)
    public void readBeforeOpen() throws Exception {
        logReader.getNextOp();
    }

    @Test
    public void junkFileFails() throws Exception {
        RandomAccessFile raf = new RandomAccessFile(logfile, "rw");
        // need at least HEADER_LEN worth of junk
        byte[] array = new byte[FileHeader.HEADER_LEN];
        Arrays.fill(array, (byte)'a');
        raf.writeBytes("This is junk in the file");
        raf.write(array);
        raf.close();
        try {
            logReader.open();
        } catch (IOException e) {
            Assert.assertTrue(
                "Cause should contain 'missing magic bytes' "
                    + "Got: " + e.getCause().getMessage(),
                e.getCause().getMessage().contains("Missing magic bytes"));
            return;
        }
        Assert.fail("No exception thrown.");
    }
}
