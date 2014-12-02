package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;

/**
 * Test case for bug 79105
 * @author iraykin
 *
 */
public class TestSearchURL {
    private String USER_NAME = "testsearchurl";
    private ZMailbox mbox;

    @Before
    public void setUp() throws Exception {
        TestUtil.createAccount(USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        String url = "https://reviewboard-mvp.eng.example.com/r/9588/";
        String msg = new MessageBuilder().withBody(url).withSubject("url test").create();
        TestUtil.addMessage(mbox, msg, ZFolder.ID_INBOX);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
    }

    @Test
    public void test() throws ServiceException {
        ZSearchParams params = new ZSearchParams("9588");
        ZSearchResult result = mbox.search(params);
        assertEquals(1, result.getHits().size());
    }
}
