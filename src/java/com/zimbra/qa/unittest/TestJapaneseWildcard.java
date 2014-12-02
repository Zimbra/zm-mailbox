package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.service.ServiceException;

/* Test case for bug 81179 */
public class TestJapaneseWildcard {
    private static final String USER_NAME = "japanesewildcard";
    private static ZMailbox mbox;
    private static List<String> ids = new ArrayList<String>();

    @BeforeClass
    public static void setUp() throws ServiceException, IOException, MessagingException {
        TestUtil.createAccount(USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        ids.add(TestUtil.addMessage(mbox, "\u30C6" + "\u30B9" + "\u30C8" + "001"));
        ids.add(TestUtil.addMessage(mbox, "\u30C6" + "\u30B9" + "\u30C8" + "0011"));
        ids.add(TestUtil.addMessage(mbox, "\u30C6" + "\u30C6" + "\u30B9" + "\u30C8" + "001"));
        ids.add(TestUtil.addMessage(mbox, "\u30C6" + "\u30B9" + "\u30C8" + "\u756A" + "\u53F7" + "001"));
    }

    @AfterClass
    public static void tearDown() throws ServiceException {
        TestUtil.deleteAccount(USER_NAME);
    }

    @Test
    public void testRegularSearch() throws ServiceException {
        List<ZMessage> msgs = TestUtil.search(mbox, "\u30C6" + "\u30B9" + "\u30C8" + "001");
        assertEquals(2, msgs.size());
        for (ZMessage msg: msgs) {
            int idx = ids.indexOf(msg.getId());
            assertTrue(idx == 0 || idx == 2);
        }
    }

    @Test
    public void testWildcard() throws ServiceException {
        List<ZMessage> msgs = TestUtil.search(mbox, "\u30C6" + "\u30B9" + "\u30C8" + "001*");
        assertEquals(3, msgs.size());
        for (ZMessage msg: msgs) {
            int idx = ids.indexOf(msg.getId());
            assertTrue(idx < 3);
        }
    }
}
