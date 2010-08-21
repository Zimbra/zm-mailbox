/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.index;

import org.junit.Test;
import org.testng.Assert;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;

/**
 * Unit test for {@link MsgQueryResults}.
 *
 * @author ysasaki
 */
public class MsgQueryResultsTest {

    @Test
    public void merge() throws Exception {
        MockQueryResults top = new MockQueryResults(SortBy.NONE);
        top.add(new MessageHit(null, null, 1000, null, 0, null));
        top.add(new MessagePartHit(null, null, 1000, null, 0, null));
        top.add(new MessagePartHit(null, null, 1000, null, 0, null));
        top.add(new MessageHit(null, null, 1001, null, 0, null));
        top.add(new MessageHit(null, null, 1001, null, 0, null));
        top.add(new MessagePartHit(null, null, 1001, null, 0, null));
        top.add(new MessagePartHit(null, null, 1001, null, 0, null));
        top.add(new MessageHit(null, null, 1002, null, 0, null));
        top.add(new MessageHit(null, null, 1003, null, 0, null));

        ProxiedHit phit = new ProxiedHit(null, null);
        phit.itemID = new ItemId("A", 1000);
        top.add(phit);

        phit = new ProxiedHit(null, null);
        phit.itemID = new ItemId("B", 1000);
        top.add(phit);

        MsgQueryResults result = new MsgQueryResults(top, null, SortBy.NONE,
                Mailbox.SearchResultMode.NORMAL);

        ZimbraHit hit = result.getNext();
        Assert.assertEquals(hit.getClass(), MessageHit.class);
        Assert.assertEquals(hit.getItemId(), 1000);

        hit = result.getNext();
        Assert.assertEquals(hit.getClass(), MessageHit.class);
        Assert.assertEquals(hit.getItemId(), 1001);

        hit = result.getNext();
        Assert.assertEquals(hit.getClass(), MessageHit.class);
        Assert.assertEquals(hit.getItemId(), 1002);

        hit = result.getNext();
        Assert.assertEquals(hit.getClass(), MessageHit.class);
        Assert.assertEquals(hit.getItemId(), 1003);

        hit = result.getNext();
        Assert.assertEquals(hit.getClass(), ProxiedHit.class);
        Assert.assertEquals(hit.getItemId(), 1000);

        hit = result.getNext();
        Assert.assertEquals(hit.getClass(), ProxiedHit.class);
        Assert.assertEquals(hit.getItemId(), 1000);

        Assert.assertFalse(result.hasNext());
    }

}
