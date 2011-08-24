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
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.lucene.search.TermQuery;

/**
 * Global Index Store.
 *
 * @author ysasaki
 */
public final class GlobalIndex {
    static final String GLOBAL_INDEX_TABLE = "zimbra.global.index";

    private final HTablePool pool;
    private final byte[] indexTableName;

    GlobalIndex(Configuration conf, HTablePool pool) {
        this.pool = pool;
        indexTableName = Bytes.toBytes(conf.get(GLOBAL_INDEX_TABLE, GLOBAL_INDEX_TABLE)); // test may override
    }

    /**
     * Fetch the item and associated terms from the private (per-mailbox) index (we can do this because everything in a
     * private index is contained in a single row and item IDs are stored as timestamp, and this is more more efficient
     * than re-tokenizing the original content), then copy to the global index.
     *
     * TODO: move this logic to HBase backend using coprocessor.
     */
    void index(HBaseIndex index, int id) throws IOException {
        byte[] gid = GlobalItemID.toBytes(index.mailbox.getAccountId(), id);
        Result result = index.fetch(id);
        List<Put> batch = new ArrayList<Put>(result.size());
        Put doc = new Put(gid);
        batch.add(doc);
        for (KeyValue kv : result.raw()) {
            if (Bytes.equals(kv.getFamily(), HBaseIndex.TERM_CF)) {
                Put put = new Put(kv.getQualifier());
                put.add(HBaseIndex.TERM_CF, gid, kv.getValue());
                batch.add(put);
            } else if (Bytes.equals(kv.getFamily(), HBaseIndex.ITEM_CF)) {
                doc.add(HBaseIndex.ITEM_CF, kv.getQualifier(), kv.getValue());
            }
        }

        HTableInterface table = pool.getTable(indexTableName);
        try {
            table.put(batch);
        } finally {
            pool.putTable(table);
        }
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
     *
     * TODO: only TermQuery (simplest query) is supported so far.
     */
    public List<GlobalDocument> search(TermQuery query) throws IOException {
        Get get = new Get(HBaseIndex.toBytes(query.getTerm()));
        get.addFamily(HBaseIndex.TERM_CF);
        HTableInterface table = pool.getTable(indexTableName);
        Result[] results;
        try {
            Result result = table.get(get);
            List<Get> batch = new ArrayList<Get>(result.size());
            for (KeyValue kv : result.raw()) {
                get = new Get(kv.getQualifier());
                get.addFamily(HBaseIndex.ITEM_CF);
                batch.add(get);
            }
            results = table.get(batch);
        } finally {
            pool.putTable(table);
        }

        List<GlobalDocument> docs = new ArrayList<GlobalDocument>(results.length);
        for (Result result : results) {
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
                case MESSAGE: //TODO not supported yet
                case CONTACT: //TODO not supported yet
                default:
                    break;
            }

        }
        return docs;
    }

}
