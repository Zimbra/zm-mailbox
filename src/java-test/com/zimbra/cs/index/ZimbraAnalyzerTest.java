/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link ZimbraAnalyzer}.
 *
 * @author ysasaki
 */
public final class ZimbraAnalyzerTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void size() throws Exception {
        String src = "123 26 1000000 100000000 1,000,000,000 1,000,000,000,000,000";
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(LuceneFields.L_SORT_SIZE, new StringReader(src));
        Assert.assertEquals(Arrays.asList("123", "26", "1000000", "100000000", "1000000000", "1000000000000000"),
                toTokens(stream));
    }

    @Test
    public void filename() throws Exception {
        String src = "This is my-filename.test.pdf";
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(LuceneFields.L_FILENAME, new StringReader(src));
        Assert.assertEquals(Arrays.asList("this", "is", "my-filename", "test", "pdf"), toTokens(stream));
    }

    /**
     * We intentionally disable the positionIncrement because we want phrases to match across removed stop words.
     *
     * @see PositionIncrementAttribute
     */
    @Test
    public void positionIncrement() throws Exception {
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(
                LuceneFields.L_H_SUBJECT, new StringReader("It's a test."));
        PositionIncrementAttribute posIncrAtt = stream.addAttribute(PositionIncrementAttribute.class);
        while (stream.incrementToken()) {
            Assert.assertEquals(posIncrAtt.getPositionIncrement(), 1);
        }
        stream.end();
        stream.close();
    }

    @Test
    public void phraseQuery() throws Exception {
        String src = "three^two";
        TokenStream stream = ZimbraAnalyzer.getInstance().tokenStream(LuceneFields.L_CONTENT, new StringReader(src));
        Assert.assertEquals(Arrays.asList("three", "two"), toTokens(stream));
    }

    public static List<String> toTokens(TokenStream stream) throws IOException {
        List<String> result = new ArrayList<String>();
        CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            result.add(termAttr.toString());
        }
        stream.end();
        return result;
    }

}
