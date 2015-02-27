package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.ZimbraIndexSearcher;

//TODO let Solr control batching of documents instead of MailboxIndex class
/**
 * Index adapter for standalone Solr
 * @author gsolovyev
 *
 */
public class SolrIndex extends SolrIndexBase {
    private final CloseableHttpClient httpClient;
    private boolean solrCoreProvisioned = false;
    
    protected SolrIndex(String accountId, CloseableHttpClient httpClient) {
        this.httpClient = httpClient; 
        this.accountId = accountId;
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
        QueryRequest req = new QueryRequest(params);
        @SuppressWarnings("rawtypes")
        NamedList rsp;
        SolrServer solrServer = getSolrServer();
        setupRequest(req, solrServer);
        try {
            ((HttpSolrServer) solrServer).setSoTimeout(60000);
            ((HttpSolrServer) solrServer).setConnectionTimeout(15000);
            rsp = solrServer.request(req);
            version = (Long) rsp.get(GENERATION);
        } catch (SolrServerException | IOException e) {
          throw ServiceException.FAILURE(e.getMessage(),e);
        } finally {
            shutdown(solrServer);
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
        QueryRequest req = new QueryRequest(params);
        SolrServer solrServer = getSolrServer();
        setupRequest(req, solrServer);
        try {
            ((HttpSolrServer) solrServer).setSoTimeout(60000);
            ((HttpSolrServer) solrServer).setConnectionTimeout(15000);
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
        } finally {
            shutdown(solrServer);
        }
    }

    @Override
    public boolean indexExists() {
        if(!solrCoreProvisioned) {
            HttpSolrServer solrServer = null;
            try {
                solrServer = (HttpSolrServer)getSolrServer();
                ((HttpSolrServer)solrServer).setBaseURL(Provisioning.getInstance().getLocalServer().getSolrURLBase());
                CoreAdminResponse resp = CoreAdminRequest.getStatus(accountId, solrServer);
                solrCoreProvisioned = resp.getCoreStatus(accountId).size() > 0;
            } catch (SolrServerException | SolrException e) {
                ZimbraLog.index.info("Solr Core for account %s does not exist", accountId);
            }  catch (IOException e) {
                 ZimbraLog.index.error("failed to check if Solr Core for account %s exists", accountId,e);
            }  catch (ServiceException e) {
                ZimbraLog.index.error("failed to check if Solr Core for account %s exists", accountId,e);
            } finally {
                shutdown(solrServer);
            }
        }
        return solrCoreProvisioned;
    }

    @Override
    public void initIndex() throws IOException, ServiceException {
        SolrServer solrServer = getSolrServer();
        try {
            ((HttpSolrServer)solrServer).setBaseURL(Provisioning.getInstance().getLocalServer().getSolrURLBase());
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set("action", CollectionAction.CREATE.toString());
            params.set("name", accountId);
            params.set("configSet","zimbra");
            SolrRequest req = new QueryRequest(params);
            req.setPath("/admin/cores");
            req.process(solrServer);
            //TODO check for errors
        } catch (SolrServerException e) {
            String errorMsg = String.format("Problem creating new Solr Core for account %s",accountId);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        } catch (RemoteSolrException e) {
            if(e.getMessage() != null && e.getMessage().indexOf("already exists") > 0) {
                return;
            }
            String errorMsg = String.format("Problem creating new Solr Core for account %s",accountId);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        } catch (SolrException e) {
            String errorMsg = String.format("Problem creating new Solr Core for account %s",accountId);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        } catch (IOException e) {
            String errorMsg = String.format("Problem creating new Solr Core for account %s",accountId);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        }  finally {
            shutdown(solrServer);
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
        /*if(!indexExists()) {
            initIndex();
        }*/
        final SolrIndexReader reader = new SolrIndexReader();
        return new SolrIndexSearcher(reader);
    }

    @Override
    public void evict() {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteIndex() throws IOException, ServiceException {
        if(indexExists()) {
            SolrServer solrServer = getSolrServer();
            try {
                ((HttpSolrServer)solrServer).setBaseURL(Provisioning.getInstance().getLocalServer().getSolrURLBase());
                CoreAdminRequest.unloadCore(accountId, true, true, solrServer);
                solrCoreProvisioned = false;
                //TODO check for errors
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Problem deleting Solr Core" , e);
                throw ServiceException.FAILURE("Problem deleting Solr Core",e);
            } catch (IOException e) {
                ZimbraLog.index.error("Problem deleting Solr Core" , e);
                throw e;
            } finally {
                shutdown(solrServer);
            }
        }
    }

    @Override
    public void setupRequest(Object obj, SolrServer solrServer) throws ServiceException {
        ((HttpSolrServer)solrServer).setBaseURL(Provisioning.getInstance().getLocalServer().getSolrURLBase() + "/" + accountId);
    }

    @Override
    public SolrServer getSolrServer() throws ServiceException {
        return new HttpSolrServer(Provisioning.getInstance().getLocalServer().getSolrURLBase() + "/" + accountId, httpClient);
    }

    @Override
    public void shutdown(SolrServer server) {
        if(server != null) {
            server.shutdown();
        }
    }

    public static final class Factory implements IndexStore.Factory {
        PoolingHttpClientConnectionManager cm;
        public Factory() {
            cm = new PoolingHttpClientConnectionManager();
            ZimbraLog.index.info("Created SolrlIndexStore\n");
        }

        @Override
        public SolrIndex getIndexStore(String accountId) {
            return new SolrIndex(accountId, HttpClients.createMinimal(cm));
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
            cm.closeIdleConnections(0, TimeUnit.MILLISECONDS);
        }
    }

    private class SolrIndexer extends SolrIndexBase.SolrIndexer {
        @Override
        public int maxDocs() {
            SolrServer solrServer = null; 
            try {
                solrServer = getSolrServer();
                ((HttpSolrServer)solrServer).setBaseURL(Provisioning.getInstance().getLocalServer().getSolrURLBase());
                CoreAdminResponse resp = CoreAdminRequest.getStatus(accountId, solrServer);
                Iterator<Map.Entry<String, NamedList<Object>>> iter = resp.getCoreStatus().iterator();
                while(iter.hasNext()) {
                    Object maxDocs = resp.getCoreStatus(accountId).findRecursive("index","maxDoc");
                    if(maxDocs != null && maxDocs instanceof Integer) {
                        return (int)maxDocs;
                    }
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Caught IOException retrieving maxDocs for mailbox %s", accountId,e );
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Caught SolrServerException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Caught RemoteSolrException retrieving maxDocs for mailbox %s", accountId,e);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Caught ServiceException retrieving maxDocs for mailbox %s", accountId,e);
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }
        
    }

    public class SolrIndexReader extends SolrIndexBase.SolrIndexReader {
        @Override
        public int numDeletedDocs() {
            SolrServer solrServer = null;
            try {
                solrServer = getSolrServer();
                ((HttpSolrServer)solrServer).setBaseURL(Provisioning.getInstance().getLocalServer().getSolrURLBase());
                CoreAdminResponse resp = CoreAdminRequest.getStatus(accountId, solrServer);
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
    }
}
