/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
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
package com.zimbra.cs.mailclient.imap;

import junit.framework.Assert;

import org.junit.Test;

import com.zimbra.cs.mailclient.ParseException;

public class MailboxNameTest {

    @Test
    public void testAsciiName() throws ParseException {
        String name = "testname";
        Assert.assertEquals(name, MailboxName.decode(name).toString());
    }
    
    @Test
    public void testSpaceName() throws ParseException {
        String name = "test name";
        Assert.assertEquals(name, MailboxName.decode(name).toString());
    }

    @Test
    public void testTabName() throws ParseException {
        String name = "test\tname";
        Assert.assertEquals(name, MailboxName.decode(name).toString());
    }

    @Test
    public void testUtf8Name() throws ParseException {
        String name = "T\u00E5\u00FFpa";
        Assert.assertEquals(name, MailboxName.decode(name).toString());
    }
    
    @Test
    public void testUtf8TabName() throws ParseException {
        String name = "T\u00E5\t\u00FFpa";
        Assert.assertEquals(name, MailboxName.decode(name).toString());
    }

    @Test
    public void testSpaceUtf8Name() throws ParseException {
        String name = "T\u00E5\u00FFpa test";
        Assert.assertEquals(name, MailboxName.decode(name).toString());
    }

    @Test
    public void testUtf7Name() throws ParseException {
        String utf7 = "Skr&AOQ-ppost";
        String name = "Skr\u00E4ppost";
        Assert.assertEquals(name, MailboxName.decode(utf7).toString());
    }
    
    @Test
    public void testUtf7SpaceName() throws ParseException {
        String utf7 = "Skr&AOQ-ppo st";
        String name = "Skr\u00E4ppo st";
        Assert.assertEquals(name, MailboxName.decode(utf7).toString());
    }

    @Test
    public void testBadAsciiName() throws ParseException {
        String name = "test\u0016test";
        Assert.assertEquals("testtest", MailboxName.decode(name).toString());
    }
    
    @Test
    public void testBadUtf8Name() throws ParseException {
        String name = "T\u00E5\u00FFpa\u0000";
        Assert.assertEquals("T\u00E5\u00FFpa", MailboxName.decode(name).toString());
    }

    @Test
    public void testBadUtf7Name() throws ParseException {
        String utf7 = "Skr&AOQ-pp\u0015ost";
        Assert.assertEquals("Skr&AOQ-ppost", MailboxName.decode(utf7).toString());
    }

}
