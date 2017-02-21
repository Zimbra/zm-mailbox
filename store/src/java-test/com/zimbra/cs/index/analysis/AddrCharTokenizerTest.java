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

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

import org.apache.lucene.analysis.Tokenizer;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzerTest;

/**
 * Unit test for {@link AddrCharTokenizer}.
 *
 * @author ysasaki
 */
public class AddrCharTokenizerTest {

    @Test
    public void addrCharTokenizer() throws Exception {
        Tokenizer tokenizer = new AddrCharTokenizer(new StringReader("all-snv"));
        Assert.assertEquals(Collections.singletonList("all-snv"), ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("."));
        Assert.assertEquals(Collections.singletonList("."), ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader(".. ."));
        Assert.assertEquals(Arrays.asList("..", "."), ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader(".abc"));
        Assert.assertEquals(Collections.singletonList(".abc"), ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("a"));
        Assert.assertEquals(Collections.singletonList("a"), ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("test.com"));
        Assert.assertEquals(Collections.singletonList("test.com"), ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("user1@zim"));
        Assert.assertEquals(Collections.singletonList("user1@zim"), ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("user1@zimbra.com"));
        Assert.assertEquals(Collections.singletonList("user1@zimbra.com"), ZimbraAnalyzerTest.toTokens(tokenizer));
    }

    /**
     * Bug 79103 tab was getting included at start of a token instead of being ignored.
     */
    @Test
    public void multiLineWithTabs() throws Exception {
        Tokenizer tokenizer = new AddrCharTokenizer(
                new StringReader("one name <one@example.net>\n\ttwo <two@example.net>"));
        Assert.assertEquals("Token list", Arrays.asList("one", "name", "one@example.net", "two", "two@example.net"),
                ZimbraAnalyzerTest.toTokens(tokenizer));
    }

    @Test
    public void japanese() throws Exception {
        Tokenizer tokenizer = new AddrCharTokenizer(new StringReader("\u68ee\u3000\u6b21\u90ce"));
        Assert.assertEquals(Arrays.asList("\u68ee", "\u6b21\u90ce"), ZimbraAnalyzerTest.toTokens(tokenizer));
    }

}
