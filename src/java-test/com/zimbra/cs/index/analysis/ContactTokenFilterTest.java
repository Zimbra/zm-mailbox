/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Collections;

import org.apache.lucene.analysis.TokenFilter;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzerTest;

/**
 * Unit test for {@link ContactTokenFilter}.
 *
 * @author ysasaki
 */
public class ContactTokenFilterTest {

    @Test
    public void contactDataFilter() throws Exception {
        AddrCharTokenizer tokenizer = new AddrCharTokenizer(new StringReader("all-snv"));
        TokenFilter filter = new ContactTokenFilter(tokenizer);
        Assert.assertEquals(Collections.singletonList("all-snv"),
                ZimbraAnalyzerTest.toTokens(filter));

        tokenizer.reset(new StringReader("."));
        Assert.assertEquals(Collections.EMPTY_LIST,
                ZimbraAnalyzerTest.toTokens(filter));

        tokenizer.reset(new StringReader(".. ."));
        Assert.assertEquals(Collections.singletonList(".."),
                ZimbraAnalyzerTest.toTokens(filter));

        tokenizer.reset(new StringReader(".abc"));
        Assert.assertEquals(Collections.singletonList(".abc"),
                ZimbraAnalyzerTest.toTokens(filter));

        tokenizer.reset(new StringReader("a"));
        Assert.assertEquals(Collections.singletonList("a"),
                ZimbraAnalyzerTest.toTokens(filter));

        tokenizer.reset(new StringReader("test.com"));
        Assert.assertEquals(Collections.singletonList("test.com"),
                ZimbraAnalyzerTest.toTokens(filter));

        tokenizer.reset(new StringReader("user1@zim"));
        Assert.assertEquals(Collections.singletonList("user1@zim"),
                ZimbraAnalyzerTest.toTokens(filter));

        tokenizer.reset(new StringReader("user1@zimbra.com"));
        Assert.assertEquals(Collections.singletonList("user1@zimbra.com"),
                ZimbraAnalyzerTest.toTokens(filter));
    }

}
