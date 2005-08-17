/*
 * Created on Mar 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.qa.unittest;

import junit.framework.TestCase;

import com.liquidsys.coco.service.util.ParseMailboxID;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.account.Provisioning;



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
            assertEquals(id.getMailbox(), Mailbox.getMailboxById(1));
            
            assertFalse(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
            
            user1AcctID = id.getMailbox().getAccountId();
        }
        
        {
            ParseMailboxID id = ParseMailboxID.parse("user1@liquidsys.com");
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 1);
            assertEquals(id.getMailbox(), Mailbox.getMailboxById(1));
            assertFalse(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
        }   
        
        {
            ParseMailboxID id = ParseMailboxID.parse(user1AcctID);
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 1);
            assertEquals(id.getMailbox(), Mailbox.getMailboxById(1));
            assertFalse(id.isAllMailboxIds());
            assertFalse(id.isAllServers());
        }

        String localhost = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_liquidServiceHostname);
        
        {
            ParseMailboxID id = ParseMailboxID.parse("/"+localhost+"/1");
            
            assertTrue(id.isLocal());
            assertEquals(id.getServer(), null);
            assertEquals(id.getMailboxId(), 1);
            assertEquals(id.getMailbox(), Mailbox.getMailboxById(1));
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
