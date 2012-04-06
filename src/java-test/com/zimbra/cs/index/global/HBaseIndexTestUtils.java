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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.HBaseAdmin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Tools for {@link HBaseIndex} tests.
 *
 * @author ysasaki
 * @author smukhopadhyay
 */
final class HBaseIndexTestUtils {
    private static final String TEST_INDEX_TABLE = "__zimbra.index";
    private static final String TEST_GLOBAL_INDEX_TABLE = "__zimbra.global.index";
    private static final String TEST_GLOBAL_TOMBSTONE_TABLE = "__zimbra.global.tombstone";
    private static final HBaseIndex.Factory FACTORY;
    static {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.setInt(HBaseIndex.TABLE_POOL_SIZE, 2);
        conf.set(HBaseIndex.INDEX_TABLE, TEST_INDEX_TABLE);
        conf.set(GlobalIndex.GLOBAL_INDEX_TABLE, TEST_GLOBAL_INDEX_TABLE);
        conf.set(GlobalIndex.GLOBAL_TOMBSTONE_TABLE, TEST_GLOBAL_TOMBSTONE_TABLE);
        FACTORY = new HBaseIndex.Factory(conf);
    }

    private HBaseIndexTestUtils() {
    }

    static HBaseIndex createIndex(Mailbox mbox) throws ServiceException {
        return FACTORY.getIndexStore(mbox);
    }
    
    static GlobalIndex getGlobalIndex() {
        return FACTORY.getGlobalIndex();
    }

    static void initSchema() throws IOException {
        HBaseAdmin admin = new HBaseAdmin(FACTORY.getConfiguration());
        try {
            admin.disableTable(TEST_INDEX_TABLE);
            admin.deleteTable(TEST_INDEX_TABLE);
        } catch (TableNotFoundException ignore) {
        }
        try {
            admin.disableTable(TEST_GLOBAL_INDEX_TABLE);
            admin.deleteTable(TEST_GLOBAL_INDEX_TABLE);
        } catch (TableNotFoundException ignore) {
        }
        try {
            admin.disableTable(TEST_GLOBAL_TOMBSTONE_TABLE);
            admin.deleteTable(TEST_GLOBAL_TOMBSTONE_TABLE);
        } catch (TableNotFoundException ignore) {
        }

        HTableDescriptor indexTableDesc = new HTableDescriptor(TEST_INDEX_TABLE);
        HColumnDescriptor mboxCF = new HColumnDescriptor(HBaseIndex.MBOX_CF);
        mboxCF.setMaxVersions(1);
        indexTableDesc.addFamily(mboxCF);
        HColumnDescriptor termCF = new HColumnDescriptor(HBaseIndex.TERM_CF);
        termCF.setMaxVersions(Integer.MAX_VALUE);
        indexTableDesc.addFamily(termCF);
        HColumnDescriptor itemCF = new HColumnDescriptor(HBaseIndex.ITEM_CF);
        itemCF.setMaxVersions(Integer.MAX_VALUE);
        indexTableDesc.addFamily(itemCF);
        admin.createTable(indexTableDesc);

        HTableDescriptor globalIndexTableDesc = new HTableDescriptor(TEST_GLOBAL_INDEX_TABLE);
        HColumnDescriptor globalTermCF = new HColumnDescriptor(HBaseIndex.TERM_CF);
        globalTermCF.setMaxVersions(1);
        globalIndexTableDesc.addFamily(globalTermCF);
        HColumnDescriptor globalItemCF = new HColumnDescriptor(HBaseIndex.ITEM_CF);
        globalItemCF.setMaxVersions(1);
        globalIndexTableDesc.addFamily(globalItemCF);
        HColumnDescriptor globalServerCF = new HColumnDescriptor(GlobalIndex.SERVER_CF);
        globalServerCF.setMaxVersions(1);
        globalIndexTableDesc.addFamily(globalServerCF);
        admin.createTable(globalIndexTableDesc);
        
        HTableDescriptor globalTombstoneTableDesc = new HTableDescriptor(TEST_GLOBAL_TOMBSTONE_TABLE);
        HColumnDescriptor globalTombstoneItemCF = new HColumnDescriptor(HBaseIndex.ITEM_CF);
        globalTombstoneItemCF.setMaxVersions(1);
        globalTombstoneTableDesc.addFamily(globalItemCF);
        admin.createTable(globalTombstoneTableDesc);
    }

}
