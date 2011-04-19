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
package com.zimbra.cs.index.analysis;

import java.util.Arrays;

import org.apache.lucene.analysis.TokenStream;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzerTest;

/**
 * Unit test for {@link MimeTypeTokenStream}.
 *
 * @author ysasaki
 */
public final class MimeTypeTokenStreamTest {

    @Test
    public void tokenize() throws Exception {
        TokenStream stream = new MimeTypeTokenStream(Arrays.asList("image/jpeg", "text/plain", " text/foo/bar ",
                "aaa bbb ccc ddd/eee fff/ggg/hhh"));
        Assert.assertEquals(Arrays.asList("image/jpeg", "image", "text/plain", "text", "text/foo/bar", "text",
                "aaa bbb ccc ddd/eee fff/ggg/hhh", "aaa bbb ccc ddd", "any"), ZimbraAnalyzerTest.toTokens(stream));
    }

}
