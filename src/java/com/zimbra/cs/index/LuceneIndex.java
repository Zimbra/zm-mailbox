/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Version;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.SortBy.SortDirection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.util.SyncToken;


/**
 * A custom Lucene provider that uses the IndexWritersCache to manage the index
 * LRU.
 */
public class LuceneIndex extends IndexWritersCache.IndexWriter
    implements ILuceneIndex, ITextIndex {

    //TODO: Change to 3.0 after writing extensive unit test
    //public static final Version VERSION = Version.LUCENE_30;
    public static final Version VERSION = Version.LUCENE_24;

    private static IndexReadersCache sIndexReadersCache;
    private static IIndexWritersCache sIndexWritersCache;

    /**
     * If documents are being constantly added to an index, then it will stay at
     * the front of the LRU cache and will never flush itself to disk: this
     * setting specifies the maximum number of writes we will allow to the index
     * before we force a flush. Higher values will improve batch-add
     * performance, at the cost of longer-lived transactions in the redolog.
     */
    private static int sMaxUncommittedOps;

    /**
     * This static array saves us from the time required to create a new array
     * everytime editDistance is called.
     */
    private int e[][] = new int[1][1];
    private ZimbraFSDirectory mIdxDirectory = null;

    private IndexWriter mIndexWriter;

    private volatile long mLastWriteTime = 0;

    private Sort mLatestSort = null;
    private SortBy mLatestSortBy = null;
    private MailboxIndex mMbidx;
    private int mNumUncommittedItems = 0;
    private SyncToken mHighestUncomittedModContent = new SyncToken(0);
    private int beginWritingNestLevel = 0;


    static void flushAllWriters() {
        if (DebugConfig.disableIndexing)
            return;

        sIndexWritersCache.flushAllWriters();
    }

    static void shutdown() {
        if (DebugConfig.disableIndexing)
            return;

        sIndexReadersCache.signalShutdown();
        try {
            sIndexReadersCache.join();
        } catch (InterruptedException e) {}

        sIndexWritersCache.shutdown();
    }

    static void startup() {
        if (DebugConfig.disableIndexing) {
            ZimbraLog.index.info("Indexing is disabled by the localconfig 'debug_disable_indexing' flag");
            return;
        }

        if (sIndexWritersCache != null) {
            // in case startup is somehow called twice
            sIndexWritersCache.shutdown();
        }

        sMaxUncommittedOps = LC.zimbra_index_max_uncommitted_operations.intValue();
        sIndexReadersCache = new IndexReadersCache(LC.zimbra_index_reader_lru_size.intValue(),
            LC.zimbra_index_reader_idle_flush_time.longValue() * 1000,
            LC.zimbra_index_reader_idle_sweep_frequency.longValue() * 1000);
        sIndexReadersCache.start();

        if (LC.get("zimbra_index_use_dummy_writer_cache").length() != 0) {
            sIndexWritersCache = new DummyIndexWritersCache();
        } else {
            sIndexWritersCache = new IndexWritersCache();
        }
    }

    /**
     * Finds and returns the smallest of three integers.
     */
    private static final int min(int a, int b, int c) {
        int t = (a < b) ? a : b;
        return (t < c) ? t : c;
    }

    public long getBytesWritten() {
        return mIdxDirectory.getBytesWritten();
    }

    public long getBytesRead() {
        return mIdxDirectory.getBytesRead();
    }

    public String generateIndexId(int itemId) {
        return Integer.toString(itemId);
    }

    LuceneIndex(MailboxIndex mbidx, String idxParentDir, long mailboxId) throws ServiceException {
        mMbidx = mbidx;
        mIndexWriter = null;

        // this must be different from the idxParentDir (see the IMPORTANT comment below)
        String idxPath = idxParentDir + File.separatorChar + '0';

        {
            File parentDirFile = new File(idxParentDir);

            // IMPORTANT!  Don't make the actual index directory (mIdxDirectory) yet!
            //
            // The runtime open-index code checks the existance of the actual index directory:
            // if it does exist but we cannot open the index, we do *NOT* create it under the
            // assumption that the index was somehow corrupted and shouldn't be messed-with....on the
            // other hand if the index dir does NOT exist, then we assume it has never existed (or
            // was deleted intentionally) and therefore we should just create an index.
            if (!parentDirFile.exists()) {
                parentDirFile.mkdirs();
            }

            if (!parentDirFile.canRead()) {
                throw ServiceException.FAILURE("Cannot READ index directory (mailbox=" + mailboxId + " idxPath=" + idxPath + ")", null);
            }
            if (!parentDirFile.canWrite()) {
                throw ServiceException.FAILURE("Cannot WRITE index directory (mailbox=" + mailboxId + " idxPath=" + idxPath + ")", null);
            }

            // the Lucene code does not atomically swap the "segments" and "segments.new"
            // files...so it is possible that a previous run of the server crashed exactly in such
            // a way that we have a "segments.new" file but not a "segments" file.  We we will check here
            // for the special situation that we have a segments.new
            // file but not a segments file...
            File segments = new File(idxPath, "segments");
            if (!segments.exists()) {
                File segments_new = new File(idxPath, "segments.new");
                if (segments_new.exists()) {
                    segments_new.renameTo(segments);
                }
            }

            try {
                mIdxDirectory = new ZimbraFSDirectory(new File(idxPath),
                        new SingleInstanceLockFactory());
            } catch (IOException e) {
                throw ServiceException.FAILURE("Cannot create FSDirectory at path: " + idxPath, e);
            }
        }
    }

    public void addDocument(IndexDocument[] docs, MailItem item, int itemId,
            String indexId, int modContent, long receivedDate, long size,
            String sortSubject, String sortSender, boolean deleteFirst)
        throws IOException {

        if (docs.length == 0) {
            return;
        }

        synchronized(getLock()) {

            beginWriting();
            try {
                assert(mIndexWriter != null);

                for (IndexDocument zdoc : docs) {
                    Document doc = (Document) zdoc.getWrappedDocument();
                    // doc can be shared by multiple threads if multiple mailboxes
                    // are referenced in a single email
                    synchronized (doc) {
                        doc.removeFields(LuceneFields.L_SORT_SUBJECT);
                        doc.removeFields(LuceneFields.L_SORT_NAME);

                        doc.add(new Field(LuceneFields.L_SORT_SUBJECT, sortSubject,
                                Field.Store.NO, Field.Index.NOT_ANALYZED));
                        doc.add(new Field(LuceneFields.L_SORT_NAME, sortSender,
                                Field.Store.NO, Field.Index.NOT_ANALYZED));

                        doc.removeFields(LuceneFields.L_MAILBOX_BLOB_ID);
                        doc.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, indexId,
                                Field.Store.YES, Field.Index.NOT_ANALYZED));

                        // If this doc is shared by multi threads, then the date might just be wrong,
                        // so remove and re-add the date here to make sure the right one gets written!
                        doc.removeFields(LuceneFields.L_SORT_DATE);
                        String dateString = DateTools.timeToString(receivedDate,
                                DateTools.Resolution.MILLISECOND);
                        doc.add(new Field(LuceneFields.L_SORT_DATE, dateString,
                                Field.Store.YES, Field.Index.NOT_ANALYZED));

                        doc.removeFields(LuceneFields.L_SORT_SIZE);
                        doc.add(new Field(LuceneFields.L_SORT_SIZE, Long.toString(size),
                                Field.Store.YES, Field.Index.NO));

                        if (null == doc.get(LuceneFields.L_ALL)) {
                            doc.add(new Field(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE,
                                    Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
                        }

                        if (deleteFirst) {
                            String itemIdStr = indexId;
                            Term toDelete = new Term(LuceneFields.L_MAILBOX_BLOB_ID, itemIdStr);
                            mIndexWriter.updateDocument(toDelete, doc);
                        } else {
                            mIndexWriter.addDocument(doc);
                        }

                    } // synchronized(doc)
                } // foreach Document

                if (modContent > 0) {
                    SyncToken token = new SyncToken(modContent, itemId);
                    mNumUncommittedItems++;
                    assert(token.after(mHighestUncomittedModContent));
                    if (token.after(mHighestUncomittedModContent)) {
                        mHighestUncomittedModContent = token;
                    } else {
                        ZimbraLog.index_add.info("Index items not submitted in order: curHighest=" +
                                             mHighestUncomittedModContent + " new highest=" + modContent + " " +
                                             "indexId=" + indexId);
                    }
                }

                // TODO: tim: this might seem bad, since an index in
                // steady-state-of-writes will never get flushed, however we
                // also track the number of uncomitted-operations on the index,
                // and will force a flush if the index has had a lot written to
                // it without a flush.
                updateLastWriteTime();
            } finally {
                doneWriting();
            }

        }
    }

    public List<String> deleteDocuments(List<String> itemIds) throws IOException {
        synchronized(getLock()) {
            beginWriting();
            try {
                int i = 0;
                for (String itemIdStr : itemIds) {
                    try {
                        Term toDelete = new Term(LuceneFields.L_MAILBOX_BLOB_ID, itemIdStr);
                        mIndexWriter.deleteDocuments(toDelete);
                        // NOTE!  The numDeleted may be < you expect here, the document may
                        // already be deleted and just not be optimized out yet -- some lucene
                        // APIs (e.g. docFreq) will still return the old count until the indexes
                        // are optimized...
                        if (ZimbraLog.index_add.isDebugEnabled()) {
                            ZimbraLog.index_add.debug("Deleted index documents for itemId " + itemIdStr);
                        }
                    } catch (IOException ioe) {
                        ZimbraLog.index_add.debug("deleteDocuments exception on index " + i +
                                " out of "+itemIds.size() + " (id=" + itemIds.get(i) + ")");
                        List<String> toRet = new ArrayList<String>(i);
                        for (int j = 0; j < i; j++) {
                            toRet.add(itemIds.get(j));
                        }
                        return toRet;
                    }
                    i++;
                }
            } finally {
                doneWriting();
            }
            return itemIds; // success
        }
    }

    public void deleteIndex() throws IOException {
        synchronized (getLock()) {
            flush();
            if (ZimbraLog.index_add.isDebugEnabled()) {
                ZimbraLog.index_add.debug("****Deleting index " + mIdxDirectory.toString());
            }

            String[] files = mIdxDirectory.listAll();
            // list method may return null (for FSDirectory if the underlying
            // directory doesn't exist in the filesystem or there are
            // permissions problems).
            if (files == null) {
                if (ZimbraLog.index_add.isDebugEnabled()) {
                    ZimbraLog.index_add.debug("****Deleting index unable to list directory " +
                            mIdxDirectory.toString());
                }
                return;
            }

            for (String file : files) {
                mIdxDirectory.deleteFile(file);
            }
        }
    }

    private void enumerateTermsForField(String regex, Term firstTerm,
            TermEnumInterface callback) throws IOException {
        synchronized(getLock()) {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                IndexReader iReader = searcher.getReader();

                TermEnum terms = iReader.terms(firstTerm);
                boolean hasDeletions = iReader.hasDeletions();

                // HACK!
                boolean stripAtBeforeRegex = false;
                if (callback instanceof DomainEnumCallback)
                    stripAtBeforeRegex = true;

                Pattern p = null;
                if (regex != null && regex.length() > 0) {
                    p = Pattern.compile(regex);
                }

                do {
                    Term cur = terms.term();
                    if (cur != null) {
                        if (!cur.field().equals(firstTerm.field())) {
                            break;
                        }

                        boolean skipIt = false;
                        if (p != null) {
                            String compareTo = cur.text();
                            if (stripAtBeforeRegex)
                                if (compareTo.length() > 1 && compareTo.charAt(0) == '@') {
                                    compareTo = compareTo.substring(1);
                                }
                            if (!p.matcher(compareTo).matches()) {
                                skipIt = true;
                            }
                        }

                        if (!skipIt) {
                            // NOTE: the term could exist in docs, but they might all be deleted. Unfortunately this means
                            // that we need to actually walk the TermDocs enumeration for this document to see if it is
                            // non-empty
                            if ((!hasDeletions) || (iReader.termDocs(cur).next())) {
                                callback.onTerm(cur, terms.docFreq());
                            }
                        }
                    }
                } while (terms.next());
            } finally {
                searcher.release();
            }
        }

    }

    /**
     * @return TRUE if all tokens were expanded
     *  or FALSE if no more tokens could be expanded
     */
    public boolean expandWildcardToken(Collection<String> toRet, String field,
            String token, int maxToReturn) throws ServiceException {
        // all lucene text should be in lowercase...
        token = token.toLowerCase();

        try {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                Term firstTerm = new Term(field, token);

                IndexReader iReader = searcher.getReader();

                TermEnum terms = iReader.terms(firstTerm);

                do {
                    Term cur = terms.term();
                    if (cur != null) {
                        if (!cur.field().equals(firstTerm.field())) {
                            break;
                        }

                        String curText = cur.text();

                        if (curText.startsWith(token)) {
                            if (toRet.size() >= maxToReturn) {
                                return false;
                            }
                            // we don't care about deletions, they will be filtered later
                            toRet.add(cur.text());
                        } else {
                            if (curText.compareTo(token) > 0) {
                                break;
                            }
                        }
                    }
                } while (terms.next());

                return true;
            } finally {
                searcher.release();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException opening index", e);
        }
    }

    /**
     * Force all outstanding index writes to go through. Do not return until complete
     */
    public void flush() {
        synchronized (getLock()) {
            sIndexWritersCache.flush(this);
            sIndexReadersCache.removeIndexReader(this);
        }
    }

    /**
     * @param fieldName - a lucene field (e.g. LuceneFields.L_H_CC)
     * @param collection - Strings which correspond to all of the domain terms stored in a given field.
     * @throws IOException
     */
    public void getDomainsForField(String fieldName, String regex,
            Collection<BrowseTerm> collection) throws IOException {
        if (regex == null) {
            regex = "";
        }
        enumerateTermsForField(regex, new Term(fieldName,""),
                new DomainEnumCallback(collection));
    }

    /**
     * @param collection - Strings which correspond to all of the attachment types in the index
     * @throws IOException
     */
    public void getAttachments(String regex, Collection<BrowseTerm> collection)
        throws IOException {

        if (regex == null) {
            regex = "";
        }
        enumerateTermsForField(regex, new Term(LuceneFields.L_ATTACHMENTS, ""),
                new TermEnumCallback(collection));
    }

    public void getObjects(String regex, Collection<BrowseTerm> collection)
        throws IOException {

        if (regex == null) {
            regex = "";
        }
        enumerateTermsForField(regex, new Term(LuceneFields.L_OBJECTS, ""),
                new TermEnumCallback(collection));
    }


    /**
     * @return A refcounted RefCountedIndexSearcher for this index.  Caller is responsible for
     *            calling RefCountedIndexReader.release() on the index before allowing it to go
     *            out of scope (otherwise a RuntimeException will occur)
     *
     * @throws IOException
     */
    public RefCountedIndexSearcher getCountedIndexSearcher() throws IOException {
        synchronized(getLock()) {
            RefCountedIndexSearcher searcher = null;
            RefCountedIndexReader cReader = getCountedIndexReader();
            searcher = new RefCountedIndexSearcher(cReader);
            return searcher;
        }
    }

    @Override
    public String toString() {
        return "LuceneIndex at " + mIdxDirectory.toString();
    }

    @Override
    long getLastWriteTime() {
        return mLastWriteTime;
    }

    private final Object getLock() {
        return mMbidx.getLock();
    }

    public Sort getSort(SortBy searchOrder) {
        if (searchOrder == null || searchOrder == SortBy.NONE) {
            return null;
        }

        synchronized(getLock()) {
            if ((mLatestSortBy == null) ||
                    ((searchOrder.getCriterion() != mLatestSortBy.getCriterion()) ||
                            (searchOrder.getDirection() != mLatestSortBy.getDirection()))) {
                String field;
                int type;
                boolean reverse = false;;

                if (searchOrder.getDirection() == SortDirection.DESCENDING) {
                    reverse = true;
                }

                switch (searchOrder.getCriterion()) {
                    case NAME:
                    case NAME_NATURAL_ORDER:
                    case SENDER:
                        field = LuceneFields.L_SORT_NAME;
                        type = SortField.STRING;
                        break;
                    case SUBJECT:
                        field = LuceneFields.L_SORT_SUBJECT;
                        type = SortField.STRING;
                        break;
                    case SIZE:
                        field = LuceneFields.L_SORT_SIZE;
                        type = SortField.LONG;
                        break;
                    case DATE:
                    default:
                        // default to DATE_DESCENDING!
                        field = LuceneFields.L_SORT_DATE;
                        type = SortField.STRING;
                        reverse = true;;
                        break;
                }

                mLatestSort = new Sort(new SortField(field, type, reverse));
                mLatestSortBy = searchOrder;
//
//                switch (searchOrder) {
//                    case NONE:
//                        return null;
//                    case DATE_DESCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case DATE_ASCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, false));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case SUBJ_DESCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, true));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case SUBJ_ASCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, false));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case NAME_DESCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, true));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case NAME_ASCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, false));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case SIZE_DESCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SIZE, SortField.LONG, true));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case SIZE_ASCENDING:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SIZE, SortField.LONG, false));
//                        mLatestSortBy = searchOrder;
//                        break;
//                    case SCORE_DESCENDING:
//                        return null;
//                    default:
//                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true));
//                        mLatestSortBy = SortBy.DATE_ASCENDING;
//                }
            }
            return mLatestSort;
        }
    }

    public List<SpellSuggestQueryInfo.Suggestion> suggestSpelling(String field,
            String token) throws ServiceException {
        LinkedList<SpellSuggestQueryInfo.Suggestion> toRet = null;

        token = token.toLowerCase();

        try {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                IndexReader iReader = searcher.getReader();

                Term term = new Term(field, token);
                int freq = iReader.docFreq(term);
                int numDocs = iReader.numDocs();

                if (freq == 0 && numDocs > 0) {
                    toRet = new LinkedList<SpellSuggestQueryInfo.Suggestion>();

//                    float frequency = ((float)freq)/((float)numDocs);
//
//                    int suggestionDistance = Integer.MAX_VALUE;

                    FuzzyTermEnum fuzzyEnum = new FuzzyTermEnum(iReader, term, 0.5f, 1);
                    if (fuzzyEnum != null) {
                        do {
                            Term cur = fuzzyEnum.term();
                            if (cur != null) {
                                String curText = cur.text();
                                int curDiff = editDistance(curText, token, curText.length(), token.length());

                                SpellSuggestQueryInfo.Suggestion sug = new SpellSuggestQueryInfo.Suggestion();
                                sug.mStr = curText;
                                sug.mEditDist = curDiff;
                                sug.mDocs = fuzzyEnum.docFreq();
                                toRet.add(sug);
                            }
                        } while(fuzzyEnum.next());
                    }
                }
            } finally {
                searcher.release();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException opening index", e);
        }

        return toRet;
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            if (mIdxDirectory != null)
                mIdxDirectory.close();
            mIdxDirectory = null;
        } finally {
            super.finalize();
        }
    }

    /**
     * Levenshtein distance also known as edit distance is a measure of
     * similiarity between two strings where the distance is measured as the
     * number of character deletions, insertions or substitutions required to
     * transform one string to the other string.
     * <p>
     * This method takes in four parameters; two strings and their respective
     * lengths to compute the Levenshtein distance between the two strings.
     * The result is returned as an integer.
     */
    private final int editDistance(String s, String t, int n, int m) {
        if (e.length <= n || e[0].length <= m) {
            e = new int[Math.max(e.length, n + 1)][Math.max(e[0].length, m + 1)];
        }
        int d[][] = e; // matrix
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s

        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }

        // init matrix d
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // start computing edit distance
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);
            for (j = 1; j <= m; j++) {
                if (s_i != t.charAt(j - 1)) {
                    d[i][j] = min(d[i - 1][j], d[i][j - 1], d[i - 1][j - 1]) + 1;
                } else {
                    d[i][j] = min(d[i - 1][j]+1, d[i][j - 1] + 1, d[i - 1][j - 1]);
                }
            }
        }

        // we got the result!
        return d[n][m];
    }

    private static final int MAX_TERMS_PER_QUERY =
        LC.zimbra_index_lucene_max_terms_per_query.intValue();

    /**
     * @return A refcounted IndexReader for this index.  Caller is responsible for
     *            calling IndexReader.release() on the index before allowing it to go
     *            out of scope (otherwise a RuntimeException will occur)
     *
     * @throws IOException
     */
    private RefCountedIndexReader getCountedIndexReader() throws IOException {
        BooleanQuery.setMaxClauseCount(MAX_TERMS_PER_QUERY);

        synchronized(getLock()) {
            sIndexWritersCache.flush(this); // flush writer if writing

            RefCountedIndexReader toRet = sIndexReadersCache.getIndexReader(this);
            if (toRet != null) {
                return toRet;
            }

            IndexReader reader = null;
            try {
                reader = IndexReader.open(mIdxDirectory);
            } catch(IOException e) {
                // Handle the special case of trying to open a not-yet-created
                // index, by opening for write and immediately closing.  Index
                // directory should get initialized as a result.
                File indexDir = mIdxDirectory.getFile();
                if (indexDirIsEmpty(indexDir)) {
                    beginWriting();
                    doneWriting();
                    flush();
                    try {
                        reader = IndexReader.open(mIdxDirectory);
                    } catch (IOException e1) {
                        if (reader != null)
                            reader.close();
                        throw e1;
                    }
                } else {
                    if (reader != null)
                        reader.close();
                    throw e;
                }
            }

            synchronized(mOpenReaders) {
                toRet = new RefCountedIndexReader(this, reader); // refcount starts at 1
                mOpenReaders.add(toRet);
            }

            sIndexReadersCache.putIndexReader(this, toRet); // addrefs if put in cache
            return toRet;
        }
    }

    /**
     * Check to see if it is OK for us to create an index in the specified
     * directory.
     *
     * @param indexDir
     * @return TRUE if the index directory is empty or doesn't exist,
     *         FALSE if the index directory exists and has files in it or if we cannot list files in the directory
     * @throws IOException
     */
    private boolean indexDirIsEmpty(File indexDir) {
        if (!indexDir.exists()) {
            // dir doesn't even exist yet.  Create the parents and return true
            indexDir.mkdirs();
            return true;
        }

        // Empty directory is okay, but a directory with any files
        // implies index corruption.

        File[] files = indexDir.listFiles();

        // if files is null here, we are likely running into file permission issue
        // log a WARN and return false
        if (files == null) {
            ZimbraLog.index.warn("Could not list files in directory " + indexDir.getAbsolutePath());
            return false;
        }

        int numFiles = 0;
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String fname = f.getName();
            if (f.isDirectory() && (fname.equals(".") || fname.equals("..")))
                continue;
            numFiles++;
        }
        return (numFiles <= 0);
    }

    private void doneWriting() throws IOException {
        assert(Thread.holdsLock(getLock()));
        assert(beginWritingNestLevel > 0);


        /*
         * assertion is by default off in production
         *
         * If beginWritingNestLevel is 0 before the decrement, the corresponding
         * beginWriting probably got an IOException so beginWritingNestLevel didn't
         * get incremented.
         *
         * If assertion is off, we really don't want to proceed here, otherwise the
         * beginWritingNestLevel will become negative, which is a situation that
         * cannot be recovered until a server restart.
         */
        if (beginWritingNestLevel == 0) {
            ZimbraLog.index.warn("beginWritingNestLevel is 0 in LuceneIndex.doneWriting, flushing skipped.");
            return;
        }

        beginWritingNestLevel--;
        if (beginWritingNestLevel == 0) {
            if (mNumUncommittedItems > sMaxUncommittedOps) {
                if (ZimbraLog.index_add.isDebugEnabled()) {
                    ZimbraLog.index_add.debug("Flushing " + toString() + " because of too many uncommitted redo ops");
                }
                flush();
            } else {
                sIndexWritersCache.doneWriting(this);
            }
            updateLastWriteTime();
        }
    }

    public void beginWriteOperation() throws IOException {
        assert(Thread.holdsLock(getLock()));
        beginWriting();
    }

    public void endWriteOperation() throws IOException {
        assert(Thread.holdsLock(getLock()));
        doneWriting();
    }

    private void beginWriting() throws IOException {
        assert(Thread.holdsLock(getLock()));

        if (beginWritingNestLevel == 0) {
            // uncache the IndexReader if it is cached
            sIndexReadersCache.removeIndexReader(this);
            sIndexWritersCache.beginWriting(this);
        }
        beginWritingNestLevel++;
    }

    @Override
    void doWriterOpen() throws IOException {
        if (mIndexWriter != null) {
            return; // already open!
        }

        assert(Thread.holdsLock(getLock()));

        boolean useBatchIndexing;
        try {
            useBatchIndexing = mMbidx.useBatchedIndexing();
        } catch (ServiceException e) {
            throw new IOException("Caught IOException checking BatchedIndexing flag " + e);
        }

        final LuceneConfigSettings.Config config;
        if (useBatchIndexing) {
            config = LuceneConfigSettings.batched;
        } else {
            config = LuceneConfigSettings.nonBatched;
        }

        try {
            // TODO: In 3.0, IndexWriter will no longer accept autoCommit=true.
            // Call commit() yourself when needed.
            mIndexWriter = new IndexWriter(mIdxDirectory, mMbidx.getAnalyzer(),
                    false, IndexWriter.MaxFieldLength.LIMITED);
            if (ZimbraLog.index_lucene.isDebugEnabled()) {
                mIndexWriter.setInfoStream(new PrintStream(
                        new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
            }
        } catch (IOException e) {
            //
            // the index (the segments* file in particular) probably didn't exist when new IndexWriter
            // was called in the try block, we would get a FileNotFoundException for that case.
            // If the directory is empty, this is the very first index write for this this mailbox
            // (or the index might be deleted), the FileNotFoundException is benign.
            //
            // If the directory is empty, try again with the create flag set to true.
            //
            // If e1 is other IOException, our second try will likely throw another IOException.
            // If the directory is not empty, we throw an IOException and set e1 as the cause.
            // For both case, the IOException will be logged at outer code.
            //
            // Log it as at DEBUG level instead of ERROR here.
            //
            ZimbraLog.index_add.debug("Caught exception trying to open index: " + e, e);
            File indexDir  = mIdxDirectory.getFile();
            if (indexDirIsEmpty(indexDir)) {
                mIndexWriter = new IndexWriter(mIdxDirectory, mMbidx.getAnalyzer(),
                        true, IndexWriter.MaxFieldLength.LIMITED);
                if (ZimbraLog.index_lucene.isDebugEnabled()) {
                    mIndexWriter.setInfoStream(new PrintStream(
                            new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
                }
                if (mIndexWriter == null) {
                    throw new IOException("Failed to open IndexWriter in directory " +
                            indexDir.getAbsolutePath());
                }
            } else {
                mIndexWriter = null;
                IOException ioe = new IOException("Could not create index " +
                        mIdxDirectory.toString() + " (directory already exists)");
                ioe.initCause(e);
                throw ioe;
            }
        }

        if (config.useSerialMergeScheduler) {
            mIndexWriter.setMergeScheduler(new SerialMergeScheduler());
        }

        mIndexWriter.setMaxBufferedDocs(config.maxBufferedDocs);
        mIndexWriter.setRAMBufferSizeMB(((double) config.ramBufferSizeKB) / 1024.0);
        mIndexWriter.setMergeFactor(config.mergeFactor);

        if (config.useDocScheduler) {
            LogDocMergePolicy policy = new LogDocMergePolicy(mIndexWriter);
            mIndexWriter.setMergePolicy(policy);
            policy.setUseCompoundDocStore(config.useCompoundFile);
            policy.setUseCompoundFile(config.useCompoundFile);
            policy.setMergeFactor(config.mergeFactor);
            policy.setMinMergeDocs((int) config.minMerge);
            if (config.maxMerge != Integer.MAX_VALUE) {
                policy.setMaxMergeDocs((int) config.maxMerge);
            }
        } else {
            LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy(mIndexWriter);
            mIndexWriter.setMergePolicy(policy);
            policy.setUseCompoundDocStore(config.useCompoundFile);
            policy.setUseCompoundFile(config.useCompoundFile);
            policy.setMergeFactor(config.mergeFactor);
            policy.setMinMergeMB(((double) config.minMerge) / 1024.0);
            if (config.maxMerge != Integer.MAX_VALUE) {
                policy.setMaxMergeMB(((double) config.maxMerge) / 1024.0);
            }
        }
    }

    @Override
    void doWriterClose() {
        if (mIndexWriter == null) {
            return;
        }

        if (ZimbraLog.index_add.isDebugEnabled()) {
            ZimbraLog.index_add.debug("Closing IndexWriter " + mIndexWriter + " for " + this);
        }

        IndexWriter writer = mIndexWriter;
        mIndexWriter = null;

        boolean success = false;
        try {
            // Flush all changes to file system before committing redos.
            writer.close();
            success = true;
        } catch (IOException e) {
            ZimbraLog.index_add.error("Caught Exception " + e + " in LuceneIndex.closeIndexWriter", e);
            // fall through to finally here with success=false
        } finally {
            if (mNumUncommittedItems > 0) {
                assert(mHighestUncomittedModContent.getChangeId() > 0);
                mMbidx.indexingCompleted(mNumUncommittedItems, mHighestUncomittedModContent, success);
            }
            mNumUncommittedItems = 0;
            mHighestUncomittedModContent = new SyncToken(0);
        }
    }

    private void updateLastWriteTime() {
        mLastWriteTime = System.currentTimeMillis();
    }

    static abstract class DocEnumInterface {
        void maxDocNo(int num) {
        };
        abstract boolean onDocument(Document doc, boolean isDeleted);
    }

    static class DomainEnumCallback implements TermEnumInterface {
        DomainEnumCallback(Collection<BrowseTerm> collection) {
            mCollection = collection;
        }

        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1 && text.charAt(0) == '@') {
                mCollection.add(new BrowseTerm(text.substring(1), docFreq));
            }
        }
        private Collection<BrowseTerm> mCollection;
    }

    static class TermEnumCallback implements TermEnumInterface {
        TermEnumCallback(Collection<BrowseTerm> collection) {
            mCollection = collection;
        }

        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1) {
                mCollection.add(new BrowseTerm(text, docFreq));
            }
        }
        private Collection<BrowseTerm> mCollection;
    }

    interface TermEnumInterface {
        abstract void onTerm(Term term, int docFreq);
    }

    public void onReaderClose(RefCountedIndexReader ref) {
        synchronized(mOpenReaders) {
            mOpenReaders.remove(ref);
        }
    }

    private List<RefCountedIndexReader> mOpenReaders = new ArrayList<RefCountedIndexReader>();

    public IndexReader reopenReader(IndexReader reader) throws IOException {
        return reader.reopen();
    }

    long getMailboxId() {
        return mMbidx.getMailboxId();
    }

}
