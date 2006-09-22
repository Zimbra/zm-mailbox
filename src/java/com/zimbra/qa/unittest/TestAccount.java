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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcDeleteAccountRequest;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.util.ZimbraLog;

import junit.framework.TestCase;


/**
 * @author bburtin
 */
public class TestAccount extends TestCase {

    private static String USER_NAME = "TestAccount";
    // xxx move this to TestUtil
    private static String PASSWORD = "test123";

    public void setUp()
    throws Exception {
        cleanUp();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraMailHost", LC.zimbra_server_hostname.value());
        attrs.put("cn", "TestAccount");
        attrs.put("displayName", "TestAccount unit test user");
        Provisioning.getInstance().createAccount(TestUtil.getAddress(USER_NAME), PASSWORD, attrs);
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    public void testDeleteAccount()
    throws Exception {
        ZimbraLog.test.debug("testDeleteAccount()");
        
        // Get the account and mailbox
        Account account = TestUtil.getAccount(USER_NAME);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        String dbName = DbMailbox.getDatabaseName(mbox);
        ZimbraLog.test.debug("Account=" + account.getId() + ", mbox=" + mbox.getId());
        
        // Confirm that the mailbox database exists
        DbResults results = DbUtil.executeQuery(
            "SELECT COUNT(*) FROM mailbox WHERE id = " + mbox.getId());
        assertEquals("Could not find row in mailbox table", 1, results.getInt(1));
        
        results = DbUtil.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");
        assertEquals("Could not find mailbox database", 1, results.size());
        
        // Delete the account
        LmcSession session = TestUtil.getAdminSoapSession();
        LmcDeleteAccountRequest req = new LmcDeleteAccountRequest(account.getId());
        req.setSession(session);
        req.invoke(TestUtil.getAdminSoapUrl());
        
        // Confirm that the mailbox was deleted
        results = DbUtil.executeQuery(
            "SELECT COUNT(*) FROM mailbox WHERE id = " + mbox.getId());
        assertEquals("Unexpected row in mailbox table", 0, results.getInt(1));
    }
    
    private void cleanUp()
    throws Exception {
        Provisioning prov = Provisioning.getInstance(); 
        Account account = prov.get(AccountBy.name, TestUtil.getAddress(USER_NAME));
        if (account != null) {
            prov.deleteAccount(account.getId());
        }
    }
}
