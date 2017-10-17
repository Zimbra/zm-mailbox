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

    public void setUp()
    throws Exception {
        cleanUp();
    }

    public void cleanUp()
    throws Exception {
        TestUtil.newSoapProvisioning().removeAccountLoggers(null, null, null);
    }

    @Test
    public void accountLoggers()
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
    @Test
    public void allCategories()
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
    @Test
    public void removeAll()
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

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestLog.class);
    }
}
