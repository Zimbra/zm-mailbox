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
import java.util.List;
import java.util.Map;
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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;

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
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * {@link IndexStore} implementation using Apache Cassandra.
 *
 * <pre>
 * Keyspace: "Zimbra"
 *  ColumnFamily: "IndexField"
 *   Key: AccountUUID
 *    SuperColumn: Term
 *     Column: ItemID
 *      Value: TermInfo
 *  ColumnFamily: "IndexDocument"
 *   Key: AccountUUID
 *    SuperColumn: ItemID
 *     Column: FieldName
 *      Value: FieldValue
 * </pre>
 *
 * @author ysasaki
 */
public final class CassandraIndex implements IndexStore {
    private static final String KEYSPACE = "Zimbra";
    private static final String CF_FIELD = "IndexField";
    private static final String CF_DOCUMENT = "IndexDocument";
    private static final Map<String, String> FIELD2PREFIX = ImmutableMap.<String, String>builder()
        .put(LuceneFields.L_CONTENT, "Content:")
        .put(LuceneFields.L_CONTACT_DATA, "Contact:")
        .put(LuceneFields.L_MIMETYPE, "MimeType:")
        .put(LuceneFields.L_ATTACHMENTS, "Attach:")
        .put(LuceneFields.L_FILENAME, "Filename:")
        .put(LuceneFields.L_OBJECTS, "Object:")
        .put(LuceneFields.L_H_FROM, "From:")
        .put(LuceneFields.L_H_TO, "To:")
        .put(LuceneFields.L_H_CC, "Cc:")
        .put(LuceneFields.L_H_X_ENV_FROM, "EnvFrom:")
        .put(LuceneFields.L_H_X_ENV_TO, "EnvTo:")
        .put(LuceneFields.L_H_MESSAGE_ID, "MsgId:")
        .put(LuceneFields.L_H_SUBJECT, "Subject:")
        .put(LuceneFields.L_FIELD, "Field:")
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
            .addDeletion(key, CF_FIELD, null, null)
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
            BasicColumnFamilyDefinition fieldCF = new BasicColumnFamilyDefinition();
            fieldCF.setKeyspaceName(KEYSPACE);
            fieldCF.setName(CF_FIELD);
            fieldCF.setColumnType(ColumnType.SUPER);
            fieldCF.setComparatorType(ComparatorType.UTF8TYPE);
            fieldCF.setSubComparatorType(ComparatorType.INTEGERTYPE);

            BasicColumnFamilyDefinition docCF = new BasicColumnFamilyDefinition();
            docCF.setKeyspaceName(KEYSPACE);
            docCF.setName(CF_DOCUMENT);
            docCF.setColumnType(ColumnType.SUPER);
            docCF.setComparatorType(ComparatorType.INTEGERTYPE);

            KeyspaceDefinition ks = HFactory.createKeyspaceDefinition(KEYSPACE, SimpleStrategy.class.getName(), 1,
                    Arrays.<ColumnFamilyDefinition>asList(new ThriftCfDef(fieldCF), new ThriftCfDef(docCF)));
            cluster.addKeyspace(ks);
        }
    }

    private final class IndexerImpl implements Indexer {
        private Mutator<UUID> mutator = HFactory.createMutator(keyspace, UUIDSerializer.get());

        @Override
        public void addDocument(MailItem item, List<IndexDocument> docs) throws IOException {
            for (IndexDocument doc : docs) {
                for (Fieldable field : doc.toDocument().getFields()) {
                    String prefix = FIELD2PREFIX.get(field.name());
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
                            List<HColumn<Integer, String>> cols = Collections.singletonList(HFactory.createColumn(
                                    item.getId(), "", IntegerSerializer.get(), StringSerializer.get()));
                            mutator.addInsertion(key, CF_FIELD, HFactory.createSuperColumn(term, cols,
                                    StringSerializer.get(), IntegerSerializer.get(), StringSerializer.get()));
                        }
                    }
                }
            }
            List<HColumn<String, String>> cols = Collections.singletonList(HFactory.createStringColumn("k", "v"));
            mutator.addInsertion(key, CF_DOCUMENT, HFactory.createSuperColumn(item.getId(), cols,
                    IntegerSerializer.get(), StringSerializer.get(), StringSerializer.get()));
        }

        @Override
        public void deleteDocument(List<Integer> ids) {
            for (int id : ids) {
                mutator.addSuperDelete(key, CF_DOCUMENT, id, IntegerSerializer.get());
            }
        }

        @Override
        public void close() {
            mutator.execute();
        }
    }

    private final class SearcherImpl implements Searcher {

        @Override
        public List<Integer> search(Query query, Filter filter, Sort sort, int max) {
            if (query instanceof TermQuery) {
                Term term = ((TermQuery) query).getTerm();
                if (Strings.isNullOrEmpty(term.text())) {
                    return Collections.emptyList();
                }
                String prefix = FIELD2PREFIX.get(term.field());
                if (prefix == null) {
                    return Collections.emptyList();
                }
                String termText = prefix + term.text();
                QueryResult<HSuperColumn<String, Integer, String>> result = HFactory.createSuperColumnQuery(keyspace,
                        UUIDSerializer.get(), StringSerializer.get(), IntegerSerializer.get(), StringSerializer.get())
                        .setKey(key).setColumnFamily(CF_FIELD).setSuperName(termText).execute();
                HSuperColumn<String, Integer, String> scol = result.get();
                if (scol == null) {
                    return Collections.emptyList();
                }
                List<Integer> ids = new ArrayList<Integer>(scol.getSize());
                for (HColumn<Integer, String> col : result.get().getColumns()) {
                    ids.add(col.getName());
                }
                return ids;
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public Document getDocument(int id) {
            Document doc = new Document();
            doc.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(id), Field.Store.YES, Field.Index.NO));
            return doc;
        }

        @Override
        public TermEnum getTerms(Term term) {
            return new TermEnumImpl();
        }

        @Override
        public int getCount(Term term) {
            return 0;
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

    private static final class TermEnumImpl extends TermEnum {

        @Override
        public boolean next() {
            return false;
        }

        @Override
        public Term term() {
            return null;
        }

        @Override
        public int docFreq() {
            return 0;
        }

        @Override
        public void close() {
        }
    }

}
