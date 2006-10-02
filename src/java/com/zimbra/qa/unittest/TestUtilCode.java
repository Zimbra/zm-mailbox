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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.ValueCounter;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestUtilCode extends TestCase
{
    public void testFillTemplate()
    {
        String template = "The quick ${COLOR} ${ANIMAL}\njumped over the ${ADJECTIVE} dogs.\n";
        Map vars = new HashMap();
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
        
        HashMap vars = new HashMap();
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
        assertEquals("Integer", StringUtil.getSimpleClassName(new Integer(0)));
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
        TimeoutMap map = new TimeoutMap(500);
        
        // Add values 1-99, which should all time out.  Test both the put()
        // and putAll methods().
        Map timeouts = new HashMap();
        for (int i = 1; i <= 49; i++) {
            timeouts.put(new Integer(i), new Integer(i));
        }
        map.putAll(timeouts);
        for (int i = 50; i <= 99; i++) {
            map.put(new Integer(i), new Integer(i));
        }
        
        Integer oneHundred = new Integer(100);

        for (int i = 1; i <= 99; i++) {
            Integer test = new Integer(i);
            assertTrue("1: map does not contain key " + test, map.containsKey(test));
            assertTrue("1: map does not contain value " + test, map.containsValue(test));
            assertEquals("1: value for key " + test + " does not match", test, map.get(test));
        }
        
        assertEquals("1: Map size is incorrect", 99, map.size());
        assertFalse("1: map contains key 100", map.containsKey(oneHundred));
        assertFalse("1: map contains value 100", map.containsValue(oneHundred));
        assertNull("1: map value for key 100 is not null", map.get(oneHundred));
        
        Thread.sleep(700);
        map.put(oneHundred, oneHundred);
        
        assertEquals("Map size is incorrect", 1, map.size());
        
        for (int i = 1; i <= 99; i++) {
            Integer test = new Integer(i);
            assertFalse("2: map contains key " + test, map.containsKey(test));
            assertFalse("2: map contains value " + test, map.containsValue(test));
            assertNull("2: value for key " + test + " is not null", map.get(test));
        }
        
        assertTrue("2: map does not contain key 100", map.containsKey(oneHundred));
        assertTrue("2: map does not contain value 100", map.containsValue(oneHundred));
        assertEquals("2: value for key 100 does not match", oneHundred, map.get(oneHundred));
    }
}
