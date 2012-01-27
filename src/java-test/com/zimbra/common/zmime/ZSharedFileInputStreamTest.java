/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.zmime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.mail.internet.SharedInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;

public class ZSharedFileInputStreamTest {
    private void checkStream(ZSharedFileInputStream is, String expected) throws Exception {
        int length = expected.length();

        Assert.assertFalse("not buffered", is.isBuffered());
        Assert.assertEquals("position is 0", 0L, is.getPosition());
        Assert.assertFalse("not buffered", is.isBuffered());
        Assert.assertEquals("available is " + length, length, is.available());
        Assert.assertArrayEquals(length + " bytes match", expected.getBytes(), ByteUtil.readInput(is, 100, 100));
        Assert.assertTrue("buffered", is.isBuffered());
        Assert.assertEquals("position is " + length, length, is.getPosition());

        Assert.assertEquals("read() eof", -1, is.read());
        byte[] buf = new byte[10];
        Assert.assertEquals("read(byte[]) eof", -1, is.read(buf));
        Assert.assertEquals("read(byte[], int, int) eof", -1, is.read(buf, 5, 2));

        is.close();
    }

    @Test
    public void stream() throws Exception {
        File file = File.createTempFile("zsfist", ".tmp");
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write("0123456789".getBytes());
        fos.close();

        ZSharedFileInputStream is = new ZSharedFileInputStream(file);
        checkStream(is, "0123456789");
        checkStream(is.newStream(0, 10), "0123456789");
        checkStream(is.newStream(0, -1), "0123456789");
        checkStream(is.newStream(1, 9), "12345678");
        checkStream(is.newStream(1, -1), "123456789");

        ZSharedFileInputStream substream = is.newStream(2, 8);
        checkStream(substream, "234567");
        checkStream(substream.newStream(0, 6), "234567");
        checkStream(substream.newStream(0, -1), "234567");
        checkStream(substream.newStream(0, 4), "2345");
        checkStream(substream.newStream(2, 6), "4567");
        checkStream(substream.newStream(2, -1), "4567");
        checkStream(substream.newStream(2, 4), "45");
    }

    @Test
    public void bis() throws Exception {
        final String CONTENT = "0123456789";
        File file = createFile(CONTENT);

        ZSharedFileInputStream in = new ZSharedFileInputStream(file);

        // Test reading all content
        String read = getContent(in, 100);
        Assert.assertEquals(CONTENT, read);
        checkEof(in);
        in.close();

        // Test reading beginning and end
        in = new ZSharedFileInputStream(file);
        Assert.assertEquals("01234", getContent(in, 5));
        Assert.assertEquals("56789", getContent(in, 100));
        checkEof(in);
        in.close();

        // Test invalid start/end
        try {
            in = new ZSharedFileInputStream(file, 6L, 5L);
            Assert.fail("Test with start=6 and end=5 should not have succeeded.");
        } catch (AssertionError e) {
        }

        // Test skip
        in = new ZSharedFileInputStream(file);
        Assert.assertEquals(2, in.skip(2));
        Assert.assertEquals("23", getContent(in, 2));
        Assert.assertEquals(3, in.skip(3));
        Assert.assertEquals("7", getContent(in, 1));
        Assert.assertEquals(2, in.skip(1000));
        checkEof(in);
        in.close();

        // Test mark
        in = new ZSharedFileInputStream(file);
        Assert.assertTrue(in.markSupported());
        try {
            in.reset();
            Assert.fail("reset() should not have succeeded");
        } catch (IOException e) {
        }
        Assert.assertEquals("012", getContent(in, 3));
        in.mark(3);
        Assert.assertEquals("34", getContent(in, 2));
        in.reset();
        Assert.assertEquals("34", getContent(in, 2));
        Assert.assertEquals("56", getContent(in, 2));
        try {
            in.reset();
        } catch (IOException e) {
            Assert.fail("reset() should not have succeeded");
        }
        in.close();

        // Test reading a byte array with an offset.
        in = new ZSharedFileInputStream(file);
        byte[] buf = new byte[5];
        for (int i = 0; i < 5; i++) {
            buf[i] = 57;
        }
        int numRead = in.read(buf, 3, 2);
        Assert.assertTrue("Unexpected number of bytes read: " + numRead, numRead == 1 || numRead == 2);
        int[] untouchedIndexes = null;
        if (numRead == 1) {
            Assert.assertEquals((byte) '0', buf[3]);
            untouchedIndexes = new int[] { 0, 1, 2, 4 };
        } else if (numRead == 2) {
            Assert.assertEquals((byte) '0', buf[3]);
            Assert.assertEquals((byte) '1', buf[4]);
            untouchedIndexes = new int[] { 0, 1, 2 };
        }
        for (int i : untouchedIndexes) {
            Assert.assertEquals("Unexpected value at index " + i, 57, buf[i]);
        }
        in.close();

        // Test reading into a byte array.
        in = new ZSharedFileInputStream(file);
        in.read();
        in.read();
        numRead = in.read(buf);
        Assert.assertTrue(numRead > 0);
        Assert.assertTrue(numRead <= 5);
        byte[] test = new byte[numRead];
        System.arraycopy(buf, 0, test, 0, numRead);
        Assert.assertTrue("23456".startsWith(new String(test)));
        in.close();

        // Test substream - all content
        InputStream sub = in.newStream(0, CONTENT.length());
        Assert.assertEquals(CONTENT, getContent(sub, 100));
        checkEof(sub);
        sub.close();

        // Test substream beginning
        sub = in.newStream(0, 5);
        Assert.assertEquals("01234", getContent(sub, 100));
        checkEof(sub);
        sub.close();

        // Test substream end
        sub = in.newStream(5, 10);
        Assert.assertEquals("56789", getContent(sub, 100));
        checkEof(sub);
        sub.close();

        sub = in.newStream(5, -1);
        Assert.assertEquals("56789", getContent(sub, 100));
        checkEof(sub);
        sub.close();

        // Test substream past EOF
        sub = in.newStream(5, 11);
        Assert.assertEquals("56789", getContent(sub, 100));
        checkEof(sub);
        sub.close();

        // Test substream middle
        sub = in.newStream(3, 6);
        Assert.assertEquals("345", getContent(sub, 100));
        checkEof(sub);
        sub.close();

        // Test substream position
        sub = in.newStream(3, 6);
        Assert.assertEquals(0, ((SharedInputStream) sub).getPosition());
        sub.read(new byte[2]);
        Assert.assertEquals(2, ((SharedInputStream) sub).getPosition());
        sub.close();

        // Test sub-substream
        InputStream subsub = ((ZSharedFileInputStream) sub).newStream(1, 3);
        Assert.assertEquals("45", getContent(subsub, 100));

        // Test position after reading 1 character
        in.close();
        in = new ZSharedFileInputStream(file);
        Assert.assertEquals(0, in.getPosition());
        in.read();
        Assert.assertEquals(1, in.getPosition());
        in.close();

        // Test reading byte arrays until the end of the file
        in = new ZSharedFileInputStream(file);
        buf = new byte[4];
        while ((numRead = in.read(buf)) >= 0) {
        }
        in.close();

        file.delete();
    }

