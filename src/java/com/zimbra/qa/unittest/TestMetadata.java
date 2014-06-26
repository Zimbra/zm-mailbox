/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
