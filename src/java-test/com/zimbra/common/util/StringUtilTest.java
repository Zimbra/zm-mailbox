/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


public class StringUtilTest {

    @Test
    public void testFillTemplate() {
        String template = "The quick ${COLOR} ${ANIMAL}\njumped over the ${ADJECTIVE} dogs.\n";
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("COLOR", "brown");
        vars.put("ANIMAL", "fox");
        vars.put("ADJECTIVE", "lazy");
        String result = StringUtil.fillTemplate(template, vars);
        String expected = "The quick brown fox\njumped over the lazy dogs.\n";
        Assert.assertEquals(expected, result);
    }

    @Test
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
        Assert.assertEquals("expected: '" + expected + "', actual: '" + actual + "'",
                expected, actual);
    }

    @Test
    public void testFillTemplateWithBraces() {
        String template = "Beginning ${VAR} { end }";
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("VAR", "middle");
        String result = StringUtil.fillTemplate(template, vars);
        String expected = "Beginning middle { end }";
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testJoin() {
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");
        Assert.assertEquals("a,b,c", StringUtil.join(",", list));
        String[] array = new String[list.size()];
        list.toArray(array);
        Assert.assertEquals("a,b,c", StringUtil.join(",", array));
        
        // Make sure things still work if the first element is empty (bug 29513)
        list.set(0, "");
        Assert.assertEquals(",b,c", StringUtil.join(",", list));
        list.toArray(array);
        Assert.assertEquals(",b,c", StringUtil.join(",", array));
    }

    @Test
    public void testSimpleClassName() {
        Assert.assertEquals("MyClass", StringUtil.getSimpleClassName("my.package.MyClass"));
        Integer i = 0;
        Assert.assertEquals("Integer", StringUtil.getSimpleClassName(i));
    }
}
