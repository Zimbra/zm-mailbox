package com.zimbra.cs.index.query;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.qa.unittest.TestUtil;

public class HostTokenTest {
    static Account account;
    static Mailbox mbox;
    static Integer msgId;

    @BeforeClass
    public static void init() throws Exception {
    	MailboxTestUtil.initServer();
    	MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();
        account = prov.createAccount("hosttest@zimbra.com", "secret", new HashMap<String, Object>());
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        ParsedMessage pm = new ParsedMessage(("Subject: www.foo.bar.com").getBytes(), false);
        Message msg = TestUtil.addMessage(mbox, pm);
        msgId = msg.getId();
        mbox.index.indexDeferredItems();
    }

    private void runTest(String query, boolean shouldMatch) throws ServiceException {
    	List<Integer> ids = TestUtil.search(mbox, query, Type.MESSAGE);
        if (shouldMatch) {
        	assertTrue(ids.size() == 1 && ids.contains(msgId));
        } else {
        	assertTrue(ids.size() == 0);
        }
    }

    @Test
    public void searchHostSubstrings() throws ServiceException {
        runTest("www.foo.bar.com", true);
        runTest("foo.bar.com", true);
        runTest("bar.com", true);
        runTest("com", false);
        runTest("foo.bar", false);
    }
}
