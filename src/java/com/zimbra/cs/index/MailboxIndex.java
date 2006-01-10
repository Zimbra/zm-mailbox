/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jul 26, 2004
 */
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.CRC32;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.FSDirectory;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbMailItem.SearchResult;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.Volume;

/**
 * @author tim
 *
 * Encapsulates the Index for one particular mailbox
 */
public final class MailboxIndex 
{
    private static class TermEnumCallback implements TermEnumInterface {
        private Collection mCollection;
        
        TermEnumCallback(Collection collection) {
            mCollection = collection;
        }
        public void onTerm(Term term, int docFreq) 
        {
            String text = term.text();
            if (text.length() > 1) {
                mCollection.add(text);
            }			
        }
    }
    
    
    private static class DomainEnumCallback implements TermEnumInterface {
        private Collection mCollection;
        
        DomainEnumCallback(Collection collection) {
            mCollection = collection;
        }
        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1 && text.charAt(0) == '@') {
                mCollection.add(text.substring(1));
            }			
        }
    }
    
    /**
     * @param fieldName - a lucene field (e.g. LuceneFields.L_H_CC)
     * @param collection - Strings which correspond to all of the domain terms stored in a given field.
     * @throws IOException
     */
    public void getDomainsForField(String fieldName, Collection collection) throws IOException
    {
        enumerateTermsForField(new Term(fieldName,""),new DomainEnumCallback(collection));
    }
    
    /**
     * @param collection - Strings which correspond to all of the attachment types in the index
     * @throws IOException
     */
    public void getAttachments(Collection collection) throws IOException
    {
        enumerateTermsForField(new Term(LuceneFields.L_ATTACHMENTS,""), new TermEnumCallback(collection));
    }
    
    public void getObjects(Collection collection) throws IOException
    {
        enumerateTermsForField(new Term(LuceneFields.L_OBJECTS,""), new TermEnumCallback(collection));
    }
    
    public void enumerateDocumentsForTerm(Collection collection, String field) throws IOException {
        enumerateTermsForField(new Term(field,""), new TermEnumCallback(collection));
    }
    
    /**
    Finds and returns the smallest of three integers 
    */
   private static final int min(int a, int b, int c) {
       int t = (a < b) ? a : b;
       return (t < c) ? t : c;
   }
   
   /**
    * This static array saves us from the time required to create a new array
    * everytime editDistance is called.
    */
   private int e[][] = new int[1][1];
   
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
    
    private List<String> suggestSpelling(String field, String token) throws ServiceException {
        LinkedList<String> toRet = new LinkedList();
        
        token = token.toLowerCase();
        
        try {
            CountedIndexReader reader = this.getCountedIndexReader();
            try {
                IndexReader iReader = reader.getReader();
                
                Term term = new Term(field, token);
                int freq = iReader.docFreq(term);
                int numDocs = iReader.numDocs();
                
                if (freq == 0 && numDocs > 0) {
                    
//                    float frequency = ((float)freq)/((float)numDocs);
                    
//                    System.out.println("Term: "+token+" appears in "+frequency*100+"% of documents");
                    
                    int suggestionDistance = Integer.MAX_VALUE;
                    
                    FuzzyTermEnum fuzzyEnum = new FuzzyTermEnum(iReader, term, 0.5f, 1);
                    if (fuzzyEnum != null) {
                        do {
                            Term cur = fuzzyEnum.term();
                            if (cur != null) {
                                String curText = cur.text();
                                System.out.println("\tSUGGEST: "+curText+" ["+fuzzyEnum.docFreq()+" docs]");
                                int curDiff = editDistance(curText, token, curText.length(), token.length());
                                if (curDiff < suggestionDistance) {
                                    suggestionDistance = curDiff;
                                    toRet = new LinkedList();
                                    toRet.add(curText);
                                }
                            }
                        } while(fuzzyEnum.next());
                    }
                }
            } finally {
                reader.release();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException opening index", e);
        }
        
        return toRet;
    }
    
    public List<String> expandWildcardToken(String field, String token, int maxToReturn) throws ServiceException 
    {
        // right now, only support wildcards like foo*
        assert(token.indexOf('*') >= 0);

        // strip the '*' from the end
        int starIdx = token.indexOf('*');
        if (starIdx >= 0)
            token = token.substring(0, starIdx);
        
        // all lucene text should be in lowercase...
        token = token.toLowerCase();
        
        try {
            CountedIndexReader reader = this.getCountedIndexReader();
            try {
                List<String> toRet = new LinkedList();
                Term firstTerm = new Term(field, token);
                
                IndexReader iReader = reader.getReader();
                
                TermEnum terms = iReader.terms(firstTerm);
                
                do {
                    Term cur = terms.term();
                    if (cur != null) {
                        if (!cur.field().equals(firstTerm.field())) {
                            break;
                        }
                        
                        String curText = cur.text();
                        
                        if (curText.startsWith(token)) {
                            // we don't care about deletions, they will be filtered later
                            toRet.add(cur.text());
                            if (toRet.size()>maxToReturn) {
                                throw MailServiceException.TOO_MANY_QUERY_TERMS_EXPANDED("Wildcard text: \""+token
                                        +"\" expands to too many terms (maximum allowed is "+maxToReturn+")", token, maxToReturn);
                            }
                        } else {
                            if (curText.compareTo(token) > 0)
                                break;
                        }
                    }
                } while (terms.next());
                
                return toRet;
            } finally {
                reader.release();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException opening index", e);
        }
    }
    
    /**
     * Force all outstanding index writes to go through.  
     * This API should be called when the system detects that it has free time.
     */
    public synchronized void flush() {
        closeIndexWriter();
    }
    
    /**
     * @param itemIds array of itemIds to be deleted
     * 
     * @return an array of itemIds which HAVE BEEN PROCESSED.  If returned.length == 
     * itemIds.length then you can assume the operation was completely successful
     * 
     * @throws IOException on index open failure, nothing processed.
     */
    public synchronized int[] deleteDocuments(int itemIds[]) throws IOException {
        CountedIndexReader reader = getCountedIndexReader(); 
        try {
            for (int i = 0; i < itemIds.length; i++) {
                try {
                    String itemIdStr = Integer.toString(itemIds[i]);
                    Term toDelete = new Term(LuceneFields.L_MAILBOX_BLOB_ID, itemIdStr);
                    int numDeleted = reader.getReader().delete(toDelete);
                    // NOTE!  The numDeleted may be < you expect here, the document may
                    // already be deleted and just not be optimized out yet -- some lucene
                    // APIs (e.g. docFreq) will still return the old count until the indexes 
                    // are optimized...
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Deleted "+numDeleted+" index documents for itemId "+itemIdStr);
                    }
                } catch (IOException ioe) {
                    mLog.debug("deleteDocuments exception on index "+i+" out of "+itemIds.length+" (id="+itemIds[i]+")");
                    int[] toRet = new int[i];
                    System.arraycopy(itemIds,0,toRet,0,i);
                    return toRet;
                }
            }
        } finally {
            reader.release();
        }
        return itemIds; // success
    }

    /**
     * @param redoOp This API takes ownership of the redoOp and will complete it when the operation is finished.
     * @param doc
     * @param mailboxBlobIdStr
     * @param receivedDate TODO
     * @throws IOException
     */
    public synchronized void addDocument(IndexItem redoOp, Document doc, String mailboxBlobIdStr, long receivedDate) throws IOException 
    {
        long start = 0;
        if (mLog.isDebugEnabled())
            start = System.currentTimeMillis();

        openIndexWriter();

        // doc can be shared by multiple threads if multiple mailboxes
        // are referenced in a single email
        synchronized (doc) {
            doc.removeFields(LuceneFields.L_MAILBOX_BLOB_ID);
            doc.add(Field.Keyword(LuceneFields.L_MAILBOX_BLOB_ID, mailboxBlobIdStr));

            // If this doc is shared by mult threads, then the date might just be wrong,
            // so remove and re-add the date here to make sure the right one gets written!
            doc.removeFields(LuceneFields.L_DATE);
            String dateString = DateField.timeToString(receivedDate);
            doc.add(Field.Text(LuceneFields.L_DATE, dateString));
            
            
            if (null == doc.get(LuceneFields.L_ALL)) {
                doc.add(Field.UnStored(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE));
            }
            mIndexWriter.addDocument(doc);

            if (redoOp != null)
                mUncommittedRedoOps.add(redoOp);
        }

        if (mUncommittedRedoOps.size() > sMaxUncommittedOps) {
            if (mLog.isDebugEnabled()) {
                mLog.debug("Flushing " + toString() + " because of too many uncommitted redo ops");
            }
            flush();
        }

        if (mLog.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            long elapsed = end - start;
            mLog.debug("MailboxIndex.addDocument took " + elapsed + " msec");
        }
    }
    
    public synchronized int numDocs() throws IOException 
    {
        CountedIndexReader reader = this.getCountedIndexReader();
        try {
            IndexReader iReader = reader.getReader();
            return iReader.numDocs();
        } finally {
            reader.release();
        }
    }
    
    public synchronized void enumerateTermsForField(Term firstTerm, TermEnumInterface callback) throws IOException
    {
        CountedIndexReader reader = this.getCountedIndexReader();
        try {
            IndexReader iReader = reader.getReader();
            
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
            reader.release();
        }
    }
    
    public synchronized void enumerateDocuments(DocEnumInterface c) throws IOException {
        CountedIndexReader reader = this.getCountedIndexReader();
        try {
            IndexReader iReader = reader.getReader();
            int maxDoc = iReader.maxDoc();
            c.maxDocNo(maxDoc);
            for (int i = 0; i < maxDoc; i++) {
                if (!c.onDocument(iReader.document(i), iReader.isDeleted(i))) {
                    return;
                }
            }
        } finally {
            reader.release();
        }
    }
    
    public synchronized Collection getFieldNames() throws IOException {
        CountedIndexReader reader = this.getCountedIndexReader();
        try {
            IndexReader iReader = reader.getReader();
            return iReader.getFieldNames();
        } finally {
            reader.release();
        }
    }
    
    // you **MUST** call ZimbraQueryResults.doneWithSearchResults() when you are done with them!
    public synchronized ZimbraQueryResults search(ZimbraQuery query, byte[] types, SortBy searchOrder,
            boolean includeTrash, boolean includeSpam, int chunkSize) throws IOException
    {
        try {
            if (mLog.isDebugEnabled()) {
                String str = this.toString() +" search([";
                for (int i = 0; i < types.length; i++) {
                    if (i > 0) {
                        str += ",";
                    }
                    str+=types[i];
                }
                str += "]," + searchOrder + ")";
                mLog.debug(str);
            }
            
            ZimbraQueryResults toret = query.execute(mMailboxId, this, types, searchOrder, includeTrash, includeSpam, chunkSize);
            
            return new HitIdGrouper(toret, searchOrder);
            
        } catch(Exception e) {
            e.printStackTrace();
            return new EmptyQueryResults(types, searchOrder);
        }
    }
    
    synchronized Sort getSort(SortBy searchOrder) {
        switch (searchOrder) {
        case DATE_DESCENDING:
        	return mLuceneSortDateDesc;
        case DATE_ASCENDING:
        	return mLuceneSortDateAsc;
        case SUBJ_DESCENDING:
        	return mLuceneSortSubjectDesc;
        case SUBJ_ASCENDING:
            return mLuceneSortSubjectAsc;
        case NAME_DESCENDING:
            return mLuceneSortNameDesc;
        case NAME_ASCENDING:
        	return mLuceneSortNameAsc;
        case SCORE_DESCENDING:
        	return null;
        default:
        	return mLuceneSortDateDesc;
        }
    }
    
    
    public String toString() {
        StringBuffer ret = new StringBuffer("MailboxIndex(");
        ret.append(mMailboxId);
        ret.append(")");
        return ret.toString();
    }
    
    public MailboxIndex(Mailbox mailbox, String root) throws ServiceException {
        int mailboxId = mailbox.getId();
        if (mLog.isDebugEnabled())
            mLog.debug("Opening Index for mailbox " + mailboxId);

        mIndexWriter = null;
        mMailboxId = mailboxId;

        Volume indexVol = Volume.getById(mailbox.getIndexVolume());
        String idxParentDir = indexVol.getMailboxDir(mailboxId, Volume.TYPE_INDEX);
        
        // this must be different from the idxParentDir (see the IMPORTANT comment below)
        mIdxPath = idxParentDir + File.separatorChar + '0';
        
        {
        	File parentDirFile = new File(idxParentDir);
        	
        	// IMPORTANT!  Don't make the actual index directory (mIdxPath) yet!  
        	//
        	// The runtime open-index code checks the existance of the actual index directory:  
        	// if it does exist but we cannot open the index, we do *NOT* create it under the 
        	// assumption that the index was somehow corrupted and shouldn't be messed-with....on the 
        	// other hand if the index dir does NOT exist, then we assume it has never existed (or 
        	// was deleted intentionally) and therefore we should just create an index.
        	if (!parentDirFile.exists())
        		parentDirFile.mkdirs();
        	
        	if (!parentDirFile.canRead()) {
        		throw ServiceException.FAILURE("Cannot READ index directory (mailbox="+mailbox.getId()+ " idxPath="+mIdxPath+")", null);
        	}
        	if (!parentDirFile.canWrite()) {
        		throw ServiceException.FAILURE("Cannot WRITE index directory (mailbox="+mailbox.getId()+ " idxPath="+mIdxPath+")", null);
        	}

        	// the Lucene code does not atomically swap the "segments" and "segments.new"
        	// files...so it is possible that a previous run of the server crashed exactly in such
        	// a way that we have a "segments.new" file but not a "segments" file.  We we will check here 
        	// for the special situation that we have a segments.new
        	// file but not a segments file...
        	File segments = new File(mIdxPath, "segments");
        	if (!segments.exists()) {
        		File segments_new = new File(mIdxPath, "segments.new");
        		if (segments_new.exists()) 
        			segments_new.renameTo(segments);
        	}
//        } catch(IOException e) {
//        	throw ServiceException.FAILURE("Error validating index path (mailbox="+mailbox.getId()+ " root="+root+")", e);
        }	
        
        mLuceneSortDateDesc = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, true));
        mLuceneSortDateAsc = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, false));
        mLuceneSortSubjectDesc = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, true));
        mLuceneSortSubjectAsc = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, false));
        mLuceneSortNameDesc = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, true));
        mLuceneSortNameAsc = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, false));
    }
    
    synchronized boolean checkMailItemExists(int mailItemId) {
        return false;
    }
    
    
    private String mIdxPath;
    private Sort mLuceneSortDateDesc = null; /* sort by date descending */
    private Sort mLuceneSortDateAsc = null; /* sort by date ascending */
    private Sort mLuceneSortSubjectAsc = null; /* sort by subject ascending */
    private Sort mLuceneSortSubjectDesc = null; /* sort by subject descending */
    private Sort mLuceneSortNameAsc = null; /* sort by subject ascending */
    private Sort mLuceneSortNameDesc = null; /* sort by subject descending */
    
    
    private int mMailboxId;
    private static Log mLog = LogFactory.getLog(MailboxIndex.class);
    private ArrayList /*IndexItem*/ mUncommittedRedoOps = new ArrayList();
    
    
    
    /******************************************************************************
     *
     *  Index Writer Management
     *  
     ********************************************************************************/
    private IndexWriter mIndexWriter;

    // access-order LinkedHashMap
    // Key is MailboxIndex and value is always null.  LinkedHashMap class is
    // used for its access-order feature.
    private static LinkedHashMap /*MailboxIndex, null*/ sOpenIndexWriters =
        new LinkedHashMap(200, 0.75f, true);

    // List of MailboxIndex objects that are waiting for room to free up in
    // sOpenIndexWriters map before opening index writer.  See openIndexWriter()
    // method.
    private static LinkedList /*MailboxIndex*/ sOpenWaitList = new LinkedList();

    private final static class IndexWritersSweeperThread extends Thread {
        private boolean mShutdown = false;
        private long mSweepIntervalMS;
        private long mIdleMS;
        private int mMaxSize;

        public IndexWritersSweeperThread(long intervalMS, long idleMS, int maxSize) {
            super("IndexWritersSweeper");
            mSweepIntervalMS = intervalMS;
            mIdleMS = idleMS;
            mMaxSize = maxSize;
        }

        public synchronized void signalShutdown() {
        	mShutdown = true;
            wakeup();
        }

        public synchronized void wakeup() {
        	notify();
        }

        public void run() {
            mLog.info(getName() + " thread starting");

            boolean full = false;
            boolean shutdown = false;
            long startTime = System.currentTimeMillis();
            ArrayList /*MailboxIndex*/ toRemove = new ArrayList(100);

            while (!shutdown) {
                // Sleep until next scheduled wake-up time, or until notified.
                synchronized (this) {
                    if (!mShutdown && !full) {  // Don't go into wait() if shutting down.  (bug 1962)
                    	long now = System.currentTimeMillis();
                        long until = startTime + mSweepIntervalMS;
                        if (until > now) {
                        	try {
    							wait(until - now);
    						} catch (InterruptedException e) {}
                        }
                    }
                    shutdown = mShutdown;
                }

                startTime = System.currentTimeMillis();

                int sizeBefore;

                // Flush out index writers that have been idle too long.
                toRemove.clear();
                synchronized (sOpenIndexWriters) {
                    sizeBefore = sOpenIndexWriters.size();
                    long cutoffTime = startTime - mIdleMS;
                    for (Iterator it = sOpenIndexWriters.entrySet().iterator(); it.hasNext(); ) {
                    	Map.Entry entry = (Map.Entry) it.next();
                        MailboxIndex mi = (MailboxIndex) entry.getKey();
                        if (mi.getLastWriteTime() < cutoffTime) {
                        	toRemove.add(mi);
                        }
                    }
                }
                int removed = toRemove.size();
                closeWriters(toRemove);

                // Flush out more index writers if map is too big.
                toRemove.clear();
                synchronized (sOpenIndexWriters) {
                    int excess = sOpenIndexWriters.size() - (mMaxSize - 20);
                    
                    if (excess > sOpenIndexWriters.size()) 
                        excess = sOpenIndexWriters.size();
                    
                    if (excess > 0) {
                        int num = 0;
                    	for (Iterator it = sOpenIndexWriters.entrySet().iterator();
                             it.hasNext() && num < excess;
                             num++) {
                            Map.Entry entry = (Map.Entry) it.next();
                            toRemove.add(entry.getKey());
                        }
                    }
                }
                removed += toRemove.size();
                closeWriters(toRemove);

                // Get final map size at the end of sweep.
                int sizeAfter;
                synchronized (sOpenIndexWriters) {
                	sizeAfter = sOpenIndexWriters.size();
                }
                long elapsed = System.currentTimeMillis() - startTime;

                if (removed > 0 || sizeAfter > 0)
                    mLog.info("open index writers sweep: before=" + sizeBefore +
                              ", closed=" + removed +
                              ", after=" + sizeAfter + " (" + elapsed + "ms)");

                full = sizeAfter >= mMaxSize;

                // Wake up some threads that were waiting for room to insert in map.
                if (sizeAfter < sizeBefore) {
                	int howmany = sizeBefore - sizeAfter;
                    for (int i = 0; i < howmany; i++)
                        notifyFirstIndexWriterWaiter();
                }
            }

            mLog.info(getName() + " thread exiting");
        }

        private void closeWriters(List writers) {
            for (Iterator it = writers.iterator(); it.hasNext(); ) {
                MailboxIndex mi = (MailboxIndex) it.next();
                mLog.debug("Flushing index writer: " + mi);
                mi.flush();
            }
        }
    }

    public static void startup() {
        // In case startup is called twice in a row without shutdown in between
        if (sIndexWritersSweeper != null && sIndexWritersSweeper.isAlive()) {
            shutdown();
        }

        sMaxUncommittedOps = LC.zimbra_index_max_uncommitted_operations.intValue();
        sLRUSize = LC.zimbra_index_lru_size.intValue();
        if (sLRUSize < 10) sLRUSize = 10;
        sIdleWriterFlushTimeMS = 1000 * LC.zimbra_index_idle_flush_time.intValue();
        sIndexWritersSweeper =
            new IndexWritersSweeperThread(sSweepIntervalMS, sIdleWriterFlushTimeMS, sLRUSize);
        sIndexWritersSweeper.start();
    }

    public static void shutdown() {
        sIndexWritersSweeper.signalShutdown();
        try {
            sIndexWritersSweeper.join();
        } catch (InterruptedException e) {}

        flushAllWriters();
    }
    
    public static void flushAllWriters() {
        mLog.info("Flushing all open index writers");
        ArrayList /*MailboxIndex*/ toRemove = new ArrayList();

        synchronized(sOpenIndexWriters) {
            for (Iterator it = sOpenIndexWriters.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                MailboxIndex mi = (MailboxIndex) entry.getKey();
                toRemove.add(mi);
            }
        }
        for (Iterator it = toRemove.iterator(); it.hasNext(); ) {
            MailboxIndex mi = (MailboxIndex) it.next();
            mi.flush();
        }
    }

    /**
     * If documents are being constantly added to an index, then it will stay at the front of the LRU cache
     * and will never flush itself to disk: this setting specifies the maximum number of writes we will allow
     * to the index before we force a flush.  Higher values will improve batch-add performance, at the cost
     * of longer-lived transactions in the redolog.
     */
    private static int sMaxUncommittedOps;
    
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

    // TODO: Make this configurable.
    private static long sSweepIntervalMS = 60 * 1000;
    private static IndexWritersSweeperThread sIndexWritersSweeper;

    private long mLastWriteTime = 0;
    private long getLastWriteTime() { return mLastWriteTime; }
    private void updateLastWriteTime() { mLastWriteTime = System.currentTimeMillis(); };

    /**
     * Close the index writer and write commit/abort entries for all
     * pending IndexItem redo operations.
     */
    private synchronized void closeIndexWriter() 
    {
        // Remove from open index writers map.
        int sizeAfter;
        Object removed;
        synchronized(sOpenIndexWriters) {
            removed = sOpenIndexWriters.remove(this);
            sizeAfter = sOpenIndexWriters.size();
        }
        if (removed != null) {
            // Notify a waiter that was waiting for room to free up in map.
            notifyFirstIndexWriterWaiter();
            if (mLog.isDebugEnabled())
                mLog.debug("closeIndexWriter: map size after close = " + sizeAfter);
        }

        if (mIndexWriter == null)
            return;

        boolean success = true;
        if (mLog.isDebugEnabled())
            mLog.debug("Closing IndexWriter " + mIndexWriter + " for " + this);
        IndexWriter writer = mIndexWriter;
        mIndexWriter = null;
        try {
            // Flush all changes to file system before committing redos.
            // TODO: Are the changes fsynced to disk when close() returns?
            writer.close();
        } catch (IOException e) {
            success = false;
            mLog.error("Caught Exception " + e + " in MailboxIndex.closeIndexWriter", e);
            // TODO: Is it okay to eat up the exception?
        } finally {
            // Write commit entries to redo log for all IndexItem entries
            // whose changes were written to disk by mIndexWriter.close()
            // above.
            for (Iterator iter = mUncommittedRedoOps.iterator(); iter.hasNext();) {
                IndexItem op = (IndexItem)iter.next();
                if (success) {
                	if (op.commitAllowed())
                		op.commit();
                    else {
                        if (mLog.isDebugEnabled()) {
                            mLog.debug("IndexItem (" + op +
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

    private synchronized void openIndexWriter() throws IOException
    {
        if (mIndexWriter != null) {
        	// Already open.
            return;
        }

        // Before opening index writer, make sure there is room for it in
        // sOpenIndexWriters map.
        int sizeAfter = 0;
        while (true) {
        	synchronized (sOpenIndexWriters) {
                int numOpenWriters = sOpenIndexWriters.size();
                ZimbraPerf.COUNTER_IDX_WRT.increment(numOpenWriters);
        		if (numOpenWriters < sLRUSize) {
                    // Put then get.  Entry will be added to map if it's not there
                    // already, and get will force access time to be updated.
                    sOpenIndexWriters.put(this, this);
                    sOpenIndexWriters.get(this);
                    sizeAfter = sOpenIndexWriters.size();
                    // Proceed.
                    break;
                }
            }
            // Map is full.  Add self to waiter list and wait until notified when
            // there's room in the map.
            synchronized (sOpenWaitList) {
                sOpenWaitList.add(this);
            }
            sIndexWritersSweeper.wakeup();
            //synchronized (this) {  // already done in method entry
                try {
                    wait(5000);
                } catch (InterruptedException e) {}
            //}
        }
        if (mLog.isDebugEnabled())
            mLog.debug("openIndexWriter: map size after open = " + sizeAfter); 

        IndexWriter writer = null;
        try {
            writer = new IndexWriter(mIdxPath, ZimbraAnalyzer.getInstance(), false);
            //mLog.info("Opening IndexWriter "+ writer+" for "+this);
            
        } catch (IOException e1) {
            //mLog.info("****Creating new index in " + mIdxPath + " for mailbox " + mMailboxId);
            File idxFile  = new File(mIdxPath);
            if (idxFile.exists()) {
                // Empty directory is okay, but a directory with any files
                // implies index corruption.
                File[] files = idxFile.listFiles();
                int numFiles = 0;
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    String fname = f.getName();
                	if (f.isDirectory() && (fname.equals(".") || fname.equals("..")))
                        continue;
                    numFiles++;
                }
                if (numFiles > 0) {
                    IOException ioe = new IOException("Could not create index " + mIdxPath + " (directory already exists)");
                    ioe.initCause(e1);
                    throw ioe;
                }
            } else {
                idxFile.mkdirs();
            }
            writer = new IndexWriter(idxFile, ZimbraAnalyzer.getInstance(), true);
            if (writer == null) 
            	throw new IOException("Failed to open IndexWriter in directory "+idxFile.getAbsolutePath());
        }

        ///////////////////////////////////////////////////
        //
        // mergeFactor and minMergeDocs are VERY poorly explained.  Here's the deal:
        //
        // The data is in a tree.  It starts out empty.
        //
        // 1) Whenever docs are added, they are merged into the the smallest node (or a new node) until its 
        //    size reaches "mergeFactor"
        //
        // 2) When we have enough "mergeFactor" sized small nodes so that the total size is "minMergeDocs", then
        //    we combine them into one "minMergeDocs" sized big node.
        //
        // 3) Rule (2) repeats recursively: every time we get "mergeFactor" small nodes, we combine them.
        //
        // 4) This means that every segment (beyond the first "level") is therefore always of size:
        //       minMergeDocs * mergeFactor^N, where N is the # times it has been merged
        //
        // 5) Be careful, (2) implies that we will have (minMergeDocs / mergeFactor) small files!
        //
        // NOTE - usually with lucene, the 1st row of the tree is stored in memory, because you keep 
        // the IndexWriter open for a long time: this dramatically changes the way it performs 
        // because you can make mergeFactor a lot bigger without worrying about the overhead of 
        // re-writing the smallest node over and over again. 
        //
        // In our case, mergeFactor is intentionally chosen to be very small: since we add one document
        // then close the index, if mergeFactor were large it would mean we copied every document
        // (mergeFactor/2) times (start w/ 1 doc, adding the second copies the first b/c it re-writes the
        // file...adding the 3rd copies 1+2....etc)
        //
        // ...in an ideal world, we'd have a separate parameter to control the size of the 1st file and the
        // mergeFactor: then we could have the 1st file be 1 entry and still have a high merge factor.  Doing
        // this we could potentially lower the indexing IO by as much as 70% for our expected usage pattern...  
        // 
        /////////////////////////////////////////////////////

        
        // tim: these are set based on an expectation of ~25k msgs/box and assuming that
        // 11 fragment files are OK.  25k msgs with these settings (mf=3, mmd=33) means that
        // each message gets written 9 times to disk...as opposed to 12.5 times with the default
        // lucene settings of 10 and 100....

        
        // FIXME TODO HACK -- tim for testing.  REMOVEME REMOVE THIS IF (but not the code inside of it) BEFORE RELEASE!
        // (this is just a hack so that it ignores our settings when our custom jar is used)
        if (IndexWriter.WRITE_LOCK_TIMEOUT != 1001) {
            writer.mergeFactor = 3; // should be > 1, otherwise segment sizes are effectively limited to 
            // minMergeDocs documents: and this is probably bad (too many files!)
            
            writer.minMergeDocs = 33; // we expect 11 index fragment files
        }
            
        mIndexWriter = writer;
        
        //
        // tim: this might seem bad, since an index in steady-state-of-writes will never get flushed, 
        // however we also track the number of uncomitted-operations on the index, and will force a 
        // flush if the index has had a lot written to it without a flush.
        //
        updateLastWriteTime();
    }

    private static void notifyFirstIndexWriterWaiter() {
        MailboxIndex waiter = null;
        synchronized (sOpenWaitList) {
            if (sOpenWaitList.size() > 0)
            	waiter = (MailboxIndex) sOpenWaitList.removeFirst();
        }
        if (waiter != null) {
        	synchronized (waiter) {
        		waiter.notifyAll();
            }
        }
    }



    /******************************************************************************
     *
     *  Index Reader Management
     *  
     ********************************************************************************/
    private synchronized CountedIndexReader getCountedIndexReader() throws IOException
    {
        IndexReader reader = null;
        try {
            /* uggghhh!  nasty.  FIXME - need to coordinate with index writer better 
             (manually create a RamDirectory and index into that?) */
            flush();
            
            reader = IndexReader.open(mIdxPath);
        } catch(IOException e) {
        	// Handle the special case of trying to open a not-yet-created
            // index, by opening for write and immediately closing.  Index
            // directory should get initialized as a result.
            File indexDir = new File(mIdxPath);
            if (!indexDir.exists()) {
            	openIndexWriter();
                flush();
                try {
                    reader = IndexReader.open(mIdxPath);
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
        return new CountedIndexReader(reader);
    }
    
    synchronized CountedIndexSearcher getCountedIndexSearcher() throws IOException
    {
        CountedIndexSearcher searcher = null;
        CountedIndexReader cReader = getCountedIndexReader();
        searcher = new CountedIndexSearcher(cReader);
        return searcher;
    }
    
    static class CountedIndexReader {
        private IndexReader mReader;
        private int mCount = 1;
        
        public CountedIndexReader(IndexReader reader) {
            mReader= reader;
        }
        public IndexReader getReader() { return mReader; }
        
        public synchronized void addRef() {
            mCount++;
        }
        
        public synchronized void forceClose() {
            closeIt();
        }
        
        public synchronized void release() {
            mCount--;
            assert(mCount >= 0);
            if (0 == mCount) {
                closeIt();
            }
        }
        
        private void closeIt() {
            try {
                mReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mReader= null;
            }
        }
        
        protected void finalize() {
            if (mReader != null) {
                throw new java.lang.RuntimeException("Reader isn't closed in CountedIndexReader's finalizer!");
            }
        }
    }
    
    static class CountedIndexSearcher {
        private Searcher mSearcher;
        private CountedIndexReader mReader;
        private int mCount = 1;
        public CountedIndexSearcher(CountedIndexReader reader) {
            mReader= reader;
            mSearcher = new IndexSearcher(mReader.getReader());
        }
        public synchronized Searcher getSearcher() { return mSearcher; }
        public synchronized void forceClose() {
            mReader.forceClose();
            mReader = null;
        }
        public synchronized void release() {
            mCount--;
            assert(mCount >= 0);
            if (0 == mCount) {
                mReader.release();
                mReader= null;
            }
        }
        public synchronized CountedIndexSearcher addRef() {
            assert(mCount > 0);
            mCount++;
            return this;
        }
    }
    
    
    
    /******************************************************************************
     *
     *  Index Search Results
     *  
     ********************************************************************************/
    // What level of result grouping do we want?  ConversationResult, MessageResult, or DocumentResult?
    public static final int FIRST_SEARCH_RETURN_NUM = 1;
    public static final int SEARCH_RETURN_CONVERSATIONS = 1;
    public static final int SEARCH_RETURN_MESSAGES      = 2;
    public static final int SEARCH_RETURN_DOCUMENTS     = 3;
    public static final int LAST_SEARCH_RETURN_NUM = 3;

    public static final String GROUP_BY_CONVERSATION = "conversation";
    public static final String GROUP_BY_MESSAGE      = "message";
    public static final String GROUP_BY_NONE         = "none";
    
    public static final String SEARCH_FOR_CONVERSATIONS = "conversation";
    public static final String SEARCH_FOR_MESSAGES = "message";
    public static final String SEARCH_FOR_CONTACTS = "contact";
    public static final String SEARCH_FOR_APPOINTMENTS = "appointment";
    public static final String SEARCH_FOR_NOTES = "note";
    
    public static enum SortBy {
    	DATE_ASCENDING  ("dateAsc",   (byte)(DbMailItem.SORT_BY_DATE | DbMailItem.SORT_ASCENDING)), 
    	DATE_DESCENDING ("dateDesc",  (byte)(DbMailItem.SORT_BY_DATE | DbMailItem.SORT_DESCENDING)),
    	SUBJ_ASCENDING  ("subjAsc",   (byte)(DbMailItem.SORT_BY_SUBJECT | DbMailItem.SORT_ASCENDING)),
    	SUBJ_DESCENDING ("subjDesc",  (byte)(DbMailItem.SORT_BY_SUBJECT | DbMailItem.SORT_DESCENDING)),
    	NAME_ASCENDING  ("nameAsc",   (byte)(DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_ASCENDING)),
    	NAME_DESCENDING ("nameDesc",  (byte)(DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_DESCENDING)),
    	SCORE_DESCENDING("scoreDesc", (byte)0);

		static HashMap<String, SortBy> sNameMap = new HashMap();
    	
    	static {
    		for (SortBy s : SortBy.values()) 
    			sNameMap.put(s.mName, s);
    	}
    	
    	byte mSort;
    	String mName;
    	
    	SortBy(String str, byte sort) {
    		mName = str;
    		mSort = sort;
    	}
    	
    	public String getName() { return mName; }
    	
    	public byte getDbMailItemSortByte() {
    		return mSort;
    	}
    	
    	public boolean isDescending() {
        	return (mSort & DbMailItem.SORT_DESCENDING)!=0;
    	}
    	
    	public static SortBy lookup(String str) {
    		return sNameMap.get(str);
    	}
    }

    public static byte[] parseGroupByString(String groupBy) throws ServiceException
    {
        String[] strs = groupBy.split("\\s*,\\s*");

        byte[] types = new byte[strs.length]; 
        for (int i = 0; i < strs.length; i++) {
            if (SEARCH_FOR_CONVERSATIONS.equals(strs[i])) {
                types[i] = MailItem.TYPE_CONVERSATION;
            } else if (SEARCH_FOR_MESSAGES.equals(strs[i])) {
                types[i] = MailItem.TYPE_MESSAGE;
            } else if (GROUP_BY_NONE.equals(strs[i])) {
                types[i] = 0;
            } else if (SEARCH_FOR_CONTACTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_CONTACT;
            } else if (SEARCH_FOR_APPOINTMENTS.equals(strs[i])) {
                types[i] = MailItem.TYPE_APPOINTMENT;
            } else if (SEARCH_FOR_NOTES.equals(strs[i])) {
                types[i] = MailItem.TYPE_NOTE;
            } else 
                throw ServiceException.INVALID_REQUEST("unknown groupBy: "+strs[i], null);
        }
        
        return types;
    }
    
    protected synchronized Spans getSpans(SpanQuery q) throws IOException {
        CountedIndexReader reader = this.getCountedIndexReader();
        try {
            IndexReader iReader = reader.getReader();
            return q.getSpans(iReader);
        } finally {
            reader.release();
        }
    }
    
    public interface TermEnumInterface {
        abstract void onTerm(Term term, int docFreq); 
    }
    public static abstract class DocEnumInterface {
        void maxDocNo(int num) {};
        abstract boolean onDocument(Document doc, boolean isDeleted);
    }
    
    private static final int INDEX_BACKUP_START_MAGIC = 0x305;
    private static final int INDEX_BACKUP_SEPARATOR_MAGIC = 0x35353535;
    private static final int BUFFER_LEN = 65536;
    
    
    /**
     * 
     * Writes a binary backup of the current index to the given output stream. 
     * 
     * @param out OutputStream to write binary encoded index data out
     * @throws IOException
     */
    public synchronized void backupIndex(OutputStream out) throws IOException
    {
        flush(); // make sure all writes are complete.
        
        // TODO - steal Lucene's much more efficient primitive-types-to-binary-file code and use that, instead of the ObjectOutputStream...
        ObjectOutputStream outStr = new ObjectOutputStream(out);
        
        //
        // simple output format:
        //
        //  - Start Magic Number
        //
        //  - Repeat:  
        //     Long length  : length of file or -1 end of files
        //     String name : file name
        //     byte[length]: file data
        //     separator magic #
        //
        outStr.writeInt(INDEX_BACKUP_START_MAGIC);
        
        FSDirectory indexDir = null;
        File idxPathFile = new File(mIdxPath);
        if (idxPathFile.exists() && idxPathFile.isDirectory()) {
            indexDir = FSDirectory.getDirectory(idxPathFile, false);
        }
        if (null != indexDir) {
            String[] files = indexDir.list();
            
            byte buffer[] = new byte[BUFFER_LEN];
            
            for (int i = 0; i < files.length; i++) {
                long fileLen = indexDir.fileLength(files[i]);
                org.apache.lucene.store.InputStream in = indexDir.openFile(files[i]);
                
                if (mLog.isDebugEnabled()) {
                    mLog.debug("\tWriting file: \""+files[i]+"\" len="+fileLen);
                }
                
                //
                // need to write file header here....
                //
                outStr.writeLong(fileLen);
                outStr.writeUTF(files[i]);
                
                //
                // write the file data now
                //
                long pos = 0;
                while (pos < fileLen) {
                    long toRead = Math.min(fileLen-pos, BUFFER_LEN);
                    in.readBytes(buffer, 0, (int)toRead);
                    outStr.write(buffer, 0, (int)toRead);
                    CRC32 check = new CRC32();
                    check.update(buffer, 0, (int)toRead);
                    
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("\t\tWrote "+(int)toRead+" bytes with checksum "+check.getValue());
                    }
                    pos+=toRead;
                }
                
                outStr.writeInt(INDEX_BACKUP_SEPARATOR_MAGIC);
                
                in.close();
            }
        } else {
            mLog.info("No index file for mailbox "+this.mMailboxId);
        }
        outStr.writeLong(-1); // end of files
        outStr.flush();
    }
    
    /**
     * 
     * WARNING: this function deletes the current index.  Only call this if you're sure that's what you want.
     * 
     * @param in InputStream to read data from
     * @throws IOException
     */
    public synchronized void restoreIndex(java.io.InputStream in) throws IOException
    {
        // TODO - steal Lucene's much more efficient primitive-types-to-binary-file code and use that, instead of the ObjectInputStream...
        ObjectInputStream inStr = new ObjectInputStream(in);
        
        if (mLog.isDebugEnabled()) {
            mLog.debug("");
            mLog.debug("");
            mLog.debug("Restoring index for "+mMailboxId+" to path "+mIdxPath);
        }
        
        int magic = inStr.readInt();
        if (magic != INDEX_BACKUP_START_MAGIC) {
            throw new IOException("Invalid magic # at start (read: "+magic+" expected: "+INDEX_BACKUP_START_MAGIC+")");
        }
        
        flush();
        
        FSDirectory indexDir = FSDirectory.getDirectory(mIdxPath, true);
        
        byte buffer[] = new byte[BUFFER_LEN];
        
        for (long length = inStr.readLong(); length >= 0; length = inStr.readLong())
        {
            // length read above
            
            // read filename:
            String name = inStr.readUTF();
            if (mLog.isDebugEnabled()) {
                mLog.debug("\tRestoring file: \""+name+"\" length:"+length);
            }
            org.apache.lucene.store.OutputStream out = indexDir.createFile(name);
            
            // read data:
            while (length > 0) {
                long toRead = Math.min(length, BUFFER_LEN);
                /*int read = */inStr.readFully(buffer, 0, (int)toRead);
                int read = (int)toRead;
                CRC32 check = new CRC32();
                check.update(buffer, 0, read);
                
                if (mLog.isDebugEnabled()) {
                    mLog.debug("\t\tRead "+read+" (out of "+(int)toRead+") bytes with checksum "+check.getValue());
                }
                out.writeBytes(buffer, read);
                if (read <= 0) {
                    throw new IOException("Read returned "+read+" reading file "+name);
                }
                length-=read;
            }

            out.close();
            
            // read MAGIC
            magic = inStr.readInt();
            if (magic != INDEX_BACKUP_SEPARATOR_MAGIC) {
                throw new IOException("Invalid magic # separator (read: "+magic+" expect:"+INDEX_BACKUP_SEPARATOR_MAGIC+")");
            }
            
        }
        
        indexDir.close();
    }

    public void reIndex() throws ServiceException
    {
        Connection conn = null;
        conn = DbPool.getConnection();
        
        DbMailItem.SearchConstraints c = new DbMailItem.SearchConstraints();
        
        c.mailboxId = mMailboxId;
        c.sort = DbMailItem.SORT_BY_DATE;
        
        Collection msgs = DbMailItem.search(conn, c);
        
        Mailbox mbx = Mailbox.getMailboxById(mMailboxId);
        
        try {
            deleteIndex();

            long start = System.currentTimeMillis();
            int successful = 0;
            int tried = 0;
            long getMsgTime = 0;
            long parseMsgTime = 0;
            long indexTime = 0;
            
            for (Iterator iter = msgs.iterator(); iter.hasNext();) {
                tried++;
                MailItem item = null;
                SearchResult sr = null;
                try {
                    long s = System.currentTimeMillis();
                    long split;
                    
                    sr = (SearchResult) iter.next();
                    item = mbx.getItemById(null, sr.id, sr.type);
                    ParsedMessage pm = null;
                    if (item instanceof Message) {
                        Message msg = (Message)item;
                        split = System.currentTimeMillis();
                        getMsgTime += (split - s);
                        s = split;
                        
                        // force the pm's received-date to be the correct one
                        long msgDate = ((Long)sr.sortkey).longValue();
                        Mailbox mbox = Mailbox.getMailboxById(mMailboxId);
                        pm = new ParsedMessage(msg.getMimeMessage(), msgDate, mbox.attachmentsIndexingEnabled());
                        
                        split = System.currentTimeMillis();
                        parseMsgTime += (split - s);
                        s = split;
                    }
                    split = System.currentTimeMillis();
                    getMsgTime += (split - s);
                    s = split;
                    
                    item.reindex(null, pm);
                
                    split = System.currentTimeMillis();
                    indexTime += (split - s);
                    s = split;
                    
                    successful++;
            
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (java.lang.RuntimeException e) {
                    e.printStackTrace();
                }
            } 
            long end = System.currentTimeMillis();
            long avg = 0;
            long mps = 0;
            if (tried > 0) {
                avg = (end - start) / tried;
                mps = avg > 0 ? 1000 / avg : 0;
            }
            mLog.info("Mbox " + mMailboxId + " Re-Indexed "+ successful + " out of "+tried +" docs in " +
                    (end-start) + "ms.  (avg "+avg+"ms/msg = "+mps+" msgs/sec)");
            mLog.info("load-message-time="+getMsgTime+" parse-mime-msg-time="+parseMsgTime+" index-time="+indexTime);
        } catch (IOException e) {
            e.printStackTrace();
            throw ServiceException.FAILURE("IOException while reIndexing mbox "+mMailboxId, e);
        } finally {
            flush();
        }
     
    }
    
    private static class ChkIndexStage1Callback implements TermEnumInterface 
    {
        HashSet msgsInMailbox = new HashSet(); // hash of all messages in my mailbox
        private MailboxIndex idx = null;
        private ArrayList toDelete = new ArrayList(); // to be deleted from index
        DbMailItem.SearchResult compareTo = new DbMailItem.SearchResult();  
        
        ChkIndexStage1Callback(MailboxIndex idx) {
            this.idx = idx;
        }
        
        void doIndexRepair() throws IOException 
        {
            // delete first -- that way if there were any re-indexes along the way we know we're OK
            if (toDelete.size() > 0) {
                mLog.info("There are "+toDelete.size()+" items to delete");
                int ids[] = new int[toDelete.size()];
                for (int i = 0; i < toDelete.size(); i++) {
                    ids[i] = ((Integer)(toDelete.get(i))).intValue();
                }
                idx.deleteDocuments(ids);
            }
            
            // if there any messages left in this list, then they are missing from the index and 
            // we should try to reindex them
            if (msgsInMailbox.size() > 0)
            {
                mLog.info("There are "+msgsInMailbox.size() + " msgs to be re-indexed");
                for (Iterator iter = msgsInMailbox.iterator(); iter.hasNext();) {
                    DbMailItem.SearchResult cur = (DbMailItem.SearchResult)iter.next();
                    
                    try {
                        idx.reIndexItem(cur.id, cur.type);
                    } catch(ServiceException e) {
                        mLog.info("Couldn't index "+compareTo.id+" caught ServiceException", e);
                    }
                }
            }
        }
        
        public void onTerm(Term term, int docFreq) 
        {
            compareTo.id = Integer.parseInt(term.text());
            
            if (!msgsInMailbox.contains(compareTo)) {
                mLog.info("In index but not DB: "+compareTo.id);
                toDelete.add(new Integer(compareTo.id));
            } else {
                // remove from the msgsInMailbox hash.  If there are still entries in this
                // table, then it means that there are items in the mailbox, but not in the index
                msgsInMailbox.remove(compareTo);
            }
        }
    }
    
    private static class ChkIndexStage2Callback 
    {
        public List msgsInMailbox = new LinkedList(); // hash of all messages in my mailbox
        private ListIterator msgsIter;
        
        private String mSortField;
        DbMailItem.SearchResult mCur = null;
        
        
        ChkIndexStage2Callback(MailboxIndex idx, String sortField, boolean reversed) {
            mSortField = sortField;
            this.reversed = reversed;
        }
        
        boolean beginIterating() {
            msgsIter = msgsInMailbox.listIterator();
            mCur = (DbMailItem.SearchResult)msgsIter.next();
            return (mCur!= null);
        }
        
        boolean reversed = false;
        
        long compare(long lhs, long rhs) {
            if (!reversed) {
                return (lhs - rhs);
            } else {
                return (rhs - lhs);
            }
        }
        
        void onDocument(Document doc) 
        {
            int idxId = Integer.parseInt(doc.get(LuceneFields.L_MAILBOX_BLOB_ID));
            
            String sortField = doc.get(mSortField);
            String partName = doc.get(LuceneFields.L_PARTNAME);
            String dateStr = doc.get(LuceneFields.L_DATE);
            long docDate = DateField.stringToTime(dateStr);
            // fix for Bug 311 -- SQL truncates dates when it stores them
            long truncDocDate = (docDate /1000) * 1000;
            
            retry: do {
                long curMsgDate = ((Long)(mCur.sortkey)).longValue();
                
                
                if (mCur.id == idxId) {
                    // next part same doc....good.  keep going..
                    if (curMsgDate != truncDocDate) {
                        mLog.info("WARN  : DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
                                " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
                                " "+mSortField+"="+sortField);
                        
                        mLog.info("\tWARNING: DB-DATE doesn't match TRUNCDATE!");
                    } else {
                        mLog.debug("OK    : DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
                                " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
                                " "+mSortField+"="+sortField);
                    }
                    return;
                } else {
                    if (false) {
//                    if (!msgsIter.hasNext()) {
//                        if (mMissingTerms == null) {
//                            mLog.info("ERROR: end of msgIter while still iterating index");
//                        }
//                        mLog.info("ERROR: DB no results INDEX has mailitem: "+idxId);
//                        return;
                    } else {
                        // 3 possibilities:
                        //    doc < cur
                        //    doc > cur
                        //    doc == cur
//                        if (truncDocDate < curMsgDate) { // case 1
                        if (compare(truncDocDate,curMsgDate) < 0) { // case 1
                            mLog.info("ERROR1: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
                                    " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
                                    " "+mSortField+"="+sortField);
                            
                            // move on to next document
                            return;
//                        } else if (truncDocDate > curMsgDate) { // case 2
                        } else if (compare(truncDocDate,curMsgDate)>0) { // case 2
//                            mLog.info("ERROR2: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
//                                    " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
//                                    " "+mSortField+"="+sortField);
                            
                            if (!msgsIter.hasNext()) {
                                mLog.info("ERROR4: DB no results INDEX has mailitem: "+idxId);
                                return;
                            }
                            mCur = (DbMailItem.SearchResult)msgsIter.next();
                            
                            continue; // try again!
                        } else { // same date!
                            // 1st,look backwards for a match
                            if (msgsIter.hasPrevious()) {
                                do {
                                    mCur = (DbMailItem.SearchResult)msgsIter.previous();
                                    if (mCur.id == idxId) {
                                        continue retry;
                                    }
                                    curMsgDate = ((Long)(mCur.sortkey)).longValue();
                                } while(msgsIter.hasPrevious() && curMsgDate == truncDocDate);
                                
                                // Move the iterator fwd one, so it is on the correct time...
                                mCur = (DbMailItem.SearchResult)msgsIter.next();
                                
                            }
                            
                            // now, look fwd.  Sure, we might check some twice here.  Oh well
                            if (msgsIter.hasNext()) {
                                do {
                                    mCur = (DbMailItem.SearchResult)msgsIter.next();
                                    if (mCur.id == idxId) {
                                        continue retry;
                                    }
                                    curMsgDate = ((Long)(mCur.sortkey)).longValue();
                                } while (msgsIter.hasNext() && curMsgDate == truncDocDate);
                                
                                // Move the iterator back one, so it is on the correct time...
                                mCur = (DbMailItem.SearchResult)msgsIter.previous();
                            }
                            
                            
                            mLog.info("ERROR3: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
                                    " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
                                    " "+mSortField+"="+sortField);
                            return;
                        } // big if...else
                    } // has a next
                } // else if IDs don't mtch
            } while(true);
        } // func
        
    } // class
                    
    synchronized void chkIndex(boolean repair) throws ServiceException 
    {
        flush();
        
        Connection conn = null;
        conn = DbPool.getConnection();
        
        
        ///////////////////////////////
        //
        // Stage 1 -- look for missing or extra messages and reindex/delete as necessary.
        //
        {
            DbMailItem.SearchConstraints c = new DbMailItem.SearchConstraints();
            
            c.mailboxId = mMailboxId;
            c.sort = DbMailItem.SORT_BY_DATE;
            c.types = new byte[] {
                    MailItem.TYPE_CONTACT, 
                    MailItem.TYPE_MESSAGE,
                    MailItem.TYPE_NOTE
            };
            
            ChkIndexStage1Callback callback = new ChkIndexStage1Callback(this);
            
            DbMailItem.search(callback.msgsInMailbox, conn, c);
            mLog.info("Verifying (repair="+(repair?"TRUE":"FALSE")+") Index for Mailbox "+this.mMailboxId+" with "+callback.msgsInMailbox.size()+" items.");
            
            try {
                this.enumerateTermsForField(new Term(LuceneFields.L_MAILBOX_BLOB_ID, ""), callback);
            } catch (IOException e) {
                throw ServiceException.FAILURE("Caught IOException while enumerating fields", e);
            }
            
            mLog.info("Stage 1 Verification complete for Mailbox "+this.mMailboxId);
            
            if (repair) {
                try {
                    mLog.info("Attempting Stage 1 Repair for mailbox "+this.mMailboxId);
                    callback.doIndexRepair();
                } catch (IOException e) {
                    throw ServiceException.FAILURE("Caught IOException while repairing index", e);
                }
                flush();
            }
        }
        
        /////////////////////////////////
        //
        // Stage 2 -- verify SORT_BY_DATE orders match up
        //
        {
            mLog.info("Stage 2 Verify SORT_DATE_ASCENDNIG for Mailbox "+this.mMailboxId);
            
            // SORT_BY__DATE_ASC
            DbMailItem.SearchConstraints c = new DbMailItem.SearchConstraints();
            
            c.mailboxId = mMailboxId;
            c.sort = DbMailItem.SORT_BY_DATE | DbMailItem.SORT_ASCENDING;
            c.types = new byte[] {
                    MailItem.TYPE_CONTACT, 
//                    MailItem.TYPE_INVITE,
                    MailItem.TYPE_MESSAGE,
                    MailItem.TYPE_NOTE
            };
            
            String lucSortField = LuceneFields.L_DATE;
            
            ChkIndexStage2Callback callback = new ChkIndexStage2Callback(this, lucSortField, false);
            
            DbMailItem.search(callback.msgsInMailbox, conn, c);
            MailboxIndex.CountedIndexSearcher searcher = null;
            try {
                callback.beginIterating();
                searcher = getCountedIndexSearcher();
                
                TermQuery q = new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE));
                Hits luceneHits = searcher.getSearcher().search(q, mLuceneSortDateAsc);
                
                for (int i = 0; i < luceneHits.length(); i++) {
                    callback.onDocument(luceneHits.doc(i));
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("Caught IOException while enumerating fields", e);
            } finally {
                if (searcher != null) {
                    searcher.release();
                }
            }
            
            mLog.info("Stage 2 Verification complete for Mailbox "+this.mMailboxId);
            
        }

        /////////////////////////////////
        //
        // Stage 3 -- verify SORT_BY_DATE orders match up
        //
        {
            mLog.info("Stage 3 Verify SORT_DATE_DESCENDING for Mailbox "+this.mMailboxId);
            
            // SORT_BY__DATE_ASC
            DbMailItem.SearchConstraints c = new DbMailItem.SearchConstraints();
            
            c.mailboxId = mMailboxId;
            c.sort = DbMailItem.SORT_BY_DATE | DbMailItem.SORT_DESCENDING;
            c.types = new byte[] {
                    MailItem.TYPE_CONTACT, 
//                    MailItem.TYPE_INVITE,
                    MailItem.TYPE_MESSAGE,
                    MailItem.TYPE_NOTE
            };
            
            String lucSortField = LuceneFields.L_DATE;
            
            ChkIndexStage2Callback callback = new ChkIndexStage2Callback(this, lucSortField, true);
            
            DbMailItem.search(callback.msgsInMailbox, conn, c);
            MailboxIndex.CountedIndexSearcher searcher = null;
            try {
                callback.beginIterating();
                searcher = getCountedIndexSearcher();
                
                TermQuery q = new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE));
                Hits luceneHits = searcher.getSearcher().search(q, mLuceneSortDateDesc);
                
                for (int i = 0; i < luceneHits.length(); i++) {
                    callback.onDocument(luceneHits.doc(i));
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("Caught IOException while enumerating fields", e);
            } finally {
                if (searcher != null) {
                    searcher.release();
                }
            }
            
            mLog.info("Stage 3 Verification complete for Mailbox "+this.mMailboxId);
            
        }
    
    }
    
    /**
     * For testing only 
     * @param msgId
     * @param type TODO
     * 
     * @throws IOException
     * @throws ServiceException
     */
    synchronized public void reIndexItem(int msgId, byte type) throws ServiceException
    {
        Mailbox mbx = Mailbox.getMailboxById(mMailboxId);
        
        MailItem item = null;
        try {
            item = mbx.getItemById(null, msgId, type);
            ParsedMessage pm = null;
            if (item instanceof Message) {
                Message msg = (Message)item;

                // force the pm's received-date to be the correct one
                long msgDate = item.getDate();
                Mailbox mbox = Mailbox.getMailboxById(mMailboxId);
                pm = new ParsedMessage(msg.getMimeMessage(), msgDate, mbox.attachmentsIndexingEnabled());
            }
            
            item.reindex(null, pm);
            
        } catch (java.lang.RuntimeException e) {
            throw ServiceException.FAILURE("Error re-indexing message "+msgId, e);
        }
    }    
    
    public synchronized void deleteIndex() throws IOException
    {
        IndexWriter writer = null;
        try {
            flush();
            //assert(false);
            // TODO - broken right now, need way to forcably close all open indices???
            //				closeIndexReader();
            mLog.info("****Deleting index " + mIdxPath);
            File path = new File(mIdxPath);
            writer = new IndexWriter(path, ZimbraAnalyzer.getInstance(), true);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    protected int countTermOccurences(String fieldName, String term) throws IOException {
        CountedIndexReader reader = getCountedIndexReader();
        try {
            TermEnum e = reader.getReader().terms(new Term(fieldName, term));
            return e.docFreq();
        } finally {
            reader.release();
        }
        
    }
    
    public synchronized void hackIndex() throws IOException
    {
        //
        // this is the place where i put test code when i want to try something quickly 
        // that requires an actual indexreader
        //
    }
    
    public static class AdminInterface {
        MailboxIndex mIdx;
        AdminInterface(MailboxIndex idx) {
            mIdx = idx;
        }
        void close() { };
        
        void hackIndex() throws IOException {
            mIdx.hackIndex();
        }
        
        public synchronized Spans getSpans(SpanQuery q) throws IOException {
            return mIdx.getSpans(q);
        }
        
        void deleteIndex() {
            try {
                mIdx.deleteIndex();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        
        public void backupIndex(java.io.OutputStream out) throws IOException
        {
            mIdx.backupIndex(out);
        }
        public void restoreIndex(java.io.InputStream in) throws IOException
        {
            mIdx.restoreIndex(in);
        }
        
        public static class TermInfo {
            /* (non-Javadoc)
             * @see java.lang.Object#equals(java.lang.Object)
             */
            public Term mTerm;
            public int mFreq; 
            
            public static class FreqComparator implements Comparator 
            {
                public int compare(Object o1, Object o2) {
                    TermInfo lhs = (TermInfo)o1;
                    TermInfo rhs = (TermInfo)o2;
                    
                    if (lhs.mFreq != rhs.mFreq) {
                        return lhs.mFreq - rhs.mFreq;
                    } else {
                        return lhs.mTerm.text().compareTo(rhs.mTerm.text());
                    }
                }
            }
        }
        
        private static class TermEnumCallback implements MailboxIndex.TermEnumInterface {
            private Collection mCollection;
            
            TermEnumCallback(Collection collection) {
                mCollection = collection;
            }
            public void onTerm(Term term, int docFreq) {
                if (term != null) {
                    TermInfo info = new TermInfo();
                    info.mTerm = term;
                    info.mFreq = docFreq;
                    mCollection.add(info);
                }
            }
        }
        
        public void enumerateTerms(Collection /*TermEnumCallback.TermInfo*/ collection, String field) throws IOException {
            TermEnumCallback cb = new TermEnumCallback(collection);
            mIdx.enumerateTermsForField(new Term(field,""), cb);
        }
        
        public int numDocs() throws IOException {
            return mIdx.numDocs();
        }
        
        public int countTermOccurences(String fieldName, String term) throws IOException {
            return mIdx.countTermOccurences(fieldName, term);
        }
    }
}
