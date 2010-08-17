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
        Tokenizer tokenizer = new AddrCharTokenizer(
                new StringReader("all-snv"));
        Assert.assertEquals(Collections.singletonList("all-snv"),
                ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("."));
        Assert.assertEquals(Collections.singletonList("."),
                ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader(".. ."));
        Assert.assertEquals(Arrays.asList("..", "."),
                ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader(".abc"));
        Assert.assertEquals(Collections.singletonList(".abc"),
                ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("a"));
        Assert.assertEquals(Collections.singletonList("a"),
                ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("test.com"));
        Assert.assertEquals(Collections.singletonList("test.com"),
                ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("user1@zim"));
        Assert.assertEquals(Collections.singletonList("user1@zim"),
                ZimbraAnalyzerTest.toTokens(tokenizer));

        tokenizer.reset(new StringReader("user1@zimbra.com"));
        Assert.assertEquals(Collections.singletonList("user1@zimbra.com"),
                ZimbraAnalyzerTest.toTokens(tokenizer));
    }

}
