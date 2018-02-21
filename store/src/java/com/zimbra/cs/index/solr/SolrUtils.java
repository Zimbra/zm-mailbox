package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CollectionAdminRequest.Create;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ClusterStateUtil;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.SolrIndex.IndexType;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.RetryUtil;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry.Command;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry.ExceptionHandler;
import com.zimbra.cs.util.RetryUtil.RequestWithRetry.OnFailureAction;

public class SolrUtils {
    private static final Pattern whitespace = Pattern.compile("\\s");
    private static final Pattern wildcard = Pattern.compile("(?<!\\\\)\\*");
    private static final String[] specialChars = new String[]{"&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "~", "?", ":"};

    public enum WildcardEscape {
        ALL, NONE, ZIMBRA
    }

    public static boolean isWildcardQuery(String text) {
        return wildcard.matcher(text).find();
    }

    public static boolean containsWhitespace(String text) {
        return whitespace.matcher(text).find();
    }

    public static String escapeSpecialChars(String text, WildcardEscape wildcards) {
        if (!containsWhitespace(text)) {
            for (int i = 0; i < specialChars.length; i++) {
                String c = specialChars[i];
                text = text.replace(c, "\\" + c);
            }
        }

        if (wildcards == WildcardEscape.ZIMBRA) {
            //escape asterisk if it's not at a valid wildcard location
            int index = text.indexOf("*");
            while (index >= 0) {
                if (index != 0 && index != text.length() - 1 && !Character.isWhitespace(text.charAt(index + 1))) {
                    text = text.substring(0, index) + "\\" + text.substring(index, text.length());
                    index++;
                }
                index = text.indexOf("*", index + 1);
            }
        } else if (wildcards == WildcardEscape.ALL) {
            text = text.replace("*", "\\*");
        }

        return text;
    }

    public static String escapeQuotes(String text) {
        return text.replace("\"", "\\\"");
    }

    public static String quoteText(String text) {
        return "\"" + escapeQuotes(text) + "\"";
    }

    private static void shutdownSolrClient(SolrClient client) {
        try {
            client.close();
        } catch (IOException e) {
            ZimbraLog.index.error("Cought an exception trying to close SolrClient instance", e);
        }
    }

    public static boolean indexExists(SolrClient client, String baseUrl, String coreName) {

        int maxTries = 1;
        try {
            maxTries = Provisioning.getInstance().getLocalServer().getSolrMaxRetries()+1;
        } catch (ServiceException e) {
            maxTries = 1;
        }
        boolean coreProvisioned = false;
        while(maxTries-- > 0 && !coreProvisioned) {
            HttpSolrClient solrServer = null;
            try {
                solrServer = (HttpSolrClient) client;
                solrServer.setBaseURL(baseUrl);
                CoreAdminResponse resp = CoreAdminRequest.getStatus(coreName, solrServer);
                coreProvisioned = resp.getCoreStatus(coreName).size() > 0;
                if (coreProvisioned) {
                    break;
                }
            } catch (SolrServerException | SolrException |IOException e) {
                if(e.getCause() instanceof NoHttpResponseException) {
                    solrServer.getHttpClient().getConnectionManager().closeExpiredConnections();
                }
                ZimbraLog.index.info("Solr Core for account %s does not exist", coreName);
            } finally {
                shutdownSolrClient(solrServer);
            }
        }
        return coreProvisioned;
    }

    public static boolean createStandaloneIndex(SolrClient client, String baseUrl, String coreName, String configSet) throws ServiceException {
        boolean coreProvisioned = false;
        try {
            ((HttpSolrClient)client).setBaseURL(baseUrl);
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set(CoreAdminParams.ACTION, CollectionAction.CREATE.toString());
            params.set(CoreAdminParams.NAME, coreName);
            params.set(CoreAdminParams.CONFIGSET, configSet);
            SolrRequest req = new QueryRequest(params);
            req.setPath("/admin/cores");
            req.process(client);
            //TODO check for errors
            ZimbraLog.index.info("Created Solr core %s", coreName);
        } catch (SolrServerException e) {
            String errorMsg = String.format("Problem creating new Solr Core %s", coreName);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        } catch (RemoteSolrException e) {
            if(e.getMessage() != null && e.getMessage().indexOf("already exists") > 0) {
                return true;
            }
            if(e.getMessage() != null && e.getMessage().indexOf("Lock obtain timed out") > -1) {
                //another thread is trying to provision the same core on a single-node Solr server
                long maxWait = ProvisioningUtil.getTimeIntervalServerAttribute(Provisioning.A_zimbraIndexReplicationTimeout, 20000L);
                long pollInterval = ProvisioningUtil.getTimeIntervalServerAttribute(Provisioning.A_zimbraIndexPollingInterval, 500L);
                while (!indexExists(client, baseUrl, coreName) && maxWait > 0) {
                    try {
                        Thread.sleep(pollInterval);
                        maxWait-=pollInterval;
                    } catch (InterruptedException e1) {
                        break;
                    }
                }
                if(coreProvisioned) {
                    return true;
                }
            }
            String errorMsg = String.format("Problem creating new Solr Core %s", coreName);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        } catch (SolrException | IOException e) {
            String errorMsg = String.format("Problem creating new Solr Core for %s", coreName);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        }  finally {
            shutdownSolrClient(client);
        }
        coreProvisioned = true;
        return coreProvisioned;
    }

    public static boolean createCloudIndex(CloudSolrClient client, String collectionName, String configSet, int numShards) throws ServiceException {
        boolean solrCollectionProvisioned = false;
        Server server = Provisioning.getInstance().getLocalServer();
        int replicationFactor = server.getSolrReplicationFactor();
        try {
            Create createCollectionRequest = CollectionAdminRequest.createCollection(collectionName, configSet, numShards, replicationFactor);
            createCollectionRequest.setMaxShardsPerNode(server.getSolrMaxShardsPerNode());
            createCollectionRequest.setRouterField(LuceneFields.SOLR_ID);
            createCollectionRequest.process(client);
        } catch (SolrServerException e) {
            if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("could not find collection") > -1) {
                solrCollectionProvisioned = false;
            }
            String errorMsg = String.format("Problem creating new Solr collection '%s'", collectionName);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg,e);
        } catch (SolrException e) {
            if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("collection already exists") > -1) {
                //it is possible that another mailstore has initialized this collection at ths same time
                ZimbraLog.index.debug("Collection with name '%s' already exists. Will not attempt to recreate it.", collectionName);
            } else  {
                String errorMsg = String.format("Problem creating new Solr collection '%s'", collectionName);
                ZimbraLog.index.error(errorMsg, e);
                throw ServiceException.FAILURE(errorMsg,e);
            }
        } catch (IOException e) {
            String errorMsg = String.format("Problem creating new Solr collection '%s'", collectionName);
            ZimbraLog.index.error(errorMsg, e);
            throw ServiceException.FAILURE(errorMsg, e);
        }

        //wait for index to get propagated (effectively polls clusterstatus.json)
        //
        int timeout = (int) server.getIndexReplicationTimeout() / 1000;
        solrCollectionProvisioned = ClusterStateUtil.waitForLiveAndActiveReplicaCount(client.getZkStateReader(),
                collectionName, replicationFactor, timeout);
        if(!solrCollectionProvisioned) {
            ZimbraLog.index.error("Could not confirm that all nodes for collection '%s' are provisioned", collectionName);
        }
        return solrCollectionProvisioned;
    }

    public static CloudSolrClient getCloudSolrClient(String zkHost) {
        if (zkHost.startsWith("http://")) {
            zkHost = zkHost.substring(7); //trim URL scheme
        }
        CloseableHttpClient client = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
        CloudSolrClient.Builder builder = new CloudSolrClient.Builder();
        builder.withHttpClient(client);
        builder.withClusterStateProvider(new ZkClientClusterStateProvider(zkHost));
        return builder.build();
    }

    public static SolrClient getSolrClient(CloseableHttpClient httpClient, String baseUrl, String coreName) {
        HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
        builder.withBaseSolrUrl(baseUrl + "/" + coreName);
        builder.withHttpClient(httpClient);
        return builder.build();
    }

    public static SolrResponse executeRequestWithRetry(String accountId, SolrClient client, SolrRequest req, String baseUrl, String coreName, IndexType indexType) throws ServiceException {
        Command<SolrResponse> command = new RetryUtil.RequestWithRetry.Command<SolrResponse>() {

            @Override
            public SolrResponse execute() throws Exception {
                return req.process(client);
            }
        };

        OnFailureAction onFailure = new RetryUtil.RequestWithRetry.OnFailureAction() {

            @Override
            protected void run() throws ServiceException {
                HttpSolrClient solrClient = (HttpSolrClient) client;
                String origBaseUrl = solrClient.getBaseURL();
                try {
                    createStandaloneIndex(client, baseUrl, coreName, getConfigSetName(indexType));
                } finally {
                    solrClient.setBaseURL(origBaseUrl);
                }
            }
        };

        ExceptionHandler exceptionHandler = new RetryUtil.RequestWithRetry.ExceptionHandler() {

            @Override
            public boolean exceptionMatches(Exception e) {
                return ((e instanceof RemoteSolrException) && e.getMessage().toLowerCase().contains("not found"));
            }
        };

        RequestWithRetry<SolrResponse> request = new RetryUtil.RequestWithRetry<SolrResponse>(command, exceptionHandler, onFailure);
        return request.execute();
    }

    public static SolrResponse executeCloudRequestWithRetry(String accountId, CloudSolrClient client, SolrRequest req, String collectionName, IndexType indexType) throws ServiceException {
        Command<SolrResponse> command = new RetryUtil.RequestWithRetry.Command<SolrResponse>() {

            @Override
            public SolrResponse execute() throws Exception {
                return req.process(client);
            }
        };

        OnFailureAction onFailure = new RetryUtil.RequestWithRetry.OnFailureAction() {

            @Override
            protected void run() throws ServiceException {
                createCloudIndex(client, collectionName, getConfigSetName(indexType), getInitialNumShards(accountId, indexType));
            }
        };

        ExceptionHandler exceptionHandler = new RetryUtil.RequestWithRetry.ExceptionHandler() {

            @Override
            public boolean exceptionMatches(Exception e) {
                return ((e instanceof SolrException) && e.getMessage().toLowerCase().contains("collection not found"));
            }
        };

        RequestWithRetry<SolrResponse> request = new RetryUtil.RequestWithRetry<SolrResponse>(command, exceptionHandler, onFailure);
        return request.execute();
    }

    public static void deleteCloudIndex(CloudSolrClient client, String collectionName) throws ServiceException {
        try {
            CollectionAdminRequest.Delete deleteCollectionRequest = CollectionAdminRequest.deleteCollection(collectionName);
            deleteCollectionRequest.process(client);
        } catch (SolrServerException e) {
            if(e != null && e.getMessage() != null && e.getMessage().toLowerCase().indexOf("could not find collection") > -1) {
                //collection has been deleted already
                ZimbraLog.index.warn("Attempting to delete a Solr collection '%s' that has been deleted already" , collectionName);
            } else {
                String error = String.format("Problem deleting Solr collection '%s'", collectionName);
                ZimbraLog.index.error(error, e);
                throw ServiceException.FAILURE(error, e);
            }
        } catch (IOException | RemoteSolrException e) {
            ZimbraLog.index.error("Problem deleting Solr collection" , e);
        }
    }

    public static void deleteStandaloneIndex(SolrClient client, String baseUrl, String coreName) throws ServiceException {
        try {
            ((HttpSolrClient)client).setBaseURL(baseUrl);
            CoreAdminRequest.unloadCore(coreName, true, true, client);
        } catch (SolrServerException | IOException e) {
            ZimbraLog.index.error("Problem deleting Solr Core" , e);
            throw ServiceException.FAILURE("Problem deleting Solr Core", e);
        } finally {
            shutdownSolrClient(client);
        }
    }

    public static String getTermsFilter(String field, Collection<String> values) {
        return String.format("{!terms f=%s}%s", field, Joiner.on(",").join(values));
    }
    public static String getTermsFilter(String field, String... values) {
        return String.format("{!terms f=%s}%s", field, Joiner.on(",").join(values));
    }

    public static String getAccountFilter(String accountId) {
        return getTermsFilter(LuceneFields.L_ACCOUNT_ID, accountId);
    }

    private static String getConfigSetName(IndexType indexType) {
        switch (indexType) {
        case EVENTS:
            return SolrConstants.CONFIGSET_EVENTS;
        case MAILBOX:
        default:
            return SolrConstants.CONFIGSET_INDEX;
        }
    }

    private static int getInitialNumShards(String accountId, IndexType indexType) throws ServiceException {
        switch (indexType) {
        case EVENTS:
            return getIntSolrAttr(accountId, Provisioning.A_zimbraMailboxIndexInitialNumShards);
        case MAILBOX:
        default:
            return getIntSolrAttr(accountId, Provisioning.A_zimbraEventIndexInitialNumShards);
        }
    }

    private static int getIntSolrAttr(String accountId, String attrName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccountById(accountId);
        int attrValue = account.getIntAttr(attrName, 0);
        if (attrValue == 0) {
            attrValue = prov.getLocalServer().getIntAttr(attrName, 0);
            if (attrValue == 0) {
                throw ServiceException.FAILURE(attrName + " is not set on account, cos, domain or server", null);
            }
        }
        return attrValue;
    }

    private static String getStringSolrAttr(String accountId, String attrName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccountById(accountId);
        String attrValue = account.getAttr(attrName);
        if (Strings.isNullOrEmpty(attrValue)) {
            attrValue = prov.getLocalServer().getAttr(attrName);
            if (Strings.isNullOrEmpty(attrValue)) {
                throw ServiceException.FAILURE(attrName + " is not set on account, cos, domain or server", null);
            }
        }
        return attrValue;
    }

    public static String getEventIndexName(String accountId) throws ServiceException {
        return getStringSolrAttr(accountId, Provisioning.A_zimbraEventIndexName);
    }

    public static String getMailboxIndexName(String accountId) throws ServiceException {
        return getStringSolrAttr(accountId, Provisioning.A_zimbraMailboxIndexName);
    }
}
