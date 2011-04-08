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
        MockHit hit11 = new MockHit(result, 1, 10L);
        result.add(hit11);
        MockHit hit12 = new MockHit(result, 2, 8L);
        result.add(hit12);
        MockHit hit13 = new MockHit(result, 2, 6L);
        result.add(hit13);
        multi.add(result);

        result = new MockQueryResults(EnumSet.of(MailItem.Type.MESSAGE), SortBy.DATE_DESC);
        MockHit hit21 = new MockHit(result, 1, 9L);
        result.add(hit21);
        MockHit hit22 = new MockHit(result, 2, 7L);
        result.add(hit22);
        MockHit hit23 = new MockHit(result, 2, 5L);
        result.add(hit23);
        multi.add(result);

        Assert.assertSame(hit11, multi.getNext());
        Assert.assertSame(hit21, multi.getNext());
        Assert.assertSame(hit12, multi.getNext());
        Assert.assertFalse(multi.hasNext());
        Assert.assertNull(multi.getNext());

        multi.shrink(1);
        multi.resetIterator();
        Assert.assertSame(hit21, multi.getNext());
        Assert.assertSame(hit12, multi.getNext());
        Assert.assertFalse(multi.hasNext());
        Assert.assertNull(multi.getNext());
    }

}
