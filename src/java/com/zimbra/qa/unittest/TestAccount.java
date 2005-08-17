package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.client.LmcSession;
import com.liquidsys.coco.client.soap.LmcDeleteAccountRequest;
import com.liquidsys.coco.db.DbMailbox;
import com.liquidsys.coco.db.DbResults;
import com.liquidsys.coco.db.DbUtil;
import com.liquidsys.coco.localconfig.LC;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.util.LiquidLog;

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
        
        Map attrs = new HashMap();
        attrs.put("liquidMailHost", LC.liquid_server_hostname.value());
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
        LiquidLog.test.debug("testDeleteAccount()");
        
        // Get the account and mailbox
        Account account = TestUtil.getAccount(USER_NAME);
        Mailbox mbox = Mailbox.getMailboxByAccount(account);
        String dbName = DbMailbox.getDatabaseName(mbox.getId());
        LiquidLog.test.debug("Account=" + account.getId() + ", mbox=" + mbox.getId());
        
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
        
        results = DbUtil.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");
        assertEquals("Found mailbox database", 0, results.size());
    }
    
    private void cleanUp()
    throws Exception {
        Provisioning prov = Provisioning.getInstance(); 
        Account account = prov.getAccountByName(TestUtil.getAddress(USER_NAME));
        if (account != null) {
            prov.deleteAccount(account.getId());
        }
    }
}
