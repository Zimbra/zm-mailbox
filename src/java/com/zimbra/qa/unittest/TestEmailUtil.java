/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

import com.zimbra.common.util.EmailUtil;

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
        
        String[] parts = EmailUtil.getLocalPartAndDomain("jspiccoli@example.zimbra.com");
        assertNotNull(parts);
        assertEquals("jspiccoli", parts[0]);
        assertEquals("example.zimbra.com", parts[1]);
    }
}
