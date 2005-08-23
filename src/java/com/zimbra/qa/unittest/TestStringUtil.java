/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.cs.util.StringUtil;

/**
 * @author bburtin
 */
public class TestStringUtil extends TestCase
{
    public void testFillTemplate()
    {
        String template = "The quick ${COLOR} ${ANIMAL}\njumped over the ${ADJECTIVE} dogs.\n";
        Map vars = new HashMap();
        vars.put("COLOR", "brown");
        vars.put("ANIMAL", "fox");
        vars.put("ADJECTIVE", "lazy");
        String result = StringUtil.fillTemplate(new StringReader(template), vars);
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
    	"\nSender: sender@example.zimbra.com\nSubject: Cool stuff\n";
        String actual = StringUtil.fillTemplate(new StringReader(template), vars);
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
}
