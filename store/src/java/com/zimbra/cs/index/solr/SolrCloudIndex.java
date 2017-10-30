package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CollectionAdminRequest.Create;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.ClusterStateUtil;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkCoreNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.zookeeper.KeeperException;

import com.google.common.io.Closeables;
import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.util.ProvisioningUtil;

/**
 *
 * @author Greg Solovyev
 */
public class SolrCloudIndex extends SolrIndexBase {

    private CloudSolrClient solrClient = null;
    private SolrCloudIndex(String accountId, CloudSolrClient cloudSolrServer) {
        this.accountId = accountId;
        this.solrClient = cloudSolrServer;
    }

    @Override
    public Indexer openIndexer() throws IOException, ServiceException {
        return new SolrIndexer();
    }

    @Override
    public ZimbraIndexSearcher openSearcher() throws IOException, ServiceException {
        final SolrIndexReader reader = new SolrIndexReader();
        return new SolrIndexSearcher(reader);
    }

    @Override
    public void evict() {
        // Solr-based indexing backends don't need to do anything here
    }

    @Override
    public void deleteIndex() throws IOException, ServiceException {
        SolrUtils.deleteCloudIndex(solrClient, accountId);
    }

    public static final class Factory implements IndexStore.Factory {
        CloudSolrClient cloudSolrServer = null;
        public Factory() throws ServiceException {
            ZimbraLog.index.info("Created SolrCloudIndex.Factory\n");
            String zkHost = Provisioning.getInstance().getLocalServer().getIndexURL().substring("solrcloud:".length());
            cloudSolrServer = SolrUtils.getCloudSolrClient(zkHost);
        }

        @Override
        public SolrIndexBase getIndexStore(String accountId) throws ServiceException {
            return new SolrCloudIndex(accountId,  cloudSolrServer);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
            try {
                cloudSolrServer.close();
            } catch (IOException e) {
                ZimbraLog.index.error("Cought an exception trying to close ClourSolrClient instance", e);
            }
            ZimbraLog.index.info("Destroyed SolrCloudIndex.Factory\n");
        }
    }

    private class SolrIndexer extends SolrIndexBase.SolrIndexer {

