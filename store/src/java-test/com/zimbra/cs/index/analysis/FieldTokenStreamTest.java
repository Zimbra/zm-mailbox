/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.analysis;

import java.util.Arrays;
import java.util.Collections;

import org.apache.lucene.util.NumericUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.cs.index.ZimbraAnalyzerTest;

/**
 * Unit test for {@link FieldTokenStream}.
 *
 * @author ysasaki
 */
public final class FieldTokenStreamTest {

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

    @Test
    public void limit() throws Exception {
        FieldTokenStream stream = new FieldTokenStream();
        stream.add(Strings.repeat("k", 50), Strings.repeat("v", 50));
        Assert.assertEquals(Collections.emptyList(), ZimbraAnalyzerTest.toTokens(stream));

        stream = new FieldTokenStream();
        for (int i = 0; i < 1001; i++) {
            stream.add("k", "v");
        }
        Assert.assertEquals(1000, ZimbraAnalyzerTest.toTokens(stream).size());
    }

}
