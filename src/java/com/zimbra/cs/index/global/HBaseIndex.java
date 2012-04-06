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
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.search.IndexSearcher;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.smile.SmileFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.primitives.UnsignedBytes;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.global.GlobalIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;

/**
 * {@link IndexStore} implementation using Apache HBase.
 *
 * @author ysasaki
 * @author smukhopadhyay
 */
public final class HBaseIndex implements IndexStore {
    static final String INDEX_TABLE = "zimbra.index";
    static final String TABLE_POOL_SIZE = "hbase.table.pool.size";
    static final byte[] MBOX_CF = Bytes.toBytes("mbox");
    static final byte[] TERM_CF = Bytes.toBytes("term");
    static final byte[] ITEM_CF = Bytes.toBytes("item");
    private static final byte[] VERSION_COL = Bytes.toBytes("ver");
    static final byte[] TYPE_COL = Bytes.toBytes("type");
    static final byte[] DATE_COL = Bytes.toBytes("date");
    static final byte[] SIZE_COL = Bytes.toBytes("size");
    static final byte[] NAME_COL = Bytes.toBytes("name");
    static final byte[] SUBJECT_COL = Bytes.toBytes("subj");
    static final byte[] SORT_SUBJECT_COL = Bytes.toBytes("^subj");
    static final byte[] FRAGMENT_COL = Bytes.toBytes("frag");
    static final byte[] SENDER_COL = Bytes.toBytes("from");
    static final byte[] SORT_SENDER_COL = Bytes.toBytes("^from");
    static final byte[] MIME_TYPE_COL = Bytes.toBytes("mime");