    @Test
    public void bisLarge() throws Exception {
        File file = createFile(5000);
        String content = ByteUtil.getContent(new FileReader(file), -1, true);

        ZSharedFileInputStream in = new ZSharedFileInputStream(file);
        Assert.assertEquals(content, getContent(in, 5000));
        in.close();

        // Test reading 1 char at a time, then a byte array.  This tests
        // the section of ZSharedFileInputStream.read(byte[]), where it reads
        // part of the data from the buffer and part from the file.
        in = new ZSharedFileInputStream(file);
        String firstChunk = getContent(in, 1000);
        Assert.assertEquals(content.substring(0, 1000), firstChunk);

        byte[] secondChunk = new byte[2000];
        int numRead = in.read(secondChunk);
        Assert.assertTrue(numRead > 0);
        byte[] test = new byte[numRead];
        System.arraycopy(secondChunk, 0, test, 0, numRead);
        Assert.assertEquals(content.substring(1000, 1000 + numRead), new String(test));
        int thirdChunkStartPos = 1000 + numRead;

        // Test bug 24715.  Make sure that we don't get IndexOutOfBoundsException
        // when reading another byte[]
        byte[] thirdChunk = new byte[2000];
        numRead = in.read(thirdChunk);
        Assert.assertTrue(numRead > 0);
        test = new byte[numRead];
        System.arraycopy(thirdChunk, 0, test, 0, numRead);
        Assert.assertEquals(content.substring(thirdChunkStartPos, thirdChunkStartPos + numRead), new String(thirdChunk));

        file.delete();
        in.close();
    }

    private File createFile(int numBytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < numBytes; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return createFile(sb.toString());
    }

    private File createFile(String data) throws Exception {
        File file = File.createTempFile("TestZSFIS", ".txt");
        FileWriter writer = new FileWriter(file);
        writer.write(data);
        writer.close();
        return file;
    }

    private String getContent(InputStream in, int maxBytes) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= maxBytes; i++) {
            int c = in.read();
            if (c <= 0)
                break;
            builder.append((char) c);
        }
        return builder.toString();
    }

    private void checkEof(InputStream in) throws Exception {
        Assert.assertEquals(-1, in.read());
        byte[] buf = new byte[10];
        Assert.assertEquals(-1, in.read(buf));
        Assert.assertEquals(-1, in.read(buf, 5, 2));
    }
}
