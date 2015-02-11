package com.zimbra.cs.index.query;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.qa.unittest.TestUtil;

public class HostTokenTest {

    Mailbox mbox;
    
    @BeforeClass
    public static void init() throws Exception {
    	MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("hosttest@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        mbox = null;
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
    }

    private void runTest(String query, boolean shouldMatch, Integer msgId) throws ServiceException {
    	List<Integer> ids = TestUtil.search(mbox, query, Type.MESSAGE);
        if (shouldMatch) {
        	assertTrue(ids.size() == 1 && ids.contains(msgId));
        } else {
        	assertTrue(ids.size() == 0);
        }
    }

    @Test
    public void searchHostSubstrings() throws Exception {
        ParsedMessage pm = new ParsedMessage(("Subject: www.foo.bar.com").getBytes(), false);
        Integer msgId = TestUtil.addMessage(mbox, pm).getId();
        
        runTest("www.foo.bar.com", true, msgId);
        runTest("foo.bar.com", true, msgId);
        runTest("bar.com", true, msgId);
        runTest("com", false, msgId);
        runTest("foo.bar", false, msgId);
    }
}
