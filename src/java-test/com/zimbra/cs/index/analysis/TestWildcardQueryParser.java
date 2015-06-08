package com.zimbra.cs.index.analysis;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.solr.EmbeddedSolrIndex;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class TestWildcardQueryParser {
    private static EmbeddedSolrIndex index;
    private static EmbeddedSolrServer solrServer;
    private static String thaiWord1 = "\u0E2D\u0E22\u0E48\u0E32\u0E07\u0E44\u0E23";
    private static String thaiWord2 = "\u0E1A\u0E49\u0E32\u0E07";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }
    
    @AfterClass
    public static void destroy() throws Exception {
        index.deleteIndex();
        solrServer = null;
    }
    
    @Before
    public void setUp() throws Exception {
        cleanup();
        
        index = (EmbeddedSolrIndex) IndexStore.getFactory().getIndexStore(EmbeddedSolrIndex.TEST_CORE_NAME);
        solrServer = index.getEmbeddedServer();

        //we need to add some documents to test wildcard query expansion
        addDoc("3", "l.content", "therr foo bar");
        addDoc("4", "l.content", "them foo bars");
        addDoc("8", "l.content", "abc1");
        addDoc("9", "l.content", "abc2");
        addDoc("1", "subject", "foo barbeque");
        addDoc("2", "subject", "foo barmitzvah");
        addDoc("5", "subject", "\u30C6" + "\u30B9" + "\u30C8" + "abc");
        addDoc("6", "subject", "\u30C6" + "\u30B9" + "\u30C8" + "abd");
        addDoc("7", "subject", "food bard");
        addDoc("10", "subject","thai text: " + thaiWord1 + thaiWord2); // no whitespace!
    }

    private void addDoc(String id, String field, String content) throws SolrServerException, IOException, ServiceException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(field, content);
        doc.addField("solrId", id);
        UpdateRequest req = new UpdateRequest();
        req.add(doc);
        req.setParam("collection", EmbeddedSolrIndex.TEST_CORE_NAME);
        req.setAction(ACTION.COMMIT, false, true, true);
        req.process(solrServer);
    }

    private void cleanup() throws Exception {
        MailboxTestUtil.clearData();
    }
    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private String debugQuery(String fields, String query) throws Exception {
        StringBuilder sb = new StringBuilder("{!zimbrawildcard fields=\"").append(fields).append("\"}").append(query);
        SolrQuery q = new SolrQuery().setQuery(sb.toString()).setRows(0);
        q.setParam("debugQuery", true);
        QueryRequest req = new QueryRequest(q, METHOD.POST);
        QueryResponse resp = req.process(solrServer);
        Map<String,Object> debug = resp.getDebugMap();
        return (String) debug.get("parsedquery");
    }

    @Test
    public void testSingleTermWildcards() throws Exception {
        //single-term leading and trailing wildcards pass through to the SolrQueryParser
        assertEquals("subject:foo*", debugQuery("subject", "foo*"));
        assertEquals("(subject:*foo)", debugQuery("subject", "*foo"));

        //stopwords don't get stripped
        assertEquals("subject:the*", debugQuery("subject", "the*"));

        //wildcards inside terms are ignored
        assertEquals("MultiPhraseQuery(subject:\"f o\")", debugQuery("subject", "f*o"));
    }

    @Test
    public void testMultiTermWildcards() throws Exception {
        //wildcards at the end of terms are expanded into a MultiPhraseQuery
        assertEquals("MultiPhraseQuery(subject:\"foo (barbeque bard barmitzvah)\")", debugQuery("subject", "foo bar*"));
        assertEquals("MultiPhraseQuery(subject:\"(foo food) bar\")", debugQuery("subject", "foo* bar"));
        assertEquals("MultiPhraseQuery(subject:\"(foo food) (barbeque bard barmitzvah)\")", debugQuery("subject", "foo* bar*"));

        //stopwords leave a position gap
        assertEquals("MultiPhraseQuery(subject:\"(foo food) ? (barbeque bard barmitzvah)\")", debugQuery("subject", "foo* the bar*"));

        //wildcard term should expand differently across different fields
        assertEquals("MultiPhraseQuery(subject:\"foo (barbeque bard barmitzvah)\") MultiPhraseQuery(l.content:\"foo (bar bars)\")", debugQuery("subject l.content", "foo bar*"));

        //if a wildcard term doesnt expand to anything in one of the specified fields, that field is not included in the query
        assertEquals("MultiPhraseQuery(l.content:\"foo bars\")", debugQuery("subject l.content", "foo bars*"));

        //leading wildcards in phrase searches are ignored
        assertEquals("MultiPhraseQuery(subject:\"foo bar\")", debugQuery("subject", "*foo bar"));
        assertEquals("MultiPhraseQuery(subject:\"foo bar\")", debugQuery("subject", "foo *bar"));
        assertEquals("MultiPhraseQuery(subject:\"foo ? bar\")", debugQuery("subject", "foo the *bar"));

        //wildcards inside terms in phrase searches are ignored
        assertEquals("MultiPhraseQuery(subject:\"f o bar\")", debugQuery("subject", "f*o bar"));
    }

    /* Wildcards following stopwords is a weird case. If the stopword term is the first term in the phrase
     * or the first term following another wildcard, like "the* bar" or "foo* the* bar", then it'll get expanded properly.
     * However, if it's following a non-wildcard term, like "foo the*", then the wildcard will be applied
     * to the last PRECEDING non-stopword term instead. For example, "foo the* bar" will be treated as "foo* bar".
     * This happens because the query parser can't know if the last token in the token stream was actually the term
     * that had the wildcard attached to it; stopwords dropped from the end of the stream leave no trace.
     * It works when the stopword wildcard is the first term because then that section of the query tokenizes
     * to an empty list, at which point the parser KNOWS that there must have been a stopword there and can reconstruct it.
     */
    @Test
    public void testStopwordWildcards() throws Exception {
        assertEquals("MultiPhraseQuery(l.content:\"(them therr) ? foo\")", debugQuery("l.content", "the* the foo"));
        assertEquals("MultiPhraseQuery(l.content:\"(abc1 abc2) bar\")", debugQuery("l.content", "abc the* bar"));
    }

    @Test
    public void testCJKWildcards() throws Exception {
        assertEquals("MultiPhraseQuery(subject:\"\u3066\u3059 \u3059\u3068\")", debugQuery("subject", "\u30C6\u30B9\u30C8" + "*"));
        assertEquals("MultiPhraseQuery(subject:\"\u3066\u3059 \u3059\u3068 (abc abd)\")", debugQuery("subject", "\u30C6\u30B9\u30C8" + "ab*"));
    }

    @Test
    public void testThaiWildcards() throws Exception {
        assertEquals("MultiPhraseQuery(subject:\"" + thaiWord1 + " " + thaiWord2 + "\")", debugQuery("subject", thaiWord1 + thaiWord2 +"*"));
    }

    /* Wildcards following punctuation sequences, though unlikely, lead to a conundrum due to their being indexed with position increment 0.
     * The approach is this: if there are non-punctuation token in the query, use the last one. If not, use the last
     * punctuation token.
     */
    @Test
    public void testPunctuationWildcards() throws Exception {
        //non-punctuation tokens present, so they are used as wildcards
        assertEquals("subject:foo*", debugQuery("subject", "foo?!*"));
        assertEquals("MultiPhraseQuery(subject:\"foo (barbeque bard barmitzvah)\")", debugQuery("subject", "foo bar?!*"));

        //everything is punctuation here, so last PUNC token is used as wilcard
        assertEquals("subject:!?*", debugQuery("subject", "!?*"));
    }
}
