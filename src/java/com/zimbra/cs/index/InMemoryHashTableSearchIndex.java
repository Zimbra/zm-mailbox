/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra, Inc.
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import org.apache.lucene.document.Field;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Reference implementation of an in memory {@link IndexStore}.
 * NOTE: Not intended for production use.
 */
public final class InMemoryHashTableSearchIndex extends IndexStore {

    private final Mailbox mailbox;
    private final String key;

    /**
     * Each entry in this map is the set of fields stored for a particular document
     * It is a multimap because multiple fields can be associated with an indexed document.
     */
    private final Multimap <UUID,DocumentFieldInfo> documentFieldStore = HashMultimap.create();

    /**
     * Key is concatenation of prefix associated with LuceneField and the value.
     * e.g. LuceneFields.FIELD2PREFIX.get(LuceneFields.ITEM_ID_PREFIX) + item.getId()
     */
    private final Multimap <String,SearchTermInfo> searchTermStore = HashMultimap.create();

    private InMemoryHashTableSearchIndex(Mailbox mbox) {
        this.mailbox = mbox;
        this.key = mailbox.getAccountId();
    }

    /**
     */
    @Override
    public Indexer openIndexer() {
        return new InMemoryHashTableIndexer();
    }

    /**
     */
    @Override
    public ZimbraIndexSearcher openSearcher() {
        final InMemoryHashTableIndexReader reader = new InMemoryHashTableIndexReader();
        return new ZimbraInMemoryHashTableIndexSearcher(reader);
    }

    /**
     * Prime the index.
     */
    @Override
    public void warmup() {
    }

    /**
     * Removes IndexSearcher used for this index from cache - if appropriate
     */
    @Override
    public void evict() {
    }

    /**
     * Deletes all index data for the mailbox.
     */
    @Override
    public void deleteIndex() {
        ZimbraLog.index.info("Delete index for account=%s", key);
        documentFieldStore.clear();
        searchTermStore.clear();
    }

    @Override
    public boolean isPendingDelete() {
        return false;
    }

    @Override
    public void setPendingDelete(boolean pendingDelete) {
    }

    /**
     * Runs a sanity check for the index data.  Used by the "VerifyIndexRequest" SOAP Admin request
     */
    @Override
    public boolean verify(PrintStream out) {
        return true;
    }

    public int getDocCount() {
        return documentFieldStore.keySet().size();
    }

    public static final class Factory implements IndexStore.Factory {

        public Factory() {
            ZimbraLog.index.info("Created InMemoryHashTableSearchIndex");
        }

        /**
         * Get an IndexStore instance for a particular mailbox
         */
        @Override
        public InMemoryHashTableSearchIndex getIndexStore(Mailbox mbox) {
            return new InMemoryHashTableSearchIndex(mbox);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
        }
    }

    /**
     * Abstraction of index write operations.
     */
    private final class InMemoryHashTableIndexer implements Indexer {

        @Override
        public void close() {
        }

        @Override
        public void optimize() {
        }

        @Override
        public void compact() {
        }

        /**
         * Used from SOAP GetIndexStatsRequest
         * @return total number of docs in this index, not counting deletions.
         */
        @Override
        public int maxDocs() {
            return getDocCount();
        }

