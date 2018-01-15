package com.zimbra.cs.redolog.logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.zimbra.cs.db.DbDistibutedRedolog;
import com.zimbra.cs.db.DbDistibutedRedolog.OpType;
import com.zimbra.cs.redolog.logger.DbLogWriter.LogHeader;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.op.RedoableOp;
import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public class DbLogWriterTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private RedoLogManager mockRedoLogManager;
    private DbLogWriter logWriter;
    private LogHeader hdr;
    private DbPool.DbConnection conn;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        mockRedoLogManager = EasyMock.createNiceMock(RedoLogManager.class);
        logWriter = new DbLogWriter(mockRedoLogManager);
        conn = DbPool.getConnection();
    }

    @Test
    public void openCloseLog() throws Exception {
        logWriter.open();
        Assert.assertTrue("Connection is open successfully", logWriter.isOpen());
        Assert.assertTrue("Table has to have header in place when open the redolog first time",
                (logWriter.getSize() == LogHeader.HEADER_LEN));

        RedoableOp op = EasyMock.createMockBuilder(RedoableOp.class)
                .withConstructor(MailboxOperation.Preview)
                .createMock();
        op.start(System.currentTimeMillis());


        logWriter.log(op, new ByteArrayInputStream("some bytes".getBytes()), false);
        Assert.assertEquals("file size incorrect.", LogHeader.HEADER_LEN + 10, logWriter.getSize());

        logWriter.close();
        Assert.assertTrue("Connection was closed successfully", !logWriter.isOpen());

        // store some fields from the current writer.
        final long createTime = logWriter.getCreateTime();
        long lastLogOp = logWriter.getLastLogTime();
        Assert.assertEquals(createTime, logWriter.getCreateTime());

        // restarting LogWriter
        logWriter = new DbLogWriter(mockRedoLogManager);
        logWriter.open();
        Assert.assertEquals("file size incorrect.", LogHeader.HEADER_LEN + 10, logWriter.getSize());

        // All data is retrieved correctly
        Assert.assertEquals(createTime, logWriter.getCreateTime());
        Assert.assertEquals(lastLogOp, logWriter.getLastLogTime());

        op = EasyMock.createMockBuilder(RedoableOp.class)
                .withConstructor(MailboxOperation.Preview)
                .createMock();
        op.start(System.currentTimeMillis());

        logWriter.log(op, new ByteArrayInputStream("some bytes".getBytes()), false);
        Assert.assertEquals("file size incorrect.", LogHeader.HEADER_LEN + 20, logWriter.getSize());
        logWriter.close();

        // Create time is fixed along redolog life but lastLogTime has to be greater than last op timestamp
        Assert.assertEquals(createTime, logWriter.getCreateTime());
        Assert.assertTrue(lastLogOp < logWriter.getLastLogTime());
    }

    @Test(expected = Exception.class)
    public void logBeforeOpen() throws Exception {
        logWriter.log(null, null, false);
    }

/*
========================================================================================================================
============================================   TEST THE HEADER   =======================================================
========================================================================================================================
*/
    @Test
    public void initializingHeader() throws Exception {
        LogHeader anExistingHdr;
        hdr = new LogHeader();

        Assert.assertFalse("file is open", hdr.getOpen());
        Assert.assertEquals("file size is not 0", 0, hdr.getFileSize());
        Assert.assertEquals("header sequence is not 0", 0, hdr.getSequence());
        Assert.assertEquals("server id is set", "unknown", hdr.getServerId());
        Assert.assertEquals("unexpected first op time", 0, hdr.getFirstOpTstamp());
        Assert.assertEquals("unexpected last op time", 0, hdr.getLastOpTstamp());
        Assert.assertEquals("unexpected create time", 0, hdr.getCreateTime());

        hdr.init(conn);

        // reading an existing header
        anExistingHdr = new LogHeader("should be overwritten");
        anExistingHdr.read(conn);
        Assert.assertEquals("header from file should match serialized data", hdr, anExistingHdr);

        // init an existing header
        anExistingHdr = new LogHeader("should be overwritten");
        anExistingHdr.init(conn);
        Assert.assertEquals("header from file should match serialized data", hdr, anExistingHdr);
    }

    @Test
    public void setAllFields() throws Exception {
        hdr = new LogHeader("serverId");
        hdr.setOpen(true);
        hdr.setFileSize(1);
        hdr.setSequence(2);
        hdr.setFirstOpTstamp(3);
        hdr.setLastOpTstamp(4);
        hdr.setCreateTime(5);

        Assert.assertTrue("open != true", hdr.getOpen());
        Assert.assertEquals(1, hdr.getFileSize());
        Assert.assertEquals(2, hdr.getSequence());
        Assert.assertEquals("serverId", hdr.getServerId());
        Assert.assertEquals(3, hdr.getFirstOpTstamp());
        Assert.assertEquals(4, hdr.getLastOpTstamp());
        Assert.assertEquals(5, hdr.getCreateTime());

        hdr.init(conn);
        LogHeader fromDB = new LogHeader("should be overwritten");
        fromDB.init(conn);
        Assert.assertEquals("header from file should match serialized data", hdr, fromDB);
    }

    @Test
    public void junkEntry() throws Exception {
        try {
            DbDistibutedRedolog.deleteHeaderOp(conn);
            DbDistibutedRedolog.logOp(conn, OpType.HEADER, new ByteArrayInputStream("this is not a valid header".getBytes()));
            conn.commit();
            hdr = new LogHeader();
            hdr.read(conn);

            fail("Exception expected here");
        } catch (Exception e) {
            assertEquals("Redolog is smaller than header length of " + LogHeader.HEADER_LEN + " bytes", e.getMessage());
        }
    }

    @Test
    public void versionTooHigh() throws Exception {
        hdr = new LogHeader();
        hdr.write(conn);

        // Fake up a bad version
        final int versionLocation =
                7 /* magic */ + 1 /* open */ + 8 /* file size */ +
                        8 /* sequence */ + 1 /* serverid length */ + 127 /* serverid */ +
                        8 /* firstOpTstamp */ + 8 /* lastOpTstamp */;


        InputStream headerData = DbDistibutedRedolog.getHeaderOp(conn);
        byte header[] = new byte[LogHeader.HEADER_LEN];
        headerData.read(header, 0, LogHeader.HEADER_LEN);
        header[versionLocation] = (byte) ((short)header[versionLocation] + 1);
        DbDistibutedRedolog.clearRedolog(conn);
        DbDistibutedRedolog.logOp(conn, OpType.HEADER, new ByteArrayInputStream(header));
        conn.commit();
        try {
            hdr.read(conn);
        } catch (IOException e) {
            Assert.assertTrue("Version in file should be too high.", e.getMessage().contains("is higher than the highest known version"));
            return;
        }
        Assert.fail("no exception thrown");
    }

    @After
    public void tearDown() throws Exception {
        conn.closeQuietly();
    }
}