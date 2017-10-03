package com.zimbra.cs.index.solr;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;

/** This test case is ignored until bug described in https://issues.apache.org/jira/browse/SOLR-2834
 *  is fixed in solrj. The bug is causing the SolrPluginTestBase.doAnalysisRequest method to fail.
 * @author iraykin
 */
@Ignore
public class SoutheastAsianLanguagesTest extends SolrPluginTestBase {

    //Thai tokenizes on words despite lack of whitespace; excludes stopwords

    @Test
    public void testThai() throws SolrServerException, IOException, ServiceException {
        String thai1 = "\u0E40\u0E1B\u0E47\u0E19"; //stopword!
        String thai2 = "\u0E2D\u0E22\u0E48\u0E32\u0E07\u0E44\u0E23";
        String thai3 = "\u0E1A\u0E49\u0E32\u0E07";
        assertEquals(Arrays.asList(thai2, thai3), getTokensWithoutReversals("zmtext", thai1 + thai2 + thai3));
    }

    //The other three languages should just tokenize on whatever whitespace is available

    @Test
    public void testLao() throws SolrServerException, IOException, ServiceException {
        String lao1 = "\u0EAA\u0EB0\u0E9A\u0EB2";
        String lao2 = "\u0E8D\u0E94\u0EB5\u0E9A";
        assertEquals(Arrays.asList(lao1, lao2), getTokensWithoutReversals("zmtext", lao1 + " " + lao2));
    }

    @Test
    public void testMyanmar() throws SolrServerException, IOException, ServiceException {
        String myanmar1 = "\u1001\u1004\u103A\u1017\u103B\u102C\u1038\u1031\u1014\u1031";
        String myanmar2 = "\u1000\u102C\u1004\u103A\u1038\u101B\u1032\u1037\u101C\u102C\u1038\u104B";
        assertEquals(Arrays.asList(myanmar1, myanmar2), getTokensWithoutReversals("zmtext", myanmar1 + " " + myanmar2));
    }

    @Test
    public void testKhmer() throws SolrServerException, IOException, ServiceException {
        String khmer1 = "\u17A2\u17D2\u1793\u1780\u179F\u17BB\u1781";
        String khmer2 = "\u179F\u1794\u17D2\u1794\u17B6\u1799\u1791\u17C1";
        assertEquals(Arrays.asList(khmer1, khmer2), getTokensWithoutReversals("zmtext", khmer1 + " " + khmer2));

    }
}
