package com.zimbra.cs.index.solr;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.zimbra.common.service.ServiceException;

/**
 * Note: many of these tests currently fail.
 * This is because the StandardAnalyzer used for cross-referencing the Zimbra tokenizer
 * was versioned to Lucene 2.4, which we no longer have access to, since we are now at 4.9 and
 * can only use versions back to 3.0. However, the StandardAnalyzer behavior has changed since 2.4
 * and many of the tests break. Eventually it may be worthwhile to remove the StandardAnalyzer
 * as a reference entirely.
 * @author iraykin
 * @author ysasaki
 *
 */
@Ignore ("This test case is ignored until bug described in https://issues.apache.org/jira/browse/SOLR-2834 is fixed in solrj. The bug is causing the SolrPluginTestBase.doAnalysisRequest method to fail.")
public class StandardTokenizerTest extends SolrPluginTestBase {
    // See https://issues.apache.org/jira/browse/LUCENE-1068
    private boolean assertOffset = true;

    @Override
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
        String src = new String(ByteStreams.toByteArray(getClass().getResourceAsStream("wikipedia-zimbra.txt")),
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

    private void testSTD(String src) throws IOException, SolrServerException, ServiceException {
        //disabling this until zm-solr can be made a dependency of zm-mailbox
        /* ZimbraTokenizer std = new ZimbraTokenizer();
        std.setReader(new StringReader(src));
        std.reset();
        CharTermAttribute stdTermAttr = std.addAttribute(CharTermAttribute.class);
        OffsetAttribute stdOffsetAttr = std.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute stdPosIncAttr = std.addAttribute(PositionIncrementAttribute.class);

        List<String> terms = new ArrayList<String>();
        List<Integer> positions = new ArrayList<Integer>();
        List<Integer> startOffsets = new ArrayList<Integer>();
        List<Integer> endOffsets = new ArrayList<Integer>();
        List<TokenInfo> tokens = getTokenInfoWithoutReversals("zmtext", src);
        for (TokenInfo token: tokens) {
            terms.add(token.getText());
            positions.add(token.getPosition());
            startOffsets.add(token.getStart());
            endOffsets.add(token.getEnd());
        }


        int idx = 0;
        while (true) {
            boolean result = std.incrementToken();
            if (!result) {
                assertEquals(idx, tokens.size());
                break;
            }
            String expectedTerm = stdTermAttr.toString();
            String actualTerm   = terms.get(idx);
            Assert.assertEquals(expectedTerm, actualTerm);
            if (assertOffset) {
                assertEquals(expectedTerm, stdOffsetAttr.startOffset(), (int) startOffsets.get(idx));
                assertEquals(expectedTerm, stdOffsetAttr.endOffset(), (int) endOffsets.get(idx));
            }
            Assert.assertEquals(expectedTerm, stdPosIncAttr.getPositionIncrement(), (int) positions.get(idx));
            idx++;
        }
        std.close(); */
    }

    private void testCJK(String src) throws IOException, SolrServerException, ServiceException {
        TokenStream cjk = new CJKAnalyzer().tokenStream(null, new StringReader(src));
        cjk.reset();
        CharTermAttribute cjkTermAttr = cjk.addAttribute(CharTermAttribute.class);
        OffsetAttribute cjkOffsetAttr = cjk.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute cjkPosIncAttr = cjk.addAttribute(PositionIncrementAttribute.class);

        List<String> terms = new ArrayList<String>();
        List<Integer> positions = new ArrayList<Integer>();
        List<Integer> startOffsets = new ArrayList<Integer>();
        List<Integer> endOffsets = new ArrayList<Integer>();
        List<TokenInfo> tokens = getTokenInfoWithoutReversals("zmtext", src);
        for (TokenInfo token: tokens) {
            terms.add(token.getText());
            positions.add(token.getPosition());
            startOffsets.add(token.getStart());
            endOffsets.add(token.getEnd());
        }

        int idx = 0;
        while (true) {
            boolean result = cjk.incrementToken();
            if (!result) {
                assertEquals(idx, tokens.size());
                break;
            }
            String expectedTerm = cjkTermAttr.toString();
            String actualTerm   = terms.get(idx);
            Assert.assertEquals(expectedTerm, actualTerm);
            if (assertOffset) {
                assertEquals(expectedTerm, cjkOffsetAttr.startOffset(), (int) startOffsets.get(idx));
                assertEquals(expectedTerm, cjkOffsetAttr.endOffset(), (int) endOffsets.get(idx));
            }
            Assert.assertEquals(expectedTerm, cjkPosIncAttr.getPositionIncrement(), (int) positions.get(idx));
            idx++;
        }
        cjk.close();
    }
}
