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
import java.util.List;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.junit.Test;
import org.testng.Assert;

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
    public void addrCharTokenizer() throws Exception {
        Tokenizer tokenizer = new ZimbraAnalyzer.AddrCharTokenizer(
                new StringReader("all-snv"));
        Assert.assertEquals(toTokens(tokenizer), new String[]{"all-snv"});

        tokenizer.reset(new StringReader("."));
        Assert.assertEquals(toTokens(tokenizer), new String[]{"."});

        tokenizer.reset(new StringReader(".. ."));
        Assert.assertEquals(toTokens(tokenizer), new String[]{"..", "."});

        tokenizer.reset(new StringReader(".abc"));
        Assert.assertEquals(toTokens(tokenizer), new String[]{".abc"});

        tokenizer.reset(new StringReader("a"));
        Assert.assertEquals(toTokens(tokenizer), new String[]{"a"});

        tokenizer.reset(new StringReader("test.com"));
        Assert.assertEquals(toTokens(tokenizer), new String[]{"test.com"});

        tokenizer.reset(new StringReader("user1@zim"));
        Assert.assertEquals(toTokens(tokenizer), new String[]{"user1@zim"});

        tokenizer.reset(new StringReader("user1@zimbra.com"));
        Assert.assertEquals(toTokens(tokenizer),
                new String[]{"user1@zimbra.com"});
    }

    @Test
    public void contactDataFilter() throws Exception {
        ZimbraAnalyzer.AddrCharTokenizer tokenizer =
            new ZimbraAnalyzer.AddrCharTokenizer(new StringReader("all-snv"));
        TokenFilter filter = new ZimbraAnalyzer.ContactDataFilter(tokenizer);
        Assert.assertEquals(toTokens(filter), new String[] {"all-snv"});

        tokenizer.reset(new StringReader("."));
        Assert.assertEquals(toTokens(filter), new String[0]);

        tokenizer.reset(new StringReader(".. ."));
        Assert.assertEquals(toTokens(filter), new String[] {".."});

        tokenizer.reset(new StringReader(".abc"));
        Assert.assertEquals(toTokens(filter), new String[] {".abc"});

        tokenizer.reset(new StringReader("a"));
        Assert.assertEquals(toTokens(filter), new String[] {"a"});

        tokenizer.reset(new StringReader("test.com"));
        Assert.assertEquals(toTokens(filter), new String[] {"test.com"});

        tokenizer.reset(new StringReader("user1@zim"));
        Assert.assertEquals(toTokens(filter), new String[] {"user1@zim"});

        tokenizer.reset(new StringReader("user1@zimbra.com"));
        Assert.assertEquals(toTokens(filter),
                new String[]{"user1@zimbra.com"});
    }

    @Test
    public void addressTokenFilter() throws Exception {
        TokenStream stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_H_FROM, new StringReader(LONG_SRC));

        Assert.assertEquals(toTokens(stream), new String[] {
            "donotreply@zimbra.com", "@zimbra", "donotreply", "@zimbra.com",
            "zimbra.com", "tim@foo.com", "@foo", "tim", "@foo.com", "foo.com",
            "tester", "address", "test.address@mail.nnnn.com", "test",
            "address", "nnnn", "test.address", "@mail.nnnn.com",
            "mail.nnnn.com", "image/jpeg", "text/plain", "text/foo/bar", "tim",
            "tim@foo.com", "@foo", "tim", "@foo.com", "foo.com",
            "bugzilla-daemon@eric.example.zimbra.com", "zimbra",
            "bugzilla-daemon", "@eric.example.zimbra.com",
            "eric.example.zimbra.com", "zug", "zug", "zug@gug.com", "@gug",
            "zug", "@gug.com", "gug.com", "foo.gub", "my", "mom",
            "mmm@nnnn.com", "@nnnn", "mmm", "@nnnn.com", "nnnn.com", "asd",
            "foo", "bar", "aaa/bbb", "ccc/ddd/eee", "fff@ggg.com", "@ggg",
            "fff", "@ggg.com", "ggg.com", "hhh@iiii", "hhh", "@iiii", "iiii"
        });
    }

    @Test
    public void from() throws Exception {
        String src = "dharma@fdharma.com";
        TokenStream stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_H_FROM, new StringReader(src));
        Assert.assertEquals(toTokens(stream), new String[] {
            "dharma@fdharma.com", "@fdharma", "dharma", "@fdharma.com",
            "fdharma.com"
        });

        src = "\"Tim Brown\" <first@domain.com>";
        stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_H_FROM, new StringReader(src));
        Assert.assertEquals(toTokens(stream), new String[] {
            "tim", "brown", "first@domain.com", "@domain", "first",
            "@domain.com", "domain.com"
        });
    }

    @Test
    public void attachments() throws Exception {
        TokenStream stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_ATTACHMENTS, new StringReader(LONG_SRC));
        //TODO: is it correct to prefix a space?
        Assert.assertEquals(toTokens(stream), new String[] {
            "donotreply@zimbra.com tim@foo.com \"tester address\" <test.address@mail.nnnn.com>",
            " image/jpeg", " image", " text/plain", " text", " text/foo/bar",
            " text", " tim (tim@foo.com)",
            "bugzilla-daemon@eric.example.zimbra.com", " zug zug [zug@gug.com]",
            " foo.gub", " \"my mom\" <mmm@nnnn.com>",
            "asd foo bar aaa/bbb ccc/ddd/eee fff@ggg.com hhh@iiii",
            "asd foo bar aaa"
        });
    }

    @Test
    public void mimeType() throws Exception {
        TokenStream stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_MIMETYPE, new StringReader(LONG_SRC));
        Assert.assertEquals(toTokens(stream), new String[] {
            "donotreply@zimbra.com tim@foo.com \"tester address\" <test.address@mail.nnnn.com>",
            " image/jpeg", " image", " text/plain", " text", " text/foo/bar",
            " text", " tim (tim@foo.com)",
            "bugzilla-daemon@eric.example.zimbra.com", " zug zug [zug@gug.com]",
            " foo.gub", " \"my mom\" <mmm@nnnn.com>",
            "asd foo bar aaa/bbb ccc/ddd/eee fff@ggg.com hhh@iiii",
            "asd foo bar aaa"
        });
    }

    @Test
    public void size() throws Exception {
        String src = "123 26 1000000 100000000 1,000,000,000 1,000,000,000,000,000";
        TokenStream stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_SORT_SIZE, new StringReader(src));
        Assert.assertEquals(toTokens(stream), new String[] {
            "123", "26", "1000000", "100000000", "1000000000",
            "1000000000000000"
        });
    }

    @Test
    public void field() throws Exception {
        String src = "test1:val1 val2 val3    val4-test\t  val5\r\n" +
                "#test2:2val1 2val2:_123 2val3\ntest3:zzz\n" +
                "#calendarItemClass:public";
        TokenStream stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_FIELD, new StringReader(src));
        Assert.assertEquals(toTokens(stream), new String[] {
            "test1:val1", "test1:val2", "test1:val3", "test1:val4",
            "test1:test", "test1:val5", "#test2:2val1", "#test2:2val2:_123",
            "#test2:2val3", "test3:zzz", "#calendaritemclass:public"
        });
    }

    @Test
    public void filename() throws Exception {
        String src = "This is my-filename.test.pdf";
        TokenStream stream = ZimbraAnalyzer.getDefaultAnalyzer().tokenStream(
                LuceneFields.L_FILENAME, new StringReader(src));
        Assert.assertEquals(toTokens(stream), new String[] {
            "this", "is", "my-filename", "test", "pdf"
        });
    }

    private String[] toTokens(TokenStream stream) throws IOException {
        List<String> result = new ArrayList<String>();
        TermAttribute termAttr = stream.addAttribute(TermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            result.add(termAttr.term());
        }
        stream.end();
        stream.close();
        return result.toArray(new String[0]);
    }

}
