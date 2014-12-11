package com.zimbra.cs.index.solr;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;

/**
 * Embedded SOLR server used for testing
 * @author iraykin
 *
 */
public class EmbeddedSolrIndex  extends SolrIndexBase {
    private static EmbeddedSolrServer server = null;
    private CoreContainer coreContainer = null;
    private static final String solrHome = "../ZimbraServer/build/test/solr/";
    private static ReentrantLock lock = new ReentrantLock(true);
    private static volatile String lockReason = null;

    private EmbeddedSolrIndex(String accountId) {
        System.setProperty("solr.allow.unsafe.resourceloading", "true");
        this.accountId = accountId;
    }

    private CoreContainer getCoreContainer() {
        if (coreContainer == null) {
            coreContainer = new CoreContainer(solrHome);
        }
        return coreContainer;
    }
    @Override
    public boolean indexExists() {
            File f = new File(solrHome, accountId);
            return f.exists();
    }

    @Override
    public synchronized void initIndex() throws IOException, ServiceException {
        if (!indexExists()) {
            CoreContainer container = getSolrServer().getCoreContainer();
            Properties props = new Properties();
            props.put("configSet", "zimbra");
            CoreDescriptor cd = new CoreDescriptor(container, accountId, accountId, props);
            container.getCoresLocator().create(container, cd);
            try {
                SolrCore c = container.create(cd);
                //container.register(accountId, c, false);
            } catch (SolrException e) {
                //already may be created by another thread
            }
        }
    }

    @Override
    public Indexer openIndexer() throws IOException, ServiceException {
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
    public void evict() {
        // TODO Auto-generated method stub

    }

    private static void lock(String reason) {
        lockReason = reason;
        lock.lock();
    }

    private static synchronized void unlock() {
        lockReason = null;
        lock.unlock();
    }

    @Override
    public synchronized void deleteIndex() throws IOException, ServiceException {
        if (indexExists()) {
            lock("deleting index");
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
                unlock();
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
        public void destroy() {
            lock("destroying");
            try {
                if (server != null) {
                    server.shutdown();
                    try {
                        FileUtils.deleteDirectory(new File("../ZimbraServer/build/test/solr/solrtestcore/"));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    server = null;
                }
            } finally {
                unlock();
            }
        }
    }

    private class SolrIndexer extends SolrIndexBase.SolrIndexer {

        @Override
        public void close() throws IOException {
        }

        @Override
        protected void setAction(UpdateRequest req) {
            req.setAction(ACTION.COMMIT, false, true, false);
        }

        @Override
        public int maxDocs() {
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
                ZimbraLog.index.error("Cought IOException retrieving maxDocs for mailbox %s", accountId,e );
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Cought SolrServerException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Cought RemoteSolrException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Cought ServiceException retrieving maxDocs for mailbox %s", accountId,e );
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }

        @Override
        public void add(List<IndexItemEntry> entries) throws IOException, ServiceException {
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
                processRequest(solrServer, req);
            } catch (RemoteSolrException | SolrServerException e) {
                ZimbraLog.index.error("Problem indexing documents", e);
            }
        }
    }

    public class SolrIndexReader extends SolrIndexBase.SolrIndexReader {
        @Override
        public int numDeletedDocs() {
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
                ZimbraLog.index.error("Cought IOException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Cought SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Cought SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Cought ServiceException retrieving number of deleted documents in mailbox %s", accountId,e);
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }
    }


    @Override
    public void setupRequest(Object obj, SolrServer solrServer) {
        if (obj instanceof UpdateRequest) {
            ((UpdateRequest) obj).setParam("collection", accountId);
        } else if (obj instanceof SolrQuery) {
            ((SolrQuery) obj).setParam("collection", accountId);
        }
    }

    private static synchronized EmbeddedSolrServer getServerInstance() {
           if (server == null) {
               lock("starting server");
               try {
                   CoreContainer coreContainer = new CoreContainer(solrHome);
                   coreContainer.load();
                   server = new EmbeddedSolrServer(coreContainer, "solrtestcore");
                   /* We have to "root" the EmbeddedSolrServer in a separate core that will be the
                    * core against which other CoreAdminRequests are run. If it does not exist (which it might),
                    * we create it
                    */
                   if (!coreContainer.isLoaded("solrtestcore")) {
                       Properties props = new Properties();
                       props.put("configSet", "zimbra");
                       CoreDescriptor cd = new CoreDescriptor(coreContainer, "solrtestcore", "solrtestcore", props);
                       SolrCore c = coreContainer.create(cd);
                       coreContainer.getCoresLocator().create(coreContainer, cd);
                      // coreContainer.register("solrtestcore", c, false);
                    }
               } finally {
                   unlock();
               }
            }
            return server;
    }

    @Override
    public EmbeddedSolrServer getSolrServer() throws ServiceException {
        return getServerInstance();
    }

    @VisibleForTesting
    public EmbeddedSolrServer getEmbeddedServer() throws ServiceException {
        return getSolrServer();
    }

    @Override
    public void shutdown(SolrServer server) {
    }

    @Override
    protected SolrResponse processRequest(SolrServer server, SolrRequest request) throws SolrServerException, IOException {
        lock("request");
        try {
            return super.processRequest(server, request);
        } finally {
            unlock();
        }
    }
}

