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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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
import java.io.InputStream;
import java.util.*;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import org.apache.lucene.analysis.Analyzer;
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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.Volume;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;

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

    void enumerateDocumentsForTerm(Collection collection, String field) throws IOException {
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

    List<SpellSuggestQueryInfo.Suggestion> suggestSpelling(String field, String token) throws ServiceException {
        LinkedList<SpellSuggestQueryInfo.Suggestion> toRet = null;

        token = token.toLowerCase();

        try {
            CountedIndexReader reader = this.getCountedIndexReader();
            try {
                IndexReader iReader = reader.getReader();

                Term term = new Term(field, token);
                int freq = iReader.docFreq(term);
                int numDocs = iReader.numDocs();

                if (freq == 0 && numDocs > 0) {
                    toRet = new LinkedList<SpellSuggestQueryInfo.Suggestion>();

                    float frequency = ((float)freq)/((float)numDocs);

//                    System.out.println("Term: "+token+" appears in "+frequency*100+"% of documents");

                    int suggestionDistance = Integer.MAX_VALUE;

                    FuzzyTermEnum fuzzyEnum = new FuzzyTermEnum(iReader, term, 0.5f, 1);
                    if (fuzzyEnum != null) {
                        do {
                            Term cur = fuzzyEnum.term();
                            if (cur != null) {
                                String curText = cur.text();
                                int curDiff = editDistance(curText, token, curText.length(), token.length());
//                                System.out.println("\tSUGGEST: "+curText+" ["+fuzzyEnum.docFreq()+" docs] dist="+curDiff);
                                
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
                reader.release();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException opening index", e);
        }

        return toRet;
    }

    /**
     * @return TRUE if all tokens were expanded or FALSE if no more tokens could be expanded
     */
    boolean expandWildcardToken(Collection<String> toRet, String field, String token, int maxToReturn) throws ServiceException 
    {
        // all lucene text should be in lowercase...
        token = token.toLowerCase();

        try {
            CountedIndexReader reader = this.getCountedIndexReader();
            try {
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
    public void flush() {
        synchronized(getLock()) {
            closeIndexWriter();
        }
    }

    /**
     * @param itemIds array of itemIds to be deleted
     * 
     * @return an array of itemIds which HAVE BEEN PROCESSED.  If returned.length == 
     * itemIds.length then you can assume the operation was completely successful
     * 
     * @throws IOException on index open failure, nothing processed.
     */
    public int[] deleteDocuments(int itemIds[]) throws IOException {
        synchronized(getLock()) {

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
    }

    private void addDocument(IndexItem redoOp, Document doc, int indexId, long receivedDate, boolean deleteFirst) throws IOException {
        addDocument(redoOp, new Document[] { doc }, indexId, receivedDate, deleteFirst);
    }

    private void addDocument(IndexItem redoOp, Document[] docs, int indexId, long receivedDate, boolean deleteFirst) throws IOException {
        synchronized(getLock()) {        
            long start = 0;
            if (mLog.isDebugEnabled())
                start = System.currentTimeMillis();
            
            if (deleteFirst) {
                try {
                    deleteDocuments(new int[] { indexId });
                } catch(IOException e) {
                    mLog.debug("MailboxIndex.addDocument ignored IOException deleting documents (index does not exist yet?)", e);
                }
            }
            
            openIndexWriter();
            assert(mIndexWriter != null);

            for (Document doc : docs) {
                // doc can be shared by multiple threads if multiple mailboxes
                // are referenced in a single email
                synchronized (doc) {
                    doc.removeFields(LuceneFields.L_MAILBOX_BLOB_ID);
                    doc.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, Integer.toString(indexId),      true/*store*/,  true/*index*/, false/*token*/));

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
    }

    int numDocs() throws IOException 
    {
        synchronized(getLock()) {        
            CountedIndexReader reader = this.getCountedIndexReader();
            try {
                IndexReader iReader = reader.getReader();
                return iReader.numDocs();
            } finally {
                reader.release();
            }
        }
    }

    void enumerateTermsForField(Term firstTerm, TermEnumInterface callback) throws IOException
    {
        synchronized(getLock()) {        
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
    }

    void enumerateDocuments(DocEnumInterface c) throws IOException {
        synchronized(getLock()) {        
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
    }

    Collection getFieldNames() throws IOException {
        synchronized(getLock()) {        
            CountedIndexReader reader = this.getCountedIndexReader();
            try {
                IndexReader iReader = reader.getReader();
                return iReader.getFieldNames();
            } finally {
                reader.release();
            }
        }
    }

    Sort getSort(SortBy searchOrder) {
//      mLuceneSortDateDesc = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, true));
//      mLuceneSortDateAsc = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, false));
//      mLuceneSortSubjectDesc = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, true));
//      mLuceneSortSubjectAsc = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, false));
//      mLuceneSortNameDesc = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, true));
//      mLuceneSortNameAsc = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, false));
        synchronized(getLock()) {
            if (searchOrder != mLatestSortBy) { 
                switch (searchOrder) {
                    case DATE_DESCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, true));
                        mLatestSortBy = searchOrder;
                        break;
                    case DATE_ASCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, false));
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
                        mLatestSort = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, true));
                       mLatestSortBy = SortBy.DATE_ASCENDING;
                }
            }
            return mLatestSort;
        }
    }


    public String toString() {
        StringBuffer ret = new StringBuffer("MailboxIndex(");
        ret.append(mMailboxId);
        ret.append(")");
        return ret.toString();
    }

    public MailboxIndex(Mailbox mbox, String root) throws ServiceException {
        int mailboxId = mbox.getId();
        if (mLog.isDebugEnabled())
            mLog.debug("Opening Index for mailbox " + mailboxId);

        mIndexWriter = null;
        mMailboxId = mailboxId;
        mMailbox = mbox;

        Volume indexVol = Volume.getById(mbox.getIndexVolume());
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
                throw ServiceException.FAILURE("Cannot READ index directory (mailbox="+mbox.getId()+ " idxPath="+mIdxPath+")", null);
            }
            if (!parentDirFile.canWrite()) {
                throw ServiceException.FAILURE("Cannot WRITE index directory (mailbox="+mbox.getId()+ " idxPath="+mIdxPath+")", null);
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
//          } catch(IOException e) {
//          throw ServiceException.FAILURE("Error validating index path (mailbox="+mailbox.getId()+ " root="+root+")", e);
        }
        
        mLatestSort = new Sort(new SortField(LuceneFields.L_DATE, SortField.STRING, true));
        mLatestSortBy = SortBy.DATE_DESCENDING;
        
        String analyzerName = mbox.getAccount().getAttr(Provisioning.A_zimbraTextAnalyzer, null);

        if (analyzerName != null)
            mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
        else
            mAnalyzer = ZimbraAnalyzer.getDefaultAnalyzer();
    }

    boolean checkMailItemExists(int mailItemId) {
        synchronized(getLock()) {        
            return false;
        }
    }


    private String mIdxPath;
    private Sort mLatestSort = null;
    private SortBy mLatestSortBy = null;

    private int mMailboxId;
    private Mailbox mMailbox;
    private static Log mLog = LogFactory.getLog(MailboxIndex.class);
    private ArrayList /*IndexItem*/ mUncommittedRedoOps = new ArrayList();

    private static boolean sNewLockModel;
    static {
        sNewLockModel = true;
        String value = LC.get(LC.debug_mailboxindex_use_new_locking.key());
        if (value != null && !value.equalsIgnoreCase("true"))
            sNewLockModel = false;
    }



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

        // Lucene creates lock files for index update.  When server crashes,
        // these lock files are not deleted and their presence causes all
        // index writes to fail for the affected mailboxes.  So delete them.
        // ("*-write.lock" and "*-commit.lock" files)

        // same lock directory search order as in org.apache.lucene.store.FSDirectory.java
        String luceneTmpDir =
            System.getProperty("org.apache.lucene.lockdir", System.getProperty("java.io.tmpdir"));

        String lockFileSuffix = ".lock";
        File lockFilePath = new File(luceneTmpDir);
        File lockFiles[] = lockFilePath.listFiles();
        if (lockFiles != null && lockFiles.length > 0) {
            for (int i = 0; i < lockFiles.length; i++) {
                File lock = lockFiles[i];
                if (lock != null && lock.isFile() && lock.getName().endsWith(lockFileSuffix)) {
                    mLog.info("Found index lock file " + lock.getName() + " from previous crash.  Deleting...");
                    boolean deleted = lock.delete();
                    if (!deleted) {
                        String message = "Unable to delete index lock file " + lock.getAbsolutePath() + "; Aborting.";
                        Zimbra.halt(message);
                    }
                }
            }
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
        ArrayList<MailboxIndex> toRemove = new ArrayList<MailboxIndex>();

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

    private Analyzer mAnalyzer = null;

    public void initAnalyzer(Mailbox mbox) throws ServiceException {
        // per bug 11052, must always lock the Mailbox before the MailboxIndex, and since
        // mbox.getAccount() is synchronized, we must lock here.
        synchronized (mbox) {
            synchronized (getLock()) {
                String analyzerName = mbox.getAccount().getAttr(Provisioning.A_zimbraTextAnalyzer, null);

                if (analyzerName != null)
                    mAnalyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
                else
                    mAnalyzer = ZimbraAnalyzer.getDefaultAnalyzer();
            }
        }
    }

    public Analyzer getAnalyzer() {
        synchronized(getLock()) {        
            return mAnalyzer;
        }
    }

    /**
     * Close the index writer and write commit/abort entries for all
     * pending IndexItem redo operations.
     */
    private void closeIndexWriter() 
    {
        synchronized(getLock()) {        
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
    }

    private void openIndexWriter() throws IOException
    {
        synchronized(getLock()) {        
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
                writer = new IndexWriter(mIdxPath, getAnalyzer(), false);
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
                writer = new IndexWriter(idxFile, getAnalyzer(), true);
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


            // tim: these are set based on an expectation of ~25k msgs/mbox and assuming that
            // 11 fragment files are OK.  25k msgs with these settings (mf=3, mmd=33) means that
            // each message gets written 9 times to disk...as opposed to 12.5 times with the default
            // lucene settings of 10 and 100....
            writer.mergeFactor = 3;    // should be > 1, otherwise segment sizes are effectively limited to 
            // minMergeDocs documents: and this is probably bad (too many files!)

            writer.minMergeDocs = 33; // we expect 11 index fragment files

            mIndexWriter = writer;

            //
            // tim: this might seem bad, since an index in steady-state-of-writes will never get flushed, 
            // however we also track the number of uncomitted-operations on the index, and will force a 
            // flush if the index has had a lot written to it without a flush.
            //
            updateLastWriteTime();
        }
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
    private CountedIndexReader getCountedIndexReader() throws IOException
    {
        BooleanQuery.setMaxClauseCount(10000); 

        synchronized(getLock()) {        
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
    }

    CountedIndexSearcher getCountedIndexSearcher() throws IOException
    {
        synchronized(getLock()) {        
            CountedIndexSearcher searcher = null;
            CountedIndexReader cReader = getCountedIndexReader();
            searcher = new CountedIndexSearcher(cReader);
            return searcher;
        }
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
    public static final String SEARCH_FOR_TASKS = "task";
    public static final String SEARCH_FOR_NOTES = "note";
    public static final String SEARCH_FOR_WIKI = "wiki";

    public static final String SEARCH_FOR_DOCUMENT = "document";

    public static enum SortBy {
        DATE_ASCENDING  ("dateAsc",   (byte)(DbMailItem.SORT_BY_DATE | DbMailItem.SORT_ASCENDING)), 
        DATE_DESCENDING ("dateDesc",  (byte)(DbMailItem.SORT_BY_DATE | DbMailItem.SORT_DESCENDING)),
        SUBJ_ASCENDING  ("subjAsc",   (byte)(DbMailItem.SORT_BY_SUBJECT | DbMailItem.SORT_ASCENDING)),
        SUBJ_DESCENDING ("subjDesc",  (byte)(DbMailItem.SORT_BY_SUBJECT | DbMailItem.SORT_DESCENDING)),
        NAME_ASCENDING  ("nameAsc",   (byte)(DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_ASCENDING)),
        NAME_DESCENDING ("nameDesc",  (byte)(DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_DESCENDING)),
        SCORE_DESCENDING("score", (byte)0);

        static HashMap<String, SortBy> sNameMap = new HashMap<String, SortBy>();

        static {
            for (SortBy s : SortBy.values()) 
                sNameMap.put(s.mName.toLowerCase(), s);
        }

        byte mSort;
        String mName;

        SortBy(String str, byte sort) {
            mName = str;
            mSort = sort;
        }

        public String toString() { return mName; }

        public byte getDbMailItemSortByte() {
            return mSort;
        }

        public boolean isDescending() {
            return (mSort & DbMailItem.SORT_ASCENDING) == 0;
        }

        public static SortBy lookup(String str) {
            if (str != null)
                return sNameMap.get(str.toLowerCase());
            else
                return null;
        }
    }

    public static byte[] parseTypesString(String groupBy) throws ServiceException
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
            } else if (SEARCH_FOR_TASKS.equals(strs[i])) {
                types[i] = MailItem.TYPE_TASK;
            } else if (SEARCH_FOR_NOTES.equals(strs[i])) {
                types[i] = MailItem.TYPE_NOTE;
            } else if (SEARCH_FOR_WIKI.equals(strs[i])) {
                types[i] = MailItem.TYPE_WIKI;
            } else if (SEARCH_FOR_DOCUMENT.equals(strs[i])) {
                types[i] = MailItem.TYPE_DOCUMENT;
            } else 
                throw ServiceException.INVALID_REQUEST("unknown groupBy: "+strs[i], null);
        }

        return types;
    }

    protected Spans getSpans(SpanQuery q) throws IOException {
        synchronized(getLock()) {        
            CountedIndexReader reader = this.getCountedIndexReader();
            try {
                IndexReader iReader = reader.getReader();
                return q.getSpans(iReader);
            } finally {
                reader.release();
            }
        }
    }


    interface TermEnumInterface {
        abstract void onTerm(Term term, int docFreq); 
    }
    static abstract class DocEnumInterface {
        void maxDocNo(int num) {};
        abstract boolean onDocument(Document doc, boolean isDeleted);
    }

    private static class ChkIndexStage1Callback implements TermEnumInterface 
    {
        HashSet msgsInMailbox = new HashSet(); // hash of all messages in my mailbox
        private MailboxIndex idx = null;
        private ArrayList<Integer> toDelete = new ArrayList<Integer>(); // to be deleted from index
        DbMailItem.SearchResult compareTo = new DbMailItem.SearchResult();  

        ChkIndexStage1Callback(MailboxIndex idx) {
            this.idx = idx;
        }

        void doIndexRepair() throws IOException 
        {
            
            Mailbox mbox = null;
            try {
                mbox = MailboxManager.getInstance().getMailboxById(idx.mMailboxId);
            } catch (ServiceException e) {
                mLog.error("Could not get mailbox: "+idx.mMailboxId+" aborting index repair");
                return;
            }
                
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
                        MailItem item = mbox.getItemById(null, cur.id, cur.type);
                        item.reindex(null, false /* already deleted above */, null);
                    } catch(ServiceException  e) {
                        mLog.info("Couldn't index "+compareTo.id+" caught ServiceException", e);
                    } catch(java.lang.RuntimeException e) {
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
//                      if (!msgsIter.hasNext()) {
//                      if (mMissingTerms == null) {
//                      mLog.info("ERROR: end of msgIter while still iterating index");
//                      }
//                      mLog.info("ERROR: DB no results INDEX has mailitem: "+idxId);
//                      return;
                    } else {
                        // 3 possibilities:
                        //    doc < cur
                        //    doc > cur
                        //    doc == cur
//                      if (truncDocDate < curMsgDate) { // case 1
                        if (compare(truncDocDate,curMsgDate) < 0) { // case 1
                            mLog.info("ERROR1: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
                                        " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
                                        " "+mSortField+"="+sortField);

                            // move on to next document
                            return;
//                          } else if (truncDocDate > curMsgDate) { // case 2
                        } else if (compare(truncDocDate,curMsgDate)>0) { // case 2
//                          mLog.info("ERROR2: DB has "+mCur.id+" (sk="+mCur.sortkey+") next and Index has "+idxId+
//                          " "+"mbid="+idxId+" part="+partName+" date="+docDate+" truncDate="+truncDocDate+
//                          " "+mSortField+"="+sortField);

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

    void chkIndex(boolean repair) throws ServiceException 
    {
        synchronized(getLock()) {        
            flush();

            Connection conn = null;
            conn = DbPool.getConnection();
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId);


            ///////////////////////////////
            //
            // Stage 1 -- look for missing or extra messages and reindex/delete as necessary.
            //
            {
                DbSearchConstraints c = new DbSearchConstraints();

                c.mailbox = mbox;
                c.sort = DbMailItem.SORT_BY_DATE;
                c.types = new HashSet<Byte>();
                c.types.add(MailItem.TYPE_CONTACT); 
                c.types.add(MailItem.TYPE_MESSAGE);
                c.types.add(MailItem.TYPE_NOTE);

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
                DbSearchConstraints c = new DbSearchConstraints();

                c.mailbox = mbox;
                c.sort = DbMailItem.SORT_BY_DATE | DbMailItem.SORT_ASCENDING;
                c.types = new HashSet<Byte>();
                c.types.add(MailItem.TYPE_CONTACT); 
                c.types.add(MailItem.TYPE_MESSAGE);
                c.types.add(MailItem.TYPE_NOTE);

                String lucSortField = LuceneFields.L_DATE;

                ChkIndexStage2Callback callback = new ChkIndexStage2Callback(this, lucSortField, false);

                DbMailItem.search(callback.msgsInMailbox, conn, c);
                MailboxIndex.CountedIndexSearcher searcher = null;
                try {
                    callback.beginIterating();
                    searcher = getCountedIndexSearcher();

                    TermQuery q = new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE));
                    Hits luceneHits = searcher.getSearcher().search(q, getSort(SortBy.DATE_ASCENDING));

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

                // SORT_BY__DATE_DESC
                DbSearchConstraints c = new DbSearchConstraints();

                c.mailbox = mbox;
                c.sort = DbMailItem.SORT_BY_DATE | DbMailItem.SORT_DESCENDING;

                c.types = new HashSet<Byte>();
                c.types.add(MailItem.TYPE_CONTACT); 
                c.types.add(MailItem.TYPE_MESSAGE);
                c.types.add(MailItem.TYPE_NOTE);


                String lucSortField = LuceneFields.L_DATE;

                ChkIndexStage2Callback callback = new ChkIndexStage2Callback(this, lucSortField, true);

                DbMailItem.search(callback.msgsInMailbox, conn, c);
                MailboxIndex.CountedIndexSearcher searcher = null;
                try {
                    callback.beginIterating();
                    searcher = getCountedIndexSearcher();

                    TermQuery q = new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE));
                    Hits luceneHits = searcher.getSearcher().search(q, getSort(SortBy.DATE_DESCENDING));

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
    }

    public void deleteIndex() throws IOException
    {
        synchronized(getLock()) {        
            IndexWriter writer = null;
            try {
                flush();
                //assert(false);
                // TODO - broken right now, need way to forcibly close all open indices???
                //				closeIndexReader();
                mLog.info("****Deleting index " + mIdxPath);
                File path = new File(mIdxPath);

                // can use default analyzer here since it is easier, and since we aren't actually
                // going to do any indexing...
                writer = new IndexWriter(path, ZimbraAnalyzer.getDefaultAnalyzer(), true);
            } finally {
                if (writer != null) {
                    writer.close();
                }
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

    void hackIndex() throws IOException
    {
        synchronized(getLock()) {        
            //
            // this is the place where i put test code when i want to try something quickly 
            // that requires an actual indexreader
            //
        }
    }

    public AdminInterface getAdminInterface() {
        return new AdminInterface(this); 
    }

    public static class AdminInterface {
        MailboxIndex mIdx;
        private AdminInterface(MailboxIndex idx) {
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


   
    /**
     * Entry point for Redo-logging system only.  Everybody else should use MailItem.reindex()
     * 
     * @throws ServiceException
     */
    public void redoIndexItem(Mailbox mbox, boolean deleteFirst, int itemId, byte itemType, long timestamp, boolean noRedo)
    throws IOException, ServiceException {
        MailItem item;
        try {
            item = mbox.getItemById(null, itemId, itemType);
        } catch (MailServiceException.NoSuchItemException e) {
            // Because index commits are batched, during mailbox restore
            // it's possible to see the commit record of indexing operation
            // after the delete operation on the item being indexed.
            // (delete followed by edit, for example)
            // We can't distinguish this legitimate case from a case of
            // really missing the item being indexed due to unexpected
            // problem.  So just ignore the NoSuchItemException.
            return;
        }

        IndexItem redo = null;
        if (!noRedo) {
            redo = new IndexItem(mbox.getId(), item.getId(), itemType, deleteFirst);
            redo.start(System.currentTimeMillis());
            redo.log();
            redo.allowCommit();
        }
        switch (itemType) {
            case MailItem.TYPE_APPOINTMENT:
            case MailItem.TYPE_TASK:
                break;
            case MailItem.TYPE_DOCUMENT:
            case MailItem.TYPE_WIKI:
                try {
                    com.zimbra.cs.mailbox.Document document = (com.zimbra.cs.mailbox.Document)item;
                    ParsedDocument pd = new ParsedDocument(document.getBlob().getBlob().getFile(),
                                document.getName(), 
                                document.getContentType(),
                                timestamp,
                                document.getCreator());
                    indexDocument(mbox, redo, deleteFirst, pd, document);
                } catch (IOException e) {
                    throw ServiceException.FAILURE("indexDocument caught Exception", e);
                }
                break;
            case MailItem.TYPE_MESSAGE:
                Message msg =  mbox.getMessageById(null, itemId);
                InputStream is =msg.getRawMessage();
                MimeMessage mm;
                try {
                    mm = new Mime.FixedMimeMessage(JMSession.getSession(), is);
                    ParsedMessage pm = new ParsedMessage(mm, timestamp, mbox.attachmentsIndexingEnabled());
                    indexMessage(mbox, redo, deleteFirst, pm, msg);
                } catch (Throwable e) {
                    mLog.warn("Skipping indexing; Unable to parse message " + itemId + ": " + e.toString(), e);
                    // Eat up all errors during message analysis.  Throwing
                    // anything here will force server halt during crash
                    // recovery.  Because we can't possibly predict all
                    // data-dependent message parse problems, we opt to live
                    // with unindexed messages rather than P1 support calls.

                    // Write abort record for this item, to prevent repeat calls
                    // to index this unindexable item.
                    if (redo != null)
                        redo.abort();
                } finally {
                    is.close();
                }
                break;
            case MailItem.TYPE_CONTACT:
                indexContact(mbox, redo, deleteFirst, (Contact) item);
                break;
            case MailItem.TYPE_NOTE:
                indexNote(mbox, redo, deleteFirst, (Note) item);
                break;
            default:
                if (redo != null)
                    redo.abort();
            throw ServiceException.FAILURE("Invalid item type for indexing: type=" + itemType, null);
        }
    }

    /**
     * Index a message in the specified mailbox.
     * @param mailboxId
     * @param messageId
     * @param pm
     * @throws ServiceException
     */
    public void indexMessage(Mailbox mbox, IndexItem redo, boolean deleteFirst, ParsedMessage pm, Message msg)
    throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {
            int indexId = msg.getIndexId();

            try {
                List<Document> docList = pm.getLuceneDocuments();
                if (docList != null) {
                    Document[] docs = new Document[docList.size()];
                    docs = docList.toArray(docs);
                    addDocument(redo, docs, indexId, pm.getReceivedDate(), deleteFirst);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("indexMessage caught IOException", e);
            }
        }
    }

    private static void appendContactField(StringBuilder sb, Contact contact, String fieldName) {
        String s = contact.get(fieldName);
        if (s!= null) {
            sb.append(s).append(' ');
        }
    }

    /**
     * Index a Contact in the specified mailbox.
     * @param deleteFirst if TRUE then we must delete the existing index records before we index
     * @param mailItemId
     * @param contact
     * @throws ServiceException
     */
    public void indexContact(Mailbox mbox, IndexItem redo, boolean deleteFirst, Contact contact) throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {        
            if (mLog.isDebugEnabled()) {
                mLog.debug("indexContact("+contact+")");
            }
            try {
                int indexId = contact.getIndexId();
                
                StringBuffer contentText = new StringBuffer();
                Map m = contact.getFields();
                for (Iterator it = m.values().iterator(); it.hasNext(); )
                {
                    String cur = (String)it.next();

                    contentText.append(cur);
                    contentText.append(' ');
                }

                Document doc = new Document();
                String subj = contact.getFileAsString().toLowerCase();
                String name = (subj.length() > DbMailItem.MAX_SENDER_LENGTH ? subj.substring(0, DbMailItem.MAX_SENDER_LENGTH) : subj);

                StringBuilder searchText = new StringBuilder();
                appendContactField(searchText, contact, Contact.A_company);
                appendContactField(searchText, contact, Contact.A_firstName);
                appendContactField(searchText, contact, Contact.A_lastName);

                StringBuilder emailStrBuf = new StringBuilder();
                List<String> emailList = contact.getEmailAddresses();
                for (String cur : emailList) {
                    emailStrBuf.append(cur).append(' ');
                }

                String emailStr = emailStrBuf.toString();

                contentText.append(ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, emailStr));
                searchText.append(ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, emailStr));

                /* put the email addresses in the "To" field so they can be more easily searched */
                doc.add(new Field(LuceneFields.L_H_TO, emailStr,                                               false/*store*/, true/*index*/, true/*token*/));

                /* put the name in the "From" field since the MailItem table uses 'Sender'*/
                doc.add(new Field(LuceneFields.L_H_FROM, name,                                                  false/*store*/, true/*index*/, true/*token*/ ));

                /* bug 11831 - put contact searchable data in its own field so wildcard search works better  */
                doc.add(new Field(LuceneFields.L_CONTACT_DATA, searchText.toString(),                false/*store*/, true/*index*/, true/*token*/));

                doc.add(new Field(LuceneFields.L_CONTENT, contentText.toString(),                      false/*store*/, true/*index*/, true/*token*/));
                doc.add(new Field(LuceneFields.L_H_SUBJECT, subj,                                              false/*store*/, true/*index*/, true/*token*/));
                doc.add(new Field(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_CONTACT,       true/*store*/,  true/*index*/, true/*token*/));
                doc.add(new Field(LuceneFields.L_SORT_SUBJECT, subj.toUpperCase(),                     true/*store*/,  true/*index*/, false /*token*/));
                doc.add(new Field(LuceneFields.L_SORT_NAME, name.toUpperCase(),                         false/*store*/, true/*index*/, false /*token*/));

                addDocument(redo, doc, indexId, contact.getDate(), deleteFirst);

            } catch (IOException ioe) {
                throw ServiceException.FAILURE("indexContact caught IOException", ioe);
            }
        }        
    }    


    /**
     * Index a Note in the specified mailbox.
     * 
     * @throws ServiceException
     */
    public void indexNote(Mailbox mbox, IndexItem redo, boolean deleteFirst, Note note)
    throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {        
            if (mLog.isDebugEnabled()) {
                mLog.debug("indexNote("+note+")");
            }
            try {
                String toIndex = note.getContent();
                int indexId = note.getIndexId(); 

                if (mLog.isDebugEnabled()) {
                    mLog.debug("Note value=\""+toIndex+"\"");
                }

                Document doc = new Document();
                doc.add(Field.UnStored(LuceneFields.L_CONTENT, toIndex));

                String subj = toIndex.toLowerCase();
                String name = (subj != null && subj.length() > DbMailItem.MAX_SENDER_LENGTH ? subj.substring(0, DbMailItem.MAX_SENDER_LENGTH) : subj);

                doc.add(new Field(LuceneFields.L_H_SUBJECT, subj,                                              false/*store*/, true/*index*/, true/*token*/));
                doc.add(new Field(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_NOTE,            true/*store*/, true/*index*/, false/*tokenize*/));

                doc.add(new Field(LuceneFields.L_SORT_SUBJECT, subj.toUpperCase(), false/*store*/, true/*index*/, false /*token*/));
                doc.add(new Field(LuceneFields.L_SORT_NAME, name.toUpperCase(), false/*store*/, true/*index*/, false /*token*/));

//              String dateString = DateField.timeToString(note.getDate());
//              mLog.debug("Note date is: "+dateString);
//              doc.add(Field.Text(LuceneFields.L_DATE, dateString));

                addDocument(redo, doc, indexId, note.getDate(), deleteFirst);

            } catch (IOException e) {
                throw ServiceException.FAILURE("indexNote caught IOException", e);
            }
        }
    }    

    public void indexDocument(Mailbox mbox, IndexItem redo, boolean deleteFirst, 
                ParsedDocument pd, com.zimbra.cs.mailbox.Document doc)  throws ServiceException {
        initAnalyzer(mbox);
        synchronized(getLock()) {        
            try {
                int indexId = doc.getIndexId();
                addDocument(redo, pd.getDocument(), indexId, pd.getCreatedDate(), deleteFirst);
            } catch (IOException e) {
                throw ServiceException.FAILURE("indexDocument caught Exception", e);
            }
        }
    }
    
    final Object getLock() {
        if (sNewLockModel)
            return mMailbox;
        else
            return this;
    }

}
