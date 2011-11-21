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

package com.zimbra.cs.util.http;

import org.eclipse.jetty.http.HttpException;
import org.junit.Assert;
import org.junit.Test;



/**
 * Unit tests for ContentRange
 *
 * @author grzes
 */
public class ContentRangeTest
{
    @Test
    public void parseRange() throws Exception
    {
        ContentRange range = ContentRange.parse("bytes 0-999/1000");

        Assert.assertTrue(range.hasStartEnd());
        Assert.assertEquals(range.getStart(), 0);
        Assert.assertEquals(range.getEnd(), 999);
        Assert.assertTrue(range.hasInstanceLength());
        Assert.assertEquals(range.getInstanceLength(), 1000);
    }

    @Test
    public void parseRangeNoInstanceLength() throws Exception
    {
        ContentRange range = ContentRange.parse("bytes 100-200/*");

        Assert.assertTrue(range.hasStartEnd());
        Assert.assertEquals(range.getStart(), 100);
        Assert.assertEquals(range.getEnd(), 200);
        Assert.assertFalse(range.hasInstanceLength());
    }

    @Test
    public void parseRangeExtraWhitespaces() throws Exception
    {
        ContentRange range = ContentRange.parse("   bytes   77777 -   99999   /   123456   ");

        Assert.assertTrue(range.hasStartEnd());
        Assert.assertEquals(range.getStart(), 77777);
        Assert.assertEquals(range.getEnd(), 99999);
        Assert.assertTrue(range.hasInstanceLength());
        Assert.assertEquals(range.getInstanceLength(), 123456);
    }

    @Test
    public void parseNoRangeWithInstanceLength() throws Exception
    {
        ContentRange range = ContentRange.parse("bytes */7000");

        Assert.assertFalse(range.hasStartEnd());
        Assert.assertTrue(range.hasInstanceLength());
        Assert.assertEquals(range.getInstanceLength(), 7000);
    }

    @Test
    public void parseNoRangeNoInstanceLength() throws Exception
    {
        ContentRange range = ContentRange.parse("bytes */*");

        Assert.assertFalse(range.hasStartEnd());
        Assert.assertFalse(range.hasInstanceLength());
    }

    @Test
    public void parseNull() throws Exception
    {
        ContentRange range = ContentRange.parse((String)null);

        Assert.assertNull(range);
    }

    @Test(expected=HttpException.class)
    public void parseReversedRange() throws Exception
    {
        ContentRange.parse("bytes 200-100/777");
    }

    @Test(expected=HttpException.class)
    public void parseRangeTooBig() throws Exception
    {
        ContentRange.parse("bytes 0-100/50");
    }

    @Test(expected=HttpException.class)
    public void parseNegativeInstanceLength() throws Exception
    {
        ContentRange.parse("bytes 0-100/-1000");
    }

    @Test(expected=HttpException.class)
    public void parseNegativeEnd() throws Exception
    {
        ContentRange.parse("bytes 0--100/1000");
    }

    @Test(expected=HttpException.class)
    public void parseGarbage() throws Exception
    {
        ContentRange.parse("garbage");
    }

}
