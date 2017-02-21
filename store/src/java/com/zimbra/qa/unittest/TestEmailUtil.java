/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.mail.internet.InternetAddress;

import junit.framework.TestCase;

import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.mime.Mime;

/**
 * @author bburtin
 */
public class TestEmailUtil extends TestCase
{
    public void testSplit() {
        assertNull(EmailUtil.getLocalPartAndDomain("foo"));
        assertNull(EmailUtil.getLocalPartAndDomain("foo@"));
        assertNull(EmailUtil.getLocalPartAndDomain("@foo"));
        
        String[] parts = EmailUtil.getLocalPartAndDomain("jspiccoli@example.zimbra.com");
        assertNotNull(parts);
        assertEquals("jspiccoli", parts[0]);
        assertEquals("example.zimbra.com", parts[1]);
    }
    
    /**
     * Tests {@link EmailUtil#isRfc822Message}.
     */
    public void testRfc822()
    throws Exception {
        assertTrue(isRfc822Message("Content-Type: text/plain"));
        assertFalse(isRfc822Message("Content-Type text/plain"));
        assertFalse(isRfc822Message("Content-Type\r\n  :text/plain"));
        
        // Test a line longer than 998 characters.
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= 998; i++) {
            buf.append("X");
        }
        buf.append(": Y");
        assertFalse(isRfc822Message(buf.toString()));
    }
    
    /**
     * Confirms that address parsing doesn't blow up when the
     * header value is malformed (bug 32271). 
     */
    public void testParseAddressHeader()
    throws Exception {
        Mime.parseAddressHeader("(Test) <djoe@zimbra.com>,djoe@zimbra.com (Test)");
    }
    
    private boolean isRfc822Message(String content)
    throws IOException {
        return EmailUtil.isRfc822Message(new ByteArrayInputStream(content.getBytes()));
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.runTest(TestEmailUtil.class);
    }
}
