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
package com.zimbra.cs.index;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.apache.cassandra.locator.SimpleStrategy;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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
import org.codehaus.jackson.annotate.JsonProperty;

import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.beans.SuperSlice;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.global.GlobalIndex;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.SmileSerializer;

/**
 * {@link IndexStore} implementation using Apache Cassandra.
 *
 * <pre>
 * Keyspace: "Zimbra"
 *  ColumnFamily: "IndexTerm"
 *   Key: AccountUUID
 *    SuperColumn: FieldPrefix + Term
 *     Column: DocumentUUID
 *      Value: TermInfo (binary JSON)
 *  ColumnFamily: "IndexDocument"
 *   Key: AccountUUID
 *    SuperColumn: DocumentUUID
 *     Column: FieldName
 *      Value: FieldValue
 * </pre>
 *
 * @author ysasaki
 */
public final class CassandraIndex implements IndexStore {
    private static final SmileSerializer<TermInfo> TERM_INFO_SERIALIZER = new SmileSerializer<TermInfo>(TermInfo.class);
    private static final String KEYSPACE = "Zimbra";
    private static final String CF_TERM = "IndexTerm";
    private static final String CF_DOCUMENT = "IndexDocument";
    private static final String ITEM_ID = "x"; // term prefix for ItemID
    private static final Map<String, Character> FIELD2PREFIX = ImmutableMap.<String, Character>builder()
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

    private final Mailbox mailbox;
    private final Keyspace keyspace;
    private final UUID key;

    private CassandraIndex(Mailbox mbox, Keyspace keyspace) {
        this.mailbox = mbox;
        this.keyspace = keyspace;
        this.key = UUID.fromString(mailbox.getAccountId());
    }

    @Override
    public Indexer openIndexer() {
        return new IndexerImpl();
    }

    @Override
    public IndexSearcher openSearcher() {
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

    @Override
    public void deleteIndex() {
        HFactory.createMutator(keyspace, UUIDSerializer.get())
            .addDeletion(key, CF_TERM, null, null)
            .addDeletion(key, CF_DOCUMENT, null, null)
            .execute();
    }

    @Override
    public void evict() {
    }

    @Override
    public void warmup() {
    }

    @Override
    public boolean verify(PrintStream out) {
        return true;
    }

    public static final class Factory implements IndexStore.Factory {
        private final Cluster cluster;
        private final Keyspace keyspace;

        public Factory() {
            cluster = HFactory.getOrCreateCluster("Zimbra", new CassandraHostConfigurator(LC.cassandra_host.value()));
            keyspace = HFactory.createKeyspace(KEYSPACE, cluster);
        }

        @Override
        public CassandraIndex getIndexStore(Mailbox mbox) {
            return new CassandraIndex(mbox, keyspace);
        }

        @Override
        public GlobalIndex getGlobalIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void destroy() {
        }

        public void createSchema() {
            try {
                cluster.dropKeyspace(KEYSPACE);
            } catch (HInvalidRequestException ignore) { // may not exist yet
            }
            BasicColumnFamilyDefinition termCF = new BasicColumnFamilyDefinition();
            termCF.setKeyspaceName(KEYSPACE);
            termCF.setName(CF_TERM);
            termCF.setColumnType(ColumnType.SUPER);
            termCF.setComparatorType(ComparatorType.UTF8TYPE);

            BasicColumnFamilyDefinition docCF = new BasicColumnFamilyDefinition();
            docCF.setKeyspaceName(KEYSPACE);
            docCF.setName(CF_DOCUMENT);
            docCF.setColumnType(ColumnType.SUPER);

            KeyspaceDefinition ks = HFactory.createKeyspaceDefinition(KEYSPACE, SimpleStrategy.class.getName(), 1,
                    Arrays.<ColumnFamilyDefinition>asList(new ThriftCfDef(termCF), new ThriftCfDef(docCF)));
            cluster.addKeyspace(ks);
        }
    }

    private final class IndexerImpl implements Indexer {
        private Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

        @Override
        public void addDocument(Folder folder, MailItem item, List<IndexDocument> docs) throws IOException {
            for (IndexDocument doc : docs) {
                setFields(item, doc);
                UUID docID = UUID.randomUUID(); //TODO use time-based (type 1) UUID
                Map<String, TermInfo> term2info = new HashMap<String, TermInfo>();
                List<HColumn<String, String>> docCols = new ArrayList<HColumn<String, String>>();
                int pos = 0;
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
                        while (stream.incrementToken()) {
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
                            info.addPosition(pos);
                        }
                    }
                    if (field.isStored()) {
                        if (field.isBinary()) {
                            ZimbraLog.index.warn("Binary fields are not supported name=%s", field.name());
                        } else {
                            docCols.add(HFactory.createColumn(field.name(), field.stringValue(),
                                    StringSerializer.get(), StringSerializer.get()));
                        }
                    }
                }

                for (Map.Entry<String, TermInfo> entry : term2info.entrySet()) {
                    List<HColumn<UUID, TermInfo>> cols = Collections.singletonList(HFactory.createColumn(
                            docID, entry.getValue(), UUIDSerializer.get(), TERM_INFO_SERIALIZER));
                    mutator.addInsertion(key, CF_TERM, HFactory.createSuperColumn(entry.getKey(), cols,
                            StringSerializer.get(), UUIDSerializer.get(), TERM_INFO_SERIALIZER));
                }
                List<HColumn<UUID, TermInfo>> cols = Collections.singletonList(HFactory.createColumn(
                        docID, new TermInfo(), UUIDSerializer.get(), TERM_INFO_SERIALIZER));
                mutator.addInsertion(key, CF_TERM, HFactory.createSuperColumn(ITEM_ID + item.getId(),
                        cols, StringSerializer.get(), UUIDSerializer.get(), TERM_INFO_SERIALIZER));
                mutator.addInsertion(key, CF_DOCUMENT, HFactory.createSuperColumn(docID, docCols,
                        UUIDSerializer.get(), StringSerializer.get(), StringSerializer.get()));
            }
        }

