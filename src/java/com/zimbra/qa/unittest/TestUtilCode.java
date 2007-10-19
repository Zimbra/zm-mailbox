/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.*;

/**
 * @author bburtin
 */
public class TestUtilCode extends TestCase
{
    public void testFillTemplate()
    {
        String template = "The quick ${COLOR} ${ANIMAL}\njumped over the ${ADJECTIVE} dogs.\n";
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("COLOR", "brown");
        vars.put("ANIMAL", "fox");
        vars.put("ADJECTIVE", "lazy");
        String result = StringUtil.fillTemplate(template, vars);
        String expected = "The quick brown fox\njumped over the lazy dogs.\n";
        assertEquals(expected, result);
    }

    public void testFillTemplateWithNewlineValue()
    {
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

    public void testJoin()
    {
        String[] lines = { "a", "b", "c" };
        assertEquals("a\nb\nc", StringUtil.join("\n", lines));
    }

    public void testSimpleClassName()
    {
        assertEquals("MyClass", StringUtil.getSimpleClassName("my.package.MyClass"));
        Integer i = 0;
        assertEquals("Integer", StringUtil.getSimpleClassName(i));
    }

    public void testValueCounter()
    throws Exception {
        ValueCounter vc = new ValueCounter();
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

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestUtilCode.class));
    }
}
