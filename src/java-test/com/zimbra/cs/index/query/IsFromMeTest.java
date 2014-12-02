package com.zimbra.cs.index.query;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
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

public class IsFromMeTest {
    Account account;
    Mailbox mbox;
    Integer msgId;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("isfrommme@test.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        ParsedMessage pm = new ParsedMessage(("From: isfrommme@test.com\n"
                + "Subject: test message").getBytes(), false);
        Message msg = TestUtil.addMessage(mbox, pm);
        msgId = msg.getId();

        //add another message with a similar sender, to make sure it doesn't get picked up
        pm = new ParsedMessage(("From: isfrommme.2@test.com\n"
                + "Subject: test message").getBytes(), false);
        TestUtil.addMessage(mbox, pm);
    }

    @Test
    public void doTest() throws ServiceException {
        List<Integer> ids = TestUtil.search(mbox, "is:fromme", Type.MESSAGE);
        assertTrue(ids.size() == 1 && ids.contains(msgId));
    }
}
