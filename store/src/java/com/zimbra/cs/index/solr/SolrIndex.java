package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
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

/**
 * Index adapter for standalone Solr
 * @author gsolovyev
 *
 */
public class SolrIndex extends SolrIndexBase {
    private final CloseableHttpClient httpClient;

    protected String getBaseURL() throws ServiceException {
        return Provisioning.getInstance().getLocalServer().getIndexURL().substring("solr:".length());
    }

    protected SolrIndex(String accountId, CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.accountId = accountId;
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
        //nothing to do here
    }

    @Override
    public void deleteIndex() throws IOException, ServiceException {
        SolrClient solrServer = getSolrServer();
        try {
            ((HttpSolrClient)solrServer).setBaseURL(getBaseURL());
            CoreAdminRequest.unloadCore(accountId, true, true, solrServer);
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

    @Override
    public void setupRequest(Object obj, SolrClient solrServer) throws ServiceException {
        ((HttpSolrClient)solrServer).setBaseURL(getBaseURL() + "/" + accountId);
    }

    @Override
    public SolrClient getSolrServer() throws ServiceException {
        return SolrUtils.getSolrClient(httpClient, getBaseURL(), accountId);
    }

    @Override
    public void shutdown(SolrClient server) {
        if(server != null) {
            try {
                server.close();
            } catch (IOException e) {
                ZimbraLog.index.error("Cought an exception trying to close SolrClient instance", e);
            }
        }
    }

    public static final class Factory implements IndexStore.Factory {
        public Factory() {
            ZimbraLog.index.info("Created SolrlIndexStore.Factory\n");
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
            ZimbraLog.index.info("Destroyed SolrlIndexStore.Factory\n");
        }

        @Override
        public IndexStore getIndexStore(String accountId) throws ServiceException {
            CloseableHttpClient client = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
            return new SolrIndex(accountId, client);
        }
    }

    private class SolrIndexer extends SolrIndexBase.SolrIndexer {
        @Override
        public void add(List<Mailbox.IndexItemEntry> entries) throws IOException, ServiceException {
            SolrClient solrServer = getSolrServer();
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
                        solrDoc.addField(SOLR_ID_FIELD, String.format("%d_%d",entry.getItem().getId(), partNum));
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
                ZimbraLog.index.error("Problem indexing documents", e);
            }  finally {
                shutdown(solrServer);
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
                    throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index part %d of Mail Item with ID %d for Account %s ", partNum, item.getId(), accountId), e);
                } finally {
                    shutdown(solrServer);
                }
            }
        }

        @Override
        public void deleteDocument(List<Integer> ids) throws IOException,ServiceException {
            deleteDocument(ids, LuceneFields.L_MAILBOX_BLOB_ID);
        }

        @Override
        public int maxDocs() {
            SolrClient solrServer = null;
            try {
                solrServer = getSolrServer();
                ((HttpSolrClient)solrServer).setBaseURL(getBaseURL());
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
            } catch (SolrServerException | IOException e) {
                throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index document for account %s", accountId), e);
            } finally {
                shutdown(solrServer);
            }
        }

        @Override
        public void deleteDocument(List<Integer> ids, String fieldName) throws IOException, ServiceException {
            SolrClient solrServer = getSolrServer();
            try {
                for (Integer id : ids) {
                    UpdateRequest req = new UpdateRequest().deleteByQuery(String.format("%s:%d", fieldName ,id));
                    setupRequest(req, solrServer);
                    try {
                        processRequest(solrServer, req);
                        ZimbraLog.index.debug("Deleted document with field %s=%d",fieldName, id);
                    } catch (SolrServerException e) {
                        ZimbraLog.index.error("Problem deleting document with field %s=%d",fieldName, id,e);
                    }
                }
            } finally {
                shutdown(solrServer);
            }

        }

    }

    public class SolrIndexReader extends SolrIndexBase.SolrIndexReader {
        @Override
        public int numDeletedDocs() {
            SolrClient solrServer = null;
            try {
                solrServer = getSolrServer();
                ((HttpSolrClient)solrServer).setBaseURL(getBaseURL());
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

    @Override
    protected SolrResponse processRequest(SolrClient server, SolrRequest request)
            throws SolrServerException, IOException, ServiceException {
        return SolrUtils.executeRequestWithRetry(server, request, getBaseURL(), accountId, CONFIG_SET);
    }
}
