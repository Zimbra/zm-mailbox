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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueExcludeFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
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
 */
public final class GlobalIndex {
    static final String GLOBAL_INDEX_TABLE = "zimbra.global.index";
    static final byte[] ACL_COL = Bytes.toBytes("acl");

    private final HTablePool pool;
    private final byte[] indexTableName;

    GlobalIndex(Configuration conf, HTablePool pool) {
        this.pool = pool;
        indexTableName = Bytes.toBytes(conf.get(GLOBAL_INDEX_TABLE, GLOBAL_INDEX_TABLE)); // test may override
        MailboxListener.register(new GlobalIndexMailboxListener());
    }

    /**
     * Fetch the item and associated terms from the private (per-mailbox) index (we can do this because everything in a
     * private index is contained in a single row and item IDs are stored as timestamp, and this is more efficient than
     * re-tokenizing the original content), then copy to the global index.
     *
     * TODO: move this logic to HBase backend using coprocessor.
     */
    void index(HBaseIndex index, Map<MailItem, Folder> items) throws IOException {
        List<Put> batch = new ArrayList<Put>();
        for (Map.Entry<MailItem, Folder> entry : items.entrySet()) {
            MailItem item = entry.getKey();
            byte[] gid = GlobalItemID.toBytes(index.mailbox.getAccountId(), item.getId());
            Result result = index.fetch(item.getId());
            Put doc = new Put(gid);
            for (KeyValue kv : result.raw()) {
                if (Bytes.equals(kv.getFamily(), HBaseIndex.TERM_CF)) {
                    Put put = new Put(kv.getQualifier());
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
        } finally {
            pool.putTable(table);
        }
    }

    /**
     * Denormalize the ACL down to the item level.
     */
    private void indexACL(Folder folder, Put put) {
        put.add(HBaseIndex.ITEM_CF, ACL_COL, encodeACL(folder));
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
            pool.putTable(table);
        }
    }

    /**
     * Update ACL for all items under the folder.
     */
    private void updateACL(Folder folder) throws IOException {
        List<Put> batch = new ArrayList<Put>();
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
            pool.putTable(table);
        }
    }

    private byte[] encodeACL(Folder folder) {
        List<String> grantees = new ArrayList<String>(1);
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

    /**
     * Delete the row from ITEM CF leaving associated terms in TERM CF orphan.
     *
     * TODO: purge orphan terms upon search or by MapReduce.
     */
    void delete(GlobalItemID id) throws IOException {
        Delete delete = new Delete(id.toBytes());
        delete.deleteFamily(HBaseIndex.ITEM_CF);
        HTableInterface table = pool.getTable(indexTableName);
        try {
            table.delete(delete);
        } finally {
            pool.putTable(table);
        }
    }

    /**
     * Lookup TERM CF first collecting global item IDs (may include orphans), then lookup ITEM CF to fetch global items.
     * Global items should include all information required for the search result as we do not want to fetch rows from
     * MAIL_ITEM table across servers.
     */
    public List<GlobalDocument> search(String principal, Query query) throws IOException {
        if (query instanceof BooleanQuery) {
            return search(principal, (BooleanQuery) query);
        } else if (query instanceof TermQuery) {
            return search(principal, (TermQuery) query);
        } else {
            throw new UnsupportedOperationException(query.getClass().getSimpleName() + " not supported");
        }
    }

    private List<GlobalDocument> fetch(HTableInterface table, String principal, Collection<GlobalItemID> ids)
            throws IOException {
        SingleColumnValueExcludeFilter filter = new SingleColumnValueExcludeFilter(HBaseIndex.ITEM_CF, ACL_COL,
                CompareFilter.CompareOp.EQUAL, new SubstringComparator(principal));
        filter.setFilterIfMissing(true);

        List<Get> batch = Lists.newArrayListWithCapacity(ids.size());
        for (GlobalItemID id : ids) {
            Get get = new Get(id.toBytes());
            get.addFamily(HBaseIndex.ITEM_CF);
            get.setFilter(filter);
            batch.add(get);
        }

        Result[] results = table.get(batch);
        List<GlobalDocument> docs = new ArrayList<GlobalDocument>(results.length);
        for (Result result : results) {
            if (result == null || result.isEmpty()) {
                continue;
            }
            switch (HBaseIndex.toType(result.getValue(HBaseIndex.ITEM_CF, HBaseIndex.TYPE_COL))) {
                case DOCUMENT:
                    GlobalDocument doc = new GlobalDocument(new GlobalItemID(result.getRow()));
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
                    docs.add(doc);
                    break;
                default:
                    break;
            }
        }
        return docs;
    }

    private List<GlobalDocument> search(String principal, TermQuery query) throws IOException {
        Get term = new Get(HBaseIndex.toBytes(query.getTerm()));
        term.addFamily(HBaseIndex.TERM_CF);
        HTableInterface table = pool.getTable(indexTableName);
        try {
            Result result = table.get(term); // query TERM CF
            if (result.isEmpty()) { // no hits
                return Collections.emptyList();
            }
            List<GlobalItemID> ids = Lists.newArrayListWithCapacity(result.size());
            for (KeyValue kv : result.raw()) {
                ids.add(new GlobalItemID(kv.getQualifier()));
            }
            return fetch(table, principal, ids);
        } finally {
            pool.putTable(table);
        }
    }

    private List<GlobalDocument> search(String principal, BooleanQuery query) throws IOException {
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

        Map<Term, Set<GlobalItemID>> term2ids = Maps.newHashMapWithExpectedSize(term2get.size());
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
                Set<GlobalItemID> ids = Sets.newHashSetWithExpectedSize(result.size());
                for (KeyValue kv : result.raw()) {
                    ids.add(new GlobalItemID(kv.getQualifier()));
                }
                term2ids.put(term, ids);
            }

            Set<GlobalItemID> conj = Sets.newHashSet(); // raw byte array is not hash-able
            for (BooleanClause clause : query.clauses()) {
                Term term = ((TermQuery) clause.getQuery()).getTerm(); // it's all TermQuery at this point
                Set<GlobalItemID> ids = term2ids.get(term);
                if (ids == null || ids.isEmpty()) { // no hit if any of terms has no hit
                    return Collections.emptyList();
                } else if (conj.isEmpty()) { // first clause
                    conj.addAll(ids);
                } else { // apply AND
                    conj.retainAll(ids);
                }
            }
            if (conj.isEmpty()) { // empty after ANDed
                return Collections.emptyList();
            }
            return fetch(table, principal, conj);
        } finally {
            pool.putTable(table);
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
}
