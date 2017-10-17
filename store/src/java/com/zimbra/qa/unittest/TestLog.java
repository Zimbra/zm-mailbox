/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016, 2017 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.common.util.AccountLogger;
import com.zimbra.common.util.Log.Level;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class TestLog {

    @Rule
    public TestName testInfo = new TestName();
    private String USER_1 = null;
    private String USER_2 = null;
    private String USER_3 = null;
    private String USER_4 = null;

    @Before
    public void setUp()
    throws Exception {
        String prefix = this.getClass().getName() + "-" + testInfo.getMethodName() + "-";
        USER_1 = prefix + "1";
        USER_2 = prefix + "2";
        USER_3 = prefix + "3";
        USER_4 = prefix + "4";
        cleanUp();
    }

    @After
    public void cleanUp()
    throws Exception {
        TestUtil.newSoapProvisioning().removeAccountLoggers(null, null, null);
        TestUtil.deleteAccountIfExists(USER_1);
        TestUtil.deleteAccountIfExists(USER_2);
        TestUtil.deleteAccountIfExists(USER_3);
        TestUtil.deleteAccountIfExists(USER_4);
    }

    @Test
    public void accountLoggers()
    throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        Account user1 = TestUtil.createAccount(USER_1);
        Account user2 = TestUtil.createAccount(USER_2);
        Account user3 = TestUtil.createAccount(USER_3);
        Account user4 = TestUtil.createAccount(USER_4);

        // Add loggers.
        List<AccountLogger> loggers = prov.addAccountLogger(user1, "zimbra.filter", "debug", null);
        assertLoggerExists(loggers, user1, "zimbra.filter", Level.debug);
        loggers = prov.addAccountLogger(user2, "zimbra.backup", "info", null);
        assertLoggerExists(loggers, user2, "zimbra.backup", Level.info);
        loggers = prov.addAccountLogger(user3, "zimbra.sync", "warn", null);
        assertLoggerExists(loggers, user3, "zimbra.sync", Level.warn);
        loggers = prov.addAccountLogger(user3, "zimbra.lmtp", "warn", null);
        assertLoggerExists(loggers, user3, "zimbra.lmtp", Level.warn);
        assertEquals(String.format("Number of loggers for acct=%s", USER_3), 1, loggers.size());
        loggers = prov.addAccountLogger(user4, "zimbra.lmtp", "error", null);
        assertLoggerExists(loggers, user4, "zimbra.lmtp", Level.error);

        // Verify <GetAccountLoggersRequest>.
        loggers = prov.getAccountLoggers(user1, null);
        assertEquals(String.format("Number of loggers for acct=%s", USER_1), 1, loggers.size());
        assertLoggerExists(loggers, user1, "zimbra.filter", Level.debug);

        loggers = prov.getAccountLoggers(user2, null);
        assertEquals(String.format("Number of loggers for acct=%s", USER_2), 1, loggers.size());
        assertLoggerExists(loggers, user2, "zimbra.backup", Level.info);

        loggers = prov.getAccountLoggers(user3, null);
        assertEquals(String.format("Number of loggers for acct=%s", USER_3), 2, loggers.size());
        assertLoggerExists(loggers, user3, "zimbra.sync", Level.warn);
        assertLoggerExists(loggers, user3, "zimbra.lmtp", Level.warn);

        loggers = prov.getAccountLoggers(user4, null);
        assertEquals(String.format("Number of loggers for acct=%s", USER_4), 1, loggers.size());
        assertLoggerExists(loggers, user4, "zimbra.lmtp", Level.error);

        // Remove loggers for everyone except user3.
        prov.removeAccountLoggers(user1, "zimbra.filter", null);
        prov.removeAccountLoggers(user2, null, null);
        prov.removeAccountLoggers(user4, null, null);

        // Test <GetAllAccountLoggersRequest>.
        Map<String, List<AccountLogger>> map = prov.getAllAccountLoggers(null);
        assertEquals("Size of map from getAllAccountLoggers", 1, map.size());
        loggers = map.get(user3.getName());
        assertEquals(String.format("Number of loggers for acct=%s", USER_3), 2, loggers.size());
        assertLoggerExists(loggers, user3, "zimbra.sync", Level.warn);
        assertLoggerExists(loggers, user3, "zimbra.lmtp", Level.warn);
    }

    /**
     * Confirms that account loggers are added for all categories when the
     * category name is set to "all" (bug 29715).
     */
    @Test
    public void allCategories()
    throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        Account account = TestUtil.createAccount(USER_1);
        assertEquals(String.format(
                "Number of loggers for acct=%s after acct creation", USER_1),
                0, prov.getAccountLoggers(account, null).size());
        List<AccountLogger> loggers = prov.addAccountLogger(account, "all", "debug", null);
        assertTrue(String.format(
                "Number of loggers (%s) for acct=%s at debug level should be greater than 1",
                loggers.size(), USER_1), loggers.size() > 1);

        // Make sure the zimbra.soap category was affected.
        assertLoggerExists(loggers, account, "zimbra.soap", Level.debug);
        loggers = prov.getAccountLoggers(account, null);
        assertLoggerExists(loggers, account, "zimbra.soap", Level.debug);
    }

    /**
     * Tests removing all account loggers for a given account.
     */
    @Test
    public void removeAll()
    throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        Account user1 = TestUtil.createAccount(USER_1);
        Account user2 = TestUtil.createAccount(USER_2);

        prov.addAccountLogger(user1, "zimbra.soap", "debug", null);
        prov.addAccountLogger(user1, "zimbra.sync", "debug", null);
        prov.addAccountLogger(user2, "zimbra.soap", "debug", null);
        prov.addAccountLogger(user2, "zimbra.sync", "debug", null);

        // Test removing loggers with no category specified.
        List<AccountLogger> loggers = prov.getAccountLoggers(user1, null);
        assertEquals(String.format("Number of loggers for acct=%s", USER_1), 2, loggers.size());
        prov.removeAccountLoggers(user1, null, null);
        loggers = prov.getAccountLoggers(user1, null);
        assertEquals(String.format(
                "Number of loggers for acct=%s after remove with null category/null svr", USER_1),
                0, loggers.size());

        // Test removing loggers with category "all".
        loggers = prov.getAccountLoggers(user2, null);
        assertEquals(String.format("Number of loggers for acct=%s", USER_2), 2, loggers.size());
        prov.removeAccountLoggers(user2, "all", null);
        loggers = prov.getAccountLoggers(user2, null);
        assertEquals(String.format(
                "Number of loggers for acct=%s after remove with category='all'/null svr", USER_2),
                0, loggers.size());
    }

    private void assertLoggerExists(Iterable<AccountLogger> loggers, Account account, String category, Level level) {
        for (AccountLogger logger : loggers) {
            if (logger.getAccountName().equals(account.getName()) && logger.getCategory().equals(category)) {
                assertEquals(String.format(
                        "Log Level for account=%s category=%s", account.getName(), category),
                        level, logger.getLevel());
                return;
            }
        }
        fail("Could not find logger for account " + account.getName() + ", category " + category);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestLog.class);
    }
}
