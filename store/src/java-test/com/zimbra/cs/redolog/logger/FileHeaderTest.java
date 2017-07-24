package com.zimbra.cs.redolog.logger;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FileHeaderTest {
    private FileHeader hdr;
    private RandomAccessFile raFile;

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        raFile = new RandomAccessFile(folder.newFile("headerfile.txt"), "rw");
    }

    @Test
    public void uninitializedHeader() throws Exception {
        hdr = new FileHeader();
        Assert.assertFalse("file is open", hdr.getOpen());
        Assert.assertEquals("file size is not 0", 0, hdr.getFileSize());
        Assert.assertEquals("header sequence is not 0", 0, hdr.getSequence());
        Assert.assertEquals("server id is set", "unknown", hdr.getServerId());
        Assert.assertEquals("unexpected first op time", 0, hdr.getFirstOpTstamp());
        Assert.assertEquals("unexpected last op time", 0, hdr.getLastOpTstamp());
        Assert.assertEquals("unexpected create time", 0, hdr.getCreateTime());

        hdr.write(raFile);
        FileHeader fromFile = new FileHeader("should be overwritten");
        fromFile.read(raFile);
        Assert.assertEquals("header from file should match serialized data",
                            hdr, fromFile);
    }

    @Test
    public void setAllFields() throws Exception {
        hdr = new FileHeader("serverId");
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

        hdr.write(raFile);
        FileHeader fromFile = new FileHeader("should be overwritten");
        fromFile.read(raFile);
        Assert.assertEquals("header from file should match serialized data",
                            hdr, fromFile);
    }

    @Test(expected = IOException.class)
    public void junkFile() throws Exception {
        raFile.write("this is not a valid header".getBytes());
        hdr = new FileHeader();
        hdr.read(raFile);
    }

    @Test
    public void versionTooHigh() throws Exception {
        hdr = new FileHeader();
        hdr.write(raFile);
        // Fake up a bad version
        final int versionLocation =
            7 /* magic */ + 1 /* open */ + 8 /* file size */ +
            8 /* sequence */ + 1 /* serverid length */ + 127 /* serverid */ +
            8 /* firstOpTstamp */ + 8 /* lastOpTstamp */;
        raFile.seek(versionLocation);
        // Read the major version and add 1 to get something invalid.
        // Version has no public methods to accomplish this.
        short majorVersion = raFile.readShort();
        raFile.seek(versionLocation);
        raFile.writeShort(majorVersion + 1);
        try {
            hdr.read(raFile);
        } catch (IOException e) {
            Assert.assertTrue("Version in file should be too high.",
                              e.getMessage().contains(
                                  "is higher than the highest known version"));
            return;
        }
        Assert.fail("no exception thrown");
    }
}
