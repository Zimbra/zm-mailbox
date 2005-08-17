package com.liquidsys.qa.unittest;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.liquidsys.coco.util.StringUtil;

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
        vars.put("SENDER_ADDRESS", "sender@liquidsys.com");
        vars.put("RECIPIENT_ADDRESS", "recipient@liquidsys.com");
        vars.put("RECIPIENT_DOMAIN", "liquidsys.com");
        vars.put("NOTIFICATION_ADDRESS", "notify@liquidsys.com");
        vars.put("SUBJECT", "Cool stuff");
        vars.put("NEWLINE", "\n");
        
        String expected = "New message received at recipient@liquidsys.com." +
    	"\nSender: sender@liquidsys.com\nSubject: Cool stuff\n";
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
