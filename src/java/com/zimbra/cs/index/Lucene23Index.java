 /* ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexCommitPoint;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * 
 */
class Lucene23Index implements ILuceneIndex, ITextIndex, IndexDeletionPolicy  {
    
    static {
        System.setProperty("org.apache.lucene.FSDirectory.class", "com.zimbra.cs.index.Z23FSDirectory");
    }
    
//    private static final boolean sBatchIndexing = (LC.debug_batch_message_indexing.intValue() > 0);
//    private static final boolean sLuceneAutocommit = LC.zimbra_index_lucene_autocommit.booleanValue();
    private static final boolean sUseDeletionPolicy = LC.zimbra_index_use_nfs_deletion_policy.booleanValue();
    
    static void flushAllWriters() {
        if (DebugConfig.disableIndexing)
            return;
        
        List<Lucene23Index> toFlush;
        synchronized(sOpenIndexWriters) {
            toFlush = new ArrayList<Lucene23Index>(sOpenIndexWriters.size());
            toFlush.addAll(sOpenIndexWriters.keySet());
        }
        
        for (Lucene23Index idx : toFlush) {
            idx.closeIndexWriter(); 
        }
    }

    static void shutdown() {
        if (DebugConfig.disableIndexing)
            return;

        sSweeper.signalShutdown();
        try {
            sSweeper.join();
        } catch (InterruptedException e) {}
        
        sIndexReadersCache.signalShutdown();
        try {
            sIndexReadersCache.join();
        } catch (InterruptedException e) {}

        flushAllWriters();
    }

    static void startup() {
        if (DebugConfig.disableIndexing) {
            ZimbraLog.index.info("Indexing is disabled by the localconfig 'debug_disable_indexing' flag");
            return;
        }
        
        // In case startup is called twice in a row without shutdown in between
        if (sSweeper != null && sSweeper.isAlive()) {
            shutdown();
        }
        
        sMaxUncommittedOps = LC.zimbra_index_max_uncommitted_operations.intValue();
        sLRUSize = LC.zimbra_index_lru_size.intValue();
        if (sLRUSize < 10) sLRUSize = 10;
        sIdleWriterFlushTimeMS = 1000 * LC.zimbra_index_idle_flush_time.intValue();
        
        sSweeperFrequencyMs = Constants.MILLIS_PER_SECOND * LC.zimbra_index_sweep_frequency.longValue();
        if (sSweeperFrequencyMs <= 0)
            sSweeperFrequencyMs = Constants.MILLIS_PER_SECOND;
        
        sSweeper = new IndexWritersSweeper();
        sSweeper.start();
        
        sIndexReadersCache = new IndexReadersCache(LC.zimbra_index_reader_lru_size.intValue(), 
            LC.zimbra_index_reader_idle_flush_time.longValue() * 1000, 
            LC.zimbra_index_reader_idle_sweep_frequency.longValue() * 1000);
        sIndexReadersCache.start();
    }

