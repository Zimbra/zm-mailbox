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
package com.zimbra.cs.index.analysis;

import java.util.Arrays;

import org.apache.lucene.util.NumericUtils;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzerTest;

/**
 * Unit test for {@link FieldTokenStream}.
 *
 * @author ysasaki
 */
public class FieldTokenStreamTest {

    @Test
    public void tokens() throws Exception {
        FieldTokenStream stream = new FieldTokenStream();
        stream.add("test1", "val1 val2 val3    val4-test\t  val5");
        stream.add("#test2", "2val1 2val2:_123 2val3");
        stream.add("test3", "zzz");
        stream.add("#calendarItemClass", "public");
        stream.add("zimbraCalResCapacity", "10");

        Assert.assertEquals(Arrays.asList(
                "test1:val1", "test1:val2", "test1:val3", "test1:val4", "test1:test", "test1:val5",
                "#test2:2val1", "#test2:2val2:_123", "#test2:2val3", "test3:zzz", "#calendaritemclass:public",
                "zimbracalrescapacity#:" + NumericUtils.intToPrefixCoded(10), "zimbracalrescapacity:10"),
                ZimbraAnalyzerTest.toTokens(stream));
    }

}