        /**
         * Adds the list of documents to the index.
         * <p>
         * If the index status is stale, delete the stale documents first, then add new documents. If the index status
         * is deferred, we are sure that this item is not already in the index, and so we can skip the check-update step.
         */
        @Override
        public void addDocument(Folder folder, MailItem item, List<IndexDocument> docs) throws IOException {
            if (docs == null || docs.isEmpty()) {
                return;
            }

            // handle the partial re-index case here by simply deleting all the documents matching the index_id
            // so that we can simply add the documents to the index later!!
            switch (item.getIndexStatus()) {
                case STALE:
                case DONE: // for partial re-index
                    List<Integer> ids = Lists.newArrayListWithCapacity(1);
                    ids.add(new Integer(item.getId()));

                    ZimbraLog.index.debug("InMemoryHash addDocument - Clear out pre-existing docs for item id  %d",
                            item.getId());
                    deleteDocument(ids);
                    break;
                case DEFERRED:
                    break;
                default:
                    assert false : item.getIndexId();
            }

            for (IndexDocument doc : docs) {
                synchronized (doc) {
                    setFields(item, doc);
                    UUID docID = UUID.randomUUID();
                    Document luceneDoc = doc.toDocument();
                    Map<String, TermInfo> term2info = Maps.newHashMap();
                    int pos = 0;
                    for (Fieldable field : luceneDoc.getFields()) {
                        pos = TermInfo.updateMapWithDetailsForField(mailbox.index.getAnalyzer(), field, term2info, pos);
                        if (field.isStored()) {
                            if (field.isBinary()) {
                                ZimbraLog.index.warn("Binary fields are not supported name=%s", field.name());
                            } else {
                                documentFieldStore.put(docID, new DocumentFieldInfo(field.name(), field.stringValue()));
                            }
                        }
                    }
                    for (Map.Entry<String, TermInfo> entry : term2info.entrySet()) {
                        searchTermStore.put(entry.getKey(), new SearchTermInfo(docID, entry.getValue()));
                    }
                    searchTermStore.put(LuceneFields.ITEM_ID_PREFIX + item.getId(),
                            new SearchTermInfo(docID, new TermInfo()));
                }
            }
            ZimbraLog.index.info("InMemoryHash Indexed document with id=%d", item.getId());
        }

        /**
         * Delete all documents associated with each mailbox item ID in the provided list.
         */
        @Override
        public void deleteDocument(List<Integer> ids) {
            Set<UUID> docIDs = Sets.newHashSet();
            for (Integer id : ids) {
                // Deleting search term information.  This process would be pretty slow.  If performance
                // was an issue, would either do this in a separate thread or have some sort of garbage
                // collection process.
                String itemIdVal = LuceneFields.ITEM_ID_PREFIX + id;
                Collection<SearchTermInfo> itemIdEntries = searchTermStore.get(itemIdVal);
                Multimap <String,SearchTermInfo> searchTermOrphans = HashMultimap.create();
                for (SearchTermInfo tInfo : itemIdEntries) {
                    UUID uuid = tInfo.docUuid;
                    docIDs.add(uuid);
                    for (Entry<String, SearchTermInfo> entry : searchTermStore.entries()) {
                        if (uuid.equals(entry.getValue().docUuid)) {
                            searchTermOrphans.put(entry.getKey(),entry.getValue());
                        }
                    }
                }
                for (Entry<String, SearchTermInfo> entry : searchTermOrphans.entries()) {
                    searchTermStore.remove(entry.getKey(), entry.getValue());
                }
                // Note that deleteDocument is called from addDocument, to start with a clean slate, so may be 0 deletes
                ZimbraLog.index.info("InMemoryHash deleted %d entries from searchTermStore with id=%d",
                        searchTermOrphans.size(), id);
            }
            for (UUID docID : docIDs) {
                ZimbraLog.index.info("InMemoryHash deleting %d documents from fieldStore with id=%s",
                        documentFieldStore.get(docID).size(), docID.toString());
                documentFieldStore.removeAll(docID);
            }
        }
    }

    private final static class DocumentFieldInfo {
        private final String name;
        private final String value;
        private DocumentFieldInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private final static class SearchTermInfo {
        private final UUID docUuid;
        private final TermInfo termInfo;
        private SearchTermInfo(UUID docUuid, TermInfo termInfo) {
            this.docUuid = docUuid;
            this.termInfo = termInfo;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("docUuid", docUuid).add("termInfo", termInfo).toString();
        }
    }

    private final class InMemoryHashTableIndexReader implements ZimbraIndexReader {

        @Override
        public void close() throws IOException {
        }

        @Override
        public int numDocs() {
            return getDocCount();
        }

