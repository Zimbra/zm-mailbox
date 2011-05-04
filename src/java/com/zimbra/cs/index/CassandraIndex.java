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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.apache.cassandra.locator.SimpleStrategy;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;

import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * {@link IndexStore} implementation using Apache Cassandra.
 *
 * <pre>
 * Keyspace: "Zimbra"
 *  ColumnFamily: "IndexTerm"
 *   Key: AccountUUID
 *    SuperColumn: FieldPrefix + Term
 *     Column: DocumentUUID
 *      Value: TermInfo
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
    private static final String KEYSPACE = "Zimbra";
    private static final String CF_TERM = "IndexTerm";
    private static final String CF_DOCUMENT = "IndexDocument";
    private static final String CN_ITEM = "Item";
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
    public Searcher openSearcher() {
        return new SearcherImpl();
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
        public CassandraIndex getInstance(Mailbox mbox) {
            return new CassandraIndex(mbox, keyspace);
        }

        @Override
        public void destroy() {
        }

        public void createSchema() {
            try {
                cluster.dropKeyspace(KEYSPACE);
            } catch (HInvalidRequestException ignore) {
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
        public void addDocument(MailItem item, List<IndexDocument> docs) throws IOException {
            for (IndexDocument doc : docs) {
                UUID docID = UUID.randomUUID(); //TODO use time-based (type 1) UUID
                List<HColumn<UUID, byte[]>> cols = Collections.singletonList(HFactory.createColumn(
                        docID, new byte[0], UUIDSerializer.get(), BytesArraySerializer.get()));
                for (Fieldable field : doc.toDocument().getFields()) {
                    Character prefix = FIELD2PREFIX.get(field.name());
                    if (prefix != null && field.isIndexed() && field.isTokenized()) {
                        TokenStream stream = field.tokenStreamValue();
                        if (stream == null) {
                            stream = mailbox.index.getAnalyzer().tokenStream(field.name(),
                                    new StringReader(field.stringValue()));
                        }
                        CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
                        stream.reset();
                        while (stream.incrementToken()) {
                            if (termAttr.length() == 0) {
                                continue;
                            }
                            String term = prefix + termAttr.toString();
                            mutator.addInsertion(key, CF_TERM, HFactory.createSuperColumn(term, cols,
                                    StringSerializer.get(), UUIDSerializer.get(), null));
                        }
                    }
                }
                mutator.addInsertion(key, CF_TERM, HFactory.createSuperColumn(ITEM_ID + item.getId(),
                        cols, StringSerializer.get(), UUIDSerializer.get(), null));
                List<HColumn<String, Integer>> docCols = Collections.singletonList(HFactory.createColumn(
                        CN_ITEM, item.getId(), StringSerializer.get(), IntegerSerializer.get()));
                mutator.addInsertion(key, CF_DOCUMENT, HFactory.createSuperColumn(docID, docCols,
                        UUIDSerializer.get(), StringSerializer.get(), IntegerSerializer.get()));
            }
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
            QueryResult<SuperSlice<String, UUID, byte[]>> qresult = HFactory.createSuperSliceQuery(keyspace,
                    UUIDSerializer.get(), StringSerializer.get(), UUIDSerializer.get(), BytesArraySerializer.get())
                    .setKey(key).setColumnFamily(CF_TERM).setColumnNames(terms.toArray(new String[terms.size()]))
                    .execute();
            for (HSuperColumn<String, UUID, byte[]> scol : qresult.get().getSuperColumns()) {
                mutator.addSuperDelete(key, CF_TERM, scol.getName(), StringSerializer.get());
                for (HColumn<UUID, byte[]> col : scol.getColumns()) {
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
    }

    private final class SearcherImpl implements Searcher {

        @Override
        public List<Document> search(Query query, Filter filter, Sort sort, int max) {
            List<Integer> itemIDs;
            if (query instanceof TermQuery) {
                itemIDs = search((TermQuery) query);
            } else if (query instanceof PrefixQuery) {
                itemIDs = search((PrefixQuery) query, max);
            } else {
                ZimbraLog.search.warn("NotImplementedYet query=%s:%s", query.getClass().getSimpleName(), query);
                return Collections.emptyList();
            }

            return Lists.transform(itemIDs, new Function<Integer, Document>() {
                @Override
                public Document apply(Integer itemID) {
                    Document doc = new Document();
                    doc.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(itemID),
                            Field.Store.YES, Field.Index.NO));
                    return doc;
                }
            });
        }

        private List<Integer> search(TermQuery query) {
            Term term = query.getTerm();
            if (Strings.isNullOrEmpty(term.text())) {
                return Collections.emptyList();
            }
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return Collections.emptyList();
            }
            String termText = prefix + term.text();
            QueryResult<HSuperColumn<String, UUID, byte[]>> result = HFactory.createSuperColumnQuery(keyspace,
                    UUIDSerializer.get(), StringSerializer.get(), UUIDSerializer.get(), BytesArraySerializer.get())
                    .setKey(key).setColumnFamily(CF_TERM).setSuperName(termText).execute();
            HSuperColumn<String, UUID, byte[]> scol = result.get();
            if (scol == null) {
                return Collections.emptyList();
            }
            List<UUID> ids = new ArrayList<UUID>(scol.getSize());
            for (HColumn<UUID, byte[]> col : result.get().getColumns()) {
                ids.add(col.getName());
            }
            return toItemID(termText, ids);
        }

        private List<Integer> search(PrefixQuery query, int max) {
            Term term = query.getPrefix();
            if (Strings.isNullOrEmpty(term.text())) {
                return Collections.emptyList();
            }
            Character prefix = FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return Collections.emptyList();
            }
            String termText = prefix + term.text();
            QueryResult<SuperSlice<String, UUID, Integer>> qresult = HFactory.createSuperSliceQuery(keyspace,
                    UUIDSerializer.get(), StringSerializer.get(), UUIDSerializer.get(), IntegerSerializer.get())
                    .setKey(key).setColumnFamily(CF_TERM).setRange(termText, termText + '\uFFFF', false, max)
                    .execute();
            Set<Integer> result = new LinkedHashSet<Integer>(); // remove duplicate IDs which may appear across terms
            for (HSuperColumn<String, UUID, Integer> scol : qresult.get().getSuperColumns()) {
                List<UUID> ids = new ArrayList<UUID>();
                for (HColumn<UUID, Integer> col : scol.getColumns()) {
                    ids.add(col.getName());
                }
                result.addAll(toItemID(scol.getName(), ids));
            }
            return new ArrayList<Integer>(result);
        }

        /**
         * Converts doc IDs to item IDs. Orphans are removed from Term CF.
         *
         * @param term term text used in search
         * @param ids document IDs to validate
         * @return item IDs
         */
        private List<Integer> toItemID(String term, List<UUID> docIDs) {
            QueryResult<SuperSlice<UUID, String, Integer>> qresult = HFactory.createSuperSliceQuery(keyspace,
                    UUIDSerializer.get(), UUIDSerializer.get(), StringSerializer.get(), IntegerSerializer.get())
                    .setKey(key).setColumnFamily(CF_DOCUMENT).setColumnNames(docIDs.toArray(new UUID[docIDs.size()]))
                    .execute();
            List<Integer> result = new ArrayList<Integer>(docIDs.size());
            for (HSuperColumn<UUID, String, Integer> scol : qresult.get().getSuperColumns()) {
                docIDs.remove(scol.getName());
                for (HColumn<String, Integer> col : scol.getColumns()) {
                    if (CN_ITEM.equals(col.getName())) {
                        result.add(col.getValue());
                        break;
                    }
                }
            }
            if (!docIDs.isEmpty()) { // remove orphan
                Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());
                for (UUID id : docIDs) {
                    mutator.addSubDelete(key, CF_TERM, term, id, StringSerializer.get(), UUIDSerializer.get());
                }
                mutator.execute();
            }
            return result;
        }

        @Override
        public TermEnum getTerms(Term term) {
            return new TermEnumImpl(term);
        }

        @Override
        public int getCount(Term term) {
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
        public int getTotal() {
            return HFactory.createSuperCountQuery(keyspace, UUIDSerializer.get(), IntegerSerializer.get())
                .setKey(key).setColumnFamily(CF_DOCUMENT).setRange(null, null, Integer.MAX_VALUE).execute().get();
        }

        @Override
        public void close() {
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
