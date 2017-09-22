package com.zimbra.cs.index.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.event.Event;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

public class InMemoryEventLoggerTest {
    private static Provisioning prov;
    private static Account acct;
    private static Mailbox mbox;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        acct = prov.createAccount("test@zimbra.com", "test123", new HashMap<>());
        mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
    }

    @Test
    public void testLogging() {
        Event event = new Event(mbox.getAccountId(), Event.EventType.SENT, System.currentTimeMillis(), "test@zimbra.com", "test1@zimbra.com", Collections.EMPTY_MAP);
        InMemoryEventLogger inMemoryEventLogger = new InMemoryEventLogger();
        inMemoryEventLogger.log(event);
        Assert.assertNotNull(inMemoryEventLogger.getLogs());
        Assert.assertEquals(1, inMemoryEventLogger.getLogs().size());
    }
}
