/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.stats;

import org.junit.Assert;
import org.junit.Test;

public class CounterTest {

    @Test
    public void testIncrement() {
        Counter counter = new Counter();
        Assert.assertTrue(0 == counter.getAverage());

        counter.increment();
        Assert.assertTrue(1 == counter.getAverage()); //1 hit

        counter.increment();
        Assert.assertTrue(1 == counter.getAverage()); //2 hits

        counter.increment(0);
        counter.increment(0);
        Assert.assertTrue(0.5 == counter.getAverage()); //2 hits, 2 misses

        counter.increment(0);
        counter.increment(0);
        counter.increment(0);
        counter.increment(0);
        Assert.assertTrue(0.25 == counter.getAverage()); //2 hits, 6 misses

        counter.increment();
        counter.increment();
        Assert.assertTrue(0.4 == counter.getAverage()); //4 hits, 6 misses

        counter.reset();
        Assert.assertTrue(0 == counter.getAverage());
    }
}
