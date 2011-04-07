/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.mailbox.MailItem;

/**
 * Unit test for {@link MultiQueryResults}.
 *
 * @author ysasaki
 */
public final class MultiQueryResultsTest {

    @Test
    public void multi() throws Exception {
        MultiQueryResults multi = new MultiQueryResults(3, SortBy.DATE_DESC);

        MockQueryResults result = new MockQueryResults(EnumSet.of(MailItem.Type.MESSAGE), SortBy.DATE_DESC);
        MockHit hit = new MockHit(result, 1, "1-1", 10L);
        result.add(hit);
        hit = new MockHit(result, 2, "1-2", 8L);
        result.add(hit);
        hit = new MockHit(result, 2, "1-3", 6L);
        result.add(hit);
        multi.add(result);

        result = new MockQueryResults(EnumSet.of(MailItem.Type.MESSAGE), SortBy.DATE_DESC);
        hit = new MockHit(result, 1, "2-1", 9L);
        result.add(hit);
        hit = new MockHit(result, 2, "2-2", 7L);
        result.add(hit);
        hit = new MockHit(result, 2, "2-3", 5L);
        result.add(hit);
        multi.add(result);

        Assert.assertEquals("1-1", multi.getNext().getName());
        Assert.assertEquals("2-1", multi.getNext().getName());
        Assert.assertEquals("1-2", multi.getNext().getName());
        Assert.assertFalse(multi.hasNext());
        Assert.assertNull(multi.getNext());

        multi.shrink(1);
        multi.resetIterator();
        Assert.assertEquals("2-1", multi.getNext().getName());
        Assert.assertEquals("1-2", multi.getNext().getName());
        Assert.assertFalse(multi.hasNext());
        Assert.assertNull(multi.getNext());
    }

}
