package com.zimbra.cs.index.solr;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraIndexDocumentID;
import com.zimbra.cs.index.ZimbraIndexReader;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraTermsFilter;
import com.zimbra.cs.index.ZimbraTopDocs;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.util.ProvisioningUtil;

/**
 * Embedded SOLR server used for testing
 * @author iraykin
 *
 */
public class EmbeddedSolrIndex  extends SolrIndexBase {
    private static EmbeddedSolrServer server = null;
    private CoreContainer coreContainer = null;
    private static final String solrHome = "../ZimbraServer/build/test/solr/";
    public static String TEST_CORE_NAME = "zsolrtestcore";

    private EmbeddedSolrIndex(String accountId) {
        System.setProperty("solr.allow.unsafe.resourceloading", "true");
        this.accountId = accountId;
    }

    @Override
    public synchronized boolean indexExists() {
        File f = new File(solrHome, accountId);
        if(!f.exists()) {
            return false;
        }
        SolrCore core = null;
        try {
            CoreContainer container = getSolrServer().getCoreContainer();
            core = container.getCore(accountId);
            if(core == null) {
                return false;
            }
            
            String szDataDir = core.getDataDir();
            if(szDataDir == null) {
                return false;
            }
            File dataDir = new File(szDataDir);
            if(!dataDir.exists()) {
                return false;
            }
            
            String szIndexDir = core.getIndexDir();
            File indexDir = new File(szIndexDir);
            if(! indexDir.exists()) {
                return false;
            }
            
        } catch (Exception e) {
            return false;
        } finally {
            if(core != null) {
                core.close();
            }
        }
        return true;
    }

    @Override
    public synchronized void initIndex() throws IOException, ServiceException {
        CoreContainer container = getSolrServer().getCoreContainer();
        Properties props = new Properties();
        props.put("configSet", "zimbra");
        CoreDescriptor cd = new CoreDescriptor(container, accountId, accountId, props);
        try {
            container.getCoresLocator().create(container, cd);
        } catch (SolrException e) {
            //someone left the folder dirty
            File f = new File(solrHome, accountId);
            FileUtils.deleteDirectory(f);
            try {
                container.getCoresLocator().create(container, cd);
            } catch (SolrException ex) {
                throw(ex);
            }
        }
        try {
            SolrCore c = container.create(cd);
        } catch (SolrException e) {
            //already may be created by another thread
            ZimbraLog.test.error("Failed to init core %s", accountId, e);
        }
    }

    @Override
    public synchronized Indexer openIndexer() throws IOException, ServiceException {
        if(!indexExists()) {
            initIndex();
        }
        return new SolrIndexer();
    }

    @Override
    public ZimbraIndexSearcher openSearcher() throws IOException, ServiceException {
        if(!indexExists()) {
            initIndex();
        }
        final SolrIndexReader reader = new SolrIndexReader();
        return new SolrIndexSearcher(reader);
    }

    @Override
    public synchronized void evict() {
        // TODO Auto-generated method stub

    }

    @Override
    public synchronized void deleteIndex() throws IOException, ServiceException {
        if (indexExists()) {
            SolrServer solrServer = getSolrServer();
            try {
                ZimbraLog.index.info(String.format("unloading core %s", accountId));
                CoreAdminRequest.unloadCore(accountId, solrServer);
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Problem deleting Solr collection" , e);
            } catch (IOException e) {
                ZimbraLog.index.error("Problem deleting Solr collection" , e);
            } catch (SolrException e) {
                ZimbraLog.index.warn(String.format("Could not unload solr core %s", accountId), e);
            }
            finally {
                File f = new File(solrHome, accountId);
                FileUtils.deleteDirectory(f);
            }
        }
    }

    public static final class Factory implements IndexStore.Factory {

        public Factory() {
            ZimbraLog.index.info("Created SolrlIndexStore\n");
        }

