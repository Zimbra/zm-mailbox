package com.liquidsys.qa.unittest;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.ldap.LdapUtil;
import com.liquidsys.coco.client.LmcSession;
import com.liquidsys.coco.client.soap.LmcGetPrefsRequest;
import com.liquidsys.coco.client.soap.LmcGetPrefsResponse;
import com.liquidsys.coco.client.soap.LmcModifyPrefsRequest;
import com.liquidsys.coco.db.DbOutOfOffice;
import com.liquidsys.coco.db.DbPool;
import com.liquidsys.coco.db.DbPool.Connection;
import com.liquidsys.coco.mailbox.Mailbox;

/**
 * @author bburtin
 */
public class TestOutOfOffice extends TestCase
{
    private Connection mConn;
    private Mailbox mMbox;
    
    private static String SOAP_URL = TestUtil.getSoapUrl();
    private static String USER_NAME = "user1";
    private static String RECIPIENT1_ADDRESS = "TestOutOfOffice1@liquidsys.com";
    private static String RECIPIENT2_ADDRESS = "TestOutOfOffice2@liquidsys.com";

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
        String keyEnabled = Provisioning.A_liquidPrefOutOfOfficeReplyEnabled;
        String keyReply = Provisioning.A_liquidPrefOutOfOfficeReply;

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
        final String keyEnabled = Provisioning.A_liquidPrefOutOfOfficeReplyEnabled;
        final String keyReply = Provisioning.A_liquidPrefOutOfOfficeReply;
        
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