        private void setFields(MailItem item, IndexDocument doc) {
            doc.removeMailboxBlobId();
            doc.addMailboxBlobId(item.getId());
            doc.removeSortDate();
            doc.addSortDate(item.getDate());
            doc.removeSortSize();
            doc.addSortSize(item.getSize());
            doc.removeSortName();
            doc.addSortName(item.getSender());
            doc.removeSortSubject();
            doc.addSortSubject(item.getSortSubject());
        }

        /**
         * Deleting a document only removes the super column from Document CF leaving orphans in Term CF. The orphans
         * are lazily removed next time they are read. This is because we don't want to maintain doc-to-term mappings.
         */
        @Override
        public void deleteDocument(List<Integer> ids) {
            List<String> terms = new ArrayList<String>(ids.size());
            for (Integer id : ids) {
                terms.add(ITEM_ID + id);
            }
            QueryResult<SuperSlice<String, UUID, TermInfo>> qresult = HFactory.createSuperSliceQuery(keyspace,
                    UUIDSerializer.get(), StringSerializer.get(), UUIDSerializer.get(), TERM_INFO_SERIALIZER)
                    .setKey(key).setColumnFamily(CF_TERM).setColumnNames(terms.toArray(new String[terms.size()]))
                    .execute();
            for (HSuperColumn<String, UUID, TermInfo> scol : qresult.get().getSuperColumns()) {
                mutator.addSuperDelete(key, CF_TERM, scol.getName(), StringSerializer.get());
                for (HColumn<UUID, TermInfo> col : scol.getColumns()) {
                    mutator.addSuperDelete(key, CF_DOCUMENT, col.getName(), UUIDSerializer.get());
                }
            }
        }

        /**
         * All changes are executed as a batch.
         */
        @Override
        public void close() {
            mutator.execute();
        }