        @Override
        public int maxDocs() {
            SolrClient solrServer = null;
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
	        	 if(e != null && e.getMessage() != null && e.getMessage().indexOf("Could not find collection") > -1) {
	             } else if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                    ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                } else {
                    ZimbraLog.index.error("Caught SolrServerException retrieving maxDocs for mailbox %s", accountId,e);
                }
            }
            return 0;
        }

        @Override
        public void add(List<Mailbox.IndexItemEntry> entries) throws IOException, ServiceException {
            CloudSolrClient solrServer = getSolrServer();
            UpdateRequest req = new UpdateRequest();
            setupRequest(req, solrServer);
            for (IndexItemEntry entry : entries) {
                if (entry.getDocuments() == null) {
                    ZimbraLog.index.warn("NULL index data item=%s", entry);
                    continue;
                }
                int partNum = 1;
                for (IndexDocument doc : entry.getDocuments()) {
                    SolrInputDocument solrDoc;
                    // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                    synchronized (doc) {
                        setFields(entry.getItem(), doc);
                        solrDoc = doc.toInputDocument();
                        solrDoc.addField(SOLR_ID_FIELD, String.format("%d_%d",entry.getItem().getId(),partNum));
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
            } catch (SolrServerException e) {
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                    ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                }
                ZimbraLog.index.error("Problem indexing documents", e);
            }
        }

        @Override
        public void addDocument(MailItem item, List<IndexDocument> docs) throws ServiceException {
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
                SolrClient solrServer = getSolrServer();
                UpdateRequest req = new UpdateRequest();
                setupRequest(req, solrServer);
                req.add(solrDoc);
                try {
                    processRequest(solrServer, req);
                } catch (SolrServerException | IOException e) {
                    if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                        ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                    }
                    throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index part %d of Mail Item with ID %d for Account %s ", partNum, item.getId(), accountId), e);
                }
            }
        }

        @Override
        public void deleteDocument(List<Integer> ids) throws IOException,ServiceException {
            deleteDocument(ids, LuceneFields.L_MAILBOX_BLOB_ID);
        }

        @Override
        public void addSearchHistoryDocument(IndexDocument doc) throws IOException, ServiceException {
            SolrClient solrServer = getSolrServer();
            UpdateRequest req = new UpdateRequest();
            setupRequest(req, solrServer);
            SolrInputDocument solrDoc=  doc.toInputDocument();
            String searchId = (String) solrDoc.getFieldValue(LuceneFields.L_SEARCH_ID);
            solrDoc.addField(SOLR_ID_FIELD, String.format("sh_%s", searchId));
            req.add(solrDoc);
            try {
                processRequest(solrServer, req);
            } catch (SolrServerException e) {
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                    ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                }
                throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index document for account %s", accountId), e);
            }
        }

        @Override
        public void deleteDocument(List<Integer> ids, String fieldName)
                throws IOException, ServiceException {
            SolrClient solrServer = getSolrServer();
            for (Integer id : ids) {
                UpdateRequest req = new UpdateRequest().deleteByQuery(String.format("%s:%d",fieldName, id));
                setupRequest(req, solrServer);
                try {
                    processRequest(solrServer, req);
                    ZimbraLog.index.debug("Deleted document with field %s=%d", fieldName, id);
                } catch (SolrServerException e) {
                    if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                        ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                    }
                    ZimbraLog.index.error("Problem deleting document with field %s=%d",fieldName, id,e);
                }
            }
        }
    }

    public class SolrIndexReader extends SolrIndexBase.SolrIndexReader {
        @Override
        public int numDeletedDocs() {
            SolrClient solrServer = null;
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
                } else if(e != null && e.getMessage() != null && e.getMessage().indexOf("Could not find collection") > -1) {
                } else {
                    ZimbraLog.index.error("Caught SolrServerException retrieving number of deleted documents in mailbox %s", accountId,e);
                }
            }
            return 0;
        }
    }


    @Override
    public void setupRequest(Object obj, SolrClient solrServer) throws ServiceException {
        if (obj instanceof UpdateRequest) {
            ((UpdateRequest) obj).setParam(CoreAdminParams.COLLECTION, accountId);
        } else if (obj instanceof SolrQuery) {
            ((SolrQuery) obj).setParam(CoreAdminParams.COLLECTION, accountId);
        } else if(obj instanceof ModifiableSolrParams) {
            ((ModifiableSolrParams)obj).set(CoreAdminParams.COLLECTION, accountId);
        }
    }

    @Override
    public CloudSolrClient getSolrServer() throws ServiceException {
        return solrClient;
    }

    @Override
    public void shutdown(SolrClient server) {
        //do nothing. CloudSolrServer is thread safe and should not be shut down after each request
    }

    /**
     * Utility method. Returns the URL of the leader replica for given account and given zookeeper URL list
     * @param zkList
     * @param accountID
     * @return
     * @throws ServiceException
     */
    public static String getLeaderURL(String zkList, String accountID) throws ServiceException {
        ZkStateReader zkStateReader = null;
        String leaderURL = null;
        try {
            int timeout = (int) ProvisioningUtil.getTimeIntervalServerAttribute(Provisioning.A_zimbraZKClientTimeout, 15000L);
            zkStateReader = new ZkStateReader(zkList, timeout, timeout);
            zkStateReader.createClusterStateWatchersAndUpdate();
            ClusterState clusterState = zkStateReader.getClusterState();
            Collection<Slice> shards = clusterState.getCollection(accountID).getSlices();
            String shardName = "shard1";
            if(!shards.isEmpty()) {
                Slice mainShard = shards.iterator().next();
                shardName = mainShard.getName();
            }
            DocCollection coll = clusterState.getCollection(accountID);
            ZkCoreNodeProps nodeProps = new ZkCoreNodeProps(coll.getLeader(shardName));
            leaderURL = nodeProps.getBaseUrl();
        } catch (InterruptedException | KeeperException e) {
            throw ServiceException.FAILURE("Failed to obtain leader URL from ZooKeeper at " + zkList, e);
        } finally {
            Closeables.closeQuietly(zkStateReader);
        }
        return leaderURL;
    }

    @Override
    protected SolrResponse processRequest(SolrClient client, SolrRequest request) throws SolrServerException, IOException, ServiceException{
        return SolrUtils.executeCloudRequestWithRetry((CloudSolrClient) client, request, accountId, CONFIG_SET);
    }
}

