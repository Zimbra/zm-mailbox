/*
 * Created on Mar 29, 2005
 *
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;
import com.liquidsys.coco.service.util.ParsedItemID;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author tim
 *
 */
public class TestParsedItemID extends TestCase {
    
    public void test() throws ServiceException {
        {
            ParsedItemID id = ParsedItemID.Parse("/SERVER/123/456");
            assertTrue(id.getServerIDString().equals("SERVER"));
            assertTrue(id.getMailboxIDString().equals("123"));
            assertTrue(id.getMailboxIDInt() == 123);
            assertTrue(id.getItemIDString().equals("456"));
            assertTrue(id.getItemIDInt() == 456);
        }
        
        {
            ParsedItemID id = ParsedItemID.Parse("/SERVER//456");
            assertTrue(id.getServerIDString().equals("SERVER"));
            assertTrue(id.getMailboxIDString() == null);
            assertTrue(id.getMailboxIDInt() == -1);
            assertTrue(id.getItemIDString().equals("456"));
            assertTrue(id.getItemIDInt() == 456);
        }
        
        {
            ParsedItemID id = ParsedItemID.Parse("123/456");
            assertTrue(id.getServerIDString() == null);
            assertTrue(id.getMailboxIDString().equals("123"));
            assertTrue(id.getMailboxIDInt() == 123);
            assertTrue(id.getItemIDString().equals("456"));
            assertTrue(id.getItemIDInt() == 456);
        }
        
        {
            ParsedItemID id = ParsedItemID.Parse("456");
            assertTrue(id.getServerIDString() == null);
            assertTrue(id.getMailboxIDString() == null);
            assertTrue(id.getMailboxIDInt() == -1);
            assertTrue(id.getItemIDString().equals("456"));
            assertTrue(id.getItemIDInt() == 456);
        }
    }
    
    {
        boolean OK = false; 
        try {
            ParsedItemID id = ParsedItemID.Parse("/SERVER/456");
            System.out.println("ERROR - parsed /SERVER/456 w/o error!");
        } catch(IllegalArgumentException e) {
            OK = true;
        } catch(ServiceException e) {
            fail("ServiceException "+e);
        }
        assertTrue(OK);
    }
    
    {
        boolean OK = false; 
        try {
            ParsedItemID id = ParsedItemID.Parse("//SERVER/456");
            System.out.println("ERROR - parsed //SERVER/456 w/o error!");
        } catch(IllegalArgumentException e) {
            OK = true;
        } catch(ServiceException e) {
            fail("ServiceException "+e);
        }
        assertTrue(OK);
    }
    
    {
        boolean OK = false; 
        try {
            ParsedItemID id = ParsedItemID.Parse("SERVER/852/456");
            System.out.println("ERROR - parsed SERVER/852/456 w/o error!");
        } catch(IllegalArgumentException e) {
            OK = true;
        } catch(ServiceException e) {
            fail("ServiceException "+e);
        }
        assertTrue(OK);
    }
}
