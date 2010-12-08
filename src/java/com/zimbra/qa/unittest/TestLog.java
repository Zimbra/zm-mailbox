/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.common.util.AccountLogger;
import com.zimbra.common.util.Log.Level;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class TestLog
extends TestCase {

    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    public void testAccountLoggers()
    throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        Account user1 = TestUtil.getAccount("user1");
        Account user2 = TestUtil.getAccount("user2");
        Account user3 = TestUtil.getAccount("user3");
        Account user4 = TestUtil.getAccount("user4");

        // Add loggers.
        List<AccountLogger> loggers = prov.addAccountLogger(user1, "zimbra.filter", "debug", null);
        assertLoggerExists(loggers, user1, "zimbra.filter", Level.debug);
        loggers = prov.addAccountLogger(user2, "zimbra.backup", "info", null);
        assertLoggerExists(loggers, user2, "zimbra.backup", Level.info);
        loggers = prov.addAccountLogger(user3, "zimbra.sync", "warn", null);
        assertLoggerExists(loggers, user3, "zimbra.sync", Level.warn);
        loggers = prov.addAccountLogger(user3, "zimbra.lmtp", "warn", null);
        assertLoggerExists(loggers, user3, "zimbra.lmtp", Level.warn);
        assertEquals(1, loggers.size());
        loggers = prov.addAccountLogger(user4, "zimbra.lmtp", "error", null);
        assertLoggerExists(loggers, user4, "zimbra.lmtp", Level.error);
        
        // Verify <GetAccountLoggersRequest>.
        loggers = prov.getAccountLoggers(user1, null);
        assertEquals(1, loggers.size());
        assertLoggerExists(loggers, user1, "zimbra.filter", Level.debug);
        
        loggers = prov.getAccountLoggers(user2, null);
        assertEquals(1, loggers.size());
        assertLoggerExists(loggers, user2, "zimbra.backup", Level.info);
        
        loggers = prov.getAccountLoggers(user3, null);
        assertEquals(2, loggers.size());
        assertLoggerExists(loggers, user3, "zimbra.sync", Level.warn);
        assertLoggerExists(loggers, user3, "zimbra.lmtp", Level.warn);
        
        loggers = prov.getAccountLoggers(user4, null);
        assertEquals(1, loggers.size());
        assertLoggerExists(loggers, user4, "zimbra.lmtp", Level.error);
        
        // Remove loggers for everyone except user3.
        prov.removeAccountLoggers(user1, "zimbra.filter", null);
        prov.removeAccountLoggers(user2, null, null);
        prov.removeAccountLoggers(user4, null, null);
        
        // Test <GetAllAccountLoggersRequest>.
        Map<String, List<AccountLogger>> map = prov.getAllAccountLoggers(null);
        assertEquals(1, map.size());
        loggers = map.get(user3.getName());
        assertEquals(2, loggers.size());
        assertLoggerExists(loggers, user3, "zimbra.sync", Level.warn);
        assertLoggerExists(loggers, user3, "zimbra.lmtp", Level.warn);
    }
    
    /**
     * Confirms that account loggers are added for all categories when the
     * category name is set to "all" (bug 29715).
     */
    public void testAllCategories()
    throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        Account account = TestUtil.getAccount("user1");
        assertEquals(0, prov.getAccountLoggers(account, null).size());
        List<AccountLogger> loggers = prov.addAccountLogger(account, "all", "debug", null);
        assertTrue(loggers.size() > 1);
        
        // Make sure the zimbra.soap category was affected.
        assertLoggerExists(loggers, account, "zimbra.soap", Level.debug);
        loggers = prov.getAccountLoggers(account, null);
        assertLoggerExists(loggers, account, "zimbra.soap", Level.debug);
    }
    
    /**
     * Tests removing all account loggers for a given account.
     */
    public void testRemoveAll()
    throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        Account user1 = TestUtil.getAccount("user1");
        Account user2 = TestUtil.getAccount("user2");
        
        prov.addAccountLogger(user1, "zimbra.soap", "debug", null);
        prov.addAccountLogger(user1, "zimbra.sync", "debug", null);
        prov.addAccountLogger(user2, "zimbra.soap", "debug", null);
        prov.addAccountLogger(user2, "zimbra.sync", "debug", null);
        
        // Test removing loggers with no category specified.
        List<AccountLogger> loggers = prov.getAccountLoggers(user1, null);
        assertEquals(2, loggers.size());
        prov.removeAccountLoggers(user1, null, null);
        loggers = prov.getAccountLoggers(user1, null);
        assertEquals(0, loggers.size());
        
        // Test removing loggers with category "all".
        loggers = prov.getAccountLoggers(user2, null);
        assertEquals(2, loggers.size());
        prov.removeAccountLoggers(user2, "all", null);
        loggers = prov.getAccountLoggers(user2, null);
        assertEquals(0, loggers.size());
    }
    
    private void assertLoggerExists(Iterable<AccountLogger> loggers, Account account, String category, Level level) {
        for (AccountLogger logger : loggers) {
            if (logger.getAccountName().equals(account.getName()) && logger.getCategory().equals(category)) {
                assertEquals(level, logger.getLevel());
                return;
            }
        }
        fail("Could not find logger for account " + account.getName() + ", category " + category);
    }

    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.newSoapProvisioning().removeAccountLoggers(null, null, null);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestLog.class);
    }
}
