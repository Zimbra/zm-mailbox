/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.common.mime.Rfc822ValidationInputStream;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileSegmentDataSource;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LruMap;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.TruncatingWriter;
import com.zimbra.common.util.ValueCounter;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestUtilCode extends TestCase
{
    private static final String NAME_PREFIX = TestUtilCode.class.getSimpleName();
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    public void testFillTemplate() {
        String template = "The quick ${COLOR} ${ANIMAL}\njumped over the ${ADJECTIVE} dogs.\n";
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("COLOR", "brown");
        vars.put("ANIMAL", "fox");
        vars.put("ADJECTIVE", "lazy");
        String result = StringUtil.fillTemplate(template, vars);
        String expected = "The quick brown fox\njumped over the lazy dogs.\n";
        assertEquals(expected, result);
    }

    public void testFillTemplateWithNewlineValue() {
        String template = "New message received at ${RECIPIENT_ADDRESS}." +
            "${NEWLINE}Sender: ${SENDER_ADDRESS}${NEWLINE}Subject: ${SUBJECT}";

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("SENDER_ADDRESS", "sender@example.zimbra.com");
        vars.put("RECIPIENT_ADDRESS", "recipient@example.zimbra.com");
        vars.put("RECIPIENT_DOMAIN", "example.zimbra.com");
        vars.put("NOTIFICATION_ADDRESS", "notify@example.zimbra.com");
        vars.put("SUBJECT", "Cool stuff");
        vars.put("NEWLINE", "\n");

        String expected = "New message received at recipient@example.zimbra.com." +
        "\nSender: sender@example.zimbra.com\nSubject: Cool stuff";
        String actual = StringUtil.fillTemplate(template, vars);
        assertEquals("expected: '" + expected + "', actual: '" + actual + "'",
                expected, actual);
    }

    public void testFillTemplateWithBraces() {
        String template = "Beginning ${VAR} { end }";
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("VAR", "middle");
        String result = StringUtil.fillTemplate(template, vars);
        String expected = "Beginning middle { end }";
        assertEquals(expected, result);
    }
    
    public void testJoin() {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals("a,b,c", StringUtil.join(",", list));
        String[] array = new String[list.size()];
        list.toArray(array);
        assertEquals("a,b,c", StringUtil.join(",", array));
        
        // Make sure things still work if the first element is empty (bug 29513)
        list.set(0, "");
        assertEquals(",b,c", StringUtil.join(",", list));
        list.toArray(array);
        assertEquals(",b,c", StringUtil.join(",", array));
    }

    public void testSimpleClassName() {
        assertEquals("MyClass", StringUtil.getSimpleClassName("my.package.MyClass"));
        Integer i = 0;
        assertEquals("Integer", StringUtil.getSimpleClassName(i));
    }

    public void testValueCounter()
    throws Exception {
        ValueCounter<String> vc = new ValueCounter<String>();
        vc.increment("one");
        vc.increment("two");
        vc.increment("two");
        vc.increment("two");
        vc.decrement("two");
        vc.increment("three", 3);

        assertEquals("one", 1, vc.getCount("one"));
        assertEquals("two", 2, vc.getCount("two"));
        assertEquals("three", 3, vc.getCount("three"));
        assertEquals("total", 6, vc.getTotal());
        assertEquals("size", 3, vc.size());

        vc.clear();

        assertEquals("one", 0, vc.getCount("one"));
        assertEquals("two", 0, vc.getCount("two"));
        assertEquals("total", 0, vc.getTotal());
        assertEquals("size", 0, vc.size());
    }

    public void testTimeoutMap()
    throws Exception {
        ZimbraLog.test.debug("testTimeoutMap()");
        TimeoutMap<Integer, Integer> map = new TimeoutMap<Integer, Integer>(500);

        // Add values 1-99, which should all time out.  Test both the put()
        // and putAll methods().
        Map<Integer, Integer> timeouts = new HashMap<Integer, Integer>();
        for (int i = 1; i <= 49; i++) {
            timeouts.put(i, i);
        }
        map.putAll(timeouts);
        for (int i = 50; i <= 99; i++) {
            map.put(i, i);
        }

        Integer oneHundred = 100;

        for (int i = 1; i <= 99; i++) {
            assertTrue("1: map does not contain key " + i, map.containsKey(i));
            assertTrue("1: map does not contain value " + i, map.containsValue(i));
            assertEquals("1: value for key " + i + " does not match", i, (int) map.get(i));
        }

        assertEquals("1: Map size is incorrect", 99, map.size());
        assertFalse("1: map contains key 100", map.containsKey(oneHundred));
        assertFalse("1: map contains value 100", map.containsValue(oneHundred));
        assertNull("1: map value for key 100 is not null", map.get(oneHundred));

        Thread.sleep(700);
        map.put(oneHundred, oneHundred);

        assertEquals("Map size is incorrect", 1, map.size());

        for (int i = 1; i <= 99; i++) {
            assertFalse("2: map contains key " + i, map.containsKey(i));
            assertFalse("2: map contains value " + i, map.containsValue(i));
            assertNull("2: value for key " + i + " is not null", map.get(i));
        }

        assertTrue("2: map does not contain key 100", map.containsKey(oneHundred));
        assertTrue("2: map does not contain value 100", map.containsValue(oneHundred));
        assertEquals("2: value for key 100 does not match", oneHundred, map.get(oneHundred));
    }

    /**
     * Tests {@link ListUtil#split} on lists of size 0 through 50, splitting by
     * 10 items.
     */
    public void testSplit()
    throws Exception {
        for (int i = 0; i < 50; i++) {
            List<Integer> list = new ArrayList<Integer>();
            for (int j = 0; j < i; j++) {
                list.add(j);
            }
            List<List<Integer>> listOfLists = ListUtil.split(list, 10);

            // Check number of splits
            int expectedSize = 0;
            if (list.size() > 0) {
                expectedSize = ((list.size() - 1) / 10) + 1;
            }
            assertEquals("Unexpected number of splits for list of size " + list.size(),
                expectedSize, listOfLists.size());

            // Check sublist elements
            for (int j = 0; j < i; j++) {
                int listNum = j / 10;
                int index = j % 10;
                String context = String.format("j=%d, listNum=%d, index=%d", j, listNum, index);
                assertEquals(context, list.get(j), listOfLists.get(listNum).get(index));
            }
            
            ZimbraLog.test.debug(String.format("Split a list of %d items into %d lists", list.size(), listOfLists.size()));
            assertTrue("Lists don't match: " + StringUtil.join(",", list), compareLists(list, listOfLists));
        }
    }
    
    /**
     * Tests {@link SystemUtil#getInnermostException(Throwable)}.
     */
    public void testInnermostException()
    throws Exception {
        assertNull(SystemUtil.getInnermostException(null));
        Exception inner = new Exception("inner");
        Exception middle = new Exception("middle", inner);
        Exception outer = new Exception("outer", middle);
        assertSame(inner, SystemUtil.getInnermostException(outer));
    }
    
    /**
     * Tests {@link ByteUtil#getSHA1Digest.
     */
    public void testSHA1Digest()
    throws Exception {
        byte[] data = "I am not a number.  I am a free man.".getBytes();
        String expected = "cc1ce56b9820cb5c4d6df9c9e39de0c7bf5b44a3";
        String expectedBase64 = "zBzla5ggy1xNbfnJ453gx79bRKM=";
        
        assertEquals(expected, ByteUtil.getSHA1Digest(data, false));
        assertEquals(expectedBase64, ByteUtil.getSHA1Digest(data, true));
        assertEquals(expectedBase64, ByteUtil.getDigest(data));
        
        assertEquals(expected, ByteUtil.getSHA1Digest(new ByteArrayInputStream(data), false));
        assertEquals(expectedBase64, ByteUtil.getSHA1Digest(new ByteArrayInputStream(data), true));
    }
    
    /**
     * Tests {@link ByteUtil#getContent(Reader, int, boolean)}.
     */
    public void testGetReaderContent()
    throws Exception {
        String s = "12345";
        assertEquals("", ByteUtil.getContent(new StringReader(s), 0, true));
        assertEquals("123", ByteUtil.getContent(new StringReader(s), 3, true));
        assertEquals("12345", ByteUtil.getContent(new StringReader(s), 5, true));
        assertEquals("12345", ByteUtil.getContent(new StringReader(s), 10, true));
        assertEquals("12345", ByteUtil.getContent(new StringReader(s), -1, true));
        
        Reader reader = new StringReader(s);
        ByteUtil.getContent(reader, 3, false);
        assertEquals("4", ByteUtil.getContent(reader, 1, true));
        try {
            ByteUtil.getContent(reader, 1, false);
            fail("IOException was not thrown");
        } catch (IOException e) {
        }
    }
    
    /**
     * Makes sure that {@link ZimbraLog#addAccountNameToContext} can be called
     * with a <tt>null</tt> value.  See bug 26997 for details.
     */
    public void testAccountLoggerNullAccountName()
    throws Exception {
        ZimbraLog.addAccountNameToContext(null);
        ZimbraLog.test.addAccountLogger(TestUtil.getAddress("user1"), Log.Level.info);
        ZimbraLog.test.debug("Testing addAccountNameToContext(null).");
    }
    
    public void testAccountLoggerMultipleAccountNames()
    throws Exception {
        String address1 = TestUtil.getAddress("user1");
        String address2 = TestUtil.getAddress("user2");
        
        ZimbraLog.addAccountNameToContext(address1);
        ZimbraLog.addAccountNameToContext(address2);
        Set<String> names = ZimbraLog.getAccountNamesFromContext();
        assertEquals(1, names.size());
        assertTrue(names.contains(address2));
    }
    
    /**
     * Tests the Gzip methods in {@link ByteUtil} and {@link FileUtil}.
     */
    public void testGzip()
    throws Exception {
        String s = "Put the message in a box";
        byte[] original = s.getBytes();
        byte[] compressed = ByteUtil.compress(original);
        byte[] uncompressed = ByteUtil.uncompress(compressed);
        
        assertFalse(ByteUtil.isGzipped(original));
        assertTrue(ByteUtil.isGzipped(compressed));
        assertFalse(ByteUtil.isGzipped(uncompressed));
        assertEquals(s, new String(uncompressed));
        
        // Test uncompressed file on disk.
        File file = File.createTempFile("TestUtilCode.testGzip-uncompressed", null);
        FileOutputStream out = new FileOutputStream(file);
        out.write(original);
        out.close();
        assertFalse(FileUtil.isGzipped(file));
        file.delete();
        
        // Test compressed file on disk.
        file = File.createTempFile("TestUtilCode.testGzip-compressed", null);
        out = new FileOutputStream(file);
        out.write(compressed);
        out.close();
        assertTrue(FileUtil.isGzipped(file));
        file.delete();
    }
    
    public void testRfc822Validation()
    throws Exception {
        validateRfc822("A", 0, null, true);
        validateRfc822("A", 1, null, true);
        validateRfc822(getLongString(10240), 0, null, true);
        validateRfc822(getLongString(10241), 0, null, false);
        validateRfc822("A: B\r\n" + getLongString(10240), 0, null, true);
        validateRfc822("A: B\r\n", 10, getLongString(10230), true);
        validateRfc822("A: B\r\n", 10240, "\r\nxyz", true);
        validateRfc822("A: B\r\n", 10241, "\r\nxyz", false);
        validateRfc822("A: B\r\n", 10240, "xyz", false);
    }
    
    private String getLongString(int length) {
        byte[] content = new byte[length];
        for (int i = 0; i < length; i++) {
            content[i] = (byte) ('a' + ((i % 26)));
        }
        return new String(content);
    }
    
    private void validateRfc822(String before, int numNullBytes, String after, boolean isValid)
    throws IOException {
        byte[] content = getBytes(before, numNullBytes, after);
        
        // Test reading into a buffer.
        byte[] buf = new byte[100];
        Rfc822ValidationInputStream in = new Rfc822ValidationInputStream(new ByteArrayInputStream(content), 10240);
        while ((in.read(buf)) >= 0) {
        }
        in.close();
        assertEquals(isValid, in.isValid());
        
        // Test reading on character at a time.
        in = new Rfc822ValidationInputStream(new ByteArrayInputStream(content), 10240);
        while ((in.read()) >= 0) {
        }
        in.close();
        assertEquals(isValid, in.isValid());
        
        // Compare content.
        byte[] copy = ByteUtil.getContent(new Rfc822ValidationInputStream(new ByteArrayInputStream(content), 10240), content.length);
        for (int i = 0; i < content.length; i++) {
            assertEquals("Mismatch at byte " + i, content[i], copy[i]);
        }
    }
    
    private byte[] getBytes(String before, int numNullBytes, String after) {
        if (before == null) {
            before = "";
        }
        if (after == null) {
            after = "";
        }
        byte[] beforeBytes = before.getBytes();
        byte[] afterBytes = after.getBytes();
        byte[] content = new byte[beforeBytes.length + numNullBytes + afterBytes.length];
        System.arraycopy(beforeBytes, 0, content, 0, beforeBytes.length);
        System.arraycopy(afterBytes, 0, content, beforeBytes.length + numNullBytes, afterBytes.length);
        return content;
    }
    
    /**
     * Tests {@link TruncatingWriter}
     */
    public void testTruncatingWriter()
    throws Exception {
        doTruncatingWriterTest(0);
        doTruncatingWriterTest(5);
        doTruncatingWriterTest(100);
    }
    
    private void doTruncatingWriterTest(int maxChars)
    throws Exception {
        String original = "Come talk to me";
        StringWriter sw = new StringWriter();
        Writer w = new TruncatingWriter(sw, maxChars);
        w.append(original);
        String s = sw.toString();
        int actualChars = Math.min(maxChars, original.length());
        assertEquals(actualChars, s.length());
        assertEquals(original.substring(0, actualChars), s);
    }
    
    private static <E> boolean compareLists(List<E> list, List<List<E>> listOfLists) {
        int i = 0;
        for (List<E> curList : listOfLists) {
            for (E item : curList) {
                if (!item.equals(list.get(i))) {
                    return false;
                }
                i++;
            }
        }
        return true;
    }
    
    public void testFileSegmentDataSource()
    throws Exception {
        // Write "12345" to a temporary file.
        File file = File.createTempFile(NAME_PREFIX, null);
        String content = "12345";
        FileOutputStream out = new FileOutputStream(file);
        out.write(content.getBytes());
        out.close();
        
        FileSegmentDataSource ds = new FileSegmentDataSource(file, 0, 5);
        assertEquals("12345", new String(ByteUtil.getContent(ds.getInputStream(), 5)));
        assertEquals("12345", new String(ByteUtil.getContent(ds.getInputStream(), 5))); // Make sure we can get multiple streams.
        
        ds = new FileSegmentDataSource(file, 1, 3);
        assertEquals("234", new String(ByteUtil.getContent(ds.getInputStream(), 5)));
        
        file.delete();
    }
    
    @SuppressWarnings("serial")
    private class LruTest<K, V>
    extends LruMap<K, V> {

        K lastRemovedKey;
        V lastRemovedValue;

        LruTest(int maxSize) {
            super(maxSize);
        }
        
        K getLastRemovedKey() { return lastRemovedKey; }
        V getLastRemovedValue() { return lastRemovedValue; }
        
        @Override
        protected void willRemove(K key, V value) {
            lastRemovedKey = key;
            lastRemovedValue = value;
        }
        
    }
    public void testLruMap()
    throws Exception {
        LruTest<Integer, String> map = new LruTest<Integer, String>(3);
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
        assertEquals(1, (int) map.getLastRemovedKey());
        assertEquals("one", map.getLastRemovedValue());
        assertFalse(map.containsKey(1));
        
        map.get(2);
        map.put(5, "five");
        assertEquals(3, (int) map.getLastRemovedKey());
        assertEquals("three", map.getLastRemovedValue());
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        ZimbraLog.test.removeAccountLogger(TestUtil.getAddress("user1"));
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestUtilCode.class);
    }
}
