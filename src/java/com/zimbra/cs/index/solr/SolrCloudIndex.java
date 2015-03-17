package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpClientManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.util.Zimbra;

public class SolrCloudIndex extends SolrIndexBase {

    private boolean solrCollectionProvisioned = false;
    private CloudSolrServer solrServer = null;
    private SolrCloudIndex(String accountId, CloudSolrServer cloudSolrServer) {
        this.accountId = accountId;
        this.solrServer = cloudSolrServer;
    }

    @Override
    public boolean indexExists() {
        if(!solrCollectionProvisioned) {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set("action", CollectionAction.LIST.toString());
            SolrRequest req = new QueryRequest(params);
            req.setPath("/admin/collections");
            try {
                SolrResponse resp = processRequest(solrServer, req);
                if(resp != null) {
                    NamedList<Object> response = resp.getResponse();
                    Object collectionsObj = response.get("collections");
                    if(collectionsObj != null && collectionsObj instanceof Iterable) {
                        for(String name : (Iterable<String>)collectionsObj) {
                            if(accountId.equalsIgnoreCase(name)) {
                                solrCollectionProvisioned = true; 
                                break;
                            }
                        }
                    } 
                }
            } catch (SolrServerException e) {
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                    ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                } else {
                    ZimbraLog.index.error("Problem checking if Solr collection exists for account %s" ,accountId, e);
                }
            } catch (SolrException e) {
                ZimbraLog.index.info("Solr collection for account %s does not exist", accountId);
            }  catch (IOException e) {
                ZimbraLog.index.error("Problem checking if Solr collection exists for account %s" ,accountId, e);
            }
        }
        return solrCollectionProvisioned;
    }

    @Override
    /**
     * Gets the latest commit version and generation from Solr
     */
    public long getLatestIndexGeneration(String accountId) throws ServiceException {
        long version = 0L;
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(COMMAND, CMD_INDEX_VERSION);
        params.set(CommonParams.WT, "javabin");
        params.set(CommonParams.QT, "/replication");
        params.set("collection", accountId);
        QueryRequest req = new QueryRequest(params);
        setupRequest(req, solrServer);
        @SuppressWarnings("rawtypes")
        NamedList rsp;
        try {
            rsp = solrServer.request(req);
            version = (Long) rsp.get(GENERATION);
        } catch (SolrServerException | IOException e) {
          throw ServiceException.FAILURE(e.getMessage(),e);
        }
        return version;
    }

    /**
     * Fetches the list of index files from Solr using Solr Replication RequestHandler
     * See {@link https://cwiki.apache.org/confluence/display/solr/Index+Replication}
     * @param gen generation of index. Required by Replication RequestHandler
     * @throws BackupServiceException
     */
    @Override
    public List<Map<String, Object>> fetchFileList(long gen, String accountId) throws ServiceException {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(COMMAND, CMD_GET_FILE_LIST);
        params.set(GENERATION, String.valueOf(gen));
        params.set(CommonParams.WT, "javabin");
        params.set(CommonParams.QT, "/replication");
        params.set("collection", accountId);
        QueryRequest req = new QueryRequest(params);
        setupRequest(req, solrServer);
        try {
            @SuppressWarnings("rawtypes")
            NamedList response = solrServer.request(req);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>) response
                    .get(CMD_GET_FILE_LIST);
            if (files != null) {
                return files;
            } else {
                ZimbraLog.index.error("No files to download for index generation: "
                        + gen + " account: " + accountId);
                return Collections.emptyList();
            }
        } catch (SolrServerException | IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
    }
    @Override
    public void initIndex() throws IOException, ServiceException {
        if (!indexExists()) {
            try {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("action", CollectionAction.CREATE.toString());
                params.set("name", accountId);
                //TODO: get global/server config for num shards, configName, replication factor and max shards per node
                params.set("numShards", 1);
                params.set("replicationFactor", 2);
                params.set("maxShardsPerNode", 1);
                params.set("collection.configName","zimbra");
                SolrRequest req = new QueryRequest(params);
                req.setPath("/admin/collections");
                processRequest(solrServer, req);
                //TODO check for errors
            } catch (SolrServerException e) {
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            } catch (RemoteSolrException e) {
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("could not find collection") > -1) {
                    solrCollectionProvisioned = false;
                }
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            } catch (SolrException e) {
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            } catch (IOException e) {
                String errorMsg = String.format("Problem creating new Solr collection for account %s",accountId);
                ZimbraLog.index.error(errorMsg, e);
                throw new IOException(errorMsg,e);
            }

            //TODO: remove this test code. Added, to give ZooKeeper time to update cluster state when running multiple Solr instances on one laptop
            //wait for index to get created
            try {
                for(int i=0;i<5;i++) {
                    if(indexExists()) {
                        return;
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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

    @Override
    public void deleteIndex() throws IOException, ServiceException {
        if (indexExists()) {
            try {
                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("action", CollectionAction.DELETE.toString());
                params.set("name", accountId);
                SolrRequest req = new QueryRequest(params);
                req.setPath("/admin/collections");
                processRequest(solrServer, req);
                solrCollectionProvisioned = false;
            } catch (RemoteSolrException e) {
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("could not find collection") > -1) {
                    //collection has been deleted already
                    solrCollectionProvisioned = false;
                    ZimbraLog.index.warn("Attempting to delete a Solr collection that has been deleted already %s" , accountId);
                } else {
                    ZimbraLog.index.error("Problem deleting Solr collection" , e);
                }
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Problem deleting Solr collection" , e);
            } catch (IOException e) {
                ZimbraLog.index.error("Problem deleting Solr collection" , e);
            }
        }
    }

    public static final class Factory implements IndexStore.Factory {
        CloudSolrServer cloudSolrServer = null;
        public Factory() {
            ZimbraLog.index.info("Created SolrCloudIndex.Factory\n");
        }

        @Override
        public SolrIndexBase getIndexStore(String accountId) throws ServiceException {
            //TODO set zk timeout from LDAP config
            cloudSolrServer = new CloudSolrServer(
                    Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraSolrURLBase, true), 
                        new LBHttpSolrServer(Zimbra.getAppContext().getBean(ZimbraHttpClientManager.class).getInternalHttpClient()));
            return new SolrCloudIndex(accountId,  cloudSolrServer);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
            cloudSolrServer.shutdown();
            ZimbraLog.index.info("Destroyed SolrCloudIndex.Factory\n");
        }
    }

    private class SolrIndexer extends SolrIndexBase.SolrIndexer {

        @Override
        public int maxDocs() {
            SolrServer solrServer = null;
            try {
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
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                    ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                } else {
                    ZimbraLog.index.error("Caught SolrServerException retrieving maxDocs for mailbox %s", accountId,e);
                }
            } catch (RemoteSolrException e) {
                if(e != null && e.getMessage() != null && e.getMessage().indexOf("Could not find collection") > -1) {
                    solrCollectionProvisioned = false;
                }
                ZimbraLog.index.error("Caught RemoteSolrException retrieving maxDocs for mailbox %s", accountId,e);
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
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                    ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                    solrCollectionProvisioned = false;
                }
                ZimbraLog.index.error("Problem indexing documents", e);
            } 
        }

        @Override
        public void addDocument(MailItem item, List<IndexDocument> docs) throws ServiceException {
            if (docs == null || docs.isEmpty()) {
                return;
            }
            try {
                if(!indexExists()) {
                    initIndex();
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index mail item with ID %d for Account %s ", item.getId(), accountId), e);
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
                try {
                    processRequest(solrServer, req);
                } catch (SolrServerException | RemoteSolrException | IOException e) {
                    if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                        ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                        solrCollectionProvisioned = false;
                    } 
                    throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index part %d of Mail Item with ID %d for Account %s ", partNum, item.getId(), accountId), e);
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
                    try {
                        processRequest(solrServer, req);
                        ZimbraLog.index.debug("Deleted document id=%d", id);
                    } catch (SolrServerException e) {
                        if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                            ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                            solrCollectionProvisioned = false;
                        } 
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
        protected void incrementUpdateCounter(SolrServer solrServer)
                throws ServiceException {
            //Do nothing. This is not supported by SolrCloud
        }

    }

    public class SolrIndexReader extends SolrIndexBase.SolrIndexReader {
        @Override
        public int numDeletedDocs() {
            SolrServer solrServer = null;
            try {
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
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                    ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                    solrCollectionProvisioned = false;
                } else {
                    ZimbraLog.index.error("Caught SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
                }
            }  catch (RemoteSolrException e) {
                if(e != null && e.getMessage() != null && e.getMessage().indexOf("Could not find collection") > -1) {
                    solrCollectionProvisioned = false;
                }
                ZimbraLog.index.error("Caught SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
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

    @Override
    public SolrServer getSolrServer() throws ServiceException {
        return solrServer;
    }

    @Override
    public void shutdown(SolrServer server) {
        //do nothing. CloudSolrServer is thread safe and should not be shut down after each request
    }
    
    @Override
    public int waitForIndexCommit(int maxWaitTimeMillis)
            throws ServiceException {
        return maxWaitTimeMillis;
    }
}

