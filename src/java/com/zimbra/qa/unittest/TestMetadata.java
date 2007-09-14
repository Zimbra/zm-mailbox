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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;

import junit.framework.TestCase;


public class TestMetadata 
extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String METADATA_SECTION = TestMetadata.class.getSimpleName();
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    /**
     * Tests insert, update and delete operations for mailbox metadata.
     */
    public void testMetadata()
    throws Exception {
        ZimbraLog.test.info("Starting testMetadata");
        
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        assertNull(mbox.getConfig(null, METADATA_SECTION));

        // Insert
        Metadata config = new Metadata();
        config.put("string", "mystring");
        mbox.setConfig(null, METADATA_SECTION, config);
        config = mbox.getConfig(null, METADATA_SECTION);
        assertEquals("mystring", config.get("string"));
        
        // Update
        config.put("long", 87);
        mbox.setConfig(null, METADATA_SECTION, config);
        config = mbox.getConfig(null, METADATA_SECTION);
        assertEquals(87, config.getLong("long"));
        assertEquals("mystring", config.get("string"));
        
        // Delete
        mbox.setConfig(null, METADATA_SECTION, null);
        assertNull(mbox.getConfig(null, METADATA_SECTION));
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        mbox.setConfig(null, METADATA_SECTION, null);
    }
}
