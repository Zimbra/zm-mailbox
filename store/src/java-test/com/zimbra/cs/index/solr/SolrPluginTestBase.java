package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.AnalysisPhase;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis;
import org.apache.solr.common.SolrException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.mailbox.MailboxTestUtil;
public abstract class SolrPluginTestBase {
    protected static SolrClient solrServer;
    protected static EmbeddedSolrIndex index;

    private List<TokenInfo> doAnalysisRequest(String fieldType, String value, boolean ignoreLastPhase) throws SolrServerException, IOException, ServiceException {
        FieldAnalysisRequest req = new FieldAnalysisRequest("/analysis/field");
        req.addFieldType(fieldType);
        req.setFieldValue(value);
        FieldAnalysisResponse resp = req.process(solrServer);
        Analysis analysis = resp.getFieldTypeAnalysis(fieldType);
        int numPhases = analysis.getIndexPhasesCount();
        int curPhaseNum = 0;
        int targetPhaseIdx = ignoreLastPhase? numPhases - 1: numPhases;
        for (AnalysisPhase phase: analysis.getIndexPhases()) {
            curPhaseNum++;
            if (curPhaseNum == targetPhaseIdx) {
                return phase.getTokens();
            }
        }
        return null;
    }


    protected List<TokenInfo> getTokenInfoWithoutReversals(String fieldType, String value) throws SolrServerException, IOException, ServiceException {
        return doAnalysisRequest(fieldType, value, true);
    }

    protected List<TokenInfo> getTokenInfo(String fieldType, String value) throws SolrServerException, IOException, ServiceException {
        return doAnalysisRequest(fieldType, value, false);
    }

    protected List<String> getTokensWithoutReversals(String fieldType, String value) throws SolrServerException, IOException, ServiceException {
        return getTokenText(fieldType, value, true);
    }

    protected List<String> getTokens(String fieldType, String value) throws SolrServerException, IOException, ServiceException {
        return getTokenText(fieldType, value, false);
    }

    protected List<Integer> getPositionOffsetsWithoutReversals(String fieldType, String value) throws SolrServerException, IOException, ServiceException {
        return getPositionOffsets(fieldType, value, true);
    }

    protected List<Integer> getPositionOffsets(String fieldType, String value) throws SolrServerException, IOException, ServiceException {
        return getPositionOffsets(fieldType, value, false);
    }

    private List<String> getTokenText(String fieldType, String value, boolean ignoreLastPhase) throws SolrServerException, IOException, ServiceException {
        List<String> tokens = new ArrayList<String>();
        for (TokenInfo token: doAnalysisRequest(fieldType, value, ignoreLastPhase)) {
            tokens.add(token.getText());
        }
        return tokens;
    }

    private List<Integer> getPositionOffsets(String fieldType, String value, boolean ignoreLastPhase) throws SolrServerException, IOException, ServiceException {
        List<Integer> tokens = new ArrayList<Integer>();
        for (TokenInfo token: doAnalysisRequest(fieldType, value, ignoreLastPhase)) {
            tokens.add(token.getPosition());
        }
        return tokens;
    }

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }
    
    @Before
    public void setUp() throws Exception {
        cleanup();
        index = (EmbeddedSolrIndex) new EmbeddedSolrIndex.Factory().getIndexStore(EmbeddedSolrIndex.TEST_CORE_NAME);
        solrServer = index.getEmbeddedServer();
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private void cleanup() throws Exception {
        IndexStore.getFactory().destroy();
        try {
            MailboxTestUtil.cleanupAllIndexStores();
        } catch (SolrException ex) {
            //ignore. We are deleting the folders anyway
        }
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
