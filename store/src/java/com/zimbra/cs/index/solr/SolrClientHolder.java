package com.zimbra.cs.index.solr;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

/**
 * Singleton wrapper around a CloudSolrClient
 */
public class SolrClientHolder implements Closeable {

    private static SolrClientHolder instance = null;
    private Map<String, CloudSolrClient> clientMap = new HashMap<>();

    private SolrClientHolder() {

    }

    private CloudSolrClient initClient(String zkHosts) {
        ZimbraLog.search.info("initializing CloudSolrClient for %s", zkHosts);
        ArrayList<String> hosts = new ArrayList<String>();
        for (String s : zkHosts.split(",")) {
            if (s == null || s.isEmpty()) {
                continue;
            }
            if (s.startsWith("http://")) {
                hosts.add(s.substring(7));
            }
            else {
                hosts.add(s);
            }
        }
        CloseableHttpClient client = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
        CloudSolrClient.Builder builder = new CloudSolrClient.Builder();
        builder.withHttpClient(client);
        builder.withClusterStateProvider(new ZkClientClusterStateProvider(hosts, null));
        // Parallel updates under heavy load results in thousands of worker threads.
        // Our updates should only go to one shard anyways, but even if they don't,
        // Async indexing should make disabling this OK.
        builder.withParallelUpdates(LC.solr_client_use_parallel_updates.booleanValue());
        return builder.build();
    }

    public synchronized CloudSolrClient getClient(String zkHosts) {
        CloudSolrClient client = clientMap.get(zkHosts);
        if (client == null ) {
            client = initClient(zkHosts);
            clientMap.put(zkHosts, client);
        }
        return client;
    }

    @Override
    public void close() {
        for (Map.Entry<String, CloudSolrClient> entry: clientMap.entrySet()) {
            ZimbraLog.search.info("closing CloudSolrClient for %s", entry.getKey());
            try {
                entry.getValue().close();
            } catch (IOException e) {
                ZimbraLog.search.warn("error closing CloudSolrClient: %s", e);
            }
        }
    }

    public static synchronized SolrClientHolder getInstance() {
        if (instance == null) {
            instance = new SolrClientHolder();
        }
        return instance;
    }

}
