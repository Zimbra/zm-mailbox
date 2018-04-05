package com.zimbra.cs.redolog.logger;

import static org.junit.Assert.*;

import com.zimbra.cs.db.DbDistibutedRedolog;
import com.zimbra.cs.db.DbDistibutedRedolog.OpType;
import com.zimbra.cs.redolog.logger.DbLogWriter.LogHeader;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.op.RedoableOp;
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
        assertTrue("Table has to have header in place when open the redolog first time",
                (logWriter.getSize() == LogHeader.HEADER_LEN));

        RedoableOp op = EasyMock.createMockBuilder(RedoableOp.class)
                .withConstructor(MailboxOperation.Preview)
                .createMock();
        op.start(System.currentTimeMillis());


        logWriter.log(op, new ByteArrayInputStream("some bytes".getBytes()), false);
        assertEquals("file size incorrect.", LogHeader.HEADER_LEN + 10, logWriter.getSize());

        logWriter.close();

        // store some fields from the current writer.
        final long createTime = logWriter.getCreateTime();
        long lastLogOp = logWriter.getLastLogTime();
        assertEquals(createTime, logWriter.getCreateTime());

        // restarting LogWriter
        logWriter = new DbLogWriter(mockRedoLogManager);
        logWriter.open();
        assertEquals("file size incorrect.", LogHeader.HEADER_LEN + 10, logWriter.getSize());

        // All data is retrieved correctly
        assertEquals(createTime, logWriter.getCreateTime());
        assertEquals(lastLogOp, logWriter.getLastLogTime());

        op = EasyMock.createMockBuilder(RedoableOp.class)
                .withConstructor(MailboxOperation.Preview)
                .createMock();
        op.start(System.currentTimeMillis());

        logWriter.log(op, new ByteArrayInputStream("some bytes".getBytes()), false);
        assertEquals("file size incorrect.", LogHeader.HEADER_LEN + 20, logWriter.getSize());
        logWriter.close();

        // Create time is fixed along redolog life but lastLogTime has to be greater than last op timestamp
        assertEquals(createTime, logWriter.getCreateTime());
        assertTrue(lastLogOp < logWriter.getLastLogTime());
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

        assertFalse("file is open", hdr.getOpen());
        assertEquals("file size is not 0", 0, hdr.getFileSize());
        assertEquals("header sequence is not 0", 0, hdr.getSequence());
        assertEquals("server id is set", "unknown", hdr.getServerId());
        assertEquals("unexpected first op time", 0, hdr.getFirstOpTstamp());
        assertEquals("unexpected last op time", 0, hdr.getLastOpTstamp());
        assertEquals("unexpected create time", 0, hdr.getCreateTime());

        hdr.init(conn);

        // reading an existing header
        anExistingHdr = new LogHeader("should be overwritten");
        anExistingHdr.read(conn);
        assertEquals("header from file should match serialized data", hdr, anExistingHdr);

        // init an existing header
        anExistingHdr = new LogHeader("should be overwritten");
        anExistingHdr.init(conn);
        assertEquals("header from file should match serialized data", hdr, anExistingHdr);
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

        assertTrue("open != true", hdr.getOpen());
        assertEquals(1, hdr.getFileSize());
        assertEquals(2, hdr.getSequence());
        assertEquals("serverId", hdr.getServerId());
        assertEquals(3, hdr.getFirstOpTstamp());
        assertEquals(4, hdr.getLastOpTstamp());
        assertEquals(5, hdr.getCreateTime());

        hdr.init(conn);
        LogHeader fromDB = new LogHeader("should be overwritten");
        fromDB.init(conn);
        assertEquals("header from file should match serialized data", hdr, fromDB);
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
        header[versionLocation] = (byte) (header[versionLocation] + 1);
        DbDistibutedRedolog.clearRedolog(conn);
        DbDistibutedRedolog.logOp(conn, OpType.HEADER, new ByteArrayInputStream(header));
        conn.commit();
        try {
            hdr.read(conn);
        } catch (IOException e) {
            assertTrue("Version in file should be too high.", e.getMessage().contains("is higher than the highest known version"));
            return;
        }
        fail("no exception thrown");
    }

    @After
    public void tearDown() throws Exception {
        conn.closeQuietly();
    }
}