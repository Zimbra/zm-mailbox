/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Mar 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.util.ParseMailboxID;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.account.Provisioning;



/**
 * @author tim
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestParseMailboxID extends TestCase {

    public void testParseMailboxID() throws ServiceException {
        
        String user1AcctID = null;
        {
            ParseMailboxID id = ParseMailboxID.parse("1");
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 1);
            assertEquals(id.getMailbox(), MailboxManager.getInstance().getMailboxById(1));
            
            assertFalse(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
            
            user1AcctID = id.getMailbox().getAccountId();
        }
        
        {
            ParseMailboxID id = ParseMailboxID.parse("user1@example.zimbra.com");
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 1);
            assertEquals(id.getMailbox(), MailboxManager.getInstance().getMailboxById(1));
            assertFalse(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
        }   
        
        {
            ParseMailboxID id = ParseMailboxID.parse(user1AcctID);
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 1);
            assertEquals(id.getMailbox(), MailboxManager.getInstance().getMailboxById(1));
            assertFalse(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
        }

        String localhost = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        
        {
            ParseMailboxID id = ParseMailboxID.parse("/"+localhost+"/1");
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 1);
            assertEquals(id.getMailbox(), MailboxManager.getInstance().getMailboxById(1));
            assertFalse(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
        }

        {
            ParseMailboxID id = ParseMailboxID.parse("*");
            
            assertFalse(id.isLocal());
            assertEquals(id.getServer(), "*");
            assertEquals(id.getMailboxId(), 0);
            assertEquals(id.getMailbox(), null);
            assertTrue(id.isAllMailboxIds());
            assertTrue(id.isAllServers());
        }
        
        {
            String idStr = "/"+localhost+"/*";
            ParseMailboxID id = ParseMailboxID.parse(idStr);
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 0);
            assertEquals(id.getMailbox(), null);
            assertTrue(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
        }
        
        {
            try {
                // this should fail:
                ParseMailboxID id = ParseMailboxID.parse(localhost+"*/3");
                assertFalse(true); // error!
            } catch(ServiceException e) {}
        }
        
    }

}