        /**
         * Does nothing.
         */
        @Override
        public void optimize() {
        }
    }

    private static final class TermInfo {
        @JsonProperty("pos")
        private List<Integer> positions;

        void addPosition(int value) {
            if (positions == null) {
                positions = new ArrayList<Integer>();
            }
            positions.add(value);
        }

        List<Integer> getPositions() {
            return positions;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("pos", positions).toString();
        }
    }

    private final class IdMap {
        private int lastDocId = 0;
        private BiMap<Integer, UUID> id2uuid = HashBiMap.create(100);
        private final Map<UUID, Document> uuid2doc = Maps.newHashMapWithExpectedSize(100);

        int get(UUID uuid) {
            Integer id = id2uuid.inverse().get(uuid);
            if (id == null) {
                id = ++lastDocId;
                id2uuid.put(id, uuid);
            }
            return id;
        }

        UUID get(int id) {
            return id2uuid.get(id);
        }

        Document doc(int id) {
            UUID uuid = get(id);
            return uuid != null ? uuid2doc.get(uuid) : null;
        }

        /**
         * Validates document UUIDs. Orphans are removed from the collection as well as Term CF.
         *
         * @param term term text used in search
         * @param docUUIDs document UUIDs to validate
         */
        void validate(String term, Collection<UUID> docUUIDs) {
            Set<UUID> orphan = new HashSet<UUID>(docUUIDs);
            orphan.removeAll(id2uuid.values());
            if (orphan.isEmpty()) {
                return;
            }
            QueryResult<SuperSlice<UUID, String, String>> qresult = HFactory.createSuperSliceQuery(keyspace,
                    UUIDSerializer.get(), UUIDSerializer.get(), StringSerializer.get(), StringSerializer.get())
                    .setKey(key).setColumnFamily(CF_DOCUMENT)
                    .setColumnNames(orphan.toArray(new UUID[orphan.size()])).execute();
            for (HSuperColumn<UUID, String, String> scol : qresult.get().getSuperColumns()) {
                orphan.remove(scol.getName());
                Document doc = new Document();
                for (HColumn<String, String> col : scol.getColumns()) {
                    doc.add(new Field(col.getName(), col.getValue(), Field.Store.YES, Field.Index.NO));
                }
                uuid2doc.put(scol.getName(), doc);
            }
            if (orphan.isEmpty()) {
                return;
            }
            // remove orphan
            docUUIDs.removeAll(orphan);
            Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());
            for (UUID uuid : docUUIDs) {
                mutator.addSubDelete(key, CF_TERM, term, uuid, StringSerializer.get(), UUIDSerializer.get());
            }
            mutator.execute();
        }

    }

    private final class IndexReaderImpl extends IndexReader {
        private final IdMap idMap = new IdMap();
        private int numDocs = -1;

        IndexReaderImpl() {
            readerFinishedListeners = new ArrayList<ReaderFinishedListener>(); //TODO
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected void doCommit(Map<String, String> commitUserData) {
            throw new UnsupportedOperationException();
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
        public int docFreq(Term term) {
            if (Strings.isNullOrEmpty(term.text())) {
                return 0;
            }
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return 0;
            }
            String termText = prefix + term.text();
            return HFactory.createSubCountQuery(keyspace, UUIDSerializer.get(), StringSerializer.get(),
                    IntegerSerializer.get()).setColumnFamily(CF_TERM).setKey(key).setSuperColumn(termText)
                    .setRange(null, null, Integer.MAX_VALUE).execute().get();
        }

        @Override
        public Document document(int id, FieldSelector selector) {
            return idMap.doc(id);
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

        @Override
        public int maxDoc() {
            return numDocs() + 1;
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
        public int numDocs() {
            if (numDocs < 0) {
                numDocs = HFactory.createSuperCountQuery(keyspace, UUIDSerializer.get(), IntegerSerializer.get())
                    .setKey(key).setColumnFamily(CF_DOCUMENT).setRange(null, null, Integer.MAX_VALUE).execute().get();
            }
            return numDocs;
        }

        @Override
        public TermDocs termDocs() {
            return new TermDocsImpl(idMap);
        }

        @Override
        public TermDocs termDocs(Term term) {
            TermDocsImpl result = new TermDocsImpl(idMap);
            result.seek(term);
            return result;
        }

        @Override
        public TermPositions termPositions() {
            return new TermDocsImpl(idMap);
        }

        @Override
        public TermPositions termPositions(Term term) {
            TermDocsImpl result = new TermDocsImpl(idMap);
            result.seek(term);
            return result;
        }

        @Override
        public TermEnum terms() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TermEnum terms(Term term) {
            return new TermEnumImpl(term);
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
        private final List<TermDoc> termDocs = new ArrayList<TermDoc>();
        private int docCursor = -1;
        private int posCursor = -1;
        private final IdMap idMap;

        TermDocsImpl(IdMap idMap) {
            this.idMap = idMap;
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
        public void seek(Term term) {
            docCursor = -1;
            termDocs.clear();
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return;
            }
            String termText = prefix + term.text();
            QueryResult<HSuperColumn<String, UUID, TermInfo>> qresult = HFactory.createSuperColumnQuery(keyspace,
                    UUIDSerializer.get(), StringSerializer.get(), UUIDSerializer.get(), TERM_INFO_SERIALIZER)
                    .setKey(key).setColumnFamily(CF_TERM).setSuperName(termText).execute();
            if (qresult.get() == null) {
                return;
            }
            assert(qresult.get().getSize() == qresult.get().getColumns().size());
            Set<UUID> docUUIDs = Sets.newHashSetWithExpectedSize(qresult.get().getSize());
            for (HColumn<UUID, TermInfo> col : qresult.get().getColumns()) {
                docUUIDs.add(col.getName());
            }
            idMap.validate(termText, docUUIDs);
            for (HColumn<UUID, TermInfo> col : qresult.get().getColumns()) {
                if (docUUIDs.contains(col.getName())) {
                    termDocs.add(new TermDoc(idMap.get(col.getName()), col.getValue().getPositions()));
                }
            }
        }

        @Override
        public void seek(TermEnum termEnum) {
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
        private final Queue<Term> terms = new LinkedList<Term>();

        private TermEnumImpl(Term term) {
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return;
            }
            String start = prefix + term.text();
            String end = String.valueOf(prefix) + '\uFFFF';
            QueryResult<SuperSlice<String, Integer, String>> qresult = HFactory.createSuperSliceQuery(keyspace,
                    UUIDSerializer.get(), StringSerializer.get(), IntegerSerializer.get(), StringSerializer.get())
                    .setKey(key).setColumnFamily(CF_TERM).setRange(start, end, false, 1000)
                    .execute();
            for (HSuperColumn<String, Integer, String> scol : qresult.get().getSuperColumns()) {
                terms.add(term.createTerm(scol.getName().substring(1))); // remove prefix
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
