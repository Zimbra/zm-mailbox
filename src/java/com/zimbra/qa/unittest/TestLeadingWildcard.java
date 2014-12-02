package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGrant;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMountpoint;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;

public class TestLeadingWildcard extends TestCase {
    private static final String USER_NAME = "wildcardtest";
    private static ZMailbox mbox;
    ZSearchParams params;
    ZSearchResult results;
    
    @Override
    @Before
    public void setUp() throws Exception {
        TestUtil.createAccount(USER_NAME);
        mbox = TestUtil.getZMailbox(USER_NAME);
        String message = new MessageBuilder().withBody("dungeons deep and caverns old").
                withSubject("misty mountains").withToRecipient("bilbo@shire.com").create();
        mbox.addMessage(ZFolder.ID_INBOX, null, null, 0, message, true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
    }
    
    @Test
    public void testSearch() throws ServiceException {
        params = new ZSearchParams("*ngeons");
        results = mbox.search(params);
        assertEquals(1, results.getHits().size());
    }
    
    @Test
    public void testSearchSubject() throws ServiceException {
        params = new ZSearchParams("subject:*isty");
        results = mbox.search(params);
        assertEquals(1, results.getHits().size());
    }
    
    @Test
    public void testSearchTo() throws ServiceException {
        params = new ZSearchParams("to:*ilbo");
        results = mbox.search(params);
        assertEquals(1, results.getHits().size());
    }
}
