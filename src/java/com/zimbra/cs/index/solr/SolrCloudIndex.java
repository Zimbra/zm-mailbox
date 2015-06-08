package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkCoreNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.BeansException;

import com.google.common.io.Closeables;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpClientManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;

/**
 *
 * @author Greg Solovyev
 */
public class SolrCloudIndex extends SolrIndexBase {

    private boolean solrCollectionProvisioned = false;
    private CloudSolrClient solrClient = null;
    private SolrCloudIndex(String accountId, CloudSolrClient cloudSolrServer) {
        this.accountId = accountId;
        this.solrClient = cloudSolrServer;
    }

    @Override
    public boolean indexExists() {
        if(!solrCollectionProvisioned) {
            CollectionAdminRequest.List listRequest = new CollectionAdminRequest.List();
            try {
                CollectionAdminResponse resp = listRequest.process(solrClient);
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
    public void initIndex() throws IOException, ServiceException {
        if (!indexExists()) {
            try {
                Server server = Provisioning.getInstance().getLocalServer();
                CollectionAdminRequest.Create createCollectionRequest = new CollectionAdminRequest.Create();
                createCollectionRequest.setCollectionName(accountId);
                createCollectionRequest.setNumShards(1);
                createCollectionRequest.setReplicationFactor(server.getSolrReplicationFactor());
                createCollectionRequest.setMaxShardsPerNode(server.getSolrMaxShardsPerNode());
                createCollectionRequest.setConfigName("zimbra");
                createCollectionRequest.process(solrClient);
            } catch (SolrServerException e) {
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
                CollectionAdminRequest.Delete deleteCollectionRequest = new CollectionAdminRequest.Delete();
                deleteCollectionRequest.setCollectionName(accountId);
                deleteCollectionRequest.process(solrClient);
                solrCollectionProvisioned = false;
            } catch (SolrServerException e) {
                if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("could not find collection") > -1) {
                    //collection has been deleted already
                    solrCollectionProvisioned = false;
                    ZimbraLog.index.warn("Attempting to delete a Solr collection that has been deleted already %s" , accountId);
                } else {
                    ZimbraLog.index.error("Problem deleting Solr collection" , e);
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Problem deleting Solr collection" , e);
            }
        }
    }

    public static final class Factory implements IndexStore.Factory {
        CloudSolrClient cloudSolrServer = null;
        public Factory() throws BeansException, ServiceException {
            ZimbraLog.index.info("Created SolrCloudIndex.Factory\n");
            cloudSolrServer = new CloudSolrClient(
                    Provisioning.getInstance().getLocalServer().getIndexURL().substring(10), 
                        new LBHttpSolrClient(Zimbra.getAppContext().getBean(ZimbraHttpClientManager.class).getInternalHttpClient()));
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
	                 solrCollectionProvisioned = false;
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
            if(!indexExists()) {
                initIndex();
            }
            SolrClient solrServer = getSolrServer();
            UpdateRequest req = new UpdateRequest();
            setupRequest(req, solrServer);
            boolean manualCommit = ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false);
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
                if (manualCommit) {
                    incrementUpdateCounter(solrServer);
                }
                processRequest(solrServer, req);
            } catch (SolrServerException e) {
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
                SolrClient solrServer = getSolrServer();
                UpdateRequest req = new UpdateRequest();
                setupRequest(req, solrServer);
                req.add(solrDoc);
                try {
                    if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                        incrementUpdateCounter(solrServer);
                    }
                    processRequest(solrServer, req);
                } catch (SolrServerException | IOException e) {
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
            SolrClient solrServer = getSolrServer();
            for (Integer id : ids) {
                UpdateRequest req = new UpdateRequest().deleteByQuery(String.format("%s:%d",LuceneFields.L_MAILBOX_BLOB_ID,id));
                setupRequest(req, solrServer);
                try {
                    if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                        incrementUpdateCounter(solrServer);
                    }
                    processRequest(solrServer, req);
                    ZimbraLog.index.debug("Deleted document id=%d", id);
                } catch (SolrServerException e) {
                    if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("no live solrservers available to handle this request") > -1) {
                        ZimbraLog.index.warn("The Collection %s has likely been lost. Account needs re-indexing." , accountId);
                        solrCollectionProvisioned = false;
                    }
                    ZimbraLog.index.error("Problem deleting document with id=%d", id,e);
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
                    solrCollectionProvisioned = false;
                } else if(e != null && e.getMessage() != null && e.getMessage().indexOf("Could not find collection") > -1) {
                    solrCollectionProvisioned = false;
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
            if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                ((UpdateRequest) obj).setAction(ACTION.COMMIT, true, true, true);
                ((UpdateRequest) obj).setParam(UpdateRequest.MIN_REPFACT, String.valueOf(Provisioning.getInstance().getLocalServer().getSolrReplicationFactor()));
            }
        } else if (obj instanceof SolrQuery) {
            ((SolrQuery) obj).setParam(CoreAdminParams.COLLECTION, accountId);
        } else if(obj instanceof ModifiableSolrParams) {
            ((ModifiableSolrParams)obj).set(CoreAdminParams.COLLECTION, accountId);
        }
    }

    @Override
    public SolrClient getSolrServer() throws ServiceException {
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
        //  String leaderURL = null;
        ZkStateReader zkStateReader = null;
        String leaderURL = null;
        try {
            zkStateReader = new ZkStateReader(zkList,15000,15000);
            zkStateReader.createClusterStateWatchersAndUpdate();
            ClusterState clusterState = zkStateReader.getClusterState();
            Collection<Slice> shards = clusterState.getCollection(accountID).getSlices();
            String shardName = "shard1";
            if(!shards.isEmpty()) {
                Slice mainShard = shards.iterator().next();
                shardName = mainShard.getName();
            }
            ZkCoreNodeProps nodeProps = new ZkCoreNodeProps(clusterState.getLeader(accountID, shardName));
            leaderURL = nodeProps.getBaseUrl();
        } catch (InterruptedException | KeeperException e) {
            throw ServiceException.FAILURE("Failed to obtain leader URL from ZooKeeper at " + zkList, e);
        } finally {
            Closeables.closeQuietly(zkStateReader);
        }
        return leaderURL;
    }
}

