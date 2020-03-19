package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.util.Zimbra;

public class BatchedIndexDeletions {

    private static BatchedIndexDeletions instance = null;
    private DeletionProvider deletionProvider;

    public static BatchedIndexDeletions getInstance() {
        if (instance == null) {
            synchronized (BatchedIndexDeletions.class) {
                instance = new BatchedIndexDeletions();
            }
        }
        return instance;
    }

    private BatchedIndexDeletions() {
        deletionProvider = new RedisDeletionProvider();
    }

    public void addDeletion(Deletion deletion) {
        deletionProvider.addDeletion(deletion);
    }

    public static abstract class Deletion {

        private String collection;
        private String accountId;

        public Deletion(String collection, String accountId) {
            this.collection = collection;
            this.accountId = accountId;
        }

        public String getCollection() {
            return collection;
        }

        public String getAccountId() {
            return accountId;
        }

        protected Query getBaseQuery() {
            return termQuery(LuceneFields.L_ACCOUNT_ID, accountId);
        }

        public abstract Query getQuery();

        protected TermQuery termQuery(String field, String value) {
            return new TermQuery(new Term(field, value));
        }

        protected ToStringHelper toStringHelper() {
            return MoreObjects.toStringHelper(this)
                    .add("col", collection)
                    .add("acct", accountId);
        }
    }

    public static class AccountDeletion extends Deletion {

        public AccountDeletion(String collection, String accountId) {
            super(collection, accountId);
        }

        @Override
        public Query getQuery() {
            return getBaseQuery();
        }

        @Override
        public String toString() {
            return toStringHelper().toString();
        }
    }

    public static class ItemDeletion extends Deletion {

        private int itemId;

        public ItemDeletion(String collection, String accountId, int itemId) {
            super(collection, accountId);
            this.itemId = itemId;
        }

        @Override
        public Query getQuery() {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(getBaseQuery(), Occur.MUST);
            builder.add(termQuery(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(itemId)), Occur.MUST);
            return builder.build();
        }

        public int getItemId() {
            return itemId;
        }

        @Override
        public String toString() {
            return toStringHelper().add("id", itemId).toString();
        }
    }

    private class DeletionSweeper extends TimerTask {

        private static final long DEFAULT_SWEEPER_INTERVAL = Constants.MILLIS_PER_HOUR;
        private static final String DEFAULT_SWEEPER_INTERVAL_STR = "1h";
        private CloudSolrClient solrClient;

        public DeletionSweeper(CloudSolrClient solrClient) {
            this.solrClient = solrClient;

        }

        private void processBatch(Collection<Deletion> batch) throws ServiceException {
            Map<String, BooleanQuery.Builder> queriesByCollection = new HashMap<>();
            for (Deletion del: batch) {
                String collection = del.getCollection();
                BooleanQuery.Builder builder = queriesByCollection.computeIfAbsent(collection, k -> new BooleanQuery.Builder());
                builder.add(new BooleanClause(del.getQuery(), Occur.SHOULD));
            }
            for (Map.Entry<String, BooleanQuery.Builder> entry: queriesByCollection.entrySet()) {
                String collection = entry.getKey();
                BooleanQuery query = entry.getValue().build();
                int numDeletions = query.clauses().size();
                try {
                    long start = System.currentTimeMillis();
                    solrClient.deleteByQuery(collection, query.toString());
                    ZimbraLog.index.debug("deleted batch of %s items by query from %s (elapsed=%s)", numDeletions, collection, System.currentTimeMillis() - start);
                } catch (SolrServerException | IOException e) {
                    ZimbraLog.index.error("error deleting batch of %s items by query from collection '%s'", numDeletions, collection, e);
                }
            }
        }

        public void processDeletes() throws ServiceException {
            ZimbraLog.index.info("starting DeletionSweeper");
            int batchSize = Provisioning.getInstance().getLocalServer().getMaxSolrBatchDeletionSize();
            while (deletionProvider.hasMore()) {
                Collection<Deletion> batch = deletionProvider.getDeletions(batchSize);
                processBatch(batch);
                deletionProvider.clearDeleted(batch);
            }
        }

        @Override
        public void run() {
            try {
                processDeletes();
            } catch (ServiceException e) {
                ZimbraLog.index.error("error running batch index deletion!", e);
            }
        }
    }