        /**
         * Used from SOAP GetIndexStatsRequest
         * @return number of deleted documents for this index
         */
        @Override
        public int numDeletedDocs() {
            return 0;  // We delete documents immediately, so there are always 0 deleted docs in the index
        }

        /**
         * Returns an enumeration of all the terms in the index. The enumeration is ordered by Term.compareTo().
         * Each term is greater than all that precede it in the enumeration. Note that after calling terms(),
         * TermEnum.next() must be called on the resulting enumeration before calling other methods such as
         * TermEnum.term().
         *
         * A Term represents a word from text. This is the unit of search. It is composed of two elements:
         * the text of the word, as a string, and the name of the field that the text occurred in, an interned string.
         * Note that terms may represent more than words from text fields, but also things like dates, email
         * addresses, urls, etc.
         */
        @Override
        public TermEnum terms(Term term) throws IOException {
            return new TermEnumImpl(term);
        }
    }

    /**
     * class for enumerating terms.
     * Term enumerations are always ordered by Term.compareTo(). Each term in the enumeration is greater than all that
     * precede it.
     */
    private final class TermEnumImpl extends TermEnum {
        private final Queue<Term> terms = new LinkedList<Term>();

        private TermEnumImpl(Term term) {
            String fieldName = term.field();
            Character prefix = LuceneFields.FIELD2PREFIX.get(fieldName);
            if (prefix == null) {
                return;
            }
            List<Term> allTerms = Lists.newArrayList();
            for (String searchTerm : searchTermStore.keySet()) {
                String searchTermText = searchTerm.substring(1);
                Character testPrefix = searchTerm.charAt(0);
                String testFieldName = null;
                for (Entry<String, Character> entry : LuceneFields.FIELD2PREFIX.entrySet()) {
                    if (entry.getValue().equals(testPrefix)) {
                        testFieldName = entry.getKey();
                        break;
                    }
                }
                if (testFieldName == null) {
                    continue;
                }
                Term testTerm = new Term(testFieldName, searchTermText);
                if (term.compareTo(testTerm) <= 0) {
                    allTerms.add(testTerm);
                }
            }
            Collections.sort(allTerms);
            terms.addAll(allTerms);
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

    public final class ZimbraInMemoryHashTableIndexSearcher implements ZimbraIndexSearcher {
        final InMemoryHashTableIndexReader reader;

        public ZimbraInMemoryHashTableIndexSearcher(InMemoryHashTableIndexReader reader) {
            this.reader = reader;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        /**
         * Returns the stored fields of document {@code docID} (an index store specific ID for the document)
         */
        @Override
        public Document doc(ZimbraIndexDocumentID docID) throws IOException {
            if (docID instanceof ZimbraInMemoryHashTableDocumentID) {
                ZimbraInMemoryHashTableDocumentID myDocID = (ZimbraInMemoryHashTableDocumentID) docID;
                Collection<DocumentFieldInfo> fieldInfos = documentFieldStore.get(myDocID.getDocID());
                if (fieldInfos == null || fieldInfos.isEmpty()) {
                    // TODO: This error message applies to Lucene indexing.  Revise for this.
                    throw new IllegalArgumentException(
                            String.format("No document stored with documentID=%s", docID.toString()));
                }
                Document document = new Document();
                for ( DocumentFieldInfo fieldInfo : fieldInfos) {
                    document.add(new Field(fieldInfo.name, fieldInfo.value, Field.Store.YES, Field.Index.NO));
                }
                return document;
            }
            throw new IllegalArgumentException("Expected a ZimbraZimbraInMemoryHashTableDocumentID");
        }

        /**
         * Returns the number of documents containing the term {@code term}.
         */
        @Override
        public int docFreq(Term term) throws IOException {
            if (Strings.isNullOrEmpty(term.text())) {
                return 0;
            }
            Character prefix = LuceneFields.FIELD2PREFIX.get(term.field());
            if (prefix == null) {
                return 0;
            }
            String termText = prefix + term.text();
            Collection<SearchTermInfo> matches = searchTermStore.get(termText);
            if (matches == null || matches.isEmpty()) {
                return 0;
            } else {
                return matches.size();
            }
        }

        @Override
        public ZimbraIndexReader getIndexReader() {
            return reader;
        }

        /**
         * Finds the top n hits for query.
         */
        @Override
        public ZimbraTopDocs search(Query query, int n) throws IOException {
             return search(query, null, n);
        }

        /**
         * Finds the top n hits for query, applying filter if non-null.
         */
        @Override
        public ZimbraTopDocs search(Query query, Filter filter, int n) throws IOException {
            List<ZimbraScoreDoc>scoreDocs = Lists.newArrayList();
            Map<UUID,Integer> uuidInfo = Maps.newHashMap();
            if (filter != null) {
                ZimbraLog.index.warn("InMemoryHashTableSearchIndex search does not support filters - filter ignored");
            }
            if (query instanceof TermQuery) {
                TermQuery termQuery = (TermQuery) query;
                Term term = termQuery.getTerm();
                Character prefix = LuceneFields.FIELD2PREFIX.get(term.field());
                if (prefix == null) {
                    ZimbraLog.index.info("InMemoryHashTableSearchIndex search - unknown search field=%s", term.field());
                    return ZimbraTopDocs.create(scoreDocs.size(), scoreDocs);
                }
                String termText = prefix + term.text();
                Collection<SearchTermInfo> matches = searchTermStore.get(termText);
                if (!(matches == null || matches.isEmpty())) {
                    for (SearchTermInfo match : matches) {
                        if (uuidInfo.containsKey(match.docUuid)) {
                            uuidInfo.put(match.docUuid, uuidInfo.get(match.docUuid) + 1);
                        } else {
                            uuidInfo.put(match.docUuid, 1);
                        }
                    }
                }
            } else if (query instanceof PrefixQuery) {
                PrefixQuery prefixQuery = (PrefixQuery) query;
                Term term = prefixQuery.getPrefix();
                Character prefix = LuceneFields.FIELD2PREFIX.get(term.field());
                if (prefix == null) {
                    ZimbraLog.index.info("InMemoryHashTableSearchIndex search - unknown search field=%s", term.field());
                    return ZimbraTopDocs.create(scoreDocs.size(), scoreDocs);
                }
                for (Entry<String, SearchTermInfo> entry: searchTermStore.entries()) {
                    String key = entry.getKey();
                    if (prefix.charValue() == key.charAt(0)) {
                        if (key.substring(1).startsWith(term.text())) {
                            if (uuidInfo.containsKey(entry.getValue().docUuid)) {
                                uuidInfo.put(entry.getValue().docUuid, uuidInfo.get(entry.getValue().docUuid) + 1);
                            } else {
                                uuidInfo.put(entry.getValue().docUuid, 1);
                            }
                        }
                    }
                }
            } else {
            // TODO support other types of query
                ZimbraLog.index.info("InMemoryHashTableSearchIndex does not support search queries of type %",
                        query.getClass().getName());
            }
            for (UUID uuid: uuidInfo.keySet()) {
                scoreDocs.add(ZimbraScoreDoc.create(new ZimbraInMemoryHashTableDocumentID(uuid)));
            }
            return ZimbraTopDocs.create(scoreDocs.size(), scoreDocs);
        }

        /**
         * Search implementation with arbitrary sorting. Finds the top n hits for query, applying filter if non-null,
         * and sorting the hits by the criteria in sort.
         */
        @Override
        public ZimbraTopFieldDocs search(Query query, Filter filter, int n, Sort sort) throws IOException {
            // TODO unimplemented at present
            if (filter != null) {
                ZimbraLog.index.warn("InMemoryHashTableSearchIndex search does not support filters - filter ignored");
            }
            if (sort != null) {
                ZimbraLog.index.warn("InMemoryHashTableSearchIndex search does not support sort - sort ignored");
            }
            return null;
        }
    }
}