    /**
    Finds and returns the smallest of three integers 
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
    
    
    Lucene23Index(MailboxIndex mbidx, String idxParentDir, int mailboxId) throws ServiceException {
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
            if (!parentDirFile.exists())
                parentDirFile.mkdirs();

            if (!parentDirFile.canRead()) {
                throw ServiceException.FAILURE("Cannot READ index directory (mailbox="+mailboxId+ " idxPath="+idxPath+")", null);
            }
            if (!parentDirFile.canWrite()) {
                throw ServiceException.FAILURE("Cannot WRITE index directory (mailbox="+mailboxId+ " idxPath="+idxPath+")", null);
            }

            // the Lucene code does not atomically swap the "segments" and "segments.new"
            // files...so it is possible that a previous run of the server crashed exactly in such
            // a way that we have a "segments.new" file but not a "segments" file.  We we will check here 
            // for the special situation that we have a segments.new
            // file but not a segments file...
            File segments = new File(idxPath, "segments");
            if (!segments.exists()) {
                File segments_new = new File(idxPath, "segments.new");
                if (segments_new.exists()) 
                    segments_new.renameTo(segments);
            }
            
            try {
                // must call getDirectory then setLockFactory via 2 calls -- there's the possibility
                // that the directory we're returned is actually a cached FSDirectory (e.g. if the index
                // was deleted and re-created) in which case we should be using the existing LockFactory
                // and not creating a new one
                mIdxDirectory = (Z23FSDirectory)FSDirectory.getDirectory(idxPath);
                if (mIdxDirectory.getLockFactory() == null || !(mIdxDirectory.getLockFactory() instanceof SingleInstanceLockFactory))
                    mIdxDirectory.setLockFactory(new SingleInstanceLockFactory());
            } catch (IOException e) {
                throw ServiceException.FAILURE("Cannot create FSDirectory at path: "+idxPath, e);
            }
        }
    }

    public void addDocument(IndexItem redoOp, Document doc, int indexId, long receivedDate, 
        String sortSubject, String sortSender, boolean deleteFirst) throws IOException {
        addDocument(redoOp, new Document[] { doc }, indexId, receivedDate, sortSubject, sortSender, deleteFirst);
    }
    
    public void addDocument(IndexItem redoOp, Document[] docs, int indexId, long receivedDate, 
        String sortSubject, String sortSender, boolean deleteFirst) throws IOException {
        if (docs.length == 0)
            return;
        
        long start = 0;
        synchronized(getLock()) {        
            
            openIndexWriter();
            try {
                assert(mIndexWriterMutex.isHeldByCurrentThread());
                assert(mIndexWriter != null);

                for (Document doc : docs) {
                    // doc can be shared by multiple threads if multiple mailboxes
                    // are referenced in a single email
                    synchronized (doc) {
                        doc.removeFields(LuceneFields.L_SORT_SUBJECT);
                        doc.removeFields(LuceneFields.L_SORT_NAME);
                        //                                                                                                  store, index, tokenize
                        doc.add(new Field(LuceneFields.L_SORT_SUBJECT, sortSubject, Field.Store.NO, Field.Index.UN_TOKENIZED));
                        doc.add(new Field(LuceneFields.L_SORT_NAME,    sortSender, Field.Store.NO, Field.Index.UN_TOKENIZED));
                        
                        doc.removeFields(LuceneFields.L_MAILBOX_BLOB_ID);
                        doc.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, Integer.toString(indexId), Field.Store.YES, Field.Index.UN_TOKENIZED));
                        
                        // If this doc is shared by mult threads, then the date might just be wrong,
                        // so remove and re-add the date here to make sure the right one gets written!
                        doc.removeFields(LuceneFields.L_SORT_DATE);
                        String dateString = DateField.timeToString(receivedDate);
                        doc.add(new Field(LuceneFields.L_SORT_DATE, dateString, Field.Store.YES, Field.Index.UN_TOKENIZED));
                        
                        if (null == doc.get(LuceneFields.L_ALL)) {
                            doc.add(new Field(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
                        }
                        
                        if (deleteFirst) {
                            String itemIdStr = Integer.toString(indexId);
                            Term toDelete = new Term(LuceneFields.L_MAILBOX_BLOB_ID, itemIdStr);
                            mIndexWriter.updateDocument(toDelete, doc);
                        } else {
                            mIndexWriter.addDocument(doc);
                        }
                        
                    } // synchronized(doc)
                } // foreach Document

                if (redoOp != null) {
                    mUncommittedRedoOps.add(redoOp);
                }
                
                // tim: this might seem bad, since an index in steady-state-of-writes will never get flushed, 
                // however we also track the number of uncomitted-operations on the index, and will force a 
                // flush if the index has had a lot written to it without a flush.
                updateLastWriteTime();
            } finally {
                mIndexWriterMutex.unlock();
            }

            if (mUncommittedRedoOps.size() > sMaxUncommittedOps) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Flushing " + toString() + " because of too many uncommitted redo ops");
                }
                flush();
            }
        }
    }
    
//    int countTermOccurences(String fieldName, String term) throws IOException {
//        RefCountedIndexReader reader = getCountedIndexReader();
//        try {
//            TermEnum e = reader.getReader().terms(new Term(fieldName, term));
//            return e.docFreq();
//        } finally {
//            reader.release();
//        }
//    }

    public int[] deleteDocuments(int itemIds[]) throws IOException {
        synchronized(getLock()) {
            openIndexWriter();
            try {
                for (int i = 0; i < itemIds.length; i++) {
                    try {
                        String itemIdStr = Integer.toString(itemIds[i]);
                        Term toDelete = new Term(LuceneFields.L_MAILBOX_BLOB_ID, itemIdStr);
                        mIndexWriter.deleteDocuments(toDelete);
                        // NOTE!  The numDeleted may be < you expect here, the document may
                        // already be deleted and just not be optimized out yet -- some lucene
                        // APIs (e.g. docFreq) will still return the old count until the indexes 
                        // are optimized...
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("Deleted index documents for itemId "+itemIdStr);
                        }
                    } catch (IOException ioe) {
                        sLog.debug("deleteDocuments exception on index "+i+" out of "+itemIds.length+" (id="+itemIds[i]+")");
                        int[] toRet = new int[i];
                        System.arraycopy(itemIds,0,toRet,0,i);
                        return toRet;
                    }
                }
            } finally {
                mIndexWriterMutex.unlock();
            }
            return itemIds; // success
        }
    }

    public void deleteIndex() throws IOException
    {
        synchronized(getLock()) {        
            IndexWriter writer = null;
            try {
                flush();
                // FIXME maybe: under Windows only, this can fail.  Might need way to forcibly close all open indices???
                //              closeIndexReader();
                if (sLog.isDebugEnabled())
                    sLog.debug("****Deleting index " + mIdxDirectory.toString());

                // can use default analyzer here since it is easier, and since we aren't actually
                // going to do any indexing...
                writer = new IndexWriter(mIdxDirectory, true, ZimbraAnalyzer.getDefaultAnalyzer(), true, (sUseDeletionPolicy ? this : null));
                
                if (ZimbraLog.index_lucene.isDebugEnabled())
                    writer.setInfoStream(new PrintStream(new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

//    void enumerateDocuments(DocEnumInterface c) throws IOException {
//        synchronized(getLock()) {        
//            RefCountedIndexReader reader = this.getCountedIndexReader();
//            try {
//                IndexReader iReader = reader.getReader();
//                int maxDoc = iReader.maxDoc();
//                c.maxDocNo(maxDoc);
//                for (int i = 0; i < maxDoc; i++) {
//                    if (!c.onDocument(iReader.document(i), iReader.isDeleted(i))) {
//                        return;
//                    }
//                }
//            } finally {
//                reader.release();
//            }
//        }        
//    }
////    
    
    public void checkBlobIds() throws IOException {
        sLog.warn("Starting blob ID check for index "+this.toString());
        List<String> l = new LinkedList<String>();
        enumerateTermsForField(new Term(LuceneFields.L_MAILBOX_BLOB_ID, ""),new TermEnumCallback(l));
        
        for (String s : l) {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                Query q = new TermQuery(new Term(LuceneFields.L_MAILBOX_BLOB_ID, s));
                Hits hits = searcher.getSearcher().search(q);
                for (int i = 0; i < hits.length(); i++) {
                    Document d = hits.doc(i);
                    String blobId = d.get(LuceneFields.L_MAILBOX_BLOB_ID);
                    if (blobId == null || !blobId.equals(s)) {
                        sLog.warn("Stored IndexId does not match indexed ItemId (stored="+blobId+" indexed="+s);
                    }
                }
                    
            } finally {
                searcher.release();
            }
        }
        sLog.warn("Completed blob ID check for index "+this.toString());
    }
    
    private void enumerateTermsForField(Term firstTerm, TermEnumInterface callback) throws IOException
    {
        synchronized(getLock()) {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                IndexReader iReader = searcher.getReader();

                TermEnum terms = iReader.terms(firstTerm);
                boolean hasDeletions = iReader.hasDeletions();

                do {
                    Term cur = terms.term();
                    if (cur != null) {
                        if (!cur.field().equals(firstTerm.field())) {
                            break;
                        }

                        // NOTE: the term could exist in docs, but they might all be deleted. Unfortunately this means  
                        // that we need to actually walk the TermDocs enumeration for this document to see if it is
                        // non-empty
                        if ((!hasDeletions) || (iReader.termDocs(cur).next())) {
                            callback.onTerm(cur, terms.docFreq());
                        }
                    }
                } while (terms.next());
            } finally {
                searcher.release();
            }
        }
        
    }

    /**
     * @return TRUE if all tokens were expanded or FALSE if no more tokens could be expanded
     */
    public boolean expandWildcardToken(Collection<String> toRet, String field, String token, int maxToReturn) throws ServiceException 
    {
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
                            if (toRet.size() >= maxToReturn) 
                                return false;

                            // we don't care about deletions, they will be filtered later
                            toRet.add(cur.text());
                        } else {
                            if (curText.compareTo(token) > 0)
                                break;
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
     * Force all outstanding index writes to go through.  
     */
    public void flush() {
        synchronized(getLock()) {
            if (isIndexWriterOpen()) 
                closeIndexWriter();
            else
                sIndexReadersCache.removeIndexReader(this);
        }
    }

//    private static class ChkIndexStage1Callback implements TermEnumInterface 
//    {
//        HashSet msgsInMailbox = new HashSet(); // hash of all messages in my mailbox
//        private MailboxIndex idx = null;
//        private ArrayList<Integer> toDelete = new ArrayList<Integer>(); // to be deleted from index
//        SearchResult compareTo = new SearchResult();  
//
//        ChkIndexStage1Callback(MailboxIndex idx) {
//            this.idx = idx;
//        }
//
//        void doIndexRepair() throws IOException 
//        {
//            
//            Mailbox mbox = null;
//            try {
//                mbox = MailboxManager.getInstance().getMailboxById(idx.mMailboxId);
//            } catch (ServiceException e) {
//                sLog.error("Could not get mailbox: "+idx.mMailboxId+" aborting index repair");
//                return;
//            }
//                
//            // delete first -- that way if there were any re-indexes along the way we know we're OK
//            if (toDelete.size() > 0) {
//                sLog.info("There are "+toDelete.size()+" items to delete");
//                int ids[] = new int[toDelete.size()];
//                for (int i = 0; i < toDelete.size(); i++) {
//                    ids[i] = ((Integer)(toDelete.get(i))).intValue();
//                }
//                idx.deleteDocuments(ids);
//            }
//            
//            
//            // if there any messages left in this list, then they are missing from the index and 
//            // we should try to reindex them
//            if (msgsInMailbox.size() > 0)
//            {
//                sLog.info("There are "+msgsInMailbox.size() + " msgs to be re-indexed");
//                for (Iterator iter = msgsInMailbox.iterator(); iter.hasNext();) {
//                    SearchResult cur = (SearchResult)iter.next();
//
//                    try {
//                        MailItem item = mbox.getItemById(null, cur.id, cur.type);
//                        item.reindex(null, false /* already deleted above */, null);
//                    } catch(ServiceException  e) {
//                        sLog.info("Couldn't index "+compareTo.id+" caught ServiceException", e);
//                    } catch(java.lang.RuntimeException e) {
//                        sLog.info("Couldn't index "+compareTo.id+" caught ServiceException", e);
//                    }
//                }
//            }
//        }
//
//        public void onTerm(Term term, int docFreq) 
//        {
//            compareTo.id = Integer.parseInt(term.text());
//
//            if (!msgsInMailbox.contains(compareTo)) {
//                sLog.info("In index but not DB: "+compareTo.id);
//                toDelete.add(new Integer(compareTo.id));
//            } else {
//                // remove from the msgsInMailbox hash.  If there are still entries in this
//                // table, then it means that there are items in the mailbox, but not in the index
//                msgsInMailbox.remove(compareTo);
//            }
//        }
//    }
//
//    private static class ChkIndexStage2Callback 
//    {
//        public List msgsInMailbox = new LinkedList(); // hash of all messages in my mailbox
//        private ListIterator msgsIter;
//
//        private String mSortField;
//        SearchResult mCur = null;
//
//
//        ChkIndexStage2Callback(MailboxIndex idx, String sortField, boolean reversed) {
//            mSortField = sortField;
//            this.reversed = reversed;
//        }
//
//        boolean beginIterating() {
//            msgsIter = msgsInMailbox.listIterator();
//            mCur = (SearchResult)msgsIter.next();
//            return (mCur!= null);
//        }
//
//        boolean reversed = false;
//
//        long compare(long lhs, long rhs) {
//            if (!reversed) {
//                return (lhs - rhs);
//            } else {
//                return (rhs - lhs);
//            }
//        }
//
//        void onDocument(Document doc) 
//        {
//            int idxId = Integer.parseInt(doc.get(LuceneFields.L_MAILBOX_BLOB_ID));
//
//            String sortField = doc.get(mSortField);
//            String partName = doc.get(LuceneFields.L_PARTNAME);
//            String dateStr = doc.get(LuceneFields.L_SORT_DATE);
//            long docDate = DateField.stringToTime(dateStr);
//            // fix for Bug 311 -- SQL truncates dates when it stores them
//            long truncDocDate = (docDate /1000) * 1000;
//
//            retry: do {
//                long curMsgDate = ((Long)(mCur.sortkey)).longValue();
//
//
//                if (mCur.id == idxId) {
//                    // next part same doc....good.  keep going..
//                    if (curMsgDate != truncDocDate) {
//                        sLog.info("WARN  : DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
//                                    " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
//                                    " "+mSortField+"="+sortField);
//
//                        sLog.info("\tWARNING: DB-DATE doesn't match TRUNCDATE!");
//                    } else {
//                        sLog.debug("OK    : DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
//                                    " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
//                                    " "+mSortField+"="+sortField);
//                    }
//                    return;
//                } else {
//                    if (false) {
////                      if (!msgsIter.hasNext()) {
////                      if (mMissingTerms == null) {
////                      mLog.info("ERROR: end of msgIter while still iterating index");
////                      }
////                      mLog.info("ERROR: DB no results INDEX has mailitem: "+idxId);
////                      return;
//                    } else {
//                        // 3 possibilities:
//                        //    doc < cur
//                        //    doc > cur
//                        //    doc == cur
////                      if (truncDocDate < curMsgDate) { // case 1
//                        if (compare(truncDocDate,curMsgDate) < 0) { // case 1
//                            sLog.info("ERROR1: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
//                                        " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
//                                        " "+mSortField+"="+sortField);
//
//                            // move on to next document
//                            return;
////                          } else if (truncDocDate > curMsgDate) { // case 2
//                        } else if (compare(truncDocDate,curMsgDate)>0) { // case 2
////                          mLog.info("ERROR2: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
////                          " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
////                          " "+mSortField+"="+sortField);
//
//                            if (!msgsIter.hasNext()) {
//                                sLog.info("ERROR4: DB no results INDEX has mailitem: "+idxId);
//                                return;
//                            }
//                            mCur = (SearchResult)msgsIter.next();
//
//                            continue; // try again!
//                        } else { // same date!
//                            // 1st,look backwards for a match
//                            if (msgsIter.hasPrevious()) {
//                                do {
//                                    mCur = (SearchResult)msgsIter.previous();
//                                    if (mCur.id == idxId) {
//                                        continue retry;
//                                    }
//                                    curMsgDate = ((Long)(mCur.sortkey)).longValue();
//                                } while(msgsIter.hasPrevious() && curMsgDate == truncDocDate);
//
//                                // Move the iterator fwd one, so it is on the correct time...
//                                mCur = (SearchResult)msgsIter.next();
//
//                            }
//
//                            // now, look fwd.  Sure, we might check some twice here.  Oh well
//                            if (msgsIter.hasNext()) {
//                                do {
//                                    mCur = (SearchResult)msgsIter.next();
//                                    if (mCur.id == idxId) {
//                                        continue retry;
//                                    }
//                                    curMsgDate = ((Long)(mCur.sortkey)).longValue();
//                                } while (msgsIter.hasNext() && curMsgDate == truncDocDate);
//
//                                // Move the iterator back one, so it is on the correct time...
//                                mCur = (SearchResult)msgsIter.previous();
//                            }
//
//
//                            sLog.info("ERROR3: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
//                                        " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
//                                        " "+mSortField+"="+sortField);
//                            return;
//                        } // big if...else
//                    } // has a next
//                } // else if IDs don't mtch
//            } while(true);
//        } // func
//
//    } // class
//
//    void chkIndex(boolean repair) throws ServiceException 
//    {
//        synchronized(getLock()) {        
//            flush();
//
//            Connection conn = null;
//            conn = DbPool.getConnection();
//            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
//
//
//            ///////////////////////////////
//            //
//            // Stage 1 -- look for missing or extra messages and reindex/delete as necessary.
//            //
//            {
//                DbSearchConstraints c = new DbSearchConstraints();
//
//                c.mailbox = mbox;
//                c.sort = DbSearch.SORT_BY_DATE;
//                c.types = new HashSet<Byte>();
//                c.types.add(MailItem.TYPE_CONTACT); 
//                c.types.add(MailItem.TYPE_MESSAGE);
//                c.types.add(MailItem.TYPE_NOTE);
//
//                ChkIndexStage1Callback callback = new ChkIndexStage1Callback(this);
//
//                DbSearch.search(callback.msgsInMailbox, conn, c);
//                sLog.info("Verifying (repair="+(repair?"TRUE":"FALSE")+") Index for Mailbox "+this.mMailboxId+" with "+callback.msgsInMailbox.size()+" items.");
//
//                try {
//                    this.enumerateTermsForField(new Term(LuceneFields.L_MAILBOX_BLOB_ID, ""), callback);
//                } catch (IOException e) {
//                    throw ServiceException.FAILURE("Caught IOException while enumerating fields", e);
//                }
//
//                sLog.info("Stage 1 Verification complete for Mailbox "+this.mMailboxId);
//
//                if (repair) {
//                    try {
//                        sLog.info("Attempting Stage 1 Repair for mailbox "+this.mMailboxId);
//                        callback.doIndexRepair();
//                    } catch (IOException e) {
//                        throw ServiceException.FAILURE("Caught IOException while repairing index", e);
//                    }
//                    flush();
//                }
//            }
//
//            /////////////////////////////////
//            //
//            // Stage 2 -- verify SORT_BY_DATE orders match up
//            //
//            {
//                sLog.info("Stage 2 Verify SORT_DATE_ASCENDNIG for Mailbox "+this.mMailboxId);
//
//                // SORT_BY__DATE_ASC
//                DbSearchConstraints c = new DbSearchConstraints();
//
//                c.mailbox = mbox;
//                c.sort = DbSearch.SORT_BY_DATE | DbSearch.SORT_ASCENDING;
//                c.types = new HashSet<Byte>();
//                c.types.add(MailItem.TYPE_CONTACT); 
//                c.types.add(MailItem.TYPE_MESSAGE);
//                c.types.add(MailItem.TYPE_NOTE);
//
//                String lucSortField = LuceneFields.L_SORT_DATE;
//
//                ChkIndexStage2Callback callback = new ChkIndexStage2Callback(this, lucSortField, false);
//
//                DbSearch.search(callback.msgsInMailbox, conn, c);
//                RefCountedIndexSearcher searcher = null;
//                try {
//                    callback.beginIterating();
//                    searcher = getCountedIndexSearcher();
//
//                    TermQuery q = new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE));
//                    Hits luceneHits = searcher.getSearcher().search(q, getSort(SortBy.DATE_ASCENDING));
//
//                    for (int i = 0; i < luceneHits.length(); i++) {
//                        callback.onDocument(luceneHits.doc(i));
//                    }
//                } catch (IOException e) {
//                    throw ServiceException.FAILURE("Caught IOException while enumerating fields", e);
//                } finally {
//                    if (searcher != null) {
//                        searcher.release();
//                    }
//                }
//
//                sLog.info("Stage 2 Verification complete for Mailbox "+this.mMailboxId);
//
//            }
//
//            /////////////////////////////////
//            //
//            // Stage 3 -- verify SORT_BY_DATE orders match up
//            //
//            {
//                sLog.info("Stage 3 Verify SORT_DATE_DESCENDING for Mailbox "+this.mMailboxId);
//
//                // SORT_BY__DATE_DESC
//                DbSearchConstraints c = new DbSearchConstraints();
//
//                c.mailbox = mbox;
//                c.sort = DbSearch.SORT_BY_DATE | DbSearch.SORT_DESCENDING;
//
//                c.types = new HashSet<Byte>();
//                c.types.add(MailItem.TYPE_CONTACT); 
//                c.types.add(MailItem.TYPE_MESSAGE);
//                c.types.add(MailItem.TYPE_NOTE);
//
//
//                String lucSortField = LuceneFields.L_SORT_DATE;
//
//                ChkIndexStage2Callback callback = new ChkIndexStage2Callback(this, lucSortField, true);
//
//                DbSearch.search(callback.msgsInMailbox, conn, c);
//                RefCountedIndexSearcher searcher = null;
//                try {
//                    callback.beginIterating();
//                    searcher = getCountedIndexSearcher();
//
//                    TermQuery q = new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE));
//                    Hits luceneHits = searcher.getSearcher().search(q, getSort(SortBy.DATE_DESCENDING));
//
//                    for (int i = 0; i < luceneHits.length(); i++) {
//                        callback.onDocument(luceneHits.doc(i));
//                    }
//                } catch (IOException e) {
//                    throw ServiceException.FAILURE("Caught IOException while enumerating fields", e);
//                } finally {
//                    if (searcher != null) {
//                        searcher.release();
//                    }
//                }
//
//                sLog.info("Stage 3 Verification complete for Mailbox "+this.mMailboxId);
//            }
//        }
//    }

    /**
     * @param fieldName - a lucene field (e.g. LuceneFields.L_H_CC)
     * @param collection - Strings which correspond to all of the domain terms stored in a given field.
     * @throws IOException
     */
    public void getDomainsForField(String fieldName, Collection<String> collection) throws IOException
    {
        enumerateTermsForField(new Term(fieldName,""),new DomainEnumCallback(collection));
    }
    
    /**
     * @param collection - Strings which correspond to all of the attachment types in the index
     * @throws IOException
     */
    public void getAttachments(Collection<String> collection) throws IOException
    {
        enumerateTermsForField(new Term(LuceneFields.L_ATTACHMENTS,""), new TermEnumCallback(collection));
    }

    public void getObjects(Collection<String> collection) throws IOException
    {
        enumerateTermsForField(new Term(LuceneFields.L_OBJECTS,""), new TermEnumCallback(collection));
    }

  
    /**
     * @return A refcounted RefCountedIndexSearcher for this index.  Caller is responsible for 
     *            calling RefCountedIndexReader.release() on the index before allowing it to go
     *            out of scope (otherwise a RuntimeException will occur)
     * 
     * @throws IOException
     */
    public RefCountedIndexSearcher getCountedIndexSearcher() throws IOException
    {
        synchronized(getLock()) {        
            RefCountedIndexSearcher searcher = null;
            RefCountedIndexReader cReader = getCountedIndexReader();
            searcher = new RefCountedIndexSearcher(cReader);
            return searcher;
        }
    }

    public String toString() { return "Lucene23Index at "+mIdxDirectory.toString(); }

//    Collection getFieldNames() throws IOException {
//        synchronized(getLock()) {        
//            RefCountedIndexReader reader = this.getCountedIndexReader();
//            try {
//                IndexReader iReader = reader.getReader();
//                return iReader.getFieldNames(FieldOption.ALL);
//            } finally {
//                reader.release(); 
//            }
//        }
//    }
    
    private long getLastWriteTime() { return mLastWriteTime; }

    private final Object getLock() { return mMbidx.getLock(); }

    public Sort getSort(SortBy searchOrder) {
        synchronized(getLock()) {
            if (searchOrder != mLatestSortBy) { 
                switch (searchOrder) {
                    case NONE:
                        return null;
                    case DATE_DESCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true));
                        mLatestSortBy = searchOrder;
                        break;
                    case DATE_ASCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, false));
                        mLatestSortBy = searchOrder;
                        break;
                    case SUBJ_DESCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, true));
                        mLatestSortBy = searchOrder;
                        break;
                    case SUBJ_ASCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, false));
                        mLatestSortBy = searchOrder;
                        break;
                    case NAME_DESCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, true));
                        mLatestSortBy = searchOrder;
                        break;
                    case NAME_ASCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, false));
                        mLatestSortBy = searchOrder;
                        break;
                    case SCORE_DESCENDING:
                        return null;
                    default:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true));
                       mLatestSortBy = SortBy.DATE_ASCENDING;
                }
            }
            return mLatestSort;
        }
    }
    
    public List<SpellSuggestQueryInfo.Suggestion> suggestSpelling(String field, String token) throws ServiceException {
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
//    protected Spans getSpans(SpanQuery q) throws IOException {
//        synchronized(getLock()) {        
//            RefCountedIndexReader reader = this.getCountedIndexReader();
//            try {
//                IndexReader iReader = reader.getReader();
//                return q.getSpans(iReader);
//            } finally {
//                reader.release();
//            }
//        }
//    };

    /**
     * Close the index writer and write commit/abort entries for all
     * pending IndexItem redo operations.
     */
    private void closeIndexWriter() 
    {
        assert(!mIndexWriterMutex.isHeldByCurrentThread());
        int sizeAfter = -1;
        synchronized (sOpenIndexWriters) {
            sReservedWriterSlots++;
            mIndexWriterMutex.lock();
            if (sOpenIndexWriters.remove(this) != null) {
                sizeAfter = sOpenIndexWriters.size();
                ZimbraPerf.COUNTER_IDX_WRT.increment(sizeAfter);                
            }
        }
        
        // only need to do a log output if we actually removed one
        if (sLog.isDebugEnabled() && sizeAfter > -1)
            sLog.debug("closeIndexWriter: map size after close = " + sizeAfter);
        
        try {
            closeIndexWriterAfterRemove();
        } finally {
            assert(mIndexWriterMutex.isHeldByCurrentThread());
            mIndexWriterMutex.unlock();
            synchronized(sOpenIndexWriters) {
                sReservedWriterSlots--;
                assert(mIndexWriter == null);
            }
        }
    }
    
    private void closeIndexWriterAfterRemove() {
        assert(mIndexWriterMutex.isHeldByCurrentThread());

        if (mIndexWriter == null) {
            return;
        }

        if (sLog.isDebugEnabled())
            sLog.debug("Closing IndexWriter " + mIndexWriter + " for " + this);

        IndexWriter writer = mIndexWriter;
        mIndexWriter = null;

        boolean success = true;
        try {
            // Flush all changes to file system before committing redos.
            writer.close();
        } catch (IOException e) {
            success = false;
            sLog.error("Caught Exception " + e + " in Lucene23Index.closeIndexWriter", e);
            // TODO: Is it okay to eat up the exception?
        } finally {
            // Write commit entries to redo log for all IndexItem entries
            // whose changes were written to disk by mIndexWriter.close()
            // above.
            for (Iterator<IndexItem> iter = mUncommittedRedoOps.iterator(); iter.hasNext();) {
                IndexItem op = iter.next();
                if (success) {
                    if (op.commitAllowed())
                        op.commit();
                    else {
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("IndexItem (" + op +
                            ") not allowed to commit yet; attaching to parent operation");
                        }
                        op.attachToParent();
                    }
                } else
                    op.abort();
                iter.remove();
            }
            assert(mUncommittedRedoOps.size() == 0);
        }
    }
    
    /**
    Levenshtein distance also known as edit distance is a measure of similiarity
    between two strings where the distance is measured as the number of character 
    deletions, insertions or substitutions required to transform one string to 
    the other string. 
    <p>This method takes in four parameters; two strings and their respective 
    lengths to compute the Levenshtein distance between the two strings.
    The result is returned as an integer.
     */ 
    private final int editDistance(String s, String t, int n, int m) {
        if (e.length <= n || e[0].length <= m) {
            e = new int[Math.max(e.length, n+1)][Math.max(e[0].length, m+1)];
        }
        int d[][] = e; // matrix
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s

        if (n == 0) return m;
        if (m == 0) return n;

        // init matrix d
        for (i = 0; i <= n; i++) d[i][0] = i;
        for (j = 0; j <= m; j++) d[0][j] = j;

        // start computing edit distance
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);
            for (j = 1; j <= m; j++) {
                if (s_i != t.charAt(j-1))
                    d[i][j] = min(d[i-1][j], d[i][j-1], d[i-1][j-1])+1;
                else d[i][j] = min(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]);
            }
        }

        // we got the result!
        return d[n][m];
    }
    
    /**
     * @return A refcounted IndexReader for this index.  Caller is responsible for 
     *            calling IndexReader.release() on the index before allowing it to go
     *            out of scope (otherwise a RuntimeException will occur)
     * 
     * @throws IOException
     */
    private RefCountedIndexReader getCountedIndexReader() throws IOException
    {
        BooleanQuery.setMaxClauseCount(10000); 

        synchronized(getLock()) {
            if (isIndexWriterOpen()) 
                closeIndexWriter();
            
            RefCountedIndexReader toRet = sIndexReadersCache.getIndexReader(this);
            if (toRet != null)
                return toRet;
            
            assert(!isIndexWriterOpen());
            
            IndexReader reader = null;
            try {
                reader = IndexReader.open(mIdxDirectory);
            } catch(IOException e) {
                // Handle the special case of trying to open a not-yet-created
                // index, by opening for write and immediately closing.  Index
                // directory should get initialized as a result.
                File indexDir = mIdxDirectory.getFile();
                if (indexDirIsEmpty(indexDir)) {
                    openIndexWriter();
                    mIndexWriterMutex.unlock();
                    closeIndexWriter();
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
     *             FALSE if the index directory exists and has files in it  
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
    
    
    private boolean isIndexWriterOpen() {
        mIndexWriterMutex.lock();
        try {
            return mIndexWriter != null;
        } finally {
            mIndexWriterMutex.unlock();
        }
    }
    
    private void openIndexWriter() throws IOException
    {
        ZimbraPerf.COUNTER_IDX_WRT_OPENED.increment();
        
        assert(Thread.holdsLock(getLock()));
        assert(!mIndexWriterMutex.isHeldByCurrentThread());
        
        // uncache the IndexReader if it is cached
        sIndexReadersCache.removeIndexReader(this);

        //
        // First, get a 'slot' in the cache, closing some other Writer
        // if necessary to do it...
        boolean haveReservedSlot = false;
        boolean mustSleep = false;
        do {
            // Entry cases:
            //       - We might get a slot (or already be in cache) and be done
            //
            //       - We might reserve a slot and go down to close a Writer
            //
            //       - We couldn't find anything to close, so we just did a sleep
            //         and hope to retry again
            //
            //       - We just closed a writer, and we reserved a spot
            //         so we know there will be room this time.
            Lucene23Index toClose;
            synchronized(sOpenIndexWriters) {
                toClose = null;
                mustSleep = false;
                
                if (haveReservedSlot) {
                    sReservedWriterSlots--;
                    haveReservedSlot = false;
                    assert(sOpenIndexWriters.size() + sReservedWriterSlots < sLRUSize);
                }

                if (sOpenIndexWriters.containsKey(this)) {
                    mIndexWriterMutex.lock(); // maintain order in the LinkedHashMap
                    sOpenIndexWriters.remove(this);
                    sOpenIndexWriters.put(this, this);
                } else if (sOpenIndexWriters.size() + sReservedWriterSlots < sLRUSize) {
                    mIndexWriterMutex.lock();
                    sOpenIndexWriters.put(this, this);
                } else {
                    if (!sOpenIndexWriters.isEmpty()) {
                        // find the oldest (first when iterating) entry to remove
                        toClose = sOpenIndexWriters.keySet().iterator().next();
                        sReservedWriterSlots++;
                        haveReservedSlot = true;
                        sOpenIndexWriters.remove(toClose);
                        toClose.mIndexWriterMutex.lock();
                    } else {
                        sLog.info("MI"+this.toString()+"LRU empty and all slots reserved...retrying");
                        mustSleep = true;
                    }
                }
                ZimbraPerf.COUNTER_IDX_WRT.increment(sOpenIndexWriters.size());
            } // synchronized(mOpenIndexWriters)
            
            if (toClose != null) {
                assert(toClose.mIndexWriterMutex.isHeldByCurrentThread());
                assert(!mIndexWriterMutex.isHeldByCurrentThread());
                try {
                    toClose.closeIndexWriterAfterRemove();
                } finally {
                    toClose.mIndexWriterMutex.unlock();
                }
            } else if (mustSleep) {
                assert(!mIndexWriterMutex.isHeldByCurrentThread());
                try { Thread.sleep(100); } catch (Exception e) {};
            }
        } while (haveReservedSlot || mustSleep);
        
        assert(mIndexWriterMutex.isHeldByCurrentThread());
        
        boolean useBatchIndexing;
        try {
            useBatchIndexing = mMbidx.useBatchedIndexing();
        } catch (ServiceException e) {
            throw new IOException("Caught IOException checking BatchedIndexing flag "+e);
        }
        final LuceneConfigSettings.Config config;
        if (useBatchIndexing) {
            config = LuceneConfigSettings.batched;
        } else {
            config = LuceneConfigSettings.nonBatched;
        }
        
        //
        // at this point, we've put ourselves into the writer cache
        // and we have the Write Mutex....so open the file if
        // necessary and return
        //
        try {
            if (mIndexWriter == null) {
                try {
//                  sLog.debug("MI"+this.toString()+" Opening IndexWriter(1) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());
                    mIndexWriter = new IndexWriter(mIdxDirectory, config.autocommit, mMbidx.getAnalyzer(), false, (sUseDeletionPolicy ? this : null));
                    if (ZimbraLog.index_lucene.isDebugEnabled())
                        mIndexWriter.setInfoStream(new PrintStream(new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
//                  sLog.debug("MI"+this.toString()+" Opened IndexWriter(1) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());

                } catch (IOException e1) {
//                    mLog.debug("****Creating new index in " + mIdxPath + " for mailbox " + mMailboxId);
                    File indexDir  = mIdxDirectory.getFile();
                    if (indexDirIsEmpty(indexDir)) {
//                      sLog.debug("MI"+this.toString()+" Opening IndexWriter(2) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());
                        mIndexWriter = new IndexWriter(mIdxDirectory, config.autocommit, mMbidx.getAnalyzer(), true, (sUseDeletionPolicy ? this : null));
                        if (ZimbraLog.index_lucene.isDebugEnabled())
                            mIndexWriter.setInfoStream(new PrintStream(new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
                        
//                      sLog.debug("MI"+this.toString()+" Opened IndexWriter(2) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());
                        if (mIndexWriter == null) 
                            throw new IOException("Failed to open IndexWriter in directory "+indexDir.getAbsolutePath());
                    } else {
                        mIndexWriter = null;
                        IOException ioe = new IOException("Could not create index " + mIdxDirectory.toString() + " (directory already exists)");
                        ioe.initCause(e1);
                        throw ioe;
                    }
                }

                if (config.useSerialMergeScheduler)
                    mIndexWriter.setMergeScheduler(new SerialMergeScheduler());
                
                mIndexWriter.setUseCompoundFile(config.useCompoundFile);
                mIndexWriter.setMaxBufferedDocs(config.maxBufferedDocs);
                mIndexWriter.setRAMBufferSizeMB(((double)config.ramBufferSizeKB)/1024.0);
                mIndexWriter.setMergeFactor(config.mergeFactor);
                
                if (config.useDocScheduler) {
                    LogDocMergePolicy policy = new LogDocMergePolicy();
                    mIndexWriter.setMergePolicy(policy);
                    policy.setMergeFactor(config.mergeFactor);
                    policy.setMinMergeDocs((int)config.minMerge);
                    if (config.maxMerge != Integer.MAX_VALUE) 
                        policy.setMaxMergeDocs((int)config.maxMerge);
                } else {
                    LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
                    mIndexWriter.setMergePolicy(policy);
                    policy.setMergeFactor(config.mergeFactor);
                    policy.setMinMergeMB(((double)config.minMerge)/1024.0);
                    if (config.maxMerge != Integer.MAX_VALUE)
                        policy.setMaxMergeMB(((double)config.maxMerge)/1024.0);
                }

            } else {
                ZimbraPerf.COUNTER_IDX_WRT_OPENED_CACHE_HIT.increment();
            }
            // tim: this might seem bad, since an index in steady-state-of-writes will never get flushed, 
            // however we also track the number of uncomitted-operations on the index, and will force a 
            // flush if the index has had a lot written to it without a flush.
            updateLastWriteTime();
        } finally {
            if (mIndexWriter == null) {
                mIndexWriterMutex.unlock();
                assert(!mIndexWriterMutex.isHeldByCurrentThread());
            }
        }
    }
    

    private void updateLastWriteTime() { mLastWriteTime = System.currentTimeMillis(); }

    /**
     * How many open indexWriters do we allow?  This value must be >= the # threads in the system, and really 
     * should be a good bit higher than that to lessen thrash.  Ideally this value will never get hit in a
     * "real" system, instead the indexes will be flushed via timeout or # ops -- but this value is here so
     * that the # open file descriptors is controlled.
     */
    private static int sLRUSize;
    
    /**
     * After we add a document to it, how long do we hold an index open for writing before closing it 
     * (and therefore flushing the writes to disk)?
     * 
     * Note that there are other things that might cause us to flush the index to disk -- e.g. if the user
     * does a search on the index, or if the system decides there are too many open IndexWriters (see 
     * sLRUSize) 
     */
    private static long sIdleWriterFlushTimeMS;

    private static IndexReadersCache sIndexReadersCache;
    
    private static Log sLog = ZimbraLog.index;
    
    /**
     * If documents are being constantly added to an index, then it will stay at the front of the LRU cache
     * and will never flush itself to disk: this setting specifies the maximum number of writes we will allow
     * to the index before we force a flush.  Higher values will improve batch-add performance, at the cost
     * of longer-lived transactions in the redolog.
     */
    private static int sMaxUncommittedOps;
    
    private static LinkedHashMap<Lucene23Index, Lucene23Index> sOpenIndexWriters =
        new LinkedHashMap<Lucene23Index, Lucene23Index>(200, 0.75f, true);
    
    private static int sReservedWriterSlots = 0;
    
    private static IndexWritersSweeper sSweeper = null;
    
    /**
     * How often do we walk the list of open IndexWriters looking for idle writers
     * to close.  On very busy systems, the default time might be too long.
     */
    private static long sSweeperFrequencyMs = 30 * Constants.MILLIS_PER_SECOND;
    
    /**
     * This static array saves us from the time required to create a new array
     * everytime editDistance is called.
     */
    private int e[][] = new int[1][1];
    private Z23FSDirectory mIdxDirectory = null;
    
    private IndexWriter mIndexWriter;
    private ReentrantLock mIndexWriterMutex = new ReentrantLock();
    
    private volatile long mLastWriteTime = 0;
    
    private Sort mLatestSort = null;
    private SortBy mLatestSortBy = null;
    private MailboxIndex mMbidx;
    private ArrayList<IndexItem>mUncommittedRedoOps = new ArrayList<IndexItem>();
    static abstract class DocEnumInterface {
        void maxDocNo(int num) {};
        abstract boolean onDocument(Document doc, boolean isDeleted);
    }
    static class DomainEnumCallback implements TermEnumInterface {
        DomainEnumCallback(Collection<String> collection) {
            mCollection = collection;
        }

        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1 && text.charAt(0) == '@') {
                mCollection.add(text.substring(1));
            }           
        }
        private Collection<String> mCollection;
    }
    static class TermEnumCallback implements TermEnumInterface {
        TermEnumCallback(Collection<String> collection) {
            mCollection = collection;
        }

        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1) {
                mCollection.add(text);
            }           
        }
        private Collection<String> mCollection;
    }
    interface TermEnumInterface {
        abstract void onTerm(Term term, int docFreq); 
    }
    
    private static final class IndexWritersSweeper extends Thread {
        
        public IndexWritersSweeper() {
            super("IndexWritersSweeperThread");
        }
        
        /**
         * Main loop for the Sweeper thread.  This thread does a sweep automatically
         * every (mSweepIntervalMS) ms, or it will run a sweep when woken up
         * bia the wakeupSweeperThread() API
         */
        @Override
        public void run() {
            sLog.info(getName() + " thread starting");

            boolean shutdown = false;
            long startTime = System.currentTimeMillis();

            while (!shutdown) {
                // Sleep until next scheduled wake-up time, or until notified.
                synchronized (this) {
                    if (!mShutdown) {  // Don't go into wait() if shutting down.  (bug 1962)
                        long now = System.currentTimeMillis();
                        long until = startTime + sSweeperFrequencyMs;
                        if (until > now) {
                            try {
                                wait(until - now);
                            } catch (InterruptedException e) {}
                        }
                    }
                    shutdown = mShutdown;
                }

                startTime = System.currentTimeMillis();
                

                // Flush out index writers that have been idle too long.
                Lucene23Index toRemove = null;
                int removed = 0;
                int sizeAfter = 0;
                int sizeBefore = -1;
                do {
                    try {
                        synchronized (sOpenIndexWriters) {
                            if (sizeBefore == -1)
                                sizeBefore = sOpenIndexWriters.size();
                            toRemove = null;
                            long cutoffTime = startTime - sIdleWriterFlushTimeMS;
                            for (Iterator it = sOpenIndexWriters.entrySet().iterator(); toRemove==null && it.hasNext(); ) {
                                Map.Entry entry = (Map.Entry) it.next();
                                Lucene23Index mi = (Lucene23Index) entry.getKey();
                                if (mi.getLastWriteTime() < cutoffTime) {
                                    removed++;
                                    toRemove = mi;
                                    it.remove();
                                    toRemove.mIndexWriterMutex.lock();
                                    sReservedWriterSlots++;
                                }
                            }
                            sizeAfter = sOpenIndexWriters.size();
                            ZimbraPerf.COUNTER_IDX_WRT.increment(sizeAfter);
                        }
                        if (toRemove != null) {
                            try {
                                toRemove.closeIndexWriterAfterRemove();
                            } finally {
                                toRemove.mIndexWriterMutex.unlock();
                                assert(!toRemove.mIndexWriterMutex.isHeldByCurrentThread());
                                synchronized(sOpenIndexWriters) {
                                    sReservedWriterSlots--;
                                    assert(toRemove.mIndexWriter == null);
                                }
                            }
                        }
                    } finally {
                        if (toRemove != null && toRemove.mIndexWriterMutex.isHeldByCurrentThread()) {
                            sLog.error("Error: sweeper still holding mutex for %s at end of cycle!", toRemove.mIndexWriter);
                            assert(false);
                            toRemove.mIndexWriterMutex.unlock();
                        }
                    }
                    synchronized(this) {
                        if (mShutdown)
                            shutdown = true;
                    }
                } while (!shutdown && toRemove != null);
                
                long elapsed = System.currentTimeMillis() - startTime;
                
                if (removed > 0 || sizeAfter > 0)
                    sLog.info("open index writers sweep: before=" + sizeBefore +
                                ", closed=" + removed +
                                ", after=" + sizeAfter + " (" + elapsed + "ms)");
            }

            sLog.info(getName() + " thread exiting");
        }

        /**
         * Shutdown the sweeper thread
         */
        synchronized void signalShutdown() {
            mShutdown = true;
            notify();
        }
        
        private boolean mShutdown = false;
    }

    /**
     * See {@link IndexDeletionPolicy.onCommit(List)}
     */
    public void onCommit(List c) throws IOException {
        List<IndexCommitPoint> commits = (List<IndexCommitPoint>)c;

        if (commits.size() == 1)
            return;
        
        synchronized(mOpenReaders) {
            // the 0th commit point is the oldest, the size()-1th entry is the most recent
            // the size-1th entry must never be deleted!
            mCurrentCommitPoint = commits.get(commits.size() -1).getSegmentsFileName();
            
//            String secondOldestCommitPoint = commits.get(commits.size()-2).getSegmentsFileName();
//            
//            IndexCommitPoint oldestCommitPoint = commits.get(0);
            
            Set<String> toSave = new HashSet<String>(); 
            
            for (RefCountedIndexReader or : mOpenReaders) {
                String orPoint = or.getCommitPoint();
                
                if (orPoint == null) {
                    // the assumption here is, if the reader has a
                    // NULL then when it was opened we had NOT ever opened the writer.
                    // therefore the reader is pointing to the most recent point.
                    orPoint = mCurrentCommitPoint;
                }
                if (orPoint != null)
                    toSave.add(orPoint);
            }
            
            int commitsSize = commits.size();
            
            // <size()-1 so we never touch the last one, we NEVER want to delete it 
            for (int i = 0; i < commitsSize-1; i++) {
                IndexCommitPoint cur = commits.get(i);
                if (!toSave.contains(cur.getSegmentsFileName())) {
                    cur.delete();
                    ZimbraLog.index.debug(this.toString()+ " Deleting commit point: "+cur.getSegmentsFileName()+" because it is not referenced by open IndexReader");
                } else
                    ZimbraLog.index.info(this.toString()+ " Saving commit point: "+cur.getSegmentsFileName()+" because it is referenced by open IndexReader");
            }
        }
    }

    /**
     * See {@link IndexDeletionPolicy.onInit(List)}
     */
    public void onInit(List c) throws IOException {
        onCommit(c);
    }
    
    public String getCurrentCommitPoint() { return mCurrentCommitPoint; }
    
    public void onClose(RefCountedIndexReader ref) {
        synchronized(mOpenReaders) {
            mOpenReaders.remove(ref);
        }
    }
    
    private String mCurrentCommitPoint = null;
    List<RefCountedIndexReader> mOpenReaders = new ArrayList<RefCountedIndexReader>();
    
    public IndexReader reopenReader(IndexReader reader) throws IOException {
        return reader.reopen();
    }
}
