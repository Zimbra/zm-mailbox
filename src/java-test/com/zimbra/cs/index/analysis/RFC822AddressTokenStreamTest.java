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

import org.apache.lucene.analysis.TokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzerTest;

/**
 * Unit test for {@link RFC822AddressTokenStream}.
 *
 * @author ysasaki
 */
public class RFC822AddressTokenStreamTest {

    @Test
    public void single() throws Exception {
        TokenStream stream = new RFC822AddressTokenStream("user@domain.com");
        Assert.assertEquals(Arrays.asList("user@domain.com", "user",
                "@domain.com", "domain.com", "domain", "@domain"),
                ZimbraAnalyzerTest.toTokens(stream));

        stream = new RFC822AddressTokenStream("\"Tim Brown\" <first.last@sub.domain.com>");
        Assert.assertEquals(Arrays.asList("tim", "brown",
                "first.last@sub.domain.com", "first.last", "first", "last",
                "@sub.domain.com", "sub.domain.com", "domain", "@domain"),
                ZimbraAnalyzerTest.toTokens(stream));
    }

    @Test
    public void multi() throws Exception {
        TokenStream stream = new RFC822AddressTokenStream(
                "\"User One\" <user.1@zimbra.com>, \"User Two\" <user.2@zimbra.com>, \"User Three\" <user.3@zimbra.com>");
        Assert.assertEquals(Arrays.asList(
                "user", "one", "user.1@zimbra.com", "user.1", "user", "1",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra",
                "user", "two", "user.2@zimbra.com", "user.2", "user", "2",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra",
                "user", "three", "user.3@zimbra.com", "user.3", "user", "3",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra"),
                ZimbraAnalyzerTest.toTokens(stream));
    }

    @Test
    public void comment() throws Exception {
        TokenStream stream = new RFC822AddressTokenStream(
                "Pete(A wonderful \\) chap) <pete(his account)@silly.test(his host)>");
        Assert.assertEquals(Arrays.asList("pete", "wonderful", "chap", "pete",
                "his", "account", "@silly.test", "his", "host",
                "pete@silly.test", "pete", "@silly.test", "silly.test"),
                ZimbraAnalyzerTest.toTokens(stream));
    }

    @Test
    public void topPrivateDomain() throws Exception {
        TokenStream stream = new RFC822AddressTokenStream("support@zimbra.com");
        Assert.assertEquals(Arrays.asList("support@zimbra.com", "support",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra"),
                ZimbraAnalyzerTest.toTokens(stream));

        stream = new RFC822AddressTokenStream("support@zimbra.vmware.co.jp");
        Assert.assertEquals(Arrays.asList("support@zimbra.vmware.co.jp",
                "support", "@zimbra.vmware.co.jp", "zimbra.vmware.co.jp",
                "vmware", "@vmware"), ZimbraAnalyzerTest.toTokens(stream));

        stream = new RFC822AddressTokenStream("test@co.jp");
        Assert.assertEquals(Arrays.asList("test@co.jp", "test", "@co.jp", "co.jp"),
                ZimbraAnalyzerTest.toTokens(stream));
    }

    @Test
    public void reset() throws Exception {
        TokenStream stream = new RFC822AddressTokenStream("user@domain.com");
        stream.reset();
        Assert.assertEquals(Arrays.asList("user@domain.com", "user",
                "@domain.com", "domain.com", "domain", "@domain"),
                ZimbraAnalyzerTest.toTokens(stream));
        stream.reset();
        Assert.assertEquals(Arrays.asList("user@domain.com", "user",
                "@domain.com", "domain.com", "domain", "@domain"),
                ZimbraAnalyzerTest.toTokens(stream));
    }

}