    static final ObjectMapper JSON_MAPPER = new ObjectMapper(new SmileFactory());
    static {
        JSON_MAPPER.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        JSON_MAPPER.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true)
            .configure(SerializationConfig.Feature.AUTO_DETECT_FIELDS, false)
            .configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, false)
            .configure(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS, false)
            .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false)
            .configure(DeserializationConfig.Feature.USE_ANNOTATIONS, true)
            .configure(DeserializationConfig.Feature.AUTO_DETECT_CREATORS, false)
            .configure(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, false)
            .configure(DeserializationConfig.Feature.AUTO_DETECT_SETTERS, false);
    }
    static final BiMap<String, Character> FIELD2PREFIX = ImmutableBiMap.<String, Character>builder()
        .put(LuceneFields.L_CONTENT, 'A')
        .put(LuceneFields.L_CONTACT_DATA, 'B')
        .put(LuceneFields.L_MIMETYPE, 'C')
        .put(LuceneFields.L_ATTACHMENTS, 'D')
        .put(LuceneFields.L_FILENAME, 'E')
        .put(LuceneFields.L_OBJECTS, 'F')
        .put(LuceneFields.L_H_FROM, 'G')
        .put(LuceneFields.L_H_TO, 'H')
        .put(LuceneFields.L_H_CC, 'I')
        .put(LuceneFields.L_H_X_ENV_FROM, 'J')
        .put(LuceneFields.L_H_X_ENV_TO, 'K')
        .put(LuceneFields.L_H_MESSAGE_ID, 'L')
        .put(LuceneFields.L_H_SUBJECT, 'M')
        .put(LuceneFields.L_FIELD, 'N')
        .build();

    final Mailbox mailbox;
    private final Factory factory;
    private byte[] row; // Account UUID + row version

    private HBaseIndex(Mailbox mbox, Factory factory) throws IOException {
        this.mailbox = mbox;
        this.factory = factory;
        this.row = getRow(mailbox.getAccountId());
        ZimbraLog.index.debug("%s", this);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("account", mailbox.getAccountId())
            .add("mbox", mailbox.getId())
            .add("ver", UnsignedBytes.toInt(row[row.length - 1]))
            .toString();
    }

    public GlobalIndex getGlobalIndex() {
        return factory.getGlobalIndex();
    }

    private byte[] getRow(String account) throws IOException {
        UUID uuid = UUID.fromString(account);
        ByteBuffer buf = ByteBuffer.allocate(16);  // 128-bit UUID
        byte[] row = buf.putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
        HTableInterface table = factory.pool.getTable(factory.indexTableName);
        try {
            Result result = table.get(new Get(row));
            return Bytes.add(row, result.isEmpty() ?
                    new byte[] {Byte.MIN_VALUE} : result.getValue(MBOX_CF, VERSION_COL));
        } finally {
            table.close();
        }
    }

    @Override
    public Indexer openIndexer() throws IOException {
        return new IndexerImpl();
    }

    @Override
    public IndexSearcher openSearcher() throws IOException {
        final IndexReaderImpl reader = new IndexReaderImpl();
        return new IndexSearcher(reader) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    reader.close();
                }
            }
        };
    }

    /**
     * Does nothing.
     */
    @Override
    public void warmup() {
    }

    /**
     * Does nothing.
     */
    @Override
    public void evict() {
    }

    @Override
    public void deleteIndex() throws IOException {
        List<Row> batch = Lists.newArrayListWithCapacity(2);
        // delete the entire row
        Delete del = new Delete(row);
        del.setTimestamp(Long.MAX_VALUE);
        batch.add(del);
        // increment the row version
        byte ver = (byte) (row[row.length - 1] + 1);
        Put put = new Put(Bytes.head(row, row.length - 1)); // UUID row
        put.add(MBOX_CF, VERSION_COL, new byte[] {ver});
        batch.add(put);

        HTableInterface table = factory.pool.getTable(factory.indexTableName);
        try {
            table.batch(batch);
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            table.close();
        }
        row[row.length - 1] = ver;

        getGlobalIndex().delete(mailbox.getAccountId());
    }
    
    static Put put(Put put, long ts, MailItem item) {
        
        assert put != null;
        
        put.add(ITEM_CF, TYPE_COL, ts, toBytes(item.getType()));
        put.add(ITEM_CF, DATE_COL, ts, Bytes.toBytes(item.getDate()));
        put.add(ITEM_CF, SIZE_COL, ts, Bytes.toBytes(item.getSize()));
        switch (item.getType()) {
            case MESSAGE:
                Message msg = (Message) item;
                put.add(ITEM_CF, SENDER_COL, ts, Bytes.toBytes(msg.getSender()));
                put.add(ITEM_CF, SORT_SENDER_COL, ts, Bytes.toBytes(msg.getSortSender()));
                put.add(ITEM_CF, SUBJECT_COL, ts, Bytes.toBytes(msg.getSubject()));
                put.add(ITEM_CF, SORT_SUBJECT_COL, ts, Bytes.toBytes(msg.getSortSubject()));
                put.add(ITEM_CF, FRAGMENT_COL, ts, Bytes.toBytes(msg.getFragment()));
                break;
            case CONTACT:
                Contact contact = (Contact) item;
                put.add(ITEM_CF, NAME_COL, ts, Bytes.toBytes(contact.getSender()));
                break;
            case DOCUMENT:
                com.zimbra.cs.mailbox.Document doc = (com.zimbra.cs.mailbox.Document) item;
                put.add(ITEM_CF, SENDER_COL, ts, Bytes.toBytes(doc.getCreator()));
                put.add(ITEM_CF, NAME_COL, ts, Bytes.toBytes(doc.getName()));
                put.add(ITEM_CF, FRAGMENT_COL, ts, Bytes.toBytes(doc.getFragment()));
                put.add(ITEM_CF, MIME_TYPE_COL, ts, Bytes.toBytes(doc.getContentType()));
                break;
        }
        return put;
    }

    /**
     * Does nothing.
     */
    @Override
    public boolean verify(PrintStream out) {
        return true;
    }

    Result fetch(int id) throws IOException {
        Get get = new Get(row);
        get.setTimeRange(toTimestamp(id, 0), toTimestamp(id, mailbox.getLastChangeID() + 1));
        get.setMaxVersions(1);
        HTableInterface table = factory.pool.getTable(factory.indexTableName);
        try {
            return table.get(get);
        } finally {
            table.close();
        }
    }

    static byte[] toBytes(Term term) {
        Character prefix = FIELD2PREFIX.get(term.field());
        return prefix != null ? Bytes.toBytes(prefix + term.text()) : null;
    }

    static Term toTerm(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return null;
        }
        String field = FIELD2PREFIX.inverse().get((char) raw[0]);
        return field != null ? new Term(field, new String(raw, 1, raw.length - 1)) : null;
    }

    static byte[] toBytes(MailItem.Type type) {
        return new byte[] {type.toByte()};
    }

    static MailItem.Type toType(byte[] raw) {
        if (raw != null && raw.length == 1) {
            return MailItem.Type.of(raw[0]);
        }
        return MailItem.Type.UNKNOWN;
    }

    /**
     * {@code timestamp (64-bit) = id (32-bit) + mod_content (32-bit)}.
     */
    private long toTimestamp(int id, int mod) {
        return ((long) id) << 32 | mod;
    }

    private int toDocId(long ts) {
        return (int) (ts >> 32);
    }

    public static final class Factory implements IndexStore.Factory {
        private final Configuration config;
        private final HTablePool pool;
        private final byte[] indexTableName;
        private final GlobalIndex globalIndex;

        public Factory() {
            this(HBaseConfiguration.create());
        }

        @VisibleForTesting
        Factory(Configuration conf) {
            config = conf;
            if (conf.get("hbase.zookeeper.quorum") == null) {
                conf.set("hbase.zookeeper.quorum", LC.hbase_host.value());
            }
            // test may override
            pool = new HTablePool(conf, conf.getInt(TABLE_POOL_SIZE, LC.hbase_table_pool_size.intValue()));
            indexTableName = Bytes.toBytes(conf.get(INDEX_TABLE, INDEX_TABLE));
            globalIndex = new GlobalIndex(config, pool);
        }

        @VisibleForTesting
        Configuration getConfiguration() {
            return config;
        }

        @Override
        public HBaseIndex getIndexStore(Mailbox mbox) throws ServiceException {
            try {
                return new HBaseIndex(mbox, this);
            } catch (IOException e) {
                throw ServiceException.FAILURE("Failed to open HBase index", e);
            }
        }

        @Override
        public void destroy() {
            globalIndex.destroy();
        }

        @Override
        public GlobalIndex getGlobalIndex() {
            return globalIndex;
        }
    }

    static final class TermInfo {
        @JsonProperty("tc")
        int totalTermCount;

        @JsonProperty("pos")
        final List<Integer> positions = Lists.newArrayList();

        /**
         * Returns the total number of terms in the document.
         */
        int getTotalTermCount() {
            return totalTermCount;
        }

        /**
         * Returns the number of times this term occurs in the document.
         */
        int getTermCount() {
            return positions.size();
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("tc", getTermCount() + "/" + getTotalTermCount()).toString();
        }

        static byte[] encode(TermInfo info) throws IOException {
            return JSON_MAPPER.writeValueAsBytes(info);
        }

        static TermInfo decode(byte[] raw) throws IOException {
            return JSON_MAPPER.readValue(raw, TermInfo.class);
        }
    }
    
    /**
     * List of unique prefixed terms appearing in the doc/item
     * @author smukhopadhyay
     *
     */
    static final class TermsInfo {
        @JsonProperty("terms")
        Set<String> termsSet = Sets.newHashSet();
        
        /*
         * Returns the total number of unique terms in the document
         */
        int getTotalTerms() {
            return termsSet.size();
        }
        
        static byte[] encode(TermsInfo info) throws IOException {
            return JSON_MAPPER.writeValueAsBytes(info);
        }

        static TermsInfo decode(byte[] raw) throws IOException {
            return JSON_MAPPER.readValue(raw, TermsInfo.class);
        }
    }

    private final class IndexerImpl implements Indexer {
        private final List<Row> batch = Lists.newArrayList();
        private final HTableInterface table;
        private final Map<MailItem, Folder> indexGlobal = Maps.newHashMapWithExpectedSize(1);
        private final List<Integer> deleteGlobal = Lists.newArrayList();

        IndexerImpl() {
            table = factory.pool.getTable(factory.indexTableName);
        }

        @Override
        public void addDocument(Folder folder, MailItem item, List<IndexDocument> docs) throws IOException {
            long ts = toTimestamp(item.getId(), item.getSavedSequence());
            batch.add(put(ts, item));
            Map<String, TermInfo> term2info = Maps.newHashMap();
            Map<Character, Integer> prefix2count = Maps.newHashMapWithExpectedSize(FIELD2PREFIX.size());
            int pos = 0;
            for (IndexDocument doc : docs) {
                for (Fieldable field : doc.toDocument().getFields()) {
                    Character prefix = FIELD2PREFIX.get(field.name());
                    if (prefix != null && field.isIndexed() && field.isTokenized()) {
                        TokenStream stream = field.tokenStreamValue();
                        if (stream == null) {
                            stream = mailbox.index.getAnalyzer().tokenStream(field.name(),
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
            for (Map.Entry<String, TermInfo> entry : term2info.entrySet()) {
                entry.getValue().totalTermCount = prefix2count.get(entry.getKey().charAt(0));
                Put put = new Put(row);
                put.add(TERM_CF, Bytes.toBytes(entry.getKey()), ts, TermInfo.encode(entry.getValue()));
                batch.add(put);
            }
            if (item.getType() == MailItem.Type.DOCUMENT) { // promote all documents to global index
                indexGlobal.put(item, folder);
            }
        }

        private Put put(long ts, MailItem item) {
            Put put = new Put(row);
            return HBaseIndex.put(put, ts, item);
        }

        /**
         * Since there is no DELETE that deletes all columns in a row with a particular TS, issue a GET to
         * fetch all columns with the TS first, then specify those columns in a DELETE.
         */
        @Override
        public void deleteDocument(List<Integer> ids) throws IOException {
            int last = mailbox.getLastChangeID();
            List<Get> gets = Lists.newArrayListWithCapacity(ids.size());
            for (int id : ids) {
                Get get = new Get(row);
                get.setTimeRange(toTimestamp(id, 0), toTimestamp(id, last + 1));
                gets.add(get);
            }
            Delete del = new Delete(row);
            for (Result result : table.get(gets)) {
                if (result == null || result.isEmpty()) {
                    continue;
                }
                for (KeyValue kv : result.raw()) {
                    del.deleteColumn(kv.getFamily(), kv.getQualifier(), kv.getTimestamp());
                }
            }
            batch.add(del);
            // don't bother with checking the existence, attempt to delete from global index no matter what
            deleteGlobal.addAll(ids);
        }

        /**
         * All changes are executed as a batch.
         */
        @Override
        public void close() throws IOException {
            try {
                table.batch(batch);
            } catch (InterruptedException e) {
                throw new IOException(e);
            } finally {
                table.close();
            }
            if (!indexGlobal.isEmpty()) {
                getGlobalIndex().index(HBaseIndex.this, indexGlobal);
            }
            for (int id : deleteGlobal) {
                getGlobalIndex().delete(new GlobalItemID(mailbox.getAccountId(), id));
            }
        }

        /**
         * Does nothing.
         */
        @Override
        public void optimize() {
        }
    }

    private final class IndexReaderImpl extends IndexReader {
        private final HTableInterface table;

        IndexReaderImpl() {
            readerFinishedListeners = Lists.newArrayList(); //TODO
            table = factory.pool.getTable(factory.indexTableName);
        }

        @Override
        protected void doClose() throws IOException {
            table.close();
        }

        @Override
        protected void doCommit(Map<String, String> commitUserData) throws IOException {
            table.close();
        }

        @Override
        protected void doDelete(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doSetNorm(int id, String field, byte value) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doUndeleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int docFreq(Term term) throws IOException {
            if (Strings.isNullOrEmpty(term.text())) {
                return 0;
            }
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return 0;
            }
            Get get = new Get(row);
            get.addColumn(TERM_CF, Bytes.toBytes(prefix + term.text()));
            get.setMaxVersions();
            return table.get(get).size();
        }

        @Override
        public Document document(int id, FieldSelector selector) throws IOException {
            Get get = new Get(row);
            get.addFamily(ITEM_CF);
            get.setTimeRange(toTimestamp(id, 0), toTimestamp(id, Integer.MAX_VALUE));
            get.setMaxVersions(1);
            Result result = table.get(get);
            if (result.isEmpty()) {
                return null;
            }
            IndexDocument doc = new IndexDocument();
            doc.addMailboxBlobId(id);
            KeyValue date = result.getColumnLatest(ITEM_CF, DATE_COL);
            doc.addSortDate(date != null ? Bytes.toLong(date.getValue()) : 0L);
            KeyValue name = result.getColumnLatest(ITEM_CF, SORT_SENDER_COL);
            if (name == null) {
                name = result.getColumnLatest(ITEM_CF, NAME_COL);
            }
            doc.addSortName(name != null ? Bytes.toString(name.getValue()) : "");
            KeyValue size = result.getColumnLatest(ITEM_CF, SIZE_COL);
            doc.addSortSize(size != null ? Bytes.toLong(size.getValue()) : 0L);
            KeyValue subject = result.getColumnLatest(ITEM_CF, SORT_SUBJECT_COL);
            doc.addSortSubject(subject != null ? Bytes.toString(subject.getValue()) : "");
            return doc.toDocument();
        }

        @Override
        public Collection<String> getFieldNames(FieldOption opt) {
            return Collections.emptyList();
        }

        @Override
        public TermFreqVector getTermFreqVector(int id, String field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getTermFreqVector(int id, TermVectorMapper mapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getTermFreqVector(int id, String field, TermVectorMapper mapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TermFreqVector[] getTermFreqVectors(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasDeletions() {
            return false;
        }

        @Override
        public boolean isDeleted(int id) {
            return false;
        }

        /**
         * Currently doesn't return an accurate number.
         * TODO: Count the number of columns in DOC_CF if HBase supports it.
         */
        @Override
        public int numDocs() {
            return mailbox.getLastItemId() - Mailbox.FIRST_USER_ID;
        }

        @Override
        public int maxDoc() {
            return mailbox.getLastItemId() + 1;
        }

        @Override
        public boolean hasNorms(String field) {
            return false;
        }

        @Override
        public byte[] norms(String field) {
            return null;
        }

        @Override
        public void norms(String field, byte[] bytes, int offset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TermDocs termDocs() {
            return new TermDocsImpl(table);
        }

        @Override
        public TermDocs termDocs(Term term) throws IOException {
            TermDocsImpl result = new TermDocsImpl(table);
            result.seek(term);
            return result;
        }

        @Override
        public TermPositions termPositions() {
            return new TermDocsImpl(table);
        }

        @Override
        public TermPositions termPositions(Term term) throws IOException {
            TermDocsImpl result = new TermDocsImpl(table);
            result.seek(term);
            return result;
        }

        @Override
        public TermEnum terms() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TermEnum terms(Term term) throws IOException {
            return new TermEnumImpl(this, term);
        }
    }

    private static class TermDoc {
        final int doc;
        final List<Integer> pos;

        TermDoc(int doc, List<Integer> pos) {
            this.doc = doc;
            this.pos = pos;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("doc", doc).add("freq", pos.size()).add("pos", pos).toString();
        }
    }

    private final class TermDocsImpl implements TermPositions {
        private final HTableInterface table;
        private final List<TermDoc> termDocs = Lists.newArrayList();
        private int docCursor = -1;
        private int posCursor = -1;

        TermDocsImpl(HTableInterface table) {
            this.table = table;
        }

        @Override
        public void close() {
            docCursor = -1;
            posCursor = -1;
            termDocs.clear();
        }

        @Override
        public int doc() {
            return termDocs.get(docCursor).doc;
        }

        @Override
        public int freq() {
            return termDocs.get(docCursor).pos.size();
        }

        @Override
        public boolean next() {
            docCursor++;
            return termDocs.size() > docCursor;
        }

        @Override
        public int read(int[] docs, int[] freqs)  {
            Preconditions.checkArgument(docs.length == freqs.length, "docs array must be as long as freqs array");
            if (termDocs.isEmpty()) {
                return 0;
            }
            if (docCursor < 0) {
                docCursor = 0;
            }
            int i;
            for (i = 0; i < docs.length && termDocs.size() > docCursor; i++, docCursor++) {
                docs[i] = doc();
                freqs[i] = freq();
            }
            return i;
        }

        @Override
        public void seek(Term term) throws IOException {
            docCursor = -1;
            termDocs.clear();
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return;
            }

            Get get = new Get(row);
            get.addFamily(TERM_CF);
            get.setMaxVersions();
            get.setFilter(new QualifierFilter(CompareFilter.CompareOp.EQUAL, new BinaryPrefixComparator(
                    Strings.isNullOrEmpty(term.text()) ? Bytes.toBytes(prefix) : Bytes.toBytes(prefix + term.text()))));

            Result result = table.get(get);
            for (KeyValue kv : result.raw()) {
                TermInfo info = TermInfo.decode(kv.getValue());
                termDocs.add(new TermDoc(toDocId(kv.getTimestamp()), info.positions));
            }
        }

        @Override
        public void seek(TermEnum termEnum) throws IOException {
            seek(termEnum.term());
        }

        @Override
        public int nextPosition() {
            return termDocs.get(docCursor).pos.get(++posCursor);
        }

        /**
         * Iterate through from the head because termDocs are not sorted by doc ID.
         */
        @Override
        public boolean skipTo(int doc) {
            int i = 0;
            for (TermDoc termDoc : termDocs) {
                if (termDoc.doc == doc) {
                    docCursor = i;
                    return true;
                }
            }
            return false;
        }

        @Override
        public byte[] getPayload(byte[] data, int offset) {
            return null;
        }

        @Override
        public int getPayloadLength() {
            return 0;
        }

        @Override
        public boolean isPayloadAvailable() {
            return false;
        }
    }

    private final class TermEnumImpl extends TermEnum {
        private final Queue<Term> terms = Lists.newLinkedList();

        private TermEnumImpl(IndexReaderImpl reader, Term term) throws IOException {
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return;
            }
            Get get = new Get(row);
            get.addFamily(TERM_CF);
            if (Strings.isNullOrEmpty(term.text())) {
                get.setFilter(new ColumnPrefixFilter(Bytes.toBytes(String.valueOf(prefix))));
            } else {
                get.setFilter(new QualifierFilter(CompareFilter.CompareOp.EQUAL,
                        new BinaryPrefixComparator(Bytes.toBytes(prefix + term.text()))));
            }
            Result result = reader.table.get(get);
            for (KeyValue kv : result.raw()) {
                byte[] col = kv.getQualifier();
                terms.add(term.createTerm(Bytes.toString(col, 1, col.length - 1))); // remove prefix
            }
        }

        @Override
        public boolean next() {
            terms.poll();
            return !terms.isEmpty();
        }

        @Override
        public Term term() {
            return terms.peek();
        }

        @Override
        public int docFreq() {
            return 0;
        }

        @Override
        public void close() {
            terms.clear();
        }
    }

}
