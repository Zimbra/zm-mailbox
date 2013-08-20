/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
