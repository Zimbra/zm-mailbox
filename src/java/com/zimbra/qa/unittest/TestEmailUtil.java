package com.zimbra.qa.unittest;

import com.zimbra.cs.util.EmailUtil;

import junit.framework.TestCase;

/**
 * @author bburtin
 */
public class TestEmailUtil extends TestCase
{
    public void testSplit()
    {
        assertNull(EmailUtil.getLocalPartAndDomain("foo"));
        assertNull(EmailUtil.getLocalPartAndDomain("foo@"));
        assertNull(EmailUtil.getLocalPartAndDomain("@foo"));
        
        String[] parts = EmailUtil.getLocalPartAndDomain("jspiccoli@liquidsys.com");
        assertNotNull(parts);
        assertEquals("jspiccoli", parts[0]);
        assertEquals("liquidsys.com", parts[1]);
    }
}