        @Override
        public SolrIndexBase getIndexStore(String accountId) {
            return new EmbeddedSolrIndex(accountId);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public synchronized void destroy() {
            if (server != null) {
                try {
                    server.shutdown();
                } catch (Exception e) {
                    //can be ignored, since the test server may have not been properly initialized
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

    private class SolrIndexer extends SolrIndexBase.SolrIndexer {

        @Override
        public synchronized void close() throws IOException {
        }

        @Override
        protected void setAction(UpdateRequest req) {
            req.setAction(ACTION.COMMIT, false, true, false);
        }

        @Override
        public synchronized int maxDocs() {
            SolrServer solrServer = null;
            try {
                solrServer = getSolrServer();
                CoreAdminResponse resp = CoreAdminRequest.getStatus(null, solrServer);
                Iterator<Map.Entry<String, NamedList<Object>>> iter = resp.getCoreStatus().iterator();
                while(iter.hasNext()) {
                    Map.Entry<String, NamedList<Object>> entry = iter.next();
                    if(entry.getKey().indexOf(accountId, 0)==0) {
                        Object maxDocs = entry.getValue().findRecursive("index","maxDoc");
                        if(maxDocs != null && maxDocs instanceof Integer) {
                            return (int)maxDocs;
                        }
                    }
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Caught IOException retrieving maxDocs for mailbox %s", accountId,e );
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Caught SolrServerException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Caught RemoteSolrException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Caught ServiceException retrieving maxDocs for mailbox %s", accountId,e );
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }

        @Override
        public void add(List<Mailbox.IndexItemEntry> entries) throws IOException, ServiceException {
            if(!indexExists()) {
                initIndex();
            }
            SolrServer solrServer = getSolrServer();
            UpdateRequest req = new UpdateRequest();
            setupRequest(req, solrServer);
            setAction(req);
            for (IndexItemEntry entry : entries) {
                if (entry.documents == null) {
                    ZimbraLog.index.warn("NULL index data item=%s", entry);
                    continue;
                }
                int partNum = 1;
                for (IndexDocument doc : entry.documents) {
                    SolrInputDocument solrDoc;
                    // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                    synchronized (doc) {
                        setFields(entry.item, doc);
                        solrDoc = doc.toInputDocument();
                        solrDoc.addField(SOLR_ID_FIELD, String.format("%d_%d",entry.item.getId(),partNum));
                        partNum++;
                        if (ZimbraLog.index.isTraceEnabled()) {
                            ZimbraLog.index.trace("Adding solr document %s", solrDoc.toString());
                        }
                    }
                    req.add(solrDoc);
                }
            }
            try {
                incrementUpdateCounter(solrServer);
                processRequest(solrServer, req);
            } catch (RemoteSolrException | SolrServerException e) {
                ZimbraLog.index.error("Problem indexing documents", e);
            }  finally {
                shutdown(solrServer);
            }
        }

        @Override
        public void addDocument(MailItem item, List<IndexDocument> docs) throws IOException, ServiceException {
            if(!indexExists()) {
                initIndex();
            }
            if (docs == null || docs.isEmpty()) {
                return;
            }

            int partNum = 1;
            for (IndexDocument doc : docs) {
                SolrInputDocument solrDoc;
                // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                synchronized (doc) {
                    setFields(item, doc);
                    solrDoc = doc.toInputDocument();
                    solrDoc.addField(SOLR_ID_FIELD, String.format("%d_%d",item.getId(),partNum));
                    partNum++;
                    if (ZimbraLog.index.isTraceEnabled()) {
                        ZimbraLog.index.trace("Adding solr document %s", solrDoc.toString());
                    }
                }
                SolrServer solrServer = getSolrServer();
                UpdateRequest req = new UpdateRequest();
                setupRequest(req, solrServer);
                req.add(solrDoc);
                setAction(req);
                try {
                    incrementUpdateCounter(solrServer);
                    processRequest(solrServer, req);
                } catch (SolrServerException e) {
                    ZimbraLog.index.error("Problem indexing document with id=%d", item.getId(),e);
                } catch (RemoteSolrException e) {
                    ZimbraLog.index.error("Problem indexing document with id=%d", item.getId(),e);
                }  finally {
                    shutdown(solrServer);
                }
            }
        }

        @Override
        public void deleteDocument(List<Integer> ids) throws IOException,ServiceException {
            if(!indexExists()) {
                return;
            }
            SolrServer solrServer = getSolrServer();
            try {
                for (Integer id : ids) {
                    UpdateRequest req = new UpdateRequest().deleteByQuery(String.format("%s:%d",LuceneFields.L_MAILBOX_BLOB_ID,id));
                    setupRequest(req, solrServer);
                    if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                        setAction(req);
                    }
                    try {
                        incrementUpdateCounter(solrServer);
                        processRequest(solrServer, req);
                        ZimbraLog.index.debug("Deleted document id=%d", id);
                    } catch (SolrServerException e) {
                        ZimbraLog.index.error("Problem deleting document with id=%d", id,e);
                    } catch (RemoteSolrException e) {
                        ZimbraLog.index.error("Problem deleting document with id=%d", id,e);
                    }
                }
            } finally {
                shutdown(solrServer);
            }
        }
        
        @Override
        protected synchronized void incrementUpdateCounter(SolrServer solrServer)
                throws ServiceException {
            // TODO Auto-generated method stub
            super.incrementUpdateCounter(solrServer);
        }

        @Override
        public synchronized void compact() {
            // TODO Auto-generated method stub
            super.compact();
        }
    }

    public class SolrIndexSearcher extends SolrIndexBase.SolrIndexSearcher {
        public SolrIndexSearcher(
                com.zimbra.cs.index.solr.SolrIndexBase.SolrIndexReader reader) {
            super(reader);
        }

        @Override
        public synchronized void close() throws IOException {
            // TODO Auto-generated method stub
            super.close();
        }

        @Override
        public synchronized Document doc(ZimbraIndexDocumentID docID) throws IOException,
                ServiceException {
            // TODO Auto-generated method stub
            return super.doc(docID);
        }

        @Override
        public synchronized int docFreq(Term term) throws IOException, ServiceException {
            // TODO Auto-generated method stub
            return super.docFreq(term);
        }

        @Override
        public synchronized ZimbraIndexReader getIndexReader() {
            // TODO Auto-generated method stub
            return super.getIndexReader();
        }

        @Override
        public synchronized ZimbraTopDocs search(Query query, int n) throws IOException,
                ServiceException {
            // TODO Auto-generated method stub
            return super.search(query, n);
        }

        @Override
        public synchronized ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n)
                throws IOException, ServiceException {
            // TODO Auto-generated method stub
            return super.search(query, filter, n);
        }

        @Override
        public synchronized ZimbraTopDocs search(Query query, ZimbraTermsFilter filter,
                int n, Sort sort) throws IOException, ServiceException {
            // TODO Auto-generated method stub
            return super.search(query, filter, n, sort);
        }
    }
    public class SolrIndexReader extends SolrIndexBase.SolrIndexReader {
        @Override
        public synchronized int numDeletedDocs() {
            SolrServer solrServer = null;
            try {
                solrServer = getSolrServer();
                CoreAdminResponse resp = CoreAdminRequest.getStatus(null, solrServer);
                Iterator<Map.Entry<String, NamedList<Object>>> iter = resp.getCoreStatus().iterator();
                while(iter.hasNext()) {
                    Map.Entry<String, NamedList<Object>> entry = iter.next();
                    if(entry.getKey().indexOf(accountId, 0)==0) {
                        return (int)entry.getValue().findRecursive("index","deletedDocs");
                    }
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Caught IOException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Caught SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Caught SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Caught ServiceException retrieving number of deleted documents in mailbox %s", accountId,e);
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }

        @Override
        public synchronized void close() throws IOException {
            // TODO Auto-generated method stub
            super.close();
        }

        @Override
        public synchronized int numDocs() throws ServiceException {
            // TODO Auto-generated method stub
            return super.numDocs();
        }

        @Override
        public TermFieldEnumeration getTermsForField(String field,
                String firstTermValue) throws IOException, ServiceException {
            // TODO Auto-generated method stub
            return super.getTermsForField(field, firstTermValue);
        }
    }


    @Override
    public synchronized void setupRequest(Object obj, SolrServer solrServer) {
        if (obj instanceof UpdateRequest) {
            ((UpdateRequest) obj).setParam("collection", accountId);
        } else if (obj instanceof SolrQuery) {
            ((SolrQuery) obj).setParam("collection", accountId);
        }
    }

    @Override
    public synchronized void warmup() {
        // TODO Auto-generated method stub
        super.warmup();
    }

    @Override
    public synchronized  boolean isPendingDelete() {
        // TODO Auto-generated method stub
        return super.isPendingDelete();
    }

    @Override
    public synchronized void setPendingDelete(boolean pendingDelete) {
        // TODO Auto-generated method stub
        super.setPendingDelete(pendingDelete);
    }

    @Override
    public synchronized void optimize() {
        // TODO Auto-generated method stub
        super.optimize();
    }

    @Override
    public synchronized boolean verify(PrintStream out) throws IOException {
        // TODO Auto-generated method stub
        return super.verify(out);
    }

    @Override
    protected synchronized Query optimizeQueryOps(Query query) {
        // TODO Auto-generated method stub
        return super.optimizeQueryOps(query);
    }

    @Override
    protected synchronized String queryToString(Query query) {
        // TODO Auto-generated method stub
        return super.queryToString(query);
    }

    @Override
    protected synchronized String TermsToQuery(Collection<Term> terms) {
        // TODO Auto-generated method stub
        return super.TermsToQuery(terms);
    }

    @Override
    protected synchronized String TermToQuery(Term term) {
        // TODO Auto-generated method stub
        return super.TermToQuery(term);
    }

    @Override
    public synchronized void waitForIndexCommit(SolrServer solrServer)
            throws ServiceException {
        // TODO Auto-generated method stub
        super.waitForIndexCommit(solrServer);
    }

    @Override
    public synchronized long getLatestIndexGeneration(String accountId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return super.getLatestIndexGeneration(accountId);
    }

    @Override
    public synchronized List<Map<String, Object>> fetchFileList(long gen, String accountId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return super.fetchFileList(gen, accountId);
    }

    @Override
    public synchronized EmbeddedSolrServer getSolrServer() throws ServiceException {
        if (server == null) {
            CoreContainer coreContainer = new CoreContainer(solrHome);
            coreContainer.load();
            
            server = new EmbeddedSolrServer(coreContainer, accountId);
            /* We have to "root" the EmbeddedSolrServer in a separate core that will be the
             * core against which other CoreAdminRequests are run. If it does not exist (which it might),
             * we create it
             */
            if (!coreContainer.isLoaded(TEST_CORE_NAME)) {
                Properties props = new Properties();
                props.put("configSet", "zimbra");
                CoreDescriptor cd = new CoreDescriptor(coreContainer, TEST_CORE_NAME, TEST_CORE_NAME, props);
                SolrCore c = coreContainer.create(cd);
                coreContainer.getCoresLocator().create(coreContainer, cd);
             }
            
         }
         return server;
    }

    @VisibleForTesting
    public synchronized EmbeddedSolrServer getEmbeddedServer() throws ServiceException {
        return getSolrServer();
    }

    @Override
    public synchronized void shutdown(SolrServer server) {
    }

    @Override
    protected SolrResponse processRequest(SolrServer server, SolrRequest request) throws SolrServerException, IOException {
        return super.processRequest(server, request);
    }
}

