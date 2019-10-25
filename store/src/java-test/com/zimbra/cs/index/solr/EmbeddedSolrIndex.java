package com.zimbra.cs.index.solr;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.ZimbraIndexDocumentID;
import com.zimbra.cs.index.ZimbraIndexReader;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraTermsFilter;
import com.zimbra.cs.index.ZimbraTopDocs;

/**
 * Embedded SOLR server used for testing
 *
 * @author iraykin
 *
 */
public class EmbeddedSolrIndex extends SolrIndex {
    private static EmbeddedSolrServer server = null;
    private static final String solrHome = "../store/build/test/solr/";
    public static String TEST_CORE_NAME = "zsolrtestcore";

    private EmbeddedSolrIndex(String accountId, SolrRequestHelper solrHelper) {
        super(accountId, solrHelper);
        System.setProperty("solr.allow.unsafe.resourceloading", "true");
        this.accountId = accountId;
    }

    @Override
    public synchronized Indexer openIndexer() throws IOException, ServiceException {
        return new SolrIndexer();
    }

    @Override
    public ZimbraIndexSearcher openSearcher() throws IOException, ServiceException {
        final SolrIndexReader reader = new SolrIndexReader();
        return new SolrIndexSearcher(reader);
    }

    @Override
    public synchronized void evict() {
        // TODO Auto-generated method stub
    }

    @Override
    public synchronized void deleteIndex() throws IOException, ServiceException {
        SolrClient solrServer = getSolrServer();
        try {
            ZimbraLog.index.info(String.format("unloading core %s", accountId));
            CoreAdminRequest.unloadCore(accountId, solrServer);
        } catch (SolrServerException e) {
            ZimbraLog.index.error("Problem deleting Solr collection", e);
        } catch (IOException e) {
            ZimbraLog.index.error("Problem deleting Solr collection", e);
        } catch (SolrException e) {
            ZimbraLog.index.warn(String.format("Could not unload solr core %s", accountId), e);
        } finally {
            File f = new File(solrHome, accountId);
            FileUtils.deleteDirectory(f);
            }
    }

    public static final class Factory implements IndexStore.Factory {

        public Factory() {
            ZimbraLog.index.info("Created SolrIndexStore\n");
        }

        @Override
        public SolrIndex getIndexStore(String accountId) {
            SolrRequestHelper solrHelper = null; //TODO: implement EmbeddedSolrRequestHelper
            return new EmbeddedSolrIndex(accountId, solrHelper);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public synchronized void destroy() {
            if (server != null) {
                try {
                    server.close();
                    //server.shutdown();
                } catch (Exception e) {
                    // can be ignored, since the test server may have not been
                    // properly initialized
                    ZimbraLog.test.warn("Caught an exception trying to shutdown EmbeddedSolrServer instance", e);
                }
                try {
                    FileUtils.deleteDirectory(new File("../ZimbraServer/build/test/solr/" + TEST_CORE_NAME));
                } catch (Exception e) {
                    ZimbraLog.test.warn("Caught an exception trying to delete solr data folders", e);
                }
                server = null;
            }
        }
    }

    public class SolrIndexSearcher extends SolrIndex.SolrIndexSearcher {
        public SolrIndexSearcher(com.zimbra.cs.index.solr.SolrIndex.SolrIndexReader reader) {
            super(reader);
        }

        @Override
        public synchronized void close() throws IOException {
            super.close();
        }

        @Override
        public synchronized Document doc(ZimbraIndexDocumentID docID) throws IOException, ServiceException {
            return super.doc(docID);
        }

        @Override
        public synchronized int docFreq(Term term) throws IOException, ServiceException {
            return super.docFreq(term);
        }

        @Override
        public synchronized ZimbraIndexReader getIndexReader() {
            return super.getIndexReader();
        }

        @Override
        public synchronized ZimbraTopDocs search(Query query, int n) throws IOException, ServiceException {
            return super.search(query, n);
        }

        @Override
        public synchronized ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n) throws IOException,
                ServiceException {
            return super.search(query, filter, n);
        }

        @Override
        public synchronized ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n, Sort sort)
                throws IOException, ServiceException {
            return super.search(query, filter, n, sort);
        }
    }

    @Override
    public synchronized void warmup() {
        super.warmup();
    }

    @Override
    public synchronized boolean isPendingDelete() {
        return super.isPendingDelete();
    }

    @Override
    public synchronized void setPendingDelete(boolean pendingDelete) {
        super.setPendingDelete(pendingDelete);
    }

    @Override
    public synchronized void optimize() {
        super.optimize();
    }

    @Override
    public synchronized boolean verify(PrintStream out) throws IOException {
        return super.verify(out);
    }

    @Override
    protected synchronized void addTermsFilter(SolrQuery query, Collection<Term> terms) {
        super.addTermsFilter(query, terms);
    }

    @Override
    protected synchronized String termToQuery(Term term) {
        return super.termToQuery(term);
    }

    @Override
    public synchronized List<Map<String, Object>> fetchFileList(long gen, String accountId) throws ServiceException {
        return super.fetchFileList(gen, accountId);
    }

    public synchronized EmbeddedSolrServer getSolrServer() throws ServiceException {
        if (server == null) {
            Path home = Paths.get(solrHome);
            Path xml = Paths.get(solrHome, "solr.xml");
            CoreContainer cores = CoreContainer.createAndLoad(home, xml);
            ZimbraLog.test.info("created core container at %s", home.toString());
            server = new EmbeddedSolrServer(cores, accountId);
            /*
             * We have to "root" the EmbeddedSolrServer in a separate core that
             * will be the core against which other CoreAdminRequests are run.
             * If it does not exist (which it might), we create it
             */
            if (!cores.isLoaded(TEST_CORE_NAME)) {
                ZimbraLog.test.info("creating core zsolrtestcore");
                Properties props = new Properties();
                Map<String, String> params = new HashMap<String, String>();
                props.put("configSet", "zimbra");
                SolrCore core = cores.create(TEST_CORE_NAME, params);
                cores.getCoresLocator().create(cores, core.getCoreDescriptor());
                ZimbraLog.test.info("created zsolrtestcore at %s", core.getDataDir());
            } else {
                ZimbraLog.test.info("zsolrtestcore is already loaded");
            }

        }
        return server;
    }

    @VisibleForTesting
    public synchronized EmbeddedSolrServer getEmbeddedServer() throws ServiceException {
        return getSolrServer();
    }
}
