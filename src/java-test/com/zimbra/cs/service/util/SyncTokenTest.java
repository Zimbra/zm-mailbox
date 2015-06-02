/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.service.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author psurana
 *
 */
public class SyncTokenTest {

    @Test
    public void syncTokenParseTest() throws Exception {
        String token = "123";
        SyncToken synctoken = new  SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeId());
        Assert.assertEquals(token, synctoken.toString());

        token = "123-032";
        synctoken = new SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeId());
        Assert.assertEquals(32, synctoken.getOffsetInNext());
        Assert.assertEquals("123-32", synctoken.toString());

        token = "123-032:d0345-908";
        synctoken = new SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeId());
        Assert.assertEquals(32, synctoken.getOffsetInNext());
        Assert.assertEquals(345, synctoken.getDeleteModSeq());
        Assert.assertEquals(908, synctoken.getDeleteOffsetInNext());
        Assert.assertEquals("123-32:d345-908", synctoken.toString());

        token = "123:d0345-908";
        synctoken = new SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeId());
        Assert.assertEquals(345, synctoken.getDeleteModSeq());
        Assert.assertEquals(908, synctoken.getDeleteOffsetInNext());
        Assert.assertEquals("123:d345-908", synctoken.toString());

        token = "123:d0345";
        synctoken = new SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeId());
        Assert.assertEquals(345, synctoken.getDeleteModSeq());
        Assert.assertEquals("123:d345", synctoken.toString());
    }

    @Test
    public void testCompare1() {
        SyncToken sc1 = new SyncToken(3, -1, -1, -1);
        SyncToken sc2 = new SyncToken(3, 2, -1, -1);
        SyncToken sc3 = new SyncToken(2, 5, 3, -1);
        SyncToken sc4 = new SyncToken(5, 2, 8, 6);
        SyncToken osc1 = new SyncToken(3, -1, -1, -1);
        SyncToken osc2 = new SyncToken(3, 2, -1, -1);
        SyncToken osc3 = new SyncToken(2, 5, 3, -1);
        SyncToken osc4 = new SyncToken(5, 2, 8, 6);

        Assert.assertEquals(0, sc1.compareTo(osc1));
        Assert.assertEquals(-1, sc1.compareTo(osc2));
        Assert.assertEquals(1, sc1.compareTo(osc3));
        Assert.assertEquals(-2, sc1.compareTo(osc4));

        Assert.assertEquals(1, sc2.compareTo(osc1));
        Assert.assertEquals(0, sc2.compareTo(osc2));
        Assert.assertEquals(1, sc2.compareTo(osc3));
        Assert.assertEquals(-2, sc2.compareTo(osc4));

        Assert.assertEquals(-1, sc3.compareTo(osc1));
        Assert.assertEquals(-1, sc3.compareTo(osc2));
        Assert.assertEquals(0, sc3.compareTo(osc3));
        Assert.assertEquals(-3, sc3.compareTo(osc4));

        Assert.assertEquals(2, sc4.compareTo(osc1));
        Assert.assertEquals(2, sc4.compareTo(osc2));
        Assert.assertEquals(3, sc4.compareTo(osc3));
        Assert.assertEquals(0, sc4.compareTo(osc4));
    }

    @Test
    public void testCompare2() {
        SyncToken sc1 = new SyncToken(3, 4, 4, -1);
        SyncToken osc1 = new SyncToken(3, -1, 4, -1);
        SyncToken osc2 = new SyncToken(3, 4, 4, 2);
        SyncToken osc3 = new SyncToken(3, -1, 4, 2);
        SyncToken osc4 = new SyncToken(3, 5, 4, -1);

        Assert.assertEquals(1, sc1.compareTo(osc1));
        Assert.assertEquals(-1, osc1.compareTo(sc1));
        Assert.assertEquals(-3, sc1.compareTo(osc2));
        Assert.assertEquals(3, osc2.compareTo(sc1));
        Assert.assertEquals(5, sc1.compareTo(osc3));
        Assert.assertEquals(-5, osc3.compareTo(sc1));
        Assert.assertEquals(-1, sc1.compareTo(osc4));
        Assert.assertEquals(1, osc4.compareTo(sc1));

        sc1 = new SyncToken(3, 4, 4, 6);
        Assert.assertEquals(1, sc1.compareTo(osc1));
        Assert.assertEquals(-1, osc1.compareTo(sc1));
        Assert.assertEquals(4, sc1.compareTo(osc2));
        Assert.assertEquals(-4, osc2.compareTo(sc1));
        Assert.assertEquals(5, sc1.compareTo(osc3));
        Assert.assertEquals(-5, osc3.compareTo(sc1));
        Assert.assertEquals(-1, sc1.compareTo(osc4));
        Assert.assertEquals(1, osc4.compareTo(sc1));

        sc1 = new SyncToken(3, -1, 4, -1);
        Assert.assertEquals(0, sc1.compareTo(osc1));
        Assert.assertEquals(0, osc1.compareTo(sc1));
        Assert.assertEquals(-1, sc1.compareTo(osc2));
        Assert.assertEquals(1, osc2.compareTo(sc1));
        Assert.assertEquals(-1, sc1.compareTo(osc3));
        Assert.assertEquals(1, osc3.compareTo(sc1));
        Assert.assertEquals(-1, sc1.compareTo(osc4));
        Assert.assertEquals(1, osc4.compareTo(sc1));
    }

    @Test
    public void testSyncToken() {
        SyncToken one = new SyncToken(1);
        SyncToken two = new SyncToken(2);
        SyncToken three = new SyncToken(3);
        SyncToken two_one = new SyncToken(2,1);
        SyncToken two_two = new SyncToken(2,2);
        SyncToken three_one = new SyncToken(3,1);

        assertTrue(two.after(one));
        assertTrue(three.after(two));
        assertTrue(three.after(one));
        assertFalse(one.after(three));
        assertTrue(two_one.after(two));
        assertFalse(two.after(two_one));
        assertTrue(two_two.after(two_one));
        assertFalse(two_one.after(two_two));
        assertTrue(three_one.after(three));
        assertTrue(three_one.after(two));

        assertFalse(three.after(three_one));
        assertFalse(one.after(three_one));
        assertFalse(one.after(one));
    }

}
