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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcGetPrefsRequest;
import com.zimbra.cs.client.soap.LmcGetPrefsResponse;
import com.zimbra.cs.client.soap.LmcModifyPrefsRequest;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author bburtin
 */
public class TestOutOfOffice extends TestCase
{
    private Connection mConn;
    private Mailbox mMbox;
    
    private static String SOAP_URL = TestUtil.getSoapUrl();
    private static String USER_NAME = "user1";
    private static String RECIPIENT1_ADDRESS = "TestOutOfOffice1@example.zimbra.com";
    private static String RECIPIENT2_ADDRESS = "TestOutOfOffice2@example.zimbra.com";

    protected void setUp() throws Exception
    {
        super.setUp();
        
        Account account = TestUtil.getAccount(USER_NAME);
        mMbox = Mailbox.getMailboxByAccount(account);
        mConn = DbPool.getConnection();

        DbOutOfOffice.clear(mConn, mMbox);
        mConn.commit();
}
    
    public void testRowExists() throws Exception
    {
        long fiveDaysAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 5);

        DbOutOfOffice.setSentTime(mConn, mMbox, RECIPIENT1_ADDRESS, fiveDaysAgo);
        mConn.commit();
        assertFalse("1 day", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 1));
        assertFalse("4 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 4));
        assertFalse("5 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 5));
        assertTrue("6 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 6));
        assertTrue("100 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 100));
    }

    public void testRowDoesntExist() throws Exception
    {
        assertFalse("1 day", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 1));
        assertFalse("5 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 5));
        assertFalse("100 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 100));
    }
    
    public void testPrune() throws Exception
    {
        long fiveDaysAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 5);
        long sixDaysAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 6);

        DbOutOfOffice.setSentTime(mConn, mMbox, RECIPIENT1_ADDRESS, fiveDaysAgo);
        DbOutOfOffice.setSentTime(mConn, mMbox, RECIPIENT2_ADDRESS, sixDaysAgo);
        mConn.commit();
        
        // Prune the entry for 6 days ago
        DbOutOfOffice.prune(mConn, 6);
        mConn.commit();
        
        // Make sure that the later entry is still there and the earlier one is gone 
        assertTrue("recipient1", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 6));
        assertFalse("recipient2", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT2_ADDRESS, 7));
    }

    public void testGetOutOfOffice() throws Exception
    {
        String keyEnabled = Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled;
        String keyReply = Provisioning.A_zimbraPrefOutOfOfficeReply;

        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcGetPrefsRequest req = new LmcGetPrefsRequest();
        String[] prefsToGet = { keyEnabled, keyReply };
        req.setPrefsToGet(prefsToGet);
        req.setSession(session);
        LmcGetPrefsResponse response = (LmcGetPrefsResponse)req.invoke(SOAP_URL);
        

        // Get current settings
        Account account = TestUtil.getAccount(USER_NAME);
        boolean isCurrentlyEnabled =
            account.getBooleanAttr(keyEnabled, false);
        String currentReplyBody = account.getAttr(keyReply);
        
        // Get SOAP response settings
        Map prefs = response.getPrefsMap();
        boolean isEnabled;
        if (prefs.containsKey(keyEnabled)) {
            String value = (String) prefs.get(keyEnabled);
            isEnabled = (value.equals(LdapUtil.LDAP_TRUE));
        }
        else {
            // If the pref isn't set in the response, make sure the test passes 
            isEnabled = isCurrentlyEnabled;
        }
        String replyBody = (String) prefs.get(keyReply);

        // Normalize nulls so we don't get false positives
        if (currentReplyBody == null) {
            currentReplyBody = "";
        }
        if (replyBody == null) {
            replyBody = "";
        }
        
        assertEquals(isCurrentlyEnabled, isEnabled);
        assertEquals("currentReplyBody='" + currentReplyBody +"', replyBody='" + replyBody + "'",
                     currentReplyBody, replyBody);
    }
    
    public void testModifyOutOfOffice() throws Exception
    {
        final String keyEnabled = Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled;
        final String keyReply = Provisioning.A_zimbraPrefOutOfOfficeReply;
        
        // Get current settings
        Account account = TestUtil.getAccount(USER_NAME);
        boolean wasEnabled =
            account.getBooleanAttr(keyEnabled, false);
        String oldReplyBody = account.getAttr(keyReply);
        
        // Set new settings
        boolean isEnabled = !wasEnabled;
        StringBuffer buf = new StringBuffer();
        int numWords = (new Random()).nextInt(1000) + 1; 
        for (int i = 0; i < numWords; i++) {
            buf.append("blah ");
        }
        String replyBody = buf.toString();

        // Send the request
        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcModifyPrefsRequest req = new LmcModifyPrefsRequest();
        Map prefs = new HashMap();
        prefs.put(keyEnabled, LdapUtil.getBooleanString(isEnabled));
        prefs.put(keyReply, replyBody);
        req.setPrefMods(prefs);
        req.setSession(session);
        req.invoke(SOAP_URL);

        // Refresh Account object
        account = TestUtil.getAccount(USER_NAME);
        
        // Validate
        assertNotNull(account.getAttr(keyEnabled));
        assertEquals(isEnabled, account.getBooleanAttr(keyEnabled, wasEnabled));
        assertEquals(replyBody, account.getAttr(keyReply));

        // Reset, so that the account attributes are not modified
        prefs = new HashMap();
        prefs.put(keyEnabled, LdapUtil.getBooleanString(wasEnabled));
        prefs.put(keyReply, oldReplyBody);
        account.modifyAttrs(prefs);
    }
    
    protected void tearDown() throws Exception
    {
        DbOutOfOffice.clear(mConn, mMbox);
        mConn.commit();
        
        DbPool.quietClose(mConn);
        super.tearDown();
    }
    
}
