/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 29, 2005
 *
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.service.ServiceException;

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
