package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;

public class TestSearchHeaders extends TestCase {
    private ZMailbox mbox;
    private String USER_NAME = "user1";
    String id;

    @Override
    public void setUp() throws Exception {
        mbox = TestUtil.getZMailbox(USER_NAME);
        id = mbox.addMessage("2",null, null, System.currentTimeMillis(), getMimeString(), false);
    }

    private String getMimeString() {
        return "Subject: test\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/alternative; \n" +
                "    boundary=\"=_43a9c340-deb5-4ad3-a0a6-809c5d444d94\"\n" +
                "\n" +
                "--=_43a9c340-deb5-4ad3-a0a6-809c5d444d94\n" +
                "Content-Type: text/plain; charset=utf-8\n" +
                "Content-Transfer-Encoding: 8bit\n" +
                "\n" +
                "header search test\n" +
                "\n" +
                "--=_43a9c340-deb5-4ad3-a0a6-809c5d444d94--";
    }

    @Test
    public void testSearchNonTopLevelHeaders() throws ServiceException {
        ZSearchParams params = new ZSearchParams("\"header search test\" #Content-Transfer-Encoding:8bit");
        params.setTypes("message");
        ZSearchResult result = mbox.search(params);
        boolean found = false;
        for (ZSearchHit hit: result.getHits()) {
            if (found == false && hit.getId().equals(id)) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Override
    public void tearDown() throws Exception {
        mbox.deleteMessage(id);
    }
}
