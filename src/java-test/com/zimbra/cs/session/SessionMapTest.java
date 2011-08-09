/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link SessionMap}.
 *
 * @author ysasaki
 */
public final class SessionMapTest {

    @Test
    public void test() throws Exception {
        SessionMap map = new SessionMap(Session.Type.NULL);

        Session s1 = new AdminSession("a1").testSetSessionId("s1");
        Session s12 = new AdminSession("a1").testSetSessionId("s2");
        Session s2 = new AdminSession("a2").testSetSessionId("s1");
        Session s3 = new AdminSession("a3").testSetSessionId("s1");
        Session s4 = new AdminSession("a4").testSetSessionId("s1");
        Session s5 = new AdminSession("a5").testSetSessionId("s1");
        Session s52 = new AdminSession("a5").testSetSessionId("s2");

        map.put("a1", "s1", s1);
        map.put("a1", "s2", s12);
        // sleep ensures the system clock goes fwd, important since
        // we're testing things that work based on last access time
        Thread.sleep(1);
        map.put("a2", "s1", s2);
        Thread.sleep(1);
        long afterA2 = System.currentTimeMillis();
        Thread.sleep(1);
        map.put("a3", "s1", s3);
        map.put("a4", "s1", s4);
        map.put("a5", "s1", s5);
        map.put("a5", "s2", s52);
        Thread.sleep(1);

        Assert.assertEquals(5, map.totalActiveAccounts());
        Assert.assertEquals(7, map.totalActiveSessions());

        Session check = map.get("a2", "s1");
        Assert.assertNotNull(check);
        Assert.assertEquals(5, map.totalActiveAccounts());
        Assert.assertEquals(7, map.totalActiveSessions());

        // map should be (access-order): a1_s2, s1_s2, a3_s1, ... a2_s1
        List<Session> removed = map.pruneSessionsByTime(afterA2);
        Assert.assertFalse(removed.isEmpty());
        boolean hasA1S1 = false;
        boolean hasA1S2 = false;
        boolean hasA2 = false;
        for (Session s : removed) {
            if (s.getAuthenticatedAccountId().equals("a1")) {
                if (s.getSessionId().equals("s1")) {
                    hasA1S1 = true;
                }
                if (s.getSessionId().equals("s2")) {
                    hasA1S2 = true;
                }
            }
            if (s.getAuthenticatedAccountId().equals("a2")) {
                hasA2 = true;
            }
        }
        Assert.assertTrue(hasA1S1);
        Assert.assertTrue(hasA1S2);
        Assert.assertFalse(hasA2);

        Assert.assertEquals(4, map.totalActiveAccounts());
        Assert.assertEquals(5, map.totalActiveSessions());

        map.remove("a5", "s1");
        Assert.assertEquals(4, map.totalActiveAccounts());
        Assert.assertEquals(4, map.totalActiveSessions());

        map.remove("a5", "s2");
        Assert.assertEquals(3, map.totalActiveAccounts());
        Assert.assertEquals(3, map.totalActiveSessions());

        List<Session> sessions = map.copySessionList();
        Assert.assertEquals(3, sessions.size());
        Collections.sort(sessions, new Comparator<Session>() {
            @Override
            public int compare(Session s1, Session s2) {
                String hash1 = s1.getAuthenticatedAccountId() + ':' + s1.getSessionId();
                String hash2 = s2.getAuthenticatedAccountId() + ':' + s2.getSessionId();
                return hash1.compareTo(hash2);
            }
        });
        Assert.assertEquals("a2", sessions.get(0).getAuthenticatedAccountId());
        Assert.assertEquals("s1", sessions.get(0).getSessionId());
        Assert.assertEquals("a3", sessions.get(1).getAuthenticatedAccountId());
        Assert.assertEquals("s1", sessions.get(1).getSessionId());
        Assert.assertEquals("a4", sessions.get(2).getAuthenticatedAccountId());
        Assert.assertEquals("s1", sessions.get(2).getSessionId());
    }
}
