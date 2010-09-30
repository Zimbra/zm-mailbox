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

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

/**
 * Unit test for {@link UniversalAnalyzer}.
 *
 * @author ysasaki
 */
public class UniversalAnalyzerTest {
    private UniversalAnalyzer universalAnalyzer = new UniversalAnalyzer();
    // for backward compatibility
    private StandardAnalyzer standardAnalyzer = new StandardAnalyzer(
            Version.LUCENE_24);
    private CJKAnalyzer cjkAnalyzer = new CJKAnalyzer(Version.LUCENE_30);
    // See https://issues.apache.org/jira/browse/LUCENE-1068
    private boolean assertOffset = true;

    @Before
    public void setUp() {
        assertOffset = true;
    }

    @Test
    public void variousText() throws Exception {
        testSTD("C embedded developers wanted");
        testSTD("foo bar FOO BAR");
        testSTD("foo      bar .  FOO <> BAR");
        testSTD("\"QUOTED\" word");

        testSTD("Zimbra is awesome.");
    }

    @Test
    public void acronym() throws Exception {
        testSTD("U.S.A.");
    }

    @Test
    public void alphanumeric() throws Exception {
        testSTD("B2B");
        testSTD("2B");
    }

    @Test
    public void underscore() throws Exception {
        testSTD("word_having_underscore");
        testSTD("word_with_underscore_and_stopwords");
    }

    @Test
    public void delimiter() throws Exception {
        testSTD("some-dashed-phrase");
        testSTD("dogs,chase,cats");
        testSTD("ac/dc");
    }

    @Test
    public void apostrophe() throws Exception {
        testSTD("O'Reilly");
        testSTD("you're");
        testSTD("she's");
        testSTD("Jim's");
        testSTD("don't");
        testSTD("O'Reilly's");
    }

    @Test
    public void tsa() throws Exception {
        // t and s had been stopwords in Lucene <= 2.0, which made it impossible
        // to correctly search for these terms:
        testSTD("s-class");
        testSTD("t-com");
        // 'a' is still a stopword:
        testSTD("a-class");
    }

    @Test
    public void company() throws Exception {
        testSTD("AT&T");
        testSTD("Excite@Home");
    }

    @Test
    public void domain() throws Exception {
        testSTD("www.nutch.org");
        assertOffset = false;
        testSTD("www.nutch.org.");
    }

    @Test
    public void email() throws Exception {
        testSTD("test@example.com");
        testSTD("first.lastname@example.com");
        testSTD("first-lastname@example.com");
        testSTD("first_lastname@example.com");
    }

    @Test
    public void number() throws Exception {
        // floating point, serial, model numbers, ip addresses, etc.
        // every other segment must have at least one digit
        testSTD("21.35");
        testSTD("R2D2 C3PO");
        testSTD("216.239.63.104");
        testSTD("1-2-3");
        testSTD("a1-b2-c3");
        testSTD("a1-b-c3");
    }

    @Test
    public void textWithNumber() throws Exception {
        testSTD("David has 5000 bones");
    }

    @Test
    public void cPlusPlusHash() throws Exception {
        testSTD("C++");
        testSTD("C#");
    }

    @Test
    public void filename() throws Exception {
        testSTD("2004.jpg");
    }

    @Test
    public void numericIncorrect() throws Exception {
        testSTD("62.46");
    }

    @Test
    public void numericLong() throws Exception {
        testSTD("978-0-94045043-1");
    }

    @Test
    public void numericFile() throws Exception {
        testSTD("78academyawards/rules/rule02.html");
    }

    @Test
    public void numericWithUnderscores() throws Exception {
        testSTD("2006-03-11t082958z_01_ban130523_rtridst_0_ozabs");
    }

    @Test
    public void numericWithDash() throws Exception {
        testSTD("mid-20th");
    }

    @Test
    public void manyTokens() throws Exception {
        testSTD("/money.cnn.com/magazines/fortune/fortune_archive/2007/03/19/8402357/index.htm " +
                "safari-0-sheikh-zayed-grand-mosque.jpg");
    }

    @Test
    public void wikipedia() throws Exception {
        String src = new String(ByteStreams.toByteArray(
                getClass().getResourceAsStream("wikipedia-zimbra.txt")),
                Charsets.ISO_8859_1);
        assertOffset = false;
        testSTD(src);
    }

    @Test
    public void japanese() throws Exception {
        testCJK("\u4e00");

        testCJK("\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341");
        testCJK("\u4e00 \u4e8c\u4e09\u56db \u4e94\u516d\u4e03\u516b\u4e5d \u5341");

        testCJK("\u3042\u3044\u3046\u3048\u304aabc\u304b\u304d\u304f\u3051\u3053");
        testCJK("\u3042\u3044\u3046\u3048\u304aab\u3093c\u304b\u304d\u304f\u3051 \u3053");
    }

    @Test
    public void jaPunc() throws Exception {
        testCJK("\u4e00\u3001\u4e8c\u3001\u4e09\u3001\u56db\u3001\u4e94");
    }

    @Test
    public void fullwidth() throws Exception {
        testCJK("\uff34\uff45\uff53\uff54 \uff11\uff12\uff13\uff14");
    }

    private void testSTD(String src) throws IOException {
        TokenStream std = standardAnalyzer.tokenStream(null, new StringReader(src));
        TermAttribute stdTermAttr = std.addAttribute(TermAttribute.class);
        OffsetAttribute stdOffsetAttr = std.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute stdPosIncAttr = std.addAttribute(
                PositionIncrementAttribute.class);

        TokenStream uni = universalAnalyzer.tokenStream(null, new StringReader(src));
        TermAttribute uniTermAttr = uni.addAttribute(TermAttribute.class);
        OffsetAttribute uniOffsetAttr = uni.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute uniPosIncAttr = uni.addAttribute(
                PositionIncrementAttribute.class);

        while (true) {
            boolean result = std.incrementToken();
            Assert.assertEquals(result, uni.incrementToken());
            if (!result) {
                break;
            }
            String term = stdTermAttr.term();
            Assert.assertEquals(stdTermAttr, uniTermAttr);
            if (assertOffset) {
                Assert.assertEquals(term, stdOffsetAttr, uniOffsetAttr);
            }
            Assert.assertEquals(term, stdPosIncAttr, uniPosIncAttr);
        }
    }

    private void testCJK(String src) throws IOException {
        TokenStream cjk = cjkAnalyzer.tokenStream(null, new StringReader(src));
        TermAttribute cjkTermAttr = cjk.addAttribute(TermAttribute.class);
        OffsetAttribute cjkOffsetAttr = cjk.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute cjkPosIncAttr = cjk.addAttribute(
                PositionIncrementAttribute.class);

        TokenStream uni = universalAnalyzer.tokenStream(null, new StringReader(src));
        TermAttribute uniTermAttr = uni.addAttribute(TermAttribute.class);
        OffsetAttribute uniOffsetAttr = uni.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute uniPosIncAttr = uni.addAttribute(
                PositionIncrementAttribute.class);

        while (true) {
            boolean result = cjk.incrementToken();
            Assert.assertEquals(result, uni.incrementToken());
            if (!result) {
                break;
            }
            String term = cjkTermAttr.term();
            Assert.assertEquals(cjkTermAttr, uniTermAttr);
            if (assertOffset) {
                Assert.assertEquals(term, cjkOffsetAttr, uniOffsetAttr);
            }
            Assert.assertEquals(term, cjkPosIncAttr, uniPosIncAttr);
        }
    }

}
