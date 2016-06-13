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
package com.zimbra.cs.util.http;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.Pair;

public class RangeTest
{
    @Test
    public void parseSingleRange() throws Exception
    {
        Range range = Range.parse("bytes=0-999");

        List<Pair<Long, Long>> ranges = range.getRanges();
        Assert.assertFalse(ranges.isEmpty());
        Iterator<Pair<Long, Long>> iter = ranges.iterator();
        Pair<Long, Long> byteRange = iter.next();
        Assert.assertFalse(iter.hasNext());

        Assert.assertEquals(byteRange.getFirst().longValue(), 0);
        Assert.assertEquals(byteRange.getSecond().longValue(), 999);
    }

    @Test
    public void parseOpenEndedRange() throws Exception
    {
        Range range = Range.parse("bytes=123-");

        List<Pair<Long, Long>> ranges = range.getRanges();
        Assert.assertFalse(ranges.isEmpty());
        Iterator<Pair<Long, Long>> iter = ranges.iterator();
        Pair<Long, Long> byteRange = iter.next();
        Assert.assertFalse(iter.hasNext());

        Assert.assertEquals(byteRange.getFirst().longValue(), 123);
        Assert.assertNull(byteRange.getSecond());
    }

    @Test
    public void parseSuffixRange() throws Exception
    {
        Range range = Range.parse("bytes=-999");

        List<Pair<Long, Long>> ranges = range.getRanges();
        Assert.assertFalse(ranges.isEmpty());
        Iterator<Pair<Long, Long>> iter = ranges.iterator();
        Pair<Long, Long> byteRange = iter.next();
        Assert.assertFalse(iter.hasNext());

        Assert.assertNull(byteRange.getFirst());
        Assert.assertEquals(byteRange.getSecond().longValue(), 999);
    }

    @Test
    public void parseMultipleRanges() throws Exception
    {
        Range range = Range.parse("bytes=0-999,2000-3000, 4444 - 7777, -12345");

        List<Pair<Long, Long>> ranges = range.getRanges();
        Assert.assertFalse(ranges.isEmpty());

        Iterator<Pair<Long, Long>> iter = ranges.iterator();

        {
            Pair<Long, Long> byteRange = iter.next();
            Assert.assertEquals(byteRange.getFirst().longValue(), 0);
            Assert.assertEquals(byteRange.getSecond().longValue(), 999);
        }

        {
            Pair<Long, Long> byteRange = iter.next();
            Assert.assertEquals(byteRange.getFirst().longValue(), 2000);
            Assert.assertEquals(byteRange.getSecond().longValue(), 3000);
        }

        {
            Pair<Long, Long> byteRange = iter.next();
            Assert.assertEquals(byteRange.getFirst().longValue(), 4444);
            Assert.assertEquals(byteRange.getSecond().longValue(), 7777);
        }

        {
            Pair<Long, Long> byteRange = iter.next();
            Assert.assertNull(byteRange.getFirst());
            Assert.assertEquals(byteRange.getSecond().longValue(), 12345);
        }

        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void parseMultipleRangesWithExtraSpacesAndCommas() throws Exception
    {
        Range range = Range.parse("  bytes = ,, 0   - 999,,,, ,  ,2000  -3000  ");

        List<Pair<Long, Long>> ranges = range.getRanges();
        Assert.assertFalse(ranges.isEmpty());

        Iterator<Pair<Long, Long>> iter = ranges.iterator();

        {
            Pair<Long, Long> byteRange = iter.next();
            Assert.assertEquals(byteRange.getFirst().longValue(), 0);
            Assert.assertEquals(byteRange.getSecond().longValue(), 999);
        }

        {
            Pair<Long, Long> byteRange = iter.next();
            Assert.assertEquals(byteRange.getFirst().longValue(), 2000);
            Assert.assertEquals(byteRange.getSecond().longValue(), 3000);
        }
    }

    @Test
    public void parseNull() throws Exception
    {
        Range range = Range.parse((String)null);
        Assert.assertNull(range);
    }

    @Test(expected=RangeException.class)
    public void parseReversedRange() throws Exception
    {
        Range.parse("bytes=200-100");
    }

    @Test(expected=RangeException.class)
    public void parseNegativeEndValue() throws Exception
    {
        Range.parse("bytes=100--200");
    }

    @Test(expected=RangeException.class)
    public void parseNegativeStartValue() throws Exception
    {
        Range.parse("bytes=-100-200");
    }

    @Test(expected=RangeException.class)
    public void parseGarbage() throws Exception
    {
        Range.parse("garbage");
    }

    @Test(expected=RangeException.class)
    public void parseNoValues() throws Exception
    {
        Range.parse("-");
    }
}
