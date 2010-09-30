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

package com.zimbra.cs.index;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzer;

/**
 * Unit test for {@link ZimbraAnalyzer}.
 *
 * @author ysasaki
 */
public class ZimbraAnalyzerTest {
    private static final String LONG_SRC =
        "DONOTREPLY@zimbra.com tim@foo.com " +
        "\"Tester Address\" <test.address@mail.nnnn.com>, " +
        "image/jpeg, text/plain, text/foo/bar, tim (tim@foo.com)," +
        "bugzilla-daemon@eric.example.zimbra.com, zug zug [zug@gug.com], " +
        "Foo.gub, \"My Mom\" <mmm@nnnn.com>,asd foo bar aaa/bbb ccc/ddd/eee " +
        "fff@ggg.com hhh@iiii";

    @Test
    public void attachments() throws Exception {
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(
                LuceneFields.L_ATTACHMENTS, new StringReader(LONG_SRC));
        //TODO: is it correct to prefix a space?
        Assert.assertEquals(Arrays.asList(
                "donotreply@zimbra.com tim@foo.com \"tester address\" <test.address@mail.nnnn.com>",
                " image/jpeg", " image", " text/plain", " text", " text/foo/bar",
                " text", " tim (tim@foo.com)",
                "bugzilla-daemon@eric.example.zimbra.com", " zug zug [zug@gug.com]",
                " foo.gub", " \"my mom\" <mmm@nnnn.com>",
                "asd foo bar aaa/bbb ccc/ddd/eee fff@ggg.com hhh@iiii",
                "asd foo bar aaa"),
                toTokens(stream));
    }

    @Test
    public void mimeType() throws Exception {
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(
                LuceneFields.L_MIMETYPE, new StringReader(LONG_SRC));
        Assert.assertEquals(Arrays.asList(
                "donotreply@zimbra.com tim@foo.com \"tester address\" <test.address@mail.nnnn.com>",
                " image/jpeg", " image", " text/plain", " text", " text/foo/bar",
                " text", " tim (tim@foo.com)",
                "bugzilla-daemon@eric.example.zimbra.com", " zug zug [zug@gug.com]",
                " foo.gub", " \"my mom\" <mmm@nnnn.com>",
                "asd foo bar aaa/bbb ccc/ddd/eee fff@ggg.com hhh@iiii",
                "asd foo bar aaa"),
                toTokens(stream));
    }

    @Test
    public void size() throws Exception {
        String src = "123 26 1000000 100000000 1,000,000,000 1,000,000,000,000,000";
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(
                LuceneFields.L_SORT_SIZE, new StringReader(src));
        Assert.assertEquals(Arrays.asList("123", "26", "1000000", "100000000",
                "1000000000", "1000000000000000"),
                toTokens(stream));
    }

    @Test
    public void field() throws Exception {
        String src = "test1:val1 val2 val3    val4-test\t  val5\r\n" +
                "#test2:2val1 2val2:_123 2val3\ntest3:zzz\n" +
                "#calendarItemClass:public";
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(
                LuceneFields.L_FIELD, new StringReader(src));
        Assert.assertEquals(Arrays.asList("test1:val1", "test1:val2",
                "test1:val3", "test1:val4", "test1:test", "test1:val5",
                "#test2:2val1", "#test2:2val2:_123", "#test2:2val3", "test3:zzz",
                "#calendaritemclass:public"),
                toTokens(stream));
    }

    @Test
    public void filename() throws Exception {
        String src = "This is my-filename.test.pdf";
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(
                LuceneFields.L_FILENAME, new StringReader(src));
        Assert.assertEquals(Arrays.asList("this", "is", "my-filename", "test", "pdf"),
                toTokens(stream));
    }

    /**
     * We intentionally disable the positionIncrement because we want phrases to
     * match across removed stop words.
     *
     * @see PositionIncrementAttribute
     */
    @Test
    public void positionIncrement() throws Exception {
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(
                LuceneFields.L_H_SUBJECT, new StringReader("It's a test."));
        PositionIncrementAttribute posIncrAtt = stream.addAttribute(
                PositionIncrementAttribute.class);
        while (stream.incrementToken()) {
            Assert.assertEquals(posIncrAtt.getPositionIncrement(), 1);
        }
        stream.end();
        stream.close();
    }

    public static List<String> toTokens(TokenStream stream) throws IOException {
        List<String> result = new ArrayList<String>();
        TermAttribute termAttr = stream.addAttribute(TermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            result.add(termAttr.term());
        }
        stream.end();
        stream.close();
        return result;
    }

}
