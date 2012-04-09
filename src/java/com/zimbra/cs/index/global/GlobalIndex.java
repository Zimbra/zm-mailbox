/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.global;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueExcludeFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.global.HBaseIndex.TermInfo;
import com.zimbra.cs.index.global.HBaseIndex.TermsInfo;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxListener;
import com.zimbra.cs.session.PendingModifications;

/**
 * Global Index Store.
 *
 * @author ysasaki
 * @author smukhopadhyay
 */
public final class GlobalIndex {
    static final String GLOBAL_INDEX_TABLE = "zimbra.global.index";
    static final String GLOBAL_TOMBSTONE_TABLE = "zimbra.global.tombstone";
    static final byte[] SERVER_CF = Bytes.toBytes("server");
    static final byte[] ITEM_COUNT_COL = Bytes.toBytes("#item"); // number of total items per server
    static final byte[] ACL_COL = Bytes.toBytes("acl");
    static final byte[] TERMS_COL = Bytes.toBytes("terms"); // list of unique terms appears in the item; used for deleting the orphan terms 

    private final HTablePool pool;
    private final byte[] indexTableName;
    private final byte[] tombstoneTableName;
    private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("GlobalIndex").setDaemon(true).build());
    private final AtomicLong totalItemCount = new AtomicLong(0L);

    GlobalIndex(Configuration conf, HTablePool pool) {
        this.pool = pool;
        indexTableName = Bytes.toBytes(conf.get(GLOBAL_INDEX_TABLE, GLOBAL_INDEX_TABLE)); // test may override
        tombstoneTableName = Bytes.toBytes(conf.get(GLOBAL_TOMBSTONE_TABLE, GLOBAL_TOMBSTONE_TABLE)); // test may override
        MailboxListener.register(new GlobalIndexMailboxListener());
        timer.scheduleWithFixedDelay(new TotalItemCountUpdater(), 0L, 60L, TimeUnit.SECONDS); //TODO LC
    }

    void destroy() {
        timer.shutdownNow();
    }

    /**
     * Fetch the item and associated terms from the private (per-mailbox) index (we can do this because everything in a
     * private index is contained in a single row and item IDs are stored as timestamp, and this is more efficient than
     * re-tokenizing the original content), then copy to the global index.
     *
     * TODO: move this logic to HBase backend using coprocessor.
     */
    void index(HBaseIndex index, Map<MailItem, Folder> items) throws IOException {
        // item and terms share a same timestamp to filter out stale terms during ITEM CF fetch
        long ts = System.currentTimeMillis();
        List<Put> batch = Lists.newArrayList();
        for (Map.Entry<MailItem, Folder> entry : items.entrySet()) {
            MailItem item = entry.getKey();
            byte[] gid = GlobalItemID.toBytes(index.mailbox.getAccountId(), item.getId());
            Result result = index.fetch(item.getId());
            Put doc = new Put(gid, ts);
            for (KeyValue kv : result.raw()) {
                if (Bytes.equals(kv.getFamily(), HBaseIndex.TERM_CF)) {
                    Put put = new Put(kv.getQualifier(), ts);
                    put.add(HBaseIndex.TERM_CF, gid, kv.getValue());
                    batch.add(put);
                } else if (Bytes.equals(kv.getFamily(), HBaseIndex.ITEM_CF)) {
                    doc.add(HBaseIndex.ITEM_CF, kv.getQualifier(), kv.getValue());
                }
            }
            // Since we are in the middle of Mailbox.endTransaction(), do not call Mailbox.getFolderById(), which calls
            // another Mailbox.beginTransaction().
            indexACL(entry.getValue(), doc);
            batch.add(doc);
        }
        HTableInterface table = pool.getTable(indexTableName);
        try {
            table.put(batch);
            table.incrementColumnValue(Bytes.toBytes(LC.zimbra_server_hostname.value()),
                    SERVER_CF, ITEM_COUNT_COL, items.size());
        } finally {
            table.close();
        }
        totalItemCount.addAndGet(items.size());
    }
    
    
    public void addDocument(Folder folder, MailItem item, List<IndexDocument> docs) throws IOException {
        if (item.getType() != MailItem.Type.DOCUMENT) {
            return;
        }
        
        // item and terms share a same timestamp to filter out stale terms during ITEM CF fetch
        long ts = System.currentTimeMillis();
        List<Put> batch = Lists.newArrayList();
        byte[] gid = GlobalItemID.toBytes(item.getMailbox().getAccountId(), item.getId());
        
        Map<String, TermInfo> term2info = Maps.newHashMap();
        Map<Character, Integer> prefix2count = Maps.newHashMapWithExpectedSize(HBaseIndex.FIELD2PREFIX.size());
        int pos = 0;
        for (IndexDocument doc : docs) {
            for (Fieldable field : doc.toDocument().getFields()) {
                Character prefix = HBaseIndex.FIELD2PREFIX.get(field.name());
                if (prefix != null && field.isIndexed() && field.isTokenized()) {
                    TokenStream stream = field.tokenStreamValue();
                    if (stream == null) {
                        stream = item.getMailbox().index.getAnalyzer().tokenStream(field.name(),
                                new StringReader(field.stringValue()));
                    }
                    CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
                    PositionIncrementAttribute posAttr = stream.addAttribute(PositionIncrementAttribute.class);
                    stream.reset();
                    int termCount = 0; // number of terms per field
                    while (stream.incrementToken()) {
                        termCount++;
                        if (termAttr.length() == 0) {
                            continue;
                        }
                        String term = prefix + termAttr.toString();
                        TermInfo info = term2info.get(term);
                        if (info == null) {
                            info = new TermInfo();
                            term2info.put(term, info);
                        }
                        pos += posAttr.getPositionIncrement();
                        info.positions.add(pos);
                    }
                    stream.end();
                    Closeables.closeQuietly(stream);
                    Integer count = prefix2count.get(prefix);
                    prefix2count.put(prefix, count != null ? count + termCount : termCount);
                }
            }
        }
            
        Put globalItem = new Put(gid, ts);
        globalItem = HBaseIndex.put(globalItem, ts, item);            
        // Since we are in the middle of Mailbox.endTransaction(), do not call Mailbox.getFolderById(), which calls
        // another Mailbox.beginTransaction().
        indexACL(folder, globalItem);
        indexTerms(term2info.keySet(), globalItem);
        batch.add(globalItem);

        for (Map.Entry<String, TermInfo> entry : term2info.entrySet()) {
            entry.getValue().totalTermCount = prefix2count.get(entry.getKey().charAt(0));
            Put put = new Put(Bytes.toBytes(entry.getKey()), ts);
            put.add(HBaseIndex.TERM_CF, gid, TermInfo.encode(entry.getValue()));
            batch.add(put);
        }
            
        HTableInterface table = pool.getTable(indexTableName);
        try {
            table.put(batch);
            table.incrementColumnValue(Bytes.toBytes(LC.zimbra_server_hostname.value()),
                    SERVER_CF, ITEM_COUNT_COL, 1);
        } finally {
            table.close();
        }
        totalItemCount.addAndGet(1);
    }

    /**
     * Denormalize the ACL down to the item level.
     */
    private void indexACL(Folder folder, Put put) {
        put.add(HBaseIndex.ITEM_CF, ACL_COL, encodeACL(folder));
    }
    
    /**
     * index all the prefixed terms for the item
     * @throws IOException 
     */
    private void indexTerms(Set<String> terms, Put put) throws IOException {
        TermsInfo info = new TermsInfo();
        info.termsSet = terms;
        put.add(HBaseIndex.ITEM_CF, TERMS_COL, TermsInfo.encode(info));
    }

    /**
     * Update ACL for the item.
     */
    private void updateACL(MailItem item) throws IOException {
        byte[] gid = GlobalItemID.toBytes(item.getMailbox().getAccountId(), item.getId());
        Put put = new Put(gid);
        try {
            indexACL(item.getMailbox().getFolderById(null, item.getFolderId()), put);
        } catch (ServiceException e) {
            ZimbraLog.index.error("Failed to index ACL id=%d,folder=%d", item.getId(), item.getFolderId());
            return;
        }
        HTableInterface table = pool.getTable(indexTableName);
        try {
            table.put(put);
        } finally {
            table.close();
        }
    }

    /**
     * Update ACL for all items under the folder.
     */
    private void updateACL(Folder folder) throws IOException {
        List<Put> batch = Lists.newArrayList();
        for (Folder sub : folder.getSubfolderHierarchy()) { // this folder and its descendants
            if (sub.getDefaultView() == MailItem.Type.DOCUMENT) {
                byte[] acl = encodeACL(sub);
                try {
                    for (int id : folder.getMailbox().listItemIds(null, MailItem.Type.DOCUMENT, sub.getId())) {
                        byte[] gid = GlobalItemID.toBytes(folder.getMailbox().getAccountId(), id);
                        Put put = new Put(gid);
                        put.add(HBaseIndex.ITEM_CF, ACL_COL, acl);
                        batch.add(put);
                    }
                } catch (ServiceException e) {
                    ZimbraLog.index.error("Failed to update ACL account=%s,folder=%d",
                            folder.getMailbox().getAccountId(), sub.getId());
                }
            }
        }
        if (batch.isEmpty()) {
            return;
        }
        HTableInterface table = pool.getTable(indexTableName);
        try {
            table.put(batch);
        } finally {
            table.close();
        }
    }

    private byte[] encodeACL(Folder folder) {
        List<String> grantees = Lists.newArrayListWithExpectedSize(1);
        grantees.add(folder.getMailbox().getAccountId()); // owner
        ACL acl = folder.getEffectiveACL();
        if (acl != null) {
            for (ACL.Grant grant : acl.getGrants()) {
                if ((grant.getGrantedRights() & ACL.RIGHT_READ) == 0) {
                    continue; // no read access
                }
                switch (grant.getGranteeType()) {
                    case ACL.GRANTEE_USER:
                        break;
                    case ACL.GRANTEE_GROUP: //TODO support group sharing
                    case ACL.GRANTEE_DOMAIN: //TODO support domain sharing
                    case ACL.GRANTEE_COS: //TODO support CoS sharing
                    case ACL.GRANTEE_AUTHUSER: //TODO support authenticated user sharing
                    case ACL.GRANTEE_PUBLIC: //TODO support public sharing
                    case ACL.GRANTEE_GUEST: //TODO support guest sharing
                    case ACL.GRANTEE_KEY: //TODO support access key sharing
                        continue;
                }
                grantees.add(grant.getGranteeId());
            }
        }
        return Bytes.toBytes(Joiner.on('\0').join(grantees));
    }
    
    private Put put(HTableInterface table, long ts, GlobalItemID id) throws IOException {
        Get get = new Get(id.toBytes());
        get.addColumn(HBaseIndex.ITEM_CF, TERMS_COL);
        Result result = table.get(get);
        if (result != null && !result.isEmpty()) {
            byte[] terms = result.getValue(HBaseIndex.ITEM_CF, TERMS_COL);
            Put put = new Put(id.toBytes(), ts);
            put.add(HBaseIndex.ITEM_CF, TERMS_COL, terms);
            return put;
        }
        return null;
    }
    
    private void writeTombstones(List<Put> batch) throws IOException {
        HTableInterface tombstoneTable = pool.getTable(tombstoneTableName);
        try {
            tombstoneTable.put(batch);
        } finally {
            tombstoneTable.close();
        }
    }
    
    /**
     * Delete the row from ITEM CF leaving associated terms in TERM CF orphan.
     *
     * TODO: purge orphan terms upon search or by MapReduce.
     */
    void delete(GlobalItemID id) throws IOException {
        HTableInterface table = pool.getTable(indexTableName);
        
        //add item to tombstone so the terms can be purged later
        long ts = System.currentTimeMillis();
        Put put = put(table, ts, id);
        if (put != null) {
            writeTombstones(ImmutableList.of(put));
        }
        
        Delete delete = new Delete(id.toBytes());
        delete.deleteFamily(HBaseIndex.ITEM_CF);
        try {
            table.delete(delete);
            table.incrementColumnValue(Bytes.toBytes(LC.zimbra_server_hostname.value()),
                    SERVER_CF, ITEM_COUNT_COL, -1L);
        } finally {
            table.close();
        }
        totalItemCount.addAndGet(-1L);
    }
    
    public void delete(String account, List<Integer> ids) throws IOException {
        long ts = System.currentTimeMillis();
        List<Put> putList = Lists.newArrayList();
        List<Delete> batch = Lists.newArrayList();
        HTableInterface table = pool.getTable(indexTableName);

        for (int id : ids) {
            GlobalItemID gid = new GlobalItemID(account, id);
            Put put = put(table, ts, gid);
            if (put != null)
                putList.add(put);
            batch.add(new Delete(gid.toBytes()));
        }
        
        if (!putList.isEmpty())
            writeTombstones(putList);
        
        if (!batch.isEmpty()) {
            try {
            table.delete(batch);
            table.incrementColumnValue(Bytes.toBytes(LC.zimbra_server_hostname.value()),
                    SERVER_CF, ITEM_COUNT_COL, -batch.size());
            } finally {
                table.close();
            }
        }
    }

    /**
     * Delete all rows from ITEM CF for the account leaving associated terms in TERM CF orphan.
     * Move the item terms to tombstone so that terms can be purged from the index table later.
     */
    public void delete(String account) throws IOException {
        long ts = System.currentTimeMillis();
        List<Delete> batch = Lists.newArrayList();
        HTableInterface table = pool.getTable(indexTableName);
        
        int tombstoneItemsBatchSize = 10; //we have lot of data to write per item in tombstone; so let's use a fixed batch size for now!!
        List<Put> itemsList = Lists.newArrayListWithCapacity(tombstoneItemsBatchSize);
        
        ResultScanner scanner = null;
        try {
            scanner = table.getScanner(new Scan(new GlobalItemID(account, 0).toBytes(),
                    new GlobalItemID(account, Integer.MAX_VALUE).toBytes()));
            while (true) {
                Result result = scanner.next();
                if (result == null) {
                    //make sure we write remaining items to tombstone before quitting
                    if (itemsList.size() > 0) {
                        writeTombstones(itemsList);
                    }   
                    break;
                }
                
                Put put = put(table, ts, new GlobalItemID(result.getRow()));
                itemsList.add(put);
                if (itemsList.size() >= tombstoneItemsBatchSize) {
                    writeTombstones(itemsList);
                    itemsList.clear();
                }
                
                batch.add(new Delete(result.getRow()));
            }
            if (!batch.isEmpty()) {
                table.delete(batch);
                table.incrementColumnValue(Bytes.toBytes(LC.zimbra_server_hostname.value()),
                        SERVER_CF, ITEM_COUNT_COL, -batch.size());
            }
        } finally {
            Closeables.closeQuietly(scanner);
            table.close();
        }
        totalItemCount.addAndGet(-batch.size());
    }
    
    public void purgeOrphanTerms(String account) throws IOException {
        HTableInterface table = pool.getTable(tombstoneTableName);
        List<Delete> deletes = Lists.newArrayList();
        ResultScanner scanner = null;
        try {
            scanner = table.getScanner(new Scan(new GlobalItemID(account, 0).toBytes(),
                    new GlobalItemID(account, Integer.MAX_VALUE).toBytes()));
            while (true) {
                Result result = scanner.next();
                if (result == null) {
                    break;
                }
                byte[] gid = result.getRow();
                TermsInfo info = TermsInfo.decode(result.getValue(HBaseIndex.ITEM_CF, TERMS_COL));
                List<Delete> batch = Lists.newArrayList();
                for (String term : info.termsSet) {
                    Delete delete = new Delete(Bytes.toBytes(term));
                    delete.deleteColumns(HBaseIndex.TERM_CF, gid);
                    batch.add(delete);
                }
                boolean deleteItem = true;
                HTableInterface indexTable = pool.getTable(indexTableName);
                if (!batch.isEmpty()) {
                    try {
                        indexTable.delete(batch);
                    } catch (Exception x) {
                        deleteItem = false; //we should try purging the terms next time!! hence keep the reference in tombstone
                    } finally {
                        indexTable.close();
                    }
                }
                //finally add the gid row for batch deletion from tombstone table
                if (deleteItem) {
                    deletes.add(new Delete(gid));
                }
            }
            if (!deletes.isEmpty()) {
                table.delete(deletes);
            }
        } finally {
            Closeables.closeQuietly(scanner);
            table.close();
        }
    }

    /**
     * Lookup TERM CF first collecting global item IDs (may include orphans), then lookup ITEM CF to fetch global items.
     * Global items should include all information required for the search result as we do not want to fetch rows from
     * MAIL_ITEM table across servers.
     */
    public List<GlobalSearchHit> search(String principal, Query query) throws IOException {
        if (query instanceof BooleanQuery) {
            return search(principal, (BooleanQuery) query);
        } else if (query instanceof TermQuery) {
            return search(principal, (TermQuery) query);
        } else {
            throw new UnsupportedOperationException(query.getClass().getSimpleName() + " not supported");
        }
    }

    private List<GlobalSearchHit> fetch(HTableInterface table, String principal, List<TermHit> termHits)
            throws IOException {
        SingleColumnValueExcludeFilter filter = new SingleColumnValueExcludeFilter(HBaseIndex.ITEM_CF, ACL_COL,
                CompareFilter.CompareOp.EQUAL, new SubstringComparator(principal));
        filter.setFilterIfMissing(true);

        Map<GlobalItemID, TermHit> id2hit = Maps.newHashMapWithExpectedSize(termHits.size());
        List<Get> batch = Lists.newArrayListWithCapacity(termHits.size());
        for (TermHit hit : termHits) {
            id2hit.put(hit.id, hit);
            Get get = new Get(hit.id.toBytes());
            get.addFamily(HBaseIndex.ITEM_CF);
            get.setFilter(filter);
            batch.add(get);
        }

        Result[] results = table.get(batch);
        List<GlobalSearchHit> hits = Lists.newArrayListWithCapacity(results.length);
        for (Result result : results) {
            if (result == null || result.isEmpty()) {
                continue;
            }
            GlobalItemID id = new GlobalItemID(result.getRow());
            // validate TYPE COL's timestamp, which is immutable in practice, to filter out stale terms
            KeyValue kv = result.getColumnLatest(HBaseIndex.ITEM_CF, HBaseIndex.TYPE_COL);
            if (kv != null && kv.getTimestamp() != id2hit.get(id).timestamp) {
                continue;
            }
            switch (HBaseIndex.toType(result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.TYPE_COL))) {
                case DOCUMENT:
                    GlobalDocument doc = new GlobalDocument(id);
                    byte[] date = result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.DATE_COL);
                    if (date != null) {
                        doc.setDate(Bytes.toLong(date));
                    }
                    byte[] size = result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.SIZE_COL);
                    if (size != null) {
                        doc.setSize(Bytes.toLong(size));
                    }
                    byte[] name = result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.NAME_COL);
                    if (name != null) {
                        doc.setFilename(Bytes.toString(name));
                    }
                    byte[] creator = result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.SENDER_COL);
                    if (creator != null) {
                        doc.setCreator(Bytes.toString(creator));
                    }
                    byte[] mtype = result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.MIME_TYPE_COL);
                    if (mtype != null) {
                        doc.setMimeType(Bytes.toString(mtype));
                    }
                    byte[] fragment = result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.FRAGMENT_COL);
                    if (fragment != null) {
                        doc.setFragment(Bytes.toString(fragment));
                    }
                    hits.add(new GlobalSearchHit(doc, id2hit.get(doc.getGID()).score));
                    break;
                default:
                    break;
            }
        }
        return hits;
    }

    private List<GlobalSearchHit> search(String principal, TermQuery query) throws IOException {
        Get term = new Get(HBaseIndex.toBytes(query.getTerm()));
        term.addFamily(HBaseIndex.TERM_CF);
        HTableInterface table = pool.getTable(indexTableName);
        try {
            Result result = table.get(term); // query TERM CF
            if (result.isEmpty()) { // no hits
                return Collections.emptyList();
            }
            List<TermHit> hits = Lists.newArrayListWithCapacity(result.size());
            for (KeyValue kv : result.raw()) {
                hits.add(new TermHit(kv, 1.0F)); // IDF is irrelevant in a single term query
            }
            Collections.sort(hits); // sort by score
            return fetch(table, principal, hits);
        } finally {
            table.close();
        }
    }

    private List<GlobalSearchHit> search(String principal, BooleanQuery query) throws IOException {
        Map<Term, Get> term2get = Maps.newHashMap(); // merge duplicate terms
        for (BooleanClause clause : query.clauses()) {
            if (!clause.isRequired()) {
                throw new UnsupportedOperationException(clause.getOccur() + " not supported");
            }
            Query sub = clause.getQuery();
            if (sub instanceof TermQuery) {
                Term term = ((TermQuery) sub).getTerm();
                if (!term2get.containsKey(term)) {
                    Get get = new Get(HBaseIndex.toBytes(term));
                    get.addFamily(HBaseIndex.TERM_CF);
                    term2get.put(term, get);
                }
            } else {
                throw new UnsupportedOperationException(sub.getClass().getSimpleName() + " not supported");
            }
        }
        if (term2get.isEmpty()) { // empty after expansion
            return Collections.emptyList();
        }

        Map<Term, Set<TermHit>> term2hits = Maps.newHashMapWithExpectedSize(term2get.size());
        HTableInterface table = pool.getTable(indexTableName);
        try {
            Result[] results = table.get(Lists.newArrayList(term2get.values())); // query TERM CF
            for (Result result : results) {
                if (result == null || result.isEmpty()) { // no hit for the term
                    continue;
                }
                Term term = HBaseIndex.toTerm(result.getRow());
                if (term == null) { // invalid term for some reasons
                    continue;
                }
                // idf(t) = log(total number of documents / number of documents where the term(t) appears)
                float idf = (float) Math.log(totalItemCount.doubleValue() / result.size());
                Set<TermHit> hits = Sets.newHashSetWithExpectedSize(result.size());
                for (KeyValue kv : result.raw()) {
                    hits.add(new TermHit(kv, idf));
                }
                term2hits.put(term, hits);
            }

            Set<TermHit> conj = Sets.newHashSet();
            for (BooleanClause clause : query.clauses()) {
                Term term = ((TermQuery) clause.getQuery()).getTerm(); // it's all TermQuery at this point
                Set<TermHit> hits = term2hits.get(term);
                if (hits == null || hits.isEmpty()) { // no hit if any of terms has no hit
                    return Collections.emptyList();
                } else if (conj.isEmpty()) { // first clause
                    conj.addAll(hits);
                } else { // apply AND
                    conj.retainAll(hits);
                }
            }
            if (conj.isEmpty()) { // empty after ANDed
                return Collections.emptyList();
            }

            List<TermHit> hits = Lists.newArrayList(conj);
            Collections.sort(hits); // sort by score
            return fetch(table, principal, hits);
        } finally {
            table.close();
        }
    }

    private final class GlobalIndexMailboxListener extends MailboxListener {

        @Override
        public Set<MailItem.Type> registerForItemTypes() {
            return EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.DOCUMENT);
        }

        @Override
        public void notify(ChangeNotification event) {
            if (event.mods.modified != null) {
                onModify(event.mods.modified);
            }
        }

        private void onModify(Map<PendingModifications.ModificationKey, PendingModifications.Change> changes) {
            for (Map.Entry<PendingModifications.ModificationKey, PendingModifications.Change> entry : changes.entrySet()) {
                PendingModifications.Change change = entry.getValue();
                if (change.what instanceof Document && (change.why & PendingModifications.Change.FOLDER) != 0) {
                    // Update ACL for the document.
                    Document doc = (Document) change.what;
                    try {
                        updateACL(doc);
                    } catch (IOException e) {
                        ZimbraLog.index.error("Failed to update ACL account=%s,id=%d",
                                doc.getMailbox().getAccountId(), doc.getId());
                    }
                } else if (change.what instanceof Folder &&
                        (change.why & (PendingModifications.Change.FOLDER | PendingModifications.Change.ACL)) != 0) {
                    // Update ACL for all items under the folder.
                    Folder folder = (Folder) change.what;
                    try {
                        updateACL(folder);
                    } catch (IOException e) {
                        ZimbraLog.index.error("Failed to update ACL account=%s,folder=%d",
                                folder.getMailbox().getAccountId(), folder.getId());
                    }
                }
            }
        }
    }

    /**
     * Can't do {@code COUNT(*) FROM table} in HBase, so each server updates its own counter in SERVER CF, and
     * periodically aggregates counters from all servers.
     *
     * TODO: Admin util to recalculate from MySQL in case the counter gets skewed.
     */
    private final class TotalItemCountUpdater implements Runnable {
        @Override
        public void run() {
            HTableInterface table = pool.getTable(indexTableName);
            ResultScanner scanner = null;
            try {
                scanner = table.getScanner(SERVER_CF, ITEM_COUNT_COL);
                long total = 0;
                while (true) {
                    Result result = scanner.next();
                    if (result == null) {
                        break;
                    }
                    total += Bytes.toLong(result.getValue(SERVER_CF, ITEM_COUNT_COL));
                }
                totalItemCount.set(total);
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to update total item count", e);
            } finally {
                Closeables.closeQuietly(scanner);
                try {
                    table.close();
                } catch (IOException e) {
                    ZimbraLog.index.error("Failed to close the table", e);
                }
            }
        }
    }

    private static final class TermHit implements Comparable<TermHit> {
        private final GlobalItemID id;
        private final long timestamp;
        private final TermInfo termInfo;
        private final float score; // tf-idf(t,d) = tf(t,d) * idf(t)

        TermHit(KeyValue kv, float idf) throws IOException {
            this.id = new GlobalItemID(kv.getQualifier());
            this.timestamp = kv.getTimestamp();
            this.termInfo = TermInfo.decode(kv.getValue());
            this.score = ((float) termInfo.getTermCount() / (float) termInfo.getTotalTermCount()) * idf;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TermHit && id.equals(((TermHit) o).id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        /**
         * Sort by score in descending order (higher to lower).
         */
        @Override
        public int compareTo(TermHit o) {
            return Float.compare(o.score, score);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("id", id).add("score", score).toString();
        }
    }
}
