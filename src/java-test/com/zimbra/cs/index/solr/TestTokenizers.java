package com.zimbra.cs.index.solr;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

/** 
 * @author iraykin
 */

@Ignore ("This test case is ignored until bug described in https://issues.apache.org/jira/browse/SOLR-2834 is fixed in solrj. The bug is causing the SolrPluginTestBase.doAnalysisRequest method to fail.") 
public class TestTokenizers extends SolrPluginTestBase {

    @Test
    public void testNumber() throws Exception {
        String src = "123 456 1,000,000";
        assertEquals(Arrays.asList("123", "456", "1000000"), getTokens("zmnumber", src));
    }

    @Test
    public void testFilename() throws Exception {
        String src = "This is my-filename.test.pdf";
        assertEquals(Arrays.asList("this", "is", "my-filename", "test", "pdf"), getTokens("zmfilename", src));
    }

    @Test
    public void testPhraseQuery() throws Exception {
        String src = "ONE two^three.";
        assertEquals(Arrays.asList("one", "two", "^", "three"), getTokensWithoutReversals("zmtext", src));
    }

    @Test
    public void testContacts() throws Exception {
        assertEquals(Collections.singletonList("all-snv"), getTokens("zmcontact", "all-snv"));
        assertEquals(Collections.EMPTY_LIST, getTokens("zmcontact", "."));
        assertEquals(Collections.singletonList(".."), getTokens("zmcontact", ".. ."));
        assertEquals(Collections.singletonList(".abc"), getTokens("zmcontact", ".abc"));
        assertEquals(Collections.singletonList("a"), getTokens("zmcontact", "a"));
        assertEquals(Collections.singletonList("test.com"), getTokens("zmcontact", "test.com"));
        assertEquals(Collections.singletonList("user1@zim"), getTokens("zmcontact", "user1@zim"));
        assertEquals(Collections.singletonList("user1@zimbra.com"), getTokens("zmcontact", "user1@zimbra.com"));
    }
}
