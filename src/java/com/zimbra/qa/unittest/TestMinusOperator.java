/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;

public class TestMinusOperator extends TestCase {

    private static final String USER_NAME = "testuser123";
    private static final String REMOTE_USER_NAME = "testuser456";
    private static ZMailbox mbox;

    @Override
    @BeforeClass
    public void setUp() throws ServiceException{
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        ZOutgoingMessage msg = TestUtil.getOutgoingMessage(REMOTE_USER_NAME, "test message", "far over the misty mountains cold",null);
        mbox.sendMessage(msg,null,false);
    }

    @Override
    @AfterClass
    public void tearDown() throws ServiceException {
        TestUtil.deleteAccount(USER_NAME);
        TestUtil.deleteAccount(REMOTE_USER_NAME);
    }

    @Test
    public void testExcludeText() throws ServiceException {
        ZSearchResult search1 = mbox.search(new ZSearchParams("in:sent test"));
        assertEquals(search1.getHits().size(),1); //control
        ZSearchResult search2 = mbox.search(new ZSearchParams("in:sent -test"));
        assertEquals(search2.getHits().size(),0);
    }

    @Test
    public void testExcludeRecipient() throws ServiceException {
        ZSearchResult search1 = mbox.search(new ZSearchParams("in:sent to:"+REMOTE_USER_NAME));
        assertEquals(search1.getHits().size(),1);
        ZSearchResult search2 = mbox.search(new ZSearchParams("in:sent -to:"+REMOTE_USER_NAME));
        assertEquals(search2.getHits().size(),0);
    }

}
