package com.zimbra.cs.index.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.qa.unittest.TestUtil;

public class FromOrSenderQueryTest {
    Account account;
    Mailbox mbox;
    Integer msgId;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("fromorsendertest", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        ParsedMessage pm = new ParsedMessage(("From: abc@zimbra.com\n"
                + "Sender: xyz@zimbra.com\n"
                + "To: fromorsendertest@zimbra.com\n"
                + "Subject: fromorsender test").getBytes(), false);
        Message msg = TestUtil.addMessage(mbox, pm);
        msgId = msg.getId();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void doTest() throws ServiceException {
        //from:abc should return the message
        List<Integer> ids = TestUtil.search(mbox, "from:abc", Type.MESSAGE);
        assertTrue(ids.size() == 1 && ids.contains(msgId));
        //from:xyz search shouldn't return the message
        ids = TestUtil.search(mbox, "from:xyz", Type.MESSAGE);
        assertEquals(0, ids.size());
        //but fromorsender:xyz should
        ids = TestUtil.search(mbox, "fromorsender:xyz", Type.MESSAGE);
        assertTrue(ids.size() == 1 && ids.contains(msgId));
    }
}
