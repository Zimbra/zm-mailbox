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

import junit.framework.TestCase;

import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMessage.ZMimePart;


/**
 * @author bburtin
 */
public class TestConversion extends TestCase {

    private static final String USER_NAME = "user1";
    
    /**
     * Tests downloading attachments from a TNEF message (bug 44263).
     */
    public void testTnef()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZMessage msg = TestUtil.getMessage(mbox, "in:inbox subject:\"Rich text (TNEF) test\"");
        byte[] data = TestUtil.getContent(mbox, msg.getId(), "upload.gif");
        assertEquals(73, data.length);
        data = TestUtil.getContent(mbox, msg.getId(), "upload2.gif");
        assertEquals(851, data.length);
        
        ZMimePart part = TestUtil.getPart(msg, "upload.gif");
        assertEquals(73, part.getSize());
        part = TestUtil.getPart(msg, "upload2.gif");
        assertEquals(851, part.getSize());
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestConversion.class);
    }
}
