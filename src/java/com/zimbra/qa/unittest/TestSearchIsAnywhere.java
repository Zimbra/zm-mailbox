package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.service.ServiceException;

/* Test case for bug 82489 */
public class TestSearchIsAnywhere {
    private static final String USER_NAME = "isanywheretest1";
    private static final String REMOTE_USER_NAME = "isanywheretest2";
    private static ZMailbox mbox;
    private static ZMailbox remote_mbox;
    private static String subject = "messagetodelete";

    @Before
    public void setUp() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        remote_mbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.sendMessage(remote_mbox, USER_NAME, subject);
        Thread.sleep(1000);
        ZMessage msg = TestUtil.search(mbox, subject).get(0);
        msg.move(ZFolder.ID_TRASH);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
        TestUtil.deleteAccount(REMOTE_USER_NAME);
    }
    @Test
    public void test() throws ServiceException {
        String query = new StringBuilder(subject)
        .append(" is:anywhere")
        .append(" from:").append(REMOTE_USER_NAME).toString();
        List<ZMessage> msgs = TestUtil.search(mbox, query);
        assertEquals(1, msgs.size());
    }
}