    public void startSweeper() {
        long batchDeletionInterval;
        String intervalStr;
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            batchDeletionInterval = server.getSolrBatchDeletionInterval();
            intervalStr = server.getSolrBatchDeletionIntervalAsString();
        } catch (ServiceException e) {
            ZimbraLog.index.error("unable to do determine deletion sweeper interval, using default", e);
            batchDeletionInterval = DeletionSweeper.DEFAULT_SWEEPER_INTERVAL;
            intervalStr = DeletionSweeper.DEFAULT_SWEEPER_INTERVAL_STR;
        }
        if (batchDeletionInterval <= 0) {
            ZimbraLog.index.info("not starting DeletionSweeper");
            return;
        }
        String zkHost;
        try {
            zkHost = Provisioning.getInstance().getLocalServer().getIndexURL().substring("solrcloud:".length());
            CloudSolrClient solrClient = SolrUtils.getCloudSolrClient(zkHost);
            ZimbraLog.index.info("scheduling DeletionSweeper (interval=%s)", intervalStr);
            Zimbra.sTimer.schedule(new DeletionSweeper(solrClient), batchDeletionInterval, batchDeletionInterval);
        } catch (ServiceException e) {
            ZimbraLog.index.error("unable to initialize DeletionSweeper!", e);
        }
    }

    private static abstract class DeletionProvider {

        public abstract void addDeletion(Deletion deletion);

        public abstract Collection<Deletion> getDeletions(int limit);

        public abstract boolean hasMore();

        public abstract void clearDeleted(Collection<Deletion> deleted);
    }

    private static class RedisDeletionProvider extends DeletionProvider {

        private static final String KEY_DELETIONS_SET = "batched_deletions";
        private static final String ACCT_PREFIX = "acct";
        private static final String ITEM_PREFIX = "item";
        private static final Joiner JOINER = Joiner.on(":");

        RedissonClient client;
        private RSet<String> deletionSet;

        public RedisDeletionProvider() {
            client = RedissonClientHolder.getInstance().getRedissonClient();
            deletionSet = client.getSet(KEY_DELETIONS_SET, StringCodec.INSTANCE);
        }

        private String encode(Deletion deletion) {
            String collection = deletion.getCollection();
            String accountId = deletion.getAccountId();
            if (deletion instanceof AccountDeletion) {
                return JOINER.join(ACCT_PREFIX, collection, accountId);
            } else if (deletion instanceof ItemDeletion) {
                int itemId = ((ItemDeletion) deletion).getItemId();
                return JOINER.join(ITEM_PREFIX, collection, accountId, itemId);
            } else {
                return null;
            }
        }

        private Deletion decode(String encoded) {
            String[] parts = encoded.split(":");
            String prefix = parts[0];
            if (prefix.equals(ACCT_PREFIX)) {
                String collection = parts[1];
                String accountId = parts[2];
                return new AccountDeletion(collection, accountId);
            } else if (prefix.equals(ITEM_PREFIX)) {
                String collection = parts[1];
                String accountId = parts[2];
                Integer itemId = Integer.valueOf(parts[3]);
                return new ItemDeletion(collection, accountId, itemId);
            } else {
                ZimbraLog.index.warn("cannot parse deletion item: %s", encoded);
                return null;
            }
        }

        @Override
        public void addDeletion(Deletion deletion) {
            String encoded = encode(deletion);
            if (encoded != null) {
                deletionSet.add(encoded);
            }
        }

        @Override
        public Collection<Deletion> getDeletions(int limit) {
            Set<String> encodedDeletions;
            if (limit > 0) {
                encodedDeletions = deletionSet.random(limit);
            } else {
                encodedDeletions = deletionSet.readAll();
            }
            return encodedDeletions.stream().filter(Objects::nonNull).map(s -> decode(s)).collect(Collectors.toSet());
        }

        @Override
        public void clearDeleted(Collection<Deletion> deleted) {
            Collection<String> encoded = deleted.stream().map(d -> encode(d)).collect(Collectors.toSet());
            deletionSet.removeAll(encoded);
        }

        @Override
        public boolean hasMore() {
            return !deletionSet.isEmpty();
        }
    }
}
