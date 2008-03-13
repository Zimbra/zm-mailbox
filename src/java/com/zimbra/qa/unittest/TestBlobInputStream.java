/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.mail.internet.SharedInputStream;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.cs.store.BlobInputStream;


public class TestBlobInputStream extends TestCase {

    private File mFile;
    
    public void setUp()
    throws Exception {
    }
    
    public void testBlobInputStream()
    throws Exception {
        String CONTENT = "0123456789";
        createFile(CONTENT);
        BlobInputStream in = new BlobInputStream(mFile);
        
        // Test reading all content
        String read = getContent(in, 100);
        assertEquals(CONTENT, read);
        checkEof(in);
        in.close();
        
        // Test reading beginning and end
        in = new BlobInputStream(mFile);
        assertEquals("01234", getContent(in, 5));
        assertEquals("56789", getContent(in, 100));
        checkEof(in);
        in.close();
        
        // Test invalid start/end
        try {
            in = new BlobInputStream(mFile, 6L, 5L);
            fail("Test with start=6 and end=5 should not have succeeded.");
        } catch (IOException e) {
        }
        
        // Test skip
        in = new BlobInputStream(mFile);
        assertEquals(2, in.skip(2));
        assertEquals("23", getContent(in, 2));
        assertEquals(3, in.skip(3));
        assertEquals("7", getContent(in, 1));
        assertEquals(2, in.skip(1000));
        checkEof(in);
        in.close();
        
        // Test mark
        in = new BlobInputStream(mFile);
        assertTrue(in.markSupported());
        boolean success = true;
        try {
            in.reset();
        } catch (IOException e) {
            success = false;
        }
        assertFalse("reset() should not have succeeded", success);
        assertEquals("012", getContent(in, 3));
        in.mark(3);
        assertEquals("34", getContent(in, 2));
        in.reset();
        assertEquals("34", getContent(in, 2));
        assertEquals("56", getContent(in, 2));
        success = true;
        try {
            in.reset();
        } catch (IOException e) {
            success = false;
        }
        assertFalse("reset() should not have succeeded", success);
        in.close();
        
        // Test reading a byte array
        in = new BlobInputStream(mFile);
        byte[] buf = new byte[5];
        assertEquals(2, in.read(buf, 3, 2));
        assertEquals((byte) '0', buf[3]);
        assertEquals((byte) '1', buf[4]);
        assertEquals(5, in.read(buf));
        assertEquals("23456", new String(buf));
        in.close();
        
        // Test substream - all content
        InputStream sub = in.newStream(0, CONTENT.length());
        assertEquals(CONTENT, getContent(sub, 100));
        checkEof(sub);
        sub.close();
        
        // Test substream beginning
        sub = in.newStream(0, 5);
        assertEquals("01234", getContent(sub, 100));
        checkEof(sub);
        sub.close();
        
        // Test substream end
        sub = in.newStream(5, 10);
        assertEquals("56789", getContent(sub, 100));
        checkEof(sub);
        sub.close();
        
        sub = in.newStream(5, -1);
        assertEquals("56789", getContent(sub, 100));
        checkEof(sub);
        sub.close();

        // Test substream past EOF
        assertEquals(null, in.newStream(5, 11));
        
        // Test substream middle
        sub = in.newStream(3, 6);
        assertEquals("345", getContent(sub, 100));
        checkEof(sub);
        sub.close();
        
        // Test substream position
        sub = in.newStream(3, 6);
        assertEquals(0, ((SharedInputStream) sub).getPosition());
        sub.read(new byte[2]);
        assertEquals(2, ((SharedInputStream) sub).getPosition());
        sub.close();
        
        // Test sub-substream
        InputStream subsub = ((BlobInputStream) sub).newStream(1, 3);
        assertEquals("45", getContent(subsub, 100));
        
        // Test position after reading 1 character
        in.close();
        in = new BlobInputStream(mFile);
        assertEquals(0, in.getPosition());
        in.read();
        assertEquals(1, in.getPosition());
        in.close();
    }

    /**
     * Tests reading a large file.  Exercises the buffering code.
     */
    public void testLargeFile()
    throws Exception {
    	String content = createFile(5000);
    	BlobInputStream in = new BlobInputStream(mFile);
    	assertEquals(content, getContent(in, 5000));
    	in.close();
    	
    	// Test reading 1 char at a time, then a byte array.  This tests
    	// the section of BlobInputStream.read(byte[]), where it reads
    	// part of the data from the buffer and part from the file.
    	in = new BlobInputStream(mFile);
    	String firstChunk = getContent(in, 1000);
    	assertEquals(content.substring(0, 1000), firstChunk);
    	byte[] secondChunk = new byte[2000];
    	int numRead = in.read(secondChunk);
        assertEquals(2000, numRead);
    	assertEquals(content.substring(1000, 3000), new String(secondChunk));
        
        // Test bug 24715.  Make sure that we don't get IndexOutOfBoundsException
        // when reading another byte[]
        byte[] thirdChunk = new byte[2000];
        numRead = in.read(thirdChunk);
        assertEquals(2000, numRead);
        assertEquals(content.substring(3000, 5000), new String(thirdChunk));
        
        in.close();
    }
    
    private String createFile(int numBytes)
    throws Exception {
    	StringBuilder buf = new StringBuilder();
    	Random random = new Random();
    	for (int i = 0; i < numBytes; i++) {
    		char c = 'a';
    		c += random.nextInt(26);
    		buf.append(c);
    	}
    	String s = buf.toString();
    	createFile(s);
    	return s;
    }
    
    private void createFile(String data)
    throws Exception {
        mFile = File.createTempFile("TestBlobInputStream", ".txt");
        FileWriter writer = new FileWriter(mFile);
        writer.write(data);
        writer.close();
    }
    
    private String getContent(InputStream in, int maxBytes)
    throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= maxBytes; i++) {
            int c = in.read();
            if (c <= 0) {
                break;
            }
            builder.append((char) c);
        }
        return builder.toString();
    }
    
    private void checkEof(InputStream in)
    throws Exception {
        assertEquals(-1, in.read());
        byte[] buf = new byte[10];
        assertEquals(-1, in.read(buf));
        assertEquals(-1, in.read(buf, 5, 2));
    }
    
    public void tearDown()
    throws Exception {
    	if (mFile != null) {
    		mFile.delete();
    	}
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestBlobInputStream.class));
    }
}
