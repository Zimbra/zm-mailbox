package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZInvite;
import com.zimbra.client.ZInvite.ZComponent;
import com.zimbra.client.ZInvite.ZOrganizer;
import com.zimbra.client.ZInvite.ZStatus;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZAppointmentResult;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.type.SearchSortBy;


public class TestSearchTask {
    private static String USER_NAME = "user1";
    private static ZMailbox mbox;
    private static ArrayList<String> ids = new ArrayList<String>();
    private static final int numTasks = 60;
    private static final int limit = 50;

    @BeforeClass
    public static void populate() throws ServiceException{
        mbox = TestUtil.getZMailbox(USER_NAME);
        for (int i = 0; i < numTasks; i++) {
            ZOutgoingMessage msg = TestUtil.getOutgoingMessage(USER_NAME, null, String.format("task body %d",i), null);
            ZInvite inv = new ZInvite();
            ZInvite.ZComponent comp = new ZComponent();
            comp.setIsAllDay(true);
            comp.setStatus(ZStatus.NEED);
            comp.setLocation("mount erebor");
            comp.setPercentCompleted("0");
            comp.setPriority("0");
            comp.setName(String.format("destroy the one ring %d",i));
            comp.setOrganizer(new ZOrganizer(mbox.getName()));

            inv.getComponents().add(comp);
            ZAppointmentResult resp = mbox.createTask("15", null, msg, inv, null);
            ids.add(resp.getInviteId());
        }
    }

    @Test
    public void testTaskSearch() throws Exception {
        boolean hasMore;
        int offset = 0;

        do {
            ZSearchParams params = new ZSearchParams("in:tasks");
            params.setTypes("task");
            params.setOffset(offset);
            params.setLimit(limit);
            params.setSortBy(SearchSortBy.taskDueAsc);
            ZSearchResult resp = mbox.search(params);
            int numResults = resp.getHits().size();
            hasMore = resp.hasMore();
            assertEquals(Math.min(limit, numTasks - offset), numResults);
            offset += numResults;
            }
        while (hasMore == true);
    }

    @AfterClass
    public static void destroy() throws ServiceException {
        for (String id: ids) {
            mbox.cancelTask(id, "0", null, null, null, null);
        }
    }
}
